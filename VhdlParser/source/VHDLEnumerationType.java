import java.util.ArrayList;

// Class that represents the data needed by an arbitrary VHDL enumeration type
// The type is immutable
public class VHDLEnumerationType extends VHDLType
{
	private static final long serialVersionUID = 1L;
	private ArrayList<String> possibleValues = null;
	private VHDLEnumerationType parentType = null;
	
	public VHDLEnumerationType(String name, ArrayList<String> possibleValues)
	{	
		super(name);
		this.possibleValues = possibleValues;
		
		// Make sure there is at least one possible value
		if(possibleValues == null || possibleValues.size() < 1)
		{
			System.out.println("ERROR: An enumeration requires at least one possible value: " + name);
			System.exit(-1);
		}
	}
	
	// Constructor for when the enumeration is a subtype of an existing enumeration
	public VHDLEnumerationType(String name, VHDLEnumerationType parent, int leftIndex, int rightIndex)
	{
		super(name);
		parentType = parent;
		possibleValues = new ArrayList<String>(Math.abs(leftIndex-rightIndex));
		
		// Verify indices
		if(rightIndex < 0 || leftIndex < 0)
		{
			System.err.println("ERROR: Enumeration subtype " + name + " has index less than 0");
			System.exit(-1);
		}
		if(rightIndex >= parentType.numPossibleValues() || leftIndex >= parentType.numPossibleValues())
		{
			System.err.println("ERROR: Enumeration subtype " + name + " has index greater than the numer of possible values in parent type " + parentType.getName());
			System.exit(-1);
		}
		
		// Possible values come from the parent
		// Subtype can order the elements copied from the parent in reverse
		// Enums are always numbered from 0 to n
		if(leftIndex <= rightIndex)
		{
			for(int value = leftIndex; value <= rightIndex; ++value)
			{
				possibleValues.add(parent.getPossibleValueAt(value));
			}
		}
		else
		{
			for(int value = leftIndex; value >= rightIndex; --value)
			{
				possibleValues.add(parent.getPossibleValueAt(value));
			}
		}
	}
	
	// Constructor for when the enumeration is a subtype of an existing enumeration, uses values as range bounds
	public VHDLEnumerationType(String name, VHDLEnumerationType parent, String left, String right)
	{
		super(name);
		int leftIndex = parent.getIndexOfPossibleValue(left);
		int rightIndex = parent.getIndexOfPossibleValue(right);
		parentType = parent;
		possibleValues = new ArrayList<String>(Math.abs(leftIndex-rightIndex));
		
		// Verify indices
		if(rightIndex < 0 || leftIndex < 0)
		{
			System.err.println("ERROR: Enumeration subtype " + name + " has index less than 0");
			System.exit(-1);
		}
		if(rightIndex >= parentType.numPossibleValues() || leftIndex >= parentType.numPossibleValues())
		{
			System.err.println("ERROR: Enumeration subtype " + name + " has index greater than the numer of possible values in parent type " + parentType.getName());
			System.exit(-1);
		}
		
		// Possible values come from the parent
		// Subtype can order the elements copied from the parent in reverse
		// Enums are always numbered from 0 to n
		if(leftIndex <= rightIndex)
		{
			for(int value = leftIndex; value <= rightIndex; ++value)
			{
				possibleValues.add(parent.getPossibleValueAt(value));
			}
		}
		else
		{
			for(int value = leftIndex; value >= rightIndex; --value)
			{
				possibleValues.add(parent.getPossibleValueAt(value));
			}
		}
	}
	
	// Constructor for when the enumeration is a subtype of an existing enumeration, alias for parent
	public VHDLEnumerationType(String name, VHDLEnumerationType parent)
	{
		super(name);
		parentType = parent;
		possibleValues = new ArrayList<String>(parentType.numPossibleValues());
		
		// Possible values come from the parent
		for(int value = 0; value < parentType.numPossibleValues(); ++value)
		{
			possibleValues.add(parent.getPossibleValueAt(value));
		}
	}	
	
	// Getters for instance variable values
	public boolean isSubtype()
	{
		return (parentType != null);
	}
	public VHDLEnumerationType getParentType()
	{
		return parentType;
	}
	public boolean isEnumerationType()
	{
		return true;
	}
	public boolean isScalarType()
	{
		return true;
	}
	public boolean isDiscreteType()
	{
		return true;
	}

	// Returns the number of possible values this type has
	public int numPossibleValues()
	{
		return possibleValues.size();
	}
	
	// Returns the value at the requested index in the possible value list
	public String getPossibleValueAt(int index)
	{
		if(possibleValues.size() <= index || index < 0)
		{
			System.err.println("ERROR: Bad request for enumeration value " + index + " in enumeration " + name);
			System.exit(-1);
			return null;
		}
		return possibleValues.get(index);
	}
	
	public int getIndexOfPossibleValue(String value)
	{
		for(int index = 0; index < possibleValues.size(); ++index)
		{
			if(possibleValues.get(index).equalsIgnoreCase(value))
			{
				return index;
			}
		}
		
		return -1;
	}
	
	// Predefined attributes
	// Is the leftmost value of the type. (Largest if downto)
	public Object left() throws IllegalAttributeException
	{
		return getPossibleValueAt(0);
	}
	// Is the rightmost value of type T. (Smallest if downto)
	public Object right() throws IllegalAttributeException
	{
		return getPossibleValueAt(numPossibleValues() - 1);
	}
    // Is the highest value of type T.
	public Object high() throws IllegalAttributeException
	{
		return right();
	}
    // Is the lowest value of type T.
	public Object low() throws IllegalAttributeException
	{
		return left();
	}
	// Is boolean true if range of T defined with to .
	public boolean ascending() throws IllegalAttributeException
	{
		return true;
	}
	// Is the range  A'LEFT to A'RIGHT  or  A'LEFT downto A'RIGHT .
    public int[] range() throws IllegalAttributeException
    {
    	throw new IllegalAttributeException("Enumeration type " + name + " doesn't support RANGE attribute");
    }
    // Is the range of A with to and downto reversed.
    public int[] reverse_range() throws IllegalAttributeException
    {
    	throw new IllegalAttributeException("Enumeration type " + name + " doesn't support RECERSE_RANGE attribute");
    }
    // Is the integer value of the number of elements in array A.
    public int length() throws IllegalAttributeException
    {
    	throw new IllegalAttributeException("Enumeration type " + name + " doesn't support LENGTH attribute");
    }
    
	// Returns a string containing the type's information
	public String typeToString()
	{
		String temp = "";
		
		temp += "Type name: " + name;
		temp += "\nEnumeration";
		if(possibleValues != null)
		{
			for(int value = 0; value < possibleValues.size(); ++value)
			{
				temp += "\n\tPossible value: " + possibleValues.get(value);
			}
		}
		if(parentType != null)
		{
			temp += "\nParent type:" + parentType.getName();
		}
		
		return temp;
	}
}