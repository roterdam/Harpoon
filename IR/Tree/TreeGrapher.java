// TreeGrapher2.java, created Sun Jul 16 18:23:48 2000 by cananian
// Copyright (C) 2000 C. Scott Ananian <cananian@alumni.princeton.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.IR.Tree;

import harpoon.ClassFile.HCode;
import harpoon.ClassFile.HCodeEdge; 
import harpoon.ClassFile.HCodeElement; 
import harpoon.IR.Properties.CFGrapher; 
import harpoon.Temp.Label;
import harpoon.Temp.LabelList; 
import harpoon.Util.Collections.Factories;
import harpoon.Util.Collections.GenericMultiMap;
import harpoon.Util.Collections.MultiMap;
import harpoon.Util.Util; 

import java.util.ArrayList;
import java.util.Collection; 
import java.util.HashMap; 
import java.util.Iterator; 
import java.util.List;
import java.util.Map; 

/**
 * <code>TreeGrapher</code> provides a means to externally associate
 * control-flow graph information with elements of a canonical tree.
 * 
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: TreeGrapher.java,v 1.3.2.1 2002-02-27 08:36:48 cananian Exp $
 */
class TreeGrapher extends CFGrapher {
    Tree firstElement = null;
    List lastElements = new ArrayList();
    final MultiMap predMap = new GenericMultiMap(Factories.arrayListFactory);
    final MultiMap succMap = new GenericMultiMap(Factories.arrayListFactory);
   /** Class constructor.  Don't call this directly -- use
    *  the getGrapher() method in <code>harpoon.IR.Tree.Code</code> instead.
    */ 
    TreeGrapher(Code code) {
	// Tree grapher only works on canonical trees. 
	assert code.getName().equals("canonical-tree");
	Edger e = new Edger(code);
	// done.
    }
    public HCodeElement getFirstElement(HCode hcode) { return firstElement; }
    public HCodeElement[] getLastElements(HCode hcode) {
	return (HCodeElement[])
	    lastElements.toArray(new Tree[lastElements.size()]);
    }
    public Collection predC(HCodeElement hc) { return predMap.getValues(hc); }
    public Collection succC(HCodeElement hc) { return succMap.getValues(hc); }

    /** this class does the real work of the grapher */
    private class Edger {
	/** maps Temp.Labels to IR.Tree.LABELs */
	private final Map labelmap = new HashMap();
	/** Look up an IR.Tree.LABEL given a Temp.Label. */
	private LABEL lookup(Label l) {
	    assert labelmap.containsKey(l);
	    return (LABEL)labelmap.get(l);
	}
	/** Add a <from, to> edge to the predMap and succMap */
	private void addEdge(final Tree from, final Tree to) {
	    HCodeEdge hce = new HCodeEdge() {
		public HCodeElement from() { return from; }
		public HCodeElement to() { return to; }
		public String toString() { return "Edge from "+from+" to "+to;}
	    };
	    predMap.add(to, hce);
	    succMap.add(from, hce);
	}
	/** the constructor does the analysis. */
	Edger(Code code) {
	    // collect labels from tree
	    TreeVisitor labelv = new TreeVisitor() {
		public void visit(Tree e) { /* no op */ }
		public void visit(LABEL l) { labelmap.put(l.label, l); }
	    };
	    for (Iterator it=code.getElementsI(); it.hasNext(); )
		((Tree)it.next()).accept(labelv);
	    // now make all the edges
	    TreeVisitor edgev = new TreeVisitor() {
		Tree last = null;
		void linkup(Stm s, boolean canFallThrough) {
		    if (firstElement==null) firstElement = s;
		    if (last!=null) addEdge(last, s);
		    last = canFallThrough ? s : null;
		    if (succMap.getValues(s).size()==0)
			lastElements.add(s);
		}
		public void visit(Tree t) { /* ignore */ }
		public void visit(Stm s) { linkup(s, true); }
		public void visit(CALL c) {
		    // edge to handler; also fall-through.
		    addEdge(c, lookup(c.getHandler().label));
		    linkup(c, true);
		}
		public void visit(CJUMP c) {
		    // edges to iftrue and iffalse; no fall-through.
		    addEdge(c, lookup(c.iffalse));
		    addEdge(c, lookup(c.iftrue));
		    linkup(c, false);
		}
		public void visit(ESEQ e) {
		    assert false : "Not in canonical form!";
		}
		public void visit(JUMP j) {
		    // edges to targets list. no fall-through.
		    assert j.targets!=null : "JUMP WITH NO TARGETS!";
		    for (LabelList ll=j.targets; ll!=null; ll=ll.tail)
			addEdge(j, lookup(ll.head));
		    linkup(j, false);
		}
		public void visit(RETURN r) {
		    // no fall-through.
		    linkup(r, false);
		}
		public void visit(SEQ s) { /* ignore this guy! */ }
		public void visit(THROW t) {
		    // no fall-through.
		    linkup(t, false);
		}
	    };
	    // iterate in depth-first pre-order:
	    for (Iterator it=code.getElementsI(); it.hasNext(); )
		((Tree)it.next()).accept(edgev);
	    // done!
	}
    }
}
