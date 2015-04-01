package VHDLUtils;

import java.util.ArrayList;

// Object that contains all essential information about types allowed in VHDL
public class VHDLType
{
	private String name;
	private ArrayList<VHDLTypeField> fields;
	private boolean isComposite;
	private boolean isArray = false;
	private int leftBound = 0;
	private int rightBound = 0;
	private VHDLType elementType = null;
	
	public VHDLType(VHDLType copy)
	{
		this.name = copy.name;
		this.fields = copy.fields;
		this.isComposite = copy.isComposite;
		this.isArray = copy.isArray;
		this.leftBound = copy.leftBound;
		this.rightBound = copy.rightBound;
		this.elementType = copy.elementType;
	}
	
	public VHDLType(String name, boolean isComposite)
	{
		this.name = name;
		this.isComposite = isComposite;
		
		if(isComposite)
		{
			fields = new ArrayList<VHDLTypeField>();
		}
		else
		{
			fields = null;
		}
	}
	
	// Getters for this object's private instance variables
	public String getName()
	{
		return name;
	}
	public boolean isCompositeType()
	{
		return isComposite;
	}
	public int numFields()
	{
		return isComposite ? fields.size() : -1;
	}
	public VHDLTypeField getField(int fieldNum)
	{
		if(isComposite && fieldNum > -1 && fieldNum < fields.size())
		{
			return fields.get(fieldNum);
		}
		return null;
	}

	// Returns the index of a given field name or -1 if none exists
	public int indexOfField(String name)
	{
		return fields.indexOf(new VHDLTypeField(name, null));
	}
	
	// Appends the passed field to the end of this types field list
	public void addField(VHDLTypeField field)
	{
		if(isComposite)
		{
			fields.add(field);
		}
	}
	
	// Remove a field by its index
	public void removeField(int fieldNum)
	{
		if(isComposite && fieldNum > -1 && fieldNum < fields.size())
		{
			fields.remove(fieldNum);
		}
	}
	
	// Returns whether this type represents an array
	public boolean isArray()
	{
		return isArray;
	}
	// Sets whether this type represents an array
	public void setArray(boolean isArray)
	{
		this.isArray = isArray;
	}
	
	// Sets the type of element stored in this array
	public void setArrayElementType(VHDLType type)
	{
		elementType = type;
	}
	// Gets the type of element stored in this array
	public VHDLType getArrayElementType()
	{
		return elementType;
	}
	
	// set the bounds for array types
	public void setArrayBounds(int left, int right)
	{
		leftBound = left;
		rightBound = right;
	}
	
	// Returns the left bound of the array
	public int getLeftBound()
	{
		return leftBound;
	}
	
	// Returns the right bound of the array
	public int getRightBound()
	{
		return rightBound;
	}
	
	// Returns a string with this Type's basic information
	public String printTypeNameAndFields()
	{
		String out = name + "&" + isComposite + "&" + isArray + "\n";
		
		for(int field = 0; isComposite && field < fields.size(); ++field)
		{
			out += fields.get(field).getName() + "&" + fields.get(field).getType().getName() + "\n";
		}
		
		return out.substring(0, out.length() - 1);
	}
	
	// Compares other VHDLTypes by name to see if they are equal
	public boolean equals(Object o)
	{
		if(o.getClass() != this.getClass())
		{
			return false;
		}
		
		return name.equalsIgnoreCase(((VHDLType)o).getName());
	}
	
	public String toString()
	{
		return name;
	}
}
