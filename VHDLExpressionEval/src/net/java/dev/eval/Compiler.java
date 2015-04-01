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

class Compiler
{
	private final Tokeniser tokeniser;

	Compiler(String expression)
	{
		tokeniser = new Tokeniser(expression);
	}

	Operation compile()
	{
		Object expression = compile(null, null, 0, (char) 0, -1);

		/*
		 * If expression is a variable name or constant value then we
		 * need to put into a NOP operation.
		 */
		if(expression instanceof Operation)
		{
			return (Operation)expression;
		}
		return Operation.nopOperationfactory(expression);
	}

	private Object compile(Object preReadOperand, Operator preReadOperator, int nestingLevel, char endOfExpressionChar, int terminatePrecedence)
	{
		Object operand = preReadOperand != null ? preReadOperand : getOperand(nestingLevel);
		Operator operator = preReadOperator != null ? preReadOperator : tokeniser.getOperator(endOfExpressionChar);

		while(operator != Operator.END)
		{
			Object nextOperand = getOperand(nestingLevel);
			Operator nextOperator = tokeniser.getOperator(endOfExpressionChar);
			
			if(nextOperator == Operator.END)
			{
				/* We are at the end of the expression */
				operand = Operation.binaryOperationfactory(operator, operand, nextOperand);
				operator = Operator.END;
			}
			else if(nextOperator.precedence <= terminatePrecedence)
			{
				/*
				 * The precedence of the following operator effectively
				 * brings this expression to an end.
				 */
				operand = Operation.binaryOperationfactory(operator, operand, nextOperand);
				tokeniser.pushBack(nextOperator);
				operator = Operator.END;
			}
			else if(operator.precedence >= nextOperator.precedence)
			{
				/* The current operator binds tighter than any following it */
				operand = Operation.binaryOperationfactory(operator, operand, nextOperand);
				operator = nextOperator;
			}
			else
			{
				/*
				 * The following operator binds tighter so compile the
				 * following expression first.
				 */
				operand = Operation.binaryOperationfactory(operator, operand, compile(nextOperand, nextOperator, nestingLevel, endOfExpressionChar, operator.precedence));
				operator = tokeniser.getOperator(endOfExpressionChar);
			}
		}
		return operand;
	}

	private Object getOperand(int nestingLevel)
	{
		Object operand = tokeniser.getOperand();
		if(operand == Tokeniser.START_NEW_EXPRESSION)
		{
			operand = compile(null, null, nestingLevel + 1, ')', -1);
		}
		else if(operand instanceof Operator)
		{
			/* Can get unary operators when expecting operand */
			return Operation.unaryOperationfactory((Operator) operand, getOperand(nestingLevel));
		}
		return operand;
	}
}
