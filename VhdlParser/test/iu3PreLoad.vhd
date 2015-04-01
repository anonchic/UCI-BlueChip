package iu3PreLoad is
    TYPE log2arr IS ARRAY(0 TO 512) OF integer;
--	CONSTANT log2   : log2arr := (
--		0,0,1,2,2,3,3,3,3,4,4,4,4,4,4,4,4,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,
--  		6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
--  		7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
--  		7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
--  		8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,
--  		8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,
--  		8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,
--  		8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,
--  		OTHERS => 9);
--	CONSTANT log2x  : log2arr := (
--		0,1,1,2,2,3,3,3,3,4,4,4,4,4,4,4,4,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,
--  		6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
--  		7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
--  		7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
--  		8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,
--  		8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,
--  		8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,
--  		8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,
--  		OTHERS => 9);
	CONSTANT NTECH : integer := 32;
	TYPE tech_ability_type IS ARRAY(0 TO NTECH) OF integer;	   
	CONSTANT inferred    : integer := 0;
	CONSTANT virtex      : integer := 1;
	CONSTANT virtex2     : integer := 2;
	CONSTANT memvirage   : integer := 3;
	CONSTANT axcel       : integer := 4;
	CONSTANT proasic     : integer := 5;
	CONSTANT atc18s      : integer := 6;
	CONSTANT altera      : integer := 7;
	CONSTANT umc         : integer := 8;
	CONSTANT rhumc       : integer := 9;
	CONSTANT apa3        : integer := 10;
	CONSTANT spartan3    : integer := 11;
	CONSTANT ihp25       : integer := 12; 
	CONSTANT rhlib18t    : integer := 13;
	CONSTANT virtex4     : integer := 14; 
	CONSTANT lattice     : integer := 15;
	CONSTANT ut25        : integer := 16;
	CONSTANT spartan3e   : integer := 17;
	CONSTANT peregrine   : integer := 18;
	CONSTANT memartisan  : integer := 19;
	CONSTANT virtex5     : integer := 20;
	CONSTANT custom1     : integer := 21;
	CONSTANT ihp25rh     : integer := 22; 
	CONSTANT stratix1    : integer := 23;
	CONSTANT stratix2    : integer := 24;
	CONSTANT eclipse     : integer := 25;
	CONSTANT stratix3    : integer := 26;
	CONSTANT cyclone3    : integer := 27;
	CONSTANT memvirage90 : integer := 28;
	CONSTANT tsmc90      : integer := 29;
	CONSTANT easic90    : integer := 30;
	CONSTANT atc18rha   : integer := 31;
	CONSTANT smic013     : integer := 32;
--	CONSTANT is_fpga : tech_ability_type :=
--		(inferred => 1,
--		virtex => 1,
--		virtex2 => 1,
--		axcel => 1,
--		proasic => 1,
--		altera => 1,
--		apa3 => 1,
--		spartan3 => 1,
--		virtex4 => 1,
--		lattice => 1,
--		spartan3e => 1,
--		virtex5 => 1,
--		stratix1 => 1,
--		stratix2 => 1,
--		eclipse => 1, 
--		stratix3 => 1,
--		cyclone3 => 1,
--		others => 0);				
	Constant zero32 : std_logic_vector(31 downto 0) := X"0000_0000";
	CONSTANT zero64 : std_logic_vector(63 downto 0) := X"0000_0000_0000_0000";
	CONSTANT one32  : std_logic_vector(31 downto 0) := X"FFFF_FFFF";
	-- op decoding (inst(31 downto 30))

subtype op_type is std_logic_vector(1 downto 0);

constant FMT2     : op_type := "00";
constant CALL     : op_type := "01";
constant FMT3     : op_type := "10";
constant LDST     : op_type := "11";

-- op2 decoding (inst(24 downto 22))

subtype op2_type is std_logic_vector(2 downto 0);

constant UNIMP    : op2_type := "000";
constant BICC     : op2_type := "010";
constant SETHI    : op2_type := "100";
constant FBFCC    : op2_type := "110";
constant CBCCC    : op2_type := "111";

