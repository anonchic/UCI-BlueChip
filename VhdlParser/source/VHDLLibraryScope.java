import java.util.Iterator;
import java.util.LinkedHashMap;

//Library only has constants, types, and subprograms
public class VHDLLibraryScope extends VHDLScope
{
	private static final long serialVersionUID = 1L;

	public VHDLLibraryScope(String filename, int line, LinkedHashMap<String, VHDLConstant> consts, LinkedHashMap<String, VHDLType> types, LinkedHashMap<String, VHDLScope> scopes)throws Exception
	{
		super("library"+filename+line, filename, line, null, null);
		
		// Copy constants to this scope
		for(Iterator<VHDLConstant> item = consts.values().iterator(); item.hasNext(); )
		{
			try
			{
				addConstant(item.next());
			}
			catch(Exception e)
			{
				;
			}
		}
		
		// Copy types to this scope
		for(Iterator<VHDLType> item = types.values().iterator(); item.hasNext(); )
		{
			try
			{
				addType(item.next());
			}
			catch(Exception e)
			{
				;
			}
		}
		
		// Copy subprograms to this scope
		for(Iterator<VHDLScope> scope = scopes.values().iterator(); scope.hasNext(); )
		{
			VHDLScope temp = scope.next();
			
			// Only add subprogram sub-scopes
			if(!(temp instanceof VHDLSubprogramScope))
			{
				continue;
			}
			
			try
			{
				addScope(temp);
			}
			catch(Exception e)
			{
				;
			}
		}
	}
	
	@Override
	public boolean isLibrary()
	{
		return true;
	}
}
