// Adds the ability for variables to be declared in this scope
public class VHDLProcessScope extends VHDLScope
{
	private static final long serialVersionUID = 1L;

	public VHDLProcessScope(String name, String filename, int line, VHDLArchitectureScope parent)throws Exception
	{
		super(name, filename, line, parent, null);
	}
	
	public VHDLProcessScope(String filename, int line, VHDLArchitectureScope parent)throws Exception
	{
		this("" + line, filename, line, parent);
	}
	
	// Overrides and abstract implementations
	@Override
	public boolean isProcess()
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