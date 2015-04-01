import java.io.Serializable;

// Immutable class that represents a subfield for record types
public class VHDLTypeField implements Serializable
{
	private static final long serialVersionUID = 1L;
	private String name;
	private VHDLType type;
	
	public VHDLTypeField(String name, VHDLType type)
	{
		this.name = name;
		this.type = type;
	}
	
	// Returns the values of the instance variables
	public String getName()
	{
		return name;
	}
	public VHDLType getType()
	{
		return type;
	}
	
	public String toString()
	{
		return name + "\t" + type;
	}
	
	// Method that returns true if this object has the same name as the passed type object
	public boolean equals(Object o)
	{
		if(o.getClass() != this.getClass())
		{
			return false;
		}
		
		return (name.equalsIgnoreCase(((VHDLTypeField)o).getName()) && type.equals(((VHDLTypeField)o).getType()));
	}
}