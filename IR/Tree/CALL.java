// CALL.java, created Wed Jan 13 21:14:57 1999 by cananian
// Copyright (C) 1998 C. Scott Ananian <cananian@alumni.princeton.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.IR.Tree;

import harpoon.ClassFile.HCodeElement;
import harpoon.Temp.CloningTempMap;
import harpoon.Util.Util;

import java.util.HashSet;
import java.util.Set;

/**
 * <code>CALL</code> objects are statements which stand for 
 * java method invocations, using our runtime's calling convention.
 * <p>
 * The <code>handler</code> expression is a
 * <code>Tree.NAME</code> specifying the label to which we should return
 * from this call if an exception occurs.  If the called method throws
 * an exception, the throwable object is placed in the <code>Temp</code>
 * specified by <code>retex</code> and a control tranfer to the
 * <code>Label</code> specified by <code>handler</code> occurs.
 * <p>
 * If there is no exception thrown by the callee, then the return
 * value is placed in the <code>Temp</code> specified by
 * <code>retval</code> and execution continues normally.  Note that
 * <code>retval</code> may be null if the called method has void return
 * type.
 * 
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>, based on
 *          <i>Modern Compiler Implementation in Java</i> by Andrew Appel.
 * @version $Id: CALL.java,v 1.1.2.30 2000-01-29 01:27:27 pnkfelix Exp $
 * @see harpoon.IR.Quads.CALL
 * @see INVOCATION
 * @see NATIVECALL
 */
public class CALL extends INVOCATION {
    /** Destination for any exception which the callee might throw.
     *  Must be non-null. */
    private TEMP retex;
    /** Expression indicating the destination to which we should return
     *  if our caller throws an exception. */
    private NAME handler;
    /** Whether this invocation should be performed as a tail call. */
    public boolean isTailCall;

    private CONST nullRetval; 

    /** Create a <code>CALL</code> object. */
    public CALL(TreeFactory tf, HCodeElement source,
		TEMP retval, TEMP retex, Exp func, ExpList args,
		NAME handler, boolean isTailCall) {
	super(tf, source, retval, func, args); 
	Util.assert(retex != null && handler != null);
	Util.assert(retex.tf == tf);
	this.retex = retex; this.handler = handler; 
	this.setRetex(retex); this.setHandler(handler);
	this.setRetval(this.getRetval()); 
	this.setFunc(this.getFunc()); 
	this.setArgs(this.getArgs()); 
	this.isTailCall = isTailCall;
	if (retval == null) { this.nullRetval = new CONST(tf, null); } 
	
	// FSK: debugging hack
	// this.accept(TreeVerifyingVisitor.norepeats());
    }

    public Tree getFirstChild() { 
	TEMP retval = this.getRetval(); 
	return (retval == null) ? this.retex : retval; 
    }
    public TEMP getRetex() { return this.retex; } 
    public NAME getHandler() { return this.handler; } 
  
    public void setRetval(TEMP retval) { 
	super.setRetval(retval); 
	if (retval != null) { 
	    retval.parent  = this;
	    retval.sibling = this.retex; 
	}
    }

    public void setRetex(TEMP retex) { 
	this.retex    = retex; 
	retex.parent  = this; 
	retex.sibling = this.getFunc(); 
	TEMP retval = (TEMP)this.getRetval();
	if (retval != null) { retval.sibling = retex; }
    }

    public void setFunc(Exp func) { 
	super.setFunc(func); 
	func.parent  = this;
	func.sibling = this.handler; 
	this.retex.sibling = func; 
    }

    public void setHandler(NAME handler) { 
	this.handler = handler; 
	handler.parent = this;
	handler.sibling = null; 
	this.getFunc().sibling = handler; 
    }

    public void setArgs(ExpList args) { 
	super.setArgs(args); 
	Exp prev = this.handler, current; 

	prev.sibling = null; 
	for (ExpList e = args; e != null; e = e.tail) { 
	    current        = e.head; 
	    prev.sibling   = current; 
	    current.parent = this;
	    prev           = current;
	}
    }

    public boolean isNative() { return false; }

    public int kind() { return TreeKind.CALL; }

    // FIXME:  this is an ugly hack which should be cleaned up. 
    public ExpList kids() { 
	ExpList result = new ExpList
	    (this.retex, 
	     new ExpList
	     (this.getFunc(), 
	      new ExpList
	      (this.handler,
	       this.getArgs()))); 
	      
	if (this.getRetval() == null) { 
	    result = new ExpList(nullRetval, result); 
	} else { 
	    result = new ExpList(this.getRetval(), result); 
	}
	return result; 
    }

    public Stm build(ExpList kids) { return build(tf, kids); }

    public Stm build(TreeFactory tf, ExpList kids) {
	for (ExpList e = kids; e!=null; e=e.tail)
	    Util.assert(e.head == null || tf == e.head.tf);

	TEMP retval = kids.head.kind() == TreeKind.TEMP ? 
	    (TEMP)kids.head : null; 

	return new CALL(tf, this, 
			retval,                         // retval
			(TEMP)kids.tail.head,           // retex
			kids.tail.tail.head,            // func
			kids.tail.tail.tail.tail,       // args
			(NAME)kids.tail.tail.tail.head, // handler
			isTailCall); 
    }

    /** Accept a visitor */
    public void accept(TreeVisitor v) { v.visit(this); }

    public Tree rename(TreeFactory tf, CloningTempMap ctm) {
        return new CALL(tf, this, 
			(TEMP)this.getRetval().rename(tf, ctm),
			(TEMP)retex.rename(tf, ctm), 
			(Exp)this.getFunc().rename(tf, ctm),
			ExpList.rename(this.getArgs(), tf, ctm),
			(NAME)handler.rename(tf, ctm),
			isTailCall);
  }

    protected Set defSet() { 
	Set def = super.defSet();
	if (retex.kind()==TreeKind.TEMP)  def.add(((TEMP)retex).temp);
	return def;
    }

    protected Set useSet() { 
	Set uses = super.useSet();
	if (!(retex.kind()==TreeKind.TEMP))  uses.addAll(retex.useSet());
	return uses;
    }

    public String toString() {
        ExpList list;
        StringBuffer s = new StringBuffer();
        s.append("CALL(");
	if (this.getRetval()==null) { s.append("null"); } 
	else { s.append("#"+this.getRetval().getID()); } 
	s.append(", #"+retex.getID()+
                 ", #" + this.getFunc().getID() + ", {");
        list = this.getArgs();
        while (list != null) {
            s.append(" #"+list.head.getID());
            if (list.tail != null) {
                s.append(",");
            }
            list = list.tail;
        }
        s.append(" }");
	s.append(", #"+handler.getID());
	s.append(")");
	if (isTailCall) s.append(" [tail call]");
        return new String(s);
    }
}
