import java.io.*;
import java.util.*;

public class PaintDotPairs
{
	private static final String OBJECT_NAME_PREFIX = "/testbench/cpu/l3/cpu__0/u0/p0/iu0/";

	public static void main(String args[])
	{
		HashSet<String> nameList = new HashSet<String>();

		if(args.length != 1)
		{
			System.out.println("Usage: java PaintDot pairFile.txt");
			System.exit(0);
		}

		try
		{
			BufferedReader inFile = new BufferedReader(new FileReader(args[0]));
			String line;

			while((line = inFile.readLine()) != null)
			{
				String[] tokens = line.split("&");
				tokens = tokens[0].split(" ");
				nameList.add(tokens[0]);
				tokens = line.split("&");
				tokens = tokens[1].split(" ");
				nameList.add(tokens[0]);
			}
	
			inFile.close();
		}
		catch(IOException ioe)
		{
			System.err.println("ERROR: Couldn't open file " + args[0]);
			System.exit(0);
		}

		for(Iterator<String> name = nameList.iterator(); name.hasNext(); )
		{
			String temp = name.next();

			try
			{
				Integer.parseInt(temp);
				continue;
			}
			catch(Exception e)
			{
				if(temp.equalsIgnoreCase("TRUE") || temp.equalsIgnoreCase("FALSE"))
				{
					continue;
				}
			}

			System.out.println("\t\"" + temp.substring(OBJECT_NAME_PREFIX.length()) + "\" [style=filled]");
		}
	}
}
