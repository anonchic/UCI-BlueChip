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

final class Operation
{
	private static final boolean DEBUG = false;
	final Operator operator;
	final Object operand1;
	final Object operand2;

	// Private constructor used by static operation factories
	private Operation(Operator operator, Object operand1, Object operand2)
	{
		this.operator = operator;
		this.operand1 = operand1;
		this.operand2 = operand2;
	}

	// Create and return a no-op operation
	static Operation nopOperationfactory(Object operand)
	{
		return new Operation(Operator.NOP, operand, null);
	}

	// Create a new unary operation or just return the result, if ready
	static Object unaryOperationfactory(Operator operator, Object operand)
	{
		/*
		 * If values can already be resolved then return result instead of
		 * operation
		 */
		if(operand instanceof String && Type.parsesToType((String) operand))
		{
			String result = operator.perform((String) operand, null); 
			if(DEBUG)
				System.out.println("" + operator + " " + operand + " = " + result);
			return result;
		}
		return new Operation(operator, operand, null);
	}

	// Create a new binary operation or just return the result, if ready
	static Object binaryOperationfactory(Operator operator, Object operand1, Object operand2)
	{
		/*
		 * If values can already be resolved then return result instead of
		 * operation
		 */
		if(operand1 instanceof String && Type.parsesToType((String) operand1) && operand2 instanceof String && Type.parsesToType((String) operand2))
		{
			String result = operator.perform((String) operand1, (String) operand2); 
			if(DEBUG)
				System.out.println("" + operand1 + " " + operator + " " + operand2 + " = " + result);
			return result;
		}
		return new Operation(operator, operand1, operand2);
	}

	// Performs and returns the result of this operation by reducing both operands to directly usable forms, passing these forms to the operator's perform method
	String eval(Map<String, String> variables, Map<String, String> names)
	{
		switch(operator.numberOfOperands)
		{
			case 2:
			{
				String result = operator.perform(evaluateOperand(operand1, variables, names), evaluateOperand(operand2, variables, names)); 
				if(DEBUG)
					System.out.println("" + operand1 + " " + operator + " " + operand2 + " = " + result);
				return result;
			}
			default:
			{	
				String result = operator.perform(evaluateOperand(operand1, variables, names), null); 
				if(DEBUG)
					System.out.println("" + operator + " " + operand1 + " = " + result);
				return result;
			}
		}
	}

	// Reduce the passed operand to a directly usable value and return it
	// May need to evaluate another expression or get a constant from the variables list
	// Throws an exception if there is no suitable way to reduce the operand to a usable form
	private String evaluateOperand(Object operand, Map<String, String> variables, Map<String, String> names)
	{
		// If the operand is the result of another operation, evaluate it and return the result
		if(operand instanceof Operation)
		{
			return ((Operation) operand).eval(variables, names);
		}
		// The operand may already be reduced to a usable form, if so, just return it
		else if(operand instanceof String && Type.parsesToType((String) operand))
		{
			return (String) operand;
		}
		// If the operand is some other string look for it in the variable list
		else if(operand instanceof String)
		{
			String value;
			if (variables == null || (value = variables.get(operand)) == null)
			{
				throw new RuntimeException("no value for variable \"" + operand + "\"");
			}
			return value;
		}
		
		throw new RuntimeException("no options for handling the operand " + operand + " in the operation: " + this);
	}

	@Override
	public String toString()
	{
		switch (this.operator.numberOfOperands)
		{
			case 2:
				return "(" + this.operand1 + this.operator.string + this.operand2 + ")";
			default:
				return "(" + this.operator.string + this.operand1 + ")";
		}
	}
}
