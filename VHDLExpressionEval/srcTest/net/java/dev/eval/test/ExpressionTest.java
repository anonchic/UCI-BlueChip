/*
 * Copyright 2008  Reg Whitton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.dev.eval.test;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import net.java.dev.eval.Expression;

public class ExpressionTest extends TestCase
{
	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
	}

	private void eval(String expression, String... args)
	{
		Map<String, BigDecimal> variables = null;
		Expression exp = new Expression(expression);
		// System.out.println(exp.toString());
		for (int i = 0; i < args.length - 1; i++)
		{
			String[] bits = args[i].split("=");
			if (variables == null)
			{
				variables = new HashMap<String, BigDecimal>();
			}
			variables.put(bits[0], new BigDecimal(bits[1]));
		}
		if (variables == null)
		{
			assertEquals(expression, new BigDecimal(args[args.length - 1]), exp
					.eval());
		}
		else
		{
			assertEquals(expression + " with " + variables, new BigDecimal(
					args[args.length - 1]), exp.eval(variables));
		}
	}

	public final void testEval()
	{
		eval("(1+2)*(1 + x)/2", "x=3", "" + ((1 + 2) * (1 + 3) / 2));
		eval("1 + 2*3 - x/2", "x=8", "" + (1 + 2 * 3 - 8 / 2));
		eval("1 + 2*3 - x/2 + 7", "x=8", "" + (1 + 2 * 3 - 8 / 2 + 7));
		eval("1 + 2 + 3 * 4 * 5 / y - x + 8 - 9", "y=6", "x=7", ""
				+ (1 + 2 + 3 * 4 * 5 / 6 - 7 + 8 - 9));
		eval("(((1 + (2 + 3)) * 4)) * 5 / (y - x + (8 - 9))", "y=6", "x=7", ""
				+ ((((1 + (2 + 3)) * 4)) * 5 / (6 - 7 + (8 - 9))));
		eval("1", "" + 1);
		eval("-1", "" + (-1));
		eval("2 + -1", "" + (2 + -1));
		eval("+2++1", "" + (+2 + +1));
		eval("1000 + (10 + 200) + 300", "" + (1000 + (10 + 200) + 300));

		/* Examples of values from BigDecimal java doc */
		eval("0", "0");
		eval("123", "123");
		eval("-123", "-123");
		eval("1.23E3", "1.23E3");
		eval("1.23E+3", "1.23E+3");
		eval("12.3E+7", "12.3E+7");
		eval("12.0", "12.0");
		eval("12.3", "12.3");
		eval("0.00123", "0.00123");
		eval(".00123", "0.00123");
		eval("-1.23E-12", "-1.23E-12");
		eval("1234.5E-4", "1234.5E-4");
		eval(".5E-4", ".5E-4");
		eval("-.5E-4", "-.5E-4");
		eval("+.5E-4", "+.5E-4");
		eval("0E+7", "0E+7");
		eval("-0", "-0");

		eval("x ? y : z", "x=1", "y=2", "z=3", "2");
		eval("x ? y : z", "x=0", "y=2", "z=3", "3");
		eval("x>y ? x*4-(2*x) : y*(3+1)", "x=3", "y=2", "z=3", "6");
		eval("1000 + (10 > 1 ? 100 : 200) + 300", ""
				+ (1000 + (10 > 1 ? 100 : 200) + 300));
		eval("1000 + (10 > 1 ? 323+100 : 542-200) + 300", ""
				+ (1000 + (10 > 1 ? 323 + 100 : 542 - 200) + 300));
		eval("1000+(10>1?(323+100):(542-200))+300", ""
				+ (1000 + (10 > 1 ? (323 + 100) : (542 - 200)) + 300));
		eval("(1000+(10>1?(323+100):(542-200))+300)", ""
				+ ((1000 + (10 > 1 ? (323 + 100) : (542 - 200)) + 300)));
		eval("1000 + 10 > 11 - 2 ? 100 : 200 + 300", ""
				+ (1000 + 10 > 11 - 2 ? 100 : 200 + 300));
		eval("22/7", new BigDecimal("22").divide(new BigDecimal("7"),
				MathContext.DECIMAL128).toPlainString());
		eval("22%7", new BigDecimal("22").remainder(new BigDecimal("7"),
				MathContext.DECIMAL128).toPlainString());
		eval("15%4", "" + (15 % 4));
		eval("(-15)%4", "" + ((-15) % 4));
		eval("-x", "x=4", "-4");
		eval("y * -x + 1", "y=2", "x=4", "-7");
		eval("-abs(x)", "x=2.5", "-2.5");
		eval("-abs x", "x=-2.5", "-2.5");
		eval("abs-x", "x=2.5", "2.5");
		eval("int x", "x=2.5", "2");
		eval("int y * int x", "y=4.2", "x=2.5", "8");
		eval("y pow x", "y=4", "x=2", "16");
		eval("y", "y=4", "4");

		try
		{
			eval("y pow x", "y=4", "x=2.5", "16");
			fail("can only use integer operand to pow");
		}
		catch (RuntimeException re)
		{
			assertEquals("pow argument: Rounding necessary", re.getMessage());
		}
		eval("y pow int x", "y=4", "x=2.5", "16");

		eval("1 > 2 ? 100 : 200", "200");
		eval("2 > 1 ? 100 : 200", "100");

		eval("1 >= 2 ? 100 : 200", "200");
		eval("2 >= 1 ? 100 : 200", "100");
		eval("2 >= 2 ? 100 : 200", "100");

		eval("2 < 1 ? 100 : 200", "200");
		eval("1 < 2 ? 100 : 200", "100");

		eval("2 <= 1 ? 100 : 200", "200");
		eval("1 <= 2 ? 100 : 200", "100");
		eval("2 <= 2 ? 100 : 200", "100");

		eval("2 <> 2 ? 100 : 200", "200");
		eval("2 <> 1 ? 100 : 200", "100");

		eval("2 != 2 ? 100 : 200", "200");
		eval("2 != 1 ? 100 : 200", "100");

		eval("2 == 1 ? 100 : 200", "200");
		eval("2 == 2 ? 100 : 200", "100");

		eval("x > y && x != 4 ? x : y", "x=2", "y=1", "2");
		eval("x > y && x != 4 ? x : y", "x=4", "y=1", "1");
		eval("x > y && x != 4 ? x : y", "x=1", "y=3", "3");

		eval("x > y || x != 4 ? x : y", "x=2", "y=1", "2");
		eval("x > y || x != 4 ? x : y", "x=3", "y=1", "3");
		eval("x > y || x != 4 ? x : y", "x=4", "y=1", "4");
		eval("x > y || x != 4 || x<y? x : y", "x=2", "y=3", "2");
	}
}
