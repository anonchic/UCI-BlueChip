
// Class that contains the possible properties for a VHDL type
public final class VHDLTypeProps
{
	private String name;
	public boolean isRecord;
	public boolean isArray;
	public boolean isConstrained;
	public boolean isEnumeration;
	public boolean isSubtype;
	public boolean isInteger;
	public VHDLType parentType;
	public java.util.ArrayList<String> possibleValues;
	public java.util.ArrayList<VHDLTypeField> fields;
	public String leftBound;
	public String rightBound;
	
	public VHDLTypeProps()
	{
		reset();
	}
	
	// Using the information currently contained in this object, this method creates and returns the appropriate VHDL type object
	public VHDLType generateTypeFromProps()
	{
		// Ensure no conflicting settings
		if((isRecord && (isArray || isEnumeration || isInteger)) || (isArray && (isRecord || isEnumeration || isInteger)) || (isEnumeration && (isArray || isRecord || isInteger)) || (isInteger && (isArray || isEnumeration || isRecord)))
		{
			System.out.println("ERROR: Cannot determine which type to create for " + name + ", conflicting parameters");
			System.exit(-1);
		}
		
		// Handle subtypes with no kind of type set by copying parents
		if(isSubtype && !(isRecord || isArray || isEnumeration || isInteger))
		{
			if(parentType == null)
			{
				System.out.println("ERROR: Cannot create subtype " + name + " due to missing parent type");
				System.exit(-1);
			}
			
			if(parentType instanceof VHDLRecordType)
			{
				isRecord = true;
			}
			else if(parentType instanceof VHDLArrayType)
			{
				isArray = true;
			}
			else if(parentType instanceof VHDLEnumerationType)
			{
				isEnumeration = true;
			}
			else if(parentType instanceof VHDLIntegerType)
			{
				isInteger = true;
			}
			else
			{
				System.out.println("ERROR: Cannot create subtype " + name + " due to bad parent type kind");
				System.exit(-1);
			}
		}
		
		if(isRecord)
		{
			VHDLRecordType newType;
			
			if(isSubtype)
			{
				// Make sure that the parent type matches the new type
				if(parentType.getClass() != VHDLRecordType.class)
				{
					System.out.println("ERROR: Parent of record type " + name + " isn't a record");
					System.exit(-1);
				}
				
				newType = new VHDLRecordType(name, (VHDLRecordType)parentType);
			}
			else
			{
				newType = new VHDLRecordType(name, fields);
			}
			
			return newType;
		}
		else if(isArray)
		{
			VHDLArrayType newType;
			int lBound = 0;
			int rBound = 0;
			
			// If constrained, make sure bounds are ints
			if(leftBound != null)
			{
				try
				{
					lBound = Integer.parseInt(leftBound);
					rBound = Integer.parseInt(rightBound);
				}
				catch(NumberFormatException nfe)
				{
					System.out.println("ERROR: Array " + name + " bounds not integers: " + leftBound + " and " + rightBound);
					System.exit(-1);
				}
			}
			
			if(isSubtype)
			{
				// Make sure that the parent type matches the new type
				if(parentType.getClass() != VHDLArrayType.class)
				{
					System.out.println("ERROR: Parent of array type " + name + " isn't an array");
					System.exit(-1);
				}
				
				// Copy the entire parent array or slice it
				if(leftBound == null) // unconstrained
				{
					newType = new VHDLArrayType(name, (VHDLArrayType)parentType);
				}
				else
				{
					newType = new VHDLArrayType(name, (VHDLArrayType)parentType, lBound, rBound);
				}
			}
			else
			{
				if(isConstrained)
				{
					newType = new VHDLArrayType(name, parentType, lBound, rBound);
				}
				else
				{
					newType = new VHDLArrayType(name, parentType);
				}
			}
			
			return newType;
		}
		else if(isEnumeration)
		{
			VHDLEnumerationType newType = null;
			
			if(isSubtype)
			{
				// Make sure that the parent type matches the new type
				if(parentType.getClass() != VHDLEnumerationType.class)
				{
					System.out.println("ERROR: Parent of enumeration type " + name + " isn't an enumeration");
					System.exit(-1);
				}
				
				// Copy the entire parent array or slice it
				if(leftBound == null)
				{
					newType = new VHDLEnumerationType(name, (VHDLEnumerationType)parentType);
				}
				else
				{
					// See if we need to use ints as bounds or possible values
					if(((VHDLEnumerationType)parentType).getIndexOfPossibleValue(leftBound) == -1 || ((VHDLEnumerationType)parentType).getIndexOfPossibleValue(rightBound) == -1)
					{
						try{newType = new VHDLEnumerationType(name, (VHDLEnumerationType)parentType, Integer.parseInt(leftBound), Integer.parseInt(rightBound));}
						catch(NumberFormatException nfe)
						{
							System.out.println("ERROR: Enumeration type " + name + " bounds not correctly specified");
							System.exit(-1);
						}
					}
					else
					{
						newType = new VHDLEnumerationType(name, (VHDLEnumerationType)parentType, leftBound, rightBound);
					}
				}
			}
			else
			{
				newType = new VHDLEnumerationType(name, possibleValues);
			}
			
			return newType;
		}
		else if(isInteger)
		{
			VHDLIntegerType newType;
			int lBound = 0;
			int rBound = 0;
			
			// If constrained, make sure bounds are ints
			if(leftBound != null)
			{
				try
				{
					lBound = Integer.parseInt(leftBound);
					rBound = Integer.parseInt(rightBound);
				}
				catch(NumberFormatException nfe)
				{
					System.out.println("ERROR: Integer " + name + " bounds not integers: " + leftBound + " and " + rightBound);
					System.exit(-1);
				}
			}
			
			if(isSubtype)
			{
				// Make sure that the parent type matches the new type
				if(parentType.getClass() != VHDLIntegerType.class)
				{
					System.out.println("ERROR: Parent of integer type " + name + " isn't an integer");
					System.exit(-1);
				}
				
				// Copy the entire parent array or slice it
				if(leftBound == null)
				{
					newType = new VHDLIntegerType(name, (VHDLIntegerType)parentType);
				}
				else
				{
					newType = new VHDLIntegerType(name, (VHDLIntegerType)parentType, lBound, rBound);
				}
			}
			else
			{
				newType = new VHDLIntegerType(name, lBound, rBound);
			}
			
			return newType;
		}
		
		System.out.println("ERROR: Cannot determine which type to create for " + name);
		System.exit(-1);
		return null;
	}
	
	// Resets the type properties back to their default values
	public void reset()
	{
		name = "";
		isRecord = false;
		isArray = false;
		isConstrained = false;
		isEnumeration = false;
		isSubtype = false;
		isInteger = false;
		parentType = null;
		possibleValues = null;
		fields = null;
		leftBound = null;
		rightBound = null;
	}
	
	// Creates a new list of possible values
	public void newPossibleValueList()
	{
		possibleValues = new java.util.ArrayList<String>();
	}
	
	// Adds a possible value to the list of possible values
	// Used for enumerations
	public void addPossibleValue(String value)
	{
		if(possibleValues != null)
		{
			possibleValues.add(value);
		}
	}
	
	// Creates a new list of fields
	public void newFieldList()
	{
		fields = new java.util.ArrayList<VHDLTypeField>();
	}
	
	// Adds a field to the list of subfields for this record type
	public void addField(String name, VHDLType type)
	{
		if(fields != null)
		{
			fields.add(new VHDLTypeField(name, type));
		}
	}
	
	// Sets the bounds and range for type which require them
	public void setLeftBound(String left)
	{
		leftBound = left;
	}
	public void setRightBound(String right)
	{
		rightBound = right;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	public String getName()
	{
		return name;
	}
}
