// Code.java, created Fri Aug  7 13:45:29 1998 by cananian
// Copyright (C) 1998 C. Scott Ananian <cananian@alumni.princeton.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.IR.Quads;

import harpoon.Analysis.QuadSSA.DeadCode;
import harpoon.ClassFile.*;
import harpoon.Util.Util;

/**
 * <code>Quads.QuadSSA</code> is a code view that exposes the details of
 * the java classfile bytecodes in a quadruple format.  Implementation
 * details of the stack-based JVM are hidden in favor of a flat consistent
 * temporary-variable based approach.  The generated quadruples adhere
 * to an SSA form; that is, every variable has exactly one definition,
 * and <code>PHI</code> functions are used where control flow merges.
 * 
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: QuadSSA.java,v 1.1.2.2 1998-12-09 02:16:05 cananian Exp $
 */
public class QuadSSA extends Code /* which extends HCode */ {
    /** The name of this code view. */
    public static final String codename = "quad-ssa";

    /** Creates a <code>Code</code> object from a bytecode object. */
    QuadSSA(harpoon.IR.Bytecode.Code bytecode) 
    {
	super(bytecode.getMethod(), Translate.trans(bytecode));
	CleanUp.cleanup(this); // cleanup null predecessors of phis.
	Peephole.optimize(this); // peephole optimizations.
	FixupFunc.fixup(this); // add phi/sigma functions.
	DeadCode.optimize(this); // get rid of unused phi/sigmas.
    }
    /** 
     * Create a new code object given a quadruple representation
     * of the method instructions.  If <code>addPhi</code> is true,
     * adds phi and sigma functions to the <code>PHI</code> and
     * <code>SIGMA</code> quads in the representations.
     */
    public QuadSSA(HMethod parent, Quad quads, boolean addPhi) {
	super(parent, quads);
	// if addPhi, check that phis and sigmas are empty?
	if (addPhi)
	    FixupFunc.fixup(this);
	DeadCode.optimize(this);
    }
    /** Same as above; with <code>addPhi==false</code>. */
    public QuadSSA(HMethod parent, Quad quads) {
	this(parent, quads, false);
    }

    /** Clone this code representation. The clone has its own
     *  copy of the quad graph. */
    public HCode clone(HMethod newMethod) {
	return new QuadSSA(newMethod, Quad.clone(quads));
    }

    /**
     * Return the name of this code view.
     * @return the string <code>"quad-ssa"</code>.
     */
    public String getName() { return codename; }
    
    public static void register() {
	HCodeFactory f = new HCodeFactory() {
	    public HCode convert(HMethod m) {
		HCode c = m.getCode("bytecode");
		return (c==null)?null:new QuadSSA((harpoon.IR.Bytecode.Code)c);
	    }
	    public String getCodeName() {
		return codename;
	    }
	};
	HMethod.register(f);
    }
}