-- op3 decoding (inst(24 downto 19))

subtype op3_type is std_logic_vector(5 downto 0);

constant IADD     : op3_type := "000000";
constant IAND     : op3_type := "000001";
constant IOR      : op3_type := "000010";
constant IXOR     : op3_type := "000011";
constant ISUB     : op3_type := "000100";
constant ANDN     : op3_type := "000101";
constant ORN      : op3_type := "000110";
constant IXNOR    : op3_type := "000111";
constant ADDX     : op3_type := "001000";
constant UMUL     : op3_type := "001010";
constant SMUL     : op3_type := "001011";
constant SUBX     : op3_type := "001100";
constant UDIV     : op3_type := "001110";
constant SDIV     : op3_type := "001111";
constant ADDCC    : op3_type := "010000";
constant ANDCC    : op3_type := "010001";
constant ORCC     : op3_type := "010010";
constant XORCC    : op3_type := "010011";
constant SUBCC    : op3_type := "010100";
constant ANDNCC   : op3_type := "010101";
constant ORNCC    : op3_type := "010110";
constant XNORCC   : op3_type := "010111";
constant ADDXCC   : op3_type := "011000";
constant UMULCC   : op3_type := "011010";
constant SMULCC   : op3_type := "011011";
constant SUBXCC   : op3_type := "011100";
constant UDIVCC   : op3_type := "011110";
constant SDIVCC   : op3_type := "011111";
constant TADDCC   : op3_type := "100000";
constant TSUBCC   : op3_type := "100001";
constant TADDCCTV : op3_type := "100010";
constant TSUBCCTV : op3_type := "100011";
constant MULSCC   : op3_type := "100100";
constant ISLL     : op3_type := "100101";
constant ISRL     : op3_type := "100110";
constant ISRA     : op3_type := "100111";
constant RDY      : op3_type := "101000";
constant RDPSR    : op3_type := "101001";
constant RDWIM    : op3_type := "101010";
constant RDTBR    : op3_type := "101011";
constant WRY      : op3_type := "110000";
constant WRPSR    : op3_type := "110001";
constant WRWIM    : op3_type := "110010";
constant WRTBR    : op3_type := "110011";
constant FPOP1    : op3_type := "110100";
constant FPOP2    : op3_type := "110101";
constant CPOP1    : op3_type := "110110";
constant CPOP2    : op3_type := "110111";
constant JMPL     : op3_type := "111000";
constant TICC     : op3_type := "111010";
constant FLUSH    : op3_type := "111011";
constant RETT     : op3_type := "111001";
constant SAVE     : op3_type := "111100";
constant RESTORE  : op3_type := "111101";
constant UMAC     : op3_type := "111110";
constant SMAC     : op3_type := "111111";

constant LD       : op3_type := "000000";
constant LDUB     : op3_type := "000001";
constant LDUH     : op3_type := "000010";
constant LDD      : op3_type := "000011";
constant LDSB     : op3_type := "001001";
constant LDSH     : op3_type := "001010";
constant LDSTUB   : op3_type := "001101";
constant SWAP     : op3_type := "001111";
constant LDA      : op3_type := "010000";
constant LDUBA    : op3_type := "010001";
constant LDUHA    : op3_type := "010010";
constant LDDA     : op3_type := "010011";
constant LDSBA    : op3_type := "011001";
constant LDSHA    : op3_type := "011010";
constant LDSTUBA  : op3_type := "011101";
constant SWAPA    : op3_type := "011111";
constant LDF      : op3_type := "100000";
constant LDFSR    : op3_type := "100001";
constant LDDF     : op3_type := "100011";
constant LDC      : op3_type := "110000";
constant LDCSR    : op3_type := "110001";
constant LDDC     : op3_type := "110011";
constant ST       : op3_type := "000100";
constant STB      : op3_type := "000101";
constant STH      : op3_type := "000110";
constant ISTD     : op3_type := "000111";
constant STA      : op3_type := "010100";
constant STBA     : op3_type := "010101";
constant STHA     : op3_type := "010110";
constant STDA     : op3_type := "010111";
constant STF      : op3_type := "100100";
constant STFSR    : op3_type := "100101";
constant STDFQ    : op3_type := "100110";
constant STDF     : op3_type := "100111";
constant STC      : op3_type := "110100";
constant STCSR    : op3_type := "110101";
constant STDCQ    : op3_type := "110110";
constant STDC     : op3_type := "110111";

