// ALENGTH.java, created Wed Aug 26 18:58:09 1998 by cananian
package harpoon.IR.QuadSSA;

import harpoon.ClassFile.*;
import harpoon.Temp.Temp;

/**
 * <code>ALENGTH</code> represents an array length query.
 * 
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: ALENGTH.java,v 1.4 1998-09-11 17:13:57 cananian Exp $
 * @see ANEW
 * @see AGET
 * @see ASET
 */

public class ALENGTH extends Quad {
    /** The Temp in which to store the array length. */
    public Temp dst;
    /** The array reference to query. */
    public Temp objectref;
    
    /** Creates a <code>ALENGTH</code>. */
    public ALENGTH(HCodeElement source,
		   Temp dst, Temp objectref) {
	super(source);
	this.dst = dst;
	this.objectref = objectref;
    }

    /** Returns the Temp defined by this Quad. 
     * @return the <code>dst</code> field. */
    public Temp[] def() { return new Temp[] { dst }; }
    /** Returns the Temp used by this Quad.
     * @return the <code>objectref</code> field. */
    public Temp[] use() { return new Temp[] { objectref }; }

    public void accept(Visitor v) { v.visit(this); }

    /** Returns a human-readable representation of this quad. */
    public String toString() {
	return dst.toString() + " = ALENGTH " + objectref;
    }
}
