import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashMap;

// Need to more carefully handle finding registers, especially evaluating clocked conditions
// 	Handle wait statements
// How does the program handle multiple design units
// Can't delete generics or simulation won't work as higher-level modules try to access non-existent generics
// Incorporate constants into parse tree
//     Replace identifier token
//     Replace subtree
// No support for true multidimensional arrays
// No support for -/+ in exponent part of integer literals

class DataflowPair
{
	String left = "";
	String right = "";
	
	public DataflowPair(String a, String b)
	{
		left = a;
		right = b;
	}
	
	public boolean equals(Object test)
	{
		if(test.getClass() != DataflowPair.class)
		{
			return false;
		}
		
		if(left.equalsIgnoreCase(((DataflowPair)test).left) && right.equalsIgnoreCase(((DataflowPair)test).right))
		{
			return true;
		}
		
		if(right.equalsIgnoreCase(((DataflowPair)test).left) && left.equalsIgnoreCase(((DataflowPair)test).right))
		{
			return true;
		}
		
		return false;
	}
}

// Given two names, possibly with a mix of subfield and range/index constraints
// Go through the names, ignoring whitespace (see DFP code)
// If a name is proper subset of the other name, they will match until the superset strings stops and the subset strings adds extra constraints
//	Requires that the superset string be a prefix to the subset string, possibly except for the final effective constraint
//		This can only happen with range constraints, but they can be anywhere in the constraint string, which causes problems
//	The effective constraint is the combined range constraint result of several consecutive range constraints, this doesn't affect index or subfields
//	The additional constraint(s) are added to the driver constraint (if any) to make it appropriate for the more constrained object
//		The difference in constraints can be appended when the superset string is a complete prefix of the subset string
//		The difference in constraints must be used as an offset then the rest can be appended if the subset constraint merges with the superset string's
// For overlapping constraints, adjust the driver's constraint using the difference between the super and subset strings as an offset
// Then directly append any additional non-merging constraints

public class Vhdl
{
	private static final boolean PRINT_TREE_DEBUG = true;
	private static final boolean DEBUG_UNSTACKING = false;
	private static final boolean DEBUG_DATAFLOW = true;
	private static final boolean DEBUG_VAR_NAMES = false;
	private static final boolean DEBUG_SIG_NAMES = false;
	private static final boolean DEBUG_REGS = false;
	private static final boolean DEBUG_DRIVERS = false;
	private static final int MAX_NAME_LENGTH = 60;
	
	// Handle for the VHDL parser
	private static VhdlParser parser;
	
	// Direct link to the architecture scope we want to inspect
	private static VHDLScope currentScope;
	
	// List of signals in the processed VHDL file
	private static final ArrayList<VHDLSignal> signalList = new ArrayList<VHDLSignal>();
	// List of variables in the processed VHDL file
	private static final ArrayList<VHDLVariable> variableList = new ArrayList<VHDLVariable>();
	// List of object lists to make common iterative ops on both lists easier
	private static final ArrayList<?>[] objectLists = {signalList, variableList};
	
	private static HashMap<String, String> pairList = new HashMap<String, String>();
	private static ArrayList<DataflowPair> dataflowPairs = new ArrayList<DataflowPair>();
	
	private static VHDLType NULL_TYPE = null;
	private static int currentCycle = 0;
	
	public static void main(String args[]) throws Exception
	{	
		System.out.println("VHDL Parser/Elaborater/Beautifier/Writer (c) 2008-2009 by Matthew Hicks");
		System.out.println("Vhdl-AMS JJTree specification code provided by Christoph Grimm's");
		System.out.println("VHDL-Parser (c) 1997, Version 0.1.2");
		
		if(args.length != 1 && args.length != 2) 
		{
			System.out.println("usage:java -classpath path\\to\\classes Vhdl inputFilename.vhd [preLoad.vhd]");
			return;
		}

		// If we need to preload some stuff do it
		File file = new File(args[0]);
		if(args.length == 2)
		{
			VHDLWorkingLibraryScope preStuff;
			
			File preFile = new File(args[1]);

			// Initialize the parser and link to error stream
			VhdlParser preParser = new VhdlParser(new java.io.FileInputStream(preFile), preFile.getName(), preFile.getCanonicalPath().substring(0, preFile.getCanonicalPath().length() - preFile.getName().length()), null);
			ErrorHandler errs = preParser.errs;
			
			try
			{
				System.out.println("Reading from preload file:\t" + args[1] + "...");
				preParser.design_file();
				errs.Summary();
			}
			catch(Exception e)
			{
				System.out.println("error: " + e.getMessage());
				e.printStackTrace();
				throw e;
			}
			
			// Save the constants and types from the preload file
			preStuff = preParser.workingLibraryScope;
			
			// Create the main parser with the preloaded information
			parser = new VhdlParser(new FileInputStream(file), file.getName(), file.getCanonicalPath().substring(0, file.getCanonicalPath().length() - file.getName().length()), preStuff);
		}
		else
		{
			// Initialize the parser class
			parser = new VhdlParser(new FileInputStream(file), file.getName(), file.getCanonicalPath().substring(0, file.getCanonicalPath().length() - file.getName().length()), null);
		}
		
		// Link to error stream and set filename
		ErrorHandler errs = parser.errs;
		
		try
		{
			System.out.println("Reading from file:\t" + file.getName() + "...");
			parser.design_file();
			errs.Summary();
			
			// Link to the list of scopes from the parser
			ArrayList<VHDLScope> scopeList = parser.workingLibraryScope.designUnits;
			
			java.io.ObjectOutputStream objOutFile = new java.io.ObjectOutputStream(new java.io.FileOutputStream("objectOut.txt"));
			objOutFile.writeObject(scopeList);
			objOutFile.close();
			
			// Find the architecture scope we want to work with
			int index = 0;
			do
			{
				currentScope = scopeList.get(index);
				++index;
			} while(!currentScope.isArchitecture() && index < scopeList.size());
			
			// Exit if we didn't find an architecture 
			if(index == scopeList.size() && !currentScope.isArchitecture())
			{
				System.err.println("ERROR: Cannot find any architecture declarations");
				System.exit(-1);
			}
			
			// In the current scope, print all types (besides character)
			// Formatted for the Dataflow Analyzer program
			System.out.println("\nPrinting type information...\n" + currentScope.typesToStringAnalysis());
			
			// In the current and all sub  scopes print all object names (signals and variables)
			// Formatted for the Dataflow Analyzer program
			System.out.println("\nPrinting object information...\n" + currentScope.objectsToStringAnalysis());
			
			System.out.println("Replacing implicit nulls with explicit ones...");
			replaceImplicitNulls(parser.jjtree.rootNode());
			
			System.out.println("Exanding stacked declarations so there is only one per line...");
			expandVariableDeclaration(parser.jjtree.rootNode());

			// Using the lists from the parser, generate signal and variable lists containing all wholey targetable names
			System.out.println("\nCopying object information from scope to local list...");
			generateTargetableNamesLists();
			
			// Find and label the signals and variables that require clock controlled storage
			System.out.println("\nFinding objects that are memory elements...");
			findRegisterNames(parser.jjtree.rootNode());
			
			new VhdlWriter();
			System.out.println("\nFinding all possible signal and variable drivers...");
			findDrivers(parser.jjtree.rootNode());
			objectListClosure();
			
			// In the current and all sub  scopes print all object names (signals and variables) paired with their non-constant drivers
			// Formatted for the Highlighter program
			System.out.println("\nPrinting driver information...\n" + objectsToStringDrivers());
			
			System.out.println("\nPrinting aiSee3 information...\n" + objectsToStringaiSee3());
			
			System.out.println("\nPrinting dataflow pairs...");
			findDataflowPairs();
			removeDuplicatePairs();
			printDataflowPairs();
		
//			System.out.println("Printing the CST to CSTResults.txt");
//			printTree(parser.jjtree.rootNode(), "CSTResults.txt");
			
			/*
			// Initialize the VHDL file writer class
			new VhdlWriter("result.vhd", constants);
//			new VhdlWriter("result.vhd");
			// Write unmodified file out
			VhdlWriter.start(parser.jjtree.rootNode());

			VhdlWriter.stopWriter();
			*/
		}
		catch(Exception e)
		{
			System.out.println("error: ");
			e.printStackTrace();
			throw e;
		}
	}
	
  // Generates signal and variable lists for an architecture scope that consists of strings representing all possible assignment
  // target when just considering subfield addressing above and beyond whole object assignment
  private static void generateTargetableNamesLists()
  {
	  // For every signal in scope
	  for(Iterator<VHDLSignal> sigNames = currentScope.getSignalListIterator(); sigNames.hasNext(); )
	  {
		  signalList.add(sigNames.next());
		  /*
		  // For each way of addressing any whole part of this signal, add it to the signal list as its own entity
		  for(Iterator<Tuple> addresses = sigNames.next().getListOfAllAddressableObjectNames().iterator(); addresses.hasNext(); )
		  {
			  Tuple temp = addresses.next();
			  signalList.add(new VHDLSignal(temp.path, temp.type));
		  }*/
	  }
	  
	  // For every variable in any sub-scope
	  for(Iterator<VHDLScope> scopes = currentScope.getScopeListIterator(); scopes.hasNext(); )
	  {
		  VHDLScope scope = scopes.next();
		  
		  for(Iterator<VHDLVariable> varNames = scope.getVariableListIterator(); varNames.hasNext(); )
		  {
			  VHDLVariable temp = varNames.next();
			  
			  variableList.add(new VHDLVariable(scope.getName() + "/" + temp.getName(), temp.getType()));
			  /*
			  // For each way of addressing any whole part of this signal, add it to the signal list as its own entity
			  for(Iterator<Tuple> addresses = varNames.next().getListOfAllAddressableObjectNames().iterator(); addresses.hasNext(); )
			  {
				  Tuple temp = addresses.next();
				  variableList.add(new VHDLVariable(scope.getName() + "/" + temp.path, temp.type));
			  }
			  */
		  }
	  }
  }
  
