import java.io.FileReader;
import java.io.BufferedReader;
import java.lang.Math;
import java.util.ArrayList;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import VHDLUtils.*;

public class PairsToVHDL
{
	private static final boolean DEBUG = false;
	private static ArrayList<DataflowPair> pairs = new ArrayList<DataflowPair>();
	private static ArrayList<VHDLType> typeList = new ArrayList<VHDLType>();
	private static ArrayList<VHDLObject> objectList = new ArrayList<VHDLObject>();
	private static ArrayList<String> shadowAssigns = new ArrayList<String>(100);
	private static String LCP = null;
	
	public static void main(String args[])
	{
		if(args.length != 3)
		{
			System.out.println("Usage: java PairsToVHDL typesFile.ext objectsFile.ext pairsFile.ext");
			System.exit(0);
		}

		// Retrieve type information from the type file
		System.out.println("Parsing types from input file " + args[0]);
		getTypes(args[0]);
		System.out.println("There are a total of " + typeList.size() + " types");
		
		// Retrieve base object information from the objects file
		System.out.println("Parsing object information from input file " + args[1]);
		getObjectInformation(args[1]);
		System.out.println("There are a total of " + objectList.size() + " objects");
		
		// Parse dataflow pairs from input file
		System.out.println("Parsing dataflow pairs from: " + args[2]);
		parsePairsFromFile(args[2]);
		System.out.println("Parsing complete, found " + pairs.size() + " pairs");
		System.out.println("Trimming pair term names based on the longest common prefix " + LCP);
		trimCommonPairPrefix();

		// Parse dataflow pairs from input file
		System.out.println("Removing pairs that aren't in the object list");
		removeOrphanPairs();
		System.out.println("Trimming complete, " + pairs.size() + " pairs remaining\n");
		
		// Make sure there will be no naming conflicts
		reportNameConflicts();

		// Print the signals required for infrastructure
		printDCESignals();
		
		// Print the shadow signal declarations
		printShadowSignals();

		// Print signal declarations
		printObjectDecls();
		
		// Print shadow assignments
		printShadowAssignments();

		// Print delay process
		printDelayProcess();

		// Print handlers
		printHandlers();
		
		// Print the or reduction tree
		printOrReduce();
		
		// Print the rest of the DCE support code
		printSupportCode();
	}
	
	// Parse the given types file creating types and adding them to the types list
	// Types file is in the format name&isComposite&isArray\n[leftBound&rightBound&telementTypeName]?[fieldName&type\n]*
	// Assumes no duplicates or blank lines
	private static void getTypes(String filename)
	{
		BufferedReader inFile = null;
		
		// Open the file
		try
		{
			inFile = new BufferedReader(new FileReader(filename));
		}
		catch(IOException e)
		{
			System.err.println("ERROR: Couldn't open file " + filename);
			System.exit(-1);
		}
		
		// Look for and parse types until the end of the file
		try
		{
			String line;
			VHDLType newType = null;
			while((line = inFile.readLine()) != null)
			{
				String tokens[] = line.toUpperCase().split("&");
				
				// Type declaration lines have three parts
				if(tokens.length == 3)
				{
					newType = new VHDLType(tokens[0], Boolean.parseBoolean(tokens[1]));
					newType.setArray(Boolean.parseBoolean(tokens[2]));
					
					// If this type represents an array, get its bounds
					if(newType.isArray())
					{
						String bounds[] = inFile.readLine().toUpperCase().trim().split("&");
						newType.setArrayBounds(Integer.parseInt(bounds[0]), Integer.parseInt(bounds[1]));
						newType.setArrayElementType(typeList.get(typeList.indexOf(new VHDLType(bounds[2], false))));
					}
					
					typeList.add(newType);
				}
				// Field information contains two parts
				else if(tokens.length == 2)
				{
					// Find the type associated with the field in the type list
					int indexInList;
					if((indexInList = typeList.indexOf(new VHDLType(tokens[1], false))) == -1)
					{
						System.err.println("ERROR: Can't find the field type " + tokens[1] + " in the type list");
						System.exit(-1);
					}
					// Add it as a field to the most recently added type
					newType.addField(new VHDLTypeField(tokens[0], typeList.get(indexInList)));
				}
				else
				{
					System.err.println("ERROR: Types input file contains malformed data");
					System.exit(-1);
				}
			}
		}
		catch(IOException e)
		{
			System.err.println("ERROR: Couldn't read from file " + filename);
			System.exit(-1);
		}
		
		// Close the file
		try
		{
			inFile.close();
		}
		catch(IOException e)
		{
			System.err.println("ERROR: Couldn't close file " + filename);
		}
	}
	
