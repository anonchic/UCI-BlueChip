/**
 * (c) 1997 Christoph Grimm, J. W. Goethe-University.
 * Beginning of an error handler, should organize different
 * error messages, depending on a chosen level of error reporting. 
 * Still very simple.
 */

class ErrorHandler
{
  public static VhdlParser parser;
  public static int Level1Warnings = 0;
  public static int Errors         = 0;

  /**
   * Print a warning, that a construct is not supported in SIWG Level 1,
   * if this is switched on by command line.
   */
  void WarnLevel1(String w)
  {
    Token t = parser.getToken(0);
    System.err.println("line "+t.beginLine+": "+w+" in SIWG Level 1");
    Level1Warnings++;
  }

  /**
   * Print a warning, that a semantic/syntactic error has occured.
   */
  void Error(String w)
  {
    Token t = parser.getToken(0);
    System.err.println("line "+t.beginLine+": "+w);
    Errors++;
  }

  /**
   * Print a summary, consisting of all numbers of errors and warnings
   * detected.
   */
  void Summary()
  {
  /*
    System.err.println("Incompatibilities with SIWG Level 1 Subset: "+Level1Warnings);
   */
    System.err.println("Syntax errors: "+Errors);
  }
}


