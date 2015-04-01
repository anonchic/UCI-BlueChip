import java.io.Serializable;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Tuple
{
	String path;
	VHDLType type;
	public Tuple(String name, VHDLType type)
	{
		this.path = name;
		this.type = type;
	}
}

abstract class VHDLObject implements Serializable
{
	private static final long serialVersionUID = 1L;
	private static final boolean DEBUG = false;
	private static final boolean DEBUG_CONSTRAINTS = true;
	private static final String RANGE_DOWNTO = "\\([ \\t]*[0-9]+[ \\t]+DOWNTO[ \\t]+[0-9]+[ \\t]*\\)";
	private static final String RANGE_TO = "\\([ \\t]*[0-9]+[ \\t]+TO[ \\t]+[0-9]+[ \\t]*\\)";
	private static final String INDEX = "\\([ \\t]*[0-9]+[ \\t]*\\)";
	private static final String SUBFIELD = "\\.[a-zA-Z][a-zA-Z_0-9]*";
	private static final Pattern RANGE_DOWNTO_PATTERN = Pattern.compile(RANGE_DOWNTO);
	private static final Pattern RANGE_TO_PATTERN = Pattern.compile(RANGE_TO);
	private static final Pattern INDEX_PATTERN = Pattern.compile(INDEX);
	private static final Pattern SUBFIELD_PATTERN = Pattern.compile(SUBFIELD);
	
	private String name;
	private boolean isRegister;
	java.util.ArrayList<String> drivers = new java.util.ArrayList<String>();
	private VHDLType type;
	
	public VHDLObject(String name, VHDLType type)
	{
		this.name = name;
		this.type = type;
		isRegister = false;
	}
	public VHDLObject(String name, VHDLType type, boolean infersStorage)
	{
		this.name = name;
		this.type = type;
		isRegister = infersStorage;
	}
	
	abstract public boolean isSignal();
	abstract public boolean isVariable();
	
