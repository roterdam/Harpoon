// OpLocalVariable.java, created by cananian
// Copyright (C) 1998 C. Scott Ananian <cananian@alumni.princeton.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.IR.Bytecode;

import harpoon.ClassFile.*;

/**
 * <code>OpLocalVariable</code> represents an operand of a java bytecode
 * instruction which indicates a local variable index.
 *
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: OpLocalVariable.java,v 1.2 1998-10-11 03:01:16 cananian Exp $
 */
public class OpLocalVariable extends Operand {
  int index;
  /** Create an <code>OpLocalVariable</code> given the index of the 
   *  local variable referenced. */
  public OpLocalVariable(int local_variable_index) {
    this.index = local_variable_index;
  }
  /** Get the local variable index */
  public int getIndex() { return index; }
  /** Return a human-readable representation of this Operand. */
  public String toString() { return "lv_"+index; }
}
