// ALENGTH.java, created Wed Aug 26 18:58:09 1998 by cananian
// Copyright (C) 1998 C. Scott Ananian <cananian@alumni.princeton.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.IR.Quads;

import harpoon.ClassFile.*;
import harpoon.Temp.Temp;
import harpoon.Temp.TempMap;
import harpoon.Util.Util;

/**
 * <code>ALENGTH</code> represents an array length query.
 * 
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: ALENGTH.java,v 1.1.2.4 1998-12-17 21:38:35 cananian Exp $
 * @see ANEW
 * @see AGET
 * @see ASET
 */
public class ALENGTH extends Quad {
    /** The Temp in which to store the array length. */
    protected Temp dst;
    /** The array reference to query. */
    protected Temp objectref;
    
    /** Creates a <code>ALENGTH</code> representing an array length
     *  query.
     * @param dst
     *        the <code>Temp</code> in which to store the array length.
     * @param objectref
     *        the <code>Temp</code> holding the array reference to query.
     */
    public ALENGTH(QuadFactory qf, HCodeElement source,
		   Temp dst, Temp objectref) {
	super(qf, source);
	this.dst = dst;
	this.objectref = objectref;
	// VERIFY legality of this ALENGTH
	Util.assert(dst!=null && objectref!=null);
    }
    /** Returns the destination <code>Temp</code>. */
    public Temp dst() { return dst; }
    /** Returns the <code>Temp</code> holding the array reference to query. */
    public Temp objectref() { return objectref; }

    /** Returns the Temp defined by this Quad. 
     * @return the <code>dst</code> field. */
    public Temp[] def() { return new Temp[] { dst }; }
    /** Returns the Temp used by this Quad.
     * @return the <code>objectref</code> field. */
    public Temp[] use() { return new Temp[] { objectref }; }

    public int kind() { return QuadKind.ALENGTH; }

    public Quad rename(QuadFactory qqf, TempMap tm) {
	return new ALENGTH(qqf, this,
			   map(tm,dst), map(tm,objectref));
    }
    /** Rename all used variables in this Quad according to a mapping. */
    void renameUses(TempMap tm) {
	objectref = tm.tempMap(objectref);
    }
    /** Rename all defined variables in this Quad according to a mapping. */
    void renameDefs(TempMap tm) {
	dst = tm.tempMap(dst);
    }

    public void visit(QuadVisitor v) { v.visit(this); }

    /** Returns a human-readable representation of this quad. */
    public String toString() {
	return dst.toString() + " = ALENGTH " + objectref;
    }
}