	// Getters for static signal properties
	public String getName()
	{
		return name;
	}
	// Given a string representing a name, possibly with constraints, the base object name is returned
	public String getBaseName()
	{
		try
		{
			return name.split(" ")[0].trim().split("'")[0].trim().split("\\.")[0].trim().split("\\(")[0].trim();
		}
		catch(Exception e)
		{
			System.err.println("Problem getting base name for " + name);
		}
		return name;
	}
	//Given a string representing a name, possibly with constraints, the object name with possibly subfield references is returned
	public String getNameNoSlicing()
	{
		return name.split("\\(")[0].trim();
	}
	public static String getNameNoSlicing(String name)
	{
		return name.split("\\(")[0].trim();
	}
	// Convenience method that, given a string representing a name, possibly with constraints, the base object name is returned
	public static String getBaseName(String name)
	{
		try
		{
			return name.split(" ")[0].trim().split("'")[0].trim().split("\\.")[0].trim().split("\\(")[0].trim();
		}
		catch(Exception e)
		{
			System.err.println("Problem getting base name for " + name);
		}
		return name;
	}
	public VHDLType getType()
	{
		return type;
	}
	// Utility method that finds the type of the passed object name/path
	public static VHDLType getTypeOf(String path, VHDLType baseType)
	{
		// Get base name associated with base type
		String baseName = getBaseName(path);
		VHDLType currentType = baseType;
		String currentPath = path.substring(baseName.length()).trim();
		
		while(true)
		{
			// Return the type when we have no constraints to parse
			if(currentPath.length() == 0)
			{
				return currentType;
			}
			
			// Can't have constraints on objects of discrete type
			if(currentType.isEnumerationType() || currentType.isIntegerType())
			{
				// Error if trying to constraint objects of this type
				System.err.println("ERROR: Improper use of constraints in: " + path);
				return null;
			}
			
			// The current type's kind tells us what valid constraints to expect next
			if(currentType.isArrayType())
			{
				// Can have index or range constraints here, but bounds/index must match
				if(!RANGE_DOWNTO_PATTERN.matcher(currentPath).lookingAt() && !RANGE_TO_PATTERN.matcher(currentPath).lookingAt() && !INDEX_PATTERN.matcher(currentPath).lookingAt())
				{
					System.err.println("ERROR: Can't find range constraint while getting type of " + path + ": " + currentPath + " using type " + currentType.getName());
					return null;
				}
				
				// Get the bounds/index of this constraint
				String bounds = findFirstRangeConstraint(path);
				
				if(bounds.indexOf('&') == -1)
				{
					int index = Integer.parseInt(bounds);
					
					if(((VHDLArrayType)currentType).isRangeDownto() && (index > ((VHDLArrayType)currentType).getLeftIndex() || index < ((VHDLArrayType)currentType).getRightIndex()))
					{
						System.err.println("ERROR: Index constraint " + index + " is invalid in " + path + " with object of type:\n" + currentType.typeToString());
						return null;
					}
					else if(((VHDLArrayType)currentType).isRangeTo() && (index < ((VHDLArrayType)currentType).getLeftIndex() || index > ((VHDLArrayType)currentType).getRightIndex()))
					{
						System.err.println("ERROR: Index constraint " + index + " is invalid in " + path + " with object of type:\n" + currentType.typeToString());
						return null;
					}
					
					// Update current type with the element type just indexed
					currentType = ((VHDLArrayType)currentType).getMemberType();
				}
				else
				{
					int leftBound = Integer.parseInt(bounds.split("&")[0]);
					int rightBound = Integer.parseInt(bounds.split("&")[1]);
					
					// Create a new anonymous type which isn't tracked anywhere else so we know the bounds of this array slice
					// Set current type to this anon type
					// Compatibility checking is done in constructor	
					currentType = new VHDLArrayType("MORE_CONSTRAINED_ANON_TYPE", ((VHDLArrayType)currentType), leftBound, rightBound);
				}
				
				// Get ready to handle next constraint
				currentPath = currentPath.substring(currentPath.indexOf(')') + 1).trim();
			}
			else if(currentType.isRecordType())
			{
				// Can only have subfield constraints here and subfield must match
				Matcher match = SUBFIELD_PATTERN.matcher(currentPath);
				if(!match.find() || match.start() != 0)
				{
					System.err.println("ERROR: Can't find subfield constraint while getting type of " + path);
					return null;
				}
				
				// Get the requested field name (skip leading .) and ensure it is an actual subfield of the current type
				String fieldName = currentPath.substring(1, match.end());
				VHDLTypeField field;
				if((field = ((VHDLRecordType)currentType).getField(fieldName)) == null)
				{
					System.err.println("ERROR: Subfield constraint asks for invalid field: " + fieldName + " from " + path + " in type " + currentType.getName());
					return null;
				}
				
				// Get ready to handle next constraint
				currentPath = currentPath.substring(match.end()).trim();
				currentType = field.getType();
			}
			else
			{
				System.err.println("ERROR: Type not formed correctly: " + currentType.getName());
				return null;
			}
		}
	}
	public boolean isRegister()
	{
		return isRegister;
	}
	
	// Sets whether this signal infers storage
	public void setRegister(boolean infersStorage)
	{
		isRegister = infersStorage;
	}
	
