// Class that represents the data needed by an arbitrary VHDL array type
// The type is immutable
public class VHDLArrayType extends VHDLType
{
	private static final long serialVersionUID = 1L;
	private int leftIndex = -1;
	private int rightIndex = -1;
	private boolean isConstrained = false;
	private VHDLType memberType = null;
	private VHDLArrayType parentType = null;
	
	// Constructs an new unconstrained array
	public VHDLArrayType(String name, VHDLType memberType)
	{	
		super(name);
		this.memberType = memberType;
		this.isConstrained = false;
	}
	
	// Constructs a new constrained array
	public VHDLArrayType(String name, VHDLType memberType, int leftIndex, int rightIndex)
	{	
		super(name);
		this.memberType = memberType;
		this.isConstrained = true;
		this.leftIndex = leftIndex;
		this.rightIndex = rightIndex;
	}
	
	// Constructor for when the array is a subtype of an existing array
	public VHDLArrayType(String name, VHDLArrayType parent, int leftIndex, int rightIndex)
	{
		super(name);
		this.leftIndex = leftIndex;
		this.rightIndex = rightIndex;
		parentType = parent;
		memberType = parentType.getMemberType();
		isConstrained = true;
		
		// Verify indices are compatible with parent's, if parent is constrained
		if(parentType.isConstrained)
		{
			if(Math.min(leftIndex, rightIndex) < Math.min(parentType.getLeftIndex(), parentType.getRightIndex()) || Math.max(leftIndex, rightIndex) > Math.max(parentType.getLeftIndex(), parentType.getRightIndex()))
			{
				System.err.println("ERROR: Array subtype " + name + " has index outside of parent's " + parentType.getName());
				System.exit(-1);
			}
		}
	}
	
	// Constructor for when the array is a subtype of an existing array, alias
	public VHDLArrayType(String name, VHDLArrayType parent)
	{
		super(name);
		leftIndex = parent.getLeftIndex();
		rightIndex = parent.getRightIndex();
		parentType = parent;
		memberType = parentType.getMemberType();
		isConstrained = parentType.isConstrainedArrayType();
	}
	
	// Getters for instance variable values
	public boolean isSubtype()
	{
		return (parentType != null);
	}
	public VHDLArrayType getParentType()
	{
		return parentType;
	}
	public boolean isArrayType()
	{
		return true;
	}
	public boolean isConstrainedArrayType()
	{
		return isConstrained;
	}
	public VHDLType getMemberType()
	{
		return memberType;
	}
	public int getLeftIndex()
	{
		return leftIndex;
	}
	public int getRightIndex()
	{
		return rightIndex;
	}
	public boolean isCompositeType()
	{
		return true;
	}
	
	// Returns the range of this array type
	public int getRange()
	{
		return Math.abs(leftIndex - rightIndex) + 1;
	}
	
	// Returns whether the the constructed direction of this array type is descending
	public boolean isRangeDownto()
	{
		return leftIndex >= rightIndex;
	}
	
	// Returns whether the the constructed direction of this array type is ascending
	public boolean isRangeTo()
	{
		return leftIndex <= rightIndex;
	}
	
	// Predefined attributes
	// Is the leftmost value of the type. (Largest if downto)
	public Object left() throws IllegalAttributeException
	{
		if(!isConstrained)
		{	
			throw new IllegalAttributeException("Unconstrained array type " + name + " doesn't support LEFT attribute");
		}
		return leftIndex;
	}
	// Is the rightmost value of type T. (Smallest if downto)
	public Object right() throws IllegalAttributeException
	{
		if(!isConstrained)
		{	
			throw new IllegalAttributeException("Unconstrained array type " + name + " doesn't support RIGHT attribute");
		}
		return rightIndex;
	}
    // Is the highest value of type T.
	public Object high() throws IllegalAttributeException
	{
		if(!isConstrained)
		{	
			throw new IllegalAttributeException("Unconstrained array type " + name + " doesn't support HIGH attribute");
		}
		return leftIndex > rightIndex ? leftIndex : rightIndex;
	}
    // Is the lowest value of type T.
	public Object low() throws IllegalAttributeException
	{
		if(!isConstrained)
		{	
			throw new IllegalAttributeException("Unconstrained array type " + name + " doesn't support LOW attribute");
		}
		return leftIndex < rightIndex ? leftIndex : rightIndex;
	}
	// Is boolean true if range of T defined with to .
	public boolean ascending() throws IllegalAttributeException
	{
		if(!isConstrained)
		{	
			throw new IllegalAttributeException("Unconstrained array type " + name + " doesn't support ASCENDING attribute");
		}
		return leftIndex <= rightIndex;
	}
	// Is the range  A'LEFT to A'RIGHT  or  A'LEFT downto A'RIGHT .
    public int[] range() throws IllegalAttributeException
    {
    	if(!isConstrained)
		{	
			throw new IllegalAttributeException("Unconstrained array type " + name + " doesn't support RANGE attribute");
		}
    	return new int[]{leftIndex, rightIndex};
    }
    // Is the range of A with to and downto reversed.
    public int[] reverse_range() throws IllegalAttributeException
    {
    	if(!isConstrained)
		{	
			throw new IllegalAttributeException("Unconstrained array type " + name + " doesn't support REVERSE_RANGE attribute");
		}
    	return new int[]{rightIndex, leftIndex};
    }
    // Is the integer value of the number of elements in array A.
    public int length() throws IllegalAttributeException
    {
    	if(!isConstrained)
		{	
			throw new IllegalAttributeException("Unconstrained array type " + name + " doesn't support LENGTH attribute");
		}
    	return Math.abs(leftIndex - rightIndex);
    }
    
	// Returns a string containing the type's information
	public String typeToString()
	{
		String temp = "";
		
		temp += "Type name: " + name;
		temp += "\nArray";
		temp += "\nConstrained: " + isConstrained;
		temp += "\nLeft index: " + leftIndex;
		temp += "\nRight index: " + rightIndex;
		temp += "\nOf: " + memberType.getName();
		if(parentType != null)
		{
			temp += "\nParent type:" + parentType.getName();
		}
		
		return temp;
	}
}