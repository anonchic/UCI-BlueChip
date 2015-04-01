public class VHDLEntityScope extends VHDLScopeSignal
{
	private static final long serialVersionUID = 1L;

	public VHDLEntityScope(String name, String filename, int line)throws Exception
	{
		this(name, filename, line, null);
	}
	
	public VHDLEntityScope(String name, String filename, int line, VHDLUniversalScope lib)throws Exception
	{
		super(name, filename, line, null, lib);
	}
	
	// Overrides and abstract implementations
	@Override
	public boolean isEntity()
	{
		return true;
	}
}
