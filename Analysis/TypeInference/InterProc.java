// InterProc.java, created Fri Nov 20 19:21:40 1998 by marinov
package harpoon.Analysis.TypeInference;

import harpoon.IR.QuadSSA.*;
import harpoon.Temp.*;
import java.util.Hashtable;
import java.util.Enumeration;
import harpoon.Util.UniqueFIFO;
import harpoon.Util.Worklist;
import harpoon.ClassFile.*;
import harpoon.Analysis.QuadSSA.ClassHierarchy;
/**
 * <code>InterProc</code>
 * 
 * @author  Darko Marinov <marinov@lcs.mit.edu>
 * @version $Id: InterProc.java,v 1.1.2.1 1998-12-02 08:08:32 marinov Exp $
 */

public class InterProc implements harpoon.Analysis.Maps.SetTypeMap {
    HCode main;
    boolean analyzed = false;
    Hashtable proc = new Hashtable();
    ClassHierarchy ch = null;

    /** Creates an <code>InterProc</code> analyzer. */
    public InterProc(HCode main) { this.main = main; }    
    public InterProc(HCode main, ClassHierarchy ch) { this.main = main; this.ch = ch; }

    public SetHClass setTypeMap(HCode c, Temp t) { 
	analyze();
	return ((IntraProc)proc.get(c.getMethod())).getTempType(t);
    }

    public SetHClass getReturnType(HCode c) {
	analyze();
	return ((IntraProc)proc.get(c.getMethod())).getReturnType();
    }

    public HMethod[] calls(HMethod m) { 
	analyze();
	return ((IntraProc)proc.get(m)).calls();
    }

    Worklist wl;
    void analyze() {
	if (analyzed) return; else analyzed = true;	
	/* main method, the one from which the analysis starts. */
	HMethod m = main.getMethod();
	/* build class hierarchy of classess reachable from main.
	   used for coning, i.e. finding all children of a given class. */
	if (ch==null) ch = new ClassHierarchy(m);
	cc = new ClassCone(ch);
	/* worklist of methods that are to be processed. */
	wl = new UniqueFIFO();
	/* put classinitializers for reachable classes on the worklist. */
	SetHClass[] ep = new SetHClass[0];
	for (Enumeration e = classInitializers(ch); e.hasMoreElements(); ) {
	    HMethod ci = (HMethod)e.nextElement();
	    getIntra(null, ci, ep);
	}       
	/* put the main method on the worklist. */
	HClass[] c = m.getParameterTypes();
	SetHClass[] p = new SetHClass[c.length];
	for (int i=0; i<c.length; i++)
	    p[i] = cone(c[i]);
        getIntra(null, m, p);
	/* use straightforward worklist algorithm. */
	while (!wl.isEmpty()) {
	    IntraProc i = (IntraProc)wl.pull();
	    i.analyze();
	}
    }
 
    ClassCone cc;
    SetHClass cone(HClass c) { return cc.cone(c); }
    
    void reanalyze(IntraProc i) { wl.push(i); }

    IntraProc getIntra(IntraProc c, HMethod m, SetHClass[] p) {
	IntraProc i = (IntraProc)proc.get(m);
	if (i==null) {
	    i = new IntraProc(this, m); 
	    proc.put(m, i);
	    i.addParameters(p);
	    reanalyze(i);
	} else if (i.addParameters(p)) reanalyze(i);
	if (c!=null) i.addCallee(c);
	return i;
    }

    Hashtable instVar = new Hashtable();
    SetHClass getType(HField f, IntraProc i) {
	FieldType t = (FieldType)instVar.get(f);
	if (t==null) {
	    t = new FieldType();
	    instVar.put(f, t);
	} 
	t.addCallee(i);
	return t.getType();
    }   
    void mergeType(HField f, SetHClass s) {
	FieldType t = (FieldType)instVar.get(f);
	if (t==null) {
	    t = new FieldType();
	    instVar.put(f, t);
	}
	if (t.union(s))
	    for (Enumeration e=t.getCallees(); e.hasMoreElements(); )
		reanalyze((IntraProc)e.nextElement());
    }

    Enumeration classInitializers(final ClassHierarchy ch) {
	return new Enumeration() {
	    Enumeration e = ch.classes();
	    HMethod m = null;
	    private void advance() {
		while (e.hasMoreElements()&&(m==null)) {
		    HClass c = (HClass)e.nextElement();
		    m = c.getClassInitializer();
		}
	    }
	    public boolean hasMoreElements() { advance(); return (m!=null); }
	    public Object nextElement() {
		advance();
		if (m!=null) { HMethod n = m; m = null; return n; }
		else return null;
	    }
	};
    }

}
