package VHDLUtils;

import java.util.ArrayList;

public class VHDLObject
{
	private String name;
	private VHDLType type;
	private ArrayList<String> valueQueue = new ArrayList<String>();
	
	public VHDLObject(String name, VHDLType type)
	{
		this.name = name;
		this.type = type;
	}
	
	// Methods used to directly compare two VHDL objects
	// Returns true if the names are the same
	public boolean equals(Object o)
	{
		if(o.getClass() != VHDLObject.class)
		{
			return false;
		}
		
		return name.equalsIgnoreCase(((VHDLObject)o).name);
	}
	
	// Returns the depth of the value queue
	public int valueQueueDepth()
	{
		return valueQueue.size();
	}
	
	// Adds the passed value to the value queue
	public void addValueToQueue(String value)
	{
		valueQueue.add(0, value);
	}
	
	// Removes the oldest value in the value queue
	public void removeOldestValue()
	{
		if(valueQueue.size() > 0)
		{
			valueQueue.remove(valueQueue.size() - 1);
		}
	}

	// Returns the value in the queue at the requested index
	// 0 based, returns null if index is invalid
	public String getValueAt(int index)
	{
		if(index < 0 || index >= valueQueue.size())
		{
			return null;
		}
		return valueQueue.get(index);
	}
	
	// Returns the name of this object
	public String getName()
	{
		return name;
	}
	
	// Returns a reference to the type of this object
	public VHDLType getType()
	{
		return type;
	}
	
	// Given a field name, return the index of the field that matches the name
	// -1 if no match
	public int indexOfField(String fieldName)
	{
		return type.indexOfField(fieldName);
	}
}