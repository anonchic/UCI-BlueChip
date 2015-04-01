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

// Enumeration of operations, their properties, and how they manipulate the operands to produce a result
// Each operation has the following properties:
//		precedence
//		numberOfOperands
//		string
// Each operator also defines a perform method that produces a result given the operations properties
// Operator precedence is based-on that used in the VHDL language, with higher values meaning higher precedence

// Add support for attribute dereference

package net.java.dev.eval;
import java.math.BigInteger;

enum Operator
{	
	/**
	 * End of string reached.
	 */
	END(-1, 0, null)
	{
		@Override
		String perform(String value1, String value2)
		{
			throw new RuntimeException("END is a dummy operation");
		}
	},
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Can have bits, booleans, and bit vectors as operands, but size and type of operands must match
	/**
	 * AND - and
	 */
	AND(0, 2, "AND")
	{
		@Override
		String perform(String value1, String value2)
		{
			prePerformChecks(value1, value2);
			Type type = Type.getTypeOf(value1);
			
			if(type == Type.BIT)
			{
				if(value1.charAt(1) == '1' && value2.charAt(1) == '1')
				{
					return "'1'";
				}
				return "'0'";
			}
			else if(type == Type.BOOLEAN)
			{
				if(value1.equalsIgnoreCase("true") && value2.equalsIgnoreCase("true"))
				{
					return "TRUE";
				}
				return "FALSE";
			}
			else if(type == Type.BIT_STRING)
			{
				String val1 = type.getNormalFormOf(value1);
				val1 = val1.substring(1, val1.length() - 1);
				String val2 = type.getNormalFormOf(value2);
				val2 = val2.substring(1, val2.length() - 1);
				
				// Remember the length so we can pad with leading zeros
				int length = val1.length();
				
				return "\"" + padWithLeadingZeros((new BigInteger(val1, 2)).and(new BigInteger(val2, 2)).toString(2), length) + "\"";
			}
			return "";
		}
		
		@Override
		boolean canOperateOnType(Type type)
		{
			return type == Type.BIT || type == Type.BIT_STRING || type == Type.BOOLEAN;
		}
	},
	/**
	 * OR - or
	 */
	OR(0, 2, "OR")
	{
		@Override
		String perform(String value1, String value2)
		{
			prePerformChecks(value1, value2);
			Type type = Type.getTypeOf(value1);
			
			if(type == Type.BIT)
			{
				if(value1.charAt(1) == '0' && value2.charAt(1) == '0')
				{
					return "'0'";
				}
				return "'1'";
			}
			else if(type == Type.BOOLEAN)
			{
				if(value1.equalsIgnoreCase("false") && value2.equalsIgnoreCase("false"))
				{
					return "FALSE";
				}
				return "TRUE";
			}
			else if(type == Type.BIT_STRING)
			{
				String val1 = type.getNormalFormOf(value1);
				val1 = val1.substring(1, val1.length() - 1);
				String val2 = type.getNormalFormOf(value2);
				val2 = val2.substring(1, val2.length() - 1);
					
				// Remember the length so we can pad with leading zeros
				int length = val1.length();
				
				return "\"" + padWithLeadingZeros((new BigInteger(val1, 2)).or(new BigInteger(val2, 2)).toString(2), length) + "\"";
			}
			return "";
		}
		
		@Override
		boolean canOperateOnType(Type type)
		{
			return type == Type.BIT || type == Type.BIT_STRING || type == Type.BOOLEAN;
		}
	},
	/**
	 * NAND - not and
	 */
	NAND(0, 2, "NAND")
	{
		@Override
		String perform(String value1, String value2)
		{
			prePerformChecks(value1, value2);
			Type type = Type.getTypeOf(value1);
			
			if(type == Type.BIT)
			{
				if(value1.charAt(1) == '1' && value2.charAt(1) == '1')
				{
					return "'0'";
				}
				return "'1'";
			}
			else if(type == Type.BOOLEAN)
			{
				if(value1.equalsIgnoreCase("true") && value2.equalsIgnoreCase("true"))
				{
					return "FALSE";
				}
				return "TRUE";
			}
			else if(type == Type.BIT_STRING)
			{
				String val1 = type.getNormalFormOf(value1);
				val1 = val1.substring(1, val1.length() - 1);
				String val2 = type.getNormalFormOf(value2);
				val2 = val2.substring(1, val2.length() - 1);
				
				// Remember the length so we can pad with leading zeros
				int length = val1.length();
				
				return "\"" + padWithLeadingZeros((new BigInteger(val1, 2)).and(new BigInteger(val2, 2)).not().toString(2), length) + "\"";
			}
			return "";
		}
		
		@Override
		boolean canOperateOnType(Type type)
		{
			return type == Type.BIT || type == Type.BIT_STRING || type == Type.BOOLEAN;
		}
	},
	/**
	 * NOR - not or
	 */
	NOR(0, 2, "NOR")
	{
		@Override
		String perform(String value1, String value2)
		{
			prePerformChecks(value1, value2);
			Type type = Type.getTypeOf(value1);
			
			if(type == Type.BIT)
			{
				if(value1.charAt(1) == '0' && value2.charAt(1) == '0')
				{
					return "'1'";
				}
				return "'0'";
			}
			else if(type == Type.BOOLEAN)
			{
				if(value1.equalsIgnoreCase("false") && value2.equalsIgnoreCase("false"))
				{
					return "TRUE";
				}
				return "FALSE";
			}
			else if(type == Type.BIT_STRING)
			{
				String val1 = type.getNormalFormOf(value1);
				val1 = val1.substring(1, val1.length() - 1);
				String val2 = type.getNormalFormOf(value2);
				val2 = val2.substring(1, val2.length() - 1);
					
				// Remember the length so we can pad with leading zeros
				int length = val1.length();
				
				return "\"" + padWithLeadingZeros((new BigInteger(val1, 2)).or(new BigInteger(val2, 2)).not().toString(2), length) + "\"";
			}
			return "";
		}
		
		@Override
		boolean canOperateOnType(Type type)
		{
			return type == Type.BIT || type == Type.BIT_STRING || type == Type.BOOLEAN;
		}
	},
	/**
	 * XOR - exclusive or
	 */
	XOR(0, 2, "XOR")
	{
		@Override
		String perform(String value1, String value2)
		{
			prePerformChecks(value1, value2);
			Type type = Type.getTypeOf(value1);
			
			if(type == Type.BIT)
			{
				if((value1.charAt(1) == '0' && value2.charAt(1) == '0') || (value1.charAt(1) == '1' && value2.charAt(1) == '1'))
				{
					return "'0'";
				}
				return "'1'";
			}
			else if(type == Type.BOOLEAN)
			{
				if((value1.equalsIgnoreCase("false") && value2.equalsIgnoreCase("false")) || (value1.equalsIgnoreCase("true") && value2.equalsIgnoreCase("true")))
				{
					return "FALSE";
				}
				return "TRUE";
			}
			else if(type == Type.BIT_STRING)
			{
				String val1 = type.getNormalFormOf(value1);
				val1 = val1.substring(1, val1.length() - 1);
				String val2 = type.getNormalFormOf(value2);
				val2 = val2.substring(1, val2.length() - 1);
					
				// Remember the length so we can pad with leading zeros
				int length = val1.length();
				
				return "\"" + padWithLeadingZeros((new BigInteger(val1, 2)).xor(new BigInteger(val2, 2)).toString(2), length) + "\"";
			}
			return "";
		}
		
		@Override
		boolean canOperateOnType(Type type)
		{
			return type == Type.BIT || type == Type.BIT_STRING || type == Type.BOOLEAN;
		}
	},
	/**
	 * XNOR - exclusive not or
	 */
	XNOR(0, 2, "XNOR")
	{
		@Override
		String perform(String value1, String value2)
		{
			prePerformChecks(value1, value2);
			Type type = Type.getTypeOf(value1);
			
			if(type == Type.BIT)
			{
				if((value1.charAt(1) == '0' && value2.charAt(1) == '0') || (value1.charAt(1) == '1' && value2.charAt(1) == '1'))
				{
					return "'1'";
				}
				return "'0'";
			}
			else if(type == Type.BOOLEAN)
			{
				if((value1.equalsIgnoreCase("false") && value2.equalsIgnoreCase("false")) || (value1.equalsIgnoreCase("true") && value2.equalsIgnoreCase("true")))
				{
					return "TRUE";
				}
				return "FALSE";
			}
			else if(type == Type.BIT_STRING)
			{
				String val1 = type.getNormalFormOf(value1);
				val1 = val1.substring(1, val1.length() - 1);
				String val2 = type.getNormalFormOf(value2);
				val2 = val2.substring(1, val2.length() - 1);
					
				// Remember the length so we can pad with leading zeros
				int length = val1.length();
				
				return "\"" + padWithLeadingZeros((new BigInteger(val1, 2)).xor(new BigInteger(val2, 2)).not().toString(2), length) + "\"";
			}
			return "";
		}
		
		@Override
		boolean canOperateOnType(Type type)
		{
			return type == Type.BIT || type == Type.BIT_STRING || type == Type.BOOLEAN;
		}
	},
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Can take any same type operands, producing a boolean result
	// Compares arrays element-by-element from left to right
	/**
	 * &gt;= - greater than or equal to
	 */
	GE(1, 2, ">=")
	{
		@Override
		String perform(String value1, String value2)
		{
			prePerformChecks(value1, value2);
			Type type = Type.getTypeOf(value1);
			
			if(type == Type.BIT)
			{
				if(value1.charAt(1) == '1' || value2.charAt(1) == '0')
				{
					return "TRUE";
				}
				return "FALSE";
			}
			else if(type == Type.BOOLEAN)
			{
				if(value1.equalsIgnoreCase("true") || value2.equalsIgnoreCase("false"))
				{
					return "TRUE";
				}
				return "FALSE";
			}
			else if(type == Type.INTEGER)
			{
				if(Integer.parseInt(Type.getCanonicalFormOf(value1)) >= Integer.parseInt(Type.getCanonicalFormOf(value2)))
				{
					return "TRUE";
				}
				return "FALSE";
			}
			else if(type == Type.BIT_STRING)
			{
				String val1 = Type.getCanonicalFormOf(value1);
				String val2 = Type.getCanonicalFormOf(value2);

				// Compare the vectors, index by index, from left to right
				for(int index  = 0; index < val1.length(); ++ index)
				{
					if(val1.charAt(index) != val2.charAt(index))
					{
						if(val1.charAt(index) == '1')
						{
							return "TRUE";
						}
						return "FALSE";
					}	
				}
				
				// They are equal
				return "TRUE";
			}
			return "ERROR";
		}
	},
	/**
	 * &lt;= - less than or equal to
	 */
	LE(1, 2, "<=")
	{
		@Override
		String perform(String value1, String value2)
		{
			prePerformChecks(value1, value2);
			Type type = Type.getTypeOf(value1);
			
			if(type == Type.BIT)
			{
				if(value1.charAt(1) == '0' || value2.charAt(1) == '1')
				{
					return "TRUE";
				}
				return "FALSE";
			}
			else if(type == Type.BOOLEAN)
			{
				if(value1.equalsIgnoreCase("false") || value2.equalsIgnoreCase("true"))
				{
					return "TRUE";
				}
				return "FALSE";
			}
			else if(type == Type.INTEGER)
			{
				if(Integer.parseInt(Type.getCanonicalFormOf(value1)) <= Integer.parseInt(Type.getCanonicalFormOf(value2)))
				{
					return "TRUE";
				}
				return "FALSE";
			}
			else if(type == Type.BIT_STRING)
			{
				String val1 = Type.getCanonicalFormOf(value1);
				String val2 = Type.getCanonicalFormOf(value2);

				// Compare the vectors, index by index, from left to right
				for(int index  = 0; index < val1.length(); ++ index)
				{
					if(val1.charAt(index) != val2.charAt(index))
					{
						if(val1.charAt(index) == '0')
						{
							return "TRUE";
						}
						return "FALSE";
					}	
				}
				
				// They are equal
				return "TRUE";
			}
			return "ERROR";
		}
	},
	/**
	 * &gt; - greater than
	 */
	GT(1, 2, ">")
	{
		@Override
		String perform(String value1, String value2)
		{
			prePerformChecks(value1, value2);
			Type type = Type.getTypeOf(value1);
			
			if(type == Type.BIT)
			{
				if(value1.charAt(1) == '1' && value2.charAt(1) == '0')
				{
					return "TRUE";
				}
				return "FALSE";
			}
			else if(type == Type.BOOLEAN)
			{
				if(value1.equalsIgnoreCase("true") && value2.equalsIgnoreCase("false"))
				{
					return "TRUE";
				}
				return "FALSE";
			}
			else if(type == Type.INTEGER)
			{
				if(Integer.parseInt(Type.getCanonicalFormOf(value1)) > Integer.parseInt(Type.getCanonicalFormOf(value2)))
				{
					return "TRUE";
				}
				return "FALSE";
			}
			else if(type == Type.BIT_STRING)
			{
				String val1 = Type.getCanonicalFormOf(value1);
				String val2 = Type.getCanonicalFormOf(value2);

				// Compare the vectors, index by index, from left to right
				for(int index  = 0; index < val1.length(); ++ index)
				{
					if(val1.charAt(index) != val2.charAt(index))
					{
						if(val1.charAt(index) == '1')
						{
							return "TRUE";
						}
						return "FALSE";
					}	
				}
				
				// They are equal
				return "FALSE";
			}
			return "ERROR";
		}
	},
	/**
	 * &lt; - less than
	 */
	LT(1, 2, "<")
	{
		@Override
		String perform(String value1, String value2)
		{
			prePerformChecks(value1, value2);
			Type type = Type.getTypeOf(value1);
			
			if(type == Type.BIT)
			{
				if(value1.charAt(1) == '0' && value2.charAt(1) == '1')
				{
					return "TRUE";
				}
				return "FALSE";
			}
			else if(type == Type.BOOLEAN)
			{
				if(value1.equalsIgnoreCase("false") && value2.equalsIgnoreCase("true"))
				{
					return "TRUE";
				}
				return "FALSE";
			}
			else if(type == Type.INTEGER)
			{
				if(Integer.parseInt(Type.getCanonicalFormOf(value1)) < Integer.parseInt(Type.getCanonicalFormOf(value2)))
				{
					return "TRUE";
				}
				return "FALSE";
			}
			else if(type == Type.BIT_STRING)
			{
				String val1 = Type.getCanonicalFormOf(value1);
				String val2 = Type.getCanonicalFormOf(value2);

				// Compare the vectors, index by index, from left to right
				for(int index  = 0; index < val1.length(); ++ index)
				{
					if(val1.charAt(index) != val2.charAt(index))
					{
						if(val1.charAt(index) == '0')
						{
							return "TRUE";
						}
						return "FALSE";
					}	
				}
				
				// They are equal
				return "FALSE";
			}
			return "ERROR";
		}
	},
	/**
	 * = - equal
	 */
	EQ(1, 2, "=")
	{
		@Override
		String perform(String value1, String value2)
		{
			prePerformChecks(value1, value2);
			Type type = Type.getTypeOf(value1);
			
			if(type == Type.BIT)
			{
				if(value1.charAt(1) == value2.charAt(1))
				{
					return "TRUE";
				}
				return "FALSE";
			}
			else if(type == Type.BOOLEAN)
			{
				if(value1.equalsIgnoreCase(value2))
				{
					return "TRUE";
				}
				return "FALSE";
			}
			else if(type == Type.INTEGER)
			{
				if(Integer.parseInt(Type.getCanonicalFormOf(value1)) == Integer.parseInt(Type.getCanonicalFormOf(value2)))
				{
					return "TRUE";
				}
				return "FALSE";
			}
			else if(type == Type.BIT_STRING)
			{
				String val1 = Type.getCanonicalFormOf(value1);
				String val2 = Type.getCanonicalFormOf(value2);

				// Compare the vectors, index by index, from left to right
				for(int index  = 0; index < val1.length(); ++ index)
				{
					if(val1.charAt(index) != val2.charAt(index))
					{
						return "FALSE";
					}	
				}
				
				// They are equal
				return "TRUE";
			}
			return "ERROR";
		}
	},
	/**
	 * /= - not equal
	 */
	NE(1, 2, "/=")
	{
		@Override
		String perform(String value1, String value2)
		{
			prePerformChecks(value1, value2);
			Type type = Type.getTypeOf(value1);
			
			if(type == Type.BIT)
			{
				if(value1.charAt(1) != value2.charAt(1))
				{
					return "TRUE";
				}
				return "FALSE";
			}
			else if(type == Type.BOOLEAN)
			{
				if(!value1.equalsIgnoreCase(value2))
				{
					return "TRUE";
				}
				return "FALSE";
			}
			else if(type == Type.INTEGER)
			{
				if(Integer.parseInt(Type.getCanonicalFormOf(value1)) != Integer.parseInt(Type.getCanonicalFormOf(value2)))
				{
					return "TRUE";
				}
				return "FALSE";
			}
			else if(type == Type.BIT_STRING)
			{
				String val1 = Type.getCanonicalFormOf(value1);
				String val2 = Type.getCanonicalFormOf(value2);

				// Compare the vectors, index by index, from left to right
				for(int index  = 0; index < val1.length(); ++ index)
				{
					if(val1.charAt(index) != val2.charAt(index))
					{
						return "TRUE";
					}	
				}
				
				// They are equal
				return "FALSE";
			}
			return "ERROR";
		}
	},
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Left operand is an array (bit or boolean based) type, right operand is an integer type
	// Logical shift fills the void using the leftmost member of base type for an array element
	// Arithmetic shifts retain the old value in the void
	// Returns a same-sized array as the left operand
	/**
	 * SLL - shift left logical
	 */
	SLL(2, 2, "SLL")
	{
		// Operands are different so we need to override the default operand checks
		void prePerformChecks(String value1, String value2)
		{
			if(Type.getTypeOf(value1) != Type.BIT_STRING || Type.getTypeOf(value2) != Type.INTEGER)
			{
				throw new RuntimeException("Shift operators require types BIT_VECTOR INTEGER not " + Type.getTypeOf(value1) + " " + Type.getTypeOf(value2));
			}
		}
		
		@Override
		String perform(String value1, String value2)
		{
			prePerformChecks(value1, value2);

			String number = Type.getCanonicalFormOf(value1);
			number = number.substring(1, number.length() - 1);
			int shiftAmnt = Integer.parseInt(Type.getCanonicalFormOf(value2)) % number.length();

			// Append the correct amount of 0 padding to the end of the bit string and trim to original length
			String padding = "";
			while(shiftAmnt > 0)
			{
				padding += "0";
				--shiftAmnt;
			}
			
			return "\"" + number.substring(shiftAmnt) + padding + "\"";
		}
		
		@Override
		boolean canOperateOnType(Type type)
		{
			return type == Type.BIT_STRING || type == Type.INTEGER;
		}
	},
	/**
	 * SRL - shift right logical
	 */
	SRL(2, 2, "SRL")
	{
		// Operands are different so we need to override the default operand checks
		void prePerformChecks(String value1, String value2)
		{
			if(Type.getTypeOf(value1) != Type.BIT_STRING || Type.getTypeOf(value2) != Type.INTEGER)
			{
				throw new RuntimeException("Shift operators require types BIT_VECTOR INTEGER not " + Type.getTypeOf(value1) + " " + Type.getTypeOf(value2));
			}
		}
		
		@Override
		String perform(String value1, String value2)
		{
			prePerformChecks(value1, value2);

			String number = Type.getCanonicalFormOf(value1);
			number = number.substring(1, number.length() - 1);
			int shiftAmnt = Integer.parseInt(Type.getCanonicalFormOf(value2)) % number.length();

			// Append the correct amount of 0 padding to the front of the bit string and trim to original length
			String padding = "";
			while(shiftAmnt > 0)
			{
				padding += "0";
				--shiftAmnt;
			}
			
			return "\"" + padding + number.substring(0, number.length() - shiftAmnt) + "\"";
		}
		
		@Override
		boolean canOperateOnType(Type type)
		{
			return type == Type.BIT_STRING || type == Type.INTEGER;
		}
	},
	/**
	 * SLA - shift left arithmetic
	 */
	SLA(2, 2, "SLA")
	{
		// Operands are different so we need to override the default operand checks
		void prePerformChecks(String value1, String value2)
		{
			if(Type.getTypeOf(value1) != Type.BIT_STRING || Type.getTypeOf(value2) != Type.INTEGER)
			{
				throw new RuntimeException("Shift operators require types BIT_VECTOR INTEGER not " + Type.getTypeOf(value1) + " " + Type.getTypeOf(value2));
			}
		}
		
		@Override
		String perform(String value1, String value2)
		{
			prePerformChecks(value1, value2);

			String number = Type.getCanonicalFormOf(value1);
			number = number.substring(1, number.length() - 1);
			int shiftAmnt = Integer.parseInt(Type.getCanonicalFormOf(value2)) % number.length();

			// Append the correct amount of rightmost digit padding to the end of the bit string and trim to original length
			String padding = "";
			while(shiftAmnt > 0)
			{
				padding += number.charAt(number.length() - 1);
				--shiftAmnt;
			}
			
			return "\"" + number.substring(shiftAmnt) + padding + "\"";
		}
		
		@Override
		boolean canOperateOnType(Type type)
		{
			return type == Type.BIT_STRING || type == Type.INTEGER;
		}
	},
	/**
	 * SRA - shift right arithmetic
	 */
	SRA(2, 2, "SRA")
	{
		// Operands are different so we need to override the default operand checks
		void prePerformChecks(String value1, String value2)
		{
			if(Type.getTypeOf(value1) != Type.BIT_STRING || Type.getTypeOf(value2) != Type.INTEGER)
			{
				throw new RuntimeException("Shift operators require types BIT_VECTOR INTEGER not " + Type.getTypeOf(value1) + " " + Type.getTypeOf(value2));
			}
		}
		
		@Override
		String perform(String value1, String value2)
		{
			prePerformChecks(value1, value2);

			String number = Type.getCanonicalFormOf(value1);
			number = number.substring(1, number.length() - 1);
			int shiftAmnt = Integer.parseInt(Type.getCanonicalFormOf(value2)) % number.length();

			// Append the correct amount of leftmost padding to the front of the bit string and trim to original length
			String padding = "";
			while(shiftAmnt > 0)
			{
				padding += number.charAt(0);
				--shiftAmnt;
			}
			
			return "\"" + padding + number.substring(0, number.length() - shiftAmnt) + "\"";
		}
		
		@Override
		boolean canOperateOnType(Type type)
		{
			return type == Type.BIT_STRING || type == Type.INTEGER;
		}
	},
	/**
	 * ROL - rotate left
	 */
	ROL(2, 2, "ROL")
	{
		// Operands are different so we need to override the default operand checks
		void prePerformChecks(String value1, String value2)
		{
			if(Type.getTypeOf(value1) != Type.BIT_STRING || Type.getTypeOf(value2) != Type.INTEGER)
			{
				throw new RuntimeException("Shift operators require types BIT_VECTOR INTEGER not " + Type.getTypeOf(value1) + " " + Type.getTypeOf(value2));
			}
		}
		
		@Override
		String perform(String value1, String value2)
		{
			prePerformChecks(value1, value2);

			String number = Type.getCanonicalFormOf(value1);
			number = number.substring(1, number.length() - 1);
			int shiftAmnt = Integer.parseInt(Type.getCanonicalFormOf(value2)) % number.length();

			// Catch 0 shift amount case
			if(shiftAmnt == 0)
			{
				return value1;
			}
			
			return "\"" + number.substring(shiftAmnt) + number.substring(0, shiftAmnt) + "\"";
		}
		
		@Override
		boolean canOperateOnType(Type type)
		{
			return type == Type.BIT_STRING || type == Type.INTEGER;
		}
	},
	/**
	 * ROR - rotate right
	 */
	ROR(2, 2, "ROR")
	{
		// Operands are different so we need to override the default operand checks
		void prePerformChecks(String value1, String value2)
		{
			if(Type.getTypeOf(value1) != Type.BIT_STRING || Type.getTypeOf(value2) != Type.INTEGER)
			{
				throw new RuntimeException("Shift operators require types BIT_VECTOR INTEGER not " + Type.getTypeOf(value1) + " " + Type.getTypeOf(value2));
			}
		}
		
		@Override
		String perform(String value1, String value2)
		{
			prePerformChecks(value1, value2);

			String number = Type.getCanonicalFormOf(value1);
			number = number.substring(1, number.length() - 1);
			int shiftAmnt = Integer.parseInt(Type.getCanonicalFormOf(value2)) % number.length();

			// Catch 0 shift amount case
			if(shiftAmnt == 0)
			{
				return value1;
			}
			
			return "\"" + number.substring(number.length() - shiftAmnt) + number.substring(0, number.length() - shiftAmnt) + "\"";
		}
		
		@Override
		boolean canOperateOnType(Type type)
		{
			return type == Type.BIT_STRING || type == Type.INTEGER;
		}
	},
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * + - addition
	 */
	ADD(3, 2, "+")
	{
		@Override
		String perform(String value1, String value2)
		{
			prePerformChecks(value1, value2);
			
			String opA = Type.getCanonicalFormOf(value1);
			String opB = Type.getCanonicalFormOf(value2);
			
			if(Type.getTypeOf(value1) == Type.BIT)
			{
				if(opA.charAt(1) == '1')
				{
					return opB.charAt(1) == '1' ? "'" + 0 + "'" : opA;
				}
				return opB;
			}
			else if(Type.getTypeOf(value1) == Type.BIT_STRING)
			{
				BigInteger stringA = new BigInteger(opA.substring(1, opA.length() - 1), 2);
				BigInteger stringB = new BigInteger(opB.substring(1, opB.length() - 1), 2);
				String result = stringA.add(stringB).toString(2);
				
				return "\"" + result.substring(result.length() - Type.BIT_STRING.getLengthOf(value1)) + "\"";
			}
			else if(Type.getTypeOf(value1) == Type.INTEGER)
			{
				return "" + (Integer.parseInt(opA) + Integer.parseInt(opB));
			}
			
			throw new RuntimeException("Value " + value1 + " isn't the correct type for operator " + this);
		}
		
		@Override
		boolean canOperateOnType(Type type)
		{
			return type == Type.BIT || type == Type.BIT_STRING || type == Type.INTEGER;
		}
	},
	/**
	 * - - subtraction
	 */
	SUB(3, 2, "-")
	{
		@Override
		String perform(String value1, String value2)
		{
			prePerformChecks(value1, value2);
			
			String opA = Type.getCanonicalFormOf(value1);
			String opB = Type.getCanonicalFormOf(value2);
			
			if(Type.getTypeOf(opA) == Type.BIT)
			{
				if(opA.charAt(1) == '1')
				{
					return opB.charAt(1) == '1' ? "'" + 0 + "'" : opA;
				}
				return opB;
			}
			else if(Type.getTypeOf(opA) == Type.BIT_STRING)
			{
				BigInteger stringA = new BigInteger(opA.substring(1, opA.length() - 1), 2);
				BigInteger stringB = new BigInteger(opB.substring(1, opB.length() - 1), 2);
				String result = padWithLeadingZeros(stringA.subtract(stringB).toString(2), Type.BIT_STRING.getLengthOf(opA));
				
				return "\"" + result.substring(result.length() - Type.BIT_STRING.getLengthOf(opA)) + "\"";
			}
			else if(Type.getTypeOf(value1) == Type.INTEGER)
			{
				return "" + (Integer.parseInt(opA) - Integer.parseInt(opB));
			}
			
			throw new RuntimeException("Value " + value1 + " isn't the correct type for operator " + this);
		}
		
		@Override
		boolean canOperateOnType(Type type)
		{
			return type == Type.BIT || type == Type.BIT_STRING || type == Type.INTEGER;
		}
	},
	// Operands must be arrays or elements of the same type
	// Concating two scalars of the same type forms an array of length 2
	/**
	 * &amp; - concatenation
	 */
	CONCAT(3, 2, "&")
	{
		void prePerformChecks(String value1, String value2)
		{
			if(!canOperateOnValue(value1))
			{
				throw new RuntimeException("Operator " + this + " cannot operate on operand " + value1 + " of type " + Type.getTypeOf(value1));
			}
			
			if(!canOperateOnValue(value2))
			{
				throw new RuntimeException("Operator " + this + " cannot operate on operand " + value2 + " of type " + Type.getTypeOf(value2));
			}
		}
		
		@Override
		String perform(String value1, String value2)
		{
			prePerformChecks(value1, value2);
			
			return "\"" + value1.substring(1, value1.length() - 1) + value2.substring(1, value2.length() - 1) + "\"";
		}
		@Override
		boolean canOperateOnType(Type type)
		{
			return type == Type.BIT || type == Type.BIT_STRING;
		}
	},
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Operates on and returns an integer
	/**
	 * - - negate
	 */
	NEG(4, 1, "-")
	{
		@Override
		String perform(String value1, String value2)
		{
			prePerformChecks(value1, value2);
			return "" + (-1 * Integer.parseInt(Type.getCanonicalFormOf(value1)));
		}
		@Override
		boolean canOperateOnType(Type type)
		{
			return type == Type.INTEGER;
		}
	},
	/**
	 * + - positive
	 */
	POS(4, 1, "+")
	{
		@Override
		String perform(String value1, String value2)
		{
			prePerformChecks(value1, value2);
			if(value2 != null)
			{
				throw new RuntimeException("Unary operator + cannot have two operands: " + value1 + " and " + value2);
			}
			return "" + Integer.parseInt(Type.getCanonicalFormOf(value1));
		}
		@Override
		boolean canOperateOnType(Type type)
		{
			return type == Type.INTEGER;
		}
	},
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////	
	// Operands and results are integers
	/**
	 * / - division
	 */
	DIV(5, 2, "/")
	{
		@Override
		String perform(String value1, String value2)
		{
			prePerformChecks(value1, value2);
			return "" + (Integer.parseInt(Type.getCanonicalFormOf(value1)) / Integer.parseInt(Type.getCanonicalFormOf(value2)));
		}
		
		@Override
		boolean canOperateOnType(Type type)
		{
			return type == Type.INTEGER;
		}
	},
	/**
	 * * - multiplication
	 */
	MUL(5, 2, "*")
	{
		@Override
		String perform(String value1, String value2)
		{
			prePerformChecks(value1, value2);
			return "" + (Integer.parseInt(Type.getCanonicalFormOf(value1)) * Integer.parseInt(Type.getCanonicalFormOf(value2)));
		}
		
		@Override
		boolean canOperateOnType(Type type)
		{
			return type == Type.INTEGER;
		}
	},
	// A rem B == (|A| rem |B|)*(A/|A|)
	/**
	 * REM - remainder
	 */
	REM(5, 2, "REM")
	{
		@Override
		String perform(String value1, String value2)
		{
			prePerformChecks(value1, value2);
			int opA = Integer.parseInt(Type.getCanonicalFormOf(value1));
			int opB = Integer.parseInt(Type.getCanonicalFormOf(value2));
			
			return "" + (opA - (opA/opB)*opB);
		}
		@Override
		boolean canOperateOnType(Type type)
		{
			return type == Type.INTEGER;
		}
	},
	// A mod B == ((A rem B) + B) rem B 
	/**
	 * MOD - modulus
	 */
	MOD(5, 2, "MOD")
	{
		@Override
		String perform(String value1, String value2)
		{
			prePerformChecks(value1, value2);
			int opA = Integer.parseInt(Type.getCanonicalFormOf(value1));
			int opB = Integer.parseInt(Type.getCanonicalFormOf(value2));
			int BPlusAREMB = ((opA - (opA/opB)*opB) + opB);
			
			return "" + (BPlusAREMB - (BPlusAREMB/opB)*opB);
		}
		@Override
		boolean canOperateOnType(Type type)
		{
			return type == Type.INTEGER;
		}
	},
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * ** - power
	 */
	POW(6, 2, "**")
	{
		@Override
		String perform(String value1, String value2)
		{
			prePerformChecks(value1, value2);
			return "" + (int)Math.pow(Integer.parseInt(Type.getCanonicalFormOf(value1)), Integer.parseInt(Type.getCanonicalFormOf(value2)));
		}
		
		@Override
		boolean canOperateOnType(Type type)
		{
			return type == Type.INTEGER;
		}
	},
	/**
	 * ABS - absolute value
	 */
	ABS(6, 1, "ABS")
	{
		@Override
		String perform(String value1, String value2)
		{
			prePerformChecks(value1, value2);
			return "" + Math.abs(Integer.parseInt(Type.getCanonicalFormOf(value1)));
		}
		
		@Override
		boolean canOperateOnType(Type type)
		{
			return type == Type.INTEGER;
		}
	},
	// Can have bits, booleans, and bit vectors as an operand
	/**
	 * NOT - not
	 */
	NOT(6, 1, "NOT")
	{
		@Override
		String perform(String value1, String value2)
		{
			prePerformChecks(value1, value2);
			Type type = Type.getTypeOf(value1);
			
			if(type == Type.BIT)
			{
				if(value1.charAt(1) == '1')
				{
					return "'0'";
				}
				return "'1'";
			}
			else if(type == Type.BOOLEAN)
			{
				if(value1.equalsIgnoreCase("true"))
				{
					return "FALSE";
				}
				return "TRUE";
			}
			else if(type == Type.BIT_STRING)
			{
				String val1 = type.getNormalFormOf(value1);
				val1 = val1.substring(1, val1.length() - 1);
				
				// Remember the length so we can pad with leading zeros
				int length = val1.length();
				
				return "\"" + padWithLeadingZeros((new BigInteger(val1, 2)).not().toString(2), length) + "\"";
			}
			return "";
		}
		
		@Override
		boolean canOperateOnType(Type type)
		{
			return type == Type.BIT || type == Type.BIT_STRING || type == Type.BOOLEAN;
		}
	},
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * ' - attribute dereference
	 */
	/*ATTRIB(7, 2, "'", Type.INTEGER, Type.NAME)
	{
		@Override
		String perform(String name, String attribute)
		{
			// Make sure the name is in the list of names
			if(!nameList.containsKey(name) || !nameList.get(name).hasAttribute(attribute))
			{
				throw new RuntimeException("Cannot get attribute " + attribute + " for name " + name);
			}
			return nameList.get(name).getAttribute(attribute);
		}
		@Override
		boolean canOperateOnType(Type type)
		{
			return true;
		}
	},
	/**
	 * No operation - used internally when expression contains only a reference
	 * to a variable.
	 */
	NOP(8, 1, "")
	{
		@Override
		String perform(String value1, String value2)
		{
			return value1;
		}
	};