	// Goes through the type list, creating a string to be used during signal declarations for each anonymous type
	// The string is directly linked to its replacement string through the typeMapping variable
	private static String reWriteAnonymousTypes(VHDLType currentType)
	{
		if(!currentType.getName().startsWith("ANONYMOUSNAME"))
		{
			return currentType.getName();
		}
			
		// Only can handle anonymous types which are arrays that have element types of std_logic or std_ulogic
		if(!currentType.isArray() || !(currentType.getArrayElementType().getName().equals("STD_LOGIC") || currentType.getArrayElementType().getName().equals("STD_ULOGIC")))
		{
			return currentType.getName();
		}
		
		// Create and add to the remapping structure a string that represents a usable type
		if(currentType.getLeftBound() >= currentType.getRightBound())
		{
			return "" + currentType.getArrayElementType().getName() + "_VECTOR(" + currentType.getLeftBound() + " downto " + currentType.getRightBound() + ")";
		}
		else
		{
			return "" + currentType.getArrayElementType().getName() + "_VECTOR(" + currentType.getLeftBound() + " to " + currentType.getRightBound() + ")";
		}
	}
	
	// Parse the given objects file creating a list of object names and their types
	// File is in the format name&type
	// Assumes no duplicates or blank lines
	private static void getObjectInformation(String filename)
	{
		BufferedReader inFile = null;
		
		// Open the file
		try
		{
			inFile = new BufferedReader(new FileReader(filename));
		}
		catch(IOException e)
		{
			System.err.println("ERROR: Couldn't open file " + filename);
			System.exit(-1);
		}
		
		// Look for and parse object entries until the end of the file
		try
		{
			String line;
			while((line = inFile.readLine()) != null)
			{
				String tokens[] = line.toUpperCase().split("&");
				
				// Each line must contain two entries
				if(tokens.length != 2)
				{
					System.err.println("ERROR: Objects input file contains malformed data");
					System.exit(-1);
				}
				
				// Find the associated type in the type list
				int indexInList;
				if((indexInList = typeList.indexOf(new VHDLType(tokens[1], false))) == -1)
				{
					System.err.println("ERROR: Can't find the object type " + tokens[1] + " in the type list");
					System.exit(-1);
				}
				
				// Create a new object and add it to the list
				objectList.add(new VHDLObject(tokens[0], typeList.get(indexInList)));
			}
		}
		catch(IOException e)
		{
			System.err.println("ERROR: Couldn't read from file " + filename);
			System.exit(-1);
		}
		
		// Close the file
		try
		{
			inFile.close();
		}
		catch(IOException e)
		{
			System.err.println("ERROR: Couldn't close file " + filename);
		}
	}

	// Given a filename, parse dataflow pair information from the file, adding them to the pairs list
	private static void parsePairsFromFile(String filename)
	{
		BufferedReader inFile = null;

		try
		{
			inFile = new BufferedReader(new FileReader(filename));
		}
		catch(IOException ioe)
		{
			System.err.println("Problem opening the pairs file: " + filename);
			System.exit(-1);
		}

		// Pairs are in the form target&driver&delay
		try
		{
			String line;

			while((line = inFile.readLine()) != null)
			{
				String parts[] = line.trim().toUpperCase().split("&");

				// Should be exactly three parts with the third part being an integer
				try
				{
					if(parts.length != 3 || Integer.parseInt(parts[2]) < 0)
					{
						System.err.println("Line not formatted correctly: " + line);
						System.exit(-1);
					}
				}
				catch(Exception e)
				{
					System.err.println("Line not formatted correctly: " + line);
					System.exit(-1);
				}

				// Add a new handler based on the current pair
				try
				{
					DataflowPair toAdd = new DataflowPair(parts[0], parts[1], Integer.parseInt(parts[2]));
					
					// Keep track of longest common prefix among pair objects
					if(LCP == null)
					{
						LCP = parts[0];
					}
					else
					{
						LCP = longestCommonPrefix(LCP, parts[0]);
					}
					
					if(!toAdd.driverIsConstant())
					{
						LCP = longestCommonPrefix(LCP, parts[1]);
					}
					
					pairs.add(toAdd);
				}
				catch(Exception e)
				{
					System.err.println("ERROR: " + e.getMessage());
					System.exit(-1);
				}
			}
		}
		catch(IOException ioe)
		{
			System.err.println("Problem reading from the pairs file: " + filename);
			System.exit(-1);
		}
	}
	
