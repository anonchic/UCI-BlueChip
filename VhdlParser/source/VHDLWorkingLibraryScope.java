import java.util.ArrayList;

public class VHDLWorkingLibraryScope
{
	// Name assigned to this library
	private String name;
	
	// List of user accessible libraries
	public ArrayList<VHDLWorkingLibraryScope> loadedLibraries = new ArrayList<VHDLWorkingLibraryScope>();
	
	// List of primary and secondary units compiled into this library
	public ArrayList<VHDLScope> designUnits = new ArrayList<VHDLScope>();
	
	public VHDLWorkingLibraryScope(String name)
	{
		this.name = name;
	}
	
	public String getName()
	{
		return name;
	}
	
	// Manipulate loaded libraries
	public void addLoadedLibrary(VHDLWorkingLibraryScope toAdd)throws Exception
	{
		if(loadedLibraries.contains(toAdd.getName()))
		{
			throw new Exception("Working library " + name + " already has loaded library " + toAdd.getName());
		}
		loadedLibraries.add(toAdd);
	}
	public boolean libraryIsLoaded(VHDLWorkingLibraryScope lib)
	{
		return loadedLibraries.contains(lib);
	}
	public boolean libraryNameIsLoaded(String lib)
	{
		return libraryIsLoaded(new VHDLWorkingLibraryScope(lib));
	}
	public int getIndexOfLoadedLibrary(VHDLWorkingLibraryScope lib)
	{
		return loadedLibraries.indexOf(lib);
	}
	public int getIndexOfLoadedLibrary(String lib)
	{
		return getIndexOfLoadedLibrary(new VHDLWorkingLibraryScope(lib));
	}
	public VHDLWorkingLibraryScope getLoadedLibrary(int index)throws Exception
	{
		if(index < 0 || index >= loadedLibraries.size())
		{
			throw new Exception("Invalid index " + index + " into loaded library list of library " + name + " with " + loadedLibraries.size() + " libraries loaded");
		}
		return loadedLibraries.get(index);
	}
	public VHDLWorkingLibraryScope getLoadedLibrary(String libName)throws Exception
	{
		if(!libraryNameIsLoaded(libName))
		{
			throw new Exception("Cannot find loaded library " + libName + " in the working library " + name);
		}
		return loadedLibraries.get(this.getIndexOfLoadedLibrary(libName));
	}
	
	// Manipulate design unit list
	public void addDesignUnit(VHDLScope toAdd)throws Exception
	{
		if(designUnits.contains(toAdd.getName()))
		{
			throw new Exception("Working library " + name + " already has design unit " + toAdd.getName());
		}
		designUnits.add(toAdd);
	}
	public boolean ontainsDesignUnit(VHDLScope unit)
	{
		return designUnits.contains(unit);
	}
	public int getIndexOfDesignUnit(VHDLScope unit)
	{
		return designUnits.indexOf(unit);
	}
	public VHDLScope getDesignUnit(int index)throws Exception
	{
		if(index < 0 || index >= designUnits.size())
		{
			throw new Exception("Invalid index " + index + " into loaded library list of library " + name + " with " + designUnits.size() + " libraries loaded");
		}
		return designUnits.get(index);
	}

	// Methods that get design unit based on a passed name
	public boolean containsDesignUnit(String unitName)
	{
		for(int index = 0; index < designUnits.size(); ++index)
		{
			if(designUnits.get(index).getName().equalsIgnoreCase(unitName))
			{
				return true;
			}
		}
		
		return false;
	}
	public VHDLScope getDesignUnit(String unitName)
	{
		for(int index = 0; index < designUnits.size(); ++index)
		{
			if(designUnits.get(index).getName().equalsIgnoreCase(unitName))
			{
				return designUnits.get(index);
			}
		}
		
		return null;
	}
	
	// Override equals method to aide in comparison based operations
	@Override
	public boolean equals(Object x)
	{
		if(!(x instanceof VHDLWorkingLibraryScope))
		{
			return false;
		}
		
		if(this.name.equalsIgnoreCase(((VHDLWorkingLibraryScope)x).getName()))
		{
			return true;
		}
		
		return false;
	}
}
