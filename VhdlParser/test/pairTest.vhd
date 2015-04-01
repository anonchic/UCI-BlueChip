--EXPECTED RESULTS
--RESAB&0&0
--RESCD&0&0
--RESEF&0&0
--RESGH&0&0
--RESABCD&0&0
--RESEFGH&0&0
--RESULT&0&0
--RESAB&OPERANDA&1
--RESAB&OPERANDB&1
--RESCD&OPERANDC&1
--RESCD&OPERANDD&1
--RESEF&OPERANDE&1
--RESEF&OPERANDF&1
--RESGH&OPERANDG&1
--RESGH&OPERANDH&1
--RESABCD&OPERANDA&2
--RESABCD&OPERANDB&2
--RESABCD&OPERANDC&2
--RESABCD&OPERANDD&2
--RESEFGH&OPERANDE&2
--RESEFGH&OPERANDF&2
--RESEFGH&OPERANDG&2
--RESEFGH&OPERANDH&2
--RESULT&OPERANDA&3
--RESULT&OPERANDB&3
--RESULT&OPERANDC&3
--RESULT&OPERANDD&3
--RESULT&OPERANDE&3
--RESULT&OPERANDF&3
--RESULT&OPERANDG&3
--RESULT&OPERANDH&3
--RESABCD&RESAB&1
--RESABCD&RESCD&1
--RESEFGH&RESEF&1
--RESEFGH&RESGH&1
--RESULT&RESAB&2
--RESULT&RESCD&2
--RESULT&RESEF&2
--RESULT&RESGH&2
--RESULT&RESABCD&1
--RESULT&RESEFGH&1
--OPSABCOMBINED&???&0	   -- how do we deal with this
--OPSABCOMBINED (15 DOWNTO 12)&OPERANDA&0
--OPSABCOMBINED (11 DOWNTO 8)&OPERANDB&0	 
--TRICKYSPLIT&OPSABCOMBINED&1
--TRICKYSPLIT (5 DOWNTO 2)&TRICKYSPLIT (9 DOWNTO 6)&1
--TRICKYSPLIT (5 DOWNTO 2)&OPSABCOMBINED (15 DOWNTO 12)&2
--TRICKYSPLIT (5 DOWNTO 2)&OPERANDA&2
--TRICKYSPLIT (5 DOWNTO 2)&OPSABCOMBINED (11 DOWNTO 8)&1
--TRICKYSPLIT (5 DOWNTO 2)&OPERANDB&1
--TRICKYSPLIT (9 DOWNTO 6)&OPSABCOMBINED (15 DOWNTO 12)&1
--TRICKYSPLIT2 (19 DOWNTO 12)&???&0	   -- how do we deal with these
--TRICKYSPLIT2 (19 DOWNTO 16)&OPERANDA&0
--TRICKYSPLIT2 (15 DOWNTO 12)&OPERANDB&0
--TRICKYSPLIT2 (11 DOWNTO 4)&TRICKYSPLIT2 (19 DOWNTO 12)&1
--TRICKYSPLIT2 (11 DOWNTO 4)&???&1	   -- how do we deal with these
--TRICKYSPLIT2 (7 DOWNTO 4)&TRICKYSPLIT2 (15 DOWNTO 12)&1
--TRICKYSPLIT2 (7 DOWNTO 4)&OPERANDB (3 DOWNTO 0)&1
--TRICKYSPLIT2 (7 DOWNTO 4)&TRICKYSPLIT2 (11 DOWNTO 8)&1 
--TRICKYSPLIT2 (11 DOWNTO 8)&TRICKYSPLIT2 (19 DOWNTO 16)&1
--TRICKYSPLIT2 (11 DOWNTO 8)&OPERANDA&1
--TRICKYSPLIT2 (7 DOWNTO 4)&TRICKYSPLIT2 (19 DOWNTO 16)&2
--TRICKYSPLIT2 (7 DOWNTO 4)&OPERANDA&2
--TRICKYREC.A&OPERANDA&1
--TRICKYREC.B&OPERANDB&1			 
--TRICKYREC&???&0	   -- how do we deal with these
--TRICKYREC.C&???&1	   -- how do we deal with these
--TRICKYREC.C (7 DOWNTO 4)&TRICKYREC.A&1	   -- how do we deal with these
--TRICKYREC.C (3 downto 0)&TRICKYREC.B&1	   -- how do we deal with these

library IEEE;
  use ieee.std_logic_1164.all;
  use ieee.numeric_std.all;

entity pairTest is
	port(
		clk		: in std_logic;
		rst		: in std_logic;
		operandA	: in unsigned(3 downto 0);
		operandB	: in unsigned(3 downto 0);
		operandC	: in unsigned(3 downto 0);
		operandD	: in unsigned(3 downto 0);
		operandE	: in unsigned(3 downto 0);
		operandF	: in unsigned(3 downto 0);
		operandG	: in unsigned(3 downto 0);
		operandH	: in unsigned(3 downto 0);
		result		: out unsigned(3 downto 0)
	);
end entity pairTest;

architecture rtl of pairTest is
	type My_Rec is record
	   a : unsigned(3 downto 0);
	   b : unsigned(3 downto 0);
	   c : unsigned(7 downto 0);
	end record;					
	
	signal resAB : unsigned(3 downto 0);
	signal resCD : unsigned(3 downto 0);
	signal resEF : unsigned(3 downto 0);
	signal resGH : unsigned(3 downto 0);
	signal resABCD : unsigned(3 downto 0);
	signal resEFGH : unsigned(3 downto 0);
	signal opsABCombined : unsigned(15 downto 8);
	signal trickySplit : unsigned(9 downto 2);
	signal trickySplit2 : unsigned(19 downto 4);
	signal trickyRec : My_rec;
begin								 
	opsABCombined <= operandA & operandB;
	trickySplit2(19 downto 12) <= operandA & operandB;
	
	DoOps : process(clk)
	begin
		if(clk'event and clk = '1')then
			if(rst = '1')then
				resAB <= "0000";
				resCD <= "0000";
				resEF <= "0000";
				resGH <= "0000";
				resABCD <= "0000";
				resEFGH <= "0000";
				result <= "0000";
				trickySplit <= opsABCombined;
				trickySplit2(11 downto 4) <= trickySplit2(19 downto 12);
				trickyRec <= (operandA, operandB, (operandC & operandD));
			else
				resAB <= operandA + operandB;
				resCD <= operandC + operandD;
				resEF <= operandE + operandF;
				resGH <= operandG + operandH;
				resABCD <= resAB + resCD;
				resEFGH <= resEF + resGH;
				result <= resABCD + resEFGH;
				
				-- ts(7 4) should be added to the signal list since it is a driver, and it should be marked as a register due to it's less constrained version being marked as such
				if(operandA(3) and operandB(3) and operandC(3) and operandD(3) and operandE(3) and operandF(3) and operandG(3) and operandH(3))then 
					trickySplit(5 downto 2) <= trickySplit(9 downto 6);
					trickySplit2(7 downto 4) <= trickySplit2(11 downto 8);
					trickyRec.c <= trickyRec.a & trickyRec.b;
				end if;
			end if;
		end if;
	end process;
end architecture rtl;