  // Write padding spaces to the output file
  private static void printPaddingSpaces(BufferedWriter toFile, int indent)throws IOException
  {
	  final String tab = "    ";
	  char tabs[] = new char[indent*tab.length()];
	  
	  for(int counter = (tabs.length - 1); counter >= 0; --counter)
	  {
		  tabs[counter] = ' ';
	  }
	  toFile.write(tabs);
  }
	
  // Functions that print the CST tree to an output file
  private static void printTree(Node root, String filename)
  {
	  try
	  {
		  BufferedWriter toFile = new BufferedWriter(new FileWriter(filename));
	  
		  printTreeRecurse(root, 0, toFile);
		  
		  toFile.close();
	  }
	  catch(IOException ioe)
	  {
		  System.err.println("ERROR: Problem writing CST tree to " + filename);
	  }
  }
  
  private static void printTreeRecurse(Node root, int indent, BufferedWriter toFile)throws IOException
  {
	  // Write the name of the aspect of the language out to the file
	  printPaddingSpaces(toFile, indent);
	  toFile.write(root.toString());
	  toFile.newLine();
	  
	  // Handle terminal nodes
	  if(root.jjtGetNumChildren() == 0)
	  {
		  switch(root.getId())
		  {
		  	// Special cases
		  	case VhdlParserTreeConstants.JJTACTUAL_DESIGNATOR:
		  	case VhdlParserTreeConstants.JJTENTITY_ASPECT:
		  		printPaddingSpaces(toFile, indent+1);
		  		toFile.write("OPEN");
		  		toFile.newLine();
		  		break;
		  	case VhdlParserTreeConstants.JJTCHOICE:
		  		printPaddingSpaces(toFile, indent+1);
		  		toFile.write("OTHERS");
		  		toFile.newLine();
		  		break;
		  	case VhdlParserTreeConstants.JJTEXIT_STATEMENT:
		  		printPaddingSpaces(toFile, indent+1);
		  		toFile.write("EXIT");
		  		toFile.newLine();
		  		break;
		  	case VhdlParserTreeConstants.JJTOPTIONS_:
		  		if(((SimpleNode)root).first_token.toString().compareToIgnoreCase("GUARDED") != 0)
		  		{
		  			break;
		  		}
		  		printPaddingSpaces(toFile, indent+1);
		  		toFile.write("GUARDED");
		  		toFile.newLine();
		  		break;
		  	case VhdlParserTreeConstants.JJTRETURN_STATEMENT:
		  		printPaddingSpaces(toFile, indent+1);
		  		toFile.write("RETURN");
		  		toFile.newLine();
		  		break;
		  	case VhdlParserTreeConstants.JJTWAIT_STATEMENT:
		  		printPaddingSpaces(toFile, indent+1);
		  		toFile.write("WAIT");
		  		toFile.newLine();
		  		break;
		  	case VhdlParserTreeConstants.JJTWAVEFORM:
		  		printPaddingSpaces(toFile, indent+1);
		  		toFile.write("UNAFFECTED");
		  		toFile.newLine();
		  		break;
		  	case VhdlParserTreeConstants.JJTWAVEFORM_ELEMENT:
		  		printPaddingSpaces(toFile, indent+1);
		  		toFile.write("NULL");
		  		toFile.newLine();
		  		break;
		  	case VhdlParserTreeConstants.JJTATTRIBUTE_SIMPLE_NAME:
		  		printPaddingSpaces(toFile, indent+1);
		  		toFile.write("RANGE");
		  		toFile.newLine();
		  		break;
		  	// Print the token associated with the terminal node
		  	case VhdlParserTreeConstants.JJTABSTRACT_LITERAL:
		  	case VhdlParserTreeConstants.JJTADDING_OPERATOR:
		  	case VhdlParserTreeConstants.JJTDIRECTION:
		  	case VhdlParserTreeConstants.JJTENTITY_CLASS:
		  	case VhdlParserTreeConstants.JJTENTITY_NAME_LIST:
		  	case VhdlParserTreeConstants.JJTENTITY_TAG:
		  	case VhdlParserTreeConstants.JJTENUMERATION_LITERAL:
		  	case VhdlParserTreeConstants.JJTIDENTIFIER:
		  	case VhdlParserTreeConstants.JJTINSTANTIATION_LIST:
		  	case VhdlParserTreeConstants.JJTLITERAL:
		  	case VhdlParserTreeConstants.JJTLOGICAL_OPERATOR:
		  	case VhdlParserTreeConstants.JJTMISCELLANEOUS_OPERATOR:
		  	case VhdlParserTreeConstants.JJTMODE:
		  	case VhdlParserTreeConstants.JJTMULTIPLYING_OPERATOR:
		  	case VhdlParserTreeConstants.JJTNEXT_STATEMENT:
		  	case VhdlParserTreeConstants.JJTNULL_STATEMENT:
		  	case VhdlParserTreeConstants.JJTOPERATOR_SYMBOL:
		  	case VhdlParserTreeConstants.JJTRELATIONAL_OPERATOR:
		  	case VhdlParserTreeConstants.JJTSHIFT_OPERATOR:
		  	case VhdlParserTreeConstants.JJTSIGN:
		  	case VhdlParserTreeConstants.JJTSIGNAL_KIND:
		  	case VhdlParserTreeConstants.JJTSIGNAL_LIST:
		  	case VhdlParserTreeConstants.JJTSUBPROGRAM_KIND:
		  	case VhdlParserTreeConstants.JJTSUFFIX:
		  		printPaddingSpaces(toFile, indent+1);
		  		toFile.write("" + ((SimpleNode)root).first_token);
		  		toFile.newLine();
		  		break;
		  	// These may be empty, so print nothing
		  	case VhdlParserTreeConstants.JJTARCHITECTURE_DECLARATIVE_PART:
		  	case VhdlParserTreeConstants.JJTARCHITECTURE_STATEMENT_PART:
		  	case VhdlParserTreeConstants.JJTBINDING_INDICATION:
		  	case VhdlParserTreeConstants.JJTBLOCK_DECLARATIVE_PART:
		  	case VhdlParserTreeConstants.JJTBLOCK_STATEMENT_PART:
		  	case VhdlParserTreeConstants.JJTCONFIGURATION_DECLARATIVE_PART:
		  	case VhdlParserTreeConstants.JJTCONTEXT_CLAUSE:
		  	case VhdlParserTreeConstants.JJTENTITY_DECLARATIVE_PART:
		  	case VhdlParserTreeConstants.JJTENTITY_HEADER:
		  	case VhdlParserTreeConstants.JJTPACKAGE_BODY_DECLARATIVE_PART:
		  	case VhdlParserTreeConstants.JJTPACKAGE_DECLARATIVE_PART:
		  	case VhdlParserTreeConstants.JJTPROCESS_DECLARATIVE_PART:
		  	case VhdlParserTreeConstants.JJTPROCESS_STATEMENT_PART:
		  	case VhdlParserTreeConstants.JJTSEQUENCE_OF_STATEMENTS:
		  	case VhdlParserTreeConstants.JJTSUBPROGRAM_DECLARATIVE_PART:
		  	case VhdlParserTreeConstants.JJTSUBPROGRAM_STATEMENT_PART:
			case VhdlParserTreeConstants.JJTCONSUME_LEFT_PAREN:
			case VhdlParserTreeConstants.JJTCONSUME_RIGHT_PAREN:
		  		break;
			default:
		  		if(PRINT_TREE_DEBUG)
		  		{
		  			System.err.println("DEBUG: Leaf node with no handler: " + root.toString());
		  		}
		  }
	  }
	  else
	  {
		  for(int counter = 0; counter < root.jjtGetNumChildren(); ++counter)
		  {
			  printTreeRecurse(root.jjtGetChild(counter), indent+1, toFile);
		  }
	  }
  }
  
