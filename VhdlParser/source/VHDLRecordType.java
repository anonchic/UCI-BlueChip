import java.util.ArrayList;

// Class that represents the data needed by an arbitrary VHDL record type
// The type is immutable
public class VHDLRecordType extends VHDLType
{
	private static final long serialVersionUID = 1L;
	private ArrayList<VHDLTypeField> fields = null;
	private VHDLRecordType parentType = null;
	
	public VHDLRecordType(String name, ArrayList<VHDLTypeField> fields)
	{	
		super(name);
		this.fields = fields;
		
		// Make sure there is at least one field
		if(fields == null || fields.size() < 1)
		{
			System.out.println("ERROR: A record requires at least one field: " + name);
			System.exit(-1);
		}
	}
	
	// Constructor for when the record is a subtype of an existing record
	// Record subtypes serve only as aliases to their parent types
	public VHDLRecordType(String name, VHDLRecordType parent)
	{
		super(name);
		parentType = parent;
		
		
		// Copy the list of fields from the parent
		fields = new ArrayList<VHDLTypeField>(parentType.numFields());
		for(int field = 0; field < parentType.numFields(); ++field)
		{
			fields.add(parent.getFieldAt(field));
		}
	}
	
	// Getters for instance variable values
	public boolean isSubtype()
	{
		return (parentType != null);
	}
	public VHDLRecordType getParentType()
	{
		return parentType;
	}
	public boolean isRecordType()
	{
		return true;
	}
	public boolean isCompositeType()
	{
		return true;
	}
	
	// Returns the number of fields this record has
	public int numFields()
	{
		return fields.size();
	}
	
	// Returns the field at the requested index in the field list
	public VHDLTypeField getFieldAt(int fieldNum)
	{
		if(fields.size() <= fieldNum || fieldNum < 0)
		{
			System.err.println("ERROR: Bad request for field " + fieldNum + " in record " + name);
			System.exit(-1);
			return null;
		}
		return fields.get(fieldNum);
	}
	// Returns the field with the passed name or null if non fits
	public VHDLTypeField getField(String name)
	{
		for(int field = 0; field < numFields(); ++field)
		{
			if(fields.get(field).getName().equals(name))
			{
				return fields.get(field);
			}
		}
		
		return null;
	}
	// Predefined attributes
	// Is the leftmost value of the type. (Largest if downto)
	public Object left() throws IllegalAttributeException
	{
		throw new IllegalAttributeException("Record type " + name + " doesn't support LEFT attribute");
	}
	// Is the rightmost value of type T. (Smallest if downto)
	public Object right() throws IllegalAttributeException
	{
		throw new IllegalAttributeException("Record type " + name + " doesn't support RIGHT attribute");
	}
    // Is the highest value of type T.
	public Object high() throws IllegalAttributeException
	{
		throw new IllegalAttributeException("Record type " + name + " doesn't support HIGH attribute");
	}
    // Is the lowest value of type T.
	public Object low() throws IllegalAttributeException
	{
		throw new IllegalAttributeException("Record type " + name + " doesn't support LOW attribute");
	}
	// Is boolean true if range of T defined with to .
	public boolean ascending() throws IllegalAttributeException
	{
		throw new IllegalAttributeException("Record type " + name + " doesn't support ASCENDING attribute");
	}
	// Is the range  A'LEFT to A'RIGHT  or  A'LEFT downto A'RIGHT .
    public int[] range() throws IllegalAttributeException
    {
    	throw new IllegalAttributeException("Record type " + name + " doesn't support RANGE attribute");
    }
    // Is the range of A with to and downto reversed.
    public int[] reverse_range() throws IllegalAttributeException
    {
    	throw new IllegalAttributeException("Record type " + name + " doesn't support REVERSE_RANGE attribute");
    }
    // Is the integer value of the number of elements in array A.
    public int length() throws IllegalAttributeException
    {
    	throw new IllegalAttributeException("Record type " + name + " doesn't support LENGTH attribute");
    }
    
	// Returns a string containing the type's information
	public String typeToString()
	{
		String temp = "";
		
		temp += "Type name: " + name;
		temp += "\nRecord";
		if(fields != null)
		{
			for(int field = 0; field < fields.size(); ++field)
			{
				temp += "\n\tField: " + fields.get(field);
			}
		}
		if(parentType != null)
		{
			temp += "\nParent type:" + parentType.getName();
		}
		
		return temp;
	}
	
	// Returns a list of all possible combinations of subfield addresses
	public ArrayList<Tuple> getListOfAllPossibleSubfieldStrings()
	{
		ArrayList<Tuple> toRet = new ArrayList<Tuple>(2*numFields());
		
		for(int field = 0; field < numFields(); ++field)
		{
			VHDLTypeField currentField = getFieldAt(field);
			
			toRet.add(new Tuple(currentField.getName(), currentField.getType()));
			
			// Need to recursively query record types only to add their fields
			if(currentField.getType().isRecordType())
			{
				// Each field of a record is addressable independently
				// Prepend the signal name on each possible subfield address string
				ArrayList<Tuple> subfieldStrings = ((VHDLRecordType)currentField.getType()).getListOfAllPossibleSubfieldStrings();
					
				for(int index = 0; index < subfieldStrings.size(); ++index)
				{
					toRet.add(new Tuple(currentField.getName() + "." + subfieldStrings.get(index).path, subfieldStrings.get(index).type));
				}
			}
		}
		
		return toRet;
	}
}