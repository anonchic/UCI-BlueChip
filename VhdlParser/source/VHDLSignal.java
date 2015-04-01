// Class that contains key information about VHDL signal objects and methods that operate on that data

import java.io.Serializable;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class VHDLSignal extends VHDLObject
{
	private static final long serialVersionUID = 1L;
	
	public VHDLSignal(String name, VHDLType type)
	{
		super(name, type, false);
	}
	public VHDLSignal(String name, VHDLType type, boolean infersStorage)
	{
		super(name, type, infersStorage);
	}
	
	@Override
	public boolean isSignal()
	{
		return true;
	}
	@Override
	public boolean isVariable()
	{
		return false;
	}
}