  // Recursive function that searches the AST for implicit NULL statement and replaces them with explicit NULL statements
  private static void replaceImplicitNulls(Node currentTreeNode)
  {
	  //  Null statements can only occur in sequential statements
	  //  Sequential statments can occur 0 or more times in a sequence of statements, a subprogram statement part, or a process statement part
	  //  Lack of a sequential statment implies a null statement
	  if(currentTreeNode.jjtGetNumChildren() == 0)
	  {
		  if(currentTreeNode.getId() == VhdlParserTreeConstants.JJTSEQUENCE_OF_STATEMENTS || currentTreeNode.getId() == VhdlParserTreeConstants.JJTSUBPROGRAM_STATEMENT_PART || currentTreeNode.getId() == VhdlParserTreeConstants.JJTPROCESS_STATEMENT_PART) 
		  {
			  // Create a sequential statement child node
			  currentTreeNode.jjtAddChild(new ASTsequential_statement(VhdlParserTreeConstants.JJTSEQUENTIAL_STATEMENT), 0);
			  ((SimpleNode)currentTreeNode.jjtGetChild(0)).first_token = new Token(VhdlParserConstants.NULL, "null");
			  ((SimpleNode)currentTreeNode.jjtGetChild(0)).last_token = ((SimpleNode)currentTreeNode.jjtGetChild(0)).first_token;

			  // Create a null statement child to the sequential statement
			  currentTreeNode.jjtGetChild(0).jjtAddChild(new ASTnull_statement(VhdlParserTreeConstants.JJTNULL_STATEMENT), 0);
			  ((SimpleNode)currentTreeNode.jjtGetChild(0).jjtGetChild(0)).first_token = new Token(VhdlParserConstants.NULL, "null");
			  ((SimpleNode)currentTreeNode.jjtGetChild(0).jjtGetChild(0)).last_token = ((SimpleNode)currentTreeNode.jjtGetChild(0)).first_token;
		  }
	  }
	  // If not, walk the child subtrees
	  else
	  {
		  for(int counter = 0; counter < currentTreeNode.jjtGetNumChildren(); ++ counter)
		  {
			  replaceImplicitNulls(currentTreeNode.jjtGetChild(counter));
		  }
	  }
  }
 
  // Recursive function that searches the AST for cases of stacked variable declaration and breaks the subtree into a declaration per variable name
  // Done by copying the parent of the given identifier_list node, one for each variable, and adding (directly after the original) the copied subtrees as siblings to the original parent node in the grandparent
  private static void expandVariableDeclaration(Node currentTreeNode)
  {
	  // If an identifier list has multiple identifiers
	  // Note identifier list is always child 0 in parent
	  if(currentTreeNode.getId() == VhdlParserTreeConstants.JJTIDENTIFIER_LIST && currentTreeNode.jjtGetNumChildren() > 1)
	  {
		  int indexOfParent;
			
		  switch(currentTreeNode.jjtGetParent().getId())
		  {
		  		// Just require copying the grand parent when parent of identifier list is:
		  		case VhdlParserTreeConstants.JJTCONSTANT_DECLARATION:
		  		case VhdlParserTreeConstants.JJTSIGNAL_DECLARATION:
		  		case VhdlParserTreeConstants.JJTVARIABLE_DECLARATION:
		  			// Find the and check index of the 2nd parent node in its parent's list of children
		  			if((indexOfParent = ((SimpleNode)currentTreeNode.jjtGetParent().jjtGetParent()).jjtIndexInParent()) < 0)
		  			{
		  				System.err.println("ERROR: Can't find correct child index of parent(2) or identifier_list");
		  				return; 
		  			}

		  			// For each identifier other than the first
		  			for(int child = currentTreeNode.jjtGetNumChildren() - 1; child > 0; --child)
		  			{
		  				// Copy the 3rd parent node
		  				SimpleNode tempNode = ((SimpleNode)currentTreeNode.jjtGetParent().jjtGetParent()).clone();
		  				// Quick reference for the clone of the current node
		  				SimpleNode tempChild = ((SimpleNode)tempNode.jjtGetChild(((SimpleNode)currentTreeNode).jjtGetParent().jjtIndexInParent()).jjtGetChild(((SimpleNode)currentTreeNode).jjtIndexInParent()));

		  				// Remove other identifier subtrees in the given identifier_list subtree
		  				tempChild.jjtAddChild(((SimpleNode)currentTreeNode.jjtGetChild(child)).clone(), 0);
		  				while(tempChild.jjtGetNumChildren() > 1)
		  				{
		  					tempChild.jjtRemoveChild(1);
		  				}
				  
		  				// Add as direct sibling to original node, if not child 0
		  				((SimpleNode)tempNode.jjtGetParent()).jjtInsertChildAt(tempNode, indexOfParent+1);
		  			}
	
		  			// Remove other identifier subtrees in child 0 (this node)
		  			while(currentTreeNode.jjtGetNumChildren() > 1)
		  			{
		  				((SimpleNode)currentTreeNode).jjtRemoveChild(1);
		  			}
		  			break;
		  		// Just require copying the parent when parent of identifier list is: constant_declaration()
		  		case VhdlParserTreeConstants.JJTELEMENT_DECLARATION:
		  		case VhdlParserTreeConstants.JJTINTERFACE_VARIABLE_DECLARATION:
		  		case VhdlParserTreeConstants.JJTINTERFACE_SIGNAL_DECLARATION:
		  		case VhdlParserTreeConstants.JJTINTERFACE_CONSTANT_DECLARATION:
		  			// Find the and check index of the 2nd parent node in its parent's list of children
		  			if((indexOfParent = ((SimpleNode)currentTreeNode.jjtGetParent()).jjtIndexInParent()) < 0)
		  			{
		  				System.err.println("ERROR: Can't find correct child index of parent(2) or identifier_list");
		  				return; 
		  			}

		  			// For each identifier other than the first
		  			for(int child = currentTreeNode.jjtGetNumChildren() - 1; child > 0; --child)
		  			{
		  				// Copy the 3rd parent node
		  				SimpleNode tempNode = ((SimpleNode)currentTreeNode.jjtGetParent()).clone();
		  				// Quick reference for the clone of the current node
		  				SimpleNode tempChild = ((SimpleNode)tempNode.jjtGetChild(((SimpleNode)currentTreeNode).jjtIndexInParent()));

		  				// Remove other identifier subtrees in the given identifier_list subtree
		  				tempChild.jjtAddChild(((SimpleNode)currentTreeNode.jjtGetChild(child)).clone(), 0);
		  				while(tempChild.jjtGetNumChildren() > 1)
		  				{
		  					tempChild.jjtRemoveChild(1);
		  				}
				  
		  				// Add as direct sibling to original node, if not child 0
		  				((SimpleNode)tempNode.jjtGetParent()).jjtInsertChildAt(tempNode, indexOfParent+1);
		  			}
	
		  			// Remove other identifier subtrees in child 0 (this node)
		  			while(currentTreeNode.jjtGetNumChildren() > 1)
		  			{
		  				((SimpleNode)currentTreeNode).jjtRemoveChild(1);
		  			}
		  			break;
		  		default:
		  			if(DEBUG_UNSTACKING)
		  			{
		  				System.err.println("DEBUG: Identifier list parent type not handled: " + ((SimpleNode)currentTreeNode.jjtGetParent()));
		  			}
		  }
	  }
	  // Otherwise, try for children
	  else
	  {
		  for(int counter = 0; counter < currentTreeNode.jjtGetNumChildren(); ++ counter)
		  {
			  expandVariableDeclaration(currentTreeNode.jjtGetChild(counter));
		  }
	  }
  }

  // Rename variables in a process so each target variable is unique
  // Requires creating declarations and modifying variable assignment targets
  private static void renameProcessVariables(Node currentNode)
  {
	  // Find the targets in blocks that are repeated
	  // Only look inside a process as variables can't appear outside them (ignoring functions and procedures)
	  // For each block of sequential statements
	  //     A variable needs renaming if it is the target of an assignment more than once and more than one of assigned values affect other variable/signal values
	  //     Any number of assignments to a given variable in a sub-block counts as one assignment in the upper block
	  // A block is delineated by a conditional (if/else or case) statement
  }
  
  // Find the type object in the current scope that corresponds to the passed name
  // Searches the current scope and all sub-scopes
  // Returns null if no match found
  private static VHDLType getTypeFromName(String typeName)
  {
	  if(currentScope.getType(typeName.toUpperCase()) != null)
	  {
		  return currentScope.getType(typeName.toUpperCase());
	  }
	  
	  for(Iterator<VHDLScope> scopes = currentScope.getScopeListIterator(); scopes.hasNext(); )
	  {
		  VHDLScope scope = scopes.next();
		  
		  if(scope.getType(typeName.toUpperCase()) != null)
		  {
			  return scope.getType(typeName.toUpperCase());
		  }
	  }
	  
	  return null;
  }
  
  // Find the constant object in the current scope that corresponds to the passed name
  // Searches the current scope and all sub-scopes
  // Returns null if no match found
  private static VHDLConstant getConstantFromName(String name)
  {
	  if(currentScope.getConstant(name.toUpperCase()) != null)
	  {
		  return currentScope.getConstant(name.toUpperCase());
	  }
	  
	  for(Iterator<VHDLScope> scopes = currentScope.getScopeListIterator(); scopes.hasNext(); )
	  {
		  VHDLScope scope = scopes.next();
		  
		  if(scope.getConstant(name.toUpperCase()) != null)
		  {
			  return scope.getConstant(name.toUpperCase());
		  }
	  }
	  
	  return null;
  }
  
  // Find the signal object in the current scope that corresponds to the passed name
  // Searches the current scope and all sub-scopes
  // Returns null if no match found
  private static VHDLSignal getSignalFromName(String name)
  {
	  if(currentScope.getSignal(name.toUpperCase()) != null)
	  {
		  return currentScope.getSignal(name.toUpperCase());
	  }
	  
	  for(Iterator<VHDLScope> scopes = currentScope.getScopeListIterator(); scopes.hasNext(); )
	  {
		  VHDLScope scope = scopes.next();
		  
		  if(scope.getSignal(name.toUpperCase()) != null)
		  {
			  return scope.getSignal(name.toUpperCase());
		  }
	  }
	  
	  return null;
  }
  
