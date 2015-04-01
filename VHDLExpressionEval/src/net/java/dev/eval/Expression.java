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
package net.java.dev.eval;

import java.util.Map;

/**
 * A simple VHDL expression evaluator.
 * <P>
 * The precedence of operators and the type of operator is specific to VHDL and differs
 * slightly from C and Java.  The expression evaluator only performs operations defined
 * in the standard libraries using operands that are bit strings, integers, booleans, or
 * enumeration elements.  Expression using user defined types and/or user defined
 * operations may not evaluate correctly, resulting in an exception.
 * <P>
 * Example of use:
 * 
 * <PRE>
 * Expression exp = new Expression(&quot;(x + y)/2&quot;);
 * 
 * Map&lt;String, String&gt; variables = new HashMap&lt;String, String&gt;();
 * variables.put(&quot;x&quot;, &quot;4.32&quot;);
 * variables.put(&quot;y&quot;, &quot;342.1&quot;);
 * 
 * String result = exp.eval(variables, null);
 * 
 * Alternative:
 * 
 * String result = Expression.eval(&quot;(x + y)/2&quot;, variables, null);
 * 
 * System.out.println(result);
 * </PRE>
 * 
 * <P>
 * The following operators are supported (presented from lower precedence to higher by group):
 * <UL>
 * <LI>The following logical operations performed on booleans, bits, and bit strings, resulting in the same type as the operands:
 * <DL>
 * <DT>AND</DT>
 * <DD>and</DD>
 * <DT>OR</DT>
 * <DD>or</DD>
 * <DT>NAND</DT>
 * <DD>not and</DD>
 * <DT>NOR</DT>
 * <DD>not or</DD>
 * <DT>XOR</DT>
 * <DD>exclusive-or</DD>
 * <DT>XNOR</DT>
 * <DD>not exclusive-or</DD>
 * </DL>
 * </LI>
 * <BR>
 * <LI>The following comparison operations performed on all standard types resulting in a boolean:
 * <DL>
 * <DT>&lt;</DT>
 * <DD>less than</DD>
 * <DT>&lt;=</DT>
 * <DD>less than or equal to</DD>
 * <DT>=</DT>
 * <DD>equal to</DD>
 * <DT>&gt;</DT>
 * <DD>greater than</DD>
 * <DT>&gt;=</DT>
 * <DD>greater than or equal to</DD>
 * <DT>/=</DT>
 * <DD>not equal to</DD>
 * </DL>
 * </LI>
 * <BR>
 * <LI>The following shift operations performed on bit strings, resulting in a bit string:
 * <DL>
 * <DT>SLL</DT>
 * <DD>shift left - logical</DD>
 * <DT>SRL</DT>
 * <DD>shift right - logical</DD>
 * <DT>SLA</DT>
 * <DD>shift left - arithmetic</DD>
 * <DT>SRA</DT>
 * <DD>shift right - arithmetic</DD>
 * <DT>ROL</DT>
 * <DD>rotate left</DD>
 * <DT>ROR</DT>
 * <DD>rotate right</DD>
 * </DL>
 * </LI>
 * <BR>
 * <LI>Addition operations performed on integers and bit strings, resulting in the same type as the operands:
 * <DL>
 * <DT>+</DT>
 * <DD>addition</DD>
 * <DT>-</DT>
 * <DD>subtraction</DD>
 * </DL>
 * </LI>
 * <BR>
 * <LI>The concatenation operation performed on strings, resulting in a string the length of the sum of the operand string lengths:
 * <BR>Same precedence as the addition operations
 * <DL>
 * <DT>&amp;</DT>
 * <DD>concatenation</DD>
 * </DL>
 * </LI>
 * <LI>The uniary operations for positive and negative numbers, performed on integers, resulting in integers:
 * <DL>
 * <DT>+</DT>
 * <DD>positive</DD>
 * <DT>-</DT>
 * <DD>negative</DD>
 * </DL>
 * </LI>
 * <LI>The multiplication operations performed on integers, resulting in integers:
 * <DL>
 * <DT>*</DT>
 * <DD>multiplication</DD>
 * <DT>/</DT>
 * <DD>division</DD>
 * <DT>REM</DT>
 * <DD>remainder</DD>
 * <DT>MOD</DT>
 * <DD>modulus</DD>
 * </DL>
 * </LI>
 * <LI>The power operations performed on integers, resulting in integers:
 * <DL>
 * <DT>**</DT>
 * <DD>exponent</DD>
 * <DT>ABS</DT>
 * <DD>absolute value</DD>
 * <DT>NOT</DT>
 * <DD>logical negation - accepts booleans, bits, and bit strings</DD>
 * </DL>
 * </LI>
 * <LI>Special operator for getting attribute information from names:
 * <DL>
 * <DT>'</DT>
 * <DD>attribute dereference</DD>
 * </DL>
 * </LI>
 * </UL>
 * 
 * <P>
 * Expressions are evaluated using the precedence rules found in VHDL, and
 * parentheses can be used to control the evaluation order.
 * <P>
 * Operators and other strings are case-insensitive.
 * <P>
 * <P>
 * Example expressions:
 * 
 * <PRE>
 * 2*2
 * 2+2
 * 100/2
 * x/100 * 17.5
 * 2 ** 32 - 1
 * 2 ** (32 - 1)
 * 2 ** (21 MOD 6)
 * abs(-15)
 * x &gt; y AND x != 4
 * y &gt; 4*x
 * </PRE>
 * 
 * @author Matthew Hicks based on a Java expression evaluator by Reg Whitton
 */
public class Expression
{
	/**
	 * The root of the tree of arithmetic operations.
	 */
	private final Operation rootOperation;

	/**
	 * Construct an {@link Expression} that may be used multiple times to
	 * evaluate the expression using different sets of variables and names. This holds the
	 * results of parsing the expression to minimize further work.
	 * 
	 * @param expression
	 *            the arithmetic expression to be parsed.
	 */
	public Expression(String expression)
	{
		rootOperation = new Compiler(expression).compile();
	}

	/**
	 * Evaluate the expression with the given set of values and a set of names.
	 * 
	 * @param variables
	 *            the values to use in the expression.
	 * @param names
	 *            the names to use in attribute dereferencing.
	 * @return the result of the evaluation
	 */
	public String eval(Map<String, String> variables, Map<String, String> names)
	{
		return rootOperation.eval(variables, names);
	}

	/**
	 * Evaluate the expression which does not reference any variables or attributes.
	 * 
	 * @return the result of the evaluation
	 */
	public String eval()
	{
		return eval(null, null);
	}
	
	/**
	 * A convenience method that constructs an {@link Expression} and evaluates
	 * it, returning the result.
	 * 
	 * @param expression
	 *            the expression to evaluate.
	 * @param variables
	 *            the values to use in the evaluation.
	 * @param names
	 *            the names to use in attribute dereferencing.
	 * @return the result of the evaluation
	 */
	public static String eval(String expression, Map<String, String> variables, Map<String, String> names)
	{
		return new Expression(expression).eval(variables, names);
	}

	/**
	 * A convenience method that constructs an {@link Expression} that
	 * references no variables or attributes and evaluates it.
	 * 
	 * @param expression
	 *            the expression to evaluate.
	 * @return the result of the evaluation
	 */
	public static String eval(String expression)
	{
		return new Expression(expression).eval();
	}

	/**
	 * Creates a string showing expression as it has been parsed.
	 */
	@Override
	public String toString()
	{
		return rootOperation.toString();
	}
}
