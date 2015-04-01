// Instead of having a subtype, create a new type with the correct properties
	// The relationship amongst types isn't important during processing, it's mainly there to help programmers document their code
	// Subtypes take over the functions designated for their parents (keep a reference to the parent type so we can look-up functions when processing statements)

import java.io.Serializable;
import java.util.regex.Pattern;

// Exception thrown when an attribute is requested of a type that doesn't support it
class IllegalAttributeException extends Exception
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 5216067372242327986L;

	public IllegalAttributeException()
	{
		super("Type doesn't support this attribute");
	}
	
	public IllegalAttributeException(String message)
	{
		super(message);
	}
}

// Class that represents the data needed by an arbitrary VHDL type
// The type is immutable
public abstract class VHDLType implements Serializable
{
	private static final long serialVersionUID = 1L;
	protected String name;
	
	public VHDLType(String typeName)
	{
		// Verify that the name is valid
		if(!Pattern.matches("[a-zA-Z]+[a-zA-Z_0-9]*", typeName) || typeName.equalsIgnoreCase("ERROR_1234567"))
		{
			System.out.println("ERROR: VHDL Type not constructed correctly: " + name);
			System.exit(-1);
		}
		name = typeName;
	}
	
	// Getters for instance variable values
	public String getName()
	{
		return name;
	}
	public boolean isRecordType()
	{
		return false;
	}
	public boolean isConstrainedArrayType()
	{
		return false;
	}
	public boolean isArrayType()
	{
		return false;
	}
	abstract public boolean isSubtype();
	public boolean isEnumerationType()
	{
		return false;
	}
	public boolean isIntegerType()
	{
		return false;
	}
	public boolean isScalarType()
	{
		return false;
	}
	public boolean isDiscreteType()
	{
		return false;
	}
	public boolean isCompositeType()
	{
		return false;
	}
	abstract public VHDLType getParentType();
	
	// Predefined attributes
	// Is the base type of the type
	public VHDLType base()
	{
		return getParentType() == null ? this : getParentType();
	}
	// Is the leftmost value of the type. (Largest if downto)
	abstract public Object left() throws IllegalAttributeException;
	// Is the rightmost value of type T. (Smallest if downto)
	abstract public Object right() throws IllegalAttributeException;
    // Is the highest value of type T.
	abstract public Object high() throws IllegalAttributeException;
    // Is the lowest value of type T.
	abstract public Object low() throws IllegalAttributeException;
	// Is boolean true if range of T defined with to .
	abstract public boolean ascending() throws IllegalAttributeException;
	// Is the range  A'LEFT to A'RIGHT  or  A'LEFT downto A'RIGHT .
	abstract public int[] range() throws IllegalAttributeException;
    // Is the range of A with to and downto reversed.
	abstract public int[] reverse_range() throws IllegalAttributeException;
    // Is the integer value of the number of elements in array A.
	abstract public int length() throws IllegalAttributeException;

	// Unsupported attributes
	// Performed on base type of this type (if a subtype)
	// Converts the passed value into a string
	public String image(String value) throws IllegalAttributeException
	{
		throw new IllegalAttributeException("IMAGE attribute not supported");
	}
	// Converts the passed value string into a value of the type
	public String value(String value) throws IllegalAttributeException
	{
		throw new IllegalAttributeException("VALUE attribute not supported");
	}
	// Returns the position of the given value in the type's range
	public int pos(String value) throws IllegalAttributeException
	{
		throw new IllegalAttributeException("POS attribute not supported");
	}
	// Returns the value of the item in the given position in the type's range
	public String val(int index) throws IllegalAttributeException
	{
		throw new IllegalAttributeException("VAL attribute not supported");
	}
	// Returns the value that proceeds the given value for this type
	public String succ(String value) throws IllegalAttributeException
	{
		throw new IllegalAttributeException("SUCC attribute not supported");
	}
	// Returns the value that precedes the given value for this type
	public String pred(String value) throws IllegalAttributeException
	{
		throw new IllegalAttributeException("PRED attribute not supported");
	}
	// Returns the value that is left of the given value for this type
	public String leftOf(String value) throws IllegalAttributeException
	{
		throw new IllegalAttributeException("LEFTOF attribute not supported");
	}
	// Returns the value that is left of the given value for this type
	public String rightOf(String value) throws IllegalAttributeException
	{
		throw new IllegalAttributeException("RIGHTOF attribute not supported");
	}

	// Method that returns true if this object has the same name as the passed type object
	public boolean equals(Object o)
	{
		if(o.getClass() != this.getClass())
		{
			return false;
		}
		
		return name.equalsIgnoreCase(((VHDLType)o).getName());
	}
	
	// Returns a string representation of this type
	public String toString()
	{
		return name;
	}
	
	// Returns a string containing the type's information
	abstract public String typeToString();
}