  // Find the variable object in the current scope that corresponds to the passed name
  // Searches the current scope and all sub-scopes
  // Returns null if no match found
  private static VHDLVariable getVariableFromName(String name)
  {
	  if(currentScope.getVariable(name.toUpperCase()) != null)
	  {
		  return currentScope.getVariable(name.toUpperCase());
	  }
	  
	  for(Iterator<VHDLScope> scopes = currentScope.getScopeListIterator(); scopes.hasNext(); )
	  {
		  VHDLScope scope = scopes.next();
		  
		  if(scope.getVariable(name.toUpperCase()) != null)
		  {
			  return scope.getVariable(name.toUpperCase());
		  }
	  }
	  
	  return null;
  }

  // Find and update, in the signal or variable list, the objects that will generate registers for a given sub-AST
  // Note that record fields may not have the same storage characteristics
  private static void findRegisterNames(Node currentNode)
  {
	  // Look for a clock controlled condition in an if statement inside a process
	  if(currentNode.getId() == VhdlParserTreeConstants.JJTCONDITION && currentNode.jjtGetParent().getId() == VhdlParserTreeConstants.JJTIF_STATEMENT && currentNode.jjtHasParentOfID(VhdlParserTreeConstants.JJTPROCESS_STATEMENT))
	  {
		  // Check for clock control
		  String condition = VhdlWriter.start(currentNode).trim().toLowerCase();
		  if(condition.indexOf("' event") != -1 || condition.indexOf("rising_edge") != -1 || condition.indexOf("falling_edge") != -1 || condition.indexOf("' stable") != -1)
		  {
			  // A sequence of (assignment) statements directly follows the condition in the condition's parent's child list
			  // Print all assignment targets in those statements
			  updateLHSSignalVariableTypes(currentNode.jjtGetParent().jjtGetChild(currentNode.jjtIndexInParent()+1));
		  }
	  }
	  else
	  {
		  // Look for clocked if in all of the child subtrees
		  for(int child = 0; child < currentNode.jjtGetNumChildren(); ++child)
		  {
			  // Don't need to waste time in functions, procedures, or process headers
			  switch(currentNode.jjtGetChild(child).getId())
			  {
			  		case VhdlParserTreeConstants.JJTPROCESS_DECLARATIVE_PART:
			  		case VhdlParserTreeConstants.JJTSUBPROGRAM_BODY:
			  			break;
			  		default:
			  			findRegisterNames(currentNode.jjtGetChild(child));
			  }
		  }
	  }
  }
  
  // Update the target signal names of all signal assignment statements in the passed tree
  private static void updateLHSSignalVariableTypes(Node currentNode)
  {
	  // Assignments consist of targets (LHS) followed by expressions/waveforms
	  // We care about targets that are part of signal and variable assignment statements
	  if(currentNode.getId() == VhdlParserTreeConstants.JJTTARGET && (currentNode.jjtGetParent().getId() == VhdlParserTreeConstants.JJTSIGNAL_ASSIGNMENT_STATEMENT || currentNode.jjtGetParent().getId() == VhdlParserTreeConstants.JJTVARIABLE_ASSIGNMENT_STATEMENT))
	  {
		  if(currentNode.jjtGetChild(0).getId() == VhdlParserTreeConstants.JJTNAME)
		  {
			  // A signal assignment statement has the target followed directly by a waveform
			  String name = VhdlWriter.start(currentNode).trim().toUpperCase();
			  
			  // Find and update the signal type in the signal list
			  int index;
			  if((index = signalList.indexOf(new VHDLSignal(name, NULL_TYPE))) != -1)
			  {
				  if(DEBUG_REGS && !signalList.get(index).isRegister())
				  {
					  System.out.println("Setting " + name + " to be a register");
				  }
				  signalList.get(index).setRegister(true);
			  }
			  // If the name is derived from a base name in the list, add it as a registered signal
			  else if((index = signalList.indexOf(new VHDLSignal(VHDLObject.getBaseName(name), NULL_TYPE))) != -1)
			  {
				  addSignalToList(name, true);
				  if(DEBUG_REGS)
				  {
					  System.out.println("Setting " + name + " to be a register, derived from " + VHDLObject.getBaseName(name));
				  }
			  }
			  // Variables need to have the process name/line prefix
			  else
			  {
				  String processLabel = getProcessLabel(currentNode);
				  
				  // Check the variable list
				  if((index = variableList.indexOf(new VHDLVariable(processLabel + "/" + name, NULL_TYPE))) != -1)
				  {
					  if(DEBUG_REGS && !variableList.get(index).isRegister())
					  {
						  System.out.println("Setting " + name + " to be a register");
					  }
					  variableList.get(index).setRegister(true);
				  }
				  // If the name is derived from a base name in the list, add it as a registered variable
				  else if((index = variableList.indexOf(new VHDLVariable(VHDLObject.getBaseName(processLabel + "/" + name), NULL_TYPE))) != -1)
				  {
					  addVariableToList(processLabel + "/" + name, true);
					  if(DEBUG_REGS)
					  {
						  System.out.println("Setting " + name + " to be a register, derived from " + VHDLObject.getBaseName(name));
					  }
				  }
				  else
				  {
					  System.err.println("ERROR: Couldn't find object " + VHDLObject.getBaseName(name) + " from " + name + " in the signal or variable list");
				  }
			  }
		  }
		  else if(DEBUG_SIG_NAMES)
		  {
			  System.err.println("ERROR: Left hand side of signal/variable assignment statement isn't a name: " + currentNode.jjtGetChild(0).toString());
		  }
	  }
	  else
	  {
		  // Keep looking for assignment statements
		  for(int child = 0; child < currentNode.jjtGetNumChildren(); ++child)
		  {
			  updateLHSSignalVariableTypes(currentNode.jjtGetChild(child));
		  }
	  }
  }
  
  // Update implicit drivers for record and sliced objects, adding new drivers to signal/variable list if needed
  // This may require several iterations
  private static void objectListClosure()
  {
	  do
	  {
		  addImplicitDrivers();
	  } while(addDriversToList() != 0);
	  
	  updateObjectTypes();
  }
  
  // Go through the signal and variable lists, ensuring that all subfields/slices of a record/array object get the appropriate drivers from their super-fields/slices
  // Need a better way to reconcile overlapping/contained slices and how to propagate their drivers
  // Need to fix for records
  @SuppressWarnings("unchecked")
  private static void addImplicitDrivers()
  {
	  for(int list = 0; list < objectLists.length; ++list)
	  {
		  ArrayList<? extends VHDLObject> objectList = (ArrayList<? extends VHDLObject>)objectLists[list];
	  
		  for(int i = 0; i < objectList.size(); ++i)
		  {
			  VHDLObject child = objectList.get(i);
		  
			  // Look for all possible less constrained versions of the child object
			  for(int j = 0; j < objectList.size(); ++j)
			  {
				  VHDLObject parent = objectList.get(j);
			  
				  // If the child object is a more constrained version of the parent object copy the properly constrained drivers to the child
				  if(i != j && child.isMoreConstrainedVersionOf(parent))
				  {
					  if(DEBUG_SIG_NAMES || DEBUG_VAR_NAMES)
					  {
						  System.out.println("Prefix: Child: " + child.getName() + "\tParent: " + parent.getName());
					  }
				  
					  // Copy drivers from the parent to the child, adding the correct constraints
					  for(int driver = 0; driver < parent.numDrivers(); ++driver)
					  {
						  String newDriver = parent.getDriver(driver);
						  VHDLType baseDriverType = null;
						  
						  // Check the signal and variable list to find base type for driver
						  // If not in either list it must be a constant
						  int indexInList;
						  if((indexInList = signalList.indexOf(new VHDLSignal(VHDLObject.getBaseName(newDriver), NULL_TYPE))) != -1)
						  {
							  baseDriverType = signalList.get(indexInList).getType();
						  }
						  else if((indexInList = variableList.indexOf(new VHDLVariable(VHDLObject.getBaseName(newDriver), NULL_TYPE))) != -1)
						  {
							  baseDriverType = variableList.get(indexInList).getType();
						  }
						  // Filter numbers an Booleans
						  else if(isNumberOrBoolean(newDriver))
						  {
							  continue;
						  }
						  else
						  {
							  exit("Problem while trying to handle driver" + newDriver);
						  }
					  
						  if(child.addAndConstrainDriverFromObject(parent, newDriver, baseDriverType) && DEBUG_DRIVERS)
						  {
							  System.out.println("Adding driver " + child.getDriver(child.numDrivers() - 1) + " to the object " + child.getName() + " derived from " + parent.getName());
						  }
					  }
				  }
			  }
		  }
	  }
  }
  
