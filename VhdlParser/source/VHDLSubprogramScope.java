// Adds the ability to add variables in this scope
public class VHDLSubprogramScope extends VHDLScopeSignal
{
	private static final long serialVersionUID = 1L;
	private VHDLScope parent = null;
	
	public VHDLSubprogramScope(String name, String filename, int line, VHDLScope parent)throws Exception
	{
		super(name, filename, line, parent, null);
		
		this.parent = parent;
		
		// Make sure parent is of correct type
		if(!(parent instanceof VHDLPackageScope) && !(parent instanceof VHDLEntityScope) && !(parent instanceof VHDLArchitectureScope))
		{
			throw new Exception("Suprogram " + name + " must be a sub-scope of either an entity, an architecture, or a package scope");
		}
		
		// Subprograms keep parent type and constant information but discard parent signal and variable information
		signalList.clear();
		variableList.clear();
	}
	
	// Returns the parent scope to this scope to the user
	public VHDLScope getParent()
	{
		return parent;
	}
	
	// Overrides and abstract implementations
	@Override
	public boolean isSubprogram()
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