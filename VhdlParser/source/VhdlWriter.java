// Add error processing
// Differentiate between conditional and mandatory productions

public class VhdlWriter
{
	private static final String INDENT_STRING = "    ";

	private static java.io.BufferedWriter toFile = null;
	private static String outputBuffer = "";
	private static String output = "";
	private static int indentLevel = 0;
	private static  String[][] subs = null;
	
	public VhdlWriter()
	{
		stopWriter();
	}
	
	public VhdlWriter(String[][] substitutions)
	{
		stopWriter();
		subs = substitutions;
	}
	
	public VhdlWriter(String outfile, String[][] substitutions)
	{
		stopWriter();
		try
		{
			subs = substitutions;
			toFile = new java.io.BufferedWriter(new java.io.FileWriter(outfile));
		}
		catch(java.io.IOException ioe)
		{
			System.err.println("ERROR: Problem opening VHDL output file " + outfile);
		}
	}
	
	public VhdlWriter(String outfile)
	{
		stopWriter();
		try
		{
			toFile = new java.io.BufferedWriter(new java.io.FileWriter(outfile));
		}
		catch(java.io.IOException ioe)
		{
			System.err.println("ERROR: Problem opening VHDL output file " + outfile);
		}
	}

	public static void stopWriter()
	{
		try
		{
			if(toFile != null)
			{
				toFile.close();
			}
		}
		catch(java.io.IOException ioe)
		{
			System.err.println("ERROR: Problem closing VHDL output file.");
		}
		toFile = null;
		output = "";
	}

	private static void writeToken(String token)
	{
		String temp = outputBuffer + " " + token;
		
		// Check for an end-of-line marker
		int index = temp.indexOf(";");
		if(index != -1)
		{
			// Get the part of the string preceeding the semi-colon
			// Remove spaces around dots
			String toOut[] = (removeSpaces(temp.substring(0, index)) + ";").split("\\.");

			// Write the indents
			for(int i = 0; i < indentLevel; ++i)
			{
				output += INDENT_STRING;
			}

			for(int i = 0; i < (toOut.length - 1); ++i)
			{
				output += removeSpaces(toOut[i]) + ".";
			}
			output += removeSpaces(toOut[toOut.length - 1]);
			output += "\n";

			// New string buffer is remaining substring
			outputBuffer = temp.substring(index + 1).trim();
		}
		// If no end of line marker yet, just update the string buffer
		else
		{
			outputBuffer = temp;
		}
	}

	// Helper function that only removes spaces from strings
	private static String removeSpaces(String start)
	{
		// Leading spaces
		while(start.length() > 0 && start.charAt(0) == ' ')
		{
			start = start.substring(1, start.length());
		}

		// Trailing spaces
		while(start.length() > 0 && start.charAt(start.length() - 1) == ' ')
		{
			start = start.substring(0, start.length() - 1);
		}

		return start;
	}
	
	private static void increaseIndent()
	{
		flushOutputBuffer();
		++indentLevel;
	}

	private static void decreaseIndent()
	{
		flushOutputBuffer();
		--indentLevel;
		if(indentLevel < 0)
		{
			indentLevel = 0;
		}
	}

	// Flush the output buffer whenever the indent level changes
	private static void flushOutputBuffer()
	{
		if(outputBuffer.length() == 0)
		{
			return;
		}
		
		// Remove spaces around dots
		String toOut[] = outputBuffer.split("\\.");

		// Write the indents
		for(int i = 0; i < indentLevel; ++i)
		{
			output += INDENT_STRING;
		}

		for(int i = 0; i < (toOut.length - 1); ++i)
		{
			output += removeSpaces(toOut[i]) + ".";
		}
		output += removeSpaces(toOut[toOut.length - 1]);
		output += "\n";

		// New string buffer is empty
		outputBuffer = "";
	}

	
	public static String start(Node node)
	{	
		output = "";
		
		switch(node.getId())
		{
			case VhdlParserTreeConstants.JJTDESIGN_FILE:
				design_file(node);
				break;
			case VhdlParserTreeConstants.JJTSTATIC_EXPRESSION:
				static_expression(node);
				break;
			case VhdlParserTreeConstants.JJTEXPRESSION:
				expression(node);
				break;
			case VhdlParserTreeConstants.JJTTARGET:
				target(node);
				break;
			case VhdlParserTreeConstants.JJTWAVEFORM:
				waveform(node);
				break;
			case VhdlParserTreeConstants.JJTPRIMARY:
				primary(node);
				break;
			case VhdlParserTreeConstants.JJTCONDITION:
				condition(node);
				break;
			default:
				System.err.println("ERROR: start of VHDL Writer can't handle passed node type: " + node.toString());
				System.exit(0);
		}
		flushOutputBuffer();
		
		// Return or print the results
		if(toFile == null)
		{
			return output;
		}
		else
		{
			try
			{
				toFile.write(output);
			}
			catch(Exception e)
			{
				System.err.println("ERROR: Problem writing a token to the output file.");
			}
		}
		
		return "";
	}

	private static void abstract_literal(Node node)
	{
		writeToken(((SimpleNode)node).first_token.image);
	}

