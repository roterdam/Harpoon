// ConstantLong.java, created by cananian
// Copyright (C) 1998 C. Scott Ananian <cananian@alumni.princeton.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.ClassFile.Raw.Constant;

import harpoon.ClassFile.Raw.*;
/** 
 * The <code>CONSTANT_Long_info</code> structure represents eight-byte
 * integer numeric constants.
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: ConstantLong.java,v 1.13 1998-10-11 03:01:15 cananian Exp $
 * @see "The Java Virtual Machine Specification, section 4.4.5"
 * @see Constant
 * @see ConstantDouble
 */
public class ConstantLong extends ConstantValue {
  /** The value of the <code>long</code> constant. */
  public long val;
  
  /** Constructor. */
  ConstantLong(ClassFile parent, ClassDataInputStream in) 
    throws java.io.IOException {
    super(parent);
    val = in.readLong();
  }
  /** Constructor. */
  public ConstantLong(ClassFile parent, long val) { 
    super(parent);
    this.val = val; 
  }

  /** Write to a bytecode file. */
  public void write(ClassDataOutputStream out) throws java.io.IOException {
    out.write_u1(CONSTANT_Long);
    out.writeLong(val);
  }

  /** Returns the value of this constant. */
  public long longValue() { return val; }
  /** Returns the value of this constant, wrapped as a 
   *  <code>java.lang.Long</code>. */
  public Object value() { return new Long(val); }

  /** Create a human-readable representation of this constant. */
  public String toString() {
    return "CONSTANT_Long: "+longValue();
  }
}