	// Methods that manipulate and query the driver list for this object
	public int numDrivers()
	{
		return drivers.size();
	}
	public String getDriver(int index)
	{
		return drivers.get(index);
	}
	public boolean hasDriver(String name)
	{
		for(int index = 0; index < drivers.size(); ++index)
		{
			if(drivers.get(index).equals(name))
			{
				return true;
			}
		}
		
		return false;
	}
	public void removeDriver(int index)
	{
		drivers.remove(index);
	}
	public void addDriver(String name)
	{
		if(!hasDriver(name) && !this.name.equals(name))
		{
			drivers.add(name);
		}
	}
	public void addDriver(VHDLObject obj)
	{
		if(!hasDriver(obj.getName()) && !this.name.equals(obj.getName()))
		{
			drivers.add(obj.getName());
		}
	}
	// Given a parent object and a driver from that object, this method properly constrains the driver and adds it to the driver list if not present already
	// Assumes that this object is a strictly more constrained version of the parent object
	// Assumes the driver involves a signal or variable
	public boolean addAndConstrainDriverFromObject(VHDLObject parent, String driver, VHDLType driverBaseType)
	{
		// Get the type of the driver and the parent
		VHDLType driverType = getTypeOf(driver, driverBaseType);
		VHDLType parentType = parent.getType();
		
		String editedDriver = driver;
		String prefix = parent.getName();
		String postfix;
		
		// Handle the case when the parent ends in a range constraint that the child further reduces
		if(!name.startsWith(prefix) || (parentType.isArrayType() && name.substring(prefix.length()).trim().charAt(0) == '('))
		{
			int offsetFromBase = 0;
			int range = 0;
			
			// Ensure that the driver is also an array
			if(driverType == null || !driverType.isArrayType())
			{
				System.err.println(name + ": Can't add constrained version of " + driver + " of base type " + driverBaseType.typeToString() + " with parent " + parent.getName());
				return false;
			}
			
			// Adjust the pre and post fix
			if(!name.startsWith(prefix))
			{
				prefix = prefix.substring(0, prefix.lastIndexOf('(')).trim();
			}
			postfix = name.substring(prefix.length()).trim();
			
			// Get the values of the parent and child constraints
			String chCon = findFirstRangeConstraint(postfix);
			
			if(DEBUG)
			{
				System.out.println("Child: " + name + "\tParent: " + parent.getName() + "\tPostfix: " + postfix + "\tReturned: " + chCon);
			}
			
			// Skip the first range constraint in the postfix
			Matcher match = null;
			if(RANGE_DOWNTO_PATTERN.matcher(postfix).lookingAt())
			{	
				match = RANGE_DOWNTO_PATTERN.matcher(postfix);
			}
			else if(RANGE_TO_PATTERN.matcher(postfix).lookingAt())
			{	
				match = RANGE_TO_PATTERN.matcher(postfix);
			}
			else if(INDEX_PATTERN.matcher(postfix).lookingAt())
			{	
				match = INDEX_PATTERN.matcher(postfix);
			}
			else
			{
				System.err.println("ERROR: Can't find leading range constraint in " + postfix);
				return false;
			}
			match.find();
			postfix = postfix.substring(match.end()).trim();
		
			
			
			// We know parent is a range constraint and child is index or range constraint by our assumptions
			// We also know they are compatible
			if(chCon.indexOf('&') == -1)
			{
				// Get the offset from the base we need to apply to driver constraints
				try{offsetFromBase = Integer.parseInt(chCon) - Math.min(((VHDLArrayType)parentType).getLeftIndex(), ((VHDLArrayType)parentType).getRightIndex());}
				catch(Exception e)
				{
					System.err.println("Parent should be of VHDLArrayType, but is of " + parentType.getName());
					System.err.println("Child: " + name + "\nType: " + type.typeToString());
					System.err.println("Parent: " + parent.getName() + "\nType: " + parentType.typeToString());
					e.printStackTrace();
					System.exit(-1);
				}
			}
			else
			{
				int leftBound = Integer.parseInt(chCon.split("&")[0]);
				int rightBound = Integer.parseInt(chCon.split("&")[1]);
				
				range = Math.abs(leftBound - rightBound) + 1;
				offsetFromBase = Math.min(leftBound, rightBound) - Math.min(((VHDLArrayType)parentType).getLeftIndex(), ((VHDLArrayType)parentType).getRightIndex());
			}
			
			// Remove the parent's range constraint from the driver
			// Only if the parent isn't a prefix of the child (this means parent and child, hence driver have compatible but conflicting constraints)
			if(!name.startsWith(parent.getName()) && driver.indexOf('(') != -1)
			{
				editedDriver = driver.substring(0, driver.lastIndexOf('(')).trim();
			}
			
			// Adjust the driver according to the offset and base
			int newDriverLeft = ((VHDLArrayType)driverType).getLeftIndex();
			int newDriverRight = ((VHDLArrayType)driverType).getRightIndex();
			
			// Handle index constraint
			if(range == 0)
			{
				editedDriver = editedDriver + "( " + (offsetFromBase + Math.min(newDriverLeft, newDriverRight)) + " )";
				
				// Make sure the new driver is compatible with the old driver
				if((offsetFromBase + Math.min(newDriverLeft, newDriverRight)) > Math.max(newDriverLeft, newDriverRight))
				{
					System.err.println(name + ": Can't add driver " + editedDriver + postfix + " to the drivers list: Index outside of range of " + driverType.getName());
					return false;
				}
			}
			// Handle range constraints
			else if(((VHDLArrayType)driverType).isRangeDownto())
			{
				newDriverRight += offsetFromBase;
				newDriverLeft = newDriverRight + range - 1;
				editedDriver = editedDriver + "( " + newDriverLeft + " DOWNTO " + newDriverRight + " )";
				
				// Make sure the new driver is compatible with the old driver
				if(newDriverLeft > ((VHDLArrayType)driverType).getLeftIndex() || newDriverRight < ((VHDLArrayType)driverType).getRightIndex())
				{
					System.err.println(name + ": Can't add driver " + editedDriver + postfix + " to the drivers list: Range outside of range of " + driverType.getName());
					return false;
				}
			}
			else
			{
				newDriverLeft += offsetFromBase;
				newDriverRight = newDriverLeft + range - 1;
				editedDriver = editedDriver + "( " + newDriverLeft + " TO " + newDriverRight + " )";
				
				// Make sure the new driver is compatible with the old driver
				if(newDriverLeft < ((VHDLArrayType)driverType).getLeftIndex() || newDriverRight > ((VHDLArrayType)driverType).getRightIndex())
				{
					System.err.println(name + ": Can't add driver " + editedDriver + postfix + " to the drivers list: Range outside of range of " + driverType.getName());
					return false;
				}
			}
		}
		else
		{
			postfix = name.substring(prefix.length()).trim();
		}
		
		// Make sure we can get a type for a driver before adding it to the list
		if(getTypeOf(editedDriver + postfix, driverBaseType) == null)
		{
			System.err.println(name + ": Can't add driver " + editedDriver + postfix + " to the drivers list");
			return false;
		}
		
		
		if(DEBUG)
		{
			System.out.println("Child: " + name + "\tParent: " + parent.getName() + "\tDriver: " + driver + "\tReturned: " + editedDriver + postfix);
		}
		addDriver(editedDriver + postfix);
		return true;
	}