  // Given two strings that represent range constraints in the form "(int DOWNTO int)" or "(int TO int)"
  // Ignores whitespace between paramter tokens
  // This function returns a string representing the range constraint of second applied to first
  // Direction of returned constraint is same as direction of second constraint
  // i.e. if first = (7 DOWNTO 3) and second = (2 DOWNTO 0), returns (5 DOWNTO 3)
  // i.e. if first = (5 TO 14) and second = (2 TO 6), returns (7 DOWNTO 11)
  private static String mergeRangeConstraints(String first, String second)
  {
	  int firstLeft = Integer.parseInt(findRangeConstraints(first).split("&")[0]);
	  int firstRight = Integer.parseInt(findRangeConstraints(first).split("&")[1]);
	  int secondLeft = Integer.parseInt(findRangeConstraints(second).split("&")[0]);
	  int secondRight = Integer.parseInt(findRangeConstraints(second).split("&")[1]);
	  int retLeft;
	  int retRight;
	  
	  // If the first constraint's direction is DOWNTO
	  if(firstLeft > firstRight)
	  {
		  // Second direction doesn't matter, math turns out the same
		  retLeft = firstRight + secondLeft;
		  retRight = firstRight + secondRight;
	  }
	  else
	  {
		  // Second direction doesn't matter, math turns out the same
		  retLeft = firstLeft + secondLeft;
		  retRight = firstLeft + secondRight;
	  }
	
	  // Build and return the constraint with the proper direction
	  if(retLeft >= retRight)
	  {
		  return "( " + retLeft + " DOWNTO " + retRight + " )";
	  }
	  else
	  {
		  return "( " + retLeft + " TO " + retRight + " )";
	  }
  }
  
  private static String findRangeConstraints(String name)
  {
	  String rangeDownto = "\\([ \\t]*[0-9]+[ \\t]+DOWNTO[ \\t]+[0-9]+[ \\t]*\\)";
	  String rangeTo = "\\([ \\t]*[0-9]+[ \\t]+TO[ \\t]+[0-9]+[ \\t]*\\)";
	  String index = "\\([ \\t]*[0-9]+[ \\t]*\\)";
	  Pattern rangeDowntoPattern = Pattern.compile(rangeDownto);
	  Pattern rangeToPattern = Pattern.compile(rangeTo);
	  Pattern indexPattern = Pattern.compile(index);
	  int leftBound = 0;
	  int rightBound = 0;
	  int indexInto = Integer.MIN_VALUE;
	  boolean atleastOneMatch = false;
	  
	  // Test to see which type of constraint is first in the string, reduce it then look again
	  while(true)
	  {
		  Matcher match;
		  
		  if((match = rangeDowntoPattern.matcher(name)).find())
		  {
			  // Check for conditions we can't handle
			  if(atleastOneMatch && indexInto != Integer.MIN_VALUE)
			  {
				  exit("Can't handle array constraints with multiple forms of index constraint");				  
			  }
			  
			  // Get the constrain minus the open and closing parens
			  String constraint = name.substring(match.start()+1, match.end()-1).trim();
			  
			  // Get the bounds to the left and right of the direction indicator
			  leftBound += Integer.parseInt(constraint.split("DOWNTO")[0].trim());
			  rightBound += Integer.parseInt(constraint.split("DOWNTO")[1].trim());
			  
			  System.out.println("Left Bound: " + leftBound + "\tRight Bound: " + rightBound);
			  name = name.substring(match.end());
			  atleastOneMatch = true;
		  }
		  else if((match = rangeToPattern.matcher(name)).find())
		  {
			  // Check for conditions we can't handle
			  if(atleastOneMatch && indexInto != Integer.MIN_VALUE)
			  {
				  exit("Can't handle array constraints with multiple forms of index constraint");				  
			  }
			  
			  // Get the constrain minus the open and closing parens
			  String constraint = name.substring(match.start()+1, match.end()-1).trim();
			  
			  // Get the bounds to the left and right of the direction indicator
			  leftBound += Integer.parseInt(constraint.split("TO")[0].trim());
			  rightBound += Integer.parseInt(constraint.split("TO")[1].trim());
			  
			  System.out.println("Left Bound: " + leftBound + "\tRight Bound: " + rightBound);
			  name = name.substring(match.end());
			  atleastOneMatch = true;
		  }
		  else if((match = indexPattern.matcher(name)).find())
		  {
			  // Check for conditions we can't handle
			  if(atleastOneMatch)
			  {
				  exit("Can't handle array constraints with multiple forms of index constraint");				  
			  }
			  
			  // Get the constrain minus the open and closing parens
			  indexInto = Integer.parseInt(name.substring(match.start()+1, match.end()-1).trim());
			  
			  System.out.println("Index: " + indexInto);
			  name = name.substring(match.end());
			  atleastOneMatch = true;
		  }
		  else
		  {
			  if(!atleastOneMatch)
			  {
				  if(DEBUG_DRIVERS)
				  {
					  System.out.println("Couldn't find any array constraints in the string: " + name);
				  }
				  return null;
			  }
			  break;
		  }
	  }
	  
	  if(indexInto != Integer.MIN_VALUE)
	  {
		  return "" + indexInto;
	  }
	  else
	  {
		  return "" + leftBound + "&" + rightBound;
	  }
  }
  
  // Add drivers not already in either list to the appropriate list
  @SuppressWarnings("unchecked")
  private static int addDriversToList()
  {
	  int count = 0;
	  
	  for(int list = 0; list < objectLists.length; ++list)
	  {
		  ArrayList<VHDLObject> objectList = (ArrayList<VHDLObject>)objectLists[list];
		  
		  for(int object = 0; object < objectList.size(); ++object)
		  {
			  VHDLObject currentObject = objectList.get(object);
		  
			  for(int driver = 0; driver < currentObject.numDrivers(); ++driver)
			  {
				  String driverName = currentObject.getDriver(driver);
			  
				  // If the driver isn't in either list, see if we need to add it
				  if(!signalList.contains(new VHDLSignal(driverName, NULL_TYPE)) && !variableList.contains(new VHDLVariable(driverName, NULL_TYPE)))
				  {
					  // Filter numbers an Booleans
					  if(isNumberOrBoolean(driverName))
					  {
						  continue;
					  }
				  
					  // Must be a signal or variable (subprograms not allowed from start)
					  // Remove slicing and field information and see which list to add the name to
					  String rootName = VHDLObject.getBaseName(driverName);
					  int indexInList;
				  
					  // Add to the list the prefix belongs to
					  if((indexInList = signalList.indexOf(new VHDLSignal(rootName, NULL_TYPE))) != -1)
					  {
						  addSignalToList(driverName, false);
						  if(DEBUG_SIG_NAMES)
						  {
							  System.out.println("Adding signal " + driverName + ": driver of " + currentObject.getName());
						  }
					  }
					  else if((indexInList = variableList.indexOf(new VHDLVariable(rootName, NULL_TYPE))) != -1)
					  {
						  addVariableToList(driverName, false);
						  if(DEBUG_VAR_NAMES)
						  {
							  System.out.println("Adding variable " + driverName + ": driver of " + currentObject.getName());
						  }
					  }
					  else
					  {
						  if(DEBUG_SIG_NAMES || DEBUG_VAR_NAMES)
						  {
							  System.err.println("ERROR: Couldn't transform driver " + driverName + " into a signal or variable");
						  }
						  continue;
					  }
				  
					  // Increase the number of new objects added to either list
					  // More than 0 requires another iteration to check for further additions
					  ++count;
				  }
			  }
		  }
	  }
	  
	  return count;
  }

  // Go through the signal and variable lists, ensuring that all subfields below a given record subfield have the same memory storage setting
  // Also propagate register type values amongst slices
  // It is allowable for ranges of the same array to have different isRegister settings
  // It is allowable for different subfields of the same record to have different isRegister settings
  @SuppressWarnings("unchecked")
  private static void updateObjectTypes()
  {
	  for(int list = 0; list < objectLists.length; ++list)
	  {
		  ArrayList<VHDLObject> objectList = (ArrayList<VHDLObject>)objectLists[list];
		  
		  for(int parent = 0; parent < objectList.size(); ++parent)
		  {  
			  // If this object is a register
			  // Look for all objects that are more constrained versions of this object and set them to regs. if this object is
			  // This will only update strictly more restrictive versions of the current object, which is what we want
			  if(objectList.get(parent).isRegister())
			  {
				  // Go through the rest of the list and update related signal's type
				  for(int child = 0; child < objectList.size(); ++child)
				  {
					  // If the root names of the signals are the same update the signal's type to register
					  if(parent != child && !objectList.get(child).isRegister() && objectList.get(child).isMoreConstrainedVersionOf(objectList.get(parent)))
					  {
						  objectList.get(child).setRegister(true);
						  if(DEBUG_REGS)
						  {
							  System.out.println("Setting " + objectList.get(child).getName() + " to be a register");
						  }
					  }
				  }
			  }
		  }
	  }
  }
  
  // Pad a VHDL object name with spaces and return the new string
  private static String padName(String name)
  {
	  String padded = name;
	  
	  for(int spaces = (MAX_NAME_LENGTH - name.length()); spaces > 0; --spaces)
	  {
		  padded += " ";
	  }
	  
	  return padded;
  }
  
  // Adds a signal with the appropriate type to the signal list
  private static void addSignalToList(String nameToAdd, boolean isRegister)
  {
	  String baseName = VHDLObject.getBaseName(nameToAdd);
	  int baseIndex = signalList.indexOf(new VHDLSignal(baseName, NULL_TYPE));
	  
	  // Check for errors
	  if(baseIndex == -1)
	  {
		  exit("Can't add signal " + nameToAdd + " to the singal list, base name " + baseName + " doesn't exist");
	  }
	  
	  VHDLType baseType = signalList.get(baseIndex).getType();
	  
	  signalList.add(new VHDLSignal(nameToAdd, VHDLObject.getTypeOf(nameToAdd, baseType), isRegister));
  }
  
