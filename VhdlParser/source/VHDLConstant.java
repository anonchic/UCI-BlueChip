// Class that holds all information associated with VHDL constants (incl. generics)

import java.io.Serializable;
import java.util.regex.Pattern;

public class VHDLConstant implements Serializable
{
	private static final long serialVersionUID = 1L;
	private String name;
	private VHDLType type;
	private String value;
	private boolean isGeneric = false;
	
	// Constructs a non-generic constant with given name, type, and value
	public VHDLConstant(String id, VHDLType type, String value)
	{
		this.name = id;
		this.type = type;
		this.value = value;
	}
	// Creates the above, but with value assignment defered
	public VHDLConstant(String id, VHDLType type)
	{
		this.name = id;
		this.type = type;
	}
	
	// Constructs a possibly generic constant with given name, type, and value
	public VHDLConstant(String id, VHDLType type, String value, boolean isAGeneric)
	{
		this.name = id;
		this.type = type;
		this.value = value;
		this.isGeneric = isAGeneric;
	}
	// Creates the above, but with value assignment defered
	public VHDLConstant(String id, VHDLType type, boolean isAGeneric)
	{
		this.name = id;
		this.type = type;
		this.isGeneric = isAGeneric;
	}
	
	// Getters for constant properties
	public String getName()
	{
		return name;
	}
	public VHDLType getType()
	{
		return type;
	}
	public String getValue()
	{
		return value;
	}
	public boolean isAGeneric()
	{
		return isGeneric;
	}

	// Method that allows for setting of this constant's value
	// Throws an exception when a value already exists
	public void setValue(String value)throws Exception
	{
		if(value == null || this.value != null)
		{
			throw new Exception("Cannot set constant value to " + value + " " + name + "'s value is already set to " + this.value);
		}
		this.value = value;
	}
	
	// Given a string representing a name, possibly with constraints, the base object name is returned
	public static String getBaseName(String name)
	{
		return name.split("'")[0].trim().split("\\.")[0].trim().split("\\(")[0].trim();
	}
	
	// Returns true if the passed string is a number of Boolean value
	public static boolean isNumberOrBoolean(String value)
	{
		if(Pattern.matches("[0-9a-fA-F]*", value))
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
		if(type == null)
		{
			return "" + name + " : type_unknown := " + value;
		}
		return "" + name + " : " + type.getName() + " := " + value;
	}
	
	@Override
	public boolean equals(Object name)
	{
		if(!(name instanceof VHDLConstant))
		{
			return false;
		}
		return this.name.equals(((VHDLConstant)name).getName());
	}
}
