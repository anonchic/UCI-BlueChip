LIBRARY ieee;
    USE ieee.std_logic_1164.all;
    USE ieee.numeric_std.all;
LIBRARY grlib;
    USE grlib.sparc.all;
    USE grlib.stdlib.all;
LIBRARY techmap;
    USE techmap.gencomp.all;
LIBRARY gaisler;
    USE gaisler.leon3.all;
    USE gaisler.libiu.all;
    USE gaisler.arith.all;
-- pragma translate_off
    use grlib.sparc_disas.all;
-- pragma translate_on


ENTITY iu3 IS
    GENERIC (
        nwin : integer RANGE 2 to 32 := 8;
        isets : integer RANGE 1 to 4 := 2;
        dsets : integer RANGE 1 to 4 := 2;
        fpu : integer RANGE 0 to 15 := 0;
        v8 : integer RANGE 0 to 63 := 2;
        cp : integer RANGE 0 to 1 := 0;
        mac : integer RANGE 0 to 1 := 0;
        dsu : integer RANGE 0 to 1 := 1;
        nwp : integer RANGE 0 to 4 := 2;
        pclow : integer RANGE 0 to 2 := 2;
        notag : integer RANGE 0 to 1 := 0;
        index : integer RANGE 0 to 15 := 0;
        lddel : integer RANGE 1 to 2 := 1;
        irfwt : integer RANGE 0 to 1 := 1;
        disas : integer RANGE 0 to 2 := 0;
        tbuf : integer RANGE 0 to 64 := 2;
        pwd : integer RANGE 0 to 2 := 0;
        svt : integer RANGE 0 to 1 := 1;
        rstaddr : integer := 16#00000#;
        smp : integer RANGE 0 to 15 := 0;
        fabtech : integer RANGE 0 to NTECH := 2;
        clk2x : integer := 0
    );
    PORT (
        clk : in std_ulogic;
        rstn : in std_ulogic;
        holdn : in std_ulogic;
        ici : out icache_in_type;
        ico : in icache_out_type;
        dci : out dcache_in_type;
        dco : in dcache_out_type;
        rfi : out iregfile_in_type;
        rfo : in iregfile_out_type;
        irqi : in l3_irq_in_type;
        irqo : out l3_irq_out_type;
        dbgi : in l3_debug_in_type;
        dbgo : out l3_debug_out_type;
        muli : out mul32_in_type;
        mulo : in mul32_out_type;
        divi : out div32_in_type;
        divo : in div32_out_type;
        fpo : in fpc_out_type;
        fpi : out fpc_in_type;
        cpo : in fpc_out_type;
        cpi : out fpc_in_type;
        tbo : in tracebuf_out_type;
        tbi : out tracebuf_in_type;
        sclk : in std_ulogic
    );
END ENTITY;

