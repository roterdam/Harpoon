// DerivationGenerator.java, created Tue Feb  1 01:31:34 2000 by cananian
// Copyright (C) 2000 C. Scott Ananian <cananian@alumni.princeton.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.IR.Tree;

import harpoon.ClassFile.HClass;
import harpoon.Analysis.Maps.Derivation.DList;
import harpoon.Analysis.Maps.TypeMap.TypeNotKnownException;
import harpoon.Temp.Temp;
import harpoon.Util.Util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
/**
 * <code>DerivationGenerator</code> takes a partial map of
 * <code>Tree.Exp</code>s-to-type/derivations and answers
 * type/derivation queries as if the complete map were
 * present.  In particular, only <code>Tree.TEMP</code>s
 * and <code>Tree.MEM</code>s are typically explicitly
 * typed in the map; types for the rest of the
 * <code>Tree.Exp</code>s can be inferred from these.
 * 
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: DerivationGenerator.java,v 1.1.2.2 2000-02-05 22:32:05 cananian Exp $
 */
class DerivationGenerator implements TreeDerivation {
    /** private partial type map */
    private Map dtM = new HashMap();
    
    /** Creates a <code>DerivationGenerator</code>. */
    DerivationGenerator() { }

    /** internal structure of type/derivation information */
    private static class TypeAndDerivation {
	/** non-null for base pointers */
	public final HClass type;
	/** non-null for derived pointers */ 
	public final DList derivation;
	/** for some base pointers, indicates the temp containing this type */
	public final Temp temp;
	// public constructors
	TypeAndDerivation(HClass type) { this(type, null, null); }
	TypeAndDerivation(DList deriv) { this(null, deriv, null); }
	TypeAndDerivation(HClass type, Temp temp) { this(type, null, temp); }
	/** private constructor */
	private TypeAndDerivation(HClass type, DList derivation, Temp temp) {
	    Util.assert(type!=null ^ derivation!=null);
	    this.type = type;
	    this.derivation = derivation;
	    this.temp = temp;
	}
    }

    // public interface
    public HClass typeMap(Exp exp) throws TypeNotKnownException {
	TypeAndDerivation tad = getDT(exp);
	if (tad==null) throw new TypeNotKnownException(exp, null);
	if (!dtM.containsKey(exp)) dtM.put(exp, tad); // cache this result.
	return tad.type;
    }
    public DList  derivation(Exp exp) throws TypeNotKnownException {
	TypeAndDerivation tad = getDT(exp);
	if (tad==null) throw new TypeNotKnownException(exp, null);
	if (!dtM.containsKey(exp)) dtM.put(exp, tad); // cache this result.
	return tad.derivation;
    }

    // allow implementations to add explicit type/derivation information
    void putType(Exp exp, HClass type) {
	Util.assert(exp!=null && type!=null);
	Util.assert(!dtM.containsKey(exp));
	dtM.put(exp, new TypeAndDerivation(type));
    }
    void putTypeAndTemp(Exp exp, HClass type, Temp temp) {
	Util.assert(exp!=null && type!=null && temp!=null);
	Util.assert(!dtM.containsKey(exp));
	dtM.put(exp, new TypeAndDerivation(type, temp));
    }
    void putDerivation(Exp exp, DList derivation) {
	Util.assert(exp!=null && derivation!=null);
	Util.assert(!dtM.containsKey(exp));
	dtM.put(exp, new TypeAndDerivation(derivation));
    }