	// Find and return the longest common prefix between the two passed strings
	private static String longestCommonPrefix(String a, String b)
	{
		for(int index = 0; index < a.length() && index < b.length(); ++index)
		{
			if(a.charAt(index) != b.charAt(index))
			{
				return a.substring(0, index);
			}
		}
		
		return a.length() < b.length() ? a : b;
	}
	
	// Find and return the longest common prefix between the two passed strings, ignores case
	private static String longestCommonPrefixIgnoreCase(String a, String b)
	{
		return longestCommonPrefix(a.toLowerCase(), b.toLowerCase());
	}
	
	// Remove pairs that don't have an associated object in the object list
	private static void removeOrphanPairs()
	{
		for(int pair = pairs.size() - 1; pair >= 0; --pair)
		{
			DataflowPair currentPair = pairs.get(pair);
			
			//Make sure target term has an associated object, if not, remove the pair
			if(!objectList.contains(new VHDLObject(getBaseNameOf(currentPair.getTargetTerm()), null)))
			{
				System.err.println("Removing pair due to missing object: " + currentPair);
				pairs.remove(pair);
				continue;
			}
			
			//If driver isn't a constant, make sure it has an associated object, if not, remove the pair
			if(!currentPair.driverIsConstant() && !objectList.contains(new VHDLObject(getBaseNameOf(currentPair.getDriverTerm()), null)))
			{
				System.err.println("Removing pair due to missing object: " + currentPair);
				pairs.remove(pair);
			}
		}
	}
	
	// Renames all non-constant terms in the pairs list to remove common prefix
	private static void trimCommonPairPrefix()
	{
		// Trim common prefix from pairs
		for(int index = 0; index < pairs.size(); ++index)
		{
			DataflowPair toRep = pairs.get(index);
			int prefixLength = LCP.length();
			
			try
			{
				if(!toRep.driverIsConstant())
				{
					pairs.set(index, new DataflowPair(toRep.getTargetTerm().substring(prefixLength), toRep.getDriverTerm().substring(prefixLength), toRep.delay()));
				}
				else
				{
					pairs.set(index, new DataflowPair(toRep.getTargetTerm().substring(prefixLength), toRep.getDriverTerm(), toRep.delay()));
				}
			}
			catch(Exception e)
			{
				;
			}
		}
	}
	
	// Check for name conflicts if path prefix to variables is removed
	private static void reportNameConflicts()
	{
		HashMap<String, String> names = new HashMap<String, String>();  // Hashmap detects duplicates

		for(int pair = 0; pair < pairs.size(); ++pair)
		{
			DataflowPair currentPair = pairs.get(pair);

			// Check the driver
			if(currentPair.driverIsVariable())
			{
				String oldVal;
				if((oldVal = names.put(nameNoPathPrefix(currentPair.getDriverTerm()), currentPair.getDriverTerm())) != null)
				{
					if(!oldVal.equals(currentPair.getDriverTerm()))
					{
						System.err.println("ERROR: Found a name collision among variables, refer to: " + currentPair);
						System.exit(-1);
					}
				}
			}
			else
			{
				names.put(currentPair.getDriverTerm(), currentPair.getDriverTerm());
			}

			// Check the target
			if(currentPair.targetIsVariable())
			{
				String oldVal;
				if((oldVal = names.put(nameNoPathPrefix(currentPair.getTargetTerm()), currentPair.getTargetTerm())) != null)
				{
					if(!oldVal.equals(currentPair.getTargetTerm()))
					{
						System.err.println("ERROR: Found a name collision among variables, refer to: " + currentPair);
						System.exit(-1);
					}
				}
			}
			else
			{
				names.put(currentPair.getTargetTerm(), currentPair.getTargetTerm());
			}
		}
	}
	
