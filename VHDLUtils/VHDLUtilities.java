// Handy static methods that may be used by the VHDL processing tools to save 
// replication of code and to ease maintenance hassle
package VHDLUtils;

public class VHDLUtilities
{
	public VHDLUtilities()
	{
		;
	}

	// Returns true if the passed string is a number of Boolean value
	public static boolean isNumberOrBoolean(String value)
	{
		if(java.util.regex.Pattern.matches("[0-9a-fA-F]+", value) || java.util.regex.Pattern.matches("\"[0-1]+\"", value))
		{
			return true;
		}

		if(value.equalsIgnoreCase("TRUE") || value.equalsIgnoreCase("FALSE"))
		{
			return true;
		}
		
		return false;
	}
}
