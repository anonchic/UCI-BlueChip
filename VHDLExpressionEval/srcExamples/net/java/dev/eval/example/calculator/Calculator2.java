import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.math.BigInteger;
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
public class Calculator2
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
	public void execute(InputStream inputStream, OutputStream outputStream, OutputStream errorStream)
	{
		final Pattern assignmentPattern = Pattern.compile(ASSIGNMENT_PATTERN);

		final LineNumberReader reader = new LineNumberReader(new InputStreamReader(inputStream));
		final PrintWriter writer = new PrintWriter(outputStream);
		final PrintWriter errorWriter = new PrintWriter(errorStream);

		final Map<String, String> variables = new LinkedHashMap<String, String>();

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

			writer.println("Working with the expression: " + line);
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

				String result;
				try
				{
					Expression expression = new Expression(expr);
					result = expression.eval(variables, null);
					variables.put(variableName, result);
					writer.println(result);
				}
				catch (RuntimeException ex)
				{
					errorWriter.println("error at line " + reader.getLineNumber() + ": " + ex.getMessage());
					ex.printStackTrace(errorWriter);
					errorWriter.flush();
				}
			}
			else
			{
				/* If line is not an assignment then output result */
				String result;
				try
				{
					Expression expression = new Expression(line);
					result = expression.eval(variables, null);
					writer.println(result);
				}
				catch (RuntimeException ex)
				{
					errorWriter.println("error at line " + reader.getLineNumber() + ": " + ex.getMessage());
					ex.printStackTrace(errorWriter);
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
			throw new RuntimeException("I/O error at line " + reader.getLineNumber() + ": " + ioe.getMessage(), ioe);
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
		InputStream inFile;
		try
		{
			try
			{
				inFile = new FileInputStream("express.txt");
				new Calculator2().execute(inFile, System.out, System.err);
			}
			catch(IOException ioe)
			{
				System.err.println("ERROR: Couldn't open expression input file");
			}
			
		}
		catch (RuntimeException ex)
		{
			System.err.println("ERROR: Rintime exception");
			System.err.println(ex.getMessage());
			ex.printStackTrace();
		}
	}
}