	// Removes spaces from pair member names, also transforms periods, open parens, and close parens into underscores
	private static String reformatName(String name)
	{
		String toRet[] = name.replace('.', '_').replace('(', ' ').replace(')', ' ').split(" ");
		
		for(int x = 1; x < toRet.length; ++x)
		{
			toRet[0] += toRet[x];
		}

		return toRet[0];
	}

	// Print out signals that are needed for DCE infrastructure
	private static void printDCESignals()
	{
		System.out.println("-- Signals used for tracking if a handler fired and which one");
		System.out.println("signal dfp_trap_vector : std_logic_vector(" + (pairs.size() - 1) + " downto 0);");
		System.out.println("signal dfp_trap_mem : std_logic_vector(dfp_trap_vector'left downto dfp_trap_vector'right);");
		System.out.println("signal or_reduce_1 : std_logic;");
		System.out.println("signal dfp_delay_start : integer range 0 to 15;");
		System.out.println("signal handlerTrap : std_ulogic;");
		System.out.println("");
	}
	
	// Create shadow signals to replace references to variables in the pairs
	private static void printShadowSignals()
	{
		System.out.println("-- Signals that serve as shadow signals for variables used in the pairs");
		HashMap<String, String> shadowSigs = new HashMap<String, String>();  // Hashmap prevents duplicates
		
		// Find variables, which require shadow signals
		for(int pair = 0; pair < pairs.size(); ++pair)
		{
			DataflowPair currentPair = pairs.get(pair);

			// If the driver is a variable, remove path prefix and update pair
			if(currentPair.driverIsVariable())
			{
				try
				{
					pairs.set(pair, new DataflowPair(currentPair.getTargetTerm(), reformatName(nameNoPathPrefix(currentPair.getDriverTerm()) + "_shadow"), currentPair.delay()));
					shadowSigs.put(reformatName(nameNoPathPrefix(currentPair.getDriverTerm()) + "_shadow"), currentPair.getDriverTerm());
					currentPair = pairs.get(pair);
				}
				catch(Exception e)
				{
					System.err.println("ERROR: Can't create modified dataflow pair from " + currentPair);
					System.exit(-1);
				}
			}

			// If the target is a variable, remove path prefix and update pair
			if(currentPair.targetIsVariable())
			{
				try
				{
					pairs.set(pair, new DataflowPair(reformatName(nameNoPathPrefix(currentPair.getTargetTerm()) + "_shadow"), currentPair.getDriverTerm(), currentPair.delay()));
					shadowSigs.put(reformatName(nameNoPathPrefix(currentPair.getTargetTerm()) + "_shadow"), currentPair.getTargetTerm());
				}
				catch(Exception e)
				{
					System.err.println("ERROR: Can't create modified dataflow pair from " + currentPair);
					System.exit(-1);
				}
			}
		}
		
		// Declare each shadow signal
		for(Iterator<String> sig = shadowSigs.keySet().iterator(); sig.hasNext(); )
		{
			String temp = sig.next();
			VHDLType finalType = getFinalTypeOf(shadowSigs.get(temp));
			
			// Handle printing of anonymous types
			if(finalType.getName().startsWith("ANONYMOUSNAME"))
			{
				System.out.println("signal " + temp + " : " + reWriteAnonymousTypes(finalType) + ";");
			}
			// Else print explicitly declared type names
			else if(finalType.isArray() && shadowSigs.get(temp).trim().endsWith(")"))
			{
				if(finalType.getLeftBound() >= finalType.getRightBound())
				{
					System.out.println("signal " + temp + " : " + finalType.getName() + "(" + finalType.getLeftBound() + " downto " + finalType.getRightBound() + ");");
				}
				else
				{
					System.out.println("signal " + temp + " : " + finalType.getName() + "(" + finalType.getLeftBound() + " to " + finalType.getRightBound() + ");");
				}
			}
			else
			{
				System.out.println("signal " + temp + " : " + finalType.getName() + ";");
			}
			objectList.add(new VHDLObject(temp, finalType));
		}

		// List required assignments that need to be placed in the processes with the variables
		shadowAssigns.add("\n-- Assignments to be moved with variables");
		ArrayList<String> processNames = new ArrayList<String>();
		
		// Find all process names that will need shadow assignments
		for(Iterator<String> sig = shadowSigs.keySet().iterator(); sig.hasNext(); )
		{
			String current = sig.next();
			String varName = shadowSigs.get(current);
			String processName = varName.substring(0, varName.length() - nameNoPathPrefix(varName).length());
			
			// Add any new process names to the list
			if(!processNames.contains(processName))
			{
				processNames.add(processName);
			}
		}
		
		// List the required shadow assignments, grouped by process
		for(int process = 0; process < processNames.size(); ++process)
		{
			shadowAssigns.add("\n-- These assignments must be moved to process " + processNames.get(process));
			
			for(Iterator<String> sig = shadowSigs.keySet().iterator(); sig.hasNext(); )
			{
				String current = sig.next();
			
				shadowAssigns.add(current + " <= " + nameNoPathPrefix(shadowSigs.get(current)) + ";");
			}
		}
	}
	
