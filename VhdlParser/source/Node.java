/* All AST nodes must implement this interface.  It provides basic
   machinery for constructing the parent and child relationships
   between nodes. */

public interface Node
{
  /** This method is called after the node has been made the current
    node.  It indicates that child nodes can now be added to it. */
  public void jjtOpen();

  /** This method is called after all the child nodes have been
    added. */
  public void jjtClose();

  /** This pair of methods are used to inform the node of its
    parent. */
  public void jjtSetParent(Node n);
  public Node jjtGetParent();

  /** This method tells the node to add its argument to the node's
    list of children.  */
  public void jjtAddChild(Node n, int i);

  /** This method returns a child node.  The children are numbered
     from zero, left to right. */
  public Node jjtGetChild(int i);

  /** Return the number of children the node has. */
  int jjtGetNumChildren();

  public int getId();


  /**
   * Procedures that perform semantic checks...
   */
  public void Check();
  public void CheckSIWGLevel1();

  // Make a copy of the node
  public Node clone();

  // Get this node's index in its parent's child list
  public int jjtIndexInParent();

  // Does the child have an ancestor of the given ID
  public boolean jjtHasParentOfID(int id);

  // Insert a child into the node's child list
  public boolean jjtInsertChildAt(Node n, int index);

  // Remove the child at the specified index
  public void jjtRemoveChild(int index);

  
}
