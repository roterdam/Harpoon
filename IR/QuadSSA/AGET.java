// AGET.java, created Wed Aug 26 19:02:57 1998 by cananian
package harpoon.IR.QuadSSA;

import harpoon.ClassFile.*;
import harpoon.Temp.Temp;

/**
 * <code>AGET</code> represents an element fetch from an array object.
 * 
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: AGET.java,v 1.4 1998-09-11 17:13:56 cananian Exp $
 * @see ANEW
 * @see ASET
 * @see ALENGTH
 */

public class AGET extends Quad {
    /** The Temp in which to store the fetched element. */
    public Temp dst;
    /** The array reference. */
    public Temp objectref;
    /** The Temp holding the index of the element to get. */
    public Temp index;

    /** Creates an <code>AGET</code> object. */
    public AGET(HCodeElement source,
		Temp dst, Temp objectref, Temp index) {
	super(source);
	this.dst = dst;
	this.objectref = objectref;
	this.index = index;
    }

    /** Returns the Temp defined by this quad.
     * @return the <code>dst</code> field. */
    public Temp[] def() { return new Temp[] { dst }; }
    /** Returns all the Temps used by this quad.
     * @return the <code>objectref</code> and <code>index</code> fields. */
    public Temp[] use() { return new Temp[] { objectref, index }; }

    public void accept(Visitor v) { v.visit(this); }

    /** Returns a human-readable representation of this quad. */
    public String toString() {
	return dst.toString() + " = AGET " + objectref + "["+index+"]";
    }
}