	// Print the assignments to shadow signals and the process they need to go in
	// Lines create in printShadowSigs and stored in global variable
	private static void printShadowAssignments()
	{
		for(Iterator<String> line = shadowAssigns.iterator(); line.hasNext(); )
		{
			System.out.println(line.next());
		}
	}

	// For each pair with a delay, declare signals/variables to hold intermediate values
	private static void printObjectDecls()
	{
		// Use hash to remove duplicates
		HashMap<String, String> toPrint = new HashMap<String, String>();
		
		System.out.println("\n-- Intermediate value holding signal declarations");
		// Declare signals to hold the intermediate values
		for(int pair = 0; pair < pairs.size(); ++pair)
		{
			DataflowPair currentPair = pairs.get(pair);

			// Skip pairs with 0 delay
			if(currentPair.delay() == 0)
			{
				continue;
			}

			// Should be no variables used in pairs at this point
			if(currentPair.driverIsVariable() || currentPair.targetIsVariable())
			{
				System.err.println("ERROR: Variable in pair " + currentPair.toString() + " detected, when there should be none");
				System.exit(-1);
			}

			// Intermediate values are signals since variables don't have state
			// Need a new signal for each cycle of delay
			for(int delay = 1; delay <= currentPair.delay(); ++delay)
			{
				VHDLType finalType = getFinalTypeOf(currentPair.getDriverTerm());
				
				// Handle printing of anonymous types
				if(finalType.getName().startsWith("ANONYMOUSNAME"))
				{
					toPrint.put("signal " + reformatName(currentPair.getDriverTerm() + "_intermed_" + delay) + " : " + reWriteAnonymousTypes(finalType) + ";", "");
				}
				// Else print explicitly declared type names
				else if(finalType.isArray())
				{
					if(finalType.getLeftBound() >= finalType.getRightBound())
					{
						toPrint.put("signal " + reformatName(currentPair.getDriverTerm() + "_intermed_" + delay) + " : " + finalType.getName() + "(" + finalType.getLeftBound() + " downto " + finalType.getRightBound() + ");", "");
					}
					else
					{
						toPrint.put("signal " + reformatName(currentPair.getDriverTerm() + "_intermed_" + delay) + " : " + finalType.getName() + "(" + finalType.getLeftBound() + " to " + finalType.getRightBound() + ");", "");
					}
				}
				else
				{
					toPrint.put("signal " + reformatName(currentPair.getDriverTerm() + "_intermed_" + delay) + " : " + finalType.getName() + ";", "");
				}
			}
		}
		
		// Print unique lines
		for(Iterator<String> line = toPrint.keySet().iterator(); line.hasNext(); )
		{
			System.out.println(line.next());
		}
	}

