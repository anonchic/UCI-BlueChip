library ieee;
	use ieee.std_logic_1164.all;
	use ieee.numeric_std.all;

entity typeTest is
	port (
		clk		: in	std_ulogic;
		rst		: in	std_ulogic;
		dataIn	: in	std_ulogic_vector(7 downto 0);
		dataOut	: out	std_ulogic_vector(7 downto 0)
	);
end entity typeTest;

architecture rtl of typeTest is
	-- Scalar type declarations
	type midway_up is range -5 to 5;						-- Integer types using expressions and direction
	type midway_down is range 5 downto -5;
	type midway_express_up is range (-10 + 2) + 3 to abs(((10/2 * 4 mod 15) ** 1) * (-1));
	type states_text is (start, stop, running, waiting);	-- Enumerations
	type states_char is ('a', 'b', 'c', 'd');
	-- Composite type declarations
	type word_up is array (0 to 7) of std_ulogic;			-- Arrays
	type word_down is array (7 downto 0) of std_ulogic;
	type word_unlim is array (integer range <>) of std_ulogic;
	--type typeAsRange is array (states_text range stop to waiting) of std_ulogic;		-- Array that can only be indexed by the values in the index type (i.e. mapping from state to std_ulogic)
	type typeAsRangeRev is array (positive range 35 downto 15) of std_ulogic;
	--type midway_up2 is range word_up'RANGE;					-- Integer types using range attribute (attribute must be from array type)
	--type midway_down2 is range word_unlim(0 to 3)'REVERSE_RANGE;
	type reg_file is record									-- Records
		reg1 : word_down;
		reg2 : word_up;
		reg3 : midway_down;
		reg4 : midway_up;
		reg5 : word_unlim(31 downto 0);						-- Unnamed types
		reg6 : natural range 0 to 15;
		reg7, reg8 : word_unlim(31 downto -10);
	end record;
	type words_unlim is array (natural range  <>) of std_logic_vector(7 downto 0);
	--type nameRange is array (word_up'range) of word_down;    -- Range attribute as array bounds
	--type nameRangeRev is array (word_up'REVERSE_RANGE) of word_down;
	--type nameBounds is array (word_up'right downto word_up'left) of word_down;		-- Type attributes as array bounds
	--type expressionBounds is array (2+((1+3)+5) downto dataIn'right) of std_logic;	-- Expressions as array bounds with reference to non-type name
	type recArray is array (3 downto 0) of reg_file;		-- Array of record
	-- Subtypes
	subtype word_up_copy is word_up;						-- Type alias
	subtype word_unlim_copy is word_unlim;
	subtype reg_file_2 is reg_file;
	subtype word_lim is word_unlim(5 to 10);				-- Simple subset of base type
	subtype small_int is integer range -100 to 100;
	subtype small_pos is positive range 3 to 17;
	subtype states_two1 is states_text range stop to running;
	subtype states_two2 is states_text range 1 to 2;
	subtype small_int_rev is integer range 100 downto -100; -- Subtype different direction than base type
	subtype word_lim_rev is word_unlim(10 downto 5);
	subtype states_two1_rev is states_text range running downto stop;
begin
	main : process(clk)
	begin
		if(clk'event and clk = '1')then
			if(rst = '1')then
				dataOut <= X"00";
			else
				dataOut <= dataIn;
			end if;
		end if;
	end process;
end;