ARCHITECTURE rtl OF iu3 IS
    CONSTANT ISETMSB : integer := log2x ( 2 ) - 1;
    CONSTANT DSETMSB : integer := log2x ( 2 ) - 1;
    CONSTANT RFBITS : integer RANGE 6 to 10 := log2 ( 8 + 1 ) + 4;
    CONSTANT NWINLOG2 : integer RANGE 1 to 5 := log2 ( 8 );
    CONSTANT CWPOPT : boolean := ( 8 = ( 2 ** LOG2 ( 8 ) ) );
    CONSTANT CWPMIN : std_logic_vector ( LOG2 ( 8 ) - 1 downto 0 ) := ( OTHERS => '0' );
    CONSTANT CWPMAX : std_logic_vector ( LOG2 ( 8 ) - 1 downto 0 ) := conv_std_logic_vector ( 8 - 1 , LOG2 ( 8 ) );
    CONSTANT FPEN : boolean := ( 0 /= 0 );
    CONSTANT CPEN : boolean := ( 0 = 1 );
    CONSTANT MULEN : boolean := ( 2 /= 0 );
    CONSTANT MULTYPE : integer := ( 2 / 16 );
    CONSTANT DIVEN : boolean := ( 2 /= 0 );
    CONSTANT MACEN : boolean := ( 0 = 1 );
    CONSTANT MACPIPE : boolean := ( 0 = 1 ) and ( 2 / 2 = 1 );
    CONSTANT IMPL : integer := 15;
    CONSTANT VER : integer := 3;
    CONSTANT DBGUNIT : boolean := ( 1 = 1 );
    CONSTANT TRACEBUF : boolean := ( 2 /= 0 );
    CONSTANT TBUFBITS : integer := 10 + log2 ( 2 ) - 4;
    CONSTANT PWRD1 : boolean := false;
    CONSTANT PWRD2 : boolean := 0 /= 0;
    CONSTANT RS1OPT : boolean := ( is_fpga ( 2 ) /= 0 );
    SUBTYPE word IS std_logic_vector ( 31 downto 0 );
    SUBTYPE pctype IS std_logic_vector ( 31 downto 2 );
    SUBTYPE rfatype IS std_logic_vector ( LOG2 ( 8 + 1 ) + 4 - 1 downto 0 );
    SUBTYPE cwptype IS std_logic_vector ( LOG2 ( 8 ) - 1 downto 0 );
    TYPE icdtype IS ARRAY ( 0 to 2 - 1 ) OF word;
    TYPE dcdtype IS ARRAY ( 0 to 2 - 1 ) OF word;
    TYPE dc_in_type IS RECORD
        signed : std_ulogic;
        enaddr : std_ulogic;
        read : std_ulogic;
        write : std_ulogic;
        lock : std_ulogic;
        dsuen : std_ulogic;
        size : std_logic_vector ( 1 downto 0 );
        asi : std_logic_vector ( 7 downto 0 );
    END RECORD;
    TYPE pipeline_ctrl_type IS RECORD
        pc : pctype;
        inst : word;
        cnt : std_logic_vector ( 1 downto 0 );
        rd : rfatype;
        tt : std_logic_vector ( 5 downto 0 );
        trap : std_ulogic;
        annul : std_ulogic;
        wreg : std_ulogic;
        wicc : std_ulogic;
        wy : std_ulogic;
        ld : std_ulogic;
        pv : std_ulogic;
        rett : std_ulogic;
    END RECORD;
    TYPE fetch_reg_type IS RECORD
        pc : pctype;
        branch : std_ulogic;
    END RECORD;
    TYPE decode_reg_type IS RECORD
        pc : pctype;
        inst : icdtype;
        cwp : cwptype;
        set : std_logic_vector ( LOG2X ( 2 ) - 1 downto 0 );
        mexc : std_ulogic;
        cnt : std_logic_vector ( 1 downto 0 );
        pv : std_ulogic;
        annul : std_ulogic;
        inull : std_ulogic;
        step : std_ulogic;
    END RECORD;
    TYPE regacc_reg_type IS RECORD
        ctrl : pipeline_ctrl_type;
        rs1 : std_logic_vector ( 4 downto 0 );
        rfa1 : rfatype;
        rfa2 : rfatype;
        rsel1 : std_logic_vector ( 2 downto 0 );
        rsel2 : std_logic_vector ( 2 downto 0 );
        rfe1 : std_ulogic;
        rfe2 : std_ulogic;
        cwp : cwptype;
        imm : word;
        ldcheck1 : std_ulogic;
        ldcheck2 : std_ulogic;
        ldchkra : std_ulogic;
        ldchkex : std_ulogic;
        su : std_ulogic;
        et : std_ulogic;
        wovf : std_ulogic;
        wunf : std_ulogic;
        ticc : std_ulogic;
        jmpl : std_ulogic;
        step : std_ulogic;
        mulstart : std_ulogic;
        divstart : std_ulogic;
    END RECORD;
    TYPE execute_reg_type IS RECORD
        ctrl : pipeline_ctrl_type;
        op1 : word;
        op2 : word;
        aluop : std_logic_vector ( 2 downto 0 );
        alusel : std_logic_vector ( 1 downto 0 );
        aluadd : std_ulogic;
        alucin : std_ulogic;
        ldbp1 : std_ulogic;
        ldbp2 : std_ulogic;
        invop2 : std_ulogic;
        shcnt : std_logic_vector ( 4 downto 0 );
        sari : std_ulogic;
        shleft : std_ulogic;
        ymsb : std_ulogic;
        rd : std_logic_vector ( 4 downto 0 );
        jmpl : std_ulogic;
        su : std_ulogic;
        et : std_ulogic;
        cwp : cwptype;
        icc : std_logic_vector ( 3 downto 0 );
        mulstep : std_ulogic;
        mul : std_ulogic;
        mac : std_ulogic;
    END RECORD;
    TYPE memory_reg_type IS RECORD
        ctrl : pipeline_ctrl_type;
        result : word;
        y : word;
        icc : std_logic_vector ( 3 downto 0 );
        nalign : std_ulogic;
        dci : dc_in_type;
        werr : std_ulogic;
        wcwp : std_ulogic;
        irqen : std_ulogic;
        irqen2 : std_ulogic;
        mac : std_ulogic;
        divz : std_ulogic;
        su : std_ulogic;
        mul : std_ulogic;
    END RECORD;
    TYPE exception_state IS ( run , trap , dsu1 , dsu2 );
    TYPE exception_reg_type IS RECORD
        ctrl : pipeline_ctrl_type;
        result : word;
        y : word;
        icc : std_logic_vector ( 3 downto 0 );
        annul_all : std_ulogic;
        data : dcdtype;
        set : std_logic_vector ( LOG2X ( 2 ) - 1 downto 0 );
        mexc : std_ulogic;
        impwp : std_ulogic;
        dci : dc_in_type;
        laddr : std_logic_vector ( 1 downto 0 );
        rstate : exception_state;
        npc : std_logic_vector ( 2 downto 0 );
        intack : std_ulogic;
        ipend : std_ulogic;
        mac : std_ulogic;
        pwd : std_ulogic;
        debug : std_ulogic;
        error : std_ulogic;
        nerror : std_ulogic;
        et : std_ulogic;
    END RECORD;
    TYPE dsu_registers IS RECORD
        tt : std_logic_vector ( 7 downto 0 );
        err : std_ulogic;
        tbufcnt : std_logic_vector ( 10 + LOG2 ( 2 ) - 4 - 1 downto 0 );
        asi : std_logic_vector ( 7 downto 0 );
        crdy : std_logic_vector ( 2 downto 1 );
    END RECORD;
    TYPE irestart_register IS RECORD
        addr : pctype;
        pwd : std_ulogic;
    END RECORD;
    TYPE pwd_register_type IS RECORD
        pwd : std_ulogic;
        error : std_ulogic;
    END RECORD;
    TYPE special_register_type IS RECORD
        cwp : cwptype;
        icc : std_logic_vector ( 3 downto 0 );
        tt : std_logic_vector ( 7 downto 0 );
        tba : std_logic_vector ( 19 downto 0 );
        wim : std_logic_vector ( 8 - 1 downto 0 );
        pil : std_logic_vector ( 3 downto 0 );
        ec : std_ulogic;
        ef : std_ulogic;
        ps : std_ulogic;
        s : std_ulogic;
        et : std_ulogic;
        y : word;
        asr18 : word;
        svt : std_ulogic;
        dwt : std_ulogic;
    END RECORD;
    TYPE write_reg_type IS RECORD
        s : special_register_type;
        result : word;
        wa : rfatype;
        wreg : std_ulogic;
        except : std_ulogic;
    END RECORD;
    TYPE registers IS RECORD
        f : fetch_reg_type;
        d : decode_reg_type;
        a : regacc_reg_type;
        e : execute_reg_type;
        m : memory_reg_type;
        x : exception_reg_type;
        w : write_reg_type;
    END RECORD;
    TYPE exception_type IS RECORD
        pri : std_ulogic;
        ill : std_ulogic;
        fpdis : std_ulogic;
        cpdis : std_ulogic;
        wovf : std_ulogic;
        wunf : std_ulogic;
        ticc : std_ulogic;
    END RECORD;
    TYPE watchpoint_register IS RECORD
        addr : std_logic_vector ( 31 downto 2 );
        mask : std_logic_vector ( 31 downto 2 );
        exec : std_ulogic;
        imp : std_ulogic;
        load : std_ulogic;
        store : std_ulogic;
    END RECORD;
    TYPE watchpoint_registers IS ARRAY ( 0 to 3 ) OF watchpoint_register;
    CONSTANT wpr_none : watchpoint_register := ( zero32 ( 31 downto 2 ) , zero32 ( 31 downto 2 ) , '0' , '0' , '0' , '0' );
    FUNCTION dbgexc (
        r : registers;
        dbgi : l3_debug_in_type;
        trap : std_ulogic;
        tt : std_logic_vector ( 7 downto 0 )
    ) RETURN std_ulogic IS
        VARIABLE dmode : std_ulogic;
    BEGIN
        dmode := '0';
        IF ( not r.x.ctrl.annul and trap ) = '1' THEN
            IF ( ( ( tt = "00" & TT_WATCH ) and ( dbgi.bwatch = '1' ) ) or ( ( dbgi.bsoft = '1' ) and ( tt = "10000001" ) ) or ( dbgi.btrapa = '1' ) or ( ( dbgi.btrape = '1' ) and not ( ( tt ( 5 downto 0 ) = TT_PRIV ) or ( tt ( 5 downto 0 ) = TT_FPDIS ) or ( tt ( 5 downto 0 ) = TT_WINOF ) or ( tt ( 5 downto 0 ) = TT_WINUF ) or ( tt ( 5 downto 4 ) = "01" ) or ( tt ( 7 ) = '1' ) ) ) or ( ( ( not r.w.s.et ) and dbgi.berror ) = '1' ) ) THEN
                dmode := '1';
            END IF;
        END IF;
        RETURN ( dmode );
    END;
    FUNCTION dbgerr (
        r : registers;
        dbgi : l3_debug_in_type;
        tt : std_logic_vector ( 7 downto 0 )
    ) RETURN std_ulogic IS
        VARIABLE err : std_ulogic;
    BEGIN
        err := not r.w.s.et;
        IF ( ( ( dbgi.dbreak = '1' ) and ( tt = ( "00" & TT_WATCH ) ) ) or ( ( dbgi.bsoft = '1' ) and ( tt = ( "10000001" ) ) ) ) THEN
            err := '0';
        END IF;
        RETURN ( err );
    END;
    PROCEDURE diagwr (
        r : in registers;
        dsur : in dsu_registers;
        ir : in irestart_register;
        dbg : in l3_debug_in_type;
        wpr : in watchpoint_registers;
        s : out special_register_type;
        vwpr : out watchpoint_registers;
        asi : out std_logic_vector ( 7 downto 0 );
        pc : out pctype;
        npc : out pctype;
        tbufcnt : out std_logic_vector ( 10 + LOG2 ( 2 ) - 4 - 1 downto 0 );
        wr : out std_ulogic;
        addr : out std_logic_vector ( 9 downto 0 );
        data : out word;
        fpcwr : out std_ulogic
    ) IS
        VARIABLE i : integer RANGE 0 to 3;
    BEGIN
        s := r.w.s;
        pc := r.f.pc;
        npc := ir.addr;
        wr := '0';
        vwpr := wpr;
        asi := dsur.asi;
        addr := ( OTHERS => '0' );
        data := dbg.ddata;
        tbufcnt := dsur.tbufcnt;
        fpcwr := '0';
        IF ( dbg.dsuen and dbg.denable and dbg.dwrite ) = '1' THEN
            CASE dbg.daddr ( 23 downto 20 ) IS
                WHEN "0001" =>
                    IF dbg.daddr ( 16 ) = '1' THEN
                        tbufcnt := dbg.ddata ( 10 + LOG2 ( 2 ) - 4 - 1 downto 0 );
                    END IF;
                WHEN "0011" =>
                    IF dbg.daddr ( 12 ) = '0' THEN
                        wr := '1';
                        addr := ( OTHERS => '0' );
                        addr ( LOG2 ( 8 + 1 ) + 4 - 1 downto 0 ) := dbg.daddr ( LOG2 ( 8 + 1 ) + 4 + 1 downto 2 );
                    ELSE
                        fpcwr := '1';
                    END IF;
                WHEN "0100" =>
                    CASE dbg.daddr ( 7 downto 6 ) IS
                        WHEN "00" =>
                            CASE dbg.daddr ( 5 downto 2 ) IS
                                WHEN "0000" =>
                                    s.y := dbg.ddata;
                                WHEN "0001" =>
                                    s.cwp := dbg.ddata ( LOG2 ( 8 ) - 1 downto 0 );
                                    s.icc := dbg.ddata ( 23 downto 20 );
                                    s.ec := dbg.ddata ( 13 );
                                    s.pil := dbg.ddata ( 11 downto 8 );
                                    s.s := dbg.ddata ( 7 );
                                    s.ps := dbg.ddata ( 6 );
                                    s.et := dbg.ddata ( 5 );
                                WHEN "0010" =>
                                    s.wim := dbg.ddata ( 8 - 1 downto 0 );
                                WHEN "0011" =>
                                    s.tba := dbg.ddata ( 31 downto 12 );
                                    s.tt := dbg.ddata ( 11 downto 4 );
                                WHEN "0100" =>
                                    pc := dbg.ddata ( 31 downto 2 );
                                WHEN "0101" =>
                                    npc := dbg.ddata ( 31 downto 2 );
                                WHEN "0110" =>
                                    fpcwr := '1';
                                WHEN "0111" =>
                                    NULL;
                                WHEN "1001" =>
                                    asi := dbg.ddata ( 7 downto 0 );
                                WHEN OTHERS =>
                                    NULL;
                            END CASE;
                        WHEN "01" =>
                            CASE dbg.daddr ( 5 downto 2 ) IS
                                WHEN "0001" =>
                                    s.dwt := dbg.ddata ( 14 );
                                    s.svt := dbg.ddata ( 13 );
                                WHEN "0010" =>
                                    NULL;
                                WHEN "1000" =>
                                    vwpr ( 0 ).addr := dbg.ddata ( 31 downto 2 );
                                    vwpr ( 0 ).imp := dbg.ddata ( 1 );
                                    vwpr ( 0 ).exec := dbg.ddata ( 0 );
                                WHEN "1001" =>
                                    vwpr ( 0 ).mask := dbg.ddata ( 31 downto 2 );
                                    vwpr ( 0 ).load := dbg.ddata ( 1 );
                                    vwpr ( 0 ).store := dbg.ddata ( 0 );
                                WHEN "1010" =>
                                    vwpr ( 1 ).addr := dbg.ddata ( 31 downto 2 );
                                    vwpr ( 1 ).imp := dbg.ddata ( 1 );
                                    vwpr ( 1 ).exec := dbg.ddata ( 0 );
                                WHEN "1011" =>
                                    vwpr ( 1 ).mask := dbg.ddata ( 31 downto 2 );
                                    vwpr ( 1 ).load := dbg.ddata ( 1 );
                                    vwpr ( 1 ).store := dbg.ddata ( 0 );
                                WHEN "1100" =>
                                    vwpr ( 2 ).addr := dbg.ddata ( 31 downto 2 );
                                    vwpr ( 2 ).imp := dbg.ddata ( 1 );
                                    vwpr ( 2 ).exec := dbg.ddata ( 0 );
                                WHEN "1101" =>
                                    vwpr ( 2 ).mask := dbg.ddata ( 31 downto 2 );
                                    vwpr ( 2 ).load := dbg.ddata ( 1 );
                                    vwpr ( 2 ).store := dbg.ddata ( 0 );
                                WHEN "1110" =>
                                    vwpr ( 3 ).addr := dbg.ddata ( 31 downto 2 );
                                    vwpr ( 3 ).imp := dbg.ddata ( 1 );
                                    vwpr ( 3 ).exec := dbg.ddata ( 0 );
                                WHEN "1111" =>
                                    vwpr ( 3 ).mask := dbg.ddata ( 31 downto 2 );
                                    vwpr ( 3 ).load := dbg.ddata ( 1 );
                                    vwpr ( 3 ).store := dbg.ddata ( 0 );
                                WHEN OTHERS =>
                                    NULL;
                            END CASE;
                        WHEN OTHERS =>
                            NULL;
                    END CASE;
                WHEN OTHERS =>
                    NULL;
            END CASE;
        END IF;
    END;
    FUNCTION asr17_gen (
        r : in registers
    ) RETURN word IS
        VARIABLE asr17 : word;
        VARIABLE fpu2 : integer RANGE 0 to 3;
    BEGIN
        asr17 := zero32;
        asr17 ( 31 downto 28 ) := conv_std_logic_vector ( 0 , 4 );
        asr17 ( 14 ) := r.w.s.dwt;
        asr17 ( 13 ) := r.w.s.svt;
        fpu2 := 0;
        asr17 ( 11 downto 10 ) := conv_std_logic_vector ( fpu2 , 2 );
        asr17 ( 8 ) := '1';
        asr17 ( 7 downto 5 ) := conv_std_logic_vector ( 2 , 3 );
        asr17 ( 4 downto 0 ) := conv_std_logic_vector ( 8 - 1 , 5 );
        RETURN ( asr17 );
    END;
    PROCEDURE diagread (
        dbgi : in l3_debug_in_type;
        r : in registers;
        dsur : in dsu_registers;
        ir : in irestart_register;
        wpr : in watchpoint_registers;
        rfdata : in std_logic_vector ( 31 downto 0 );
        dco : in dcache_out_type;
        tbufo : in tracebuf_out_type;
        data : out word
    ) IS
        VARIABLE cwp : std_logic_vector ( 4 downto 0 );
        VARIABLE rd : std_logic_vector ( 4 downto 0 );
        VARIABLE i : integer RANGE 0 to 3;
    BEGIN
        data := ( OTHERS => '0' );
        cwp := ( OTHERS => '0' );
        cwp ( LOG2 ( 8 ) - 1 downto 0 ) := r.w.s.cwp;
        CASE dbgi.daddr ( 22 downto 20 ) IS
            WHEN "001" =>
                IF dbgi.daddr ( 16 ) = '1' THEN
                    data ( 10 + LOG2 ( 2 ) - 4 - 1 downto 0 ) := dsur.tbufcnt;
                ELSE
                    CASE dbgi.daddr ( 3 downto 2 ) IS
                        WHEN "00" =>
                            data := tbufo.data ( 127 downto 96 );
                        WHEN "01" =>
                            data := tbufo.data ( 95 downto 64 );
                        WHEN "10" =>
                            data := tbufo.data ( 63 downto 32 );
                        WHEN OTHERS =>
                            data := tbufo.data ( 31 downto 0 );
                    END CASE;
                END IF;
            WHEN "011" =>
                IF dbgi.daddr ( 12 ) = '0' THEN
                    data := rfdata ( 31 downto 0 );
                ELSE
                    data := fpo.dbg.data;
                END IF;
            WHEN "100" =>
                CASE dbgi.daddr ( 7 downto 6 ) IS
                    WHEN "00" =>
                        CASE dbgi.daddr ( 5 downto 2 ) IS
                            WHEN "0000" =>
                                data := r.w.s.y;
                            WHEN "0001" =>
                                data := conv_std_logic_vector ( 15 , 4 ) & conv_std_logic_vector ( 3 , 4 ) & r.w.s.icc & "000000" & r.w.s.ec & r.w.s.ef & r.w.s.pil & r.w.s.s & r.w.s.ps & r.w.s.et & cwp;
                            WHEN "0010" =>
                                data ( 8 - 1 downto 0 ) := r.w.s.wim;
                            WHEN "0011" =>
                                data := r.w.s.tba & r.w.s.tt & "0000";
                            WHEN "0100" =>
                                data ( 31 downto 2 ) := r.f.pc;
                            WHEN "0101" =>
                                data ( 31 downto 2 ) := ir.addr;
                            WHEN "0110" =>
                                data := fpo.dbg.data;
                            WHEN "0111" =>
                                NULL;
                            WHEN "1000" =>
                                data ( 12 downto 4 ) := dsur.err & dsur.tt;
                            WHEN "1001" =>
                                data ( 7 downto 0 ) := dsur.asi;
                            WHEN OTHERS =>
                                NULL;
                        END CASE;
                    WHEN "01" =>
                        IF dbgi.daddr ( 5 ) = '0' THEN
                            IF dbgi.daddr ( 4 downto 2 ) = "001" THEN
                                data := asr17_gen ( r );
                            END IF;
                        ELSE
                            i := conv_integer ( dbgi.daddr ( 4 downto 3 ) );
                            IF dbgi.daddr ( 2 ) = '0' THEN
                                data ( 31 downto 2 ) := wpr ( i ).addr;
                                data ( 1 ) := wpr ( i ).imp;
                                data ( 0 ) := wpr ( i ).exec;
                            ELSE
                                data ( 31 downto 2 ) := wpr ( i ).mask;
                                data ( 1 ) := wpr ( i ).load;
                                data ( 0 ) := wpr ( i ).store;
                            END IF;
                        END IF;
                    WHEN OTHERS =>
                        NULL;
                END CASE;
            WHEN "111" =>
                data := r.x.data ( conv_integer ( r.x.set ) );
            WHEN OTHERS =>
                NULL;
        END CASE;
    END;
    PROCEDURE itrace (
        r : in registers;
        dsur : in dsu_registers;
        vdsu : in dsu_registers;
        res : in word;
        exc : in std_ulogic;
        dbgi : in l3_debug_in_type;
        error : in std_ulogic;
        trap : in std_ulogic;
        tbufcnt : out std_logic_vector ( 10 + LOG2 ( 2 ) - 4 - 1 downto 0 );
        di : out tracebuf_in_type
    ) IS
        VARIABLE meminst : std_ulogic;
    BEGIN
        di.addr := ( OTHERS => '0' );
        di.data := ( OTHERS => '0' );
        di.enable := '0';
        di.write := ( OTHERS => '0' );
        tbufcnt := vdsu.tbufcnt;
        meminst := r.x.ctrl.inst ( 31 ) and r.x.ctrl.inst ( 30 );
        di.addr ( 10 + LOG2 ( 2 ) - 4 - 1 downto 0 ) := dsur.tbufcnt;
        di.data ( 127 ) := '0';
        di.data ( 126 ) := not r.x.ctrl.pv;
        di.data ( 125 downto 96 ) := dbgi.timer ( 29 downto 0 );
        di.data ( 95 downto 64 ) := res;
        di.data ( 63 downto 34 ) := r.x.ctrl.pc ( 31 downto 2 );
        di.data ( 33 ) := trap;
        di.data ( 32 ) := error;
        di.data ( 31 downto 0 ) := r.x.ctrl.inst;
        IF ( dbgi.tenable = '0' ) or ( r.x.rstate = dsu2 ) THEN
            IF ( ( dbgi.dsuen and dbgi.denable ) = '1' ) and ( dbgi.daddr ( 23 downto 20 ) & dbgi.daddr ( 16 ) = "00010" ) THEN
                di.enable := '1';
                di.addr ( 10 + LOG2 ( 2 ) - 4 - 1 downto 0 ) := dbgi.daddr ( 10 + LOG2 ( 2 ) - 4 - 1 + 4 downto 4 );
                IF dbgi.dwrite = '1' THEN
                    CASE dbgi.daddr ( 3 downto 2 ) IS
                        WHEN "00" =>
                            di.write ( 3 ) := '1';
                        WHEN "01" =>
                            di.write ( 2 ) := '1';
                        WHEN "10" =>
                            di.write ( 1 ) := '1';
                        WHEN OTHERS =>
                            di.write ( 0 ) := '1';
                    END CASE;
                    di.data := dbgi.ddata & dbgi.ddata & dbgi.ddata & dbgi.ddata;
                END IF;
            END IF;
        ELSIF ( not r.x.ctrl.annul and ( r.x.ctrl.pv or meminst ) and not r.x.debug ) = '1' THEN
            di.enable := '1';
            di.write := ( OTHERS => '1' );
            tbufcnt := dsur.tbufcnt + 1;
        END IF;
        di.diag := dco.testen & "000";
        IF dco.scanen = '1' THEN
            di.enable := '0';
        END IF;
    END;
    PROCEDURE dbg_cache (
        holdn : in std_ulogic;
        dbgi : in l3_debug_in_type;
        r : in registers;
        dsur : in dsu_registers;
        mresult : in word;
        dci : in dc_in_type;
        mresult2 : out word;
        dci2 : out dc_in_type
    ) IS
    BEGIN
        mresult2 := mresult;
        dci2 := dci;
        dci2.dsuen := '0';
        IF r.x.rstate = dsu2 THEN
            dci2.asi := dsur.asi;
            IF ( dbgi.daddr ( 22 downto 20 ) = "111" ) and ( dbgi.dsuen = '1' ) THEN
                dci2.dsuen := ( dbgi.denable or r.m.dci.dsuen ) and not dsur.crdy ( 2 );
                dci2.enaddr := dbgi.denable;
                dci2.size := "10";
                dci2.read := '1';
                dci2.write := '0';
                IF ( dbgi.denable and not r.m.dci.enaddr ) = '1' THEN
                    mresult2 := ( OTHERS => '0' );
                    mresult2 ( 19 downto 2 ) := dbgi.daddr ( 19 downto 2 );
                ELSE
                    mresult2 := dbgi.ddata;
                END IF;
                IF dbgi.dwrite = '1' THEN
                    dci2.read := '0';
                    dci2.write := '1';
                END IF;
            END IF;
        END IF;
    END;
    PROCEDURE fpexack (
        r : in registers;
        fpexc : out std_ulogic
    ) IS
    BEGIN
        fpexc := '0';
    END;
    PROCEDURE diagrdy (
        denable : in std_ulogic;
        dsur : in dsu_registers;
        dci : in dc_in_type;
        mds : in std_ulogic;
        ico : in icache_out_type;
        crdy : out std_logic_vector ( 2 downto 1 )
    ) IS
    BEGIN
        crdy := dsur.crdy ( 1 ) & '0';
        IF dci.dsuen = '1' THEN
            CASE dsur.asi ( 4 downto 0 ) IS
                WHEN ASI_ITAG | ASI_IDATA | ASI_UINST | ASI_SINST =>
                    crdy ( 2 ) := ico.diagrdy and not dsur.crdy ( 2 );
                WHEN ASI_DTAG | ASI_MMUSNOOP_DTAG | ASI_DDATA | ASI_UDATA | ASI_SDATA =>
                    crdy ( 1 ) := not denable and dci.enaddr and not dsur.crdy ( 1 );
                WHEN OTHERS =>
                    crdy ( 2 ) := dci.enaddr and denable;
            END CASE;
        END IF;
    END;
    SIGNAL r : registers;
    SIGNAL rin : registers;
    SIGNAL wpr : watchpoint_registers;
    SIGNAL wprin : watchpoint_registers;
    SIGNAL dsur : dsu_registers;
    SIGNAL dsuin : dsu_registers;
    SIGNAL ir : irestart_register;
    SIGNAL irin : irestart_register;
    SIGNAL rp : pwd_register_type;
    SIGNAL rpin : pwd_register_type;
    CONSTANT EXE_AND : std_logic_vector ( 2 downto 0 ) := "000";
    CONSTANT EXE_XOR : std_logic_vector ( 2 downto 0 ) := "001";
    CONSTANT EXE_OR : std_logic_vector ( 2 downto 0 ) := "010";
    CONSTANT EXE_XNOR : std_logic_vector ( 2 downto 0 ) := "011";
    CONSTANT EXE_ANDN : std_logic_vector ( 2 downto 0 ) := "100";
    CONSTANT EXE_ORN : std_logic_vector ( 2 downto 0 ) := "101";
    CONSTANT EXE_DIV : std_logic_vector ( 2 downto 0 ) := "110";
    CONSTANT EXE_PASS1 : std_logic_vector ( 2 downto 0 ) := "000";
    CONSTANT EXE_PASS2 : std_logic_vector ( 2 downto 0 ) := "001";
    CONSTANT EXE_STB : std_logic_vector ( 2 downto 0 ) := "010";
    CONSTANT EXE_STH : std_logic_vector ( 2 downto 0 ) := "011";
    CONSTANT EXE_ONES : std_logic_vector ( 2 downto 0 ) := "100";
    CONSTANT EXE_RDY : std_logic_vector ( 2 downto 0 ) := "101";
    CONSTANT EXE_SPR : std_logic_vector ( 2 downto 0 ) := "110";
    CONSTANT EXE_LINK : std_logic_vector ( 2 downto 0 ) := "111";
    CONSTANT EXE_SLL : std_logic_vector ( 2 downto 0 ) := "001";
    CONSTANT EXE_SRL : std_logic_vector ( 2 downto 0 ) := "010";
    CONSTANT EXE_SRA : std_logic_vector ( 2 downto 0 ) := "100";
    CONSTANT EXE_NOP : std_logic_vector ( 2 downto 0 ) := "000";
    CONSTANT EXE_RES_ADD : std_logic_vector ( 1 downto 0 ) := "00";
    CONSTANT EXE_RES_SHIFT : std_logic_vector ( 1 downto 0 ) := "01";
    CONSTANT EXE_RES_LOGIC : std_logic_vector ( 1 downto 0 ) := "10";
    CONSTANT EXE_RES_MISC : std_logic_vector ( 1 downto 0 ) := "11";
    CONSTANT SZBYTE : std_logic_vector ( 1 downto 0 ) := "00";
    CONSTANT SZHALF : std_logic_vector ( 1 downto 0 ) := "01";
    CONSTANT SZWORD : std_logic_vector ( 1 downto 0 ) := "10";
    CONSTANT SZDBL : std_logic_vector ( 1 downto 0 ) := "11";
    PROCEDURE regaddr (
        cwp : std_logic_vector;
        reg : std_logic_vector ( 4 downto 0 );
        rao : out rfatype
    ) IS
        VARIABLE ra : rfatype;
        CONSTANT globals : std_logic_vector ( LOG2 ( 8 + 1 ) + 4 - 5 downto 0 ) := conv_std_logic_vector ( 8 , LOG2 ( 8 + 1 ) + 4 - 4 );
    BEGIN
        ra := ( OTHERS => '0' );
        ra ( 4 downto 0 ) := reg;
        IF reg ( 4 downto 3 ) = "00" THEN
            ra ( LOG2 ( 8 + 1 ) + 4 - 1 downto 4 ) := CONV_STD_LOGIC_VECTOR ( 8 , LOG2 ( 8 + 1 ) + 4 - 4 );
        ELSE
            ra ( LOG2 ( 8 ) + 3 downto 4 ) := cwp + ra ( 4 );
        END IF;
        rao := ra;
    END;
    FUNCTION branch_address (
        inst : word;
        pc : pctype
    ) RETURN std_logic_vector IS
        VARIABLE baddr : pctype;
        VARIABLE caddr : pctype;
        VARIABLE tmp : pctype;
    BEGIN
        caddr := ( OTHERS => '0' );
        caddr ( 31 downto 2 ) := inst ( 29 downto 0 );
        caddr ( 31 downto 2 ) := caddr ( 31 downto 2 ) + pc ( 31 downto 2 );
        baddr := ( OTHERS => '0' );
        baddr ( 31 downto 24 ) := ( OTHERS => inst ( 21 ) );
        baddr ( 23 downto 2 ) := inst ( 21 downto 0 );
        baddr ( 31 downto 2 ) := baddr ( 31 downto 2 ) + pc ( 31 downto 2 );
        IF inst ( 30 ) = '1' THEN
            tmp := caddr;
        ELSE
            tmp := baddr;
        END IF;
        RETURN ( tmp );
    END;
    FUNCTION branch_true (
        icc : std_logic_vector ( 3 downto 0 );
        inst : word
    ) RETURN std_ulogic IS
        VARIABLE n : std_ulogic;
        VARIABLE z : std_ulogic;
        VARIABLE v : std_ulogic;
        VARIABLE c : std_ulogic;
        VARIABLE branch : std_ulogic;
    BEGIN
        n := icc ( 3 );
        z := icc ( 2 );
        v := icc ( 1 );
        c := icc ( 0 );
        CASE inst ( 27 downto 25 ) IS
            WHEN "000" =>
                branch := inst ( 28 ) xor '0';
            WHEN "001" =>
                branch := inst ( 28 ) xor z;
            WHEN "010" =>
                branch := inst ( 28 ) xor ( z or ( n xor v ) );
            WHEN "011" =>
                branch := inst ( 28 ) xor ( n xor v );
            WHEN "100" =>
                branch := inst ( 28 ) xor ( c or z );
            WHEN "101" =>
                branch := inst ( 28 ) xor c;
            WHEN "110" =>
                branch := inst ( 28 ) xor n;
            WHEN OTHERS =>
                branch := inst ( 28 ) xor v;
        END CASE;
        RETURN ( branch );
    END;
    PROCEDURE su_et_select (
        r : in registers;
        xc_ps : in std_ulogic;
        xc_s : in std_ulogic;
        xc_et : in std_ulogic;
        su : out std_ulogic;
        et : out std_ulogic
    ) IS
    BEGIN
        IF ( ( r.a.ctrl.rett or r.e.ctrl.rett or r.m.ctrl.rett or r.x.ctrl.rett ) = '1' ) and ( r.x.annul_all = '0' ) THEN
            su := xc_ps;
            et := '1';
        ELSE
            su := xc_s;
            et := xc_et;
        END IF;
    END;
    FUNCTION wphit (
        r : registers;
        wpr : watchpoint_registers;
        debug : l3_debug_in_type
    ) RETURN std_ulogic IS
        VARIABLE exc : std_ulogic;
    BEGIN
        exc := '0';
        IF ( ( wpr ( 0 ).exec and r.a.ctrl.pv and not r.a.ctrl.annul ) = '1' ) THEN
            IF ( ( ( wpr ( 0 ).addr xor r.a.ctrl.pc ( 31 downto 2 ) ) and wpr ( 0 ).mask ) = Zero32 ( 31 downto 2 ) ) THEN
                exc := '1';
            END IF;
        END IF;
        IF ( ( wpr ( 1 ).exec and r.a.ctrl.pv and not r.a.ctrl.annul ) = '1' ) THEN
            IF ( ( ( wpr ( 1 ).addr xor r.a.ctrl.pc ( 31 downto 2 ) ) and wpr ( 1 ).mask ) = Zero32 ( 31 downto 2 ) ) THEN
                exc := '1';
            END IF;
        END IF;
        IF ( debug.dsuen and not r.a.ctrl.annul ) = '1' THEN
            exc := exc or ( r.a.ctrl.pv and ( ( debug.dbreak and debug.bwatch ) or r.a.step ) );
        END IF;
        RETURN ( exc );
    END;
    FUNCTION shift3 (
        r : registers;
        aluin1 : word;
        aluin2 : word
    ) RETURN word IS
        VARIABLE shiftin : unsigned ( 63 downto 0 );
        VARIABLE shiftout : unsigned ( 63 downto 0 );
        VARIABLE cnt : natural RANGE 0 to 31;
    BEGIN
        cnt := conv_integer ( r.e.shcnt );
        IF r.e.shleft = '1' THEN
            shiftin ( 30 downto 0 ) := ( OTHERS => '0' );
            shiftin ( 63 downto 31 ) := '0' & unsigned ( aluin1 );
        ELSE
            shiftin ( 63 downto 32 ) := ( OTHERS => r.e.sari );
            shiftin ( 31 downto 0 ) := unsigned ( aluin1 );
        END IF;
        shiftout := SHIFT_RIGHT ( shiftin , cnt );
        RETURN ( std_logic_vector ( shiftout ( 31 downto 0 ) ) );
    END;
    FUNCTION shift2 (
        r : registers;
        aluin1 : word;
        aluin2 : word
    ) RETURN word IS
        VARIABLE ushiftin : unsigned ( 31 downto 0 );
        VARIABLE sshiftin : signed ( 32 downto 0 );
        VARIABLE cnt : natural RANGE 0 to 31;
    BEGIN
        cnt := conv_integer ( r.e.shcnt );
        ushiftin := unsigned ( aluin1 );
        sshiftin := signed ( '0' & aluin1 );
        IF r.e.shleft = '1' THEN
            RETURN ( std_logic_vector ( SHIFT_LEFT ( ushiftin , cnt ) ) );
        ELSE
            IF r.e.sari = '1' THEN
                sshiftin ( 32 ) := aluin1 ( 31 );
            END IF;
            sshiftin := SHIFT_RIGHT ( sshiftin , cnt );
            RETURN ( std_logic_vector ( sshiftin ( 31 downto 0 ) ) );
        END IF;
    END;
    FUNCTION shift (
        r : registers;
        aluin1 : word;
        aluin2 : word;
        shiftcnt : std_logic_vector ( 4 downto 0 );
        sari : std_ulogic
    ) RETURN word IS
        VARIABLE shiftin : std_logic_vector ( 63 downto 0 );
    BEGIN
        shiftin := zero32 & aluin1;
        IF r.e.shleft = '1' THEN
            shiftin ( 31 downto 0 ) := zero32;
            shiftin ( 63 downto 31 ) := '0' & aluin1;
        ELSE
            shiftin ( 63 downto 32 ) := ( OTHERS => sari );
        END IF;
        IF shiftcnt ( 4 ) = '1' THEN
            shiftin ( 47 downto 0 ) := shiftin ( 63 downto 16 );
        END IF;
        IF shiftcnt ( 3 ) = '1' THEN
            shiftin ( 39 downto 0 ) := shiftin ( 47 downto 8 );
        END IF;
        IF shiftcnt ( 2 ) = '1' THEN
            shiftin ( 35 downto 0 ) := shiftin ( 39 downto 4 );
        END IF;
        IF shiftcnt ( 1 ) = '1' THEN
            shiftin ( 33 downto 0 ) := shiftin ( 35 downto 2 );
        END IF;
        IF shiftcnt ( 0 ) = '1' THEN
            shiftin ( 31 downto 0 ) := shiftin ( 32 downto 1 );
        END IF;
        RETURN ( shiftin ( 31 downto 0 ) );
    END;
    PROCEDURE exception_detect (
        r : registers;
        wpr : watchpoint_registers;
        dbgi : l3_debug_in_type;
        trapin : in std_ulogic;
        ttin : in std_logic_vector ( 5 downto 0 );
        trap : out std_ulogic;
        tt : out std_logic_vector ( 5 downto 0 )
    ) IS
        VARIABLE illegal_inst : std_ulogic;
        VARIABLE privileged_inst : std_ulogic;
        VARIABLE cp_disabled : std_ulogic;
        VARIABLE fp_disabled : std_ulogic;
        VARIABLE fpop : std_ulogic;
        VARIABLE op : std_logic_vector ( 1 downto 0 );
        VARIABLE op2 : std_logic_vector ( 2 downto 0 );
        VARIABLE op3 : std_logic_vector ( 5 downto 0 );
        VARIABLE rd : std_logic_vector ( 4 downto 0 );
        VARIABLE inst : word;
        VARIABLE wph : std_ulogic;
    BEGIN
        inst := r.a.ctrl.inst;
        trap := trapin;
        tt := ttin;
        IF r.a.ctrl.annul = '0' THEN
            op := inst ( 31 downto 30 );
            op2 := inst ( 24 downto 22 );
            op3 := inst ( 24 downto 19 );
            rd := inst ( 29 downto 25 );
            illegal_inst := '0';
            privileged_inst := '0';
            cp_disabled := '0';
            fp_disabled := '0';
            fpop := '0';
            CASE op IS
                WHEN CALL =>
                    NULL;
                WHEN FMT2 =>
                    CASE op2 IS
                        WHEN SETHI | BICC =>
                            NULL;
                        WHEN FBFCC =>
                            fp_disabled := '1';
                        WHEN CBCCC =>
                            cp_disabled := '1';
                        WHEN OTHERS =>
                            illegal_inst := '1';
                    END CASE;
                WHEN FMT3 =>
                    CASE op3 IS
                        WHEN IAND | ANDCC | ANDN | ANDNCC | IOR | ORCC | ORN | ORNCC | IXOR | XORCC | IXNOR | XNORCC | ISLL | ISRL | ISRA | MULSCC | IADD | ADDX | ADDCC | ADDXCC | ISUB | SUBX | SUBCC | SUBXCC | FLUSH | JMPL | TICC | SAVE | RESTORE | RDY =>
                            NULL;
                        WHEN TADDCC | TADDCCTV | TSUBCC | TSUBCCTV =>
                            NULL;
                        WHEN UMAC | SMAC =>
                            illegal_inst := '1';
                        WHEN UMUL | SMUL | UMULCC | SMULCC =>
                            NULL;
                        WHEN UDIV | SDIV | UDIVCC | SDIVCC =>
                            NULL;
                        WHEN RETT =>
                            illegal_inst := r.a.et;
                            privileged_inst := not r.a.su;
                        WHEN RDPSR | RDTBR | RDWIM =>
                            privileged_inst := not r.a.su;
                        WHEN WRY =>
                            NULL;
                        WHEN WRPSR =>
                            privileged_inst := not r.a.su;
                        WHEN WRWIM | WRTBR =>
                            privileged_inst := not r.a.su;
                        WHEN FPOP1 | FPOP2 =>
                            fp_disabled := '1';
                            fpop := '0';
                        WHEN CPOP1 | CPOP2 =>
                            cp_disabled := '1';
                        WHEN OTHERS =>
                            illegal_inst := '1';
                    END CASE;
                WHEN OTHERS =>
                    CASE op3 IS
                        WHEN LDD | ISTD =>
                            illegal_inst := rd ( 0 );
                        WHEN LD | LDUB | LDSTUB | LDUH | LDSB | LDSH | ST | STB | STH | SWAP =>
                            NULL;
                        WHEN LDDA | STDA =>
                            illegal_inst := inst ( 13 ) or rd ( 0 );
                            privileged_inst := not r.a.su;
                        WHEN LDA | LDUBA | LDSTUBA | LDUHA | LDSBA | LDSHA | STA | STBA | STHA | SWAPA =>
                            illegal_inst := inst ( 13 );
                            privileged_inst := not r.a.su;
                        WHEN LDDF | STDF | LDF | LDFSR | STF | STFSR =>
                            fp_disabled := '1';
                        WHEN STDFQ =>
                            privileged_inst := not r.a.su;
                            fp_disabled := '1';
                        WHEN STDCQ =>
                            privileged_inst := not r.a.su;
                            cp_disabled := '1';
                        WHEN LDC | LDCSR | LDDC | STC | STCSR | STDC =>
                            cp_disabled := '1';
                        WHEN OTHERS =>
                            illegal_inst := '1';
                    END CASE;
            END CASE;
            wph := wphit ( r , wpr , dbgi );
            trap := '1';
            IF r.a.ctrl.trap = '1' THEN
                tt := TT_IAEX;
            ELSIF privileged_inst = '1' THEN
                tt := TT_PRIV;
            ELSIF illegal_inst = '1' THEN
                tt := TT_IINST;
            ELSIF fp_disabled = '1' THEN
                tt := TT_FPDIS;
            ELSIF cp_disabled = '1' THEN
                tt := TT_CPDIS;
            ELSIF wph = '1' THEN
                tt := TT_WATCH;
            ELSIF r.a.wovf = '1' THEN
                tt := TT_WINOF;
            ELSIF r.a.wunf = '1' THEN
                tt := TT_WINUF;
            ELSIF r.a.ticc = '1' THEN
                tt := TT_TICC;
            ELSE
                trap := '0';
                tt := ( OTHERS => '0' );
            END IF;
        END IF;
    END;
    PROCEDURE wicc_y_gen (
        inst : word;
        wicc : out std_ulogic;
        wy : out std_ulogic
    ) IS
    BEGIN
        wicc := '0';
        wy := '0';
        IF inst ( 31 downto 30 ) = FMT3 THEN
            CASE inst ( 24 downto 19 ) IS
                WHEN SUBCC | TSUBCC | TSUBCCTV | ADDCC | ANDCC | ORCC | XORCC | ANDNCC | ORNCC | XNORCC | TADDCC | TADDCCTV | ADDXCC | SUBXCC | WRPSR =>
                    wicc := '1';
                WHEN WRY =>
                    IF r.d.inst ( conv_integer ( r.d.set ) ) ( 29 downto 25 ) = "00000" THEN
                        wy := '1';
                    END IF;
                WHEN MULSCC =>
                    wicc := '1';
                    wy := '1';
                WHEN UMAC | SMAC =>
                    NULL;
                WHEN UMULCC | SMULCC =>
                    IF ( mulo.nready = '1' ) and ( r.d.cnt /= "00" ) THEN
                        wicc := '1';
                        wy := '1';
                    END IF;
                WHEN UMUL | SMUL =>
                    IF ( mulo.nready = '1' ) and ( r.d.cnt /= "00" ) THEN
                        wy := '1';
                    END IF;
                WHEN UDIVCC | SDIVCC =>
                    IF ( divo.nready = '1' ) and ( r.d.cnt /= "00" ) THEN
                        wicc := '1';
                    END IF;
                WHEN OTHERS =>
                    NULL;
            END CASE;
        END IF;
    END;
    PROCEDURE cwp_gen (
        r : registers;
        v : registers;
        annul : std_ulogic;
        wcwp : std_ulogic;
        ncwp : cwptype;
        cwp : out cwptype
    ) IS
    BEGIN
        IF ( r.x.rstate = trap ) or ( r.x.rstate = dsu2 ) or ( rstn = '0' ) THEN
            cwp := v.w.s.cwp;
        ELSIF ( wcwp = '1' ) and ( annul = '0' ) THEN
            cwp := ncwp;
        ELSIF r.m.wcwp = '1' THEN
            cwp := r.m.result ( LOG2 ( 8 ) - 1 downto 0 );
        ELSE
            cwp := r.d.cwp;
        END IF;
    END;
    PROCEDURE cwp_ex (
        r : in registers;
        wcwp : out std_ulogic
    ) IS
    BEGIN
        IF ( r.e.ctrl.inst ( 31 downto 30 ) = FMT3 ) and ( r.e.ctrl.inst ( 24 downto 19 ) = WRPSR ) THEN
            wcwp := not r.e.ctrl.annul;
        ELSE
            wcwp := '0';
        END IF;
    END;
    PROCEDURE cwp_ctrl (
        r : in registers;
        xc_wim : in std_logic_vector ( 8 - 1 downto 0 );
        inst : word;
        de_cwp : out cwptype;
        wovf_exc : out std_ulogic;
        wunf_exc : out std_ulogic;
        wcwp : out std_ulogic
    ) IS
        VARIABLE op : std_logic_vector ( 1 downto 0 );
        VARIABLE op3 : std_logic_vector ( 5 downto 0 );
        VARIABLE wim : word;
        VARIABLE ncwp : cwptype;
    BEGIN
        op := inst ( 31 downto 30 );
        op3 := inst ( 24 downto 19 );
        wovf_exc := '0';
        wunf_exc := '0';
        wim := ( OTHERS => '0' );
        wim ( 8 - 1 downto 0 ) := xc_wim;
        ncwp := r.d.cwp;
        wcwp := '0';
        IF ( op = FMT3 ) and ( ( op3 = RETT ) or ( op3 = RESTORE ) or ( op3 = SAVE ) ) THEN
            wcwp := '1';
            IF ( op3 = SAVE ) THEN
                ncwp := r.d.cwp - 1;
            ELSE
                ncwp := r.d.cwp + 1;
            END IF;
            IF wim ( conv_integer ( ncwp ) ) = '1' THEN
                IF op3 = SAVE THEN
                    wovf_exc := '1';
                ELSE
                    wunf_exc := '1';
                END IF;
            END IF;
        END IF;
        de_cwp := ncwp;
    END;
    PROCEDURE rs1_gen (
        r : registers;
        inst : word;
        rs1 : out std_logic_vector ( 4 downto 0 );
        rs1mod : out std_ulogic
    ) IS
        VARIABLE op : std_logic_vector ( 1 downto 0 );
        VARIABLE op3 : std_logic_vector ( 5 downto 0 );
    BEGIN
        op := inst ( 31 downto 30 );
        op3 := inst ( 24 downto 19 );
        rs1 := inst ( 18 downto 14 );
        rs1mod := '0';
        IF ( op = LDST ) THEN
            IF ( ( r.d.cnt = "01" ) and ( ( op3 ( 2 ) and not op3 ( 3 ) ) = '1' ) ) or ( r.d.cnt = "10" ) THEN
                rs1mod := '1';
                rs1 := inst ( 29 downto 25 );
            END IF;
            IF ( ( r.d.cnt = "10" ) and ( op3 ( 3 downto 0 ) = "0111" ) ) THEN
                rs1 ( 0 ) := '1';
            END IF;
        END IF;
    END;
    PROCEDURE lock_gen (
        r : registers;
        rs2 : std_logic_vector ( 4 downto 0 );
        rd : std_logic_vector ( 4 downto 0 );
        rfa1 : rfatype;
        rfa2 : rfatype;
        rfrd : rfatype;
        inst : word;
        fpc_lock : std_ulogic;
        mulinsn : std_ulogic;
        divinsn : std_ulogic;
        lldcheck1 : out std_ulogic;
        lldcheck2 : out std_ulogic;
        lldlock : out std_ulogic;
        lldchkra : out std_ulogic;
        lldchkex : out std_ulogic
    ) IS
        VARIABLE op : std_logic_vector ( 1 downto 0 );
        VARIABLE op2 : std_logic_vector ( 2 downto 0 );
        VARIABLE op3 : std_logic_vector ( 5 downto 0 );
        VARIABLE cond : std_logic_vector ( 3 downto 0 );
        VARIABLE rs1 : std_logic_vector ( 4 downto 0 );
        VARIABLE i : std_ulogic;
        VARIABLE ldcheck1 : std_ulogic;
        VARIABLE ldcheck2 : std_ulogic;
        VARIABLE ldchkra : std_ulogic;
        VARIABLE ldchkex : std_ulogic;
        VARIABLE ldcheck3 : std_ulogic;
        VARIABLE ldlock : std_ulogic;
        VARIABLE icc_check : std_ulogic;
        VARIABLE bicc_hold : std_ulogic;
        VARIABLE chkmul : std_ulogic;
        VARIABLE y_check : std_ulogic;
        VARIABLE lddlock : boolean;
    BEGIN
        op := inst ( 31 downto 30 );
        op3 := inst ( 24 downto 19 );
        op2 := inst ( 24 downto 22 );
        cond := inst ( 28 downto 25 );
        rs1 := inst ( 18 downto 14 );
        lddlock := false;
        i := inst ( 13 );
        ldcheck1 := '0';
        ldcheck2 := '0';
        ldcheck3 := '0';
        ldlock := '0';
        ldchkra := '1';
        ldchkex := '1';
        icc_check := '0';
        bicc_hold := '0';
        y_check := '0';
        IF ( r.d.annul = '0' ) THEN
            CASE op IS
                WHEN FMT2 =>
                    IF ( op2 = BICC ) and ( cond ( 2 downto 0 ) /= "000" ) THEN
                        icc_check := '1';
                    END IF;
                WHEN FMT3 =>
                    ldcheck1 := '1';
                    ldcheck2 := not i;
                    CASE op3 IS
                        WHEN TICC =>
                            IF ( cond ( 2 downto 0 ) /= "000" ) THEN
                                icc_check := '1';
                            END IF;
                        WHEN RDY =>
                            ldcheck1 := '0';
                            ldcheck2 := '0';
                        WHEN RDWIM | RDTBR =>
                            ldcheck1 := '0';
                            ldcheck2 := '0';
                        WHEN RDPSR =>
                            ldcheck1 := '0';
                            ldcheck2 := '0';
                            icc_check := '1';
                            icc_check := '1';
                        WHEN SDIV | SDIVCC | UDIV | UDIVCC =>
                            y_check := '1';
                        WHEN FPOP1 | FPOP2 =>
                            ldcheck1 := '0';
                            ldcheck2 := '0';
                        WHEN OTHERS =>
                            NULL;
                    END CASE;
                WHEN LDST =>
                    ldcheck1 := '1';
                    ldchkra := '0';
                    CASE r.d.cnt IS
                        WHEN "00" =>
                            ldcheck2 := not i;
                            ldchkra := '1';
                        WHEN "01" =>
                            ldcheck2 := not i;
                        WHEN OTHERS =>
                            ldchkex := '0';
                    END CASE;
                    IF ( op3 ( 2 downto 0 ) = "011" ) THEN
                        lddlock := true;
                    END IF;
                WHEN OTHERS =>
                    NULL;
            END CASE;
        END IF;
        chkmul := mulinsn;
        bicc_hold := bicc_hold or ( icc_check and r.m.ctrl.wicc and ( r.m.ctrl.cnt ( 0 ) or r.m.mul ) );
        bicc_hold := bicc_hold or ( y_check and ( r.a.ctrl.wy or r.e.ctrl.wy ) );
        chkmul := chkmul or divinsn;
        bicc_hold := bicc_hold or ( icc_check and ( r.a.ctrl.wicc or r.e.ctrl.wicc ) );
        IF ( ( ( r.a.ctrl.ld or chkmul ) and r.a.ctrl.wreg and ldchkra ) = '1' ) and ( ( ( ldcheck1 = '1' ) and ( r.a.ctrl.rd = rfa1 ) ) or ( ( ldcheck2 = '1' ) and ( r.a.ctrl.rd = rfa2 ) ) or ( ( ldcheck3 = '1' ) and ( r.a.ctrl.rd = rfrd ) ) ) THEN
            ldlock := '1';
        END IF;
        IF ( ( ( r.e.ctrl.ld or r.e.mac ) and r.e.ctrl.wreg and ldchkex ) = '1' ) and ( ( ( ldcheck1 = '1' ) and ( r.e.ctrl.rd = rfa1 ) ) or ( ( ldcheck2 = '1' ) and ( r.e.ctrl.rd = rfa2 ) ) ) THEN
            ldlock := '1';
        END IF;
        ldlock := ldlock or bicc_hold or fpc_lock;
        lldcheck1 := ldcheck1;
        lldcheck2 := ldcheck2;
        lldlock := ldlock;
        lldchkra := ldchkra;
        lldchkex := ldchkex;
    END;
    PROCEDURE fpbranch (
        inst : in word;
        fcc : in std_logic_vector ( 1 downto 0 );
        branch : out std_ulogic
    ) IS
        VARIABLE cond : std_logic_vector ( 3 downto 0 );
        VARIABLE fbres : std_ulogic;
    BEGIN
        cond := inst ( 28 downto 25 );
        CASE cond ( 2 downto 0 ) IS
            WHEN "000" =>
                fbres := '0';
            WHEN "001" =>
                fbres := fcc ( 1 ) or fcc ( 0 );
            WHEN "010" =>
                fbres := fcc ( 1 ) xor fcc ( 0 );
            WHEN "011" =>
                fbres := fcc ( 0 );
            WHEN "100" =>
                fbres := ( not fcc ( 1 ) ) and fcc ( 0 );
            WHEN "101" =>
                fbres := fcc ( 1 );
            WHEN "110" =>
                fbres := fcc ( 1 ) and not fcc ( 0 );
            WHEN OTHERS =>
                fbres := fcc ( 1 ) and fcc ( 0 );
        END CASE;
        branch := cond ( 3 ) xor fbres;
    END;
    PROCEDURE ic_ctrl (
        r : registers;
        inst : word;
        annul_all : in std_ulogic;
        ldlock : in std_ulogic;
        branch_true : in std_ulogic;
        fbranch_true : in std_ulogic;
        cbranch_true : in std_ulogic;
        fccv : in std_ulogic;
        cccv : in std_ulogic;
        cnt : out std_logic_vector ( 1 downto 0 );
        de_pc : out pctype;
        de_branch : out std_ulogic;
        ctrl_annul : out std_ulogic;
        de_annul : out std_ulogic;
        jmpl_inst : out std_ulogic;
        inull : out std_ulogic;
        de_pv : out std_ulogic;
        ctrl_pv : out std_ulogic;
        de_hold_pc : out std_ulogic;
        ticc_exception : out std_ulogic;
        rett_inst : out std_ulogic;
        mulstart : out std_ulogic;
        divstart : out std_ulogic
    ) IS
        VARIABLE op : std_logic_vector ( 1 downto 0 );
        VARIABLE op2 : std_logic_vector ( 2 downto 0 );
        VARIABLE op3 : std_logic_vector ( 5 downto 0 );
        VARIABLE cond : std_logic_vector ( 3 downto 0 );
        VARIABLE hold_pc : std_ulogic;
        VARIABLE annul_current : std_ulogic;
        VARIABLE annul_next : std_ulogic;
        VARIABLE branch : std_ulogic;
        VARIABLE annul : std_ulogic;
        VARIABLE pv : std_ulogic;
        VARIABLE de_jmpl : std_ulogic;
    BEGIN
        branch := '0';
        annul_next := '0';
        annul_current := '0';
        pv := '1';
        hold_pc := '0';
        ticc_exception := '0';
        rett_inst := '0';
        op := inst ( 31 downto 30 );
        op3 := inst ( 24 downto 19 );
        op2 := inst ( 24 downto 22 );
        cond := inst ( 28 downto 25 );
        annul := inst ( 29 );
        de_jmpl := '0';
        cnt := "00";
        mulstart := '0';
        divstart := '0';
        IF r.d.annul = '0' THEN
            CASE inst ( 31 downto 30 ) IS
                WHEN CALL =>
                    branch := '1';
                    IF r.d.inull = '1' THEN
                        hold_pc := '1';
                        annul_current := '1';
                    END IF;
                WHEN FMT2 =>
                    IF ( op2 = BICC ) THEN
                        branch := branch_true;
                        IF hold_pc = '0' THEN
                            IF ( branch = '1' ) THEN
                                IF ( cond = BA ) and ( annul = '1' ) THEN
                                    annul_next := '1';
                                END IF;
                            ELSE
                                annul_next := annul;
                            END IF;
                            IF r.d.inull = '1' THEN
                                hold_pc := '1';
                                annul_current := '1';
                                annul_next := '0';
                            END IF;
                        END IF;
                    END IF;
                WHEN FMT3 =>
                    CASE op3 IS
                        WHEN UMUL | SMUL | UMULCC | SMULCC =>
                            CASE r.d.cnt IS
                                WHEN "00" =>
                                    cnt := "01";
                                    hold_pc := '1';
                                    pv := '0';
                                    mulstart := '1';
                                WHEN "01" =>
                                    IF mulo.nready = '1' THEN
                                        cnt := "00";
                                    ELSE
                                        cnt := "01";
                                        pv := '0';
                                        hold_pc := '1';
                                    END IF;
                                WHEN OTHERS =>
                                    NULL;
                            END CASE;
                        WHEN UDIV | SDIV | UDIVCC | SDIVCC =>
                            CASE r.d.cnt IS
                                WHEN "00" =>
                                    cnt := "01";
                                    hold_pc := '1';
                                    pv := '0';
                                    divstart := '1';
                                WHEN "01" =>
                                    IF divo.nready = '1' THEN
                                        cnt := "00";
                                    ELSE
                                        cnt := "01";
                                        pv := '0';
                                        hold_pc := '1';
                                    END IF;
                                WHEN OTHERS =>
                                    NULL;
                            END CASE;
                        WHEN TICC =>
                            IF branch_true = '1' THEN
                                ticc_exception := '1';
                            END IF;
                        WHEN RETT =>
                            rett_inst := '1';
                        WHEN JMPL =>
                            de_jmpl := '1';
                        WHEN WRY =>
                            IF FALSE THEN
                                IF inst ( 29 downto 25 ) = "10011" THEN
                                    CASE r.d.cnt IS
                                        WHEN "00" =>
                                            pv := '0';
                                            cnt := "00";
                                            hold_pc := '1';
                                            IF r.x.ipend = '1' THEN
                                                cnt := "01";
                                            END IF;
                                        WHEN "01" =>
                                            cnt := "00";
                                        WHEN OTHERS =>
                                            NULL;
                                    END CASE;
                                END IF;
                            END IF;
                        WHEN OTHERS =>
                            NULL;
                    END CASE;
                WHEN OTHERS =>
                    CASE r.d.cnt IS
                        WHEN "00" =>
                            IF ( op3 ( 2 ) = '1' ) or ( op3 ( 1 downto 0 ) = "11" ) THEN
                                cnt := "01";
                                hold_pc := '1';
                                pv := '0';
                            END IF;
                        WHEN "01" =>
                            IF ( op3 ( 2 downto 0 ) = "111" ) or ( op3 ( 3 downto 0 ) = "1101" ) or ( ( ( 0 = 1 ) or ( 0 /= 0 ) ) and ( ( op3 ( 5 ) & op3 ( 2 downto 0 ) ) = "1110" ) ) THEN
                                cnt := "10";
                                pv := '0';
                                hold_pc := '1';
                            ELSE
                                cnt := "00";
                            END IF;
                        WHEN "10" =>
                            cnt := "00";
                        WHEN OTHERS =>
                            NULL;
                    END CASE;
            END CASE;
        END IF;
        IF ldlock = '1' THEN
            cnt := r.d.cnt;
            annul_next := '0';
            pv := '1';
        END IF;
        hold_pc := ( hold_pc or ldlock ) and not annul_all;
        IF hold_pc = '1' THEN
            de_pc := r.d.pc;
        ELSE
            de_pc := r.f.pc;
        END IF;
        annul_current := ( annul_current or ldlock or annul_all );
        ctrl_annul := r.d.annul or annul_all or annul_current;
        pv := pv and not ( ( r.d.inull and not hold_pc ) or annul_all );
        jmpl_inst := de_jmpl and not annul_current;
        annul_next := ( r.d.inull and not hold_pc ) or annul_next or annul_all;
        IF ( annul_next = '1' ) or ( rstn = '0' ) THEN
            cnt := ( OTHERS => '0' );
        END IF;
        de_hold_pc := hold_pc;
        de_branch := branch;
        de_annul := annul_next;
        de_pv := pv;
        ctrl_pv := r.d.pv and not ( ( r.d.annul and not r.d.pv ) or annul_all or annul_current );
        inull := ( not rstn ) or r.d.inull or hold_pc or annul_all;
    END;
    PROCEDURE rd_gen (
        r : registers;
        inst : word;
        wreg : out std_ulogic;
        ld : out std_ulogic;
        rdo : out std_logic_vector ( 4 downto 0 )
    ) IS
        VARIABLE write_reg : std_ulogic;
        VARIABLE op : std_logic_vector ( 1 downto 0 );
        VARIABLE op2 : std_logic_vector ( 2 downto 0 );
        VARIABLE op3 : std_logic_vector ( 5 downto 0 );
        VARIABLE rd : std_logic_vector ( 4 downto 0 );
    BEGIN
        op := inst ( 31 downto 30 );
        op2 := inst ( 24 downto 22 );
        op3 := inst ( 24 downto 19 );
        write_reg := '0';
        rd := inst ( 29 downto 25 );
        ld := '0';
        CASE op IS
            WHEN CALL =>
                write_reg := '1';
                rd := "01111";
            WHEN FMT2 =>
                IF ( op2 = SETHI ) THEN
                    write_reg := '1';
                END IF;
            WHEN FMT3 =>
                CASE op3 IS
                    WHEN UMUL | SMUL | UMULCC | SMULCC =>
                        IF ( ( ( mulo.nready = '1' ) and ( r.d.cnt /= "00" ) ) ) THEN
                            write_reg := '1';
                        END IF;
                    WHEN UDIV | SDIV | UDIVCC | SDIVCC =>
                        IF ( divo.nready = '1' ) and ( r.d.cnt /= "00" ) THEN
                            write_reg := '1';
                        END IF;
                    WHEN RETT | WRPSR | WRY | WRWIM | WRTBR | TICC | FLUSH =>
                        NULL;
                    WHEN FPOP1 | FPOP2 =>
                        NULL;
                    WHEN CPOP1 | CPOP2 =>
                        NULL;
                    WHEN OTHERS =>
                        write_reg := '1';
                END CASE;
            WHEN OTHERS =>
                ld := not op3 ( 2 );
                IF ( op3 ( 2 ) = '0' ) and not ( ( ( 0 = 1 ) or ( 0 /= 0 ) ) and ( op3 ( 5 ) = '1' ) ) THEN
                    write_reg := '1';
                END IF;
                CASE op3 IS
                    WHEN SWAP | SWAPA | LDSTUB | LDSTUBA =>
                        IF r.d.cnt = "00" THEN
                            write_reg := '1';
                            ld := '1';
                        END IF;
                    WHEN OTHERS =>
                        NULL;
                END CASE;
                IF r.d.cnt = "01" THEN
                    CASE op3 IS
                        WHEN LDD | LDDA | LDDC | LDDF =>
                            rd ( 0 ) := '1';
                        WHEN OTHERS =>
                            NULL;
                    END CASE;
                END IF;
        END CASE;
        IF ( rd = "00000" ) THEN
            write_reg := '0';
        END IF;
        wreg := write_reg;
        rdo := rd;
    END;
    FUNCTION imm_data (
        r : registers;
        insn : word
    ) RETURN word IS
        VARIABLE immediate_data : word;
        VARIABLE inst : word;
    BEGIN
        immediate_data := ( OTHERS => '0' );
        inst := insn;
        CASE inst ( 31 downto 30 ) IS
            WHEN FMT2 =>
                immediate_data := inst ( 21 downto 0 ) & "0000000000";
            WHEN OTHERS =>
                immediate_data ( 31 downto 13 ) := ( OTHERS => inst ( 12 ) );
                immediate_data ( 12 downto 0 ) := inst ( 12 downto 0 );
        END CASE;
        RETURN ( immediate_data );
    END;
    FUNCTION get_spr (
        r : registers
    ) RETURN word IS
        VARIABLE spr : word;
    BEGIN
        spr := ( OTHERS => '0' );
        CASE r.e.ctrl.inst ( 24 downto 19 ) IS
            WHEN RDPSR =>
                spr ( 31 downto 5 ) := conv_std_logic_vector ( 15 , 4 ) & conv_std_logic_vector ( 3 , 4 ) & r.m.icc & "000000" & r.w.s.ec & r.w.s.ef & r.w.s.pil & r.e.su & r.w.s.ps & r.e.et;
                spr ( LOG2 ( 8 ) - 1 downto 0 ) := r.e.cwp;
            WHEN RDTBR =>
                spr ( 31 downto 4 ) := r.w.s.tba & r.w.s.tt;
            WHEN RDWIM =>
                spr ( 8 - 1 downto 0 ) := r.w.s.wim;
            WHEN OTHERS =>
                NULL;
        END CASE;
        RETURN ( spr );
    END;
    FUNCTION imm_select (
        inst : word
    ) RETURN boolean IS
        VARIABLE imm : boolean;
    BEGIN
        imm := false;
        CASE inst ( 31 downto 30 ) IS
            WHEN FMT2 =>
                CASE inst ( 24 downto 22 ) IS
                    WHEN SETHI =>
                        imm := true;
                    WHEN OTHERS =>
                        NULL;
                END CASE;
            WHEN FMT3 =>
                CASE inst ( 24 downto 19 ) IS
                    WHEN RDWIM | RDPSR | RDTBR =>
                        imm := true;
                    WHEN OTHERS =>
                        IF ( inst ( 13 ) = '1' ) THEN
                            imm := true;
                        END IF;
                END CASE;
            WHEN LDST =>
                IF ( inst ( 13 ) = '1' ) THEN
                    imm := true;
                END IF;
            WHEN OTHERS =>
                NULL;
        END CASE;
        RETURN ( imm );
    END;
    PROCEDURE alu_op (
        r : in registers;
        iop1 : in word;
        iop2 : in word;
        me_icc : std_logic_vector ( 3 downto 0 );
        my : std_ulogic;
        ldbp : std_ulogic;
        aop1 : out word;
        aop2 : out word;
        aluop : out std_logic_vector ( 2 downto 0 );
        alusel : out std_logic_vector ( 1 downto 0 );
        aluadd : out std_ulogic;
        shcnt : out std_logic_vector ( 4 downto 0 );
        sari : out std_ulogic;
        shleft : out std_ulogic;
        ymsb : out std_ulogic;
        mulins : out std_ulogic;
        divins : out std_ulogic;
        mulstep : out std_ulogic;
        macins : out std_ulogic;
        ldbp2 : out std_ulogic;
        invop2 : out std_ulogic
    ) IS
        VARIABLE op : std_logic_vector ( 1 downto 0 );
        VARIABLE op2 : std_logic_vector ( 2 downto 0 );
        VARIABLE op3 : std_logic_vector ( 5 downto 0 );
        VARIABLE rd : std_logic_vector ( 4 downto 0 );
        VARIABLE icc : std_logic_vector ( 3 downto 0 );
        VARIABLE y0 : std_ulogic;
    BEGIN
        op := r.a.ctrl.inst ( 31 downto 30 );
        op2 := r.a.ctrl.inst ( 24 downto 22 );
        op3 := r.a.ctrl.inst ( 24 downto 19 );
        aop1 := iop1;
        aop2 := iop2;
        ldbp2 := ldbp;
        aluop := "000";
        alusel := "11";
        aluadd := '1';
        shcnt := iop2 ( 4 downto 0 );
        sari := '0';
        shleft := '0';
        invop2 := '0';
        ymsb := iop1 ( 0 );
        mulins := '0';
        divins := '0';
        mulstep := '0';
        macins := '0';
        IF r.e.ctrl.wy = '1' THEN
            y0 := my;
        ELSIF r.m.ctrl.wy = '1' THEN
            y0 := r.m.y ( 0 );
        ELSIF r.x.ctrl.wy = '1' THEN
            y0 := r.x.y ( 0 );
        ELSE
            y0 := r.w.s.y ( 0 );
        END IF;
        IF r.e.ctrl.wicc = '1' THEN
            icc := me_icc;
        ELSIF r.m.ctrl.wicc = '1' THEN
            icc := r.m.icc;
        ELSIF r.x.ctrl.wicc = '1' THEN
            icc := r.x.icc;
        ELSE
            icc := r.w.s.icc;
        END IF;
        CASE op IS
            WHEN CALL =>
                aluop := "111";
            WHEN FMT2 =>
                CASE op2 IS
                    WHEN SETHI =>
                        aluop := "001";
                    WHEN OTHERS =>
                        NULL;
                END CASE;
            WHEN FMT3 =>
                CASE op3 IS
                    WHEN IADD | ADDX | ADDCC | ADDXCC | TADDCC | TADDCCTV | SAVE | RESTORE | TICC | JMPL | RETT =>
                        alusel := "00";
                    WHEN ISUB | SUBX | SUBCC | SUBXCC | TSUBCC | TSUBCCTV =>
                        alusel := "00";
                        aluadd := '0';
                        aop2 := not iop2;
                        invop2 := '1';
                    WHEN MULSCC =>
                        alusel := "00";
                        aop1 := ( icc ( 3 ) xor icc ( 1 ) ) & iop1 ( 31 downto 1 );
                        IF y0 = '0' THEN
                            aop2 := ( OTHERS => '0' );
                            ldbp2 := '0';
                        END IF;
                        mulstep := '1';
                    WHEN UMUL | UMULCC | SMUL | SMULCC =>
                        mulins := '1';
                    WHEN UMAC | SMAC =>
                        NULL;
                    WHEN UDIV | UDIVCC | SDIV | SDIVCC =>
                        aluop := "110";
                        alusel := "10";
                        divins := '1';
                    WHEN IAND | ANDCC =>
                        aluop := "000";
                        alusel := "10";
                    WHEN ANDN | ANDNCC =>
                        aluop := "100";
                        alusel := "10";
                    WHEN IOR | ORCC =>
                        aluop := "010";
                        alusel := "10";
                    WHEN ORN | ORNCC =>
                        aluop := "101";
                        alusel := "10";
                    WHEN IXNOR | XNORCC =>
                        aluop := "011";
                        alusel := "10";
                    WHEN XORCC | IXOR | WRPSR | WRWIM | WRTBR | WRY =>
                        aluop := "001";
                        alusel := "10";
                    WHEN RDPSR | RDTBR | RDWIM =>
                        aluop := "110";
                    WHEN RDY =>
                        aluop := "101";
                    WHEN ISLL =>
                        aluop := "001";
                        alusel := "01";
                        shleft := '1';
                        shcnt := not iop2 ( 4 downto 0 );
                        invop2 := '1';
                    WHEN ISRL =>
                        aluop := "010";
                        alusel := "01";
                    WHEN ISRA =>
                        aluop := "100";
                        alusel := "01";
                        sari := iop1 ( 31 );
                    WHEN FPOP1 | FPOP2 =>
                        NULL;
                    WHEN OTHERS =>
                        NULL;
                END CASE;
            WHEN OTHERS =>
                CASE r.a.ctrl.cnt IS
                    WHEN "00" =>
                        alusel := "00";
                    WHEN "01" =>
                        CASE op3 IS
                            WHEN LDD | LDDA | LDDC =>
                                alusel := "00";
                            WHEN LDDF =>
                                alusel := "00";
                            WHEN SWAP | SWAPA | LDSTUB | LDSTUBA =>
                                alusel := "00";
                            WHEN STF | STDF =>
                                NULL;
                            WHEN OTHERS =>
                                aluop := "000";
                                IF op3 ( 2 ) = '1' THEN
                                    IF op3 ( 1 downto 0 ) = "01" THEN
                                        aluop := "010";
                                    ELSIF op3 ( 1 downto 0 ) = "10" THEN
                                        aluop := "011";
                                    END IF;
                                END IF;
                        END CASE;
                    WHEN "10" =>
                        aluop := "000";
                        IF op3 ( 2 ) = '1' THEN
                            IF ( op3 ( 3 ) and not op3 ( 1 ) ) = '1' THEN
                                aluop := "100";
                            END IF;
                        END IF;
                    WHEN OTHERS =>
                        NULL;
                END CASE;
        END CASE;
    END;
    FUNCTION ra_inull_gen (
        r : registers;
        v : registers
    ) RETURN std_ulogic IS
        VARIABLE de_inull : std_ulogic;
    BEGIN
        de_inull := '0';
        IF ( ( v.e.jmpl or v.e.ctrl.rett ) and not v.e.ctrl.annul and not ( r.e.jmpl and not r.e.ctrl.annul ) ) = '1' THEN
            de_inull := '1';
        END IF;
        IF ( ( v.a.jmpl or v.a.ctrl.rett ) and not v.a.ctrl.annul and not ( r.a.jmpl and not r.a.ctrl.annul ) ) = '1' THEN
            de_inull := '1';
        END IF;
        RETURN ( de_inull );
    END;
    PROCEDURE op_mux (
        r : in registers;
        rfd : in word;
        ed : in word;
        md : in word;
        xd : in word;
        im : in word;
        rsel : in std_logic_vector ( 2 downto 0 );
        ldbp : out std_ulogic;
        d : out word
    ) IS
    BEGIN
        ldbp := '0';
        CASE rsel IS
            WHEN "000" =>
                d := rfd;
            WHEN "001" =>
                d := ed;
            WHEN "010" =>
                d := md;
                ldbp := r.m.ctrl.ld;
            WHEN "011" =>
                d := xd;
            WHEN "100" =>
                d := im;
            WHEN "101" =>
                d := ( OTHERS => '0' );
            WHEN "110" =>
                d := r.w.result;
            WHEN OTHERS =>
                d := ( OTHERS => '-' );
        END CASE;
    END;
    PROCEDURE op_find (
        r : in registers;
        ldchkra : std_ulogic;
        ldchkex : std_ulogic;
        rs1 : std_logic_vector ( 4 downto 0 );
        ra : rfatype;
        im : boolean;
        rfe : out std_ulogic;
        osel : out std_logic_vector ( 2 downto 0 );
        ldcheck : std_ulogic
    ) IS
    BEGIN
        rfe := '0';
        IF im THEN
            osel := "100";
        ELSIF rs1 = "00000" THEN
            osel := "101";
        ELSIF ( ( r.a.ctrl.wreg and ldchkra ) = '1' ) and ( ra = r.a.ctrl.rd ) THEN
            osel := "001";
        ELSIF ( ( r.e.ctrl.wreg and ldchkex ) = '1' ) and ( ra = r.e.ctrl.rd ) THEN
            osel := "010";
        ELSIF r.m.ctrl.wreg = '1' and ( ra = r.m.ctrl.rd ) THEN
            osel := "011";
        ELSE
            osel := "000";
            rfe := ldcheck;
        END IF;
    END;
    PROCEDURE cin_gen (
        r : registers;
        me_cin : in std_ulogic;
        cin : out std_ulogic
    ) IS
        VARIABLE op : std_logic_vector ( 1 downto 0 );
        VARIABLE op3 : std_logic_vector ( 5 downto 0 );
        VARIABLE ncin : std_ulogic;
    BEGIN
        op := r.a.ctrl.inst ( 31 downto 30 );
        op3 := r.a.ctrl.inst ( 24 downto 19 );
        IF r.e.ctrl.wicc = '1' THEN
            ncin := me_cin;
        ELSE
            ncin := r.m.icc ( 0 );
        END IF;
        cin := '0';
        CASE op IS
            WHEN FMT3 =>
                CASE op3 IS
                    WHEN ISUB | SUBCC | TSUBCC | TSUBCCTV =>
                        cin := '1';
                    WHEN ADDX | ADDXCC =>
                        cin := ncin;
                    WHEN SUBX | SUBXCC =>
                        cin := not ncin;
                    WHEN OTHERS =>
                        NULL;
                END CASE;
            WHEN OTHERS =>
                NULL;
        END CASE;
    END;
    PROCEDURE logic_op (
        r : registers;
        aluin1 : word;
        aluin2 : word;
        mey : word;
        ymsb : std_ulogic;
        logicres : out word;
        y : out word
    ) IS
        VARIABLE logicout : word;
    BEGIN
        CASE r.e.aluop IS
            WHEN "000" =>
                logicout := aluin1 and aluin2;
            WHEN "100" =>
                logicout := aluin1 and not aluin2;
            WHEN "010" =>
                logicout := aluin1 or aluin2;
            WHEN "101" =>
                logicout := aluin1 or not aluin2;
            WHEN "001" =>
                logicout := aluin1 xor aluin2;
            WHEN "011" =>
                logicout := aluin1 xor not aluin2;
            WHEN "110" =>
                logicout := aluin2;
            WHEN OTHERS =>
                logicout := ( OTHERS => '-' );
        END CASE;
        IF ( r.e.ctrl.wy and r.e.mulstep ) = '1' THEN
            y := ymsb & r.m.y ( 31 downto 1 );
        ELSIF r.e.ctrl.wy = '1' THEN
            y := logicout;
        ELSIF r.m.ctrl.wy = '1' THEN
            y := mey;
        ELSIF r.x.ctrl.wy = '1' THEN
            y := r.x.y;
        ELSE
            y := r.w.s.y;
        END IF;
        logicres := logicout;
    END;
    PROCEDURE misc_op (
        r : registers;
        wpr : watchpoint_registers;
        aluin1 : word;
        aluin2 : word;
        ldata : word;
        mey : word;
        mout : out word;
        edata : out word
    ) IS
        VARIABLE miscout : word;
        VARIABLE bpdata : word;
        VARIABLE stdata : word;
        VARIABLE wpi : integer;
    BEGIN
        wpi := 0;
        miscout := r.e.ctrl.pc ( 31 downto 2 ) & "00";
        edata := aluin1;
        bpdata := aluin1;
        IF ( ( r.x.ctrl.wreg and r.x.ctrl.ld and not r.x.ctrl.annul ) = '1' ) and ( r.x.ctrl.rd = r.e.ctrl.rd ) and ( r.e.ctrl.inst ( 31 downto 30 ) = LDST ) and ( r.e.ctrl.cnt /= "10" ) THEN
            bpdata := ldata;
        END IF;
        CASE r.e.aluop IS
            WHEN "010" =>
                miscout := bpdata ( 7 downto 0 ) & bpdata ( 7 downto 0 ) & bpdata ( 7 downto 0 ) & bpdata ( 7 downto 0 );
                edata := miscout;
            WHEN "011" =>
                miscout := bpdata ( 15 downto 0 ) & bpdata ( 15 downto 0 );
                edata := miscout;
            WHEN "000" =>
                miscout := bpdata;
                edata := miscout;
            WHEN "001" =>
                miscout := aluin2;
            WHEN "100" =>
                miscout := ( OTHERS => '1' );
                edata := miscout;
            WHEN "101" =>
                IF ( r.m.ctrl.wy = '1' ) THEN
                    miscout := mey;
                ELSE
                    miscout := r.m.y;
                END IF;
                IF ( r.e.ctrl.inst ( 18 downto 17 ) = "11" ) THEN
                    wpi := conv_integer ( r.e.ctrl.inst ( 16 downto 15 ) );
                    IF r.e.ctrl.inst ( 14 ) = '0' THEN
                        miscout := wpr ( wpi ).addr & '0' & wpr ( wpi ).exec;
                    ELSE
                        miscout := wpr ( wpi ).mask & wpr ( wpi ).load & wpr ( wpi ).store;
                    END IF;
                END IF;
                IF ( r.e.ctrl.inst ( 18 downto 17 ) = "10" ) and ( r.e.ctrl.inst ( 14 ) = '1' ) THEN
                    miscout := asr17_gen ( r );
                END IF;
            WHEN "110" =>
                miscout := get_spr ( r );
            WHEN OTHERS =>
                NULL;
        END CASE;
        mout := miscout;
    END;
    PROCEDURE alu_select (
        r : registers;
        addout : std_logic_vector ( 32 downto 0 );
        op1 : word;
        op2 : word;
        shiftout : word;
        logicout : word;
        miscout : word;
        res : out word;
        me_icc : std_logic_vector ( 3 downto 0 );
        icco : out std_logic_vector ( 3 downto 0 );
        divz : out std_ulogic
    ) IS
        VARIABLE op : std_logic_vector ( 1 downto 0 );
        VARIABLE op3 : std_logic_vector ( 5 downto 0 );
        VARIABLE icc : std_logic_vector ( 3 downto 0 );
        VARIABLE aluresult : word;
    BEGIN
        op := r.e.ctrl.inst ( 31 downto 30 );
        op3 := r.e.ctrl.inst ( 24 downto 19 );
        icc := ( OTHERS => '0' );
        CASE r.e.alusel IS
            WHEN "00" =>
                aluresult := addout ( 32 downto 1 );
                IF r.e.aluadd = '0' THEN
                    icc ( 0 ) := ( ( not op1 ( 31 ) ) and not op2 ( 31 ) ) or ( addout ( 32 ) and ( ( not op1 ( 31 ) ) or not op2 ( 31 ) ) );
                    icc ( 1 ) := ( op1 ( 31 ) and ( op2 ( 31 ) ) and not addout ( 32 ) ) or ( addout ( 32 ) and ( not op1 ( 31 ) ) and not op2 ( 31 ) );
                ELSE
                    icc ( 0 ) := ( op1 ( 31 ) and op2 ( 31 ) ) or ( ( not addout ( 32 ) ) and ( op1 ( 31 ) or op2 ( 31 ) ) );
                    icc ( 1 ) := ( op1 ( 31 ) and op2 ( 31 ) and not addout ( 32 ) ) or ( addout ( 32 ) and ( not op1 ( 31 ) ) and ( not op2 ( 31 ) ) );
                END IF;
                CASE op IS
                    WHEN FMT3 =>
                        CASE op3 IS
                            WHEN TADDCC | TADDCCTV =>
                                icc ( 1 ) := op1 ( 0 ) or op1 ( 1 ) or op2 ( 0 ) or op2 ( 1 ) or icc ( 1 );
                            WHEN TSUBCC | TSUBCCTV =>
                                icc ( 1 ) := op1 ( 0 ) or op1 ( 1 ) or ( not op2 ( 0 ) ) or ( not op2 ( 1 ) ) or icc ( 1 );
                            WHEN OTHERS =>
                                NULL;
                        END CASE;
                    WHEN OTHERS =>
                        NULL;
                END CASE;
                IF aluresult = zero32 THEN
                    icc ( 2 ) := '1';
                END IF;
            WHEN "01" =>
                aluresult := shiftout;
            WHEN "10" =>
                aluresult := logicout;
                IF aluresult = zero32 THEN
                    icc ( 2 ) := '1';
                END IF;
            WHEN OTHERS =>
                aluresult := miscout;
        END CASE;
        IF r.e.jmpl = '1' THEN
            aluresult := r.e.ctrl.pc ( 31 downto 2 ) & "00";
        END IF;
        icc ( 3 ) := aluresult ( 31 );
        divz := icc ( 2 );
        IF r.e.ctrl.wicc = '1' THEN
            IF ( op = FMT3 ) and ( op3 = WRPSR ) THEN
                icco := logicout ( 23 downto 20 );
            ELSE
                icco := icc;
            END IF;
        ELSIF r.m.ctrl.wicc = '1' THEN
            icco := me_icc;
        ELSIF r.x.ctrl.wicc = '1' THEN
            icco := r.x.icc;
        ELSE
            icco := r.w.s.icc;
        END IF;
        res := aluresult;
    END;
    PROCEDURE dcache_gen (
        r : registers;
        v : registers;
        dci : out dc_in_type;
        link_pc : out std_ulogic;
        jump : out std_ulogic;
        force_a2 : out std_ulogic;
        load : out std_ulogic
    ) IS
        VARIABLE op : std_logic_vector ( 1 downto 0 );
        VARIABLE op3 : std_logic_vector ( 5 downto 0 );
        VARIABLE su : std_ulogic;
    BEGIN
        op := r.e.ctrl.inst ( 31 downto 30 );
        op3 := r.e.ctrl.inst ( 24 downto 19 );
        dci.signed := '0';
        dci.lock := '0';
        dci.dsuen := '0';
        dci.size := "10";
        IF op = LDST THEN
            CASE op3 IS
                WHEN LDUB | LDUBA =>
                    dci.size := "00";
                WHEN LDSTUB | LDSTUBA =>
                    dci.size := "00";
                    dci.lock := '1';
                WHEN LDUH | LDUHA =>
                    dci.size := "01";
                WHEN LDSB | LDSBA =>
                    dci.size := "00";
                    dci.signed := '1';
                WHEN LDSH | LDSHA =>
                    dci.size := "01";
                    dci.signed := '1';
                WHEN LD | LDA | LDF | LDC =>
                    dci.size := "10";
                WHEN SWAP | SWAPA =>
                    dci.size := "10";
                    dci.lock := '1';
                WHEN LDD | LDDA | LDDF | LDDC =>
                    dci.size := "11";
                WHEN STB | STBA =>
                    dci.size := "00";
                WHEN STH | STHA =>
                    dci.size := "01";
                WHEN ST | STA | STF =>
                    dci.size := "10";
                WHEN ISTD | STDA =>
                    dci.size := "11";
                WHEN STDF | STDFQ =>
                    NULL;
                WHEN STDC | STDCQ =>
                    NULL;
                WHEN OTHERS =>
                    dci.size := "10";
                    dci.lock := '0';
                    dci.signed := '0';
            END CASE;
        END IF;
        link_pc := '0';
        jump := '0';
        force_a2 := '0';
        load := '0';
        dci.write := '0';
        dci.enaddr := '0';
        dci.read := not op3 ( 2 );
        IF ( r.e.ctrl.annul = '0' ) THEN
            CASE op IS
                WHEN CALL =>
                    link_pc := '1';
                WHEN FMT3 =>
                    CASE op3 IS
                        WHEN JMPL =>
                            jump := '1';
                            link_pc := '1';
                        WHEN RETT =>
                            jump := '1';
                        WHEN OTHERS =>
                            NULL;
                    END CASE;
                WHEN LDST =>
                    CASE r.e.ctrl.cnt IS
                        WHEN "00" =>
                            dci.read := op3 ( 3 ) or not op3 ( 2 );
                            load := op3 ( 3 ) or not op3 ( 2 );
                            dci.enaddr := '1';
                        WHEN "01" =>
                            force_a2 := not op3 ( 2 );
                            load := not op3 ( 2 );
                            dci.enaddr := not op3 ( 2 );
                            IF op3 ( 3 downto 2 ) = "01" THEN
                                dci.write := '1';
                            END IF;
                            IF op3 ( 3 downto 2 ) = "11" THEN
                                dci.enaddr := '1';
                            END IF;
                        WHEN "10" =>
                            dci.write := '1';
                        WHEN OTHERS =>
                            NULL;
                    END CASE;
                    IF ( r.e.ctrl.trap or ( v.x.ctrl.trap and not v.x.ctrl.annul ) ) = '1' THEN
                        dci.enaddr := '0';
                    END IF;
                WHEN OTHERS =>
                    NULL;
            END CASE;
        END IF;
        IF ( ( r.x.ctrl.rett and not r.x.ctrl.annul ) = '1' ) THEN
            su := r.w.s.ps;
        ELSE
            su := r.w.s.s;
        END IF;
        IF su = '1' THEN
            dci.asi := "00001011";
        ELSE
            dci.asi := "00001010";
        END IF;
        IF ( op3 ( 4 ) = '1' ) and ( ( op3 ( 5 ) = '0' ) or not ( 0 = 1 ) ) THEN
            dci.asi := r.e.ctrl.inst ( 12 downto 5 );
        END IF;
    END;
    PROCEDURE fpstdata (
        r : in registers;
        edata : in word;
        eres : in word;
        fpstdata : in std_logic_vector ( 31 downto 0 );
        edata2 : out word;
        eres2 : out word
    ) IS
        VARIABLE op : std_logic_vector ( 1 downto 0 );
        VARIABLE op3 : std_logic_vector ( 5 downto 0 );
    BEGIN
        edata2 := edata;
        eres2 := eres;
        op := r.e.ctrl.inst ( 31 downto 30 );
        op3 := r.e.ctrl.inst ( 24 downto 19 );
    END;
    FUNCTION ld_align (
        data : dcdtype;
        set : std_logic_vector ( LOG2X ( 2 ) - 1 downto 0 );
        size : std_logic_vector ( 1 downto 0 );
        laddr : std_logic_vector ( 1 downto 0 );
        signed : std_ulogic
    ) RETURN word IS
        VARIABLE align_data : word;
        VARIABLE rdata : word;
    BEGIN
        align_data := data ( conv_integer ( set ) );
        rdata := ( OTHERS => '0' );
        CASE size IS
            WHEN "00" =>
                CASE laddr IS
                    WHEN "00" =>
                        rdata ( 7 downto 0 ) := align_data ( 31 downto 24 );
                        IF signed = '1' THEN
                            rdata ( 31 downto 8 ) := ( OTHERS => align_data ( 31 ) );
                        END IF;
                    WHEN "01" =>
                        rdata ( 7 downto 0 ) := align_data ( 23 downto 16 );
                        IF signed = '1' THEN
                            rdata ( 31 downto 8 ) := ( OTHERS => align_data ( 23 ) );
                        END IF;
                    WHEN "10" =>
                        rdata ( 7 downto 0 ) := align_data ( 15 downto 8 );
                        IF signed = '1' THEN
                            rdata ( 31 downto 8 ) := ( OTHERS => align_data ( 15 ) );
                        END IF;
                    WHEN OTHERS =>
                        rdata ( 7 downto 0 ) := align_data ( 7 downto 0 );
                        IF signed = '1' THEN
                            rdata ( 31 downto 8 ) := ( OTHERS => align_data ( 7 ) );
                        END IF;
                END CASE;
            WHEN "01" =>
                IF laddr ( 1 ) = '1' THEN
                    rdata ( 15 downto 0 ) := align_data ( 15 downto 0 );
                    IF signed = '1' THEN
                        rdata ( 31 downto 15 ) := ( OTHERS => align_data ( 15 ) );
                    END IF;
                ELSE
                    rdata ( 15 downto 0 ) := align_data ( 31 downto 16 );
                    IF signed = '1' THEN
                        rdata ( 31 downto 15 ) := ( OTHERS => align_data ( 31 ) );
                    END IF;
                END IF;
            WHEN OTHERS =>
                rdata := align_data;
        END CASE;
        RETURN ( rdata );
    END;
    PROCEDURE mem_trap (
        r : registers;
        wpr : watchpoint_registers;
        annul : in std_ulogic;
        holdn : in std_ulogic;
        trapout : out std_ulogic;
        iflush : out std_ulogic;
        nullify : out std_ulogic;
        werrout : out std_ulogic;
        tt : out std_logic_vector ( 5 downto 0 )
    ) IS
        VARIABLE cwp : std_logic_vector ( LOG2 ( 8 ) - 1 downto 0 );
        VARIABLE cwpx : std_logic_vector ( 5 downto LOG2 ( 8 ) );
        VARIABLE op : std_logic_vector ( 1 downto 0 );
        VARIABLE op2 : std_logic_vector ( 2 downto 0 );
        VARIABLE op3 : std_logic_vector ( 5 downto 0 );
        VARIABLE nalign_d : std_ulogic;
        VARIABLE trap : std_ulogic;
        VARIABLE werr : std_ulogic;
    BEGIN
        op := r.m.ctrl.inst ( 31 downto 30 );
        op2 := r.m.ctrl.inst ( 24 downto 22 );
        op3 := r.m.ctrl.inst ( 24 downto 19 );
        cwpx := r.m.result ( 5 downto LOG2 ( 8 ) );
        cwpx ( 5 ) := '0';
        iflush := '0';
        trap := r.m.ctrl.trap;
        nullify := annul;
        tt := r.m.ctrl.tt;
        werr := ( dco.werr or r.m.werr ) and not r.w.s.dwt;
        nalign_d := r.m.nalign or r.m.result ( 2 );
        IF ( ( annul or trap ) /= '1' ) and ( r.m.ctrl.pv = '1' ) THEN
            IF ( werr and holdn ) = '1' THEN
                trap := '1';
                tt := TT_DSEX;
                werr := '0';
                IF op = LDST THEN
                    nullify := '1';
                END IF;
            END IF;
        END IF;
        IF ( ( annul or trap ) /= '1' ) THEN
            CASE op IS
                WHEN FMT2 =>
                    CASE op2 IS
                        WHEN FBFCC =>
                            NULL;
                        WHEN CBCCC =>
                            NULL;
                        WHEN OTHERS =>
                            NULL;
                    END CASE;
                WHEN FMT3 =>
                    CASE op3 IS
                        WHEN WRPSR =>
                            IF ( orv ( cwpx ) = '1' ) THEN
                                trap := '1';
                                tt := TT_IINST;
                            END IF;
                        WHEN UDIV | SDIV | UDIVCC | SDIVCC =>
                            IF r.m.divz = '1' THEN
                                trap := '1';
                                tt := TT_DIV;
                            END IF;
                        WHEN JMPL | RETT =>
                            IF r.m.nalign = '1' THEN
                                trap := '1';
                                tt := TT_UNALA;
                            END IF;
                        WHEN TADDCCTV | TSUBCCTV =>
                            IF ( r.m.icc ( 1 ) = '1' ) THEN
                                trap := '1';
                                tt := TT_TAG;
                            END IF;
                        WHEN FLUSH =>
                            iflush := '1';
                        WHEN FPOP1 | FPOP2 =>
                            NULL;
                        WHEN CPOP1 | CPOP2 =>
                            NULL;
                        WHEN OTHERS =>
                            NULL;
                    END CASE;
                WHEN LDST =>
                    IF r.m.ctrl.cnt = "00" THEN
                        CASE op3 IS
                            WHEN LDDF | STDF | STDFQ =>
                                NULL;
                            WHEN LDDC | STDC | STDCQ =>
                                NULL;
                            WHEN LDD | ISTD | LDDA | STDA =>
                                IF r.m.result ( 2 downto 0 ) /= "000" THEN
                                    trap := '1';
                                    tt := TT_UNALA;
                                    nullify := '1';
                                END IF;
                            WHEN LDF | LDFSR | STFSR | STF =>
                                NULL;
                            WHEN LDC | LDCSR | STCSR | STC =>
                                NULL;
                            WHEN LD | LDA | ST | STA | SWAP | SWAPA =>
                                IF r.m.result ( 1 downto 0 ) /= "00" THEN
                                    trap := '1';
                                    tt := TT_UNALA;
                                    nullify := '1';
                                END IF;
                            WHEN LDUH | LDUHA | LDSH | LDSHA | STH | STHA =>
                                IF r.m.result ( 0 ) /= '0' THEN
                                    trap := '1';
                                    tt := TT_UNALA;
                                    nullify := '1';
                                END IF;
                            WHEN OTHERS =>
                                NULL;
                        END CASE;
                        IF ( ( ( ( wpr ( 0 ).load and not op3 ( 2 ) ) or ( wpr ( 0 ).store and op3 ( 2 ) ) ) = '1' ) and ( ( ( wpr ( 0 ).addr xor r.m.result ( 31 downto 2 ) ) and wpr ( 0 ).mask ) = zero32 ( 31 downto 2 ) ) ) THEN
                            trap := '1';
                            tt := TT_WATCH;
                            nullify := '1';
                        END IF;
                        IF ( ( ( ( wpr ( 1 ).load and not op3 ( 2 ) ) or ( wpr ( 1 ).store and op3 ( 2 ) ) ) = '1' ) and ( ( ( wpr ( 1 ).addr xor r.m.result ( 31 downto 2 ) ) and wpr ( 1 ).mask ) = zero32 ( 31 downto 2 ) ) ) THEN
                            trap := '1';
                            tt := TT_WATCH;
                            nullify := '1';
                        END IF;
                    END IF;
                WHEN OTHERS =>
                    NULL;
            END CASE;
        END IF;
        IF ( rstn = '0' ) or ( r.x.rstate = dsu2 ) THEN
            werr := '0';
        END IF;
        trapout := trap;
        werrout := werr;
    END;
    PROCEDURE irq_trap (
        r : in registers;
        ir : in irestart_register;
        irl : in std_logic_vector ( 3 downto 0 );
        annul : in std_ulogic;
        pv : in std_ulogic;
        trap : in std_ulogic;
        tt : in std_logic_vector ( 5 downto 0 );
        nullify : in std_ulogic;
        irqen : out std_ulogic;
        irqen2 : out std_ulogic;
        nullify2 : out std_ulogic;
        trap2 : out std_ulogic;
        ipend : out std_ulogic;
        tt2 : out std_logic_vector ( 5 downto 0 )
    ) IS
        VARIABLE op : std_logic_vector ( 1 downto 0 );
        VARIABLE op3 : std_logic_vector ( 5 downto 0 );
        VARIABLE pend : std_ulogic;
    BEGIN
        nullify2 := nullify;
        trap2 := trap;
        tt2 := tt;
        op := r.m.ctrl.inst ( 31 downto 30 );
        op3 := r.m.ctrl.inst ( 24 downto 19 );
        irqen := '1';
        irqen2 := r.m.irqen;
        IF ( annul or trap ) = '0' THEN
            IF ( ( op = FMT3 ) and ( op3 = WRPSR ) ) THEN
                irqen := '0';
            END IF;
        END IF;
        IF ( irl = "1111" ) or ( irl > r.w.s.pil ) THEN
            pend := r.m.irqen and r.m.irqen2 and r.w.s.et and not ir.pwd;
        ELSE
            pend := '0';
        END IF;
        ipend := pend;
        IF ( ( not annul ) and pv and ( not trap ) and pend ) = '1' THEN
            trap2 := '1';
            tt2 := "01" & irl;
            IF op = LDST THEN
                nullify2 := '1';
            END IF;
        END IF;
    END;
    PROCEDURE irq_intack (
        r : in registers;
        holdn : in std_ulogic;
        intack : out std_ulogic
    ) IS
    BEGIN
        intack := '0';
        IF r.x.rstate = trap THEN
            IF r.w.s.tt ( 7 downto 4 ) = "0001" THEN
                intack := '1';
            END IF;
        END IF;
    END;
    PROCEDURE sp_write (
        r : registers;
        wpr : watchpoint_registers;
        s : out special_register_type;
        vwpr : out watchpoint_registers
    ) IS
        VARIABLE op : std_logic_vector ( 1 downto 0 );
        VARIABLE op2 : std_logic_vector ( 2 downto 0 );
        VARIABLE op3 : std_logic_vector ( 5 downto 0 );
        VARIABLE rd : std_logic_vector ( 4 downto 0 );
        VARIABLE i : integer RANGE 0 to 3;
    BEGIN
        op := r.x.ctrl.inst ( 31 downto 30 );
        op2 := r.x.ctrl.inst ( 24 downto 22 );
        op3 := r.x.ctrl.inst ( 24 downto 19 );
        s := r.w.s;
        rd := r.x.ctrl.inst ( 29 downto 25 );
        vwpr := wpr;
        CASE op IS
            WHEN FMT3 =>
                CASE op3 IS
                    WHEN WRY =>
                        IF rd = "00000" THEN
                            s.y := r.x.result;
                        ELSIF ( rd = "10001" ) THEN
                            s.dwt := r.x.result ( 14 );
                            s.svt := r.x.result ( 13 );
                        ELSIF rd ( 4 downto 3 ) = "11" THEN
                            CASE rd ( 2 downto 0 ) IS
                                WHEN "000" =>
                                    vwpr ( 0 ).addr := r.x.result ( 31 downto 2 );
                                    vwpr ( 0 ).imp := r.x.result ( 1 );
                                    vwpr ( 0 ).exec := r.x.result ( 0 );
                                WHEN "001" =>
                                    vwpr ( 0 ).mask := r.x.result ( 31 downto 2 );
                                    vwpr ( 0 ).load := r.x.result ( 1 );
                                    vwpr ( 0 ).store := r.x.result ( 0 );
                                WHEN "010" =>
                                    vwpr ( 1 ).addr := r.x.result ( 31 downto 2 );
                                    vwpr ( 1 ).imp := r.x.result ( 1 );
                                    vwpr ( 1 ).exec := r.x.result ( 0 );
                                WHEN "011" =>
                                    vwpr ( 1 ).mask := r.x.result ( 31 downto 2 );
                                    vwpr ( 1 ).load := r.x.result ( 1 );
                                    vwpr ( 1 ).store := r.x.result ( 0 );
                                WHEN "100" =>
                                    vwpr ( 2 ).addr := r.x.result ( 31 downto 2 );
                                    vwpr ( 2 ).imp := r.x.result ( 1 );
                                    vwpr ( 2 ).exec := r.x.result ( 0 );
                                WHEN "101" =>
                                    vwpr ( 2 ).mask := r.x.result ( 31 downto 2 );
                                    vwpr ( 2 ).load := r.x.result ( 1 );
                                    vwpr ( 2 ).store := r.x.result ( 0 );
                                WHEN "110" =>
                                    vwpr ( 3 ).addr := r.x.result ( 31 downto 2 );
                                    vwpr ( 3 ).imp := r.x.result ( 1 );
                                    vwpr ( 3 ).exec := r.x.result ( 0 );
                                WHEN OTHERS =>
                                    vwpr ( 3 ).mask := r.x.result ( 31 downto 2 );
                                    vwpr ( 3 ).load := r.x.result ( 1 );
                                    vwpr ( 3 ).store := r.x.result ( 0 );
                            END CASE;
                        END IF;
                    WHEN WRPSR =>
                        s.cwp := r.x.result ( LOG2 ( 8 ) - 1 downto 0 );
                        s.icc := r.x.result ( 23 downto 20 );
                        s.ec := r.x.result ( 13 );
                        s.pil := r.x.result ( 11 downto 8 );
                        s.s := r.x.result ( 7 );
                        s.ps := r.x.result ( 6 );
                        s.et := r.x.result ( 5 );
                    WHEN WRWIM =>
                        s.wim := r.x.result ( 8 - 1 downto 0 );
                    WHEN WRTBR =>
                        s.tba := r.x.result ( 31 downto 12 );
                    WHEN SAVE =>
                        s.cwp := r.w.s.cwp - 1;
                    WHEN RESTORE =>
                        s.cwp := r.w.s.cwp + 1;
                    WHEN RETT =>
                        s.cwp := r.w.s.cwp + 1;
                        s.s := r.w.s.ps;
                        s.et := '1';
                    WHEN OTHERS =>
                        NULL;
                END CASE;
            WHEN OTHERS =>
                NULL;
        END CASE;
        IF r.x.ctrl.wicc = '1' THEN
            s.icc := r.x.icc;
        END IF;
        IF r.x.ctrl.wy = '1' THEN
            s.y := r.x.y;
        END IF;
    END;
    FUNCTION npc_find (
        r : registers
    ) RETURN std_logic_vector IS
        VARIABLE npc : std_logic_vector ( 2 downto 0 );
    BEGIN
        npc := "011";
        IF r.m.ctrl.pv = '1' THEN
            npc := "000";
        ELSIF r.e.ctrl.pv = '1' THEN
            npc := "001";
        ELSIF r.a.ctrl.pv = '1' THEN
            npc := "010";
        ELSIF r.d.pv = '1' THEN
            npc := "011";
        ELSE
            npc := "100";
        END IF;
        RETURN ( npc );
    END;
    FUNCTION npc_gen (
        r : registers
    ) RETURN word IS
        VARIABLE npc : std_logic_vector ( 31 downto 0 );
    BEGIN
        npc := r.a.ctrl.pc ( 31 downto 2 ) & "00";
        CASE r.x.npc IS
            WHEN "000" =>
                npc ( 31 downto 2 ) := r.x.ctrl.pc ( 31 downto 2 );
            WHEN "001" =>
                npc ( 31 downto 2 ) := r.m.ctrl.pc ( 31 downto 2 );
            WHEN "010" =>
                npc ( 31 downto 2 ) := r.e.ctrl.pc ( 31 downto 2 );
            WHEN "011" =>
                npc ( 31 downto 2 ) := r.a.ctrl.pc ( 31 downto 2 );
            WHEN OTHERS =>
                npc ( 31 downto 2 ) := r.d.pc ( 31 downto 2 );
        END CASE;
        RETURN ( npc );
    END;
    PROCEDURE mul_res (
        r : registers;
        asr18in : word;
        result : out word;
        y : out word;
        asr18 : out word;
        icc : out std_logic_vector ( 3 downto 0 )
    ) IS
        VARIABLE op : std_logic_vector ( 1 downto 0 );
        VARIABLE op3 : std_logic_vector ( 5 downto 0 );
    BEGIN
        op := r.m.ctrl.inst ( 31 downto 30 );
        op3 := r.m.ctrl.inst ( 24 downto 19 );
        result := r.m.result;
        y := r.m.y;
        icc := r.m.icc;
        asr18 := asr18in;
        CASE op IS
            WHEN FMT3 =>
                CASE op3 IS
                    WHEN UMUL | SMUL =>
                        result := mulo.result ( 31 downto 0 );
                        y := mulo.result ( 63 downto 32 );
                    WHEN UMULCC | SMULCC =>
                        result := mulo.result ( 31 downto 0 );
                        icc := mulo.icc;
                        y := mulo.result ( 63 downto 32 );
                    WHEN UMAC | SMAC =>
                        NULL;
                    WHEN UDIV | SDIV =>
                        result := divo.result ( 31 downto 0 );
                    WHEN UDIVCC | SDIVCC =>
                        result := divo.result ( 31 downto 0 );
                        icc := divo.icc;
                    WHEN OTHERS =>
                        NULL;
                END CASE;
            WHEN OTHERS =>
                NULL;
        END CASE;
    END;
    FUNCTION powerdwn (
        r : registers;
        trap : std_ulogic;
        rp : pwd_register_type
    ) RETURN std_ulogic IS
        VARIABLE op : std_logic_vector ( 1 downto 0 );
        VARIABLE op3 : std_logic_vector ( 5 downto 0 );
        VARIABLE rd : std_logic_vector ( 4 downto 0 );
        VARIABLE pd : std_ulogic;
    BEGIN
        op := r.x.ctrl.inst ( 31 downto 30 );
        op3 := r.x.ctrl.inst ( 24 downto 19 );
        rd := r.x.ctrl.inst ( 29 downto 25 );
        pd := '0';
        IF ( not ( r.x.ctrl.annul or trap ) and r.x.ctrl.pv ) = '1' THEN
            IF ( ( op = FMT3 ) and ( op3 = WRY ) and ( rd = "10011" ) ) THEN
                pd := '1';
            END IF;
            pd := pd or rp.pwd;
        END IF;
        RETURN ( pd );
    END;
    SIGNAL dummy : std_ulogic;
    SIGNAL cpu_index : std_logic_vector ( 3 downto 0 );
    SIGNAL disasen : std_ulogic;
