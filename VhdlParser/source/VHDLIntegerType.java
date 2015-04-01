// Class that represents the data needed by an arbitrary VHDL integer type
// The type is immutable
public class VHDLIntegerType extends VHDLType
{
	private static final long serialVersionUID = 1L;
	private int leftIndex = -1;
	private int rightIndex = -1;
	private VHDLIntegerType parentType = null;
	
	public VHDLIntegerType(String name, int leftIndex, int rightIndex)
	{	
		super(name);
		this.leftIndex = leftIndex;
		this.rightIndex = rightIndex;
	}
	
	// Constructor for when the integer is a subtype of an existing integer
	public VHDLIntegerType(String name, VHDLIntegerType parent, int leftIndex, int rightIndex)
	{
		super(name);
		this.leftIndex = leftIndex;
		this.rightIndex = rightIndex;
		parentType = parent;
		
		// Verify indices are compatible with parent's
		if(Math.min(leftIndex, rightIndex) < Math.min(parentType.getLeftIndex(), parentType.getRightIndex()) || Math.max(leftIndex, rightIndex) > Math.max(parentType.getLeftIndex(), parentType.getRightIndex()))
		{
			System.err.println("ERROR: Integer subtype " + name + " has index outside of parent's " + parentType.getName());
			System.exit(-1);
		}
	}
	
	// Constructor for when the integer is a subtype of an existing integer, alias
	public VHDLIntegerType(String name, VHDLIntegerType parent)
	{
		super(name);
		this.leftIndex = parent.getLeftIndex();
		this.rightIndex = parent.getRightIndex();
		parentType = parent;
	}
	
	// Getters for instance variable values
	public boolean isSubtype()
	{
		return (parentType != null);
	}
	public VHDLIntegerType getParentType()
	{
		return parentType;
	}
	public boolean isIntegerType()
	{
		return true;
	}
	public int getLeftIndex()
	{
		return leftIndex;
	}
	public int getRightIndex()
	{
		return rightIndex;
	}
	public boolean isScalarType()
	{
		return true;
	}
	public boolean isDiscreteType()
	{
		return true;
	}
	
	// Returns the range of this integer type
	public int getRange()
	{
		return Math.abs(leftIndex - rightIndex) + 1;
	}
	
	// Returns whether the the constructed direction of this integer type is descending
	public boolean isRangeDownto()
	{
		return leftIndex >= rightIndex;
	}
	
	// Returns whether the the constructed direction of this integer type is ascending
	public boolean isRangeTo()
	{
		return leftIndex <= rightIndex;
	}
	
	// Predefined attributes
	// Is the leftmost value of the type. (Largest if downto)
	public Object left() throws IllegalAttributeException
	{
		return leftIndex;
	}
	// Is the rightmost value of type T. (Smallest if downto)
	public Object right() throws IllegalAttributeException
	{
		return rightIndex;
	}
    // Is the highest value of type T.
	public Object high() throws IllegalAttributeException
	{
		return leftIndex > rightIndex ? leftIndex : rightIndex;
	}
    // Is the lowest value of type T.
	public Object low() throws IllegalAttributeException
	{
		return leftIndex < rightIndex ? leftIndex : rightIndex;
	}
	// Is boolean true if range of T defined with to .
	public boolean ascending() throws IllegalAttributeException
	{
		return leftIndex <= rightIndex;
	}
    // Is the range  A'LEFT to A'RIGHT  or  A'LEFT downto A'RIGHT .
    public int[] range() throws IllegalAttributeException
    {
    	throw new IllegalAttributeException("Integer type " + name + " doesn't support RANGE attribute");
    }
    // Is the range of A with to and downto reversed.
    public int[] reverse_range() throws IllegalAttributeException
    {
    	throw new IllegalAttributeException("Integer type " + name + " doesn't support REVERSE_RANGE attribute");
    }
    // Is the integer value of the number of elements in array A.
    public int length() throws IllegalAttributeException
    {
    	throw new IllegalAttributeException("Integer type " + name + " doesn't support LENGTH attribute");
    }

	// Returns a string containing the type's information
	public String typeToString()
	{
		String temp = "";
		
		temp += "Type name: " + name;
		temp += "\nInteger";
		temp += "\nLeft index: " + leftIndex;
		temp += "\nRight index: " + rightIndex;
		if(parentType != null)
		{
			temp += "\nParent type:" + parentType.getName();
		}
		
		return temp;
	}
}
