// Runtime.java, created Wed Sep  8 14:30:28 1999 by cananian
// Copyright (C) 1999 C. Scott Ananian <cananian@alumni.princeton.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Backend.Runtime1;

import harpoon.Analysis.ClassHierarchy;
import harpoon.Analysis.Quads.CallGraph;
import harpoon.Backend.Generic.Frame;
import harpoon.Backend.Maps.ClassDepthMap;
import harpoon.Backend.Maps.NameMap;
import harpoon.ClassFile.HClass;
import harpoon.ClassFile.HCode;
import harpoon.ClassFile.HCodeFactory;
import harpoon.ClassFile.HMethod;
import harpoon.ClassFile.Linker;
import harpoon.Util.Util;

import java.lang.reflect.Modifier;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
/**
 * <code>Runtime1.Runtime</code> is a no-frills implementation of the runtime
 * abstract class.
 * 
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: Runtime.java,v 1.1.2.23 2000-03-27 20:37:34 cananian Exp $
 */
public class Runtime extends harpoon.Backend.Generic.Runtime {
    final Frame frame;
    final HMethod main;
    final ClassHierarchy ch;
    final ObjectBuilder ob;
    final List staticInitializers;
    
    /** Creates a new <code>Runtime1.Runtime</code>. */
    public Runtime(Frame frame, AllocationStrategy as,
		   HMethod main, ClassHierarchy ch, CallGraph cg,
		   boolean prependUnderscore) {
	super(new Object[] { frame, as, ch, new Boolean(prependUnderscore) });
	this.frame = frame;
	this.main = main;
	this.ch = ch;
	this.ob = new harpoon.Backend.Runtime1.ObjectBuilder(this);
	this.staticInitializers =
	    new harpoon.Backend.Analysis.InitializerOrdering(ch, cg).sorted;
    }
    // protected initialization methods.
    protected NameMap initNameMap(Object closure) {
	boolean prependUnderscore = ((Boolean) ((Object[])closure)[3])
	    .booleanValue();
	return new harpoon.Backend.Maps.DefaultNameMap(prependUnderscore);
    }
    protected TreeBuilder initTreeBuilder(Object closure) {
	Frame f = (Frame) ((Object[])closure)[0];
	AllocationStrategy as = (AllocationStrategy) ((Object[])closure)[1];
	ClassHierarchy ch = (ClassHierarchy) ((Object[])closure)[2];
	return new harpoon.Backend.Runtime1.TreeBuilder(this, f.getLinker(),
							ch, as,
							f.pointersAreLong());
    }

    public HCodeFactory nativeTreeCodeFactory(final HCodeFactory hcf) {
	final HMethod HMobjAclone =
	    frame.getLinker().forDescriptor("[Ljava/lang/Object;")
	    .getMethod("clone", new HClass[0]);
	Util.assert(hcf.getCodeName().endsWith("tree"));
	return new HCodeFactory() {
	    public String getCodeName() { return hcf.getCodeName(); }
	    public void clear(HMethod m) { hcf.clear(m); }
	    public HCode convert(HMethod m) {
		HCode c = hcf.convert(m);
		// substitute stub for native methods.
		if (c==null && Modifier.isNative(m.getModifiers())) {
		    // careful w/ array clone methods -- alias out all except
		    // java.lang.Object[].clone(). UGLY.  but it works.
		    if (isArrayCloneMethod(m)) {
			if (m.equals(HMobjAclone)) {
			    // collect all other array clone methods.
			    Set all_acm = new HashSet(ch.callableMethods());
			    for (Iterator it=all_acm.iterator(); it.hasNext();)
				if (!isArrayCloneMethod((HMethod)it.next()))
				    it.remove();
			    all_acm.remove(HMobjAclone);
			    HMethod[] acm = (HMethod[]) all_acm.toArray
				(new HMethod[all_acm.size()]);
			    c = new StubCode(m, frame, acm);
			} else { /* skip non-Object[] array clone methods */ }
		    } else c = new StubCode(m, frame);
		}
		return c;
	    }
	};
    }
    private static boolean isArrayCloneMethod(HMethod m) {
	return m.getDeclaringClass().isArray() && m.getName().equals("clone")
	    && m.getDescriptor().equals("()Ljava/lang/Object;");
    }