    // private interface
    private TypeAndDerivation getDT(Exp exp) {
	if (dtM.containsKey(exp)) return (TypeAndDerivation) dtM.get(exp);
	return new DTVisitor(exp).tad;
    }
    private class DTVisitor extends TreeVisitor {
	public TypeAndDerivation tad;
	DTVisitor(Exp exp) { exp.accept(this); }
	public void visit(Tree e) { Util.assert(false, "Can't type: "+e); }
	public void visit(CONST e) {
	    // e.type()==Type.POINTER means null constant.
	    this.tad = (e.type() != Type.POINTER) ? type2tad(e.type()) : Void;
	}
	public void visit(ESEQ e) {
	    this.tad = getDT(e.getExp());
	}
	public void visit(MEM e) {
	    Util.assert(e.type() != Type.POINTER, 
			"Can't determine type for MEM.");
	    this.tad = type2tad(e.type());
	}
	public void visit(NAME e) {
	    // be careful with NAMEs of pre-constructed objects
	    this.tad = Void;
	}
	public void visit(BINOP e) {
	    TypeAndDerivation lefttad = getDT(e.getLeft());
	    TypeAndDerivation righttad = getDT(e.getRight());
	    if (e.type() != Type.POINTER)
		this.tad = type2tad(e.type());
	    else switch (e.op) { // this is a pointer operation 
	    case Bop.ADD:
		this.tad = TADadd(getDT(e.getLeft()), getDT(e.getRight()));
		break;
	    default:
		Util.assert(false, "Illegal pointer operation: "+e);
	    }
	}
	public void visit(UNOP e) {
	    if (e.type() != Type.POINTER)
		this.tad = type2tad(e.type());
	    else switch (e.op) { // this is a pointer operation
	    case Uop.NEG:
		this.tad = TADnegate(getDT(e.getOperand()));
		break;
	    default:
		Util.assert(false, "Illegal pointer operation: "+e);
	    }
	}
	public void visit(TEMP e) {
	    Util.assert(e.type != Type.POINTER, 
			"Can't determine type for TEMP.");
	    this.tad = type2tad(e.type());
	}
    }
    // common types.
    private static final TypeAndDerivation 
	Void = new TypeAndDerivation(HClass.Void),
	Int =  new TypeAndDerivation(HClass.Int),
	Long =  new TypeAndDerivation(HClass.Long),
	Float =  new TypeAndDerivation(HClass.Float),
	Double =  new TypeAndDerivation(HClass.Double);
    // utility methods for operating on/generating types and derivations
    private static TypeAndDerivation type2tad(int ty) {
	switch(ty) {
	case Type.INT: return Int;
	case Type.LONG: return Long;
	case Type.FLOAT: return Float;
	case Type.DOUBLE: return Double;
	case Type.POINTER:
	    throw new RuntimeException("Pointers do not have inherent types");
	default:
	    throw new RuntimeException("Unknown type!");
	}
    }
    private static DList makeDL(TypeAndDerivation tad) {
	if (tad.derivation != null) return tad.derivation;
	// have to derive from base pointer.
	Util.assert(tad.type != null);
	Util.assert(tad.type == HClass.Void || !tad.type.isPrimitive());
	Util.assert(tad.temp != null,
		    "Can't derive type if we don't know temp.");
	return new DList(tad.temp, true, null);
    }
    private static TypeAndDerivation TADadd(TypeAndDerivation lefttad,
					    TypeAndDerivation righttad) {
	// if there's a constant, put it on the right.
	if (lefttad.type!=null && lefttad.type.isPrimitive())
	    return TADadd(righttad, lefttad);
	// okay, deal with adding constants (including opaque pointers)
	if (righttad.type!=null && righttad.type.isPrimitive())
	    if (lefttad.type!=null && lefttad.type.isPrimitive())
		return new TypeAndDerivation(HClass.Void); // opaque!
	    else // we're adding a constant to a base or derived pointer.
		return new TypeAndDerivation(makeDL(lefttad));
	// okay, at this point we have two derived pointers to add.
	// deal with possible cancellations by adding them the simple
	// way and then canonicalizing.
	DList result = makeDL(lefttad);
	for (DList dl=makeDL(righttad); dl!=null; dl=dl.next)
	    result = new DList(dl.base, dl.sign, result);
	result = result.canonicalize();
	return (result==null) ? Void : new TypeAndDerivation(result);
    }
    private static TypeAndDerivation TADnegate(TypeAndDerivation tad) {
	return new TypeAndDerivation(DLnegate(makeDL(tad)));
    }
    private static DList DLnegate(DList dl) { // recursive! whee!
	return (dl==null) ? null :
	    new DList(dl.base, !dl.sign, DLnegate(dl.next));
    }
}