	private static void actual_designator(Node node)
	{
		int currentChild = 0;
	
		if(node.jjtGetNumChildren() == 0)
		{
			writeToken("OPEN");
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTEXPRESSION)
		{
			expression(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSIGNAL_NAME)
		{
			signal_name(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTVARIABLE_NAME)
		{
			variable_name(node.jjtGetChild(currentChild));
		}
	}

	private static void actual_parameter_part(Node node)
	{
		int currentChild = 0;
	
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTPARAMETER_ASSOCIATION_LIST)
		{
			parameter_association_list(node.jjtGetChild(currentChild));
		}
	}

	private static void actual_part(Node node)
	{
		int currentChild = 0;
	
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTACTUAL_DESIGNATOR)
		{
			actual_designator(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTFUNCTION_NAME)
		{
			function_name(node.jjtGetChild(currentChild));
			++currentChild;
			if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSUME_LEFT_PAREN)
			{
				consume_left_paren(node.jjtGetChild(currentChild));
			}
			++currentChild;
			if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTACTUAL_DESIGNATOR)
			{
				actual_designator(node.jjtGetChild(currentChild));
			}
			++currentChild;
			if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSUME_RIGHT_PAREN)
			{
				consume_right_paren(node.jjtGetChild(currentChild));
			}
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTTYPE_MARK)
		{
			type_mark(node.jjtGetChild(currentChild));
			++currentChild;
			if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSUME_LEFT_PAREN)
			{
				consume_left_paren(node.jjtGetChild(currentChild));
			}
			++currentChild;
			if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTACTUAL_DESIGNATOR)
			{
				actual_designator(node.jjtGetChild(currentChild));
			}
			++currentChild;
			if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSUME_RIGHT_PAREN)
			{
				consume_right_paren(node.jjtGetChild(currentChild));
			}
		}
	}

	private static void adding_operator(Node node)
	{
		writeToken(((SimpleNode)node).first_token.image);
	}

	private static void aggregate(Node node)
	{
		int currentChild = 0;
	
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSUME_LEFT_PAREN)
		{
			consume_left_paren(node.jjtGetChild(currentChild));
		}
		++currentChild;
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTELEMENT_ASSOCIATION)
		{
			element_association(node.jjtGetChild(currentChild));
		}
		++currentChild;
		while(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTELEMENT_ASSOCIATION)
		{
			writeToken(",");
			element_association(node.jjtGetChild(currentChild));
			++currentChild;
		}
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSUME_RIGHT_PAREN)
		{
			consume_right_paren(node.jjtGetChild(currentChild));
		}
	}

	private static void architecture_body(Node node)
	{
		int currentChild = 0;

		writeToken("\nARCHITECTURE");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTIDENTIFIER)
			identifier(node.jjtGetChild(currentChild++));
		writeToken("OF");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTENTITY_NAME)
			entity_name(node.jjtGetChild(currentChild++));
		writeToken("IS");
		increaseIndent();
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTARCHITECTURE_DECLARATIVE_PART)
			architecture_declarative_part(node.jjtGetChild(currentChild++));
		decreaseIndent();
		writeToken("BEGIN");
		increaseIndent();
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTARCHITECTURE_STATEMENT_PART)
			architecture_statement_part(node.jjtGetChild(currentChild++));
		decreaseIndent();
		writeToken("END ARCHITECTURE");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTARCHITECTURE_SIMPLE_NAME)
			architecture_simple_name(node.jjtGetChild(currentChild++));
		writeToken(";");
	}

	private static void architecture_declarative_part(Node node)
	{
		int currentChild = 0;

		while(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTBLOCK_DECLARATIVE_ITEM)
			block_declarative_item(node.jjtGetChild(currentChild++));
	}

	private static void architecture_statement_part(Node node)
	{
		int currentChild = 0;

		while(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTARCHITECTURE_STATEMENT)
			architecture_statement(node.jjtGetChild(currentChild++));
	}

	private static void architecture_statement(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTBLOCK_STATEMENT)
			block_statement(node.jjtGetChild(currentChild++));
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTPROCESS_STATEMENT)
			process_statement(node.jjtGetChild(currentChild++));
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONCURRENT_PROCEDURE_CALL_STATEMENT)
			concurrent_procedure_call_statement(node.jjtGetChild(currentChild++));
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONCURRENT_SIGNAL_ASSIGNMENT_STATEMENT)
			concurrent_signal_assignment_statement(node.jjtGetChild(currentChild++));
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCOMPONENT_INSTANTIATION_STATEMENT)
			component_instantiation_statement(node.jjtGetChild(currentChild++));
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTGENERATE_STATEMENT)
			generate_statement(node.jjtGetChild(currentChild++));
	}

	private static void array_type_definition(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTUNCONSTRAINED_ARRAY_DEFINITION)
			unconstrained_array_definition(node.jjtGetChild(currentChild++));
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSTRAINED_ARRAY_DEFINITION)
			constrained_array_definition(node.jjtGetChild(currentChild++));
	}

	private static void association_element(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTFORMAL_PART)
		{
			formal_part(node.jjtGetChild(currentChild++));
			writeToken("=>");
		}
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTACTUAL_PART)
			actual_part(node.jjtGetChild(currentChild++));
	}

	private static void association_list(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTASSOCIATION_ELEMENT)
			association_element(node.jjtGetChild(currentChild++));
		while(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTASSOCIATION_ELEMENT)
		{
			writeToken(",");
			association_element(node.jjtGetChild(currentChild++));
		}
	}

	private static void attribute_declaration(Node node)
	{
		int currentChild = 0;

		writeToken("ATTRIBUTE");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTIDENTIFIER)
			identifier(node.jjtGetChild(currentChild++));
		writeToken(":");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTTYPE_MARK)
			type_mark(node.jjtGetChild(currentChild++));
		writeToken(";");
	}

	private static void attribute_designator(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTATTRIBUTE_SIMPLE_NAME)
			attribute_simple_name(node.jjtGetChild(currentChild++));
	}

	private static void attribute_specification(Node node)
	{
		int currentChild = 0;

		writeToken("ATTRIBUTE");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTATTRIBUTE_DESIGNATOR)
			attribute_designator(node.jjtGetChild(currentChild++));
		writeToken("OF");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTENTITY_SPECIFICATION)
			entity_specification(node.jjtGetChild(currentChild++));
		writeToken("IS");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTEXPRESSION)
			expression(node.jjtGetChild(currentChild++));
		writeToken(";");
	}

	private static void binding_indication(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTENTITY_ASPECT)
		{
			writeToken("USE");
			entity_aspect(node.jjtGetChild(currentChild++));
		}
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTGENERIC_MAP_ASPECT)
			generic_map_aspect(node.jjtGetChild(currentChild++));
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTPORT_MAP_ASPECT)
			port_map_aspect(node.jjtGetChild(currentChild++));
	}

	private static void block_configuration(Node node)
	{
		int currentChild = 0;

		writeToken("FOR");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTBLOCK_SPECIFICATION)
			block_specification(node.jjtGetChild(currentChild++));
		increaseIndent();
		while(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTUSE_CLAUSE)
			use_clause(node.jjtGetChild(currentChild++));
		while(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONFIGURATION_ITEM)
			configuration_item(node.jjtGetChild(currentChild++));
		decreaseIndent();
		writeToken("END FOR;");
	}

	private static void block_declarative_item(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSUBPROGRAM_DECLARATION)
			subprogram_declaration(node.jjtGetChild(currentChild++));
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSUBPROGRAM_BODY)
			subprogram_body(node.jjtGetChild(currentChild++));
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTTYPE_DECLARATION)
			type_declaration(node.jjtGetChild(currentChild++));
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSUBTYPE_DECLARATION)
			subtype_declaration(node.jjtGetChild(currentChild++));
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSTANT_DECLARATION)
			constant_declaration(node.jjtGetChild(currentChild++));
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSIGNAL_DECLARATION)
			signal_declaration(node.jjtGetChild(currentChild++));
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCOMPONENT_DECLARATION)
			component_declaration(node.jjtGetChild(currentChild++));
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTATTRIBUTE_DECLARATION)
			attribute_declaration(node.jjtGetChild(currentChild++));
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTATTRIBUTE_SPECIFICATION)
			attribute_specification(node.jjtGetChild(currentChild++));
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONFIGURATION_SPECIFICATION)
			configuration_specification(node.jjtGetChild(currentChild++));
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTUSE_CLAUSE)
			use_clause(node.jjtGetChild(currentChild++));
	}

	private static void block_declarative_part(Node node)
	{
		int currentChild = 0;

		while(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTBLOCK_DECLARATIVE_ITEM)
			block_declarative_item(node.jjtGetChild(currentChild++));
	}

	private static void block_specification(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTARCHITECTURE_NAME)
			architecture_name(node.jjtGetChild(currentChild++));
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTBLOCK_STATEMENT_LABEL)
			block_statement_label(node.jjtGetChild(currentChild++));
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTGENERATE_STATEMENT_LABEL)
			generate_statement_label(node.jjtGetChild(currentChild++));
		if((currentChild+2) < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSUME_LEFT_PAREN && node.jjtGetChild(currentChild+1).getId() == VhdlParserTreeConstants.JJTINDEX_SPECIFICATION && node.jjtGetChild(currentChild+2).getId() == VhdlParserTreeConstants.JJTCONSUME_RIGHT_PAREN)
		{
			consume_left_paren(node.jjtGetChild(currentChild++));
			index_specification(node.jjtGetChild(currentChild++));
			consume_right_paren(node.jjtGetChild(currentChild++));
		}
	}

	private static void block_statement(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTBLOCK_LABEL)
			block_label(node.jjtGetChild(currentChild++));
		writeToken(": BLOCK");
		if((currentChild+2) < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSUME_LEFT_PAREN && node.jjtGetChild(currentChild+1).getId() == VhdlParserTreeConstants.JJTGUARD_EXPRESSION && node.jjtGetChild(currentChild+2).getId() == VhdlParserTreeConstants.JJTCONSUME_RIGHT_PAREN)
		{
			consume_left_paren(node.jjtGetChild(currentChild++));
			guard_expression(node.jjtGetChild(currentChild++));
			consume_right_paren(node.jjtGetChild(currentChild++));
		}
		writeToken("IS");
		increaseIndent();
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTBLOCK_DECLARATIVE_PART)
			block_declarative_part(node.jjtGetChild(currentChild++));
		decreaseIndent();
		writeToken("BEGIN");
		increaseIndent();
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTBLOCK_STATEMENT_PART)
			block_statement_part(node.jjtGetChild(currentChild++));
		decreaseIndent();
		writeToken("END BLOCK");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTBLOCK_LABEL)
			block_label(node.jjtGetChild(currentChild++));
		writeToken(";");
	}

	private static void block_statement_part(Node node)
	{
		int currentChild = 0;

		while(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTARCHITECTURE_STATEMENT)
			architecture_statement(node.jjtGetChild(currentChild++));
	}

	private static void case_statement(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCASE_LABEL)
		{
			case_label(node.jjtGetChild(currentChild++));
			writeToken(":");
		}
		writeToken("CASE");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTEXPRESSION)
			expression(node.jjtGetChild(currentChild++));
		writeToken("IS");
		increaseIndent();
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCASE_STATEMENT_ALTERNATIVE)
			case_statement_alternative(node.jjtGetChild(currentChild++));
		while(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCASE_STATEMENT_ALTERNATIVE)
			case_statement_alternative(node.jjtGetChild(currentChild++));
		decreaseIndent();
		writeToken("END CASE");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCASE_LABEL)
			case_label(node.jjtGetChild(currentChild++));
		writeToken(";");
	}

	private static void case_statement_alternative(Node node)
	{
		int currentChild = 0;

		writeToken("WHEN");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCHOICES)
			choices(node.jjtGetChild(currentChild++));
		writeToken("=>");
		increaseIndent();
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSEQUENCE_OF_STATEMENTS)
			sequence_of_statements(node.jjtGetChild(currentChild++));
		decreaseIndent();
	}

	private static void choice(Node node)
	{
		int currentChild = 0;

		if(node.jjtGetNumChildren() == 0)
			writeToken("OTHERS");
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSIMPLE_EXPRESSION)
			simple_expression(node.jjtGetChild(currentChild++));
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTDISCRETE_RANGE)
			discrete_range(node.jjtGetChild(currentChild++));
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTELEMENT_SIMPLE_NAME)
			element_simple_name(node.jjtGetChild(currentChild++));
	}

	private static void choices(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCHOICE)
			choice(node.jjtGetChild(currentChild++));
		while(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCHOICE)
		{
			writeToken("|");
			choice(node.jjtGetChild(currentChild++));
		}
	}

	private static void component_configuration(Node node)
	{
		int currentChild = 0;

		writeToken("FOR");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCOMPONENT_SPECIFICATION)
			component_specification(node.jjtGetChild(currentChild++));
		increaseIndent();
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTBINDING_INDICATION)
			binding_indication(node.jjtGetChild(currentChild++));
		writeToken(";");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTBLOCK_CONFIGURATION)
			block_configuration(node.jjtGetChild(currentChild++));
		decreaseIndent();
		writeToken("END FOR");
		writeToken(";");
	}

	private static void component_declaration(Node node)
	{
		int currentChild = 0;

		writeToken("COMPONENT");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTIDENTIFIER)
			identifier(node.jjtGetChild(currentChild++));
		writeToken("IS");
		increaseIndent();
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTLOCAL_GENERIC_CLAUSE)
			local_generic_clause(node.jjtGetChild(currentChild++));
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTLOCAL_PORT_CLAUSE)
			local_port_clause(node.jjtGetChild(currentChild++));
		decreaseIndent();
		writeToken("END COMPONENT");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCOMPONENT_SIMPLE_NAME)
			component_simple_name(node.jjtGetChild(currentChild++));
		writeToken(";");
	}

	private static void component_instantiation_statement(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTINSTANTIATION_LABEL)
			instantiation_label(node.jjtGetChild(currentChild++));
		writeToken(":");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTINSTANTIATED_UNIT)
			instantiated_unit(node.jjtGetChild(currentChild++));
		increaseIndent();
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTGENERIC_MAP_ASPECT)
			generic_map_aspect(node.jjtGetChild(currentChild++));
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTPORT_MAP_ASPECT)
			port_map_aspect(node.jjtGetChild(currentChild++));
		decreaseIndent();
		writeToken(";");
	}

	private static void component_specification(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTINSTANTIATION_LIST)
			instantiation_list(node.jjtGetChild(currentChild++));
		writeToken(":");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCOMPONENT_NAME)
			component_name(node.jjtGetChild(currentChild++));
	}

	private static void composite_type_definition(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTARRAY_TYPE_DEFINITION)
			array_type_definition(node.jjtGetChild(currentChild++));
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTRECORD_TYPE_DEFINITION)
			record_type_definition(node.jjtGetChild(currentChild++));
	}

	private static void concurrent_procedure_call_statement(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTLABEL)
		{
			label(node.jjtGetChild(currentChild++));
			writeToken(":");
		}
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTPROCEDURE_CALL)
			procedure_call(node.jjtGetChild(currentChild++));
		writeToken(";");
	}

	private static void concurrent_signal_assignment_statement(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTLABEL)
		{
			label(node.jjtGetChild(currentChild++));
			writeToken(":");
		}
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONDITIONAL_SIGNAL_ASSIGNMENT)
			conditional_signal_assignment(node.jjtGetChild(currentChild++)); 
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSELECTED_SIGNAL_ASSIGNMENT)
			selected_signal_assignment(node.jjtGetChild(currentChild++));
	}

	private static void condition(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTBOOLEAN_EXPRESSION)
			boolean_expression(node.jjtGetChild(currentChild++));
	}

	private static void condition_clause(Node node)
	{
		int currentChild = 0;

		writeToken("UNTIL");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONDITION)
			condition(node.jjtGetChild(currentChild++));
	}

	private static void conditional_signal_assignment(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTTARGET)
			target(node.jjtGetChild(currentChild++));
		writeToken("<=");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTOPTIONS_)
			options_(node.jjtGetChild(currentChild++));
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONDITIONAL_WAVEFORMS)
			conditional_waveforms(node.jjtGetChild(currentChild++));
		writeToken(";");
	}

	private static void conditional_waveforms(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTWAVEFORM)
			waveform(node.jjtGetChild(currentChild++));
		while((currentChild+1) < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONDITION && node.jjtGetChild(currentChild+1).getId() == VhdlParserTreeConstants.JJTWAVEFORM)
		{
			writeToken("WHEN");
			condition(node.jjtGetChild(currentChild++));
			writeToken("ELSE");
			waveform(node.jjtGetChild(currentChild++));
		}
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONDITION)
		{
			writeToken("WHEN");
			condition(node.jjtGetChild(currentChild++));
		}
	}

	private static void configuration_declaration(Node node)
	{
		int currentChild = 0;

		writeToken("CONFIGURATION");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTIDENTIFIER)
			identifier(node.jjtGetChild(currentChild++));
		writeToken("OF");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTENTITY_NAME)
			entity_name(node.jjtGetChild(currentChild++));
		writeToken("IS");
		increaseIndent();
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONFIGURATION_DECLARATIVE_PART)
			configuration_declarative_part(node.jjtGetChild(currentChild++));
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTBLOCK_CONFIGURATION)
			block_configuration(node.jjtGetChild(currentChild++));
		decreaseIndent();
		writeToken("END CONFIGURATION");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONFIGURATION_SIMPLE_NAME)
			configuration_simple_name(node.jjtGetChild(currentChild++));
		writeToken(";");
	}

	private static void configuration_declarative_item(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTUSE_CLAUSE)
			use_clause(node.jjtGetChild(currentChild++));
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTATTRIBUTE_SPECIFICATION)
			attribute_specification(node.jjtGetChild(currentChild++));
	}

	private static void configuration_declarative_part(Node node)
	{
		int currentChild = 0;

		while(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONFIGURATION_DECLARATIVE_ITEM)
			configuration_declarative_item(node.jjtGetChild(currentChild++));
	}

	private static void configuration_item(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTBLOCK_CONFIGURATION)
			block_configuration(node.jjtGetChild(currentChild++));
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCOMPONENT_CONFIGURATION)
			component_configuration(node.jjtGetChild(currentChild++));
	}

	private static void configuration_specification(Node node)
	{
		int currentChild = 0;

		writeToken("FOR");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCOMPONENT_SPECIFICATION)
			component_specification(node.jjtGetChild(currentChild++));
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTBINDING_INDICATION)
			binding_indication(node.jjtGetChild(currentChild++));
		writeToken(";");
	}

	private static void constant_declaration(Node node)
	{
		int currentChild = 0;

		writeToken("CONSTANT");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTIDENTIFIER_LIST)
			identifier_list(node.jjtGetChild(currentChild++));
		writeToken(":");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSUBTYPE_INDICATION)
			subtype_indication(node.jjtGetChild(currentChild++));
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTEXPRESSION)
		{
			writeToken(":=");
			expression(node.jjtGetChild(currentChild++));
		}
		writeToken(";");
	}

	private static void constrained_array_definition(Node node)
	{
		int currentChild = 0;

		writeToken("ARRAY");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTINDEX_CONSTRAINT)
			index_constraint(node.jjtGetChild(currentChild++));
		writeToken("OF");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTELEMENT_SUBTYPE_INDICATION)
			element_subtype_indication(node.jjtGetChild(currentChild++));
	}

	private static void constraint(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTRANGE_CONSTRAINT)
			range_constraint(node.jjtGetChild(currentChild++));
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTINDEX_CONSTRAINT)
			index_constraint(node.jjtGetChild(currentChild++));
	}

	private static void context_clause(Node node)
	{
		int currentChild = 0;

		while(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONTEXT_ITEM)
			context_item(node.jjtGetChild(currentChild++));
	}

	private static void context_item(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTLIBRARY_CLAUSE)
			library_clause(node.jjtGetChild(currentChild++));
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTUSE_CLAUSE)
			use_clause(node.jjtGetChild(currentChild++));
	}

	private static void design_file(Node node)
	{
		int currentChild = 0;

		do
		{
			design_unit(node.jjtGetChild(currentChild++));
		} while(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTDESIGN_UNIT);
	}

	private static void design_unit(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONTEXT_CLAUSE)
			context_clause(node.jjtGetChild(currentChild++));

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTLIBRARY_UNIT)
			library_unit(node.jjtGetChild(currentChild++));
	}

	private static void designator(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTIDENTIFIER)
			identifier(node.jjtGetChild(currentChild++));
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTOPERATOR_SYMBOL)
			operator_symbol(node.jjtGetChild(currentChild++));
	}

	private static void direction(Node node)
	{
		writeToken(((SimpleNode)node).first_token.image);
	}

	private static void discrete_range(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTRANGE)
			range(node.jjtGetChild(currentChild++));
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTDISCRETE_SUBTYPE_INDICATION)
			discrete_subtype_indication(node.jjtGetChild(currentChild++));
	}

	private static void element_association(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCHOICES)
		{
			choices(node.jjtGetChild(currentChild++));
			writeToken("=>");
		}
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTEXPRESSION)
			expression(node.jjtGetChild(currentChild++));
	}

	private static void element_declaration(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTIDENTIFIER_LIST)
			identifier_list(node.jjtGetChild(currentChild++));
		writeToken(":");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTELEMENT_SUBTYPE_DEFINITION)
			element_subtype_definition(node.jjtGetChild(currentChild++));
		writeToken(";");
	}

	private static void element_subtype_definition(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSUBTYPE_INDICATION)
			subtype_indication(node.jjtGetChild(currentChild++));
	}

	private static void entity_aspect(Node node)
	{
		int currentChild = 0;
		
		if(node.jjtGetNumChildren() == 0)
			writeToken("OPEN");
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTENTITY_NAME)
		{
			writeToken("ENTITY");
			entity_name(node.jjtGetChild(currentChild++));
		
			if((currentChild+2) < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSUME_LEFT_PAREN && node.jjtGetChild(currentChild+1).getId() == VhdlParserTreeConstants.JJTARCHITECTURE_IDENTIFIER && node.jjtGetChild(currentChild+2).getId() == VhdlParserTreeConstants.JJTCONSUME_RIGHT_PAREN)
			{
				consume_left_paren(node.jjtGetChild(currentChild++));
				architecture_identifier(node.jjtGetChild(currentChild++));
				consume_right_paren(node.jjtGetChild(currentChild++));
			}
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONFIGURATION_NAME)
		{
			writeToken("CONFIGURATION");
			configuration_name(node.jjtGetChild(currentChild++));
		}
	}

	private static void entity_class(Node node)
	{
		writeToken(((SimpleNode)node).first_token.image);
	}

	private static void entity_declaration(Node node)
	{
		int currentChild = 0;

		writeToken("\nENTITY");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTIDENTIFIER)
			identifier(node.jjtGetChild(currentChild++));
		writeToken("IS");
		increaseIndent();
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTENTITY_HEADER)
			entity_header(node.jjtGetChild(currentChild++));
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTENTITY_DECLARATIVE_PART)
			entity_declarative_part(node.jjtGetChild(currentChild++));
		decreaseIndent();
		writeToken("END ENTITY");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTENTITY_SIMPLE_NAME)
			entity_simple_name(node.jjtGetChild(currentChild++));
		writeToken(";");
	}

	private static void entity_declarative_item(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSUBPROGRAM_DECLARATION)
			subprogram_declaration(node.jjtGetChild(currentChild++));
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSUBPROGRAM_BODY)
			subprogram_body(node.jjtGetChild(currentChild++));
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTTYPE_DECLARATION)
			type_declaration(node.jjtGetChild(currentChild++));
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSUBTYPE_DECLARATION)
			subtype_declaration(node.jjtGetChild(currentChild++));
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSTANT_DECLARATION)
			constant_declaration(node.jjtGetChild(currentChild++));
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSIGNAL_DECLARATION)
			signal_declaration(node.jjtGetChild(currentChild++));
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTUSE_CLAUSE)
			use_clause(node.jjtGetChild(currentChild++));
	}

	private static void entity_declarative_part(Node node)
	{
		int currentChild = 0;

		while(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTENTITY_DECLARATIVE_ITEM)
			entity_declarative_item(node.jjtGetChild(currentChild++));
	}

	private static void entity_designator(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTENTITY_TAG)
			entity_tag(node.jjtGetChild(currentChild++));
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSIGNATURE)
			signature(node.jjtGetChild(currentChild++));
	}

	private static void entity_header(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTFORMAL_GENERIC_CLAUSE)
			formal_generic_clause(node.jjtGetChild(currentChild++));
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTFORMAL_PORT_CLAUSE)
			formal_port_clause(node.jjtGetChild(currentChild++));
	}

	private static void entity_name_list(Node node)
	{
		int currentChild = 0;
		
		if(node.jjtGetNumChildren() == 0)
			writeToken(((SimpleNode)node).first_token.image);
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTENTITY_DESIGNATOR)
		{
			entity_designator(node.jjtGetChild(currentChild++));
			while(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTENTITY_DESIGNATOR)
			{
				writeToken(",");
				entity_designator(node.jjtGetChild(currentChild++));
			}
		}
	}

	private static void entity_specification(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTENTITY_NAME_LIST)
			entity_name_list(node.jjtGetChild(currentChild++));
		writeToken(":");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTENTITY_CLASS)
			entity_class(node.jjtGetChild(currentChild++));
	}

	private static void entity_tag(Node node)
	{
		int currentChild = 0;

		if(node.jjtGetNumChildren() == 0)
			writeToken(((SimpleNode)node).first_token.image);
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSIMPLE_NAME)
			simple_name(node.jjtGetChild(currentChild++));
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTOPERATOR_SYMBOL)
			operator_symbol(node.jjtGetChild(currentChild++));
	}

	private static void enumeration_literal(Node node)
	{
		int currentChild = 0;
		
		if(node.jjtGetNumChildren() == 0)
			writeToken(((SimpleNode)node).first_token.image);
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTIDENTIFIER)
			identifier(node.jjtGetChild(currentChild));
	}

	private static void enumeration_type_definition(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSUME_LEFT_PAREN)
			consume_left_paren(node.jjtGetChild(currentChild++));
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTENUMERATION_LITERAL)
			enumeration_literal(node.jjtGetChild(currentChild++));
		while(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTENUMERATION_LITERAL)
		{
			writeToken(",");
			enumeration_literal(node.jjtGetChild(currentChild++));
		}
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSUME_RIGHT_PAREN)
			consume_right_paren(node.jjtGetChild(currentChild++));
	}

	private static void exit_statement(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTLABEL)
		{
			label(node.jjtGetChild(currentChild++));
			writeToken(":");
		}
		writeToken("EXIT");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTLOOP_LABEL)
			loop_label(node.jjtGetChild(currentChild++));
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONDITION)
		{
			writeToken("WHEN");
			condition(node.jjtGetChild(currentChild++));
		}
		writeToken(";");
	}

	private static void expression(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTRELATION)
			relation(node.jjtGetChild(currentChild++)); 
		while(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTLOGICAL_OPERATOR)
		{
			logical_operator(node.jjtGetChild(currentChild++));
			if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTRELATION)
				relation(node.jjtGetChild(currentChild++));
		}
	}

	private static void factor(Node node)
	{
		int currentChild = 0;

		if(((SimpleNode)node).first_token.image.compareToIgnoreCase("ABS") == 0 || ((SimpleNode)node).first_token.image.compareToIgnoreCase("NOT") == 0)
		{
			writeToken(((SimpleNode)node).first_token.image);
			if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTPRIMARY)
				primary(node.jjtGetChild(currentChild++));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTPRIMARY)
		{
			primary(node.jjtGetChild(currentChild++));
			if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTPRIMARY)
			{
				writeToken("**");
				primary(node.jjtGetChild(currentChild++));
			}
		}
	}

	private static void formal_designator(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTGENERIC_NAME)
			generic_name(node.jjtGetChild(currentChild++));
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTPORT_NAME)
			port_name(node.jjtGetChild(currentChild++));
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTPARAMETER_NAME)
			parameter_name(node.jjtGetChild(currentChild++));
	}

	private static void formal_parameter_list(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTPARAMETER_INTERFACE_LIST)
			parameter_interface_list(node.jjtGetChild(currentChild++));
	}

	private static void formal_part(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTFUNCTION_NAME)
		{
			function_name(node.jjtGetChild(currentChild++));
			if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSUME_LEFT_PAREN)
				consume_left_paren(node.jjtGetChild(currentChild++));
			if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTFORMAL_DESIGNATOR)
				formal_designator(node.jjtGetChild(currentChild++));
			if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSUME_RIGHT_PAREN)
				consume_right_paren(node.jjtGetChild(currentChild++));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTTYPE_MARK)
		{
			type_mark(node.jjtGetChild(currentChild++));
			if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSUME_LEFT_PAREN)
				consume_left_paren(node.jjtGetChild(currentChild++));
			if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTFORMAL_DESIGNATOR)
				formal_designator(node.jjtGetChild(currentChild++));
			if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSUME_RIGHT_PAREN)
				consume_right_paren(node.jjtGetChild(currentChild++));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTFORMAL_DESIGNATOR)
			formal_designator(node.jjtGetChild(currentChild++));
	}

	private static void full_type_declaration(Node node)
	{
		int currentChild = 0;

		writeToken("TYPE");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTIDENTIFIER)
			identifier(node.jjtGetChild(currentChild++));
		writeToken("IS");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTTYPE_DEFINITION)
			type_definition(node.jjtGetChild(currentChild++));
		writeToken(";");
	}

	private static void function_call(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTFUNCTION_NAME)
		function_name(node.jjtGetChild(currentChild++));
		if((currentChild+2) < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSUME_LEFT_PAREN && node.jjtGetChild(currentChild+1).getId() == VhdlParserTreeConstants.JJTACTUAL_PARAMETER_PART && node.jjtGetChild(currentChild+2).getId() == VhdlParserTreeConstants.JJTCONSUME_RIGHT_PAREN)
		{
			consume_left_paren(node.jjtGetChild(currentChild++));
			actual_parameter_part(node.jjtGetChild(currentChild++));
			consume_right_paren(node.jjtGetChild(currentChild++));
		}
	}

	private static void generate_statement(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTGENERATE_LABEL)
			generate_label(node.jjtGetChild(currentChild++));
		writeToken(":");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTGENERATION_SCHEME)
			generation_scheme(node.jjtGetChild(currentChild++));
		writeToken("GENERATE");
		increaseIndent();
		while(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTBLOCK_DECLARATIVE_ITEM)
			block_declarative_item(node.jjtGetChild(currentChild++));
		decreaseIndent();
		writeToken("BEGIN");
		increaseIndent();
		while(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTARCHITECTURE_STATEMENT)
			architecture_statement(node.jjtGetChild(currentChild++));
		decreaseIndent();
		writeToken("END GENERATE");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTGENERATE_LABEL)
			generate_label(node.jjtGetChild(currentChild++));
		writeToken(";");
	}

	private static void generation_scheme(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTGENERATE_PARAMETER_SPECIFICATION)
		{
			writeToken("FOR");	
			generate_parameter_specification(node.jjtGetChild(currentChild++));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONDITION)
		{
			writeToken("IF");
			condition(node.jjtGetChild(currentChild++));
		}
	}

	private static void generic_clause(Node node)
	{
		int currentChild = 0;

		writeToken("GENERIC");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSUME_LEFT_PAREN)
			consume_left_paren(node.jjtGetChild(currentChild++));
		increaseIndent();
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTINTERFACE_CONSTANT_DECLARATION)
			interface_constant_declaration(node.jjtGetChild(currentChild++));
		while(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTINTERFACE_CONSTANT_DECLARATION)
		{
			writeToken(";");
			interface_constant_declaration(node.jjtGetChild(currentChild++));
		}
		decreaseIndent();
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSUME_RIGHT_PAREN)
			consume_right_paren(node.jjtGetChild(currentChild++));
		writeToken(";");
	}

	private static void generic_map_aspect(Node node)
	{
		int currentChild = 0;

		writeToken("GENERIC MAP");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSUME_LEFT_PAREN)
			consume_left_paren(node.jjtGetChild(currentChild++));
		increaseIndent();
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTGENERIC_ASSOCIATION_LIST)
			generic_association_list(node.jjtGetChild(currentChild++));
		decreaseIndent();
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSUME_RIGHT_PAREN)
			consume_right_paren(node.jjtGetChild(currentChild++));
	}

	private static void identifier(Node node)
	{  
		if(subs != null && !(node.jjtHasParentOfID(VhdlParserTreeConstants.JJTIDENTIFIER_LIST) && node.jjtHasParentOfID(VhdlParserTreeConstants.JJTGENERIC_CLAUSE)) && 
				!(node.jjtHasParentOfID(VhdlParserTreeConstants.JJTIDENTIFIER_LIST) && node.jjtHasParentOfID(VhdlParserTreeConstants.JJTCONSTANT_DECLARATION)) && 
				!(node.jjtHasParentOfID(VhdlParserTreeConstants.JJTIDENTIFIER_LIST) && node.jjtHasParentOfID(VhdlParserTreeConstants.JJTRECORD_TYPE_DEFINITION)) &&
				!(node.jjtHasParentOfID(VhdlParserTreeConstants.JJTIDENTIFIER_LIST) && node.jjtHasParentOfID(VhdlParserTreeConstants.JJTVARIABLE_DECLARATION)) &&
				!(node.jjtHasParentOfID(VhdlParserTreeConstants.JJTIDENTIFIER_LIST) && node.jjtHasParentOfID(VhdlParserTreeConstants.JJTSIGNAL_DECLARATION)) &&
				!node.jjtHasParentOfID(VhdlParserTreeConstants.JJTFORMAL_PART) &&
				!node.jjtHasParentOfID(VhdlParserTreeConstants.JJTTARGET) &&
				!(node.jjtHasParentOfID(VhdlParserTreeConstants.JJTSUFFIX) || node.jjtHasParentOfID(VhdlParserTreeConstants.JJTSIGNATURE)))
		{
			for(int i = 0; i < subs.length && subs[i][0] != null; ++i)
			{
				if(subs[i][0].compareToIgnoreCase(((SimpleNode)node).first_token.image) == 0)
				{
					writeToken(subs[i][1]);
					return;
				}
			}
		}
		writeToken(((SimpleNode)node).first_token.image);
	}

	private static void identifier_list(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTIDENTIFIER)
			identifier(node.jjtGetChild(currentChild++));

		while(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTIDENTIFIER)
		{
			writeToken(",");
			identifier(node.jjtGetChild(currentChild++));
		}
	}

	private static void if_statement(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTIF_LABEL)
		{
			if_label(node.jjtGetChild(currentChild));
			++currentChild;
			writeToken(":");
		}
		writeToken("IF");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONDITION)
		{
			condition(node.jjtGetChild(currentChild));
			++currentChild;
		}
		writeToken("THEN");
		increaseIndent();
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSEQUENCE_OF_STATEMENTS)
		{
			sequence_of_statements(node.jjtGetChild(currentChild));
			++currentChild;
		}
		while(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONDITION)
		{
			decreaseIndent();
			writeToken("ELSIF");
			condition(node.jjtGetChild(currentChild));
			++currentChild;
			writeToken("THEN");
			increaseIndent();
			if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSEQUENCE_OF_STATEMENTS)
			{
				sequence_of_statements(node.jjtGetChild(currentChild));
				++currentChild;
			}
		}
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSEQUENCE_OF_STATEMENTS)
		{
			decreaseIndent();
			writeToken("ELSE");
			increaseIndent();
			sequence_of_statements(node.jjtGetChild(currentChild));
			++currentChild;
		}
		decreaseIndent();
		writeToken("END IF");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTIF_LABEL)
		{
			if_label(node.jjtGetChild(currentChild));
		}
		writeToken(";");
	}

	private static void index_constraint(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSUME_LEFT_PAREN)
		{
			consume_left_paren(node.jjtGetChild(currentChild));
			++currentChild;
		}
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTDISCRETE_RANGE)
		{
			discrete_range(node.jjtGetChild(currentChild));
			++currentChild;
		}
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSUME_RIGHT_PAREN)
		{
			consume_right_paren(node.jjtGetChild(currentChild));
		}
	}

	private static void index_specification(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTDISCRETE_RANGE)
		{
			discrete_range(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSTATIC_EXPRESSION)
		{
			static_expression(node.jjtGetChild(currentChild));
		}
	}

	private static void index_subtype_definition(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTTYPE_MARK)
		{
			type_mark(node.jjtGetChild(currentChild));
		}
		writeToken("RANGE");
		writeToken("<>");
	}

	private static void instantiated_unit(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCOMPONENT_NAME)
		{
			writeToken("COMPONENT");
			component_name(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTENTITY_NAME)
		{
			writeToken("ENTITY");
			entity_name(node.jjtGetChild(currentChild));
			++currentChild;
			if((currentChild+2) < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSUME_LEFT_PAREN && node.jjtGetChild(currentChild+1).getId() == VhdlParserTreeConstants.JJTARCHITECTURE_IDENTIFIER && node.jjtGetChild(currentChild+2).getId() == VhdlParserTreeConstants.JJTCONSUME_RIGHT_PAREN)
			{
				consume_left_paren(node.jjtGetChild(currentChild));
				++currentChild;
				architecture_identifier(node.jjtGetChild(currentChild));
				++currentChild;
				consume_right_paren(node.jjtGetChild(currentChild));
			}
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONFIGURATION_NAME)
		{
			writeToken("CONFIGURATION");
			configuration_name(node.jjtGetChild(currentChild));
		}
	}

	private static void instantiation_list(Node node)
	{
		int currentChild = 0;

		if(node.jjtGetNumChildren() == 0)
		{
			writeToken(((SimpleNode)node).first_token.image);
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTINSTANTIATION_LABEL)
		{
			instantiation_label(node.jjtGetChild(currentChild));
			++currentChild;
			while(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTINSTANTIATION_LABEL)
			{
				writeToken(",");
				instantiation_label(node.jjtGetChild(currentChild));
				++currentChild;
			}
		}
	}

	private static void integer_type_definition(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTRANGE_CONSTRAINT)
		{
			range_constraint(node.jjtGetChild(currentChild));
		}
	}

	private static void interface_constant_declaration(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTIDENTIFIER_LIST)
		{
			identifier_list(node.jjtGetChild(currentChild));
			++currentChild;
		}
		writeToken(":");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSUBTYPE_INDICATION)
		{
			subtype_indication(node.jjtGetChild(currentChild));
			++currentChild;
		}
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSTATIC_EXPRESSION)
		{
			writeToken(":=");
			static_expression(node.jjtGetChild(currentChild));
		}
	}

	private static void interface_signal_declaration(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTIDENTIFIER_LIST)
		{
			identifier_list(node.jjtGetChild(currentChild));
			++currentChild;
		}
		writeToken(":");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTMODE)
		{
			mode(node.jjtGetChild(currentChild));
			++currentChild;
		}
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSUBTYPE_INDICATION)
		{
			subtype_indication(node.jjtGetChild(currentChild));
		}
	}

	private static void interface_variable_declaration(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTIDENTIFIER_LIST)
		{
			identifier_list(node.jjtGetChild(currentChild));
			++currentChild;
		}
		writeToken(":");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTMODE)
		{
			mode(node.jjtGetChild(currentChild));
			++currentChild;
		}
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSUBTYPE_INDICATION)
		{
			subtype_indication(node.jjtGetChild(currentChild));
		}
	}

	private static void iteration_scheme(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTLOOP_PARAMETER_SPECIFICATION)
		{
			writeToken("FOR");
			loop_parameter_specification(node.jjtGetChild(currentChild));
		}
	}

	private static void label(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTIDENTIFIER)
		{
			identifier(node.jjtGetChild(currentChild));
		}
	}

	private static void library_clause(Node node)
	{
		int currentChild = 0;

		writeToken("LIBRARY");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTLOGICAL_NAME_LIST)
		{
			logical_name_list(node.jjtGetChild(currentChild));
		}
		writeToken(";");
	}

	private static void library_unit(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTPRIMARY_UNIT)
		{
			primary_unit(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSECONDARY_UNIT)
		{
			secondary_unit(node.jjtGetChild(currentChild));
		}
	}

	private static void literal(Node node)
	{
		int currentChild = 0;
		
		if(node.jjtGetNumChildren() == 0)
		{
			writeToken(((SimpleNode)node).first_token.image);
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTNUMERIC_LITERAL)
		{
			numeric_literal(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTENUMERATION_LITERAL)
		{
			enumeration_literal(node.jjtGetChild(currentChild));
		}
	}

	private static void logical_name(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTIDENTIFIER)
		{
			identifier(node.jjtGetChild(currentChild));
		}
	}

	private static void logical_name_list(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTLOGICAL_NAME)
		{
			logical_name(node.jjtGetChild(currentChild));
			++currentChild;
		}
		while(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTLOGICAL_NAME)
		{
			writeToken(",");
			logical_name(node.jjtGetChild(currentChild));
			++currentChild;
		}
	}

	private static void logical_operator(Node node)
	{
		writeToken(((SimpleNode)node).first_token.image);
	}

	private static void loop_statement(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTLOOP_LABEL)
		{
			loop_label(node.jjtGetChild(currentChild));
			++currentChild;
			writeToken(":");
		}
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTITERATION_SCHEME)
		{
			iteration_scheme(node.jjtGetChild(currentChild));
			++currentChild;
		}
		writeToken("LOOP");
		increaseIndent();
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSEQUENCE_OF_STATEMENTS)
		{
			sequence_of_statements(node.jjtGetChild(currentChild));
			++currentChild;
		}
		decreaseIndent();
		writeToken("END LOOP");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTLOOP_LABEL)
		{
			loop_label(node.jjtGetChild(currentChild));
			++currentChild;
		}
		writeToken(";");
	}

	private static void mode(Node node)
	{
		writeToken(((SimpleNode)node).first_token.image);
	}

	private static void multiplying_operator(Node node)
	{
		writeToken(((SimpleNode)node).first_token.image);
	}

	private static void name(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSIMPLE_NAME)
		{
			simple_name(node.jjtGetChild(currentChild));
			++currentChild;
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTOPERATOR_SYMBOL)
		{
			operator_symbol(node.jjtGetChild(currentChild));
			++currentChild;
		}
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTNAME_EXTENSION)
		{
			name_extension(node.jjtGetChild(currentChild));
		}
	}

	private static void name_extension(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && (node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSIGNATURE || node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTATTRIBUTE_DESIGNATOR))
		{
			if(node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSIGNATURE)
			{
				signature(node.jjtGetChild(currentChild));
				++currentChild;
			}
			writeToken("'");
			if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTATTRIBUTE_DESIGNATOR)
			{
				attribute_designator(node.jjtGetChild(currentChild));
				++currentChild;
			}
		}
		if((currentChild+2) < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSUME_LEFT_PAREN && node.jjtGetChild(currentChild+1).getId() == VhdlParserTreeConstants.JJTEXPRESSION && node.jjtGetChild(currentChild+2).getId() == VhdlParserTreeConstants.JJTCONSUME_RIGHT_PAREN)
		{
			consume_left_paren(node.jjtGetChild(currentChild));
			++currentChild;
			expression(node.jjtGetChild(currentChild));
			++currentChild;
			consume_right_paren(node.jjtGetChild(currentChild));
			++currentChild;
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSUFFIX)
		{
			writeToken(".");
			suffix(node.jjtGetChild(currentChild));
			++currentChild;
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTINDEX_CONSTRAINT)
		{
			index_constraint(node.jjtGetChild(currentChild));
			++currentChild;
		}
		else if((currentChild+1) < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSUME_LEFT_PAREN && node.jjtGetChild(currentChild+1).getId() == VhdlParserTreeConstants.JJTEXPRESSION)
		{
			consume_left_paren(node.jjtGetChild(currentChild));
			++currentChild;
			expression(node.jjtGetChild(currentChild));
			++currentChild;
			if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSUME_RIGHT_PAREN)
			{
				consume_right_paren(node.jjtGetChild(currentChild));
				++currentChild;
			}
		}
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTNAME_EXTENSION)
		{
			name_extension(node.jjtGetChild(currentChild));
		}
	}

	private static void next_statement(Node node)
	{
		int currentChild = 0;
                                                               
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTLABEL)
		{
			label(node.jjtGetChild(currentChild));
			++currentChild;
			writeToken(":");
		}
		writeToken("NEXT");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTLOOP_LABEL)
		{
			loop_label(node.jjtGetChild(currentChild));
			++currentChild;
		}
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONDITION)
		{
			writeToken("WHEN");
			condition(node.jjtGetChild(currentChild));
			++currentChild;
		}
		writeToken(";");
	}

	private static void null_statement(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTLABEL)
		{
			label(node.jjtGetChild(currentChild));
			++currentChild;
			writeToken(":");
		}
		writeToken("NULL;");
	}

	private static void numeric_literal(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTABSTRACT_LITERAL)
		{
			abstract_literal(node.jjtGetChild(currentChild));
		}
	}

	private static void operator_symbol(Node node)
	{
		writeToken(((SimpleNode)node).first_token.image);
	}

	private static void options_(Node node)
	{
		if(((SimpleNode)node).first_token.image.compareToIgnoreCase("GUARDED") == 0)
			writeToken(((SimpleNode)node).first_token.image);
	}

	private static void package_body(Node node)
	{
		int currentChild = 0;

		writeToken("PACKAGE BODY");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTPACKAGE_SIMPLE_NAME)
		{
			package_simple_name(node.jjtGetChild(currentChild));
			++currentChild;
		}
		writeToken("IS");
		increaseIndent();
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTPACKAGE_BODY_DECLARATIVE_PART)
		{
			package_body_declarative_part(node.jjtGetChild(currentChild));
			++currentChild;
		}
		decreaseIndent();
		writeToken("END PACKAGE BODY");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTPACKAGE_SIMPLE_NAME)
		{
			package_simple_name(node.jjtGetChild(currentChild));
		}
		writeToken(";");
	}

	private static void package_body_declarative_item(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSUBPROGRAM_DECLARATION)
		{
			subprogram_declaration(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSUBPROGRAM_BODY)
		{
			subprogram_body(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTTYPE_DECLARATION)
		{
			type_declaration(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSUBTYPE_DECLARATION)
		{
			subtype_declaration(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSTANT_DECLARATION)
		{
			constant_declaration(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTUSE_CLAUSE)
		{
			use_clause(node.jjtGetChild(currentChild));
		}
	}

	private static void package_body_declarative_part(Node node)
	{
		int currentChild = 0;

		while(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTPACKAGE_BODY_DECLARATIVE_ITEM)
		{
			package_body_declarative_item(node.jjtGetChild(currentChild));
			++currentChild;
		}
	}

	private static void package_declaration(Node node)
	{
		int currentChild = 0;

		writeToken("PACKAGE");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTIDENTIFIER)
		{
			identifier(node.jjtGetChild(currentChild));
			++currentChild;
		}
		writeToken("IS");
		increaseIndent();
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTPACKAGE_DECLARATIVE_PART)
		{
			package_declarative_part(node.jjtGetChild(currentChild));
			++currentChild;
		}
		decreaseIndent();
		writeToken("END PACKAGE");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTPACKAGE_SIMPLE_NAME)
		{
			package_simple_name(node.jjtGetChild(currentChild));
		}
		writeToken(";");
	}

	private static void package_declarative_item(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSUBPROGRAM_DECLARATION)
		{
			subprogram_declaration(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTTYPE_DECLARATION)
		{
			type_declaration(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSUBTYPE_DECLARATION)
		{
			subtype_declaration(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSTANT_DECLARATION)
		{
			constant_declaration(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSIGNAL_DECLARATION)
		{
			signal_declaration(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCOMPONENT_DECLARATION)
		{
			component_declaration(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTATTRIBUTE_DECLARATION)
		{
			attribute_declaration(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTATTRIBUTE_SPECIFICATION)
		{
			attribute_specification(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTUSE_CLAUSE)
		{
			use_clause(node.jjtGetChild(currentChild));
		}
	}

	private static void package_declarative_part(Node node)
	{
		int currentChild = 0;

		while(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTPACKAGE_DECLARATIVE_ITEM)
		{
			package_declarative_item(node.jjtGetChild(currentChild));
			++currentChild;
		}
	}

	private static void parameter_specification(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTIDENTIFIER)
		{
			identifier(node.jjtGetChild(currentChild));
			++currentChild;
		}
		writeToken("IN");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTDISCRETE_RANGE)
		{
			discrete_range(node.jjtGetChild(currentChild));
		}
	}

	private static void port_clause(Node node)
	{
		int currentChild = 0;

		writeToken("PORT");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSUME_LEFT_PAREN)
		{
			consume_left_paren(node.jjtGetChild(currentChild));
			++currentChild;
		}
		increaseIndent();
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTPORT_LIST)
		{
			port_list(node.jjtGetChild(currentChild));
			++currentChild;
		}
		decreaseIndent();
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSUME_RIGHT_PAREN)
		{
			consume_right_paren(node.jjtGetChild(currentChild));
		}
		writeToken(";");
	}

	private static void port_list(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTPORT_INTERFACE_LIST)
		{
			port_interface_list(node.jjtGetChild(currentChild));
		}
	}

	private static void port_map_aspect(Node node)
	{
		int currentChild = 0;

		writeToken("PORT MAP");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSUME_LEFT_PAREN)
		{
			consume_left_paren(node.jjtGetChild(currentChild));
			++currentChild;
		}
		increaseIndent();
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTPORT_ASSOCIATION_LIST)
		{
			port_association_list(node.jjtGetChild(currentChild));
			++currentChild;
		}
		decreaseIndent();
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSUME_RIGHT_PAREN)
		{
			consume_right_paren(node.jjtGetChild(currentChild));
		}
	}

	private static void primary(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTQUALIFIED_EXPRESSION)
		{
			qualified_expression(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTFUNCTION_CALL)
		{
			function_call(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTNAME)
		{
			name(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTLITERAL)
		{
			literal(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTAGGREGATE)
		{
			aggregate(node.jjtGetChild(currentChild));
		}
		else if((currentChild+2) < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSUME_LEFT_PAREN && node.jjtGetChild(currentChild+1).getId() == VhdlParserTreeConstants.JJTEXPRESSION && node.jjtGetChild(currentChild+2).getId() == VhdlParserTreeConstants.JJTCONSUME_RIGHT_PAREN)
		{
			consume_left_paren(node.jjtGetChild(currentChild));
			++currentChild;
			expression(node.jjtGetChild(currentChild));
			++currentChild;
			consume_right_paren(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTTYPE_CONVERSION)
		{
			type_conversion(node.jjtGetChild(currentChild));
		}
	}

	private static void primary_unit(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTENTITY_DECLARATION)
		{
			entity_declaration(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONFIGURATION_DECLARATION)
		{
			configuration_declaration(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTPACKAGE_DECLARATION)
		{
			package_declaration(node.jjtGetChild(currentChild));
		}
	}

	private static void procedure_call(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTPROCEDURE_NAME)
		{
			procedure_name(node.jjtGetChild(currentChild));
			++currentChild;
		}
		if((currentChild+1) < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSUME_LEFT_PAREN && node.jjtGetChild(currentChild+1).getId() == VhdlParserTreeConstants.JJTACTUAL_PARAMETER_PART)
		{
			consume_left_paren(node.jjtGetChild(currentChild));
			++currentChild;
			actual_parameter_part(node.jjtGetChild(currentChild));
			++currentChild;
			if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSUME_RIGHT_PAREN)
			{
				consume_right_paren(node.jjtGetChild(currentChild));
			}
		}
	}

	private static void procedure_call_statement(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTLABEL)
		{
			label(node.jjtGetChild(currentChild));
			++currentChild;
			writeToken(":");
		}
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTPROCEDURE_CALL)
		{
			procedure_call(node.jjtGetChild(currentChild));
		}
		writeToken(";");
	}

	private static void process_declarative_item(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSUBPROGRAM_DECLARATION)
		{
			subprogram_declaration(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSUBPROGRAM_BODY)
		{
			subprogram_body(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTTYPE_DECLARATION)
		{
			type_declaration(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSUBTYPE_DECLARATION)
		{
			subtype_declaration(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSTANT_DECLARATION)
		{
			constant_declaration(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTVARIABLE_DECLARATION)
		{
			variable_declaration(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTATTRIBUTE_DECLARATION)
		{
			attribute_declaration(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTATTRIBUTE_SPECIFICATION)
		{
			attribute_specification(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTUSE_CLAUSE)
		{
			use_clause(node.jjtGetChild(currentChild));
		}
	}

	private static void process_declarative_part(Node node)
	{
		int currentChild = 0;

		while(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTPROCESS_DECLARATIVE_ITEM)
		{
			process_declarative_item(node.jjtGetChild(currentChild));
			++currentChild;
		}
	}

	private static void process_statement(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTPROCESS_LABEL)
		{
			process_label(node.jjtGetChild(currentChild));
			++currentChild;
			writeToken(":");
		}
		writeToken("PROCESS");
		if((currentChild+1) < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSUME_LEFT_PAREN && node.jjtGetChild(currentChild+1).getId() == VhdlParserTreeConstants.JJTSENSITIVITY_LIST)
		{
			consume_left_paren(node.jjtGetChild(currentChild));
			++currentChild;
			sensitivity_list(node.jjtGetChild(currentChild));
			++currentChild;
			if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSUME_RIGHT_PAREN)
			{
				consume_right_paren(node.jjtGetChild(currentChild));
				++currentChild;
			}
		}
		increaseIndent();
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTPROCESS_DECLARATIVE_PART)
		{
			process_declarative_part(node.jjtGetChild(currentChild));
			++currentChild;
		}
		decreaseIndent();
		writeToken("BEGIN");
		increaseIndent();
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTPROCESS_STATEMENT_PART)
		{
			process_statement_part(node.jjtGetChild(currentChild));
			++currentChild;
		}
		decreaseIndent();
		writeToken("END PROCESS");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTPROCESS_LABEL)
		{
			process_label(node.jjtGetChild(currentChild));
		}
		writeToken(";");
	}

	private static void process_statement_part(Node node)
	{
		int currentChild = 0;

		while(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSEQUENTIAL_STATEMENT)
		{
			sequential_statement(node.jjtGetChild(currentChild));
			++currentChild;
		}
	}

	private static void qualified_expression(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTTYPE_MARK)
		{
			type_mark(node.jjtGetChild(currentChild));
			++currentChild;
		}
		writeToken("'");
	
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTAGGREGATE)
		{
			aggregate(node.jjtGetChild(currentChild));
			++currentChild;
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSUME_LEFT_PAREN)
		{
			consume_left_paren(node.jjtGetChild(currentChild));
			++currentChild;
			if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTEXPRESSION)
			{
				expression(node.jjtGetChild(currentChild));
				++currentChild;
			}
			if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSUME_RIGHT_PAREN)
			{
				consume_right_paren(node.jjtGetChild(currentChild));
			}
		}
	}

	private static void range(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTRANGE_ATTRIBUTE_NAME)
		{
			range_attribute_name(node.jjtGetChild(currentChild));
			++currentChild;
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSIMPLE_EXPRESSION)
		{
			simple_expression(node.jjtGetChild(currentChild));
			++currentChild;
			if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTDIRECTION)
			{
				direction(node.jjtGetChild(currentChild));
				++currentChild;
			}
			if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSIMPLE_EXPRESSION)
			{
				simple_expression(node.jjtGetChild(currentChild));
			}
		}
	}

	private static void range_constraint(Node node)
	{
		int currentChild = 0;

		writeToken("RANGE");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTRANGE)
		{
			range(node.jjtGetChild(currentChild));
			++currentChild;
		}
	}

	private static void record_type_definition(Node node)
	{
		int currentChild = 0;

		writeToken("RECORD");
		increaseIndent();

		do
		{
			element_declaration(node.jjtGetChild(currentChild));
			++currentChild;
		} while(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTELEMENT_DECLARATION);
	
		decreaseIndent();
		writeToken("END RECORD");

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTRECORD_TYPE_SIMPLE_NAME)
		{
			record_type_simple_name(node.jjtGetChild(currentChild));
		}
	}

	private static void relation(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSHIFT_EXPRESSION)
		{
			shift_expression(node.jjtGetChild(currentChild));
			++currentChild;
		} 
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTRELATIONAL_OPERATOR)
		{
			relational_operator(node.jjtGetChild(currentChild));
			++currentChild;
			if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSHIFT_EXPRESSION)
			{
				shift_expression(node.jjtGetChild(currentChild));
			}
		}
	}

	private static void relational_operator(Node node)
	{
		writeToken(((SimpleNode)node).first_token.image);
	}

	private static void return_statement(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTLABEL)
		{
			label(node.jjtGetChild(currentChild));
			writeToken(":");
			++currentChild;
		}
	
		writeToken("RETURN");
	
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTEXPRESSION)
		{
			expression(node.jjtGetChild(currentChild));
			++currentChild;
		}
		writeToken(";");
	}

	private static void scalar_type_definition(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTENUMERATION_TYPE_DEFINITION)
		{
			enumeration_type_definition(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTINTEGER_TYPE_DEFINITION)
		{
			integer_type_definition(node.jjtGetChild(currentChild));
		}
	}

	private static void secondary_unit(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTARCHITECTURE_BODY)
		{
			architecture_body(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTPACKAGE_BODY)
		{
			package_body(node.jjtGetChild(currentChild));
		}
	}

	private static void selected_name(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSIMPLE_NAME)
		{
			simple_name(node.jjtGetChild(currentChild));
			++currentChild;
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTOPERATOR_SYMBOL)
		{
			operator_symbol(node.jjtGetChild(currentChild));
			++currentChild;
		}
	
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTNAME_EXTENSION)
		{
			name_extension(node.jjtGetChild(currentChild));
		}
	}

	private static void selected_signal_assignment(Node node)
	{
		int currentChild = 0;

		writeToken("WITH");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTEXPRESSION)
		{
			expression(node.jjtGetChild(currentChild));
			++currentChild;
		}
		writeToken("SELECT");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTTARGET)
		{
			target(node.jjtGetChild(currentChild));
			++currentChild;
		}
		writeToken("<=");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTOPTIONS_)
		{
			options_(node.jjtGetChild(currentChild));
			++currentChild;
		}
		increaseIndent();
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSELECTED_WAVEFORMS)
		{
			selected_waveforms(node.jjtGetChild(currentChild));
		}
		writeToken(";");
		decreaseIndent();
	}

	private static void selected_waveforms(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTWAVEFORM)
		{
			waveform(node.jjtGetChild(currentChild));
			++currentChild;
		}

		writeToken("WHEN");
	
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCHOICES)
		{
			choices(node.jjtGetChild(currentChild));
			++currentChild;
		}
	
		while(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTWAVEFORM)
		{
			writeToken(",");
			waveform(node.jjtGetChild(currentChild));
			++currentChild;
			writeToken("WHEN");
			if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCHOICES)
			{
				choices(node.jjtGetChild(currentChild));
				++currentChild;
			}
		}
	}

	private static void sensitivity_clause(Node node)
	{
		int currentChild = 0;

		writeToken("ON");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSENSITIVITY_LIST)
		{
			sensitivity_list(node.jjtGetChild(currentChild));
		}
	}

	private static void sensitivity_list(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSIGNAL_NAME)
		{
			signal_name(node.jjtGetChild(currentChild));
			++currentChild;
		}
		while(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSIGNAL_NAME)
		{
			writeToken(",");
			signal_name(node.jjtGetChild(currentChild));
			++currentChild;
		}
	}

	private static void sequence_of_statements(Node node)
	{
		int currentChild = 0;

		while(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSEQUENTIAL_STATEMENT)
		{
			sequential_statement(node.jjtGetChild(currentChild));
			++currentChild;
		}

	}

	private static void sequential_statement(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTWAIT_STATEMENT)
		{
			wait_statement(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSIGNAL_ASSIGNMENT_STATEMENT)
		{
			signal_assignment_statement(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTVARIABLE_ASSIGNMENT_STATEMENT)
		{
			variable_assignment_statement(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTPROCEDURE_CALL_STATEMENT)
		{
			procedure_call_statement(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTIF_STATEMENT)
		{
			if_statement(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCASE_STATEMENT)
		{
			case_statement(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTLOOP_STATEMENT)
		{
			loop_statement(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTNEXT_STATEMENT)
		{
			next_statement(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTEXIT_STATEMENT)
		{
			exit_statement(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTRETURN_STATEMENT)
		{
			return_statement(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTNULL_STATEMENT)
		{
			null_statement(node.jjtGetChild(currentChild));
		}
	}

	private static void shift_expression(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSIMPLE_EXPRESSION)
		{
			simple_expression(node.jjtGetChild(currentChild));
			++currentChild;
		}
	
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSHIFT_OPERATOR)
		{
			shift_operator(node.jjtGetChild(currentChild));
			++currentChild;
			if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSIMPLE_EXPRESSION)
			{
				simple_expression(node.jjtGetChild(currentChild));
			}
		}

	}

	private static void shift_operator(Node node)
	{
		writeToken(((SimpleNode)node).first_token.image);
	}

	private static void sign(Node node)
	{
		writeToken(((SimpleNode)node).first_token.image);
	}

	private static void signal_assignment_statement(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTLABEL)
		{
			label(node.jjtGetChild(currentChild));
			writeToken(":");
			++currentChild;
		}
	
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTTARGET)
		{
			target(node.jjtGetChild(currentChild));
			++currentChild;
		}
		writeToken("<=");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTWAVEFORM)
		{
			waveform(node.jjtGetChild(currentChild));
		}
		writeToken(";");
	}

	private static void signal_declaration(Node node)
	{
		int currentChild = 0;

		writeToken("SIGNAL");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTIDENTIFIER_LIST)
		{
			identifier_list(node.jjtGetChild(currentChild));
			++currentChild;
		}
		writeToken(":");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSUBTYPE_INDICATION)
		{
			subtype_indication(node.jjtGetChild(currentChild));
			++currentChild;
		}
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSIGNAL_KIND)
		{
			signal_kind(node.jjtGetChild(currentChild));
		}
	
		writeToken(";");
	}

	private static void signal_kind(Node node)
	{
		writeToken(((SimpleNode)node).first_token.image);
	}

	private static void signature(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTTYPE_MARK)
		{
			type_mark(node.jjtGetChild(currentChild));
			++currentChild;
		}

		while(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTTYPE_MARK)
		{
			writeToken(",");
			type_mark(node.jjtGetChild(currentChild));
			++currentChild;
		}
	
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTTYPE_MARK)
		{
			writeToken("RETURN");
			type_mark(node.jjtGetChild(currentChild));
		}
	}

	private static void simple_expression(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSIGN)
		{
			sign(node.jjtGetChild(currentChild));
			++currentChild;
		}
	
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTTERM)
		{
			term(node.jjtGetChild(currentChild));
			++currentChild;
		}

		while(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTADDING_OPERATOR)
		{
			adding_operator(node.jjtGetChild(currentChild));
			++currentChild;
			if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTTERM)
			{
				term(node.jjtGetChild(currentChild));
				++currentChild;
			}
		}
	}

	private static void simple_name(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTIDENTIFIER)
		{
			identifier(node.jjtGetChild(currentChild));
		}
	}
	
	private static void subprogram_body(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSUBPROGRAM_SPECIFICATION)
		{
			subprogram_specification(node.jjtGetChild(currentChild));
			++currentChild;
		}

		writeToken("IS");
		increaseIndent();
	
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSUBPROGRAM_DECLARATIVE_PART)
		{
			subprogram_declarative_part(node.jjtGetChild(currentChild));
			++currentChild;
		}
	
		decreaseIndent();
		writeToken("BEGIN");
		increaseIndent();

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSUBPROGRAM_STATEMENT_PART)
		{
			subprogram_statement_part(node.jjtGetChild(currentChild));
			++currentChild;
		}
	
		decreaseIndent();
		writeToken("END");

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSUBPROGRAM_KIND)
		{
			subprogram_kind(node.jjtGetChild(currentChild));
			++currentChild;
		}

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTDESIGNATOR)
		{
			designator(node.jjtGetChild(currentChild));
		}
		writeToken(";");
	}

	private static void subprogram_declaration(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSUBPROGRAM_SPECIFICATION)
		{
			subprogram_specification(node.jjtGetChild(currentChild));
		}
		writeToken(";");
	}

	private static void subprogram_declarative_item(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSUBPROGRAM_DECLARATION)
		{
			subprogram_declaration(node.jjtGetChild(currentChild));
			++currentChild;
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSUBPROGRAM_BODY)
		{
			subprogram_body(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTTYPE_DECLARATION)
		{
			type_declaration(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSUBTYPE_DECLARATION)
		{
			subtype_declaration(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSTANT_DECLARATION)
		{
			constant_declaration(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTVARIABLE_DECLARATION)
		{
			variable_declaration(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTATTRIBUTE_DECLARATION)
		{
			attribute_declaration(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTATTRIBUTE_SPECIFICATION)
		{
			attribute_specification(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTUSE_CLAUSE)
		{
			use_clause(node.jjtGetChild(currentChild));
		}
	}

	private static void subprogram_declarative_part(Node node)
	{
		int currentChild = 0;

		while(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSUBPROGRAM_DECLARATIVE_ITEM)
		{
			subprogram_declarative_item(node.jjtGetChild(currentChild));
			++currentChild;
		}
	}

	private static void subprogram_kind(Node node)
	{
		writeToken(((SimpleNode)node).first_token.image);
	}

	private static void subprogram_specification(Node node)
	{
		int currentChild = 0;

		if((currentChild+4) < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTDESIGNATOR && node.jjtGetChild(currentChild+4).getId() == VhdlParserTreeConstants.JJTTYPE_MARK)
		{
			writeToken("FUNCTION");
			designator(node.jjtGetChild(currentChild));
			++currentChild;
			if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSUME_LEFT_PAREN)
			{
				consume_left_paren(node.jjtGetChild(currentChild));
				++currentChild;
				increaseIndent();
				if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTFORMAL_PARAMETER_LIST)
				{
					formal_parameter_list(node.jjtGetChild(currentChild));
					++currentChild;
				}
				decreaseIndent();
				if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSUME_RIGHT_PAREN)
				{
					consume_right_paren(node.jjtGetChild(currentChild));
					++currentChild;
				}
			}
			writeToken("RETURN");
			type_mark(node.jjtGetChild(currentChild));
			++currentChild;
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTDESIGNATOR)
		{
			writeToken("PROCEDURE");
			designator(node.jjtGetChild(currentChild));
			++currentChild;
			if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSUME_LEFT_PAREN)
			{
				consume_left_paren(node.jjtGetChild(currentChild));
				++currentChild;
				increaseIndent();
				if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTFORMAL_PARAMETER_LIST)
				{
					formal_parameter_list(node.jjtGetChild(currentChild));
					++currentChild;
				}
				decreaseIndent();
				if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSUME_RIGHT_PAREN)
				{
					consume_right_paren(node.jjtGetChild(currentChild));
				}
			}
		}
	}

	private static void subprogram_statement_part(Node node)
	{
		int currentChild = 0;
		
		while(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSEQUENTIAL_STATEMENT)
		{
			sequential_statement(node.jjtGetChild(currentChild));
			++currentChild;
		}
	}

	private static void subtype_declaration(Node node)
	{
		int currentChild = 0;

		writeToken("SUBTYPE");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTIDENTIFIER)
		{
			identifier(node.jjtGetChild(currentChild));
			++currentChild;
		}
	
		writeToken("IS");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSUBTYPE_INDICATION)
		{
			subtype_indication(node.jjtGetChild(currentChild));
		}
		writeToken(";");
	}

	private static void subtype_indication(Node node)
	{
		int currentChild = 0;

		if((currentChild+1) < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTTYPE_MARK && node.jjtGetChild(currentChild+1).getId() == VhdlParserTreeConstants.JJTCONSTRAINT)
		{
			type_mark(node.jjtGetChild(currentChild));
			++currentChild;
			constraint(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTTYPE_MARK)
		{
			type_mark(node.jjtGetChild(currentChild));
		}
	}

	private static void suffix(Node node)
	{
		int currentChild = 0;
	
		if(node.jjtGetNumChildren() == 0)
		{
			writeToken(((SimpleNode)node).first_token.image);
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSIMPLE_NAME)
		{
			simple_name(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTOPERATOR_SYMBOL)
		{
			operator_symbol(node.jjtGetChild(currentChild));
		}
	}

	private static void target(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTNAME)
		{
			name(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTAGGREGATE)
		{
			aggregate(node.jjtGetChild(currentChild));
		}
	}

	private static void term(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTFACTOR)
		{
			factor(node.jjtGetChild(currentChild));
			++currentChild;
		}
		while((currentChild+1) < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTMULTIPLYING_OPERATOR && node.jjtGetChild(currentChild+1).getId() == VhdlParserTreeConstants.JJTFACTOR)
		{
			multiplying_operator(node.jjtGetChild(currentChild));
			++currentChild;
			factor(node.jjtGetChild(currentChild));
			++currentChild;
		}
	}

	private static void type_conversion(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTTYPE_MARK)
		{
			type_mark(node.jjtGetChild(currentChild));
			++currentChild;
		}
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSUME_LEFT_PAREN)
		{
			consume_left_paren(node.jjtGetChild(currentChild));
			++currentChild;
		}
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTEXPRESSION)
		{
			expression(node.jjtGetChild(currentChild));
			++currentChild;
		}
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSUME_RIGHT_PAREN)
		{
			consume_right_paren(node.jjtGetChild(currentChild));
		}
	}

	private static void type_declaration(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTFULL_TYPE_DECLARATION)
		{
			full_type_declaration(node.jjtGetChild(currentChild));
		}
	}

	private static void type_definition(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSCALAR_TYPE_DEFINITION)
		{
			scalar_type_definition(node.jjtGetChild(currentChild));
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCOMPOSITE_TYPE_DEFINITION)
		{
			composite_type_definition(node.jjtGetChild(currentChild));
		}
	}

	private static void type_mark(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTTYPE_SUBTYPE_NAME)
		{
			type_subtype_name(node.jjtGetChild(currentChild));
		}
	}

	private static void unconstrained_array_definition(Node node)
	{
		int currentChild = 0;

		writeToken("ARRAY");
	
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSUME_LEFT_PAREN)
		{
			consume_left_paren(node.jjtGetChild(currentChild));
			++currentChild;
		}
	
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTINDEX_SUBTYPE_DEFINITION)
		{
			index_subtype_definition(node.jjtGetChild(currentChild));
			++currentChild;
		}

		while(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTINDEX_SUBTYPE_DEFINITION)
		{
			writeToken(",");
			index_subtype_definition(node.jjtGetChild(currentChild));
			++currentChild;
		}
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSUME_RIGHT_PAREN)
		{
			consume_right_paren(node.jjtGetChild(currentChild));
			++currentChild;
		}
		writeToken("OF");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTELEMENT_SUBTYPE_INDICATION)
		{
			element_subtype_indication(node.jjtGetChild(currentChild));
			++currentChild;
		}
	}

	private static void use_clause(Node node)
	{
		int currentChild = 0;

		increaseIndent();

		writeToken("USE");
		
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSELECTED_NAME)
		{
			selected_name(node.jjtGetChild(currentChild));
			++currentChild;
		}
	
		while(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSELECTED_NAME)
		{
			writeToken(",");
			selected_name(node.jjtGetChild(currentChild));
			++currentChild;
		}
	
		writeToken(";");
		decreaseIndent();
	}

	private static void variable_assignment_statement(Node node)
	{
		int currentChild = 0;
		
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTLABEL)
		{
			writeToken(":");
			label(node.jjtGetChild(currentChild++));
		}
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTTARGET)
			target(node.jjtGetChild(currentChild++)); 
		writeToken(":=");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTEXPRESSION)
			expression(node.jjtGetChild(currentChild++));
		writeToken(";");
	}
	
	/*private static void variable_assignment_statement(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTLABEL)
		{
			label(node.jjtGetChild(currentChild));
			writeToken(":");
			++currentChild;
		}

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTTARGET)
		{
			++currentChild;
			if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTNAME)
			{
				if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSIMPLE_NAME)
				{
					simple_name(node.jjtGetChild(currentChild));
					++currentChild;
				}
				else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTOPERATOR_SYMBOL)
				{
					operator_symbol(node.jjtGetChild(currentChild));
					++currentChild;
				}

				while(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTNAME_EXTENSION)
				{
					if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSIGNATURE)
					{
						signature(node.jjtGetChild(currentChild));
						++currentChild;
						writeToken("'");
						if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTATTRIBUTE_DESIGNATOR)
						{
							attribute_designator(node.jjtGetChild(currentChild));
							++currentChild;
						
							if((currentChild+2) < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSUME_LEFT_PAREN && node.jjtGetChild(currentChild+1).getId() == VhdlParserTreeConstants.JJTEXPRESSION && node.jjtGetChild(currentChild+2).getId() == VhdlParserTreeConstants.JJTCONSUME_RIGHT_PAREN)
							{
								consume_left_paren(node.jjtGetChild(currentChild));
								++currentChild;
								expression(node.jjtGetChild(currentChild));
								++currentChild;
								consume_right_paren(node.jjtGetChild(currentChild));
								++currentChild;
							}
						}
					}
					else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSUFFIX)
					{
						writeToken(".");
						suffix(node.jjtGetChild(currentChild));
						++currentChild;
					}
					else if((currentChild+1) < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSUME_LEFT_PAREN && node.jjtGetChild(currentChild+1).getId() == VhdlParserTreeConstants.JJTDISCRETE_RANGE)
					{
						consume_left_paren(node.jjtGetChild(currentChild));
						++currentChild;
						discrete_range(node.jjtGetChild(currentChild));
						++currentChild;
						consume_right_paren(node.jjtGetChild(currentChild));
						++currentChild;
					}
					else if((currentChild+1) < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSUME_LEFT_PAREN && node.jjtGetChild(currentChild+1).getId() == VhdlParserTreeConstants.JJTEXPRESSION)
					{
						consume_left_paren(node.jjtGetChild(currentChild));
						++currentChild;
						expression(node.jjtGetChild(currentChild));
						++currentChild;
						
						while(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTEXPRESSION)
						{
							writeToken(",");
							expression(node.jjtGetChild(currentChild));
							++currentChild;
						}
						
						if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONSUME_RIGHT_PAREN)
						{
							consume_right_paren(node.jjtGetChild(currentChild));
							++currentChild;
						}
					}
				}
			}
			else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTAGGREGATE)
			{
				aggregate(node.jjtGetChild(currentChild));
				++currentChild;
			}
		}
		
		writeToken(":=");

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTEXPRESSION)
		{
			expression(node.jjtGetChild(currentChild));
			++currentChild;
		}
		writeToken(";");
	}*/

	private static void variable_declaration(Node node)
	{
		int currentChild = 0;

		writeToken("VARIABLE");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTIDENTIFIER_LIST)
		{
			identifier_list(node.jjtGetChild(currentChild));
			++currentChild;
		}
		writeToken(":");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSUBTYPE_INDICATION)
		{
			subtype_indication(node.jjtGetChild(currentChild));
		}
		writeToken(";");
	}

	private static void wait_statement(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTLABEL)
		{
			label(node.jjtGetChild(currentChild));
			writeToken(":");
			++currentChild;
		}
		writeToken("WAIT");
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSENSITIVITY_CLAUSE)
		{
			sensitivity_clause(node.jjtGetChild(currentChild));
			++currentChild;
		}
		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTCONDITION_CLAUSE)
		{
			condition_clause(node.jjtGetChild(currentChild));
		}
		writeToken(";");
	}

	private static void waveform(Node node)
	{
		int currentChild = 0;

		if(node.jjtGetNumChildren() == 0)
		{
			writeToken("UNAFFECTED");
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTWAVEFORM_ELEMENT)
		{
			waveform_element(node.jjtGetChild(currentChild));
		}
	}

	private static void waveform_element(Node node)
	{
		int currentChild = 0;

		if(node.jjtGetNumChildren() == 0)
		{
			writeToken("NULL");
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTVALUE_EXPRESSION)
		{
			value_expression(node.jjtGetChild(currentChild));
		}
	}

	private static void block_label(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTLABEL)
		{
			label(node.jjtGetChild(currentChild)); 
		}
	}

	private static void block_statement_label(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTLABEL)
		{
			label(node.jjtGetChild(currentChild)); 
		}
	}

	private static void case_label(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTLABEL)
		{
			label(node.jjtGetChild(currentChild)); 
		}
	}

	private static void generate_label(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTLABEL)
		{
			label(node.jjtGetChild(currentChild)); 
		}
	}

	private static void generate_statement_label(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTLABEL)
		{
			label(node.jjtGetChild(currentChild)); 
		}
	}

	private static void if_label(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTLABEL)
		{
			label(node.jjtGetChild(currentChild)); 
		}
	}

	private static void instantiation_label(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTLABEL)
		{
			label(node.jjtGetChild(currentChild)); 
		}
	}

	private static void loop_label(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTLABEL)
		{
			label(node.jjtGetChild(currentChild)); 
		}
	}

	private static void process_label(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTLABEL)
		{
			label(node.jjtGetChild(currentChild)); 
		}
	}

	private static void architecture_simple_name(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSIMPLE_NAME)
		{
			simple_name(node.jjtGetChild(currentChild)); 
		}
	}

	private static void attribute_simple_name(Node node)
	{
		int currentChild = 0;

		if(node.jjtGetNumChildren() == 0)
		{
			writeToken("RANGE");
		}
		else if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSIMPLE_NAME)
		{
			simple_name(node.jjtGetChild(currentChild)); 
		}
	}

	private static void component_simple_name(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSIMPLE_NAME)
		{
			simple_name(node.jjtGetChild(currentChild)); 
		}
	}

	private static void configuration_simple_name(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSIMPLE_NAME)
		{
			simple_name(node.jjtGetChild(currentChild)); 
		}
	}

	private static void element_simple_name(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSIMPLE_NAME)
		{
			simple_name(node.jjtGetChild(currentChild)); 
		}
	}

	private static void entity_simple_name(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSIMPLE_NAME)
		{
			simple_name(node.jjtGetChild(currentChild)); 
		}
	}

	private static void package_simple_name(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSIMPLE_NAME)
		{
			simple_name(node.jjtGetChild(currentChild)); 
		}
	}

	private static void architecture_name(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTNAME)
		{
			name(node.jjtGetChild(currentChild));         
		}
	}

	private static void entity_name(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTNAME)
		{
			name(node.jjtGetChild(currentChild));        
		}
	}

	private static void function_name(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTNAME)
		{
			name(node.jjtGetChild(currentChild));
		}
	}

	private static void configuration_name(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTNAME)
		{
			name(node.jjtGetChild(currentChild));
		}
	}

	private static void component_name(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTNAME)
		{
			name(node.jjtGetChild(currentChild));
		}
	}

	private static void generic_name(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTNAME)
		{
			name(node.jjtGetChild(currentChild));
		}
	}

	private static void parameter_name(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTNAME)
		{
			name(node.jjtGetChild(currentChild));
		}
	}

	private static void port_name(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTNAME)
		{
			name(node.jjtGetChild(currentChild));
		}
	}

	private static void procedure_name(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTNAME)
		{
			name(node.jjtGetChild(currentChild));
		}
	}

	private static void range_attribute_name(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTNAME)
		{
			name(node.jjtGetChild(currentChild));
		}
	}

	private static void signal_name(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTNAME)
		{
			name(node.jjtGetChild(currentChild));
		}
	}

	private static void type_subtype_name(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTNAME)
		{
			name(node.jjtGetChild(currentChild));
		}
	}

	private static void record_type_simple_name(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSIMPLE_NAME)
		{
			simple_name(node.jjtGetChild(currentChild));
		}
	}

	private static void variable_name(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTNAME)
		{
			name(node.jjtGetChild(currentChild));
		}
	}

	private static void architecture_identifier(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTIDENTIFIER)
		{
			identifier(node.jjtGetChild(currentChild));
		}
	}

	private static void static_expression(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTEXPRESSION)
		{
			expression(node.jjtGetChild(currentChild));
		}
	}

	private static void boolean_expression(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTEXPRESSION)
		{
			expression(node.jjtGetChild(currentChild));
		}
	}

	private static void guard_expression(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTEXPRESSION)
		{
			expression(node.jjtGetChild(currentChild));
		}
	}

	private static void value_expression(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTEXPRESSION)
		{
			expression(node.jjtGetChild(currentChild));
		}
	}

	private static void parameter_association_list(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTASSOCIATION_LIST)
		{
			association_list(node.jjtGetChild(currentChild));
		}
	}

	private static void port_association_list(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTASSOCIATION_LIST)
		{
			association_list(node.jjtGetChild(currentChild));
		}
	}

	private static void generic_association_list(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTASSOCIATION_LIST)
		{
			association_list(node.jjtGetChild(currentChild));
		}
	}

	private static void parameter_interface_list(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTINTERFACE_VARIABLE_DECLARATION)
		{
			interface_variable_declaration(node.jjtGetChild(currentChild));
			++currentChild;
		}
		while(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTINTERFACE_VARIABLE_DECLARATION)
		{
			writeToken(";");
			interface_variable_declaration(node.jjtGetChild(currentChild));
			++currentChild;
		}
	}

	private static void port_interface_list(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTINTERFACE_SIGNAL_DECLARATION)
		{
			interface_signal_declaration(node.jjtGetChild(currentChild));
			++currentChild;
		}
		while(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTINTERFACE_SIGNAL_DECLARATION)
		{
			writeToken(";");
			interface_signal_declaration(node.jjtGetChild(currentChild));
			++currentChild;
		}
	}

	private static void formal_port_clause(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTPORT_CLAUSE)
		{
			port_clause(node.jjtGetChild(currentChild));
		}
	}

	private static void local_port_clause(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTPORT_CLAUSE)
		{
			port_clause(node.jjtGetChild(currentChild));
		}
	}

	private static void formal_generic_clause(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTGENERIC_CLAUSE)
		{
			generic_clause(node.jjtGetChild(currentChild));
		}
	}

	private static void local_generic_clause(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTGENERIC_CLAUSE)
		{
			generic_clause(node.jjtGetChild(currentChild));
		}
	}

	private static void element_subtype_indication(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSUBTYPE_INDICATION)
		{
			subtype_indication(node.jjtGetChild(currentChild));
		}
	}

	private static void discrete_subtype_indication(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTSUBTYPE_INDICATION)
		{
			subtype_indication(node.jjtGetChild(currentChild));
		}
	}

	private static void loop_parameter_specification(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTPARAMETER_SPECIFICATION)
		{
			parameter_specification(node.jjtGetChild(currentChild));
		}
	}

	private static void generate_parameter_specification(Node node)
	{
		int currentChild = 0;

		if(currentChild < node.jjtGetNumChildren() && node.jjtGetChild(currentChild).getId() == VhdlParserTreeConstants.JJTPARAMETER_SPECIFICATION)
		{
			parameter_specification(node.jjtGetChild(currentChild));
		}
	}

	private static void consume_left_paren(Node node)
	{
		writeToken("(");
	}

	private static void consume_right_paren(Node node)
	{
		writeToken(")");
	}
}
