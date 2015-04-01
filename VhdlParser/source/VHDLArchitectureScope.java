public class VHDLArchitectureScope extends VHDLScopeSignal
{
	private static final long serialVersionUID = 1L;

	public VHDLArchitectureScope(String name, String filename, int line)throws Exception
	{
		this(name, filename, line, null, null);
	}
	
	public VHDLArchitectureScope(String name, String filename, int line, VHDLUniversalScope lib)throws Exception
	{
		this(name, filename, line, null, lib);
	}
	
	public VHDLArchitectureScope(String name, String filename, int line, VHDLEntityScope parent)throws Exception
	{
		this(name, filename, line, parent, null);
	}
	
	public VHDLArchitectureScope(String name, String filename, int line, VHDLEntityScope parent, VHDLUniversalScope lib)throws Exception
	{
		super(name, filename, line, parent, lib);
	}
	
	// Overrides and abstract implementations
	@Override
	public boolean isArchitecture()
	{
		return true;
	}
}