	// Prints the process used to create delays required by the handler
	private static void printDelayProcess()
	{
		System.out.println("\ndfp_delay : process(clk) begin");
		System.out.println("\tif(clk'event and clk = '1')then");

		for(int pair = 0; pair < pairs.size(); ++pair)
		{
			DataflowPair currentPair = pairs.get(pair);

			// Skip pairs with 0 delay
			if(currentPair.delay() == 0)
			{
				continue;
			}

			// Independent of the driver type, intermediate values are signals since variables don't have state
			// Need a new signal for each cycle of delay
			for(int delay = 1; delay <= currentPair.delay(); ++delay)
			{
				
				// If this is first delay stage, then we need to drive with original signal
				if(delay == 1)
				{
					System.out.println("\t\t" + reformatName(currentPair.getDriverTerm() + "_intermed_" + delay) + " <= " + currentPair.getDriverTerm() + ";");
				}
				// otherwise, drive with previous delay signal
				else
				{
					System.out.println("\t\t" + reformatName(currentPair.getDriverTerm() + "_intermed_" + delay) + " <= " + reformatName(currentPair.getDriverTerm() + "_intermed_" + (delay-1)) + ";");
				}
			}
		}

		System.out.println("\tend if;");
		System.out.println("end process;");
	}

	// For each pair print a comparison that sets a bit if there is a mismatch between driver and target term
	private static void printHandlers()
	{
		// Padding
		System.out.println();
		
		for(int pair = 0; pair < pairs.size(); ++pair)
		{
			DataflowPair currentPair = pairs.get(pair);
			String compareString;

			// Form a string that represents the comparison required to check the validity of the current pair
			if(currentPair.delay() == 0)
			{
				// Surround constant drivers by ' ' or " "
				if(currentPair.driverIsConstant())
				{
					// Check for existing ' or "
					if(currentPair.getDriverTerm().charAt(0) == '\'' || currentPair.getDriverTerm().charAt(0) == '"')
					{
						compareString = "(" + currentPair.getTargetTerm() + " /= " + currentPair.getDriverTerm() + ")";
					}
					// Default one digit constants to being surrounded by ' '
					else if(currentPair.getDriverTerm().length() == 1)
					{
						compareString = "(" + currentPair.getTargetTerm() + " /= '" + currentPair.getDriverTerm() + "')";
					}
					else
					{
						compareString = "(" + currentPair.getTargetTerm() + " /= \"" + currentPair.getDriverTerm() + "\")";
					}
				}
				else
				{
					compareString = "(" + currentPair.getTargetTerm() + " /= " + currentPair.getDriverTerm() + ")";
				}
			}
			else
			{
				compareString = "(" + currentPair.getTargetTerm() + " /= " + reformatName(currentPair.getDriverTerm() + "_intermed_" + currentPair.delay()) + ")";
			}
			
			// Write the check as a concurrent assignment
			System.out.println("dfp_trap_vector(" + pair + ") <= '1' when " + compareString + " else '0';");
		}
	}
	
	// Generates and prints a tree of OR statements that reduces the results of all dataflow checks to a single signal
	private static void printOrReduce()
	{
		System.out.println("\ndfp_or_reduce : process(dfp_trap_vector)");

		// Declare the variables needed to store the intermediate reduction or results
		int numRemaining = pairs.size();
		while(numRemaining > 2)
		{
			numRemaining = (int)Math.ceil(numRemaining/2.0);

			System.out.println("\tvariable or_reduce_" + numRemaining + " : std_logic_vector(" + (numRemaining-1) + " downto 0);");
		}

		System.out.println("begin");

		// Assignments for reduction or
		numRemaining = pairs.size();
		while(numRemaining > 2)
		{
			int prevNumRemaining = numRemaining;
			int splitPoint = prevNumRemaining/2;
			numRemaining = (int)Math.ceil(numRemaining/2.0);

			// Check for first level
			if(numRemaining >= pairs.size()/2)
			{
				// Check if one of the terms needs a zero bit as padding
				if(prevNumRemaining%2 == 1)
				{
					System.out.println("\tor_reduce_" + numRemaining + " := dfp_trap_vector(" + (prevNumRemaining-1) + " downto " + splitPoint + ") OR (\"0\" & dfp_trap_vector(" + (splitPoint-1) + " downto 0));");
				}
				else
				{
					System.out.println("\tor_reduce_" + numRemaining + " := dfp_trap_vector(" + (prevNumRemaining-1) + " downto " + splitPoint + ") OR dfp_trap_vector(" + (splitPoint-1) + " downto 0);");
				}
			}
			else
			{
				// Check if one of the terms needs a zero bit as padding
				if(prevNumRemaining%2 == 1)
				{
					System.out.println("\tor_reduce_" + numRemaining + " := or_reduce_" + prevNumRemaining + "(" + (prevNumRemaining-1) + " downto " + splitPoint + ") OR (\"0\" & or_reduce_" + prevNumRemaining + "(" + (splitPoint-1) + " downto 0));");
				}
				else
				{
					System.out.println("\tor_reduce_" + numRemaining + " := or_reduce_" + prevNumRemaining + "(" + (prevNumRemaining-1) + " downto " + splitPoint + ") OR or_reduce_" + prevNumRemaining + "(" + (splitPoint-1) + " downto 0);");
				}
			}
		}

		System.out.println("\tor_reduce_1 <= or_reduce_2(0) OR or_reduce_2(1);");
		System.out.println("end process;\n");
	}