	// Returns a list of strings and types that represent all possible ways to address any whole version of this object and the type of the resulting object
	// Important for record type object which can have their subfields addressed independently
	public ArrayList<Tuple> getListOfAllAddressableObjectNames()
	{
		ArrayList<Tuple> toRet = new ArrayList<Tuple>();
		toRet.add(new Tuple(name, type));
		
		// Only special case is record type signals
		if(getType().isRecordType())
		{
			// Each field of a record is addressable independently
			// Prepend the variable name on each possible subfield address string
			ArrayList<Tuple> subfieldStrings = ((VHDLRecordType)getType()).getListOfAllPossibleSubfieldStrings();
			
			for(int index = 0; index < subfieldStrings.size(); ++index)
			{
				toRet.add(new Tuple(name + "." + subfieldStrings.get(index).path, subfieldStrings.get(index).type));
			}
		}
		
		return toRet;
	}
	
	// Returns true if the passed string is a number of Boolean value
	private static boolean isNumberOrBoolean(String value)
	{
		if(Pattern.matches("[0-9a-fA-F]+", value) || Pattern.matches("\"[0-1]+\"", value))
		{
			return true;
		}

		if(value.equalsIgnoreCase("TRUE") || value.equalsIgnoreCase("FALSE"))
		{
			return true;
		}
			
		return false;
	}
	
	@Override
	public String toString()
	{
		return "" + name + " : " + type.getName();
	}
	
	// Returns a string formatted for [name&driver]+ usage
	public String toStringDrivers()
	{
		StringBuffer toRet = new StringBuffer("");
		
		for(int driver = 0; driver < numDrivers(); ++driver)
		{
			// Exclude slice information
			String source = getDriver(driver);
			  
			// Filter numbers and Boolean because of the high degree of sharing
			if(isNumberOrBoolean(source))
			{
				continue;
			}
			  
			toRet.append(name + "&" + source + "\n");
		}
		
		return toRet.toString();
	}
	
