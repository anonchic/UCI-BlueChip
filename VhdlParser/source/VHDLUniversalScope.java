// Scope that allows adding of any elements allowed by any other scope
public class VHDLUniversalScope extends VHDLScopeSignal
{
	private static final long serialVersionUID = 1L;

	public VHDLUniversalScope()throws Exception
	{
		super("universal", "memory", 0, null, null);
	}
	
	// Overrides and abstract implementations
	@Override
	public boolean isUniversal()
	{
		return true;
	}
	
	// Methods that query and augment the list of variable objects
	@Override
	public void addVariable(VHDLVariable toAdd)throws Exception
	{
		if(variableList.put(toAdd.getName(), toAdd) != null)
		{
			throw new Exception("Variable " + toAdd.getName() + " already defined in scope, can't redefine in scope " + this);
		}
	}
}
