// QuadClassHierarchy.java, created Sun Oct 11 13:08:31 1998 by cananian
// Copyright (C) 1998 C. Scott Ananian <cananian@alumni.princeton.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Analysis.Quads;

import harpoon.ClassFile.*;
import harpoon.IR.Quads.*;
import harpoon.Util.ArraySet;
import harpoon.Util.HClassUtil;
import harpoon.Util.Util;
import harpoon.Util.WorkSet;

import java.lang.reflect.Modifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
/**
 * <code>QuadClassHierarchy</code> computes a <code>ClassHierarchy</code>
 * of classes possibly usable starting from some root method using
 * quad form.
 * Native methods are not analyzed.
 *
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: QuadClassHierarchy.java,v 1.1.2.15 2000-01-13 23:47:30 cananian Exp $
 */

public class QuadClassHierarchy extends harpoon.Analysis.ClassHierarchy
    implements java.io.Serializable {
    private Map children = new HashMap();
    private Set methods = new HashSet();

    /** Returns set of all callable methods. 
	@return <code>Set</code> of <code>HMethod</code>s.
     */
    public Set callableMethods() {
	return _unmod_methods;
    }
    private Set _unmod_methods = Collections.unmodifiableSet(methods);

    /** Returns all usable/reachable children of an <code>HClass</code>. */
    public Set children(HClass c) {
	if (children.containsKey(c))
	    return new ArraySet((HClass[]) children.get(c));
	return Collections.EMPTY_SET;
    }
    /** Returns the parent of an <code>HClass</code>. */
    public HClass parent(HClass c) {
	return c.getSuperclass();
    }
    /** Returns the set of all reachable/usable classes. */ 
    public Set classes() {
	if (_classes == null) {
	    _classes = new HashSet();
	    for (Iterator it = children.keySet().iterator(); it.hasNext(); )
		_classes.add(it.next());
	    for (Iterator it = children.values().iterator(); it.hasNext();) {
		HClass[] ch = (HClass[]) it.next();
		for (int i=0; i<ch.length; i++)
		    _classes.add(ch[i]);
	    }
	    _classes = Collections.unmodifiableSet(_classes);
	}
	return _classes;
    }
    private transient Set _classes = null;
    /** Returns the set of all classes instantiated.
	(Actually only the list of classes for which an explicit NEW is found;
	should include list of classes that are automatically created by JVM!) */ 
    public Set instantiatedClasses() {
	return _unmod_insted;
    }
    private Set instedClasses = new HashSet();
    private Set _unmod_insted = Collections.unmodifiableSet(instedClasses);

    /** Returns a human-readable representation of the hierarchy. */
    public String toString() {
	// not the most intuitive representation...
	StringBuffer sb = new StringBuffer("{");
	for (Iterator it = children.keySet().iterator(); it.hasNext(); ) {
	    HClass c = (HClass) it.next();
	    sb.append(c.toString());
	    sb.append("={");
	    for (Iterator it2=children(c).iterator(); it2.hasNext(); ) {
		sb.append(it2.next().toString());
		if (it2.hasNext())
		    sb.append(',');
	    }
	    sb.append('}');
	    if (it.hasNext())
		sb.append(", ");
	}
	sb.append('}');
	return sb.toString();
    }

    ////// hclass objects
    private final HMethod HMstrIntern;
    private final HMethod HMthrStart;
    private final HMethod HMthrRun;
    private QuadClassHierarchy(Linker linker) {
	HMstrIntern = linker.forName("java.lang.String")
	    .getMethod("intern",new HClass[0]);
	HMthrStart = linker.forName("java.lang.Thread")
	    .getMethod("start", new HClass[0]);
	HMthrRun = linker.forName("java.lang.Thread")
	    .getMethod("run", new HClass[0]);
    }

    /** Creates a <code>ClassHierarchy</code> of all classes
     *  reachable/usable from <code>HMethod</code>s in the <code>roots</code>
     *  <code>Collection</code>.  <code>HClass</code>es included in
     *  <code>roots</code> are guaranteed to be included in the
     *  <code>classes()</code> set of this class hierarchy, but they may
     *  not be included in the <code>instantiatedClasses</code> set
     *  (if an instantiation instruction is not found for them).  To
     *  explicitly include an instantiated class in the hierarchy, add
     *  a constructor or non-static method of that class to the
     *  <code>roots</code> <code>Collection</code>.<p> <code>hcf</code>
     *  must be a code factory that generates quad form. */
    public QuadClassHierarchy(Linker linker,
			      Collection roots, HCodeFactory hcf) {
	this(linker); // initialize hclass objects.
	// state.
	// keeps track of methods which are actually invoked at some point.
	Map classMethodsUsed = new HashMap(); // class->set map.
	// keeps track of methods which might be called, if someone gets
	// around to instantiating an object of the proper type.
	Map classMethodsPending = new HashMap(); // class->set map
	// keeps track of all known children of a given class.
	Map classKnownChildren = new HashMap(); // class->set map.
	// keeps track of which methods we've done already.
	Set done = new HashSet();

	WorkSet W = new WorkSet();
	// make initial worklist from roots collection.
	for (Iterator it=roots.iterator(); it.hasNext(); ) {
	    HClass rootC; HMethod rootM; boolean instantiated;
	    // deal with the different types of objects in the roots collection
	    Object o = it.next();
	    if (o instanceof HMethod) {
		rootM = (HMethod) o;
		rootC = rootM.getDeclaringClass();
		// let's assume non-static roots have objects to go with 'em.
		instantiated = !rootM.isStatic();
	    } else { // only HMethods and HClasses in roots, so must be HClass
		rootM = null;
		rootC = (HClass) o;
		instantiated = false;
	    }
	    if (instantiated)
		discoverInstantiatedClass(rootC, W, done,
					  classKnownChildren, classMethodsUsed,
					  classMethodsPending);
	    else
		discoverClass(rootC, W, done,
			      classKnownChildren, classMethodsUsed,
			      classMethodsPending);
	    if (rootM!=null)
		methodPush(rootM, W, done,
			   classMethodsUsed, classMethodsPending);
	}

	// worklist algorithm.
	while (!W.isEmpty()) {
	    HMethod m = (HMethod) W.pull();
	    done.add(m); // mark us done with this method.
	    // This method should be marked as usable.
	    {
		Set s = (Set) classMethodsUsed.get(m.getDeclaringClass());
		Util.assert(s!=null);
		Util.assert(s.contains(m));
	    }
	    // look at the hcode for the method.
	    harpoon.IR.Quads.Code hc = (harpoon.IR.Quads.Code) hcf.convert(m);
	    if (hc==null) { // native or unanalyzable method.
		if(!m.getReturnType().isPrimitive())
		    // be safe; assume the native method can make an object
		    // of its return type.
		    discoverInstantiatedClass(m.getReturnType(), W, done,
				  classKnownChildren, classMethodsUsed,
				  classMethodsPending);
	    } else { // look for CALLs, NEWs, and ANEWs
		for (Iterator it = hc.getElementsI(); it.hasNext(); ) {
		    Quad Q = (Quad) it.next();
		    HClass createdClass=null;
		    if (Q instanceof ANEW)
			createdClass = ((ANEW)Q).hclass();
		    if (Q instanceof NEW)
			createdClass = ((NEW)Q).hclass();
		    if (Q instanceof CONST &&
			!((CONST)Q).type().isPrimitive())
			createdClass = ((CONST)Q).type();
		    // handle creation of a (possibly-new) class.
		    if (createdClass!=null) {
			discoverInstantiatedClass(createdClass, W, done,
						  classKnownChildren,
						  classMethodsUsed,
						  classMethodsPending);
			if (Q instanceof CONST) //string constants use intern()
			    discoverMethod(HMstrIntern, W, done,
					   classKnownChildren,
					   classMethodsUsed,
					   classMethodsPending);
		    }			
		    if (Q instanceof CALL) {
			CALL q = (CALL) Q;
			if (q.isStatic() || !q.isVirtual())
			    discoverSpecial(q.method(), W, done,
					    classKnownChildren,
					    classMethodsUsed,
					    classMethodsPending);
			else
			    discoverMethod(q.method(), W, done,
					   classKnownChildren,
					   classMethodsUsed,
					   classMethodsPending);
		    }
		    // get and set discover classes (don't instantiate, though)
		    if (Q instanceof GET || Q instanceof SET) {
			HField f = (Q instanceof GET)
			    ? ((GET) Q).field() : ((SET) Q).field();
			discoverClass(f.getDeclaringClass(), W, done,
				      classKnownChildren,
				      classMethodsUsed,
				      classMethodsPending);
		    }
		    // make sure we have the class we're testing against
		    // handy.
		    if (Q instanceof INSTANCEOF) {
			discoverClass(((INSTANCEOF)Q).hclass(), W, done,
				      classKnownChildren,
				      classMethodsUsed,
				      classMethodsPending);
		    }
		}
	    }
	} // END worklist.
	
	// build method table from classMethodsUsed.
	for (Iterator it = classMethodsUsed.values().iterator();
	     it.hasNext(); )
	    methods.addAll((Set) it.next());
	// now generate children set from classKnownChildren.
	for (Iterator it = classKnownChildren.keySet().iterator();
	     it.hasNext(); ) {
	    HClass c = (HClass) it.next();
	    Set s = (Set) classKnownChildren.get(c);
	    HClass ch[] = (HClass[]) s.toArray(new HClass[s.size()]);
	    children.put(c, ch);
	}
    }

    /* when we discover a new class nc:
        for each superclass c or superinterface i of this class,
         add all called methods of c/i to worklist of nc, if nc implements.
    */
    private void discoverClass(HClass c, 
			       WorkSet W, Set done,
			       Map ckc, Map cmu, Map cmp) {
	if (ckc.containsKey(c)) return; // not a new class.
	// add to known-children lists.
	Util.assert(!ckc.containsKey(c));
	ckc.put(c, new HashSet());
	Util.assert(!cmu.containsKey(c));
	cmu.put(c, new HashSet());
	Util.assert(!cmp.containsKey(c));
	cmp.put(c, new HashSet());
	// add class initializer (if it exists) to "called" methods.
	HMethod ci = c.getClassInitializer();
	if ((ci!=null) && (!done.contains(ci)))
	    methodPush(ci, W, done, cmu, cmp);
	// mark superclass.
	HClass su = c.getSuperclass();
	if (c.isArray()) { // deal with odd inheritance of array types.
	    // Integer[][]->Number[][]->Object[][]->Object[]->Object
	    HClass base = HClassUtil.baseClass(c);
	    int dims = HClassUtil.dims(c);
	    if (base.getSuperclass()!=null)
		su = HClassUtil.arrayClass(base.getSuperclass(), dims);
	    else
		su = HClassUtil.arrayClass(base, dims-1);
	}
	if (su!=null) { // maybe discover super class?
	    discoverClass(su, W, done, ckc, cmu, cmp);
	    Set knownChildren = (Set) ckc.get(su);
	    knownChildren.add(c); // kC non-null after discoverClass.
	}
	// mark interfaces
	HClass in[] = c.getInterfaces();
	for (int i=0; i<in.length; i++) {
	    discoverClass(in[i], W, done, ckc, cmu, cmp);// discover interface?
	    Set knownChildren = (Set) ckc.get(in[i]);
	    knownChildren.add(c); // kC non-null after discoverClass.
	}
    }
    private void discoverInstantiatedClass(HClass c,
					   WorkSet W, Set done,
					   Map ckc, Map cmu, Map cmp) {
	if (instedClasses.contains(c)) return; else instedClasses.add(c);
	discoverClass(c, W, done, ckc, cmu, cmp);
	// collect superclasses and interfaces.
	// new worklist.
	WorkSet sW = new WorkSet();
	// put superclass and interfaces on worklist.
	HClass su = c.getSuperclass();
	if (su!=null) sW.add(su);
	sW.addAll(Arrays.asList(c.getInterfaces()));

	// first, wake up all methods of this class that were
	// pending instantiation, and clear the pending list.
	List ml = new ArrayList((Set) cmp.get(c)); // copy list.
	for (Iterator it=ml.iterator(); it.hasNext(); )
	    methodPush((HMethod)it.next(), W, done, cmu, cmp);

	// if instantiated,
	// add all called methods of superclasses/interfaces to worklist.
	while (!sW.isEmpty()) {
	    // pull a superclass or superinterface off the list.
	    HClass s = (HClass) sW.pop();
	    // add superclasses/interfaces of this one to local worklist
	    su = s.getSuperclass();
	    if (su!=null) sW.add(su);
	    sW.addAll(Arrays.asList(s.getInterfaces()));
	    // now add called methods of s to top-level worklist.
	    Set calledMethods = new WorkSet((Set)cmu.get(s));
	    calledMethods.addAll((Set)cmp.get(s));
	    for (Iterator it = calledMethods.iterator(); it.hasNext(); ) {
		HMethod m = (HMethod) it.next();
		if (!isVirtual(m)) continue; // not inheritable.
		try {
		    HMethod nm = c.getMethod(m.getName(),
					     m.getDescriptor());
		    if (!done.contains(nm))
			methodPush(nm, W, done, cmu, cmp);
		} catch (NoSuchMethodError nsme) { }
	    }
	}
	// done with this class/interface.
    }
    /* when we hit a method call site (method in class c):
        add method of c and all children of c to worklist.
       if method in interface i:
        add method of c and all implementations of c.
    */
    private void discoverMethod(HMethod m, 
				WorkSet W, Set done,
				Map ckc, Map cmu, Map cmp) {
	if (done.contains(m) || W.contains(m)) return;
	discoverClass(m.getDeclaringClass(), W, done, ckc, cmu, cmp);
	// Thread.start() implicitly causes a call to Thread.run()
	if (m.equals(HMthrStart))
	    discoverMethod(HMthrRun, W, done, ckc, cmu, cmp);
	// mark as pending in its own class.
	Set s = (Set) cmp.get(m.getDeclaringClass());
	s.add(m);
	// now add as 'used' to all instantiated children.
	// (including itself, if its own class has been instantiated)
	WorkSet cW = new WorkSet();
	cW.push(m.getDeclaringClass());
	while (!cW.isEmpty()) {
	    // pull a class from the worklist
	    HClass c = (HClass) cW.pull();
	    // see if we should add method-of-c to method worklist.
	    if (c.isInterface() || instedClasses.contains(c))
		try {
		    HMethod nm = c.getMethod(m.getName(),
					     m.getDescriptor());
		    if (done.contains(nm))
			continue; // nothing new to discover.
		    methodPush(nm, W, done, cmu, cmp);
		} catch (NoSuchMethodError e) { }
	    // add all children to the worklist.
	    Set knownChildren = (Set) ckc.get(c);
	    for (Iterator it = knownChildren.iterator(); it.hasNext(); ) 
		cW.push(it.next());
	}
	// done.
    }

    /* methods invoked with INVOKESPECIAL or INVOKESTATIC... */
    private void discoverSpecial(HMethod m, 
				 WorkSet W, Set done,
				 Map ckc, Map cmu, Map cmp) {
	if (done.contains(m) || W.contains(m)) return;
	discoverClass(m.getDeclaringClass(), W, done, ckc, cmu, cmp);
	// okay, push this method.
	methodPush(m, W, done, cmu, cmp);
    }
    private void methodPush(HMethod m, WorkSet W, Set done, Map cmu, Map cmp) {
	Util.assert(!done.contains(m));
	if (W.contains(m)) return; // already on work list.
	// Add to worklist
	W.add(m);
	// mark this method as used.
	Set s1 = (Set) cmu.get(m.getDeclaringClass());
	s1.add(m);
	// and no longer pending.
	Set s2 = (Set) cmp.get(m.getDeclaringClass());
	s2.remove(m);
    }

    // useful utility method
    private static boolean isVirtual(HMethod m) {
	if (m.isStatic()) return false;
	if (Modifier.isPrivate(m.getModifiers())) return false;
	if (m instanceof HConstructor) return false;
	return true;
    }

    /* ALGORITHM:
       for each class:
        table of all methods used in that class.
        list of known immediate children of the class.
       when we discover a new class nc:
        for each superclass c of this class,
         add all called methods of c to worklist of nc, if nc implements.
       when we hit a method call site (method in class c):
        add method of c and all children of c to worklist.
       for each method on worklist:
        mark method as used in class.
        for each NEW: add possibly new class.
        for each CALL: add possibly new methods.
    */
}