-- bicc decoding (inst(27 downto 25))

constant BA  : std_logic_vector(3 downto 0) := "1000";

-- fpop1 decoding

subtype fpop_type is std_logic_vector(8 downto 0);

constant FITOS    : fpop_type := "011000100";
constant FITOD    : fpop_type := "011001000";
constant FSTOI    : fpop_type := "011010001";
constant FDTOI    : fpop_type := "011010010";
constant FSTOD    : fpop_type := "011001001";
constant FDTOS    : fpop_type := "011000110";
constant FMOVS    : fpop_type := "000000001";
constant FNEGS    : fpop_type := "000000101";
constant FABSS    : fpop_type := "000001001";
constant FSQRTS   : fpop_type := "000101001";
constant FSQRTD   : fpop_type := "000101010";
constant FADDS    : fpop_type := "001000001";
constant FADDD    : fpop_type := "001000010";
constant FSUBS    : fpop_type := "001000101";
constant FSUBD    : fpop_type := "001000110";
constant FMULS    : fpop_type := "001001001";
constant FMULD    : fpop_type := "001001010";
constant FSMULD   : fpop_type := "001101001";
constant FDIVS    : fpop_type := "001001101";
constant FDIVD    : fpop_type := "001001110";

-- fpop2 decoding

constant FCMPS    : fpop_type := "001010001";
constant FCMPD    : fpop_type := "001010010";
constant FCMPES   : fpop_type := "001010101";
constant FCMPED   : fpop_type := "001010110";

-- trap type decoding

subtype trap_type is std_logic_vector(5 downto 0);

constant TT_IAEX   : trap_type := "000001";
constant TT_IINST  : trap_type := "000010";
constant TT_PRIV   : trap_type := "000011";
constant TT_FPDIS  : trap_type := "000100";
constant TT_WINOF  : trap_type := "000101";
constant TT_WINUF  : trap_type := "000110";
constant TT_UNALA  : trap_type := "000111";
constant TT_FPEXC  : trap_type := "001000";
constant TT_DAEX   : trap_type := "001001";
constant TT_TAG    : trap_type := "001010";
constant TT_WATCH  : trap_type := "001011";

constant TT_DSU    : trap_type := "010000";
constant TT_PWD    : trap_type := "010001";

constant TT_RFERR  : trap_type := "100000";
constant TT_IAERR  : trap_type := "100001";
constant TT_CPDIS  : trap_type := "100100";
constant TT_CPEXC  : trap_type := "101000";
constant TT_DIV    : trap_type := "101010";
constant TT_DSEX   : trap_type := "101011";
constant TT_TICC   : trap_type := "111111";

-- Alternate address space identifiers (only 5 lsb bist are used)

subtype asi_type is std_logic_vector(4 downto 0);

constant ASI_SYSR    : asi_type := "00010"; -- 0x02
constant ASI_UINST   : asi_type := "01000"; -- 0x08
constant ASI_SINST   : asi_type := "01001"; -- 0x09
constant ASI_UDATA   : asi_type := "01010"; -- 0x0A
constant ASI_SDATA   : asi_type := "01011"; -- 0x0B
constant ASI_ITAG    : asi_type := "01100"; -- 0x0C
constant ASI_IDATA   : asi_type := "01101"; -- 0x0D
constant ASI_DTAG    : asi_type := "01110"; -- 0x0E
constant ASI_DDATA   : asi_type := "01111"; -- 0x0F
constant ASI_IFLUSH  : asi_type := "10000"; -- 0x10
constant ASI_DFLUSH  : asi_type := "10001"; -- 0x11

constant ASI_FLUSH_PAGE     : std_logic_vector(4 downto 0) := "10000";  -- 0x10 i/dcache flush page
constant ASI_FLUSH_CTX      : std_logic_vector(4 downto 0) := "10011";  -- 0x13 i/dcache flush ctx

