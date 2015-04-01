library IEEE;
  use ieee.std_logic_1164.all;
  use ieee.numeric_std.all;
  
entity valueTest is
  generic ( 
    -- Negative integers are just a negate operation on a positive integer
    -- Can use scientific notation, with plus symbol
    -- Can seperate digits with an underscore
    -- Decimal number representation is defualt for VHDL
    interfaceConstant1 : integer := 12_0E0_2;
    interfaceConstant2 : integer := 12E3;			-- Doesn't support E+ or E-
    -- Integer literals can also be in the for base#digit and _ string#
    -- Base must be in range 2 to 16
    interfaceConstant3 : integer := 2#10_1110_11_100_0_00#;
    interfaceConstant4 : integer := 7#46662#;
    interfaceConstant5 : integer := 8#27340#;
    interfaceConstant6 : integer := 16#2EE0#;
    -- Basic enumeration assignment
    --interfaceConstant7 : character := 'x';		-- Only supports integers, booleans, bits, and bit vectors
    interfaceConstant8 : std_logic range '0' to '1' := '0';
    -- Array assignment, digits can be separated by _
    -- Digits must be members of base type
    -- Can specify base of string, for specification in octal and hex
    -- Octal and hex strings require the maximum number of bits based on the number of digits
    -- Use double quotes inside a string to add a quote character
    --interfaceConstant9 : string := "This is ""a"" string";
    interfaceConstant10 : std_logic_vector(13 downto 0) := B"10_1110_1110_0000";
    interfaceConstant11 : std_logic_vector(14 downto 0) := O"27340";
    interfaceConstant12 : std_logic_vector(15 downto 0) := X"2EE0";
    interfaceConstant13 : std_logic_vector(0 downto 0) := "0";
    -- Boolean literals
    interfaceConstant14 : boolean := true;
    interfaceConstant15 : boolean := false
    -- Handle other types of enums i.e. setting the start state of a state machine
  );
  port (
    clk     : in  std_logic;
    rst     : in  std_logic;
    dataIn  : in  std_logic_vector(7 downto 0);
    dataOut : out std_logic_vector(7 downto 0)
  );
end entity valueTest;

architecture rtl of valueTest is
begin
  process(clk)begin
    if(clk'event and clk = '1')then
      if(rst = '1')then
        dataOut <= X"00";
      else
        dataOut <= dataIn;
      end if;
    end if;  
  end process;
end architecture rtl;
