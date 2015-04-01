public class SimpleNode implements Node, VhdlParserConstants, VhdlParserTreeConstants {
  protected Node parent;
  protected Node[] children;
  protected int id;
  protected String name;

  public SimpleNode(int i) {
    id = i;
  }
  public SimpleNode(VhdlParser p, int i) {
    parser = p;
    id = i;
  }

  public void jjtOpen() {
  }

  public void jjtClose() {
  }
  
  public SimpleNode clone()
  {
	  SimpleNode temp = new SimpleNode(id);
	  
	  if(children != null)
	  {
		  Node c[] = new Node[children.length];
		  
		  // Can't use System.arraycopy because it copies references not values
		  for(int index = children.length - 1; index >= 0; --index)
		  {
			  c[index] = ((SimpleNode)children[index]).clone();
		  }
		  temp.children = c;
	  }
	  temp.parent = parent;
	  temp.first_token = first_token;
	  temp.last_token = last_token;
	  
	  return temp;
  }
  
  public void jjtSetParent(Node n) { parent = n; }
  public Node jjtGetParent() { return parent; }

  public int jjtIndexInParent()
  {
	  if(parent == null)
	  {
		  return -1;
	  }
	  
	  for(int child = parent.jjtGetNumChildren() - 1; child >= 0; --child)
	  {
		  if(((SimpleNode)parent.jjtGetChild(child)) == this)
		  {
			  return child;
		  }
	  }
	  
	  return -1;
  }
  
  public boolean jjtHasParentOfID(int id)
  {
	  Node tempNode = parent;
	  
	  while(tempNode != null)
	  {
		  if(tempNode.getId() == id)
		  {
			  return true;
		  }
		  
		  tempNode = tempNode.jjtGetParent();
	  }
	  
	  return false;
  }
  
  public void jjtAddChild(Node n, int i) {
    if(children == null)
    {
      children = new Node[i + 1];
    }
    else if(i >= children.length)
    {
      Node c[] = new Node[i + 1];
      System.arraycopy(children, 0, c, 0, children.length);
      children = c;
    }
    children[i] = n;
  }

  public boolean jjtInsertChildAt(Node n, int index)
  {
	  // Validate index value
	  if(index < 0 || index > children.length || (children == null && index > 0))
	  {
		  return false;
	  }
	  
	  // Create a new array with room for the new addition
	  if(children == null)
	  {
	      children = new Node[1];
	  }
	  else
	  {
	      Node c[] = new Node[children.length + 1];
	      
	      // Copy the old values around the new node's index
	      System.arraycopy(children, 0, c, 0, index);
	      System.arraycopy(children, index, c, index+1, children.length-index);
	      children = c;
	  }
	  
	  // add the new node to the specified index
	  children[index] = n;
	  
	  return true;
  }
  
  public void jjtRemoveChild(int index)
  {
	  // Validate index value
	  if(children == null || index < 0 || index > (children.length - 1))
	  {
		  return;
	  }
	  
	  Node c[] = new Node[children.length - 1];
      System.arraycopy(children, 0, c, 0, index);
      System.arraycopy(children, index+1, c, index, children.length-index-1);
      children = c;
  }
  
  public Node jjtGetChild(int i) {
    return children[i];
  }

  public int jjtGetNumChildren() {
    return (children == null) ? 0 : children.length;
  }

  /* You can override these two methods in subclasses of SimpleNode to
     customize the way the node appears when the tree is dumped.  If
     your output uses more than one line you should override
     toString(String), otherwise overriding toString() is probably all
     you need to do. */

  public String toString() { return VhdlParserTreeConstants.jjtNodeName[id]; }
  public String toString(String prefix) { return prefix + toString(); }

  /* Override this method if you want to customize how the node dumps
     out its children. */

  public void dump(String prefix) {
    System.out.println(toString(prefix));
    if (children != null) {
      for (int i = 0; i < children.length; ++i) {
	SimpleNode n = (SimpleNode)children[i];
	if (n != null) {
	  n.dump(prefix + " ");
	}
      }
    }
  }

  //
  // Added by Christoph Grimm
  //===============================

  /**
   * An error handler that stores all error messages and
   * warnings and decides which ones have to be displayed depending
   * on the waring level set in command line.
   */
  public static ErrorHandler handler = new ErrorHandler();

  /**
   * A static reference to the parser.
   */
  public static VhdlParser parser;

  /**
   * A static reference to the symbol table
   */
  public static SymbolTable symtab;


  /**
   * Start a new block with a new symbol table. The current table
   * is saved in the variable upper_symtab. 
   */
  public void newBlock()
  {
    SymbolTable new_symtab = new SymbolTable();
    new_symtab.upper_symtab = symtab;
    symtab = new_symtab;
  }

  /**
   * Start a new block wit a new symbol table. The current table
   * is saved in the variable upper_symtab. Furthermore, a first
   * symbol of type type and id id is added.
   */
  public void newBlock(String type, String id)
  {
    SymbolTable new_symtab = new SymbolTable();
    new_symtab.upper_symtab = symtab;
    symtab = new_symtab;
    symtab.addSymbol(new Symbol(id, DEFAULT));
  }

  /**
   * End a block: the current symbol table becomes the upper symbol
   * table.
   */
  public void endBlock()
  {
    symtab = symtab.upper_symtab;
  }

 
  /**
   * Perform all semantic actions and checks, which are necessary
   */
  public void Check()
  {
  }

  /**
   * Report warnings, when SIWG Level 1 is violated
   */
  public void CheckSIWGLevel1()
  {
  }

  public int getId()
  {
    return id;
  }

  Token first_token, last_token;
}

