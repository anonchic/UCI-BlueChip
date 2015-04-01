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

ENTITY ddrsp64a IS
    GENERIC (
        memtech : integer := 0;
        hindex : integer := 3;
        haddr : integer := 1024;
        hmask : integer := 3072;
        ioaddr : integer := 1;
        iomask : integer := 4095;
        MHz : integer := 90;
        col : integer := 9;
        Mbyte : integer := 256;
        fast : integer := 0;
        pwron : integer := 1;
        oepol : integer := 0
    );
    PORT (
        rst : in std_ulogic;
        clk_ddr : in std_ulogic;
        clk_ahb : in std_ulogic;
        ahbsi : in ahb_slv_in_type;
        ahbso : out ahb_slv_out_type;
        sdi : in sdctrl_in_type;
        sdo : out sdctrl_out_type
    );
END ENTITY;

ARCHITECTURE rtl OF ddrsp64a IS
    CONSTANT REVISION : integer := 0;
    CONSTANT CMD_PRE : std_logic_vector ( 2 downto 0 ) := "010";
    CONSTANT CMD_REF : std_logic_vector ( 2 downto 0 ) := "100";
    CONSTANT CMD_LMR : std_logic_vector ( 2 downto 0 ) := "110";
    CONSTANT CMD_EMR : std_logic_vector ( 2 downto 0 ) := "111";
    CONSTANT abuf : integer := 6;
    CONSTANT hconfig : ahb_config_type := ( 0 => ahb_device_reg ( VENDOR_GAISLER , GAISLER_DDRSP , 0 , 0 , 0 ) , 4 => ahb_membar ( 1024 , '1' , '1' , 3072 ) , 5 => ahb_iobar ( 1 , 4095 ) , OTHERS => zero32 );
    TYPE mcycletype IS ( midle , active , ext , leadout );
    TYPE ahb_state_type IS ( midle , rhold , dread , dwrite , whold1 , whold2 );
    TYPE sdcycletype IS ( act1 , act2 , act3 , rd1 , rd2 , rd3 , rd4 , rd5 , rd6 , rd7 , rd8 , wr1 , wr2 , wr3 , wr4a , wr4 , wr5 , sidle , ioreg1 , ioreg2 );
    TYPE icycletype IS ( iidle , pre , ref1 , ref2 , emode , lmode , finish );
    CONSTANT NAHBMST : integer := 16;
    CONSTANT NAHBSLV : integer := 16;
    CONSTANT NAPBSLV : integer := 16;
    CONSTANT NAHBIRQ : integer := 32;
    CONSTANT NAHBAMR : integer := 4;
    CONSTANT NAHBIR : integer := 4;
    CONSTANT NAHBCFG : integer := 4 + 4;
    CONSTANT NAPBIR : integer := 1;
    CONSTANT NAPBAMR : integer := 1;
    CONSTANT NAPBCFG : integer := 1 + 1;
    CONSTANT NBUS : integer := 4;
    SUBTYPE amba_config_word IS std_logic_vector ( 31 downto 0 );
    TYPE ahb_config_type IS ARRAY ( 0 to 4 + 4 - 1 ) OF amba_config_word;
    TYPE apb_config_type IS ARRAY ( 0 to 1 + 1 - 1 ) OF amba_config_word;
    TYPE ahb_mst_in_type IS RECORD
        hgrant : std_logic_vector ( 0 to 16 - 1 );
        hready : std_ulogic;
        hresp : std_logic_vector ( 1 downto 0 );
        hrdata : std_logic_vector ( 31 downto 0 );
        hcache : std_ulogic;
        hirq : std_logic_vector ( 32 - 1 downto 0 );
        testen : std_ulogic;
        testrst : std_ulogic;
        scanen : std_ulogic;
        testoen : std_ulogic;
    END RECORD;
    TYPE ahb_mst_out_type IS RECORD
        hbusreq : std_ulogic;
        hlock : std_ulogic;
        htrans : std_logic_vector ( 1 downto 0 );
        haddr : std_logic_vector ( 31 downto 0 );
        hwrite : std_ulogic;
        hsize : std_logic_vector ( 2 downto 0 );
        hburst : std_logic_vector ( 2 downto 0 );
        hprot : std_logic_vector ( 3 downto 0 );
        hwdata : std_logic_vector ( 31 downto 0 );
        hirq : std_logic_vector ( 32 - 1 downto 0 );
        hconfig : ahb_config_type;
        hindex : integer RANGE 0 to 16 - 1;
    END RECORD;
    TYPE ahb_slv_in_type IS RECORD
        hsel : std_logic_vector ( 0 to 16 - 1 );
        haddr : std_logic_vector ( 31 downto 0 );
        hwrite : std_ulogic;
        htrans : std_logic_vector ( 1 downto 0 );
        hsize : std_logic_vector ( 2 downto 0 );
        hburst : std_logic_vector ( 2 downto 0 );
        hwdata : std_logic_vector ( 31 downto 0 );
        hprot : std_logic_vector ( 3 downto 0 );
        hready : std_ulogic;
        hmaster : std_logic_vector ( 3 downto 0 );
        hmastlock : std_ulogic;
        hmbsel : std_logic_vector ( 0 to 4 - 1 );
        hcache : std_ulogic;
        hirq : std_logic_vector ( 32 - 1 downto 0 );
        testen : std_ulogic;
        testrst : std_ulogic;
        scanen : std_ulogic;
        testoen : std_ulogic;
    END RECORD;
    TYPE ahb_slv_out_type IS RECORD
        hready : std_ulogic;
        hresp : std_logic_vector ( 1 downto 0 );
        hrdata : std_logic_vector ( 31 downto 0 );
        hsplit : std_logic_vector ( 15 downto 0 );
        hcache : std_ulogic;
        hirq : std_logic_vector ( 32 - 1 downto 0 );
        hconfig : ahb_config_type;
        hindex : integer RANGE 0 to 16 - 1;
    END RECORD;
    TYPE ahb_mst_out_vector_type IS ARRAY ( natural RANGE <> ) OF ahb_mst_out_type;
    TYPE ahb_slv_out_vector_type IS ARRAY ( natural RANGE <> ) OF ahb_slv_out_type;
    SUBTYPE ahb_mst_out_vector IS ahb_mst_out_vector_type ( 16 - 1 downto 0 );
    SUBTYPE ahb_slv_out_vector IS ahb_slv_out_vector_type ( 16 - 1 downto 0 );
    TYPE ahb_mst_out_bus_vector IS ARRAY ( 0 to 4 - 1 ) OF ahb_mst_out_vector;
    TYPE ahb_slv_out_bus_vector IS ARRAY ( 0 to 4 - 1 ) OF ahb_slv_out_vector;
    CONSTANT HTRANS_IDLE : std_logic_vector ( 1 downto 0 ) := "00";
    CONSTANT HTRANS_BUSY : std_logic_vector ( 1 downto 0 ) := "01";
    CONSTANT HTRANS_NONSEQ : std_logic_vector ( 1 downto 0 ) := "10";
    CONSTANT HTRANS_SEQ : std_logic_vector ( 1 downto 0 ) := "11";
    CONSTANT HBURST_SINGLE : std_logic_vector ( 2 downto 0 ) := "000";
    CONSTANT HBURST_INCR : std_logic_vector ( 2 downto 0 ) := "001";
    CONSTANT HBURST_WRAP4 : std_logic_vector ( 2 downto 0 ) := "010";
    CONSTANT HBURST_INCR4 : std_logic_vector ( 2 downto 0 ) := "011";
    CONSTANT HBURST_WRAP8 : std_logic_vector ( 2 downto 0 ) := "100";
    CONSTANT HBURST_INCR8 : std_logic_vector ( 2 downto 0 ) := "101";
    CONSTANT HBURST_WRAP16 : std_logic_vector ( 2 downto 0 ) := "110";
    CONSTANT HBURST_INCR16 : std_logic_vector ( 2 downto 0 ) := "111";
    CONSTANT HSIZE_BYTE : std_logic_vector ( 2 downto 0 ) := "000";
    CONSTANT HSIZE_HWORD : std_logic_vector ( 2 downto 0 ) := "001";
    CONSTANT HSIZE_WORD : std_logic_vector ( 2 downto 0 ) := "010";
    CONSTANT HSIZE_DWORD : std_logic_vector ( 2 downto 0 ) := "011";
    CONSTANT HSIZE_4WORD : std_logic_vector ( 2 downto 0 ) := "100";
    CONSTANT HSIZE_8WORD : std_logic_vector ( 2 downto 0 ) := "101";
    CONSTANT HSIZE_16WORD : std_logic_vector ( 2 downto 0 ) := "110";
    CONSTANT HSIZE_32WORD : std_logic_vector ( 2 downto 0 ) := "111";
    CONSTANT HRESP_OKAY : std_logic_vector ( 1 downto 0 ) := "00";
    CONSTANT HRESP_ERROR : std_logic_vector ( 1 downto 0 ) := "01";
    CONSTANT HRESP_RETRY : std_logic_vector ( 1 downto 0 ) := "10";
    CONSTANT HRESP_SPLIT : std_logic_vector ( 1 downto 0 ) := "11";
    TYPE apb_slv_in_type IS RECORD
        psel : std_logic_vector ( 0 to 16 - 1 );
        penable : std_ulogic;
        paddr : std_logic_vector ( 31 downto 0 );
        pwrite : std_ulogic;
        pwdata : std_logic_vector ( 31 downto 0 );
        pirq : std_logic_vector ( 32 - 1 downto 0 );
        testen : std_ulogic;
        testrst : std_ulogic;
        scanen : std_ulogic;
        testoen : std_ulogic;
    END RECORD;
    TYPE apb_slv_out_type IS RECORD
        prdata : std_logic_vector ( 31 downto 0 );
        pirq : std_logic_vector ( 32 - 1 downto 0 );
        pconfig : apb_config_type;
        pindex : integer RANGE 0 to 16 - 1;
    END RECORD;
    TYPE apb_slv_out_vector IS ARRAY ( 0 to 16 - 1 ) OF apb_slv_out_type;
    CONSTANT AMBA_CONFIG_VER0 : std_logic_vector ( 1 downto 0 ) := "00";
    SUBTYPE amba_vendor_type IS integer RANGE 0 to 16#ff#;
    SUBTYPE amba_device_type IS integer RANGE 0 to 16#3ff#;
    SUBTYPE amba_version_type IS integer RANGE 0 to 16#3f#;
    SUBTYPE amba_cfgver_type IS integer RANGE 0 to 3;
    SUBTYPE amba_irq_type IS integer RANGE 0 to 32 - 1;
    SUBTYPE ahb_addr_type IS integer RANGE 0 to 16#fff#;
    CONSTANT zx : std_logic_vector ( 31 downto 0 ) := ( OTHERS => '0' );
    CONSTANT zxirq : std_logic_vector ( 32 - 1 downto 0 ) := ( OTHERS => '0' );
    CONSTANT zy : std_logic_vector ( 0 to 31 ) := ( OTHERS => '0' );
    TYPE memory_in_type IS RECORD
        data : std_logic_vector ( 31 downto 0 );
        brdyn : std_logic;
        bexcn : std_logic;
        writen : std_logic;
        wrn : std_logic_vector ( 3 downto 0 );
        bwidth : std_logic_vector ( 1 downto 0 );
        sd : std_logic_vector ( 63 downto 0 );
        cb : std_logic_vector ( 7 downto 0 );
        scb : std_logic_vector ( 7 downto 0 );
        edac : std_logic;
    END RECORD;
    TYPE memory_out_type IS RECORD
        address : std_logic_vector ( 31 downto 0 );
        data : std_logic_vector ( 31 downto 0 );
        sddata : std_logic_vector ( 63 downto 0 );
        ramsn : std_logic_vector ( 7 downto 0 );
        ramoen : std_logic_vector ( 7 downto 0 );
        ramn : std_ulogic;
        romn : std_ulogic;
        mben : std_logic_vector ( 3 downto 0 );
        iosn : std_logic;
        romsn : std_logic_vector ( 7 downto 0 );
        oen : std_logic;
        writen : std_logic;
        wrn : std_logic_vector ( 3 downto 0 );
        bdrive : std_logic_vector ( 3 downto 0 );
        vbdrive : std_logic_vector ( 31 downto 0 );
        svbdrive : std_logic_vector ( 63 downto 0 );
        read : std_logic;
        sa : std_logic_vector ( 14 downto 0 );
        cb : std_logic_vector ( 7 downto 0 );
        scb : std_logic_vector ( 7 downto 0 );
        vcdrive : std_logic_vector ( 7 downto 0 );
        svcdrive : std_logic_vector ( 7 downto 0 );
        ce : std_ulogic;
    END RECORD;
    TYPE sdctrl_in_type IS RECORD
        wprot : std_ulogic;
        data : std_logic_vector ( 127 downto 0 );
        cb : std_logic_vector ( 15 downto 0 );
    END RECORD;
    TYPE sdctrl_out_type IS RECORD
        sdcke : std_logic_vector ( 1 downto 0 );
        sdcsn : std_logic_vector ( 1 downto 0 );
        sdwen : std_ulogic;
        rasn : std_ulogic;
        casn : std_ulogic;
        dqm : std_logic_vector ( 15 downto 0 );
        bdrive : std_ulogic;
        qdrive : std_ulogic;
        vbdrive : std_logic_vector ( 31 downto 0 );
        address : std_logic_vector ( 16 downto 2 );
        data : std_logic_vector ( 127 downto 0 );
        cb : std_logic_vector ( 15 downto 0 );
        ce : std_ulogic;
        ba : std_logic_vector ( 1 downto 0 );
        cal_en : std_logic_vector ( 7 downto 0 );
        cal_inc : std_logic_vector ( 7 downto 0 );
        cal_rst : std_logic;
        odt : std_logic_vector ( 1 downto 0 );
    END RECORD;
    TYPE sdram_out_type IS RECORD
        sdcke : std_logic_vector ( 1 downto 0 );
        sdcsn : std_logic_vector ( 1 downto 0 );
        sdwen : std_ulogic;
        rasn : std_ulogic;
        casn : std_ulogic;
        dqm : std_logic_vector ( 7 downto 0 );
    END RECORD;
    TYPE sdram_cfg_type IS RECORD
        command : std_logic_vector ( 2 downto 0 );
        csize : std_logic_vector ( 1 downto 0 );
        bsize : std_logic_vector ( 2 downto 0 );
        trcd : std_ulogic;
        trfc : std_logic_vector ( 2 downto 0 );
        trp : std_ulogic;
        refresh : std_logic_vector ( 11 downto 0 );
        renable : std_ulogic;
        dllrst : std_ulogic;
        refon : std_ulogic;
        cke : std_ulogic;
    END RECORD;
    TYPE access_param IS RECORD
        haddr : std_logic_vector ( 31 downto 0 );
        size : std_logic_vector ( 1 downto 0 );
        hwrite : std_ulogic;
        hio : std_ulogic;
    END RECORD;
    TYPE ahb_reg_type IS RECORD
        hready : std_ulogic;
        hsel : std_ulogic;
        hio : std_ulogic;
        startsd : std_ulogic;
        ready : std_ulogic;
        ready2 : std_ulogic;
        write : std_logic_vector ( 3 downto 0 );
        state : ahb_state_type;
        haddr : std_logic_vector ( 31 downto 0 );
        hrdata : std_logic_vector ( 31 downto 0 );
        hwdata : std_logic_vector ( 31 downto 0 );
        hwrite : std_ulogic;
        htrans : std_logic_vector ( 1 downto 0 );
        hresp : std_logic_vector ( 1 downto 0 );
        raddr : std_logic_vector ( 6 - 1 downto 0 );
        size : std_logic_vector ( 1 downto 0 );
        acc : access_param;
    END RECORD;
    TYPE ddr_reg_type IS RECORD
        startsd : std_ulogic;
        startsdold : std_ulogic;
        burst : std_ulogic;
        hready : std_ulogic;
        bdrive : std_ulogic;
        qdrive : std_ulogic;
        nbdrive : std_ulogic;
        mstate : mcycletype;
        sdstate : sdcycletype;
        cmstate : mcycletype;
        istate : icycletype;
        trfc : std_logic_vector ( 2 downto 0 );
        refresh : std_logic_vector ( 11 downto 0 );
        sdcsn : std_logic_vector ( 1 downto 0 );
        sdwen : std_ulogic;
        rasn : std_ulogic;
        casn : std_ulogic;
        dqm : std_logic_vector ( 15 downto 0 );
        address : std_logic_vector ( 15 downto 2 );
        ba : std_logic_vector ( 1 downto 0 );
        waddr : std_logic_vector ( 6 - 1 downto 0 );
        cfg : sdram_cfg_type;
        hrdata : std_logic_vector ( 127 downto 0 );
    END RECORD;
    SIGNAL vcc : std_ulogic;
    SIGNAL r : ddr_reg_type;
    SIGNAL ri : ddr_reg_type;
    SIGNAL ra : ahb_reg_type;
    SIGNAL rai : ahb_reg_type;
    SIGNAL rbdrive : std_logic_vector ( 31 downto 0 );
    SIGNAL ribdrive : std_logic_vector ( 31 downto 0 );
    SIGNAL rdata : std_logic_vector ( 127 downto 0 );
    SIGNAL wdata : std_logic_vector ( 127 downto 0 );
    ATTRIBUTE syn_preserve : boolean;
    ATTRIBUTE syn_preserve OF rbdrive : signal IS true;