  // Adds a variable with the appropriate type to the variable list
  private static void addVariableToList(String nameToAdd, boolean isRegister)
  {
	  String baseName = VHDLObject.getBaseName(nameToAdd);
	  int baseIndex = variableList.indexOf(new VHDLVariable(baseName, NULL_TYPE));
	  
	  // Check for errors
	  if(baseIndex == -1)
	  {
		  exit("Can't add variable " + nameToAdd + " to the variable list, base name " + baseName + " doesn't exist");
	  }
	  
	  VHDLType baseType = variableList.get(baseIndex).getType();
	  
	  variableList.add(new VHDLVariable(nameToAdd, VHDLObject.getTypeOf(nameToAdd, baseType), isRegister));
  }
  
  // Groups all named signals and variables in the design with their possible named drivers
  private static void findDrivers(Node currentNode)
  {
	// LHS of assignments are under the target part of the language
	switch(currentNode.getId())
	{
		case VhdlParserTreeConstants.JJTTARGET:
			String rhs[];
			int indexInList;
			String name = VhdlWriter.start(currentNode).trim().toUpperCase();
			
			
			// Determine the index of the LHS name in the respective list
			switch(currentNode.jjtGetParent().getId())
			{		
				// A conditional signal assignment statement has a parent that contains a bunch of waveforms at child 2 in the parent of the target
				case VhdlParserTreeConstants.JJTCONDITIONAL_SIGNAL_ASSIGNMENT:
				// A selected signal assignment statement has a parent that contains a bunch of waveforms at child 3 in the parent of the target
				case VhdlParserTreeConstants.JJTSELECTED_SIGNAL_ASSIGNMENT:
				// A signal assignment statement has the target followed directly by a waveform
				case VhdlParserTreeConstants.JJTSIGNAL_ASSIGNMENT_STATEMENT:
					// See if this signal is already in the list
					// If not, check it for being a name with slicing and add it to the list
					if((indexInList = signalList.indexOf(new VHDLSignal(name, NULL_TYPE))) == -1)
					{
						if((indexInList = signalList.indexOf(new VHDLSignal(VHDLObject.getBaseName(name), NULL_TYPE))) != -1)
						{
							addSignalToList(name, false);
							indexInList = signalList.size() - 1;
						}
						else
						{
							System.err.println("ERROR: Signal " +  name + " couldn't be added to the signal list");
							return;
						}
					}
					break;
				// A variable assignment statement has a target followed directly by an expression
				case VhdlParserTreeConstants.JJTVARIABLE_ASSIGNMENT_STATEMENT:
					// Ensure that the variable is assigned inside a process
					if(DEBUG_DATAFLOW && !currentNode.jjtHasParentOfID(VhdlParserTreeConstants.JJTPROCESS_STATEMENT))
					{
						System.err.println("ERROR: Variable assignment not inside a process.");
						return;
					}
		  		
					name = getProcessLabel(currentNode) + "/" + name;
					
					// See if this variable is already in the list
					// If not, check it for being a name with slicing and add it to the list
					if((indexInList = variableList.indexOf(new VHDLVariable(name, NULL_TYPE))) == -1)
					{
						if((indexInList = variableList.indexOf(new VHDLVariable(VHDLObject.getBaseName(name), NULL_TYPE))) != -1)
						{
							addVariableToList(name, false);
							indexInList = variableList.size() - 1;
						}
						else
						{
							System.err.println("ERROR: Variable " +  name + " couldn't be added to the variable list");
							return;
						}
					}
					break;
				default:
					if(DEBUG_DATAFLOW)
					{
						System.err.println("ERROR: Unknown assignment type");
					}
					return;
			}
			
			// Parse the RHS to determine the driver objects
			switch(currentNode.jjtGetParent().getId())
			{	
				// A conditional signal assignment statement has a parent that contains a bunch of waveforms at child 2 in the parent of the target
				case VhdlParserTreeConstants.JJTCONDITIONAL_SIGNAL_ASSIGNMENT:
					// Conditional waveform subtree is second child to this nodes parent
					// Print the terms in each wave form
					for(int waveform = 0; waveform < currentNode.jjtGetParent().jjtGetChild(2).jjtGetNumChildren(); ++waveform)
					{
						// Only print waveforms
						if(currentNode.jjtGetParent().jjtGetChild(2).jjtGetChild(waveform).getId() == VhdlParserTreeConstants.JJTWAVEFORM)
						{
							rhs = printTerminalsInRHS(currentNode.jjtGetParent().jjtGetChild(2).jjtGetChild(waveform)).toUpperCase().split("\t");
		  				
							for(int i = 0; i < rhs.length; ++i)
							{
								String rhsParts[] = rhs[i].split(" ");
								
								// Don't add subprograms as drivers
								if(isScopeName(rhsParts[0]) || rhsParts[0].startsWith("CONV_STD_LOGIC_VECTOR"))
								{
									continue;
								}
								
								// See if we need to update one of the object lists with the driver name
								if(signalList.contains(new VHDLSignal(rhs[i], NULL_TYPE)))
								{
									signalList.get(indexInList).addDriver(rhs[i]);
									continue;
								}
								// Is it a number, a boolean, or a record type
								else
								{
									// Filter numbers
									try
									{
										signalList.get(indexInList).addDriver("" + extractPossiblyLongNumber(rhs[i].replace('\'', ' ').replace('\"', ' ').trim()));
										continue;
									}
									catch(Exception e)
									{
										;
									}
									
									// Filter Booleans
									if(rhs[i].compareToIgnoreCase("true") == 0 || rhs[i].compareToIgnoreCase("false") == 0)
									{
										signalList.get(indexInList).addDriver("" + Boolean.parseBoolean(rhs[i]));
										continue;
									}
									
									// Filter constants
									if(currentScope.containsConstant(rhs[i]))
									{
										signalList.get(indexInList).addDriver("" + currentScope.getConstant(rhs[i]).getValue());
										continue;
									}
									
									// Handle slicing
									String prefix = VHDLObject.getBaseName(rhs[i]);
									
									// If the name contains slice info and the none sliced name is in the list add the sliced name to the list and then the driver
									int indexOfDriver;
									if(!prefix.equals(rhs[i]) && (indexOfDriver = signalList.indexOf(new VHDLSignal(prefix, NULL_TYPE))) != -1)
									{
										addSignalToList(rhs[i], false);
										signalList.get(indexInList).addDriver(rhs[i]);
										continue;
									}
								}
								
								System.err.println("WARNING: Couldn't add " + rhs[i] + " as a driver");
							}
						}
					}
					break;
				// A selected signal assignment statement has a parent that contains a bunch of waveforms at child 3 in the parent of the target
				case VhdlParserTreeConstants.JJTSELECTED_SIGNAL_ASSIGNMENT:
					for(int waveform = 0; waveform < currentNode.jjtGetParent().jjtGetChild(3).jjtGetNumChildren(); ++waveform)
					{
						// Only print waveforms
						if(currentNode.jjtGetParent().jjtGetChild(3).jjtGetChild(waveform).getId() == VhdlParserTreeConstants.JJTWAVEFORM)
						{
							rhs = printTerminalsInRHS(currentNode.jjtGetParent().jjtGetChild(3).jjtGetChild(waveform)).toUpperCase().split("\t");
		  				
		  					for(int i = 0; i < rhs.length; ++i)
		  					{
		  						String rhsParts[] = rhs[i].split(" ");
								
								// Don't add subprograms as drivers
								if(isScopeName(rhsParts[0]) || rhsParts[0].startsWith("CONV_STD_LOGIC_VECTOR"))
								{
									continue;
								}
								
								// See if we need to update one of the object lists with the driver name
								if(signalList.contains(new VHDLSignal(rhs[i], NULL_TYPE)))
								{
									signalList.get(indexInList).addDriver(rhs[i]);
									continue;
								}
								// Is it a number, a boolean, or a record type
								else
								{
									// Filter numbers
									try
									{
										signalList.get(indexInList).addDriver("" + extractPossiblyLongNumber(rhs[i].replace('\'', ' ').replace('\"', ' ').trim()));
										continue;
									}
									catch(Exception e)
									{
										;
									}
									
									// Filter Booleans
									if(rhs[i].compareToIgnoreCase("true") == 0 || rhs[i].compareToIgnoreCase("false") == 0)
									{
										signalList.get(indexInList).addDriver("" + Boolean.parseBoolean(rhs[i]));
										continue;
									}
									
									// Filter constants
									if(currentScope.containsConstant(rhs[i]))
									{
										signalList.get(indexInList).addDriver("" + currentScope.getConstant(rhs[i]).getValue());
										continue;
									}
									
									// Handle slicing
									String prefix = VHDLObject.getBaseName(rhs[i]);
									
									// If the name contains slice info and the none sliced name is in the list add the sliced name to the list and then the driver
									int indexOfDriver;
									if(!prefix.equals(rhs[i]) && (indexOfDriver = signalList.indexOf(new VHDLSignal(prefix, NULL_TYPE))) != -1)
									{
										addSignalToList(rhs[i], false);
										signalList.get(indexInList).addDriver(rhs[i]);
										continue;
									}
								}
									
								System.err.println("WARNING: Couldn't add " + rhs[i] + " as a driver");
		  					}
						}
					}
					break;
				// Must make sure that variable drivers have proper prefix for sequential statements
				// A variable assignment statement has a target followed directly by an expression
				case VhdlParserTreeConstants.JJTVARIABLE_ASSIGNMENT_STATEMENT:
					// Print the right hand side
					rhs = printTerminalsInRHS(currentNode.jjtGetParent().jjtGetChild(currentNode.jjtIndexInParent()+1)).toUpperCase().split("\t");

					for(int i = 0; i < rhs.length; ++i)
					{
						String rhsParts[] = rhs[i].split(" ");
						String processLabel = getProcessLabel(currentNode);
						
						// Don't add subprograms as drivers
						if(isScopeName(rhsParts[0]) || rhsParts[0].startsWith("CONV_STD_LOGIC_VECTOR"))
						{
							continue;
						}
						
						// See if we need to update one of the object lists with the driver name
						if(signalList.contains(new VHDLSignal(rhs[i], NULL_TYPE)))
						{
							variableList.get(indexInList).addDriver(rhs[i]);
							continue;
						}
						else if(variableList.contains(new VHDLVariable(processLabel + "/" + rhs[i], NULL_TYPE)))
						{
							variableList.get(indexInList).addDriver(processLabel + "/" + rhs[i]);
							continue;
						}
						// Is it a number, a boolean, or a record type
						else
						{
							// Filter numbers
							try
							{
								variableList.get(indexInList).addDriver("" + extractPossiblyLongNumber(rhs[i].replace('\'', ' ').replace('\"', ' ').trim()));
								continue;
							}
							catch(Exception e)
							{
								;
							}
							
							// Filter Booleans
							if(rhs[i].compareToIgnoreCase("true") == 0 || rhs[i].compareToIgnoreCase("false") == 0)
							{
								variableList.get(indexInList).addDriver("" + Boolean.parseBoolean(rhs[i]));
								continue;
							}
							
							// Filter constants
							if(currentScope.containsConstant(rhs[i]))
							{
								variableList.get(indexInList).addDriver("" + currentScope.getConstant(rhs[i]).getValue());
								continue;
							}
							
							// Handle record objects
							String prefix = VHDLObject.getBaseName(rhs[i]);
							int indexOfDriver;
							if(!prefix.equals(rhs[i]) && (indexOfDriver = signalList.indexOf(new VHDLSignal(prefix, NULL_TYPE))) != -1)
							{
								addSignalToList(rhs[i], false);
								variableList.get(indexInList).addDriver(rhs[i]);
								continue;
							}
							else if(!prefix.equals(rhs[i]) && (indexOfDriver = variableList.indexOf(new VHDLVariable(processLabel + "/" + prefix, NULL_TYPE))) != -1)
							{
								addVariableToList(processLabel + "/" + rhs[i], false);
								variableList.get(indexInList).addDriver(processLabel + "/" + rhs[i]);
								continue;
							}
						}
						
						System.err.println("WARNING: Couldn't add " + rhs[i] + " as a driver");
					}
					break;
				// A signal assignment statement has the target followed directly by a waveform
				case VhdlParserTreeConstants.JJTSIGNAL_ASSIGNMENT_STATEMENT:
					// Print the right hand side
					rhs = printTerminalsInRHS(currentNode.jjtGetParent().jjtGetChild(currentNode.jjtIndexInParent()+1)).toUpperCase().split("\t");
					
					for(int i = 0; i < rhs.length; ++i)
					{
						String rhsParts[] = rhs[i].split(" ");
						String processLabel = getProcessLabel(currentNode);
						
						// Don't add subprograms as drivers
						if(isScopeName(rhsParts[0]) || rhsParts[0].startsWith("CONV_STD_LOGIC_VECTOR"))
						{
							continue;
						}
						
						// See if we need to update one of the object lists with the driver name
						if(signalList.contains(new VHDLSignal(rhs[i], NULL_TYPE)))
						{
							signalList.get(indexInList).addDriver(rhs[i]);
							continue;
						}
						else if(variableList.contains(new VHDLVariable(processLabel + "/" + rhs[i], NULL_TYPE)))
						{
							signalList.get(indexInList).addDriver(processLabel + "/" + rhs[i]);
							continue;
						}
						// Is it a number, a boolean, or a record type
						else
						{
							// Filter numbers
							try
							{
								signalList.get(indexInList).addDriver("" + extractPossiblyLongNumber(rhs[i].replace('\'', ' ').replace('\"', ' ').trim()));
								continue;
							}
							catch(Exception e)
							{
								;
							}
							
							// Filter Booleans
							if(rhs[i].compareToIgnoreCase("true") == 0 || rhs[i].compareToIgnoreCase("false") == 0)
							{
								signalList.get(indexInList).addDriver("" + Boolean.parseBoolean(rhs[i]));
								continue;
							}
							
							// Filter constants
							if(currentScope.containsConstant(rhs[i]))
							{
								signalList.get(indexInList).addDriver("" + currentScope.getConstant(rhs[i]).getValue());
								continue;
							}
							
							// Handle slicing
							String prefix = VHDLObject.getBaseName(rhs[i]);
							int indexOfDriver;
							
							// If the name contains slice info and the none sliced name is in the list add the sliced name to the list and then the driver
							if(!prefix.equals(rhs[i]) && (indexOfDriver = signalList.indexOf(new VHDLSignal(prefix, NULL_TYPE))) != -1)
							{
								addSignalToList(rhs[i], false);
								signalList.get(indexInList).addDriver(rhs[i]);
								continue;
							}
							else if(!prefix.equals(rhs[i]) && (indexOfDriver = variableList.indexOf(new VHDLVariable(processLabel + "/" + prefix, NULL_TYPE))) != -1)
							{
								addVariableToList(processLabel + "/" + rhs[i], false);
								signalList.get(indexInList).addDriver(processLabel + "/" + rhs[i]);
								continue;
							}
						}
						
						System.err.println("WARNING: Couldn't add " + rhs[i] + " as a driver");
					}
					break;
			}
			break;
		default:
			// Search for drivers in children
			for(int child = 0; child < currentNode.jjtGetNumChildren(); ++child)
			{
				// Modelsim can't track variables inside functions and procedures, so we can ignore assignment statements inside those
				// Don't need to look into process declarative part since no assignments there
				switch(currentNode.jjtGetChild(child).getId())
				{
					case VhdlParserTreeConstants.JJTPROCESS_DECLARATIVE_PART:
					case VhdlParserTreeConstants.JJTSUBPROGRAM_BODY:
						break;
					default:
						findDrivers(currentNode.jjtGetChild(child));
				}
			}
	  }
  }
  