constant ASI_DCTX           : std_logic_vector(4 downto 0) := "10100";  -- 0x14 dcache ctx
constant ASI_ICTX           : std_logic_vector(4 downto 0) := "10101";  -- 0x15 icache ctx

constant ASI_MMUFLUSHPROBE  : std_logic_vector(4 downto 0) := "11000";  -- 0x18 i/dtlb flush/(probe)
constant ASI_MMUREGS        : std_logic_vector(4 downto 0) := "11001";  -- 0x19 mmu regs access
constant ASI_MMU_BP         : std_logic_vector(4 downto 0) := "11100";  -- 0x1c mmu Bypass 
constant ASI_MMU_DIAG       : std_logic_vector(4 downto 0) := "11101";  -- 0x1d mmu diagnostic 
--constant ASI_MMU_DSU        : std_logic_vector(4 downto 0) := "11111";  -- 0x1f mmu diagnostic 

constant ASI_MMUSNOOP_DTAG  : std_logic_vector(4 downto 0) := "11110";  -- 0x1e mmusnoop physical dtag 

-- ftt decoding

subtype ftt_type is std_logic_vector(2 downto 0);

constant FPIEEE_ERR  : ftt_type := "001";
constant FPSEQ_ERR   : ftt_type := "100";
constant FPHW_ERR    : ftt_type := "101";
  	SUBTYPE cword IS std_logic_vector ( 32 - 1 downto 0 );
    TYPE cdatatype IS ARRAY ( 0 to 3 ) OF cword;
    TYPE cpartype IS ARRAY ( 0 to 3 ) OF std_logic_vector ( 3 downto 0 );
    TYPE iregfile_in_type IS RECORD
        raddr1 : std_logic_vector ( 9 downto 0 );
        raddr2 : std_logic_vector ( 9 downto 0 );
        waddr : std_logic_vector ( 9 downto 0 );
        wdata : std_logic_vector ( 31 downto 0 );
        ren1 : std_ulogic;
        ren2 : std_ulogic;
        wren : std_ulogic;
        diag : std_logic_vector ( 3 downto 0 );
    END RECORD;
    TYPE iregfile_out_type IS RECORD
        data1 : std_logic_vector ( 32 - 1 downto 0 );
        data2 : std_logic_vector ( 32 - 1 downto 0 );
    END RECORD;
    TYPE cctrltype IS RECORD
        burst : std_ulogic;
        dfrz : std_ulogic;
        ifrz : std_ulogic;
        dsnoop : std_ulogic;
        dcs : std_logic_vector ( 1 downto 0 );
        ics : std_logic_vector ( 1 downto 0 );
    END RECORD;
    TYPE icache_in_type IS RECORD
        rpc : std_logic_vector ( 31 downto 0 );
        fpc : std_logic_vector ( 31 downto 0 );
        dpc : std_logic_vector ( 31 downto 0 );
        rbranch : std_ulogic;
        fbranch : std_ulogic;
        inull : std_ulogic;
        su : std_ulogic;
        flush : std_ulogic;
        flushl : std_ulogic;
        fline : std_logic_vector ( 31 downto 3 );
        pnull : std_ulogic;
    END RECORD;
    TYPE icache_out_type IS RECORD
        data : cdatatype;
        set : std_logic_vector ( 1 downto 0 );
        mexc : std_ulogic;
        hold : std_ulogic;
        flush : std_ulogic;
        diagrdy : std_ulogic;
        diagdata : std_logic_vector ( 32 - 1 downto 0 );
        mds : std_ulogic;
        cfg : std_logic_vector ( 31 downto 0 );
        idle : std_ulogic;
    END RECORD;
    TYPE icdiag_in_type IS RECORD
        addr : std_logic_vector ( 31 downto 0 );
        enable : std_ulogic;
        read : std_ulogic;
        tag : std_ulogic;
        ctx : std_ulogic;
        flush : std_ulogic;
        ilramen : std_ulogic;
        cctrl : cctrltype;
        pflush : std_ulogic;
        pflushaddr : std_logic_vector ( 31 downto 11 );
        pflushtyp : std_ulogic;
        ilock : std_logic_vector ( 0 to 3 );
        scanen : std_ulogic;
    END RECORD;
    TYPE dcache_in_type IS RECORD
        asi : std_logic_vector ( 7 downto 0 );
        maddress : std_logic_vector ( 31 downto 0 );
        eaddress : std_logic_vector ( 31 downto 0 );
        edata : std_logic_vector ( 31 downto 0 );
        size : std_logic_vector ( 1 downto 0 );
        enaddr : std_ulogic;
        eenaddr : std_ulogic;
        nullify : std_ulogic;
        lock : std_ulogic;
        read : std_ulogic;
        write : std_ulogic;
        flush : std_ulogic;
        flushl : std_ulogic;
        dsuen : std_ulogic;
        msu : std_ulogic;
        esu : std_ulogic;
        intack : std_ulogic;
    END RECORD;
    TYPE dcache_out_type IS RECORD
        data : cdatatype;
        set : std_logic_vector ( 1 downto 0 );
        mexc : std_ulogic;
        hold : std_ulogic;
        mds : std_ulogic;
        werr : std_ulogic;
        icdiag : icdiag_in_type;
        cache : std_ulogic;
        idle : std_ulogic;
        scanen : std_ulogic;
        testen : std_ulogic;
    END RECORD;
    TYPE tracebuf_in_type IS RECORD
        addr : std_logic_vector ( 11 downto 0 );
        data : std_logic_vector ( 127 downto 0 );
        enable : std_logic;
        write : std_logic_vector ( 3 downto 0 );
        diag : std_logic_vector ( 3 downto 0 );
    END RECORD;
    TYPE tracebuf_out_type IS RECORD
        data : std_logic_vector ( 127 downto 0 );
    END RECORD;
    TYPE l3_irq_in_type IS RECORD
        irl : std_logic_vector ( 3 downto 0 );
        rst : std_ulogic;
        run : std_ulogic;
    END RECORD;
    TYPE l3_irq_out_type IS RECORD
        intack : std_ulogic;
        irl : std_logic_vector ( 3 downto 0 );
        pwd : std_ulogic;
    END RECORD;
    TYPE l3_debug_in_type IS RECORD
        dsuen : std_ulogic;
        denable : std_ulogic;
        dbreak : std_ulogic;
        step : std_ulogic;
        halt : std_ulogic;
        reset : std_ulogic;
        dwrite : std_ulogic;
        daddr : std_logic_vector ( 23 downto 2 );
        ddata : std_logic_vector ( 31 downto 0 );
        btrapa : std_ulogic;
        btrape : std_ulogic;
        berror : std_ulogic;
        bwatch : std_ulogic;
        bsoft : std_ulogic;
        tenable : std_ulogic;
        timer : std_logic_vector ( 30 downto 0 );
    END RECORD;
    TYPE l3_debug_out_type IS RECORD
        data : std_logic_vector ( 31 downto 0 );
        crdy : std_ulogic;
        dsu : std_ulogic;
        dsumode : std_ulogic;
        error : std_ulogic;
        halt : std_ulogic;
        pwd : std_ulogic;
        idle : std_ulogic;
        ipend : std_ulogic;
        icnt : std_ulogic;
    END RECORD;
    TYPE l3_debug_in_vector IS ARRAY ( natural RANGE <> ) OF l3_debug_in_type;
    TYPE l3_debug_out_vector IS ARRAY ( natural RANGE <> ) OF l3_debug_out_type;
    TYPE div32_in_type IS RECORD
        y : std_logic_vector ( 32 downto 0 );
        op1 : std_logic_vector ( 32 downto 0 );
        op2 : std_logic_vector ( 32 downto 0 );
        flush : std_logic;
        signed : std_logic;
        start : std_logic;
    END RECORD;
    TYPE div32_out_type IS RECORD
        ready : std_logic;
        nready : std_logic;
        icc : std_logic_vector ( 3 downto 0 );
        result : std_logic_vector ( 31 downto 0 );
    END RECORD;
    TYPE mul32_in_type IS RECORD
        op1 : std_logic_vector ( 32 downto 0 );
        op2 : std_logic_vector ( 32 downto 0 );
        flush : std_logic;
        signed : std_logic;
        start : std_logic;
        mac : std_logic;
        acc : std_logic_vector ( 39 downto 0 );
    END RECORD;
    TYPE mul32_out_type IS RECORD
        ready : std_logic;
        nready : std_logic;
        icc : std_logic_vector ( 3 downto 0 );
        result : std_logic_vector ( 63 downto 0 );
    END RECORD;
    TYPE fp_rf_in_type IS RECORD
        rd1addr : std_logic_vector ( 3 downto 0 );
        rd2addr : std_logic_vector ( 3 downto 0 );
        wraddr : std_logic_vector ( 3 downto 0 );
        wrdata : std_logic_vector ( 31 downto 0 );
        ren1 : std_ulogic;
        ren2 : std_ulogic;
        wren : std_ulogic;
    END RECORD;
    TYPE fp_rf_out_type IS RECORD
        data1 : std_logic_vector ( 31 downto 0 );
        data2 : std_logic_vector ( 31 downto 0 );
    END RECORD;
    TYPE fpc_pipeline_control_type IS RECORD
        pc : std_logic_vector ( 31 downto 0 );
        inst : std_logic_vector ( 31 downto 0 );
        cnt : std_logic_vector ( 1 downto 0 );
        trap : std_ulogic;
        annul : std_ulogic;
        pv : std_ulogic;
    END RECORD;
    TYPE fpc_debug_in_type IS RECORD
        enable : std_ulogic;
        write : std_ulogic;
        fsr : std_ulogic;
        addr : std_logic_vector ( 4 downto 0 );
        data : std_logic_vector ( 31 downto 0 );
    END RECORD;
    TYPE fpc_debug_out_type IS RECORD
        data : std_logic_vector ( 31 downto 0 );
    END RECORD;
    TYPE fpc_in_type IS RECORD
        flush : std_ulogic;
        exack : std_ulogic;
        a_rs1 : std_logic_vector ( 4 downto 0 );
        d : fpc_pipeline_control_type;
        a : fpc_pipeline_control_type;
        e : fpc_pipeline_control_type;
        m : fpc_pipeline_control_type;
        x : fpc_pipeline_control_type;
        lddata : std_logic_vector ( 31 downto 0 );
        dbg : fpc_debug_in_type;
    END RECORD;
    TYPE fpc_out_type IS RECORD
        data : std_logic_vector ( 31 downto 0 );
        exc : std_logic;
        cc : std_logic_vector ( 1 downto 0 );
        ccv : std_ulogic;
        ldlock : std_logic;
        holdn : std_ulogic;
        dbg : fpc_debug_out_type;
    END RECORD;
    TYPE grfpu_in_type IS RECORD
        start : std_logic;
        nonstd : std_logic;
        flop : std_logic_vector ( 8 downto 0 );
        op1 : std_logic_vector ( 63 downto 0 );
        op2 : std_logic_vector ( 63 downto 0 );
        opid : std_logic_vector ( 7 downto 0 );
        flush : std_logic;
        flushid : std_logic_vector ( 5 downto 0 );
        rndmode : std_logic_vector ( 1 downto 0 );
        req : std_logic;
    END RECORD;
    TYPE grfpu_out_type IS RECORD
        res : std_logic_vector ( 63 downto 0 );
        exc : std_logic_vector ( 5 downto 0 );
        allow : std_logic_vector ( 2 downto 0 );
        rdy : std_logic;
        cc : std_logic_vector ( 1 downto 0 );
        idout : std_logic_vector ( 7 downto 0 );
    END RECORD;
    TYPE grfpu_out_vector_type IS ARRAY ( integer RANGE 0 to 7 ) OF grfpu_out_type;
    TYPE grfpu_in_vector_type IS ARRAY ( integer RANGE 0 to 7 ) OF grfpu_in_type;
end package iu3PreLoad;