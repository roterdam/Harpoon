// DerivationMap.java, created Tue May 16 22:03:20 2000 by cananian
// Copyright (C) 2000 C. Scott Ananian <cananian@alumni.princeton.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.IR.LowQuad;

import harpoon.Analysis.Maps.Derivation;
import harpoon.Analysis.Maps.Derivation.DList;
import harpoon.Analysis.Maps.TypeMap.TypeNotKnownException;
import harpoon.ClassFile.HClass;
import harpoon.ClassFile.HCodeElement;
import harpoon.Temp.Temp;
import harpoon.Temp.TempMap;
import harpoon.Util.Default;
import harpoon.Util.Util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * <code>DerivationMap</code> is a simple map-based implementation of
 * common <code>Derivation</code> functionality.
 * 
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: DerivationMap.java,v 1.4 2002-04-10 03:04:57 cananian Exp $
 * @see harpoon.IR.Tree.DerivationGenerator
 */
public class DerivationMap implements Derivation {
    /** private type and derivation map */
    private Map dtM = new HashMap();
    
    /** Creates a <code>DerivationMap</code>. */
    public DerivationMap() { }

    /** internal structure of type/derivation information. */
    private static class TypeAndDerivation {
	/** non-null for base pointers */
	public final HClass type;
	/** non-null for derived pointers */ 
	public final DList derivation;
	// public constructors
	TypeAndDerivation(HClass type) { this(type, null); }
	TypeAndDerivation(DList deriv) { this(null, deriv); }
	/** private constructor */
	private TypeAndDerivation(HClass type, DList derivation) {
	    assert type!=null ^ derivation!=null;
	    this.type = type;
	    this.derivation = derivation;
	}
	TypeAndDerivation rename(TempMap tm) {
	    if (this.derivation!=null && tm!=null)
		return new TypeAndDerivation(DList.rename(this.derivation,tm));
	    // no need to create a new object, as tad's are immutable.
	    return this;
	}
    }
    // public interface
    public HClass typeMap(HCodeElement hce, Temp t)
	throws TypeNotKnownException {
	return getDT(hce, t).type;
    }
    public DList  derivation(HCodeElement hce, Temp t)
	throws TypeNotKnownException {
	return getDT(hce, t).derivation;
    }

    // allow implementations to add explicit type/derivation information
    /** Add a mapping from the given <code>Temp</code> <code>t</code>
     *  defined at the given <code>HCodeElement</code> <code>hce</code>
     *  to the given <code>HClass</code> <code>type</code> to this
     *  <code>DerivationMap</code>. */
    public void putType(HCodeElement hce, Temp t, HClass type) {
	assert hce!=null && t!=null && type!=null;
	putDT(hce, t, new TypeAndDerivation(type));
    }
    /** Add a mapping from the given <code>Temp</code> <code>t</code>
     *  defined at the given <code>HCodeElement</code> <code>hce</code>
     *  to the given <code>Derivation.DList</code> <code>derivation</code>
     *  to this <code>DerivationMap</code>. */
    public void putDerivation(HCodeElement hce, Temp t, DList derivation) {
	assert hce!=null && t!=null && derivation!=null;
	putDT(hce, t, new TypeAndDerivation(derivation));
    }
    /** Transfer typing from one place to another. */
    public void update(HCodeElement old_hce, Temp old_t,
		       HCodeElement new_hce, Temp new_t) {
	TypeAndDerivation tad = getDT(old_hce, old_t);
	removeDT(old_hce, old_t);
	putDT(new_hce, new_t, tad);
    }
    /** Transfer typings from one <code>Derivation</code> to another, using
     *  an appropriate <code>TempMap</code>. */
    public void transfer(HCodeElement new_hce,
			 HCodeElement old_hce, Temp[] old_defs,
			 TempMap old2new, Derivation old_deriv) {
	for (int i=0; i<old_defs.length; i++) {
	    Temp old_def = old_defs[i];
	    Temp new_def = old2new==null ? old_def : old2new.tempMap(old_def);
	    DList dl = old_deriv.derivation(old_hce, old_def);
	    if (dl==null)
		putType(new_hce, new_def, old_deriv.typeMap(old_hce,old_def));
	    else
		putDerivation(new_hce, new_def,
			      old2new==null ? dl : DList.rename(dl, old2new));
	}
    }
    // allow implementations to flush old data from the derivation generator
    /** Remove all type and derivation mappings for the given
     *  <code>Temp</code> defined at the given <code>HCodeElement</code>.
     *  Used for memory management purposes. */
    public void remove(HCodeElement hce, Temp t) {
	assert hce!=null && t!=null;
	removeDT(hce, t);
    }

    // private interface
    private TypeAndDerivation getDT(HCodeElement hce, Temp t)
	throws TypeNotKnownException {
	List pair = Default.pair(hce, t);
	if (!dtM.containsKey(pair))
	    throw new TypeNotKnownException(hce, t);
	return (TypeAndDerivation) dtM.get(pair);
    }
    private void putDT(HCodeElement hce, Temp t, TypeAndDerivation tad) {
	List pair = Default.pair(hce, t);
	assert !dtM.containsKey(pair);
	assert tad!=null;
	dtM.put(pair, tad);
    }
    private void removeDT(HCodeElement hce, Temp t) {
	List pair = Default.pair(hce, t);
	dtM.remove(pair);
    }
}