	// Returns a string formatted for DOT usage
	public String toStringDOT()
	{
		StringBuffer toRet = new StringBuffer("");
		
		// Remove slicing to get a clearer picture of actual drivers
		String target = getNameNoSlicing();
		  
		// Make boxes for registers
		if(isRegister())
		{
			toRet.append("\t\"" + target + "\" [shape=box]" + "\n");
		}
		  
		for(int driver = 0; driver < numDrivers(); ++driver)
		{
			// Exclude slice information
			String source = VHDLObject.getNameNoSlicing(getDriver(driver));
			  
			// Filter numbers and Boolean because of the high degree of sharing
			if(isNumberOrBoolean(source))
			{
				continue;
			}
			  
			toRet.append("\t\"" + source + "\" -> \"" + target + "\";" + "\n");
		}
		
		return toRet.toString();
	}
	
	// Returns a string formatted for aiSee3 usage
	public String toStringaiSee3()
	{
		StringBuffer toRet = new StringBuffer("");
		
		// Remove slicing to get a clearer picture of actual drivers
		String target = getNameNoSlicing();
		  
		// Make boxes for registers, circles for rest
		if(isRegister())
		{
			toRet.append("node: {title: \"" + target + "\"\tshape: circle}\n");
		}
		else
		{
			toRet.append("node: {title: \"" + target + "\"\tshape: box}\n");
		}
		
		// Add an edge for each driver
		for(int driver = 0; driver < numDrivers(); ++driver)
		{
			// Exclude slice information
			String source = VHDLObject.getNameNoSlicing(getDriver(driver));
			  
			// Filter numbers and Boolean because of the high degree of sharing
			if(isNumberOrBoolean(source))
			{
				continue;
			}
			
			toRet.append("edge: {source: \"" + source + "\"\ttarget: \"" + target + "\"}\n"); 
		}
		
		return toRet.toString();
	}
	
	// Returns true when this VHDL object is strictly more constrained than the passed VHDL object
	// Equality and overlapping constraints return false
	public boolean isMoreConstrainedVersionOf(VHDLObject lessConstrainedObject)
	{
		String lessCon = lessConstrainedObject.getName();
		
		// Ensure the base names match, but not equal
		if(!getBaseName().equals(lessConstrainedObject.getBaseName()) || this.equals(lessConstrainedObject))
		{
			return false;
		}
		  
		// Check for lessCon being a simple prefix to moreCon
		if(name.startsWith(lessConstrainedObject.getName()))
		{
			return true;
		}
		  
		// Only case where we have constraint containment when lessCon isn't a prefix of moreCon is when the final constraint of lessCon is a range constraint
		// For this case, we ensure that the first constraint of moreCon is compatible with the last constraint of lessCon, is a range constraint
		String lastConstraint;
		if(lessCon.lastIndexOf('.') > lessCon.lastIndexOf('('))
		{
			return false;
		}
		  
		lastConstraint = lessCon.substring(lessCon.lastIndexOf('('));
			  
		// Make sure last constraint of lessCon isn't an index constraint
		if(!lastConstraint.contains("TO"))
		{
			return false;
		}
			
		// lessCon minus last constraint must be a prefix to moreCon for strict containment
		String prefix = lessCon.substring(0, lessCon.length() - lastConstraint.length()).trim();
		if(!name.startsWith(prefix))
		{
			return false;
		}
		  
		// First constraint of moreCon constraint postfix must be a range or index constraint
		String postfix = name.substring(prefix.length()).trim();
		if(postfix.length() == 0 || postfix.charAt(0) != '(')
		{
			return false;
		}
		  
		// Make sure the index or range is within the bounds of the last constraint of lessCon
		lastConstraint = findFirstRangeConstraint(lastConstraint);
		int lessLeftBound = Integer.parseInt(lastConstraint.split("&")[0]);
		int lessRightBound = Integer.parseInt(lastConstraint.split("&")[1]);
		
		// Get the index/bounds of moreCon
		String firstConstraint = findFirstRangeConstraint(postfix);
		if(firstConstraint.indexOf('&') == -1)
		{
			int index = Integer.parseInt(firstConstraint);
			
			// lessCon constraint is DOWNTO
			if(lessLeftBound >= lessRightBound)
			{
				// Ensure that index is within range bounds
				if(index > lessLeftBound || index < lessRightBound)
				{
					return false;
				}
				return true;
			}
			
			// lessCon last constraint is TO
			// Ensure that index is within range bounds
			if(index < lessLeftBound || index > lessRightBound)
			{
				return false;
			}
			return true;
		}
		  
		// Get the range bounds for the first constraint of the postfix
		int moreLeftBound = Integer.parseInt(firstConstraint.split("&")[0]);
		int moreRightBound = Integer.parseInt(firstConstraint.split("&")[1]);
		  
		// lessCon constraint is DOWNTO
		if(lessLeftBound >= lessRightBound)
		{
			// moreCon constraint is DOWNTO
			if(moreLeftBound >= moreRightBound)
			{
				// Ensure that range is within range bounds
				if(moreLeftBound > lessLeftBound || moreRightBound < lessRightBound)
				{
					return false;
				}
			  	return true;
			}
			// moreCon constraint is TO
			else
			{
				// Ensure that range is within range bounds
				if(moreRightBound > lessLeftBound || moreLeftBound < lessRightBound)
				{
					return false;
				}
				return true;
			}
		}
			
		// lessCon last constraint is TO
		// moreCon constraint is DOWNTO
		if(moreLeftBound >= moreRightBound)
		{
			// Ensure that range is within range bounds
			if(moreLeftBound > lessRightBound || moreRightBound < lessLeftBound)
			{
				return false;
			}
			return true;
		}
		// moreCon constraint is TO
		else
		{
			// Ensure that range is within range bounds
			if(moreRightBound > lessRightBound || moreLeftBound < lessLeftBound)
			{
				return false;
			}
			return true;
		}
	}
	