	// Prints misc DCE support code
	private static void printSupportCode()
	{
		System.out.println("trap_enable_delay : process(clk)");
		System.out.println("begin");
		System.out.println("  if(rising_edge(clk))then");
		System.out.println("    if(rstn = '0')then");
		System.out.println("      dfp_delay_start <= 15;");
		System.out.println("    elsif(dfp_delay_start /= 0)then");
		System.out.println("      dfp_delay_start <= dfp_delay_start - 1;");
		System.out.println("    end if;");
		System.out.println("  end if;");
		System.out.println("end process;\n");
		System.out.println("trap_mem : process(clk)");
		System.out.println("begin");
		System.out.println("  if(rising_edge(clk))then");
		System.out.println("    if(rstn = '0')then");
		System.out.println("      dfp_trap_mem <= (others => '0');");
		System.out.println("    elsif(dfp_delay_start = 0)then");
		System.out.println("      dfp_trap_mem <= dfp_trap_mem OR dfp_trap_vector;");
		System.out.println("    end if;");
		System.out.println("  end if;");
		System.out.println("end process;\n");
		System.out.println("handlerTrap <= or_reduce_1 when (dfp_delay_start = 0) else '0';");
	}
	
	// Given an object name, possibly with a Modelsim path prefix, return the object name without a path prefix
	private static String nameNoPathPrefix(String pathAndName)
	{
		int index;

		if((index = pathAndName.lastIndexOf('/')) < 0)
		{
			return pathAndName;
		}

		return pathAndName.substring(index + 1);
	}

	// Given a object name and possibly a Modelsim path prefix, this method returns the base name of the object with the path prefix
	private static String getBaseNameOf(String name)
	{
		for(int index = 0; index < name.length(); ++index)
		{
			char currentChar = name.charAt(index);

			// Break on spaces, subfields, slices, and attributes dereferences
			if(currentChar == '.' || currentChar == '(' || currentChar == ' ' || currentChar == '\'')
			{
				return name.substring(0, index);
			}
		}

		return name;
	}

	// Given an object name, possibly with a Modelsim path prefix, this method returns a reference to the type associated with the base object
	private static VHDLType getBaseTypeOf(String name)
	{
		String baseName = getBaseNameOf(name);
		int index;
		
		if((index = objectList.indexOf(new VHDLObject(baseName, null))) < 0)
		{
			System.err.println("ERROR: Can't find the object " + baseName + " derived from " + name + " in the object list.");
		}

		return objectList.get(index).getType();
	}

