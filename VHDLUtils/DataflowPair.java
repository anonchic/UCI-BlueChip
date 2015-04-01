// Immutable objects that represent the basic information about a dataflow pair
package VHDLUtils;

public class DataflowPair
{
	private String left = "";
	private String right = "";
	private boolean rightIsConstant = false;
	private int time = 0;
	
	public DataflowPair(String target, String driver, int stages)throws Exception
	{
		time = stages;
		rightIsConstant = VHDLUtilities.isNumberOrBoolean(driver);
		
		left = target;
		right = driver;

		if(rightIsConstant && time != 0)
		{
			throw new Exception("Dataflow pairs can't have a delay when the driver is a constant: " + toString());
		}
	}
	
	public DataflowPair(String a, String b, String stages, boolean rightConstant)
	{
		time = Integer.parseInt(stages);
		rightIsConstant = rightConstant;
		
		// Handle slice bounds ModelSim won't work with
		int index;
		// Check for left
		if((index  = a.indexOf("LOG2 ( 8 + 1 ) + 4 - 1")) != -1)
		{
			left = a.substring(0, index) + "6" + a.substring(index + "LOG2 ( 8 + 1 ) + 4 - 1".length());
		}
		else if((index  = a.indexOf("LOG2 ( 8 + 1 ) + 4 + 1")) != -1)
		{
			left = a.substring(0, index) + "8" + a.substring(index + "LOG2 ( 8 + 1 ) + 4 + 1".length());
		}
		else if((index  = a.indexOf("LOG2 ( 8 ) + 3")) != -1)
		{
			left = a.substring(0, index) + "6" + a.substring(index + "LOG2 ( 8 ) + 3".length());
		}
		else if((index  = a.indexOf("LOG2X ( 2 ) - 1")) != -1)
		{
			left = a.substring(0, index) + "0" + a.substring(index + "LOG2X ( 2 ) - 1".length());
		}
		else if((index  = a.indexOf("2 + 1")) != -1)
		{
			left = a.substring(0, index) + "3" + a.substring(index + "2 + 1".length());
		}
		else
		{
			left = a;
		}
		
		// Check for right
		if(rightIsConstant)
		{
			right = b;
		}
		else if((index  = b.indexOf("LOG2 ( 8 + 1 ) + 4 - 1")) != -1)
		{
			right = b.substring(0, index) + "6" + b.substring(index + "LOG2 ( 8 + 1 ) + 4 - 1".length());
		}
		else if((index  = b.indexOf("LOG2 ( 8 + 1 ) + 4 + 1")) != -1)
		{
			right = b.substring(0, index) + "8" + b.substring(index + "LOG2 ( 8 + 1 ) + 4 + 1".length());
		}
		else if((index  = b.indexOf("LOG2 ( 8 ) + 3")) != -1)
		{
			right = b.substring(0, index) + "6" + b.substring(index + "LOG2 ( 8 ) + 3".length());
		}
		else if((index  = b.indexOf("LOG2X ( 2 ) - 1")) != -1)
		{
			right = b.substring(0, index) + "0" + b.substring(index + "LOG2X ( 2 ) - 1".length());
		}
		else if((index  = b.indexOf("2 + 1")) != -1)
		{
			right = b.substring(0, index) + "3" + b.substring(index + "2 + 1".length());
		}
		else
		{
			right = b;
		}
	}
	
	// Returns true if the driver is a signal, false otherwise
	// Determined by non-constants having a path prefix as only
	// variables have these
	public boolean driverIsSignal()
	{
		if(rightIsConstant)
		{
			return false;
		}
		return !driverIsVariable();
	}
	public boolean driverIsVariable()
	{
		if(rightIsConstant)
		{
			return false;
		}
		return (right.indexOf('/') != -1);
	}
	public boolean targetIsSignal()
	{
		return !targetIsVariable();
	}
	public boolean targetIsVariable()
	{
		return (left.indexOf('/') != -1);
	}
	
	// Returns the term in the pair that represents the target of a direct assignment
	public String getTargetTerm()
	{
		return left;
	}
	
	// Returns the driver of the target
	public String getDriverTerm()
	{
		return right;
	}
	
	// Returns true if the driver is a constant value
	public boolean driverIsConstant()
	{
		return rightIsConstant;
	}
	
	// Returns the delay between the source and the target
	public int delay()
	{
		return time;
	}
	
	public String toString()
	{
		return left + ", " + right + ", " + time;
	}
}