BEGIN
    vcc <= '1';
    ahb_ctrl : PROCESS ( rst , ahbsi , r , ra , rdata )
        VARIABLE v : ahb_reg_type;
        VARIABLE startsd : std_ulogic;
        VARIABLE dout : std_logic_vector ( 31 downto 0 );
    BEGIN
        v := ra;
        v.hresp := "00";
        v.write := "0000";
        CASE ra.raddr ( 1 downto 0 ) IS
            WHEN "00" =>
                v.hrdata := rdata ( 127 downto 96 );
            WHEN "01" =>
                v.hrdata := rdata ( 95 downto 64 );
            WHEN "10" =>
                v.hrdata := rdata ( 63 downto 32 );
            WHEN OTHERS =>
                v.hrdata := rdata ( 31 downto 0 );
        END CASE;
        v.ready := not ( ra.startsd xor r.startsdold );
        v.ready2 := ra.ready;
        IF ( ( ahbsi.hready and ahbsi.hsel ( 3 ) ) = '1' ) THEN
            v.htrans := ahbsi.htrans;
            v.haddr := ahbsi.haddr;
            v.size := ahbsi.hsize ( 1 downto 0 );
            v.hwrite := ahbsi.hwrite;
            IF ahbsi.htrans ( 1 ) = '1' THEN
                v.hio := ahbsi.hmbsel ( 1 );
                v.hsel := '1';
                v.hready := '0';
            END IF;
        END IF;
        IF ahbsi.hready = '1' THEN
            v.hsel := ahbsi.hsel ( 3 );
        END IF;
        CASE ra.state IS
            WHEN midle =>
                IF ( ( v.hsel and v.htrans ( 1 ) ) = '1' ) THEN
                    IF v.hwrite = '0' THEN
                        v.state := rhold;
                        v.startsd := not ra.startsd;
                    ELSE
                        v.state := dwrite;
                        v.hready := '1';
                        v.write := decode ( v.haddr ( 3 downto 2 ) );
                    END IF;
                END IF;
                v.raddr := ra.haddr ( 7 downto 2 );
                v.ready := '0';
                v.ready2 := '0';
                IF ahbsi.hready = '1' THEN
                    v.acc := ( v.haddr , v.size , v.hwrite , v.hio );
                END IF;
            WHEN rhold =>
                v.raddr := ra.haddr ( 7 downto 2 );
                IF ra.ready2 = '1' THEN
                    v.state := dread;
                    v.hready := '1';
                    v.raddr := ra.raddr + 1;
                END IF;
            WHEN dread =>
                v.raddr := ra.raddr + 1;
                v.hready := '1';
                IF ( ( v.hsel and v.htrans ( 1 ) and v.htrans ( 0 ) ) = '0' ) or ( ra.raddr ( 2 downto 0 ) = "000" ) THEN
                    v.state := midle;
                    v.hready := '0';
                END IF;
                v.acc := ( v.haddr , v.size , v.hwrite , v.hio );
            WHEN dwrite =>
                v.raddr := ra.haddr ( 7 downto 2 );
                v.hready := '1';
                v.write := decode ( v.haddr ( 3 downto 2 ) );
                IF ( ( v.hsel and v.htrans ( 1 ) and v.htrans ( 0 ) ) = '0' ) or ( ra.haddr ( 4 downto 2 ) = "111" ) THEN
                    v.startsd := not ra.startsd;
                    v.state := whold1;
                    v.write := "0000";
                    v.hready := '0';
                END IF;
            WHEN whold1 =>
                v.state := whold2;
                v.ready := '0';
            WHEN whold2 =>
                IF ra.ready = '1' THEN
                    v.state := midle;
                    v.acc := ( v.haddr , v.size , v.hwrite , v.hio );
                END IF;
        END CASE;
        v.hwdata := ahbsi.hwdata;
        IF ( ahbsi.hready and ahbsi.hsel ( 3 ) ) = '1' THEN
            IF ahbsi.htrans ( 1 ) = '0' THEN
                v.hready := '1';
            END IF;
        END IF;
        dout := ra.hrdata ( 31 downto 0 );
        IF rst = '0' THEN
            v.hsel := '0';
            v.hready := '1';
            v.state := midle;
            v.startsd := '0';
            v.hio := '0';
        END IF;
        rai <= v;
        ahbso.hready <= ra.hready;
        ahbso.hresp <= ra.hresp;
        ahbso.hrdata <= dout;
        ahbso.hcache <= not ra.hio;
    END PROCESS;
    ddr_ctrl : PROCESS ( rst , r , ra , sdi , rbdrive , wdata )
        VARIABLE v : ddr_reg_type;
        VARIABLE startsd : std_ulogic;
        VARIABLE dqm : std_logic_vector ( 15 downto 0 );
        VARIABLE raddr : std_logic_vector ( 13 downto 0 );
        VARIABLE adec : std_ulogic;
        VARIABLE rams : std_logic_vector ( 1 downto 0 );
        VARIABLE ba : std_logic_vector ( 1 downto 0 );
        VARIABLE haddr : std_logic_vector ( 31 downto 0 );
        VARIABLE hsize : std_logic_vector ( 1 downto 0 );
        VARIABLE hwrite : std_ulogic;
        VARIABLE htrans : std_logic_vector ( 1 downto 0 );
        VARIABLE hready : std_ulogic;
        VARIABLE vbdrive : std_logic_vector ( 31 downto 0 );
        VARIABLE bdrive : std_ulogic;
        VARIABLE writecfg : std_ulogic;
        VARIABLE regsd1 : std_logic_vector ( 31 downto 0 );
        VARIABLE regsd2 : std_logic_vector ( 31 downto 0 );
    BEGIN
        v := r;
        v.hready := '0';
        writecfg := '0';
        vbdrive := rbdrive;
        v.hrdata := sdi.data;
        v.qdrive := '0';
        regsd1 := ( OTHERS => '0' );
        regsd1 ( 31 downto 15 ) := r.cfg.refon & r.cfg.trp & r.cfg.trfc & r.cfg.trcd & r.cfg.bsize & r.cfg.csize & r.cfg.command & r.cfg.dllrst & r.cfg.renable & r.cfg.cke;
        regsd1 ( 11 downto 0 ) := r.cfg.refresh;
        regsd2 := ( OTHERS => '0' );
        regsd2 ( 8 downto 0 ) := conv_std_logic_vector ( 90 , 9 );
        regsd2 ( 14 downto 12 ) := conv_std_logic_vector ( 3 , 3 );
        CASE ra.acc.size IS
            WHEN "00" =>
                CASE ra.acc.haddr ( 3 downto 0 ) IS
                    WHEN "0000" =>
                        dqm := "0111111111111111";
                    WHEN "0001" =>
                        dqm := "1011111111111111";
                    WHEN "0010" =>
                        dqm := "1101111111111111";
                    WHEN "0011" =>
                        dqm := "1110111111111111";
                    WHEN "0100" =>
                        dqm := "1111011111111111";
                    WHEN "0101" =>
                        dqm := "1111101111111111";
                    WHEN "0110" =>
                        dqm := "1111110111111111";
                    WHEN "0111" =>
                        dqm := "1111111011111111";
                    WHEN "1000" =>
                        dqm := "1111111101111111";
                    WHEN "1001" =>
                        dqm := "1111111110111111";
                    WHEN "1010" =>
                        dqm := "1111111111011111";
                    WHEN "1011" =>
                        dqm := "1111111111101111";
                    WHEN "1100" =>
                        dqm := "1111111111110111";
                    WHEN "1101" =>
                        dqm := "1111111111111011";
                    WHEN "1110" =>
                        dqm := "1111111111111101";
                    WHEN OTHERS =>
                        dqm := "1111111111111110";
                END CASE;
            WHEN "01" =>
                CASE ra.acc.haddr ( 3 downto 1 ) IS
                    WHEN "000" =>
                        dqm := "0011111111111111";
                    WHEN "001" =>
                        dqm := "1100111111111111";
                    WHEN "010" =>
                        dqm := "1111001111111111";
                    WHEN "011" =>
                        dqm := "1111110011111111";
                    WHEN "100" =>
                        dqm := "1111111100111111";
                    WHEN "101" =>
                        dqm := "1111111111001111";
                    WHEN "110" =>
                        dqm := "1111111111110011";
                    WHEN OTHERS =>
                        dqm := "1111111111111100";
                END CASE;
            WHEN OTHERS =>
                dqm := "0000000000000000";
        END CASE;
        v.startsd := ra.startsd;
        CASE r.mstate IS
            WHEN midle =>
                IF r.startsd = '1' THEN
                    IF ( r.sdstate = sidle ) and ( r.cfg.command = "000" ) and ( r.cmstate = midle ) THEN
                        startsd := '1';
                        v.mstate := active;
                    END IF;
                END IF;
            WHEN OTHERS =>
                NULL;
        END CASE;
        startsd := r.startsd xor r.startsdold;
        haddr := ra.acc.haddr;
        CASE r.cfg.csize IS
            WHEN "00" =>
            WHEN "01" =>
            WHEN "10" =>
            WHEN OTHERS =>
        END CASE;
        rams := adec & not adec;
        IF r.trfc /= "000" THEN
            v.trfc := r.trfc - 1;
        END IF;
        CASE r.sdstate IS
            WHEN sidle =>
                IF ( startsd = '1' ) and ( r.cfg.command = "000" ) and ( r.cmstate = midle ) and ( r.istate = finish ) THEN
                    v.address := raddr;
                    v.ba := ba;
                    IF ra.acc.hio = '0' THEN
                        v.sdcsn := not rams ( 1 downto 0 );
                        v.rasn := '0';
                        v.sdstate := act1;
                    ELSE
                        v.sdstate := ioreg1;
                    END IF;
                END IF;
                v.waddr := ra.acc.haddr ( 7 downto 2 );
            WHEN act1 =>
                v.rasn := '1';
                v.trfc := r.cfg.trfc;
                IF r.cfg.trcd = '1' THEN
                    v.sdstate := act2;
                ELSE
                    v.sdstate := act3;
                    v.hready := ra.acc.hwrite;
                END IF;
                v.waddr := ra.acc.haddr ( 7 downto 2 );
            WHEN act2 =>
                v.sdstate := act3;
                v.hready := ra.acc.hwrite;
            WHEN act3 =>
                v.casn := '0';
                v.address := ra.acc.haddr ( 15 downto 13 ) & '0' & ra.acc.haddr ( 12 downto 4 ) & '0';
                v.dqm := dqm;
                IF ra.acc.hwrite = '1' THEN
                    v.waddr := r.waddr + 4;
                    v.waddr ( 1 downto 0 ) := "00";
                    v.sdstate := wr1;
                    v.sdwen := '0';
                    v.bdrive := '0';
                    v.qdrive := '1';
                    IF ( r.waddr /= ra.raddr ) THEN
                        v.hready := '1';
                        IF ( r.waddr ( 5 downto 2 ) = ra.raddr ( 5 downto 2 ) ) THEN
                            IF r.waddr ( 1 ) = '1' THEN
                                v.dqm ( 15 downto 8 ) := ( OTHERS => '1' );
                            ELSE
                                CASE ra.raddr ( 1 downto 0 ) IS
                                    WHEN "01" =>
                                        v.dqm ( 7 downto 0 ) := ( OTHERS => '1' );
                                    WHEN "10" =>
                                        v.dqm ( 3 downto 0 ) := ( OTHERS => '1' );
                                        v.dqm ( 15 downto 12 ) := ( OTHERS => r.waddr ( 0 ) );
                                    WHEN OTHERS =>
                                        v.dqm ( 15 downto 12 ) := ( OTHERS => r.waddr ( 0 ) );
                                END CASE;
                            END IF;
                        ELSE
                            CASE r.waddr ( 1 downto 0 ) IS
                                WHEN "01" =>
                                    v.dqm ( 15 downto 12 ) := ( OTHERS => '1' );
                                WHEN "10" =>
                                    v.dqm ( 15 downto 8 ) := ( OTHERS => '1' );
                                WHEN "11" =>
                                    v.dqm ( 15 downto 4 ) := ( OTHERS => '1' );
                                WHEN OTHERS =>
                                    NULL;
                            END CASE;
                        END IF;
                    ELSE
                        CASE r.waddr ( 1 downto 0 ) IS
                            WHEN "00" =>
                                v.dqm ( 11 downto 0 ) := ( OTHERS => '1' );
                            WHEN "01" =>
                                v.dqm ( 15 downto 12 ) := ( OTHERS => '1' );
                                v.dqm ( 7 downto 0 ) := ( OTHERS => '1' );
                            WHEN "10" =>
                                v.dqm ( 15 downto 8 ) := ( OTHERS => '1' );
                                v.dqm ( 3 downto 0 ) := ( OTHERS => '1' );
                            WHEN OTHERS =>
                                v.dqm ( 15 downto 4 ) := ( OTHERS => '1' );
                        END CASE;
                    END IF;
                ELSE
                    v.sdstate := rd1;
                END IF;
            WHEN wr1 =>
                v.sdwen := '1';
                v.casn := '1';
                v.qdrive := '1';
                v.waddr := r.waddr + 4;
                v.dqm := ( OTHERS => '0' );
                v.address ( 8 downto 3 ) := r.waddr;
                IF ( r.waddr <= ra.raddr ) and ( r.waddr ( 5 downto 2 ) /= "0000" ) and ( r.hready = '1' ) THEN
                    v.hready := '1';
                    IF ( r.hready = '1' ) and ( r.waddr ( 2 downto 0 ) = "000" ) THEN
                        v.sdwen := '0';
                        v.casn := '0';
                    END IF;
                    IF ( r.waddr ( 5 downto 2 ) = ra.raddr ( 5 downto 2 ) ) and ( r.waddr /= "000000" ) THEN
                        CASE ra.raddr ( 1 downto 0 ) IS
                            WHEN "00" =>
                                v.dqm ( 11 downto 0 ) := ( OTHERS => '1' );
                            WHEN "01" =>
                                v.dqm ( 7 downto 0 ) := ( OTHERS => '1' );
                            WHEN "10" =>
                                v.dqm ( 3 downto 0 ) := ( OTHERS => '1' );
                            WHEN OTHERS =>
                                NULL;
                        END CASE;
                    END IF;
                ELSE
                    v.sdstate := wr2;
                    v.dqm := ( OTHERS => '1' );
                    v.startsdold := r.startsd;
                END IF;
            WHEN wr2 =>
                v.sdstate := wr3;
                v.qdrive := '1';
            WHEN wr3 =>
                v.sdstate := wr4a;
                v.qdrive := '1';
            WHEN wr4a =>
                v.bdrive := '1';
                v.rasn := '0';
                v.sdwen := '0';
                v.sdstate := wr4;
                v.qdrive := '1';
            WHEN wr4 =>
                v.sdcsn := "11";
                v.rasn := '1';
                v.sdwen := '1';
                v.qdrive := '0';
                v.sdstate := wr5;
            WHEN wr5 =>
                v.sdstate := sidle;
            WHEN rd1 =>
                v.casn := '1';
                v.sdstate := rd7;
            WHEN rd7 =>
                v.casn := '1';
                v.sdstate := rd2;
            WHEN rd2 =>
                v.casn := '1';
                v.sdstate := rd3;
            WHEN rd3 =>
                IF 0 = 0 THEN
                    v.startsdold := r.startsd;
                END IF;
                v.sdstate := rd4;
                v.hready := '1';
                v.casn := '1';
                IF v.hready = '1' THEN
                    v.waddr := r.waddr + 4;
                END IF;
            WHEN rd4 =>
                v.hready := '1';
                v.casn := '1';
                IF ( r.sdcsn = "11" ) or ( r.waddr ( 2 downto 2 ) = "1" ) THEN
                    v.dqm := ( OTHERS => '1' );
                    v.burst := '0';
                    IF 0 /= 0 THEN
                        v.startsdold := r.startsd;
                    END IF;
                    IF ( r.sdcsn /= "11" ) THEN
                        v.rasn := '0';
                        v.sdwen := '0';
                        v.sdstate := rd5;
                    ELSE
                        IF r.cfg.trp = '1' THEN
                            v.sdstate := rd6;
                        ELSE
                            v.sdstate := sidle;
                        END IF;
                    END IF;
                END IF;
                IF v.hready = '1' THEN
                    v.waddr := r.waddr + 4;
                END IF;
            WHEN rd5 =>
                IF r.cfg.trp = '1' THEN
                    v.sdstate := rd6;
                ELSE
                    v.sdstate := sidle;
                END IF;
                v.sdcsn := ( OTHERS => '1' );
                v.rasn := '1';
                v.sdwen := '1';
                v.dqm := ( OTHERS => '1' );
            WHEN rd6 =>
                v.sdstate := sidle;
                v.dqm := ( OTHERS => '1' );
                v.sdcsn := ( OTHERS => '1' );
                v.rasn := '1';
                v.sdwen := '1';
            WHEN ioreg1 =>
                v.hrdata ( 127 downto 64 ) := regsd1 & regsd2;
                v.sdstate := ioreg2;
                IF ra.acc.hwrite = '0' THEN
                    v.hready := '1';
                END IF;
            WHEN ioreg2 =>
                writecfg := ra.acc.hwrite and not r.waddr ( 0 );
                v.startsdold := r.startsd;
                v.sdstate := sidle;
            WHEN OTHERS =>
                v.sdstate := sidle;
        END CASE;
        CASE r.cmstate IS
            WHEN midle =>
                IF r.sdstate = sidle THEN
                    CASE r.cfg.command IS
                        WHEN "010" =>
                            v.sdcsn := ( OTHERS => '0' );
                            v.rasn := '0';
                            v.sdwen := '0';
                            v.address ( 12 ) := '1';
                            v.cmstate := active;
                        WHEN "100" =>
                            v.sdcsn := ( OTHERS => '0' );
                            v.rasn := '0';
                            v.casn := '0';
                            v.cmstate := active;
                        WHEN "111" =>
                            v.sdcsn := ( OTHERS => '0' );
                            v.rasn := '0';
                            v.casn := '0';
                            v.sdwen := '0';
                            v.cmstate := active;
                            v.ba := "01";
                            v.address := "00000000000000";
                        WHEN "110" =>
                            v.sdcsn := ( OTHERS => '0' );
                            v.rasn := '0';
                            v.casn := '0';
                            v.sdwen := '0';
                            v.cmstate := active;
                            v.ba := "00";
                            v.address := "00000" & r.cfg.dllrst & "0" & "01" & "00010";
                        WHEN OTHERS =>
                            NULL;
                    END CASE;
                END IF;
            WHEN active =>
                v.sdcsn := ( OTHERS => '1' );
                v.rasn := '1';
                v.casn := '1';
                v.sdwen := '1';
                v.cfg.command := "000";
                v.cmstate := leadout;
                v.trfc := r.cfg.trfc;
            WHEN OTHERS =>
                IF r.trfc = "000" THEN
                    v.cmstate := midle;
                END IF;
        END CASE;
        CASE r.istate IS
            WHEN iidle =>
                IF r.cfg.renable = '1' THEN
                    v.cfg.cke := '1';
                    v.cfg.dllrst := '1';
                    IF r.cfg.cke = '1' THEN
                        v.istate := pre;
                        v.cfg.command := "010";
                    END IF;
                    v.ba := "00";
                END IF;
            WHEN pre =>
                IF r.cfg.command = "000" THEN
                    v.cfg.command := "11" & r.cfg.dllrst;
                    IF r.cfg.dllrst = '1' THEN
                        v.istate := emode;
                    ELSE
                        v.istate := lmode;
                    END IF;
                END IF;
            WHEN emode =>
                IF r.cfg.command = "000" THEN
                    v.istate := lmode;
                    v.cfg.command := "110";
                END IF;
            WHEN lmode =>
                IF r.cfg.command = "000" THEN
                    IF r.cfg.dllrst = '1' THEN
                        IF r.refresh ( 9 downto 8 ) = "00" THEN
                            v.cfg.command := "010";
                            v.istate := ref1;
                        END IF;
                    ELSE
                        v.istate := finish;
                        v.cfg.refon := '1';
                        v.cfg.renable := '0';
                    END IF;
                END IF;
            WHEN ref1 =>
                IF r.cfg.command = "000" THEN
                    v.cfg.command := "100";
                    v.cfg.dllrst := '0';
                    v.istate := ref2;
                END IF;
            WHEN ref2 =>
                IF r.cfg.command = "000" THEN
                    v.cfg.command := "100";
                    v.istate := pre;
                END IF;
            WHEN OTHERS =>
                IF r.cfg.renable = '1' THEN
                    v.istate := iidle;
                    v.cfg.dllrst := '1';
                END IF;
        END CASE;
        CASE r.mstate IS
            WHEN active =>
                IF v.hready = '1' THEN
                    v.mstate := midle;
                END IF;
            WHEN OTHERS =>
                NULL;
        END CASE;
        IF ( ( r.cfg.refon = '1' ) and ( r.istate = finish ) ) or ( r.cfg.dllrst = '1' ) THEN
            v.refresh := r.refresh - 1;
            IF ( v.refresh ( 11 ) and not r.refresh ( 11 ) ) = '1' THEN
                v.refresh := r.cfg.refresh;
                IF r.cfg.dllrst = '0' THEN
                    v.cfg.command := "100";
                END IF;
            END IF;
        END IF;
        IF ( ra.acc.hio and ra.acc.hwrite and writecfg ) = '1' THEN
            v.cfg.refresh := wdata ( 11 + 96 downto 0 + 96 );
            v.cfg.cke := wdata ( 15 + 96 );
            v.cfg.renable := wdata ( 16 + 96 );
            v.cfg.dllrst := wdata ( 17 + 96 );
            v.cfg.command := wdata ( 20 + 96 downto 18 + 96 );
            v.cfg.csize := wdata ( 22 + 96 downto 21 + 96 );
            v.cfg.bsize := wdata ( 25 + 96 downto 23 + 96 );
            v.cfg.trcd := wdata ( 26 + 96 );
            v.cfg.trfc := wdata ( 29 + 96 downto 27 + 96 );
            v.cfg.trp := wdata ( 30 + 96 );
            v.cfg.refon := wdata ( 31 + 96 );
        END IF;
        v.nbdrive := not v.bdrive;
        IF 0 = 1 THEN
            bdrive := r.nbdrive;
            vbdrive := ( OTHERS => v.nbdrive );
        ELSE
            bdrive := r.bdrive;
            vbdrive := ( OTHERS => v.bdrive );
        END IF;
        IF rst = '0' THEN
            v.sdstate := sidle;
            v.mstate := midle;
            v.istate := finish;
            v.cmstate := midle;
            v.cfg.command := "000";
            v.cfg.csize := conv_std_logic_vector ( 9 - 9 , 2 );
            v.cfg.bsize := conv_std_logic_vector ( log2 ( 256 / 8 ) , 3 );
            IF 90 > 100 THEN
                v.cfg.trcd := '1';
            ELSE
                v.cfg.trcd := '0';
            END IF;
            v.cfg.refon := '0';
            v.cfg.trfc := conv_std_logic_vector ( 75 * 90 / 1000 - 2 , 3 );
            v.cfg.refresh := conv_std_logic_vector ( 7800 * 90 / 1000 , 12 );
            v.refresh := ( OTHERS => '0' );
            IF 1 = 1 THEN
                v.cfg.renable := '1';
            ELSE
                v.cfg.renable := '0';
            END IF;
            IF 90 > 100 THEN
                v.cfg.trp := '1';
            ELSE
                v.cfg.trp := '0';
            END IF;
            v.dqm := ( OTHERS => '1' );
            v.sdwen := '1';
            v.rasn := '1';
            v.casn := '1';
            v.hready := '0';
            v.startsd := '0';
            v.startsdold := '0';
            v.cfg.dllrst := '0';
            v.cfg.cke := '0';
        END IF;
        ri <= v;
        ribdrive <= vbdrive;
    END PROCESS;
    sdo.sdcke <= ( OTHERS => r.cfg.cke );
    ahbso.hconfig <= ( 0 => AHB_DEVICE_REG ( VENDOR_GAISLER , GAISLER_DDRSP , 0 , 0 , 0 ) , 4 => AHB_MEMBAR ( 1024 , '1' , '1' , 3072 ) , 5 => AHB_IOBAR ( 1 , 4095 ) , OTHERS => ZERO32 );
    ahbso.hirq <= ( OTHERS => '0' );
    ahbso.hindex <= 3;
    ahbregs : PROCESS ( clk_ahb )
    BEGIN
        IF rising_edge ( clk_ahb ) THEN
            ra <= rai;
        END IF;
    END PROCESS;
    ddrregs : PROCESS ( clk_ddr , rst )
    BEGIN
        IF rising_edge ( clk_ddr ) THEN
            r <= ri;
            rbdrive <= ribdrive;
        END IF;
        IF ( rst = '0' ) THEN
            r.sdcsn <= ( OTHERS => '1' );
            r.bdrive <= '1';
            r.nbdrive <= '0';
            IF 0 = 0 THEN
                rbdrive <= ( OTHERS => '1' );
            ELSE
                rbdrive <= ( OTHERS => '0' );
            END IF;
            r.cfg.cke <= '0';
        END IF;
    END PROCESS;
    sdo.address <= '0' & ri.address;
    sdo.ba <= ri.ba;
    sdo.bdrive <= r.nbdrive WHEN 0 = 1 ELSE r.bdrive;
    sdo.qdrive <= not ( ri.qdrive or r.nbdrive );
    sdo.vbdrive <= rbdrive;
    sdo.sdcsn <= ri.sdcsn;
    sdo.sdwen <= ri.sdwen;
    sdo.dqm <= r.dqm;
    sdo.rasn <= ri.rasn;
    sdo.casn <= ri.casn;
    sdo.data <= wdata;
    read_buff : COMPONENT syncram_2p
        GENERIC MAP (
            tech => 0 , abits => 4 , dbits => 128 , sepclk => 1 , wrfst => 0
        ) PORT MAP (
            rclk => clk_ahb , renable => vcc , raddress => rai.raddr ( 5 downto 2 ) , dataout => rdata , wclk => clk_ddr , write => ri.hready , waddress => r.waddr ( 5 downto 2 ) , datain => ri.hrdata
        )
    ;
    write_buff1 : COMPONENT syncram_2p
        GENERIC MAP (
            tech => 0 , abits => 4 , dbits => 32 , sepclk => 1 , wrfst => 0
        ) PORT MAP (
            rclk => clk_ddr , renable => vcc , raddress => r.waddr ( 5 downto 2 ) , dataout => wdata ( 127 downto 96 ) , wclk => clk_ahb , write => ra.write ( 0 ) , waddress => ra.haddr ( 7 downto 4 ) , datain => ahbsi.hwdata
        )
    ;
    write_buff2 : COMPONENT syncram_2p
        GENERIC MAP (
            tech => 0 , abits => 4 , dbits => 32 , sepclk => 1 , wrfst => 0
        ) PORT MAP (
            rclk => clk_ddr , renable => vcc , raddress => r.waddr ( 5 downto 2 ) , dataout => wdata ( 95 downto 64 ) , wclk => clk_ahb , write => ra.write ( 1 ) , waddress => ra.haddr ( 7 downto 4 ) , datain => ahbsi.hwdata
        )
    ;
    write_buff3 : COMPONENT syncram_2p
        GENERIC MAP (
            tech => 0 , abits => 4 , dbits => 32 , sepclk => 1 , wrfst => 0
        ) PORT MAP (
            rclk => clk_ddr , renable => vcc , raddress => r.waddr ( 5 downto 2 ) , dataout => wdata ( 63 downto 32 ) , wclk => clk_ahb , write => ra.write ( 2 ) , waddress => ra.haddr ( 7 downto 4 ) , datain => ahbsi.hwdata
        )
    ;
    write_buff4 : COMPONENT syncram_2p
        GENERIC MAP (
            tech => 0 , abits => 4 , dbits => 32 , sepclk => 1 , wrfst => 0
        ) PORT MAP (
            rclk => clk_ddr , renable => vcc , raddress => r.waddr ( 5 downto 2 ) , dataout => wdata ( 31 downto 0 ) , wclk => clk_ahb , write => ra.write ( 3 ) , waddress => ra.haddr ( 7 downto 4 ) , datain => ahbsi.hwdata
        )
    ;
    bootmsg : COMPONENT report_version
        GENERIC MAP (
            msg1 => "ddrsp" & tost ( 3 ) & ": 64-bit DDR266 controller rev " & tost ( 0 ) & ", " & tost ( 256 ) & " Mbyte, " & tost ( 90 ) & " MHz DDR clock"
        )
    ;
END ARCHITECTURE;
