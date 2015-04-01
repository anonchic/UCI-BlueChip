import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashSet;

class VHDLObject
{
	String name;
	VHDLType type;
	ArrayList<VHDLObject> drivers = new ArrayList<VHDLObject>();
	ArrayList<VHDLObject> driverChain = new ArrayList<VHDLObject>();
	
	public VHDLObject(String name, VHDLType type)
	{
		this.name = name;
		this.type = type;
	}
	
	public int numDrivers()
	{
		return drivers.size();
	}
	
	public VHDLObject getDriver(int index)
	{
		return drivers.get(index);
	}
	
	public void addDriver(VHDLObject driver)
	{
		if(!drivers.contains(driver))
		{
			drivers.add(driver);
		}
	}
	
	public boolean equals(Object o)
	{
		if(o.getClass() != VHDLObject.class)
		{
			return false;
		}
		
		return name.equalsIgnoreCase(((VHDLObject)o).name);
	}
}

class DataflowPair
{
	String left = "";
	String right = "";
	boolean rightIsConstant = false;
	int time = 0;
	
	public DataflowPair(String a, String b, String stages, boolean rightConstant)
	{
		time = Integer.parseInt(stages);
		rightIsConstant = rightConstant;
		
		// Handle slice bounds modelsim won't work with
		int index;
		// Check for left
		left = a;
		
		// Check for right
		right = b;
	}
}

public class DOTHighlighter
{
	private static HashSet<VHDLObject> driverChain = new HashSet<VHDLObject>();
	
	public static void main(String args[])
	{	
		ArrayList<DataflowPair> pairsListProc = new ArrayList<DataflowPair>();
		HashSet<VHDLObject> nameList = new HashSet<VHDLObject>();
		ArrayList<VHDLObject> objectList = new ArrayList<VHDLObject>();
		
		// Make sure the correct arguments are passed by user
		if(args.length != 2)
		{
			System.out.println("java DOTHighlighter pairsProc.ext drivers.ext");
			return;
		}
		
		// Retrieve the dataflow pairs from the pairs file
		System.out.println("Parsing dataflow pairs from processed pairs file...");
		getDataflowPairs(pairsListProc, args[0]);
		System.out.println("There are a total of " + pairsListProc.size() + " dataflow pairs in " + args[0]);
		
		// Retrieve low-level object information from the drivers file
		System.out.println("Parsing driver information from input file...");
		getDriverInformation(objectList, args[1]);
		System.out.println("There are a total of " + objectList.size() + " objects in " + args[1]);
		
		// Get a list of unique objects in the processed pairs file so we know who to build driver chains for
		System.out.println("Determining unique objects in the processed pairs list...");
		uniqueNamesInList(pairsListProc, nameList, objectList);
		System.out.println("There are " + nameList.size() + " unique objects");
		
		// For each unique object in the name list, generate a driver chain
		System.out.println("Building driver chains for unique objects...");
		generatDriverChains(nameList);
		
		// For each pair in the proc. pair file, do a driver chain set difference to determine which nodes require highlighting
		System.out.println("Highlighting intermediate nodes...");
		graphHighlighting(pairsListProc, objectList, nameList);
	}
	