	// Utility method that finds and returns the bounds for the first range/index constraint in the passed name
	// Returns in the form of "index" or "leftBound&rightBound" or null if no range/index constraint
	public static String findFirstRangeConstraint(String name)
	{
		// Test to see which type of constraint is first in the string, reduce it then look again
		Matcher match;
		int downtoStart = Integer.MAX_VALUE;
		int toStart = Integer.MAX_VALUE;
		int indexStart = Integer.MAX_VALUE;
		
		// See what match we have and get the start to see which is first
		if((match = RANGE_DOWNTO_PATTERN.matcher(name)).find())
		{
			downtoStart = match.start();
		}
		if((match = RANGE_TO_PATTERN.matcher(name)).find())
		{
			toStart = match.start();
		}
		if((match = INDEX_PATTERN.matcher(name)).find())
		{
			indexStart = match.start();
		}
		
		// If no matches return null
		if(downtoStart == Integer.MAX_VALUE && toStart == Integer.MAX_VALUE && indexStart == Integer.MAX_VALUE)
		{
			if(DEBUG_CONSTRAINTS)
			{
				System.out.println("Couldn't find any index or range constraints in the string: " + name);
			}
			return null;
		}
		
		if(downtoStart < toStart && downtoStart < indexStart)
		{
			// Get the constraint minus the open and closing parens
			(match = RANGE_DOWNTO_PATTERN.matcher(name)).find();
			String constraint = name.substring(match.start()+1, match.end()-1).trim();
			  
			// Get the bounds to the left and right of the direction indicator
			int leftBound = Integer.parseInt(constraint.split("DOWNTO")[0].trim());
			int rightBound = Integer.parseInt(constraint.split("DOWNTO")[1].trim());
			return "" + leftBound + "&" + rightBound; 
		}
		else if(toStart < indexStart)
		{
			// Get the constraint minus the open and closing parens
			(match = RANGE_TO_PATTERN.matcher(name)).find();
			String constraint = name.substring(match.start()+1, match.end()-1).trim();
			  
			// Get the bounds to the left and right of the direction indicator
			int leftBound = Integer.parseInt(constraint.split("TO")[0].trim());
			int rightBound = Integer.parseInt(constraint.split("TO")[1].trim());
			return "" + leftBound + "&" + rightBound;
		}
		else
		{
			// Get the constraint minus the open and closing parens
			(match = INDEX_PATTERN.matcher(name)).find();
			int indexInto = Integer.parseInt(name.substring(match.start()+1, match.end()-1).trim());
			return "" + indexInto;
		}
	}
	
	@Override
	public boolean equals(Object name)
	{
		if(!(name instanceof VHDLObject))
		{
			return false;
		}
		return this.name.equals(((VHDLObject)name).getName());
	}
}