	// Given an object name, possibly with a Modelsim path prefix, this method returns the type resulting from the string of constraints
	private static VHDLType getFinalTypeOf(String name)
	{
		// Can't handle attribute dereferences
		if(name.indexOf('\'') >= 0)
		{
			System.err.println("ERROR: Can't process constraints that contain attribute dereferences.");
			System.exit(-1);
		}

		// Get the staring type and apply constraints to it
		VHDLType baseType = getBaseTypeOf(name);
		String baseName = getBaseNameOf(name);
		
		if(DEBUG)
		{
			System.err.println("Getting final type of " + name + " with base type " + baseType.getName());
		}
		
		// Create tokens by breaking at . and (
		// Ignore spaces, except between slices
		String token = "";
		boolean inSlice = false;
		VHDLType currentType = baseType;
		for(int index = baseName.length(); index < name.length(); ++index)
		{
			char currentChar = name.charAt(index);
			
			// For each token, use current type to find type associated with the token
			if(currentChar == ' ' && !inSlice)
			{
				continue;
			}
			
			token += currentChar;
			
			if((currentChar == '.' || currentChar == '(') && token.length() > 1)
			{
				inSlice = false;
				token = token.substring(0, token.length() - 1).trim();
				if(DEBUG)
				{
					System.err.println("Changing from type " + currentType.getName() + " with token " + token);
					System.err.flush();
				}
				currentType = getResultingType(currentType, token);
				token = ""  + currentChar;
				if(DEBUG)
				{
					System.err.println("to type " + currentType.getName());
				}
				if(currentChar == '(')
				{
					inSlice = true;
				}
			}
		}
		
		// Check for lack of constraints
		if(token.trim().length() == 0)
		{
			return currentType;
		}
		
		// Get final type from last token
		if(DEBUG)
		{
			System.err.println("Changing from type " + currentType.getName() + " with token " + token.trim());
			System.err.flush();
		}
		return getResultingType(currentType, token.trim());
	}
	
	// Given a type and constraint, this method returns the resulting type
	// Returns null if error
	private static VHDLType getResultingType(VHDLType type, String constraint)
	{
		// Handle record type
		if(type.isCompositeType() && !type.isArray())
		{
			// Only can have subfield constraints with records
			if(constraint.charAt(0) != '.')
			{
				System.err.println("ERROR: Can't apply constraint " + constraint + " to base type " + type.getName() + ": not a subfield reference");
				System.exit(-1);
			}
			
			String subfield = constraint.substring(1);
			int index;
			if((index = type.indexOfField(subfield)) < 0)
			{
				System.err.println("ERROR: Can't apply constraint " + constraint + " to base type " + type.getName() + ": incorrect subfield reference");
				System.exit(-1);
			}
			return type.getField(index).getType();
		}
		else if(constraint.charAt(0) != '(')
		{
			System.err.println("ERROR: Can't apply constraint " + constraint + " to base type " + type.getName() + ": invalid constraint");
			System.exit(-1);
		}
		
		// Handle array types
		
		// Parse range or index constraint
		int left = parseLeftConstraint(constraint);
		int right = parseRightConstraint(constraint);
		
		// If we have an index constraint, we return the element type
		if(left == right)
		{
			return type.getArrayElementType();
		}
		
		// Otherwise, we constrain the range of the current type and return it
		VHDLType toRet = new VHDLType(type);
		toRet.setArrayBounds(left, right);
		
		return toRet;
	}
	
	// Parse and return the left bound of a range constraint or the index of an index constraint
	private static int parseLeftConstraint(String constraint)
	{
		// Replace opening and closing parens and remove spaces
		constraint = constraint.replace('(', ' ').replace(')', ' ').trim();
		
		for(int index = 0; index < constraint.length(); ++index)
		{
			char current = constraint.charAt(index);
			
			if(current == 'd' || current == 'D' || current == 't' || current == 'T' || current == ')' || current == ' ')
			{
				return Integer.parseInt(constraint.substring(0, index));
			}
		}
		
		return Integer.parseInt(constraint);
	}
	
	// Parse and return the right bound of a range constraint or the index of an index constraint
	private static int parseRightConstraint(String constraint)
	{
		// Replace opening and closing parens and remove spaces
		constraint = constraint.replace('(', ' ').replace(')', ' ').trim();
		
		for(int index = constraint.length() - 1; index > 0; --index)
		{
			char current = constraint.charAt(index);
			
			if(current == 'o' || current == 'O' || current == '(' || current == ' ')
			{
				return Integer.parseInt(constraint.substring(index + 1, constraint.length()));
			}
		}
		
		return Integer.parseInt(constraint);
	}
}
