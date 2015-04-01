package net.java.dev.eval.example.calculator;

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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.eval.Expression;

/**
 * A till roll calculator example.
 * <P>
 * A command line program that uses {@link Expression} to evaluate the
 * expression input on each line of standard input, and then echos the answer to
 * standard output. This is similar to the Unix tool "bc".
 * <P>
 * When an input line that contains an equals (=) then the expression to the
 * right is evaluated and assigned to the variable named on the left.
 * <P>
 * A line containing the single word "show" causes all current variable to be
 * output. The word "quit" ends the program.
 * <P>
 * Example:
 * 
 * <PRE>
 * $ java -jar calculator.jar 
 * x = 4*4
 * y = 5*5
 * x + y
 * 41
 * 
 * show
 * {x=16, y=25}
 * quit
 * </PRE>
 * 
 * @author Reg Whitton
 */
public class Calculator
{
	private final static String ASSIGNMENT_PATTERN = "^\\s*(\\p{Alpha}[\\p{Alnum}_]*)\\s*=([^=].*)$";

	/**
	 * Read expressions and assignments from the input stream and write the
	 * results to the output stream
	 * 
	 * @param inputStream
	 *            the stream that expressions and assignments will be read from.
	 * @param outputStream
	 *            the stream that results will be written to.
	 * @throws RuntimeException
	 *             containing the line number upon any problem.
	 */
	public void execute(InputStream inputStream, OutputStream outputStream,
			OutputStream errorStream)
	{
		final Pattern assignmentPattern = Pattern.compile(ASSIGNMENT_PATTERN);

		final LineNumberReader reader = new LineNumberReader(
				new InputStreamReader(inputStream));
		final PrintWriter writer = new PrintWriter(outputStream);
		final PrintWriter errorWriter = new PrintWriter(errorStream);

		final Map<String, BigDecimal> variables = new LinkedHashMap<String, BigDecimal>();

		String line;
		while ((line = read(reader)) != null)
		{
			line = line.trim();

			/* Ignore blank lines */
			if (line.length() == 0)
			{
				writer.flush();
				continue;

			}
			/* If "show" display current variables */
			if (line.equalsIgnoreCase("show"))
			{
				writer.println(variables);
				writer.flush();
				continue;
			}
			else if (line.equalsIgnoreCase("quit"))
			{
				writer.flush();
				break;
			}

			/* Does this line match the format of an assignment? */
			final Matcher assignmentMatcher = assignmentPattern.matcher(line);
			if (assignmentMatcher.find())
			{
				/*
				 * If line is assignment then place result of expression into
				 * variable
				 */
				String variableName = assignmentMatcher.group(1);
				String expr = assignmentMatcher.group(2);

				BigDecimal result;
				try
				{
					Expression expression = new Expression(expr);
					result = expression.eval(variables);
					variables.put(variableName, result);
				}
				catch (RuntimeException ex)
				{
					errorWriter.println("error at line "
							+ reader.getLineNumber() + ": " + ex.getMessage());
					errorWriter.flush();
				}
			}
			else
			{
				/* If line is not an assignment then output result */
				BigDecimal result;
				try
				{
					Expression expression = new Expression(line);
					result = expression.eval(variables);
					writer.println(result);
				}
				catch (RuntimeException ex)
				{
					errorWriter.println("error at line "
							+ reader.getLineNumber() + ": " + ex.getMessage());
					errorWriter.flush();
				}
				writer.flush();
			}
		}
	}

	private String read(LineNumberReader reader)
	{
		try
		{
			return reader.readLine();
		}
		catch (IOException ioe)
		{
			throw new RuntimeException("I/O error at line "
					+ reader.getLineNumber() + ": " + ioe.getMessage(), ioe);
		}
	}

	/**
	 * The command line program entry point.
	 * 
	 * @param args
	 *            the command line arguments - which are ignored.
	 */
	public static void main(String[] args)
	{
		try
		{
			new Calculator().execute(System.in, System.out, System.err);
		}
		catch (RuntimeException ex)
		{
			System.err.println(ex.getMessage());
		}
	}
}