BEGIN
    comb : PROCESS ( ico , dco , rfo , r , wpr , ir , dsur , rstn , holdn , irqi , dbgi , fpo , cpo , tbo , mulo , divo , dummy , rp )
        VARIABLE v : registers;
        VARIABLE vp : pwd_register_type;
        VARIABLE vwpr : watchpoint_registers;
        VARIABLE vdsu : dsu_registers;
        VARIABLE npc : std_logic_vector ( 31 downto 2 );
        VARIABLE de_raddr1 : std_logic_vector ( 9 downto 0 );
        VARIABLE de_raddr2 : std_logic_vector ( 9 downto 0 );
        VARIABLE de_rs2 : std_logic_vector ( 4 downto 0 );
        VARIABLE de_rd : std_logic_vector ( 4 downto 0 );
        VARIABLE de_hold_pc : std_ulogic;
        VARIABLE de_branch : std_ulogic;
        VARIABLE de_fpop : std_ulogic;
        VARIABLE de_ldlock : std_ulogic;
        VARIABLE de_cwp : cwptype;
        VARIABLE de_cwp2 : cwptype;
        VARIABLE de_inull : std_ulogic;
        VARIABLE de_ren1 : std_ulogic;
        VARIABLE de_ren2 : std_ulogic;
        VARIABLE de_wcwp : std_ulogic;
        VARIABLE de_inst : word;
        VARIABLE de_branch_address : pctype;
        VARIABLE de_icc : std_logic_vector ( 3 downto 0 );
        VARIABLE de_fbranch : std_ulogic;
        VARIABLE de_cbranch : std_ulogic;
        VARIABLE de_rs1mod : std_ulogic;
        VARIABLE ra_op1 : word;
        VARIABLE ra_op2 : word;
        VARIABLE ra_div : std_ulogic;
        VARIABLE ex_jump : std_ulogic;
        VARIABLE ex_link_pc : std_ulogic;
        VARIABLE ex_jump_address : pctype;
        VARIABLE ex_add_res : std_logic_vector ( 32 downto 0 );
        VARIABLE ex_shift_res : word;
        VARIABLE ex_logic_res : word;
        VARIABLE ex_misc_res : word;
        VARIABLE ex_edata : word;
        VARIABLE ex_edata2 : word;
        VARIABLE ex_dci : dc_in_type;
        VARIABLE ex_force_a2 : std_ulogic;
        VARIABLE ex_load : std_ulogic;
        VARIABLE ex_ymsb : std_ulogic;
        VARIABLE ex_op1 : word;
        VARIABLE ex_op2 : word;
        VARIABLE ex_result : word;
        VARIABLE ex_result2 : word;
        VARIABLE mul_op2 : word;
        VARIABLE ex_shcnt : std_logic_vector ( 4 downto 0 );
        VARIABLE ex_dsuen : std_ulogic;
        VARIABLE ex_ldbp2 : std_ulogic;
        VARIABLE ex_sari : std_ulogic;
        VARIABLE me_inull : std_ulogic;
        VARIABLE me_nullify : std_ulogic;
        VARIABLE me_nullify2 : std_ulogic;
        VARIABLE me_iflush : std_ulogic;
        VARIABLE me_newtt : std_logic_vector ( 5 downto 0 );
        VARIABLE me_asr18 : word;
        VARIABLE me_signed : std_ulogic;
        VARIABLE me_size : std_logic_vector ( 1 downto 0 );
        VARIABLE me_laddr : std_logic_vector ( 1 downto 0 );
        VARIABLE me_icc : std_logic_vector ( 3 downto 0 );
        VARIABLE xc_result : word;
        VARIABLE xc_df_result : word;
        VARIABLE xc_waddr : std_logic_vector ( 9 downto 0 );
        VARIABLE xc_exception : std_ulogic;
        VARIABLE xc_wreg : std_ulogic;
        VARIABLE xc_trap_address : pctype;
        VARIABLE xc_vectt : std_logic_vector ( 7 downto 0 );
        VARIABLE xc_trap : std_ulogic;
        VARIABLE xc_fpexack : std_ulogic;
        VARIABLE xc_rstn : std_ulogic;
        VARIABLE xc_halt : std_ulogic;
        VARIABLE diagdata : word;
        VARIABLE tbufi : tracebuf_in_type;
        VARIABLE dbgm : std_ulogic;
        VARIABLE fpcdbgwr : std_ulogic;
        VARIABLE vfpi : fpc_in_type;
        VARIABLE dsign : std_ulogic;
        VARIABLE pwrd : std_ulogic;
        VARIABLE sidle : std_ulogic;
        VARIABLE vir : irestart_register;
        VARIABLE icnt : std_ulogic;
        VARIABLE tbufcntx : std_logic_vector ( 10 + LOG2 ( 2 ) - 4 - 1 downto 0 );
    BEGIN
        v := r;
        vwpr := wpr;
        vdsu := dsur;
        vp := rp;
        xc_fpexack := '0';
        sidle := '0';
        fpcdbgwr := '0';
        vir := ir;
        xc_rstn := rstn;
        xc_exception := '0';
        xc_halt := '0';
        icnt := '0';
        xc_waddr := ( OTHERS => '0' );
        xc_waddr ( LOG2 ( 8 + 1 ) + 4 - 1 downto 0 ) := r.x.ctrl.rd ( LOG2 ( 8 + 1 ) + 4 - 1 downto 0 );
        xc_trap := r.x.mexc or r.x.ctrl.trap;
        v.x.nerror := rp.error;
        IF r.x.mexc = '1' THEN
            xc_vectt := "00" & TT_DAEX;
        ELSIF r.x.ctrl.tt = TT_TICC THEN
            xc_vectt := '1' & r.x.result ( 6 downto 0 );
        ELSE
            xc_vectt := "00" & r.x.ctrl.tt;
        END IF;
        IF r.w.s.svt = '0' THEN
            xc_trap_address ( 31 downto 4 ) := r.w.s.tba & xc_vectt;
        ELSE
            xc_trap_address ( 31 downto 4 ) := r.w.s.tba & "00000000";
        END IF;
        xc_trap_address ( 3 downto 2 ) := ( OTHERS => '0' );
        xc_wreg := '0';
        v.x.annul_all := '0';
        IF ( r.x.ctrl.ld = '1' ) THEN
            xc_result := r.x.data ( 0 );
        ELSE
            xc_result := r.x.result;
        END IF;
        xc_df_result := xc_result;
        dbgm := dbgexc ( r , dbgi , xc_trap , xc_vectt );
        IF ( dbgi.dsuen and dbgi.dbreak ) = '0' THEN
            v.x.debug := '0';
        END IF;
        pwrd := '0';
        CASE r.x.rstate IS
            WHEN run =>
                IF ( not r.x.ctrl.annul and r.x.ctrl.pv and not r.x.debug ) = '1' THEN
                    icnt := holdn;
                END IF;
                IF dbgm = '1' THEN
                    v.x.annul_all := '1';
                    vir.addr := r.x.ctrl.pc;
                    v.x.rstate := dsu1;
                    v.x.debug := '1';
                    v.x.npc := npc_find ( r );
                    vdsu.tt := xc_vectt;
                    vdsu.err := dbgerr ( r , dbgi , xc_vectt );
                ELSIF ( pwrd = '1' ) and ( ir.pwd = '0' ) THEN
                    v.x.annul_all := '1';
                    vir.addr := r.x.ctrl.pc;
                    v.x.rstate := dsu1;
                    v.x.npc := npc_find ( r );
                    vp.pwd := '1';
                ELSIF ( r.x.ctrl.annul or xc_trap ) = '0' THEN
                    xc_wreg := r.x.ctrl.wreg;
                    sp_write ( r , wpr , v.w.s , vwpr );
                    vir.pwd := '0';
                ELSIF ( ( not r.x.ctrl.annul ) and xc_trap ) = '1' THEN
                    xc_exception := '1';
                    xc_result := r.x.ctrl.pc ( 31 downto 2 ) & "00";
                    xc_wreg := '1';
                    v.w.s.tt := xc_vectt;
                    v.w.s.ps := r.w.s.s;
                    v.w.s.s := '1';
                    v.x.annul_all := '1';
                    v.x.rstate := trap;
                    xc_waddr := ( OTHERS => '0' );
                    xc_waddr ( LOG2 ( 8 ) + 3 downto 0 ) := r.w.s.cwp & "0001";
                    v.x.npc := npc_find ( r );
                    fpexack ( r , xc_fpexack );
                    IF r.w.s.et = '0' THEN
                        xc_wreg := '0';
                    END IF;
                END IF;
            WHEN trap =>
                xc_result := npc_gen ( r );
                xc_wreg := '1';
                xc_waddr := ( OTHERS => '0' );
                xc_waddr ( LOG2 ( 8 ) + 3 downto 0 ) := r.w.s.cwp & "0010";
                IF ( r.w.s.et = '1' ) THEN
                    v.w.s.et := '0';
                    v.x.rstate := run;
                    v.w.s.cwp := r.w.s.cwp - 1;
                ELSE
                    v.x.rstate := dsu1;
                    xc_wreg := '0';
                    vp.error := '1';
                END IF;
            WHEN dsu1 =>
                xc_exception := '1';
                v.x.annul_all := '1';
                xc_trap_address ( 31 downto 2 ) := r.f.pc;
                xc_trap_address ( 31 downto 2 ) := ir.addr;
                vir.addr := npc_gen ( r ) ( 31 downto 2 );
                v.x.rstate := dsu2;
                v.x.debug := r.x.debug;
            WHEN dsu2 =>
                xc_exception := '1';
                v.x.annul_all := '1';
                xc_trap_address ( 31 downto 2 ) := r.f.pc;
                sidle := ( rp.pwd or rp.error ) and ico.idle and dco.idle and not r.x.debug;
                IF dbgi.reset = '1' THEN
                    vp.pwd := '0';
                    vp.error := '0';
                END IF;
                IF ( dbgi.dsuen and dbgi.dbreak ) = '1' THEN
                    v.x.debug := '1';
                END IF;
                diagwr ( r , dsur , ir , dbgi , wpr , v.w.s , vwpr , vdsu.asi , xc_trap_address , vir.addr , vdsu.tbufcnt , xc_wreg , xc_waddr , xc_result , fpcdbgwr );
                xc_halt := dbgi.halt;
                IF r.x.ipend = '1' THEN
                    vp.pwd := '0';
                END IF;
                IF ( rp.error or rp.pwd or r.x.debug or xc_halt ) = '0' THEN
                    v.x.rstate := run;
                    v.x.annul_all := '0';
                    vp.error := '0';
                    xc_trap_address ( 31 downto 2 ) := ir.addr;
                    v.x.debug := '0';
                    vir.pwd := '1';
                END IF;
            WHEN OTHERS =>
                NULL;
        END CASE;
        irq_intack ( r , holdn , v.x.intack );
        itrace ( r , dsur , vdsu , xc_result , xc_exception , dbgi , rp.error , xc_trap , tbufcntx , tbufi );
        vdsu.tbufcnt := tbufcntx;
        v.w.except := xc_exception;
        v.w.result := xc_result;
        IF ( r.x.rstate = dsu2 ) THEN
            v.w.except := '0';
        END IF;
        v.w.wa := xc_waddr ( LOG2 ( 8 + 1 ) + 4 - 1 downto 0 );
        v.w.wreg := xc_wreg and holdn;
        rfi.wdata <= xc_result;
        rfi.waddr <= xc_waddr;
        rfi.wren <= ( xc_wreg and holdn ) and not dco.scanen;
        irqo.intack <= r.x.intack and holdn;
        irqo.irl <= r.w.s.tt ( 3 downto 0 );
        irqo.pwd <= rp.pwd;
        dbgo.halt <= xc_halt;
        dbgo.pwd <= rp.pwd;
        dbgo.idle <= sidle;
        dbgo.icnt <= icnt;
        dci.intack <= r.x.intack and holdn;
        IF ( xc_rstn = '0' ) THEN
            v.w.except := '0';
            v.w.s.et := '0';
            v.w.s.svt := '0';
            v.w.s.dwt := '0';
            v.x.annul_all := '1';
            v.x.rstate := run;
            vir.pwd := '0';
            vp.pwd := '0';
            v.x.debug := '0';
            v.x.nerror := '0';
            v.w.s.tt := ( OTHERS => '0' );
            IF ( dbgi.dsuen and dbgi.dbreak ) = '1' THEN
                v.x.rstate := dsu1;
                v.x.debug := '1';
            END IF;
        END IF;
        v.w.s.ef := '0';
        v.x.ctrl := r.m.ctrl;
        v.x.dci := r.m.dci;
        v.x.ctrl.rett := r.m.ctrl.rett and not r.m.ctrl.annul;
        v.x.mac := r.m.mac;
        v.x.laddr := r.m.result ( 1 downto 0 );
        v.x.ctrl.annul := r.m.ctrl.annul or v.x.annul_all;
        mul_res ( r , v.w.s.asr18 , v.x.result , v.x.y , me_asr18 , me_icc );
        mem_trap ( r , wpr , v.x.ctrl.annul , holdn , v.x.ctrl.trap , me_iflush , me_nullify , v.m.werr , v.x.ctrl.tt );
        me_newtt := v.x.ctrl.tt;
        irq_trap ( r , ir , irqi.irl , v.x.ctrl.annul , v.x.ctrl.pv , v.x.ctrl.trap , me_newtt , me_nullify , v.m.irqen , v.m.irqen2 , me_nullify2 , v.x.ctrl.trap , v.x.ipend , v.x.ctrl.tt );
        IF ( r.m.ctrl.ld or not dco.mds ) = '1' THEN
            v.x.data ( 0 ) := dco.data ( 0 );
            v.x.data ( 1 ) := dco.data ( 1 );
            v.x.set := dco.set ( LOG2X ( 2 ) - 1 downto 0 );
            IF dco.mds = '0' THEN
                me_size := r.x.dci.size;
                me_laddr := r.x.laddr;
                me_signed := r.x.dci.signed;
            ELSE
                me_size := v.x.dci.size;
                me_laddr := v.x.laddr;
                me_signed := v.x.dci.signed;
            END IF;
            v.x.data ( 0 ) := ld_align ( v.x.data , v.x.set , me_size , me_laddr , me_signed );
        END IF;
        v.x.mexc := dco.mexc;
        v.x.impwp := '0';
        v.x.icc := me_icc;
        v.x.ctrl.wicc := r.m.ctrl.wicc and not v.x.annul_all;
        IF ( r.x.rstate = dsu2 ) THEN
            me_nullify2 := '0';
            v.x.set := dco.set ( LOG2X ( 2 ) - 1 downto 0 );
        END IF;
        dci.maddress <= r.m.result;
        dci.enaddr <= r.m.dci.enaddr;
        dci.asi <= r.m.dci.asi;
        dci.size <= r.m.dci.size;
        dci.nullify <= me_nullify2;
        dci.lock <= r.m.dci.lock and not r.m.ctrl.annul;
        dci.read <= r.m.dci.read;
        dci.write <= r.m.dci.write;
        dci.flush <= me_iflush;
        dci.dsuen <= r.m.dci.dsuen;
        dci.msu <= r.m.su;
        dci.esu <= r.e.su;
        dbgo.ipend <= v.x.ipend;
        v.m.ctrl := r.e.ctrl;
        ex_op1 := r.e.op1;
        ex_op2 := r.e.op2;
        v.m.ctrl.rett := r.e.ctrl.rett and not r.e.ctrl.annul;
        v.m.ctrl.wreg := r.e.ctrl.wreg and not v.x.annul_all;
        ex_ymsb := r.e.ymsb;
        mul_op2 := ex_op2;
        ex_shcnt := r.e.shcnt;
        v.e.cwp := r.a.cwp;
        ex_sari := r.e.sari;
        v.m.su := r.e.su;
        v.m.mul := '0';
        IF r.e.ldbp1 = '1' THEN
            ex_op1 := r.x.data ( 0 );
            ex_sari := r.x.data ( 0 ) ( 31 ) and r.e.ctrl.inst ( 19 ) and r.e.ctrl.inst ( 20 );
        END IF;
        IF r.e.ldbp2 = '1' THEN
            ex_op2 := r.x.data ( 0 );
            ex_ymsb := r.x.data ( 0 ) ( 0 );
            mul_op2 := ex_op2;
            ex_shcnt := r.x.data ( 0 ) ( 4 downto 0 );
            IF r.e.invop2 = '1' THEN
                ex_op2 := not ex_op2;
                ex_shcnt := not ex_shcnt;
            END IF;
        END IF;
        ex_add_res := ( ex_op1 & '1' ) + ( ex_op2 & r.e.alucin );
        IF ex_add_res ( 2 downto 1 ) = "00" THEN
            v.m.nalign := '0';
        ELSE
            v.m.nalign := '1';
        END IF;
        dcache_gen ( r , v , ex_dci , ex_link_pc , ex_jump , ex_force_a2 , ex_load );
        ex_jump_address := ex_add_res ( 32 downto 2 + 1 );
        logic_op ( r , ex_op1 , ex_op2 , v.x.y , ex_ymsb , ex_logic_res , v.m.y );
        ex_shift_res := shift ( r , ex_op1 , ex_op2 , ex_shcnt , ex_sari );
        misc_op ( r , wpr , ex_op1 , ex_op2 , xc_df_result , v.x.y , ex_misc_res , ex_edata );
        ex_add_res ( 3 ) := ex_add_res ( 3 ) or ex_force_a2;
        alu_select ( r , ex_add_res , ex_op1 , ex_op2 , ex_shift_res , ex_logic_res , ex_misc_res , ex_result , me_icc , v.m.icc , v.m.divz );
        dbg_cache ( holdn , dbgi , r , dsur , ex_result , ex_dci , ex_result2 , v.m.dci );
        fpstdata ( r , ex_edata , ex_result2 , fpo.data , ex_edata2 , v.m.result );
        cwp_ex ( r , v.m.wcwp );
        v.m.ctrl.annul := v.m.ctrl.annul or v.x.annul_all;
        v.m.ctrl.wicc := r.e.ctrl.wicc and not v.x.annul_all;
        v.m.mac := r.e.mac;
        IF ( r.x.rstate = dsu2 ) THEN
            v.m.ctrl.ld := '1';
        END IF;
        dci.eenaddr <= v.m.dci.enaddr;
        dci.eaddress <= ex_add_res ( 32 downto 1 );
        dci.edata <= ex_edata2;
        v.e.ctrl := r.a.ctrl;
        v.e.jmpl := r.a.jmpl;
        v.e.ctrl.annul := r.a.ctrl.annul or v.x.annul_all;
        v.e.ctrl.rett := r.a.ctrl.rett and not r.a.ctrl.annul;
        v.e.ctrl.wreg := r.a.ctrl.wreg and not v.x.annul_all;
        v.e.su := r.a.su;
        v.e.et := r.a.et;
        v.e.ctrl.wicc := r.a.ctrl.wicc and not v.x.annul_all;
        exception_detect ( r , wpr , dbgi , r.a.ctrl.trap , r.a.ctrl.tt , v.e.ctrl.trap , v.e.ctrl.tt );
        op_mux ( r , rfo.data1 , v.m.result , v.x.result , xc_df_result , zero32 , r.a.rsel1 , v.e.ldbp1 , ra_op1 );
        op_mux ( r , rfo.data2 , v.m.result , v.x.result , xc_df_result , r.a.imm , r.a.rsel2 , ex_ldbp2 , ra_op2 );
        alu_op ( r , ra_op1 , ra_op2 , v.m.icc , v.m.y ( 0 ) , ex_ldbp2 , v.e.op1 , v.e.op2 , v.e.aluop , v.e.alusel , v.e.aluadd , v.e.shcnt , v.e.sari , v.e.shleft , v.e.ymsb , v.e.mul , ra_div , v.e.mulstep , v.e.mac , v.e.ldbp2 , v.e.invop2 );
        cin_gen ( r , v.m.icc ( 0 ) , v.e.alucin );
        de_inst := r.d.inst ( conv_integer ( r.d.set ) );
        de_icc := r.m.icc;
        v.a.cwp := r.d.cwp;
        su_et_select ( r , v.w.s.ps , v.w.s.s , v.w.s.et , v.a.su , v.a.et );
        wicc_y_gen ( de_inst , v.a.ctrl.wicc , v.a.ctrl.wy );
        cwp_ctrl ( r , v.w.s.wim , de_inst , de_cwp , v.a.wovf , v.a.wunf , de_wcwp );
        rs1_gen ( r , de_inst , v.a.rs1 , de_rs1mod );
        de_rs2 := de_inst ( 4 downto 0 );
        de_raddr1 := ( OTHERS => '0' );
        de_raddr2 := ( OTHERS => '0' );
        IF de_rs1mod = '1' THEN
            regaddr ( r.d.cwp , de_inst ( 29 downto 26 ) & v.a.rs1 ( 0 ) , de_raddr1 ( LOG2 ( 8 + 1 ) + 4 - 1 downto 0 ) );
        ELSE
            regaddr ( r.d.cwp , de_inst ( 18 downto 15 ) & v.a.rs1 ( 0 ) , de_raddr1 ( LOG2 ( 8 + 1 ) + 4 - 1 downto 0 ) );
        END IF;
        regaddr ( r.d.cwp , de_rs2 , de_raddr2 ( LOG2 ( 8 + 1 ) + 4 - 1 downto 0 ) );
        v.a.rfa1 := de_raddr1 ( LOG2 ( 8 + 1 ) + 4 - 1 downto 0 );
        v.a.rfa2 := de_raddr2 ( LOG2 ( 8 + 1 ) + 4 - 1 downto 0 );
        rd_gen ( r , de_inst , v.a.ctrl.wreg , v.a.ctrl.ld , de_rd );
        regaddr ( de_cwp , de_rd , v.a.ctrl.rd );
        fpbranch ( de_inst , fpo.cc , de_fbranch );
        fpbranch ( de_inst , cpo.cc , de_cbranch );
        v.a.imm := imm_data ( r , de_inst );
        lock_gen ( r , de_rs2 , de_rd , v.a.rfa1 , v.a.rfa2 , v.a.ctrl.rd , de_inst , fpo.ldlock , v.e.mul , ra_div , v.a.ldcheck1 , v.a.ldcheck2 , de_ldlock , v.a.ldchkra , v.a.ldchkex );
        ic_ctrl ( r , de_inst , v.x.annul_all , de_ldlock , branch_true ( de_icc , de_inst ) , de_fbranch , de_cbranch , fpo.ccv , cpo.ccv , v.d.cnt , v.d.pc , de_branch , v.a.ctrl.annul , v.d.annul , v.a.jmpl , de_inull , v.d.pv , v.a.ctrl.pv , de_hold_pc , v.a.ticc , v.a.ctrl.rett , v.a.mulstart , v.a.divstart );
        cwp_gen ( r , v , v.a.ctrl.annul , de_wcwp , de_cwp , v.d.cwp );
        v.d.inull := ra_inull_gen ( r , v );
        op_find ( r , v.a.ldchkra , v.a.ldchkex , v.a.rs1 , v.a.rfa1 , false , v.a.rfe1 , v.a.rsel1 , v.a.ldcheck1 );
        op_find ( r , v.a.ldchkra , v.a.ldchkex , de_rs2 , v.a.rfa2 , imm_select ( de_inst ) , v.a.rfe2 , v.a.rsel2 , v.a.ldcheck2 );
        de_branch_address := branch_address ( de_inst , r.d.pc );
        v.a.ctrl.annul := v.a.ctrl.annul or v.x.annul_all;
        v.a.ctrl.wicc := v.a.ctrl.wicc and not v.a.ctrl.annul;
        v.a.ctrl.wreg := v.a.ctrl.wreg and not v.a.ctrl.annul;
        v.a.ctrl.rett := v.a.ctrl.rett and not v.a.ctrl.annul;
        v.a.ctrl.wy := v.a.ctrl.wy and not v.a.ctrl.annul;
        v.a.ctrl.trap := r.d.mexc;
        v.a.ctrl.tt := "000000";
        v.a.ctrl.inst := de_inst;
        v.a.ctrl.pc := r.d.pc;
        v.a.ctrl.cnt := r.d.cnt;
        v.a.step := r.d.step;
        IF holdn = '0' THEN
            de_raddr1 ( LOG2 ( 8 + 1 ) + 4 - 1 downto 0 ) := r.a.rfa1;
            de_raddr2 ( LOG2 ( 8 + 1 ) + 4 - 1 downto 0 ) := r.a.rfa2;
            de_ren1 := r.a.rfe1;
            de_ren2 := r.a.rfe2;
        ELSE
            de_ren1 := v.a.rfe1;
            de_ren2 := v.a.rfe2;
        END IF;
        IF ( ( dbgi.denable and not dbgi.dwrite ) = '1' ) and ( r.x.rstate = dsu2 ) THEN
            de_raddr1 ( LOG2 ( 8 + 1 ) + 4 - 1 downto 0 ) := dbgi.daddr ( LOG2 ( 8 + 1 ) + 4 + 1 downto 2 );
            de_ren1 := '1';
        END IF;
        v.d.step := dbgi.step and not r.d.annul;
        rfi.raddr1 <= de_raddr1;
        rfi.raddr2 <= de_raddr2;
        rfi.ren1 <= de_ren1 and not dco.scanen;
        rfi.ren2 <= de_ren2 and not dco.scanen;
        rfi.diag <= dco.testen & "000";
        ici.inull <= de_inull;
        ici.flush <= me_iflush;
        IF ( xc_rstn = '0' ) THEN
            v.d.cnt := ( OTHERS => '0' );
        END IF;
        npc := r.f.pc;
        IF ( xc_rstn = '0' ) THEN
            v.f.pc := ( OTHERS => '0' );
            v.f.branch := '0';
            v.f.pc ( 31 downto 12 ) := conv_std_logic_vector ( 16#00000# , 20 );
        ELSIF xc_exception = '1' THEN
            v.f.branch := '1';
            v.f.pc := xc_trap_address;
            npc := v.f.pc;
        ELSIF de_hold_pc = '1' THEN
            v.f.pc := r.f.pc;
            v.f.branch := r.f.branch;
            IF ex_jump = '1' THEN
                v.f.pc := ex_jump_address;
                v.f.branch := '1';
                npc := v.f.pc;
            END IF;
        ELSIF ex_jump = '1' THEN
            v.f.pc := ex_jump_address;
            v.f.branch := '1';
            npc := v.f.pc;
        ELSIF de_branch = '1' THEN
            v.f.pc := branch_address ( de_inst , r.d.pc );
            v.f.branch := '1';
            npc := v.f.pc;
        ELSE
            v.f.branch := '0';
            v.f.pc ( 31 downto 2 ) := r.f.pc ( 31 downto 2 ) + 1;
            npc := v.f.pc;
        END IF;
        ici.dpc <= r.d.pc ( 31 downto 2 ) & "00";
        ici.fpc <= r.f.pc ( 31 downto 2 ) & "00";
        ici.rpc <= npc ( 31 downto 2 ) & "00";
        ici.fbranch <= r.f.branch;
        ici.rbranch <= v.f.branch;
        ici.su <= v.a.su;
        ici.fline <= ( OTHERS => '0' );
        ici.flushl <= '0';
        IF ( ico.mds and de_hold_pc ) = '0' THEN
            v.d.inst ( 0 ) := ico.data ( 0 );
            v.d.inst ( 1 ) := ico.data ( 1 );
            v.d.set := ico.set ( LOG2X ( 2 ) - 1 downto 0 );
            v.d.mexc := ico.mexc;
        END IF;
        diagread ( dbgi , r , dsur , ir , wpr , rfo.data1 , dco , tbo , diagdata );
        diagrdy ( dbgi.denable , dsur , r.m.dci , dco.mds , ico , vdsu.crdy );
        rin <= v;
        wprin <= vwpr;
        dsuin <= vdsu;
        irin <= vir;
        muli.start <= r.a.mulstart and not r.a.ctrl.annul;
        muli.signed <= r.e.ctrl.inst ( 19 );
        muli.op1 <= ( ex_op1 ( 31 ) and r.e.ctrl.inst ( 19 ) ) & ex_op1;
        muli.op2 <= ( mul_op2 ( 31 ) and r.e.ctrl.inst ( 19 ) ) & mul_op2;
        muli.mac <= r.e.ctrl.inst ( 24 );
        muli.acc ( 39 downto 32 ) <= r.x.y ( 7 downto 0 );
        muli.acc ( 31 downto 0 ) <= r.w.s.asr18;
        muli.flush <= r.x.annul_all;
        divi.start <= r.a.divstart and not r.a.ctrl.annul;
        divi.signed <= r.e.ctrl.inst ( 19 );
        divi.flush <= r.x.annul_all;
        divi.op1 <= ( ex_op1 ( 31 ) and r.e.ctrl.inst ( 19 ) ) & ex_op1;
        divi.op2 <= ( ex_op2 ( 31 ) and r.e.ctrl.inst ( 19 ) ) & ex_op2;
        IF ( r.a.divstart and not r.a.ctrl.annul ) = '1' THEN
            dsign := r.a.ctrl.inst ( 19 );
        ELSE
            dsign := r.e.ctrl.inst ( 19 );
        END IF;
        divi.y <= ( r.m.y ( 31 ) and dsign ) & r.m.y;
        rpin <= vp;
        dbgo.dsu <= '1';
        dbgo.dsumode <= r.x.debug;
        dbgo.crdy <= dsur.crdy ( 2 );
        dbgo.data <= diagdata;
        tbi <= tbufi;
        dbgo.error <= dummy and not r.x.nerror;
    END PROCESS;
    preg : PROCESS ( sclk )
    BEGIN
        IF rising_edge ( sclk ) THEN
            rp <= rpin;
            IF rstn = '0' THEN
                rp.error <= '0';
            END IF;
        END IF;
    END PROCESS;
    reg : PROCESS ( clk )
    BEGIN
        IF rising_edge ( clk ) THEN
            IF ( holdn = '1' ) THEN
                r <= rin;
            ELSE
                r.x.ipend <= rin.x.ipend;
                r.m.werr <= rin.m.werr;
                IF ( holdn or ico.mds ) = '0' THEN
                    r.d.inst <= rin.d.inst;
                    r.d.mexc <= rin.d.mexc;
                    r.d.set <= rin.d.set;
                END IF;
                IF ( holdn or dco.mds ) = '0' THEN
                    r.x.data <= rin.x.data;
                    r.x.mexc <= rin.x.mexc;
                    r.x.impwp <= rin.x.impwp;
                    r.x.set <= rin.x.set;
                END IF;
            END IF;
            IF rstn = '0' THEN
                r.x.error <= '0';
                r.w.s.s <= '1';
            END IF;
        END IF;
    END PROCESS;
    dsureg : PROCESS ( clk )
    BEGIN
        IF rising_edge ( clk ) THEN
            IF holdn = '1' THEN
                dsur <= dsuin;
            ELSE
                dsur.crdy <= dsuin.crdy;
            END IF;
        END IF;
    END PROCESS;
    dsureg2 : PROCESS ( clk )
    BEGIN
        IF rising_edge ( clk ) THEN
            IF holdn = '1' THEN
                ir <= irin;
            END IF;
        END IF;
    END PROCESS;
    wpreg0 : PROCESS ( clk )
    BEGIN
        IF rising_edge ( clk ) THEN
            IF holdn = '1' THEN
                wpr ( 0 ) <= wprin ( 0 );
            END IF;
            IF rstn = '0' THEN
                wpr ( 0 ).exec <= '0';
                wpr ( 0 ).load <= '0';
                wpr ( 0 ).store <= '0';
            END IF;
        END IF;
    END PROCESS;
    wpreg1 : PROCESS ( clk )
    BEGIN
        IF rising_edge ( clk ) THEN
            IF holdn = '1' THEN
                wpr ( 1 ) <= wprin ( 1 );
            END IF;
            IF rstn = '0' THEN
                wpr ( 1 ).exec <= '0';
                wpr ( 1 ).load <= '0';
                wpr ( 1 ).store <= '0';
            END IF;
        END IF;
    END PROCESS;
    wpr ( 2 ) <= ( ZERO32 ( 31 DOWNTO 2 ) , ZERO32 ( 31 DOWNTO 2 ) , '0' , '0' , '0' , '0' );
    wpr ( 3 ) <= ( ZERO32 ( 31 DOWNTO 2 ) , ZERO32 ( 31 DOWNTO 2 ) , '0' , '0' , '0' , '0' );
    dummy <= '1';
END ARCHITECTURE;