	final int precedence;
	final int numberOfOperands;
	final String string;

	Operator(final int precedence, final int numberOfOperands, final String string)
	{
		this.precedence = precedence;
		this.numberOfOperands = numberOfOperands;
		this.string = string;
	}
	
	// Overridden by each operation to check, then perform the operation on the passed values
	abstract String perform(String value1, String value2);
	
	// Verifies that an operator can act on the passed operand types and that the operand types are compatiable
	void prePerformChecks(String value1, String value2)
	{
		if(numberOfOperands == 1)
		{
			if(!canOperateOnValue(value1))
			{
				throw new RuntimeException("Operator cannot operate on operand " + value1 + " of type " + Type.getTypeOf(value1));
			}
		}
		else if(numberOfOperands == 2)
		{
			if(!canOperateOnValue(value1))
			{
				throw new RuntimeException("Operator cannot operate on operand " + value1 + " of type " + Type.getTypeOf(value1));
			}
			
			if(!canOperateOnValue(value2))
			{
				throw new RuntimeException("Operator cannot operate on operand " + value2 + " of type " + Type.getTypeOf(value2));
			}
			
			// Verify values match in type and dimension
			if(Type.getTypeOf(value1) != Type.getTypeOf(value2) || Type.getTypeOf(value1).getLengthOf(value1) != Type.getTypeOf(value2).getLengthOf(value2))
			{
				throw new RuntimeException("Operator cannot operate on operands " + value1 + " and " + value2 + " as they don't match");
			}
		}
	}
	
	// Returns whether a given operator can operate on the passed type
	boolean canOperateOnType(Type type)
	{
		return true;
	}
	
	// Returns true if a given operator can operate on the passed string
	boolean canOperateOnValue(String value)
	{
		// Lookup the type of the value
		return canOperateOnType(Type.getTypeOf(value));
	}
	
	// Internal utility method which returns a string with the first parameter padded with enough leading zeros to make it as long as the second parameter
	private static String padWithLeadingZeros(String value, int length)
	{
		String padding = "";
		
		for(int toAdd = length - value.length(); toAdd > 0; --toAdd)
		{
			padding = "0" + padding;
		}
		
		return  padding + value;
	}
}
