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

// Create a stack of possible matches and pick the best match to handle * versus ** case
package net.java.dev.eval;
import java.util.ArrayList;

final class Tokeniser
{
	private static final boolean DEBUG = false;
	static final Character START_NEW_EXPRESSION = new Character('(');

	private final String expression;
	private int position;
	private Operator pushedBackOperator = null;

	Tokeniser(String string)
	{
		expression = string;
		position = 0;
	}

	int getPosition()
	{
		return position;
	}

	void setPosition(int position)
	{
		this.position = position;
	}

	void pushBack(Operator operator)
	{
		pushedBackOperator = operator;
	}

	Operator getOperator(char endOfExpressionChar)
	{
		/* Use any pushed back operator. */
		if(pushedBackOperator != null)
		{
			Operator operator = pushedBackOperator;
			pushedBackOperator = null;
			return operator;
		}

		// Move the current position to start of the next token (skipping white space)
		final int len = expression.length();
		char ch = 0;
		while(position < len && Character.isWhitespace(ch = expression.charAt(position)))
		{
			if(DEBUG)
				System.out.println("Skipping character: " + ch);
			position++;
		}
		
		// Check for the end of the expression
		if(position == len)
		{
			// Return an operator that represents the end of the expression if there is no preexisting marker for it
			if(endOfExpressionChar == 0)
			{
				return Operator.END;
			}
			
			throw new RuntimeException("missing " + endOfExpressionChar + " at position " + position + " char found is " + ch);
		}

		// Look for a symbol that tells us that we are at the end of an expression
		if(ch == endOfExpressionChar)
		{
			if(DEBUG)
				System.out.println("Expression end: " + position);
			++position;
			return Operator.END;
		}

		// Get the best operator match given the current position, update the position, and return the operator
		// Ignore unary operators as they show up in the place of operands, otherwise ambiguous parsing will occur
		Operator bestOp = getOpAtCurrentPosition(true);
		
		if(bestOp == null)
		{
			throw new RuntimeException("operator expected at position " + position + " instead of '" + ch + "'");
		}
		
		if(DEBUG)
			System.out.println("Found operator: " + bestOp);
		position += bestOp.string.length();
		return bestOp;
	}

	/**
	 * Called when an operand is expected next.
	 * 
	 * @return one of:
	 *         <UL>
	 *         <LI>a {@link BigInteger} value;</LI>
	 *         <LI>the {@link String} name of a variable;</LI>
	 *         <LI>{@link Tokeniser#START_NEW_EXPRESSION} when an opening
	 *         parenthesis is found: </LI>
	 *         <LI>or {@link Operator} when a unary operator is found in front
	 *         of an operand</LI>
	 *         </UL>
	 * 
	 * @throws RuntimeException
	 *             if the end of the string is reached unexpectedly.
	 */
	Object getOperand()
	{
		/* Skip whitespace */
		final int len = expression.length();
		char ch = 0;
		while (position < len && Character.isWhitespace(ch = expression.charAt(position)))
		{
			if(DEBUG)
				System.out.println("Skipping character: " + ch);
			position++;
		}
		
		// Check for the end of the expression
		if(position == len)
		{
			throw new RuntimeException("operand expected but end of expression found");
		}

		// Look if the operand is actually a sub-expression
		if(ch == '(')
		{
			if(DEBUG)
				System.out.println("Expression start: " + position);
			position++;
			return START_NEW_EXPRESSION;
		}
		// Verify that this identifier isn't actually an operator, return the op if it is
		// Especially look for unary operators
		else if(getOpAtCurrentPosition(false) != null)
		{
			Operator op = getOpAtCurrentPosition(false);
			position += op.string.length();
			if(DEBUG)
				System.out.println("Found operator: " + op);
			return op;
		}
		// Try to grab a literal/identifier string
		else if(Character.isLetterOrDigit(ch) || ch == '"' || ch == '\'')
		{
			// Save the starting position and keep looking for an end of an literal/identifier 
			int start = position;
			char currentChar = 0;
			while(++position < len && (Character.isLetterOrDigit(currentChar = expression.charAt(position)) || currentChar == '_' || currentChar == '#' || currentChar == '"' || currentChar == '\''))
			{
				;
			}

			String name = expression.substring(start, position);
			if(DEBUG)
				System.out.println("Found operand: " + name);
			// Return the literal or identifier
			return Type.getTypeOf(name) == null ? name : Type.getCanonicalFormOf(name);
		}
		
		throw new RuntimeException("operand expected but '" + ch + "' found");
	}

	// Internal method that finds the best operator, if any, that matchs the current front of the expression
	// as given by the current position
	private Operator getOpAtCurrentPosition(boolean ignoreUnaryOps)
	{
		ArrayList<Operator> matchingOps = new ArrayList<Operator>();
		
		for(Operator op : Operator.values())
		{
			// Don't process non-ops
			if(op.string == null || op.string.length() == 0)
			{
				continue;
			}
			
			try
			{
				String opString = op.string;
				
				// Continue if unary operator and we are ignoring them
				if(ignoreUnaryOps && op.numberOfOperands == 1)
				{
					continue;
				}
				
				// Does the current front of the string match this operation's string
				if(opString.equalsIgnoreCase(expression.substring(position, position + opString.length())))
				{
					// Make sure we don't split up an identifier and treat it as an operator
					// by looking at the next character and see if it is part of a running valid string
					char temp = expression.charAt(position + op.string.length());
					if(expression.length() <= position || opString.length() == 1 || Character.isWhitespace(temp) || (temp != '_' && !Character.isLetterOrDigit(temp)))
					{
						matchingOps.add(op);
					}
				}
			}
			// Catch the case when the expression isn't long enough to have an operator of a given length
			catch(StringIndexOutOfBoundsException sioobe)
			{
				;
			}
		}
		
		// Return null if no matches
		if(matchingOps.size() == 0)
		{
			return null;
		}
		
		// Pick the operator with the longest length as the best match
		Operator bestOp = matchingOps.get(0);
		
		for(int match = 1; match < matchingOps.size(); ++match)
		{
			// This operator is a better match if it matches a longer portion of the front of the string or if is the same operator, but with higher precedence
			if(matchingOps.get(match).string.length() > bestOp.string.length() || (matchingOps.get(match).string.equalsIgnoreCase(bestOp.string) && matchingOps.get(match).precedence > bestOp.precedence))
			{
				bestOp = matchingOps.get(match);
			}
		}
		
		return bestOp;
	}
	
	@Override
	public String toString()
	{
		return expression.substring(0, position) + ">>>" + expression.substring(position);
	}
}