	// Parse the given file creating dataflow pairs and adding them to the pairs list
	// File is in the format a&b&delay
	private static void getDataflowPairs(ArrayList<DataflowPair> pairsList, String filename)
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
			System.exit(0);
		}
		
		// Look for and parse pairs until the end of the file
		try
		{
			String line;
			while((line = inFile.readLine()) != null)
			{
				String tokens[] = line.split("&");
				
				if(tokens.length != 3)
				{
					System.err.println("ERROR: Pair input file " + filename + " is malformed");
					System.exit(0);
				}
				
				// Remove slicing information
				//tokens[0] = tokens[0].split(" ")[0];
				//tokens[1] = tokens[1].split(" ")[0];
				
				// Can't handle conv_integer function so skip pairs involving it
				if(line.contains("conv_integer"))
				{
					continue;
				}
				
				if(!isNumberOrBoolean(tokens[1]))
				{
					pairsList.add(new DataflowPair(tokens[0], tokens[1], tokens[2], false));
				}
				else
				{
					pairsList.add(new DataflowPair(tokens[0], tokens[1], tokens[2], true));
				}
			}
		}
		catch(IOException e)
		{
			System.err.println("ERROR: Couldn't read from file " + filename);
			System.exit(0);
		}
		
		// Close the file
		try
		{
			inFile.close();
		}
		catch(IOException e)
		{
			System.err.println("ERROR: Couldn't close file " + filename);
			System.exit(0);
		}
	}
	
	// Given a list of dataflow pairs and a target list, this methods gets all unqiue names in the pairs
	private static void uniqueNamesInList(ArrayList<DataflowPair> pairsList, HashSet<VHDLObject> nameList, ArrayList<VHDLObject> objectList)
	{
		for(Iterator<DataflowPair> pair = pairsList.iterator(); pair.hasNext(); )
		{
			DataflowPair current = pair.next();
			
			int indexInList;
			if((indexInList = objectList.indexOf(new VHDLObject(current.left, null))) == -1)
			{
				System.err.println("ERROR: Pair member " + current.left + " not found in object list");
				System.exit(0);
			}
			nameList.add(objectList.get(indexInList));
			
			// Don't add numbers or boolean Values
			if(!current.rightIsConstant)
			{
				if((indexInList = objectList.indexOf(new VHDLObject(current.right, null))) == -1)
				{
					System.err.println("ERROR: Pair member " + current.right + " not found in object list");
					System.exit(0);
				}
				nameList.add(objectList.get(indexInList));
			}
		}
	}

	// Parse the given file creating a list of object names
	// File is in the format name&type
	private static void getDriverInformation(ArrayList<VHDLObject> objectList, String filename)
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
			System.exit(0);
		}
		
		// Look for and parse types until the end of the file
		try
		{
			String line;
			while((line = inFile.readLine()) != null)
			{
				// Line format = driver&target
				String tokens[] = line.split("&");
				
				// Each line must contain two entries
				if(tokens.length != 2)
				{
					System.err.println("ERROR: Driver input file " + filename + " is malformed");
					System.exit(0);
				}
				
				// Try to find the source term in the list
				int indexOfSource;
				if((indexOfSource = objectList.indexOf(new VHDLObject(tokens[0], null))) == -1)
				{
					// Not in the list so add it
					objectList.add(new VHDLObject(tokens[0], null));
					indexOfSource = objectList.size() - 1;
				}
				
				// Try to find the target term in the list
				int indexOfTarget;
				if((indexOfTarget = objectList.indexOf(new VHDLObject(tokens[1], null))) == -1)
				{
					// Not in the list so add it
					objectList.add(new VHDLObject(tokens[1], null));
					indexOfTarget = objectList.size() - 1;
				}
				
				// Add the source as a driver of the target
				objectList.get(indexOfTarget).addDriver(objectList.get(indexOfSource));
			}
		}
		catch(IOException e)
		{
			System.err.println("ERROR: Couldn't read from file " + filename);
			System.exit(0);
		}
		
		// Close the file
		try
		{
			inFile.close();
		}
		catch(IOException e)
		{
			System.err.println("ERROR: Couldn't close file " + filename);
			System.exit(0);
		}
	}
	
	// Print the dataflow pairs in the pairs list
	private static void printDataflowPairs(ArrayList<DataflowPair> pairsList)
	{
		for(int pair = 0; pair < pairsList.size(); ++pair)
		{
			System.out.println(pairsList.get(pair).left + "&" + pairsList.get(pair).right + "&" + pairsList.get(pair).time);
		}
	}
	
	// For each unique object in the name list, generate a driver chain
	private static void generatDriverChains(HashSet<VHDLObject> nameList)
	{
		// Build a driver chain for each unique object in the name list
		for(Iterator<VHDLObject> objects = nameList.iterator(); objects.hasNext(); )
		{
			VHDLObject current = objects.next();
			
			// Build a list of all possible drivers of this object in the driverChain list
			for(int driver = 0; driver < current.numDrivers(); ++driver)
			{
				addDriversToChain(current.getDriver(driver));
			}
			
			// Copy the driver chain into the object's driver chain
			for(Iterator<VHDLObject> driver = driverChain.iterator(); driver.hasNext(); )
			{
				VHDLObject currentDriver = driver.next();
				
				// Remove self drivers
				if(!currentDriver.equals(current))
				{
					current.driverChain.add(currentDriver);
				}
			}
			
			// Clear the chain for the next object
			driverChain.clear();
		}
	}
	
	// Recursive function that collects target,driver pairs
	// Assumes there are no no object drivers (i.e. no constants)
	private static void addDriversToChain(VHDLObject driver)
	{ 
		// Make sure the driver isn't in the list to avoid cycles
		if(driverChain.contains(driver))
		{
			return;
		}
		driverChain.add(driver);
		
		for(int i = 0; i < driver.numDrivers(); ++i)
		{
			addDriversToChain(driver.getDriver(i));
		}
	}
	
	// For each pair in the processed pairs file, highlight the nodes in the original pair file that are affected
	// All drivers of the left are highlighted expect those that are drivers of the right
	private static void graphHighlighting(ArrayList<DataflowPair> pairsListProc, ArrayList<VHDLObject> objectList, HashSet<VHDLObject> nameList)
	{
		// Use a list to prevent printing of duplicates
		HashSet<String> linesToPrint = new HashSet<String>();
		
		for(Iterator<DataflowPair> pair = pairsListProc.iterator(); pair.hasNext(); )
		{
			DataflowPair currentPair = pair.next();
			
			VHDLObject left = objectList.get(objectList.indexOf(new VHDLObject(currentPair.left, null)));
			
			// If the right term is a constant, we need to highlight all of the left term's drivers
			if(currentPair.rightIsConstant)
			{
				// Print out the drivers that require inspection
				for(Iterator<VHDLObject> driver = left.driverChain.iterator(); driver.hasNext(); )
				{
					VHDLObject currentDriver = driver.next();
					
					linesToPrint.add("\"" + currentDriver.name + "\" [style=dotted]");
				}
				continue;
			}
			
			VHDLObject right = objectList.get(objectList.indexOf(new VHDLObject(currentPair.right, null)));
			
			// Print out the drivers that require inspection
			for(Iterator<VHDLObject> driver = left.driverChain.iterator(); driver.hasNext(); )
			{
				VHDLObject currentDriver = driver.next();
				
				// Check for nodes that are intermediate
				if(!right.driverChain.contains(currentDriver))
				{
					linesToPrint.add("\"" + currentDriver.name + "\" [style=dotted]");
				}
			}
		}
		
		// Print the unique strings for both the names and the intermediate nodes
		for(Iterator<VHDLObject> name = nameList.iterator(); name.hasNext(); )
		{
			System.out.println("\t" + "\"" + name.next().name + "\" [style=filled]");
		}
		
		for(Iterator<String> line = linesToPrint.iterator(); line.hasNext(); )
		{
			System.out.println("\t" + line.next());
		}
	}
	  
	// Returns true if the passed string is a number of Boolean value
	private static boolean isNumberOrBoolean(String value)
	{
		if(java.util.regex.Pattern.matches("[0-9a-fA-F]+", value))
		{
			return true;
		}

	  	if(java.util.regex.Pattern.matches("\"[01]+\"", value))
	  	{
			return true;
	  	}
	  
	  	if(value.equalsIgnoreCase("TRUE") || value.equalsIgnoreCase("FALSE"))
	  	{
			return true;
		}
		
		return false;
	}
}