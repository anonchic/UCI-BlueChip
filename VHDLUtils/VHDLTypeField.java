package VHDLUtils;

// Objects that contain subfield information for composite types
public class VHDLTypeField
{
	private String name;
	private VHDLType type;
	
	public VHDLTypeField(String name, VHDLType type)
	{
		this.name = name;
		this.type = type;
	}
	
	public VHDLType getType()
	{
		return type;
	}
	
	public String getName()
	{
		return name;
	}
	
	public boolean equals(Object o)
	{
		if(o.getClass() != VHDLTypeField.class)
		{
			return false;
		}
		
		return name.equalsIgnoreCase(((VHDLTypeField)o).name);
	}
}