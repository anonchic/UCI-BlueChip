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

import java.util.regex.Pattern;
import java.math.BigInteger;

enum Type
{
	INTEGER("integer")
	{
		// Converts a integer literal to an int then returns the string representation of that int
		// Also checks for malformed/out-of-range values
		String getNormalFormOf(String value)
		{
			value = value.toUpperCase();
			BigInteger number;
			BigInteger exponent = new BigInteger("0");
			
			if(INTEGER_PATTERN.matcher(value).matches())
			{
				number = new BigInteger(removeUnderscoresFrom(value.split("E")[0]));
				
				// Handle the exponent if one exists
				if(value.indexOf('E') > 0)
				{
					exponent = new BigInteger(removeUnderscoresFrom(value.split("E")[1].replace('+', '0')));
				}
			}
			else if(BASED_INTEGER_PATTERN.matcher(value).matches())
			{
				int base = Integer.parseInt(value.split("#")[0]);
				String rawNumber = removeUnderscoresFrom(value.split("#")[1]);
				number = new BigInteger(rawNumber, base);
			}
			else
			{
				throw new RuntimeException("Cannot convert the literal " + value + " into integer");
			}
			
			// Throw an exception if overflow
			if(Math.pow(10, exponent.longValue()) > (long)Integer.MAX_VALUE)
			{
				throw new RuntimeException("Cannot convert the integer literal " + value + " into integer");
			}
			BigInteger toRet = number.multiply(new BigInteger("" + (long)Math.pow(10, exponent.longValue())));
			if(toRet.longValue() > (long)Integer.MAX_VALUE)
			{
				throw new RuntimeException("Cannot convert the integer literal " + value + " into integer");
			}
			
			return "" + toRet.intValue();
		}
	},
	BOOLEAN("boolean")
	{
		String getNormalFormOf(String value)
		{
			return value.toUpperCase();
		}
	},
	BIT("bit")
	{
		String getNormalFormOf(String value)
		{
			return value.toUpperCase();
		}
	},
	BIT_STRING("bit_string")
	{
		String getNormalFormOf(String value)
		{
			value = removeUnderscoresFrom(value);
			// Normalize the string to a binary string
			switch(value.charAt(0))
			{
				case 'b':
				case 'B':
					return value.substring(1);
				case 'o':
				case 'O':
					return "\"" + padWithLeadingZeros((new BigInteger(value.substring(2, value.length() - 1), 8)).toString(2), getLengthOf(value)) + "\"";
				case 'x':
				case 'X':
					return "\"" + padWithLeadingZeros((new BigInteger(value.substring(2, value.length() - 1), 16)).toString(2), getLengthOf(value)) + "\"";
			}
			return value;
		}
		
		public int getLengthOf(String value)
		{
			value = removeUnderscoresFrom(value);
			char firstChar = value.substring(0, 1).toUpperCase().charAt(0);
			
			// Normalize to the length of a binary string
			return firstChar == '"' ? value.length() - 2 : (value.length() - 3) * (firstChar == 'B' ? 1 : (firstChar == 'O' ? 3 : 4));
		}
	};

	final String name;

	Type(String name)
	{
		this.name = name;
	}
	
	// Regular expressions used to determine the type of the passed string
	private static final String INTEGER_STRINGS = "[\\-]?[0-9][0-9_]*([eE][+]?([0-9][0-9_]*))?";
	private static final String BASED_INTEGER_STRINGS = "((1[0-6])|[2-9])[#][0-9a-fA-F][0-9a-fA-F_]*[#]";
	private static final String BOOLEAN_STRINGS = "TRUE|FALSE";
	private static final String BIT_STRINGS = "'[01]'";
	private static final String BIT_STRING_STRINGS = "([bB]?\"[01][01_]*\")|([oO]?\"[0-7][0-7_]*\")|([xX]?\"[0-9a-fA-F][0-9a-fA-F_]*\")";
	private static final Pattern INTEGER_PATTERN = Pattern.compile(INTEGER_STRINGS);
	private static final Pattern BASED_INTEGER_PATTERN = Pattern.compile(BASED_INTEGER_STRINGS);
	private static final Pattern BOOLEAN_PATTERN = Pattern.compile(BOOLEAN_STRINGS);
	private static final Pattern BIT_PATTERN = Pattern.compile(BIT_STRINGS);
	private static final Pattern BIT_STRING_PATTERN = Pattern.compile(BIT_STRING_STRINGS);
	
	// Static utility methods
	
	// Finds the type of then returns a canonical representation of the passed value
	public static String getCanonicalFormOf(String value)
	{
		return getTypeOf(value).getNormalFormOf(value);
	}
	
	// Checks the passed value to see if it is an integer literal in VHDL
	public static boolean isIntegerLiteral(String value)
	{
		return INTEGER_PATTERN.matcher(value).matches() || BASED_INTEGER_PATTERN.matcher(value).matches();
	}
	// Checks the passed value to see if it is a boolean literal in VHDL
	public static boolean isBooleanLiteral(String value)
	{
		return BOOLEAN_PATTERN.matcher(value.toUpperCase()).matches();
	}
	// Checks the passed value to see if it is a bit literal in VHDL
	public static boolean isBitLiteral(String value)
	{
		return BIT_PATTERN.matcher(value).matches();
	}
	// Checks the passed value to see if it is a bit string literal in VHDL
	public static boolean isBitStringLiteral(String value)
	{
		return BIT_STRING_PATTERN.matcher(value).matches();
	}
	
	// Return the types that fits the passed value
	public static Type getTypeOf(String value)
	{
		if(isIntegerLiteral(value))
		{
			return Type.INTEGER;
		}
		else if(isBooleanLiteral(value))
		{
			return Type.BOOLEAN;
		}
		else if(isBitLiteral(value))
		{
			return Type.BIT;
		}
		else if(isBitStringLiteral(value))
		{
			return Type.BIT_STRING;
		}
		
		return null;
	}
	
	// Returns true if the passed value parses to one of the available types
	public static boolean parsesToType(String value)
	{
		return getTypeOf(value) == null ? false : true;
	}
	
	// Methods designed to be overridden by each type
	
	// Returns the number of elements in an array, or -1 if not an array
	public int getLengthOf(String value)
	{
		return -1;
	}
	
	// Each type implements this method to put all acceptable values into a common form that makes them directly operatable
	abstract String getNormalFormOf(String value);
	
	// Internal utility methods

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
	
	// Removes the underscores from a given string and returns the condensed version
	private static String removeUnderscoresFrom(String value)
	{
		String rawNumber = "";
		
		for(int index = 0; index < value.length(); ++index)
		{
			// Ignore underscores
			if(value.charAt(index) != '_')
			{
				rawNumber = rawNumber + value.charAt(index);
			}
		}
		
		return rawNumber;
	}
}
