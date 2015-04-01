// Adds the ability to add signals in the current scope
public abstract class VHDLScopeSignal extends VHDLScope
{
	private static final long serialVersionUID = 1L;

	public VHDLScopeSignal(String name, String filename, int line, VHDLScope parent, VHDLUniversalScope lib)throws Exception
	{
		super(name, filename, line, parent, lib);
	}
	
	// Method that allows scopes that implement this class to have the ability to create sub-scopes
	@Override
	public void addScope(VHDLScope toAdd)throws Exception
	{
		if(scopeList.put(toAdd.getName(), toAdd) != null)
		{
			throw new Exception("Sub-scope " + toAdd.getName() + " already defined in scope, can't redefine in scope " + this);
		}
	}
	
	// Methods that query and augment the list of signal objects
	@Override
	public void addSignal(VHDLSignal toAdd)throws Exception
	{
		if(signalList.put(toAdd.getName(), toAdd) != null)
		{
			throw new Exception("Signal " + toAdd.getName() + " already defined in scope, can't redefine in scope " + this);
		}
	}
}