    /** Return a <code>Set</code> of <code>HMethod</code>s 
     *  and <code>HClass</code>es which are referenced /
     *  callable by code in the runtime implementation (and should
     *  therefore be included in every class hierarchy). */
    public static Collection runtimeCallableMethods(Linker linker) {
	HClass HCsystem = linker.forName("java.lang.System");
	HClass HCstring = linker.forName("java.lang.String");
	HClass HCcharA  = linker.forDescriptor("[C");
	HClass HCthread = linker.forName("java.lang.Thread");
	HClass HCthreadGroup = linker.forName("java.lang.ThreadGroup");
	return Arrays.asList(new Object[] {
	    // implicitly called during startup/shutdown.
	    HCsystem.getMethod("initializeSystemClass", "()V"),
	    HCthread.getConstructor(new HClass[] {
		    HCthreadGroup,
		    linker.forName("java.lang.Runnable"),
		    linker.forName("java.lang.String"),
			}),
	    HCthread.getMethod("exit", new HClass[0]),
	    HCthread.getMethod("getThreadGroup", new HClass[0]),
	    HCthreadGroup.getConstructor(new HClass[0]),
	    HCthreadGroup.getMethod("uncaughtException", new HClass[] {
		    HCthread, linker.forName("java.lang.Throwable")
			}),
	    // this is the actual implementation used for any array
	    // clone method, so hack it into the hierarchy.
	    linker.forDescriptor("[Ljava/lang/Object;")
		.getMethod("clone", new HClass[0]),
	    // jni implementation uses these:
	    linker.forName("java.lang.NoClassDefFoundError")
		.getConstructor(new HClass[] { HCstring }),
	    linker.forName("java.lang.NoSuchMethodError")
		.getConstructor(new HClass[] { HCstring }),
	    linker.forName("java.lang.NoSuchFieldError")
		.getConstructor(new HClass[] { HCstring }),
	    HCstring.getConstructor(new HClass[] { HCcharA }),
	    HCstring.getMethod("length", "()I"),
	    HCstring.getMethod("toCharArray","()[C"),

	    // in java.io implementations
	    linker.forName("java.io.IOException") 
		.getConstructor(new HClass[0]),
	    linker.forName("java.io.IOException")
		.getConstructor(new HClass[] { HCstring }),

	    // in java.lang implementations
	    linker.forName("java.lang.ArrayIndexOutOfBoundsException")
		.getConstructor(new HClass[] { HCstring }),
	    linker.forName("java.lang.ArrayStoreException")
		.getConstructor(new HClass[] { HCstring }),
	    linker.forName("java.util.Properties")
		.getMethod("setProperty", new HClass[] { HCstring, HCstring }),
	    // referenced by name in static initializers for primitive type
	    // wrappers (java.lang.Integer, java.lang.Character, etc)
	    HClass.Boolean, HClass.Byte, HClass.Short, HClass.Int,
	    HClass.Long, HClass.Float, HClass.Double, HClass.Char,
	    // passed to main()
	    linker.forDescriptor("[Ljava/lang/String;"),
	});
    }

    public List classData(HClass hc) {
	// i don't particularly like this solution to generating
	// the needed string constants, but it works.
	harpoon.Backend.Runtime1.TreeBuilder tb =
	    (harpoon.Backend.Runtime1.TreeBuilder) treeBuilder;
	tb.stringSet.removeAll(stringsSeen);
	stringsSeen.addAll(tb.stringSet);
	Set newStrings = new HashSet(tb.stringSet);
	tb.stringSet.clear();

	return Arrays.asList(new Data[] {
	    // new DataGC(frame, hc), // FSK: kkz personally had me do this
	    new DataClaz(frame, hc, ch),
	    new DataInterfaceList(frame, hc, ch),
	    new DataStaticFields(frame, hc),
	    new DataStrings(frame, hc, newStrings),
	    new DataInitializers(frame, hc, staticInitializers),
	    new DataJavaMain(frame, hc, main),
	    new DataReflection1(frame, hc, ch),
	    new DataReflection2(frame, hc, ch, frame.pointersAreLong()),
	});
    }
    final Set stringsSeen = new HashSet();
}