  // Given a node that has a process statement somewhere in the parental chain this method returns that process's label
  private static String getProcessLabel(Node currentNode)
  {
	  // Get the process statement that is the parent of this assignment statement
	  while(currentNode.getId() != VhdlParserTreeConstants.JJTPROCESS_STATEMENT)
	  {
		  currentNode = currentNode.jjtGetParent();
	  }
		
	  // We use a label if it exists, or the line number if not
	  if(currentNode.jjtGetChild(0).getId() == VhdlParserTreeConstants.JJTPROCESS_LABEL)
	  {
		  return ((SimpleNode)currentNode.jjtGetChild(0).jjtGetChild(0).jjtGetChild(0)).first_token.toString().toUpperCase();
	  }
	  else
	  {
		  return "" + ((SimpleNode)currentNode).first_token.beginLine;
	  }
  }
  
  // Use the signal and variable lists to generate and store dataflow pairs in a list
  private static void findDataflowPairs()
  {
	  ArrayList<?> currentObjectList = signalList;
	  
	  while(true)
	  {
		  // Print signal/variable-based pairs
		  for(int object = 0; object < currentObjectList.size(); ++object)
		  {
			  VHDLObject temp = (VHDLObject)currentObjectList.get(object);
			  
			  // If there is only one driver, then this is a direct assignment, meaning no possibility of dead/malicious code
			  if(temp.numDrivers() < 2)
			  {
				continue;  
			  }
			  
			  // If the current signal is a register go back one cycle
			  if(temp.isRegister())
			  {
				  ++currentCycle;
			  }
			  
			  // Create a pair for each driver
			  // Recursively cover all possible drivers
			  for(int driver = 0; driver < temp.numDrivers(); ++driver)
			  {
				  findPairs(temp.getDriver(driver));
			  }
			  
			  // If we just went back, go forward one cycle
			  if(temp.isRegister())
			  {
				  --currentCycle;
			  }
			  
			  // Print the pairs for this signal
			  for(Iterator<String> pair = pairList.keySet().iterator(); pair.hasNext(); )
			  {
				  String pairName = pair.next();
				  
				  // Don't add self driver loops
				  if(!temp.getName().equals(pairName))
				  {
					  dataflowPairs.add(new DataflowPair(temp.getName(), pairList.get(pairName)));
				  }
			  }
			  
			  // Clear the list
			  pairList.clear();
		  }
		  
		  if(currentObjectList == variableList)
		  {
			  break;
		  }
		  currentObjectList = variableList;
	  }	  
  }
  
