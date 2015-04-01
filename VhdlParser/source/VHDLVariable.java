import java.io.Serializable;
import java.util.ArrayList;
import java.util.regex.Pattern;

class VHDLVariable extends VHDLObject
{
	private static final long serialVersionUID = 1L;
	
	public VHDLVariable(String name, VHDLType type)
	{
		super(name, type, false);
	}
	public VHDLVariable(String name, VHDLType type, boolean infersStorage)
	{
		super(name, type, infersStorage);
	}
	
	@Override
	public boolean isSignal()
	{
		return false;
	}
	@Override
	public boolean isVariable()
	{
		return true;
	}
}
