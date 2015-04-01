// Class that represents a scope in VHDL
// Scopes in VHDL can be any one of:
//	entity
//	architecture
//	process
//	subprogram (function, procedure)
//	package
// Scope objects hold all the information created in the scope
//	This includes a name, name of file scope resides in, line number scope starts at, and lists of types and objects created in scope

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Iterator;
import java.lang.StringBuffer;

enum VHDLObjectTypes
{
	CONSTANT,
	TYPE,
	SIGNAL,
	VARIABLE
}

public abstract class VHDLScope implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	// Basic identifying information about this scope
	String name;
	String filename;
	int lineNumber;
	
	// A list of scopes available in this scope
	// Scopes can be declared in Entities, Architectures and Packages 
	LinkedHashMap<String, VHDLScope> scopeList = new LinkedHashMap<String, VHDLScope>(3, (float)0.5);
	
	// A list of constants declared in this scope
	// Constants can be declared in any scope
	LinkedHashMap<String, VHDLConstant> constantList = new LinkedHashMap<String, VHDLConstant>(10, (float)0.5);
	
	// A list of types defined in this scope
	// Types can be declared in any scope
	LinkedHashMap<String, VHDLType> typeList = new LinkedHashMap<String, VHDLType>(10, (float)0.5);
	
	// A list of signal objects defined in this scope
	// Signals can be declared in entities, architectures, packages, and subprograms
	LinkedHashMap<String, VHDLSignal> signalList = new LinkedHashMap<String, VHDLSignal>(30, (float)0.5);
	
	// A list of variable objects defined in this scope
	// Variables can be declared in processes and subprograms
	LinkedHashMap<String, VHDLVariable> variableList = new LinkedHashMap<String, VHDLVariable>(10, (float)0.5);
	
	// Constructor that copies the constants and types from the parent, since VHDL doesn't resolve naming conflicts
	public VHDLScope(String name, String filename, int line, VHDLScope parent, VHDLUniversalScope lib)throws Exception
	{
		this.name = name;
		this.filename = filename;
		lineNumber = line;
		
		// This is the parent if none is passed
		if(parent != null)
		{
			// Copy over sub-scope information from parent
			for(Iterator<VHDLScope> scope = parent.getScopeListIterator(); scope.hasNext();)
			{
				try
				{
					this.addScopeInit(scope.next());
				}
				catch(Exception e)
				{
					;
				}
			}
			// Copy over constant and type information from parent
			for(Iterator<VHDLConstant> constant = parent.getConstantListIterator(); constant.hasNext();)
			{
				try
				{
					this.addConstant(constant.next());
				}
				catch(Exception e)
				{
					;
				}
			}
			for(Iterator<VHDLType> type = parent.getTypeListIterator(); type.hasNext();)
			{
				try
				{
					this.addType(type.next());
				}
				catch(Exception e)
				{
					;
				}
			}
			
			// Copy over signal information from parent
			for(Iterator<VHDLSignal> signal = parent.getSignalListIterator(); signal.hasNext();)
			{
				try
				{
					this.addSignalInit(signal.next());
				}
				catch(Exception e)
				{
					;
				}
			}
			
			// Copy over variable information from parent
			for(Iterator<VHDLVariable> variable = parent.getVariableListIterator(); variable.hasNext();)
			{
				try
				{
					this.addVariableInit(variable.next());
				}
				catch(Exception e)
				{
					;
				}
			}
		}
		
		// If there is information from libraries, copy it into this scope
		// Only copy subprograms, constants, and types
		if(lib != null)
		{
			// Copy over sub-scope information from lib
			for(Iterator<VHDLScope> scope = lib.getScopeListIterator(); scope.hasNext();)
			{
				VHDLScope temp = scope.next();
				
				// Only add subprogram scopes
				if(!(temp instanceof VHDLSubprogramScope))
				{
					continue;
				}
				
				try
				{
					this.addScopeInit(temp);
				}
				catch(Exception e)
				{
					;
				}
			}
			// Copy over constant and type information from libraries
			for(Iterator<VHDLConstant> constant = lib.getConstantListIterator(); constant.hasNext();)
			{
				try
				{
					this.addConstant(constant.next());
				}
				catch(Exception e)
				{
					;
				}
			}
			for(Iterator<VHDLType> type = lib.getTypeListIterator(); type.hasNext();)
			{
				try
				{
					this.addType(type.next());
				}
				catch(Exception e)
				{
					;
				}
			}
		}
		
		// If no parent or libraries, initialize the type list with default types
		if(parent == null && lib == null)
		{
			initTypeList();
		}
	}
	
	// Add predefined VHDL types to the type list
	private void initTypeList()
	{
	  	ArrayList<String> bits = new ArrayList<String>(2);
	  	bits.add("'0'");
	  	bits.add("'1'");
	  	
	  	java.util.ArrayList<String> bools = new java.util.ArrayList<String>(2);
	  	bools.add("false");
	  	bools.add("true");
	  	
	  	ArrayList<String> characters = new ArrayList<String>(256);
	  	for(int index = 0; index < 256; ++index)
	  	{
	  		if(("" + (char)index).length() == 1)
	  		{
	  			characters.add("'" + (char)index + "'");
	  		}
	  		else
	  		{
	  			characters.add("" + (char)index);
	  		}
	  	}
	  	
	  	ArrayList<String> sevLevels = new ArrayList<String>(4);
	  	sevLevels.add("note");
	  	sevLevels.add("warning");
	  	sevLevels.add("error");
	  	sevLevels.add("failure");
	  	
		typeList.put("CHARACTER",			new VHDLEnumerationType("CHARACTER", characters));
	  	typeList.put("BIT",					new VHDLEnumerationType("BIT", bits));
	  	typeList.put("STD_ULOGIC",			new VHDLEnumerationType("STD_ULOGIC", bits));
	  	typeList.put("BOOLEAN",				new VHDLEnumerationType("BOOLEAN", bools));
	  	typeList.put("STD_LOGIC",			new VHDLEnumerationType("STD_LOGIC", (VHDLEnumerationType)typeList.get("STD_ULOGIC")));
	  	typeList.put("INTEGER",				new VHDLIntegerType("INTEGER", Integer.MIN_VALUE, Integer.MAX_VALUE));
	  	typeList.put("NATURAL",				new VHDLIntegerType("NATURAL", (VHDLIntegerType)typeList.get("INTEGER"), 0, Integer.MAX_VALUE));
	  	typeList.put("POSITIVE",			new VHDLIntegerType("POSITIVE", (VHDLIntegerType)typeList.get("INTEGER"), 1, Integer.MAX_VALUE));
	  	typeList.put("STD_ULOGIC_VECTOR",	new VHDLArrayType("STD_ULOGIC_VECTOR", typeList.get("STD_ULOGIC")));
	  	typeList.put("STD_LOGIC_VECTOR",	new VHDLArrayType("STD_LOGIC_VECTOR", typeList.get("STD_LOGIC")));
	  	typeList.put("STRING",				new VHDLArrayType("STRING", typeList.get("CHARACTER")));
	  	typeList.put("BIT_VECTOR",			new VHDLArrayType("BIT_VECTOR", typeList.get("BIT")));
	  	typeList.put("SIGNED",				new VHDLArrayType("SIGNED", typeList.get("BIT")));
	  	typeList.put("UNSIGNED",			new VHDLArrayType("UNSIGNED", typeList.get("BIT")));
	  	typeList.put("SEVERITY_LEVEL",		new VHDLEnumerationType("SEVERITY_LEVEL", sevLevels));
	}
	
	// Getters for generic scope information
	public final String getName()
	{
		return name;
	}
	public final String getFilename()
	{
		return filename;
	}
	public final int getLineNumber()
	{
		return lineNumber;
	}
	
	// Query methods that report the type of scope this is
	// Subclasses override the appropriate method to return true for their type
	public boolean isEntity()
	{
		return false;
	}
	public boolean isArchitecture()
	{
		return false;
	}
	public boolean isPackage()
	{
		return false;
	}
	public boolean isSubprogram()
	{
		return false;
	}
	public boolean isProcess()
	{
		return false;
	}
	public boolean isLibrary()
	{
		return false;
	}
	public boolean isUniversal()
	{
		return false;
	}
	
	// Methods that query and manipulate the scope list
	public int getNumScopes()
	{
		return scopeList.size();
	}
	public boolean containsScope(String scopeName)
	{
		return scopeList.containsKey(scopeName);
	}
	private void addScopeInit(VHDLScope toAdd)
	{
		scopeList.put(toAdd.getName(), toAdd);
	}
	// Throws an exception by default
	public void addScope(VHDLScope toAdd)throws Exception
	{
		throw new Exception("Scope " + this + " isn't allowed to have sub-scopes declared in itself");
	}
	public Iterator<VHDLScope> getScopeListIterator()
	{
		return scopeList.values().iterator();
	}
	public VHDLScope getScope(String scopeName)
	{
		return scopeList.get(scopeName);
	}
	
	// Methods that query and edit the constant and type lists
	public int getNumConstants()
	{
		return constantList.size();
	}
	public int getNumTypes()
	{
		return typeList.size();
	}
	public boolean containsConstant(String name)
	{
		return constantList.containsKey(name);
	}
	public boolean containsType(String name)
	{
		return typeList.containsKey(name);
	}
	public void addConstant(VHDLConstant toAdd)throws Exception
	{
		if(constantList.put(toAdd.getName(), toAdd) != null)
		{
			throw new Exception("Constant " + toAdd.getName() + " already defined in scope, can't redefine in scope " + this);
		}
	}
	public void addType(VHDLType toAdd)throws Exception
	{
		if(typeList.put(toAdd.getName(), toAdd) != null)
		{
			throw new Exception("Type " + toAdd.getName() + " already defined in scope, can't redefine in scope " + this);
		}
	}
	public Iterator<VHDLConstant> getConstantListIterator()
	{
		return constantList.values().iterator();
	}
	public Iterator<VHDLType> getTypeListIterator()
	{
		return typeList.values().iterator();
	}
	public VHDLConstant getConstant(String constName)
	{
		return constantList.get(constName);
	}
	public VHDLType getType(String typeName)
	{
		return typeList.get(typeName);
	}

	// Method that that allows for dynamic access to list iterators
	public Iterator<?> getListIteratorFor(VHDLObjectTypes type)
	{
		switch(type)
		{
			case CONSTANT:
				return getConstantListIterator();
			case TYPE:
				return getTypeListIterator();
			case SIGNAL:
				return getSignalListIterator();
			case VARIABLE:
				return getVariableListIterator();
			default:
				return null;
		}
	}
	
	// Methods that query and augment the list of signal objects
	public int getNumSignals()
	{
		return signalList.size();
	}
	public boolean containsSignal(String name)
	{
		return signalList.containsKey(name);
	}
	// Hidden method only used during construction of VHDLScope objects
	// Used to build the initial signal list in scopes that don't allow signal declarations
	private void addSignalInit(VHDLSignal toAdd)
	{
		signalList.put(toAdd.getName(), toAdd);
	}
	// Throws an exception by default
	// Subclasses for scopes that allow signal decls override this method to support that behavior
	public void addSignal(VHDLSignal toAdd)throws Exception
	{
		throw new Exception("Signal " + toAdd.getName() + " can't be declared in this scope: " + this);
	}
	public Iterator<VHDLSignal> getSignalListIterator()
	{
		return signalList.values().iterator();
	}
	public VHDLSignal getSignal(String sigName)
	{
		return signalList.get(sigName);
	}
	
	// Methods that query and augment the list of variable objects
	public int getNumVariables()
	{
		return variableList.size();
	}
	public boolean containsVariable(String name)
	{
		return variableList.containsKey(name);
	}
	// Hidden method only used during construction of VHDLScope objects
	// Used to build the initial variable list in scopes that don't allow variable declarations
	private void addVariableInit(VHDLVariable toAdd)
	{
		variableList.put(toAdd.getName(), toAdd);
	}
	// Throws an exception by default
	// Subclasses for scopes that allow variable decls override this method to support that behavior
	public void addVariable(VHDLVariable toAdd)throws Exception
	{
		throw new Exception("Variable " + toAdd.getName() + " can't be declared in this scope: " + this);
	}
	public Iterator<VHDLVariable> getVariableListIterator()
	{
		return variableList.values().iterator();
	}
	public VHDLVariable getVariable(String varName)
	{
		return variableList.get(varName);
	}
	
	// Methods that searches all lists, looking for a name in scope
	public boolean isNameInScope(String name)
	{
		return containsConstant(name) || containsType(name) || containsSignal(name) || containsVariable(name) || containsScope(name);
	}
	
	// Overrides the equals method to aide in comparing scopes
	@Override
	public boolean equals(Object x)
	{
		if(!(x instanceof VHDLScope))
		{
			return false;
		}
		
		if(((VHDLScope)x).getName().equals(name))
		{
			return true;
		}
		return false;
	}
	
	// Methods used to get string representations of desired aspects in a desired format for this scope
	///////////////////////////////////////////////////////////////////////////////////////////////////
	
	// Overrides how this object is automatically converted to a string
	@Override
	public String toString()
	{
		return "Scope " + name + " in file " + filename + " starts on line " + lineNumber;
	}

	// Returns a string with the contents of the lists contained in the this scope
	public String scopeContentsToString()
	{
		StringBuffer toRet = new StringBuffer(toString() + "\n");
		
		toRet.append("\nCONSTANTS:" + "\n");
		for(Iterator<VHDLConstant> constants = getConstantListIterator(); constants.hasNext(); )
		{
			toRet.append("\t" + constants.next() + "\n");
		}
		  
		toRet.append("\nTYPES:" + "\n");
		for(Iterator<VHDLType> types = getTypeListIterator(); types.hasNext(); )
		{
			VHDLType temp = types.next();
			  
			// Don't print character types as bad things happen to text output
			if(!temp.getName().equalsIgnoreCase("CHARACTER"))
			{
				toRet.append(temp.typeToString() + "\n");
			}
		}
		  
		toRet.append("\nSIGNALS:" + "\n");
		for(Iterator<VHDLSignal> signals = getSignalListIterator(); signals.hasNext(); )
		{
			toRet.append("\t" + signals.next() + "\n");
		}
		
		return toRet.toString();
	}
	
	// Returns a string of the desired objects available in this scope and those declared in the sub-scopes of this scope
	// Pretty-print format
	public String constantsToStringPretty()
	{
		return objectsToStringPretty(VHDLObjectTypes.CONSTANT);
	}
	public String typesToStringPretty()
	{
		return objectsToStringPretty(VHDLObjectTypes.TYPE);
	}
	public String signalsToStringPretty()
	{
		return objectsToStringPretty(VHDLObjectTypes.SIGNAL);
	}
	public String variablesToStringPretty()
	{
		return objectsToStringPretty(VHDLObjectTypes.VARIABLE);
	}
	
	// Template utility method used for pretty printing objects in this scope
	private String objectsToStringPretty(VHDLObjectTypes objType)
	{
		StringBuffer toRet = new StringBuffer("");
		
		for(Iterator<?> objects = getListIteratorFor(objType); objects.hasNext(); )
		{
			toRet.append(getName() + ": " + objects.next() + "\n");
		}
		  
		// Also print the objects for each sub-scope
		for(Iterator<VHDLScope> scopes = getScopeListIterator(); scopes.hasNext(); )
		{
			VHDLScope scope = scopes.next();
			
			for(Iterator<?> objects = scope.getListIteratorFor(objType); objects.hasNext(); )
			{
				Object temp = objects.next();
				
				// Only add object declared in this sub-scope
				if(isNameInScope(temp.toString().split(":")[0].trim()))
				{
					continue;
				}
				toRet.append(getName() + "/" + scope.getName() + ": " + temp + "\n");
			}
		}
		
		return toRet.toString();
	}

	// Returns a string of the desired types available in this scope and those declared in the sub-scopes of this scope
	// Formatted for input into the Dataflow analysis program
	public String typesToStringAnalysis()
	{
		StringBuffer toRet = new StringBuffer("");
		
		for(Iterator<VHDLType> type = getTypeListIterator(); type.hasNext(); )
		{
			VHDLType temp = type.next();
			 
			toRet.append(temp.getName() + "&" + temp.isCompositeType() + "&" + temp.isArrayType() + "\n");
			  
			// If record type, print field names and type
			if(temp.isRecordType())
			{
				for(int fieldNum = 0; fieldNum < ((VHDLRecordType)temp).numFields(); ++fieldNum)
				{
					VHDLTypeField field = ((VHDLRecordType)temp).getFieldAt(fieldNum);
					  
					toRet.append(field.getName() + "&" + field.getType().getName() + "\n");
				}
			}
			// If constrained array, print the bounds
			else if(temp.isArrayType())
			{
				toRet.append(((VHDLArrayType)temp).getLeftIndex() + "&" + ((VHDLArrayType)temp).getRightIndex() + "&" + ((VHDLArrayType)temp).getMemberType().getName() + "\n");
			}
		}
		
		return toRet.toString();
	}
	
	// Returns a string of the desired objects available in this scope and those declared in the sub-scopes of this scope
	// Formatted for input into the Dataflow analysis program
	public String objectsToStringAnalysis()
	{
		StringBuffer toRet = new StringBuffer("");
		
		// Constants
		for(Iterator<VHDLConstant> consts = getConstantListIterator(); consts.hasNext(); )
		{
			VHDLConstant temp = consts.next();
			toRet.append(temp.getName() + "&" + temp.getType().getName() + "\n");
		}
		  
		// Signals
		for(Iterator<VHDLSignal> signals = getSignalListIterator(); signals.hasNext(); )
		{
			VHDLSignal temp = signals.next();
			toRet.append(temp.getName() + "&" + temp.getType().getName() + "\n");
		}
		
		// Variables
		for(Iterator<VHDLVariable> vars = getVariableListIterator(); vars.hasNext(); )
		{
			VHDLVariable temp = vars.next();
			toRet.append(getName() + "/" + temp.getName() + "&" + temp.getType().getName() + "\n");
		}
		
		// Go into each sub-scope and print variable information
		for(Iterator<VHDLScope> scopes = getScopeListIterator(); scopes.hasNext(); )
		{
			VHDLScope temp = scopes.next();
			  
			// Only need to output process information since Dataflow Analyzer program doesn't look into subprogram scopes
			if(!temp.isProcess())
			{
				continue;
			}
			  
			// Constants
			for(Iterator<VHDLConstant> consts = temp.getConstantListIterator(); consts.hasNext(); )
			{
				VHDLConstant tempC = consts.next();
				  
				// Don't print inherited constants
				if(containsConstant(tempC.getName()))
				{
					continue;
				}
				  
				toRet.append(temp.getName() + "/" + tempC.getName() + "&" + tempC.getType().getName() + "\n");
			}
			  
			// Print variable name prepended with process name
			for(Iterator<VHDLVariable> vars = temp.getVariableListIterator(); vars.hasNext(); )
			{
				VHDLVariable tempV = vars.next();
				toRet.append(temp.getName() + "/" + tempV.getName() + "&" + tempV.getType().getName() + "\n");
			}
		}
		
		return toRet.toString();
	}

	// Returns a string containing target driver pairs for all signals and variables
	public String objectsToStringDrivers()
	{
		LinkedHashMap<String, String> lines = new LinkedHashMap<String, String>();
		
		// Signals
		for(Iterator<VHDLSignal> sigs = getSignalListIterator(); sigs.hasNext(); )
		{
			String[] temp = sigs.next().toStringDrivers().split("\\n");
			
			// Use hash to filter duplicates
			for(int index = 0; index < temp.length; ++index)
			{
				lines.put(temp[index], "");
			}
		}
		
		// Variables
		for(Iterator<VHDLVariable> vars = getVariableListIterator(); vars.hasNext(); )
		{
			String[] temp = vars.next().toStringDrivers().split("\\n");
			
			// Use hash to filter duplicates
			for(int index = 0; index < temp.length; ++index)
			{
				lines.put(temp[index], "");
			}
		}
		
		// Appended the results for all sub-scopes
		for(Iterator<VHDLScope> scopes = getScopeListIterator(); scopes.hasNext(); )
		{
			String[] temp = scopes.next().objectsToStringDrivers().split("\\n");
			
			// Use hash to filter duplicates
			for(int index = 0; index < temp.length; ++index)
			{
				lines.put(temp[index], "");
			}
		}
		
		// Walk the hash to build a string
		StringBuffer toRet = new StringBuffer("");
		for(Iterator<String> line = lines.keySet().iterator(); line.hasNext(); )
		{
			toRet.append(line.next() + "\n");
		}
		
		return toRet.toString();
	}
	
	// Returns a string that can be used in-conjunction with DOT to build a graph that shows the high-level relationships between in-scope and below objects
	// Formatted for use in DOT graph generation
	// No duplicate lines returned
	public String objectsToStringDOT()
	{
		LinkedHashMap<String, String> lines = new LinkedHashMap<String, String>();
		
		// Signals
		for(Iterator<VHDLSignal> sigs = getSignalListIterator(); sigs.hasNext(); )
		{
			String[] temp = sigs.next().toStringDOT().split("\\n");
			
			// Use hash to filter duplicates
			for(int index = 0; index < temp.length; ++index)
			{
				lines.put(temp[index], "");
			}
		}
		
		// Variables
		for(Iterator<VHDLVariable> vars = getVariableListIterator(); vars.hasNext(); )
		{
			String[] temp = vars.next().toStringDOT().split("\\n");
			
			// Use hash to filter duplicates
			for(int index = 0; index < temp.length; ++index)
			{
				lines.put(temp[index], "");
			}
		}
		
		// Appended the results for all sub-scopes
		for(Iterator<VHDLScope> scopes = getScopeListIterator(); scopes.hasNext(); )
		{
			String[] temp = scopes.next().objectsToStringDOT().split("\\n");
			
			// Use hash to filter duplicates
			for(int index = 0; index < temp.length; ++index)
			{
				lines.put(temp[index], "");
			}
		}
		
		// Walk the hash to build a string
		StringBuffer toRet = new StringBuffer("");
		for(Iterator<String> line = lines.keySet().iterator(); line.hasNext(); )
		{
			toRet.append(line.next() + "\n");
		}
		
		return toRet.toString();
	}
}
