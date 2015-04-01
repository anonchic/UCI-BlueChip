public class VHDLPackageScope extends VHDLScopeSignal
{
	private static final long serialVersionUID = 1L;

	public VHDLPackageScope(String name, String filename, int line)throws Exception
	{
		this(name, filename, line, null, null);
	}
	
	public VHDLPackageScope(String name, String filename, int line, VHDLPackageScope parent)throws Exception
	{
		this(name, filename, line, parent, null);
	}
	
	public VHDLPackageScope(String name, String filename, int line, VHDLUniversalScope lib)throws Exception
	{
		this(name, filename, line, null, lib);
	}
	
	public VHDLPackageScope(String name, String filename, int line, VHDLPackageScope parent, VHDLUniversalScope lib)throws Exception
	{
		super(name, filename, line, parent, lib);
	}
	
	// Overrides and abstract implementations
	@Override
	public boolean isPackage()
	{
		return true;
	}
}