  // Recursive function that collects target,driver pairs
  private static void findPairs(String driver)
  { 
	  // Make sure the driver isn't in the list to avoid cycles
	  if(!pairsListContains(driver) && driver.length() > 0)
	  {
		  // No need for a cycle delay when looking at constant drivers
		  if(isNumberOrBoolean(driver))
		  {
			  pairList.put(driver, driver + "&0");  
		  }
		  else
		  {
			  pairList.put(driver, driver + "&" + currentCycle);
		  }
	  }
	  else
	  {
		  return;
	  }
	  
	  // Find the driver in the signal or variable list
	  int indexInList;
	  if((indexInList = signalList.indexOf(new VHDLSignal(driver, NULL_TYPE))) != -1)
	  {
		  // If the current driver is a register go back one cycle
		  if(signalList.get(indexInList).isRegister())
		  {
			  ++currentCycle;
		  }
		  
		  for(int i = 0; i < signalList.get(indexInList).numDrivers(); ++i)
		  {
			  findPairs(signalList.get(indexInList).getDriver(i));
		  }
		  
		  // If we just went back, go forward one cycle
		  if(signalList.get(indexInList).isRegister())
		  {
			  --currentCycle;
		  }
	  }
	  else if((indexInList = variableList.indexOf(new VHDLVariable(driver, NULL_TYPE))) != -1)
	  {
		  // If the current driver is a register go back one cycle
		  if(variableList.get(indexInList).isRegister())
		  {
			  ++currentCycle;
		  }
		  
		  for(int i = 0; i < variableList.get(indexInList).numDrivers(); ++i)
		  {
			  findPairs(variableList.get(indexInList).getDriver(i));
		  }
		  
		  // If we just went back, go forward one cycle
		  if(variableList.get(indexInList).isRegister())
		  {
			  --currentCycle;
		  }
	  }
	  else
	  {
		  // Filter numbers and Boolean values
		  if(!isNumberOrBoolean(driver))
		  {
			  System.err.println("ERROR: Couldn't find the driver in either list " + driver);
		  }
	  }
  }
  
  // Remove duplicates from the dataflow pairs list
  private static void removeDuplicatePairs()
  {
	  for(int pair = 0; pair < dataflowPairs.size(); ++ pair)
	  {
		  int indexOfDupe;
		  
		  while((indexOfDupe = dataflowPairs.lastIndexOf(dataflowPairs.get(pair))) > pair)
		  {
				  dataflowPairs.remove(indexOfDupe);
		  }
	  }
  }
  
  // Print the driver information for usage in the highlughter program
	private static String objectsToStringDrivers()
	{
		StringBuffer temp = new StringBuffer("");
		
		for(int list = 0; list < objectLists.length; ++list)
		{
			ArrayList<VHDLObject> objectList = (ArrayList<VHDLObject>)objectLists[list];
			
			for(int object = 0; object < objectList.size(); ++object)
			{
				temp.append(objectList.get(object).toStringDrivers());
			}
		}
		
		return temp.toString();
	}
	
	// Print the DOT file
	private static String objectsToStringDOT()
	{
		LinkedHashMap<String, String> noDupes = new LinkedHashMap<String, String>(1000);
		
		for(int list = 0; list < objectLists.length; ++list)
		{
			ArrayList<VHDLObject> objectList = (ArrayList<VHDLObject>)objectLists[list];
			
			// Remove duplicates by using a hash
			for(int object = 0; object < objectList.size(); ++object)
			{
				String[] temp = objectList.get(object).toStringDOT().split("\\n");
				
				for(int line = 0; line < temp.length; ++line)
				{
					noDupes.put(temp[line], null);
				}
			}
		}
		
		// Re-build the return string
		StringBuffer toRet = new StringBuffer("");
		for(Iterator<String> lines = noDupes.keySet().iterator(); lines.hasNext(); )
		{
			toRet.append(lines.next() + "\n");
		}
		
		return toRet.toString();
	}
	
	// Print the aiSee3 file
	private static String objectsToStringaiSee3()
	{
		LinkedHashMap<String, String> noDupes = new LinkedHashMap<String, String>(1000);
		
		for(int list = 0; list < objectLists.length; ++list)
		{
			ArrayList<VHDLObject> objectList = (ArrayList<VHDLObject>)objectLists[list];
			
			// Get the aiSee3 string for each object that has a driver
			for(int object = 0; object < objectList.size(); ++object)
			{
				if(objectList.get(object).numDrivers() <= 0)
				{
					continue;
				}
				
				String[] temp = objectList.get(object).toStringaiSee3().split("\\n");
				
				for(int line = 0; line < temp.length; ++line)
				{
					noDupes.put(temp[line], null);
				}
			}
		}
		
		// Re-build the return string, print node information, then edge information
		StringBuffer toRet = new StringBuffer("graph: {\n");
		for(Iterator<String> lines = noDupes.keySet().iterator(); lines.hasNext(); )
		{
			String temp = lines.next();
			
			if(temp.charAt(0) == 'n')
			{
				toRet.append("\t" + temp + "\n");
			}
		}
		for(Iterator<String> lines = noDupes.keySet().iterator(); lines.hasNext(); )
		{
			String temp = lines.next();
			
			if(temp.charAt(0) == 'e')
			{
				toRet.append("\t" + temp + "\n");
			}
		}
		return toRet.toString() + "}";
	}
	
  // Print the contents of the dataflow pairs list to the output
  private static void printDataflowPairs()
  {
	  for(int pair = 0; pair < dataflowPairs.size(); ++pair)
	  {
		  System.out.println(dataflowPairs.get(pair).left + "&" + dataflowPairs.get(pair).right);
	  }
  }
  
  // Returns true if the dataflow pairs list contains the string except for the delay portion
  private static boolean pairsListContains(String driver)
  {
	  return pairList.containsKey(driver);
  }
 
  private static String printTerminalsInRHS(Node currentNode)
  {
	  String temp = "";
	  
	  if(currentNode.getId() == VhdlParserTreeConstants.JJTPRIMARY)
	  {
		  switch(currentNode.jjtGetChild(0).getId())
		  {
		  	//case  VhdlParserTreeConstants.JJTISDISCRETESUBTYPE:
		  	case  VhdlParserTreeConstants.JJTFUNCTION_CALL:
		  	case  VhdlParserTreeConstants.JJTTYPE_CONVERSION:
		  	case  VhdlParserTreeConstants.JJTNAME:
		  	case  VhdlParserTreeConstants.JJTLITERAL:
		  		temp = VhdlWriter.start(currentNode).trim() + "\t";
		  		break;
		  	case  VhdlParserTreeConstants.JJTAGGREGATE:
		  	case  VhdlParserTreeConstants.JJTCONSUME_LEFT_PAREN:
		  	case  VhdlParserTreeConstants.JJTQUALIFIED_EXPRESSION:
		  		for(int child = 0; child < currentNode.jjtGetNumChildren(); ++child)
				{
		  			temp += printTerminalsInRHS(currentNode.jjtGetChild(child));
				}
		  		break;
		  	default:
		  		if(DEBUG_DATAFLOW)
		  		{
		  			System.err.println("ERROR: Child of primary not expected: " + currentNode.jjtGetChild(0).toString());
		  		}
		  }
	  }
	  else
	  {
		  for(int child = 0; child < currentNode.jjtGetNumChildren(); ++child)
		  {
			  temp += printTerminalsInRHS(currentNode.jjtGetChild(child));
		  }
	  }
	  
	  return temp;
  }
  
  // Collects terminal tokens by DFSing the subtree rooted at the given node
  private static String printTerminalsInSubtree(Node currentNode)
  {
	  String temp = "";
	  
	  if(currentNode.jjtGetNumChildren() == 0)
	  {
		  temp = ((SimpleNode)currentNode).first_token.toString() + " ";
	  }
	  else
	  {
		  for(int child = 0; child < currentNode.jjtGetNumChildren(); ++child)
		  {
			  temp += printTerminalsInSubtree(currentNode.jjtGetChild(child));
		  }
	  }
	  
	  return temp;
  }
  
  // Returns true if the passed string is a number of Boolean value
  private static boolean isNumberOrBoolean(String value)
  {
	  if(Pattern.matches("[0-9a-fA-F]+", value))
	  {
		  return true;
	  }

	  if(Pattern.matches("\"[01]+\"", value))
	  {
		  return true;
	  }
	  
	  if(value.equalsIgnoreCase("TRUE") || value.equalsIgnoreCase("FALSE"))
	  {
		  return true;
	  }
		
	  return false;
  }
  
  // Given a string containing numbers of base less than 16, this method returns the string back
  // Otherwise it throws a number format exception
  private static String extractPossiblyLongNumber(String aNumber)throws NumberFormatException
  {
	  // Because ints are small and BigInt can't handle leading 0's look at each digit
	  for(int digit = 0; digit < aNumber.length(); ++digit)
	  {
		  char current = aNumber.charAt(digit);
		  
		  if((current >= 'a' && current <= 'f') || (current >= 'A' && current <= 'F') || (current >= '0' && current <= '9'))
		  {
			  continue;
		  }
		  throw new NumberFormatException();
	  }
	  
	  return aNumber;
  }
  
  // Returns true if the name is a name of the current scope or a sub-scope of the current scope
  private static boolean isScopeName(String name)
  {
	  return currentScope.getName().equalsIgnoreCase(name) || currentScope.containsScope(name.toUpperCase());
  }
  
  private static void exit(String message)
  {
  	System.err.println("ERROR: " + message);
  	(new Exception()).printStackTrace();
  	System.exit(-1);
  }
}
