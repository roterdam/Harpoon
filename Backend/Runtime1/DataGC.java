// DataGC.java, created Thu Jan 13 20:55:26 2000 by kkz
// Copyright (C) 1999 Karen K. Zee <kkz@tesuji.lcs.mit.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Backend.Runtime1;

import harpoon.Analysis.ClassHierarchy;
import harpoon.Analysis.Maps.Derivation;
import harpoon.Backend.Generic.Frame;
import harpoon.Backend.Generic.GCInfo;
import harpoon.Backend.Generic.GCInfo.DLoc;
import harpoon.Backend.Generic.GCInfo.GCPoint;
import harpoon.Backend.Generic.RegFileInfo;
import harpoon.Backend.Generic.RegFileInfo.CommonLoc;
import harpoon.Backend.Generic.RegFileInfo.MachineRegLoc;
import harpoon.Backend.Generic.RegFileInfo.StackOffsetLoc;
import harpoon.Backend.Maps.NameMap;
import harpoon.ClassFile.HClass;
import harpoon.ClassFile.HDataElement;
import harpoon.ClassFile.HMethod;
import harpoon.IR.Tree.ALIGN;
import harpoon.IR.Tree.CONST;
import harpoon.IR.Tree.LABEL;
import harpoon.IR.Tree.SEGMENT;
import harpoon.IR.Tree.Stm;
import harpoon.Temp.Label;
import harpoon.Util.Util;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * <code>DataGC</code> outputs the tables needed by the garbage collector.
 * 
 * @author  Karen K. Zee <kkz@tesuji.lcs.mit.edu>
 * @version $Id: DataGC.java,v 1.1.2.6 2000-02-10 23:54:03 kkz Exp $
 */
public class DataGC extends Data {
    final GCInfo m_gc;
    final NameMap m_nm;
    final TreeBuilder m_tb;
    final int numRegs;
    
    // number of bits in a 32-bit int
    final int INT_BITS = 32;
    // size of descriptor in bits
    final int DESC_BITS = 6;

    /** Creates a <code>DataGC</code>. */
    public DataGC(Frame f, HClass hc) {
        super("gc-data", hc, f);
	Util.assert(f.getGCInfo() != null);
	this.m_gc = f.getGCInfo();
	this.m_nm = f.getRuntime().nameMap;
	this.m_tb = (TreeBuilder) f.getRuntime().treeBuilder;
	this.numRegs = f.getRegFileInfo().maxRegIndex();
	this.root = build();
    }

    private HDataElement build() {
	List stmlist = new ArrayList();
	// switch to the GC segment
	stmlist.add(new SEGMENT(tf, null, SEGMENT.GC));
	// align on word-boundary
	stmlist.add(new ALIGN(tf, null, 4));
	// handle only methods that we've had to generate
	// GCInfo for and do so in the order saved by GCInfo
	for (Iterator it=m_gc.getOrderedMethods(hc).iterator(); it.hasNext(); )
	    stmlist.add(outputGCData((HMethod)it.next()));
	return (HDataElement) Stm.toStm(stmlist);
    }

    // requires: method for which GC points have been calculated
    // modifies: basetableMap
    // effects:  returns statements for outputting base table and
    //           puts numeric mapping of stackOffsetLocs into the
    //           basetableMap
    private Stm outputBaseTable(HMethod hm) {
	// get ordered list of GC points for this method
	List gcps = m_gc.gcPoints(hm);
	// make sure the list is non-null
	Util.assert(gcps != null);
	// first create base table for stack locations
	Set basetableSet = new HashSet();
	// take union of live StackOffsetLocs for all GC points
	for(Iterator it = gcps.iterator(); it.hasNext(); )
	    basetableSet.addAll(((GCPoint)it.next()).liveStackOffsetLocs());
	// make statements
	List stmlist = new ArrayList(basetableSet.size()+2);
	// label base table location
	stmlist.add(new LABEL(tf, null, m_nm.label(hm, "gc_bt"), true));
	// number of entries in base table (int)
	stmlist.add(_DATUM(new CONST(tf, null, basetableSet.size())));
	report("Size of base table = " + basetableSet.size());
	// turn basetableSet into array
	StackOffsetLoc[] basetableArray = 
	    (StackOffsetLoc[])basetableSet.toArray();
	// clear basetableMap
	basetableMap = new HashMap();
	// put new entries into basetableMap
	for(int i=0; i < basetableArray.length; i++) {
	    StackOffsetLoc sol = basetableArray[i];
	    // output stackOffset (int)
	    stmlist.add(_DATUM(new CONST(tf, null, sol.stackOffset())));
	    report("Base table entry: " + sol.stackOffset());
	    basetableMap.put(sol, new Integer(i));
	}
	return Stm.toStm(stmlist);
    }
    private Map basetableMap;

    private Stm outputGCData(HMethod hm) {
	List stmlist = new ArrayList();
	// make base table for method
	stmlist.add(outputBaseTable(hm));
	// convert list of GC points into array
	GCPoint[] gcpArray = (GCPoint[])m_gc.gcPoints(hm).toArray();
	// make entry for each GC point
	for(int i=0; i < gcpArray.length; i++) {
	    // add index entry to GC_INDEX segment
	    stmlist.add(new SEGMENT(tf, null, SEGMENT.GC_INDEX));
	    // location of GC point
	    stmlist.add(_DATUM(gcpArray[i].label()));
	    // location of GC data
	    stmlist.add(_DATUM(m_nm.label(hm, "gcp_index_"+i)));
	    // location of GC base table
	    stmlist.add(_DATUM(m_nm.label(hm, "gc_bt")));
	    // add data entry to GC segment
	    stmlist.add(new SEGMENT(tf, null, SEGMENT.GC));
	    // align on word boundary
	    stmlist.add(new ALIGN(tf, null, 4));
	    // label GC data
	    stmlist.add(new LABEL(tf, null, m_nm.label(hm, "gcp_index_"+i), 
				  true));
	    // output actual data
	    stmlist.add(outputGCPoint(gcpArray[i], gcpArray[i-1]));
	}
	return Stm.toStm(stmlist);
    }

    // bit positions for descriptor
    final int REGS_ARE_ZERO       = 31;
    final int REGS_SAME_AS_PREV   = 30;
    final int STACKLOCS_ARE_ZERO  = 29;
    final int STACK_SAME_AS_PREV  = 28;
    final int DERIVS_ARE_ZERO     = 27;
    final int DERIVS_SAME_AS_PREV = 26;
    // requires: current and previous GC point
    // modifies: nil
    // effects:  returns statements for outputting data relevant
    //           to current GC point
    private Stm outputGCPoint(GCPoint curr, GCPoint prev) {
	// output format: bits 31-26 -- descriptor
	int output = 0;
	boolean needRegs = true;
	Set regs = curr.liveMachineRegLocs();
	if (regs.isEmpty()) {
	    output |= 1 << (REGS_ARE_ZERO - 1);
	    needRegs = false;
	} else if (prev != null && 
		   regs.equals(prev.liveMachineRegLocs())) {
	    output |= 1 << (REGS_SAME_AS_PREV - 1);
	    needRegs = false;
	}
	boolean needStack = true;
	Set stack = curr.liveStackOffsetLocs();
	if (stack.isEmpty()) {
	    output |= 1 << (STACKLOCS_ARE_ZERO - 1);
	    needStack = false;
	} else if (prev != null &&
		   stack.equals(prev.liveStackOffsetLocs())) {
	    output |= 1 << (STACK_SAME_AS_PREV - 1);
		needStack = false;
	    }   
	boolean needDerivs = true;
	Map derivs = curr.liveDerivations();
	if (derivs.isEmpty()) {
	    output |= 1 << (DERIVS_ARE_ZERO - 1);
	    needDerivs = false;
	} else if (prev != null && 
		   derivs.equals(prev.liveDerivations())) {
	    output |= 1 << (DERIVS_SAME_AS_PREV - 1);
	    needDerivs = false;
	}
	List stmlist = new ArrayList();
	if (!needRegs && !needStack)
	    // output descriptor (int)
	    stmlist.add(_DATUM(new CONST(tf, null, output)));
	else
	    // handle both registers and stack together to save bits
	    stmlist.add(outputRS(needRegs, needStack, regs, stack, output));
	if (needDerivs)
	    stmlist.add(outputDerivs(derivs));
	return Stm.toStm(stmlist);
    } // outputGCData

    // requires: 6-bit descriptor set in output
    // modifies: nil
    // effects: returns statements that output 6-bit descriptor
    //          plus register data (if needed) and stack data
    //          (if needed)
    private Stm outputRS(boolean needRegs, boolean needStack,
			 Set regs, Set stack, int output)
    {
	// number of bits needed to store the 6-bit descriptor,
	// the register bitmap and the stack bitmap
	final int bits = 
	    DESC_BITS + (needRegs?numRegs:0) + (needStack?stack.size():0);
	// number of 32-bit integers needed to encode the data 
	final int numInts = (bits + INT_BITS - 1) / INT_BITS;
	int[] data = new int[numInts];
	// remember the 6-bit descriptor
	int offset = DESC_BITS;
	// get 6-bit descriptor from output
	data[0] = output;
	// do registers first
	if (needRegs) {
	    for(Iterator it=regs.iterator(); it.hasNext(); ) {
		int regIndex = ((MachineRegLoc)it.next()).regIndex();
		int i = (regIndex+offset) / INT_BITS;
		int j = (regIndex+offset) % INT_BITS;
		data[i] |= 1 << (INT_BITS - j - 1);
	    }
	    offset += numRegs;
	}
	if (needStack) {
	    for(Iterator it=stack.iterator(); it.hasNext(); ) {
		int index = ((Integer)basetableMap.get(it.next())).intValue();
		int i = (index+offset) / INT_BITS;
		int j = (index+offset) % INT_BITS;
		data[i] |= 1 << (INT_BITS - j - 1);
	    }
	}
	// output accumulated data
	List stmlist = new ArrayList();
	for(int k=0; k < numInts; k++)
	    stmlist.add(_DATUM(new CONST(tf, null, data[k])));
	return Stm.toStm(stmlist);
    }

    // requires: non-empty Map of Sets (of CommonLocs) to GCInfo.DLocs
    // modifies: nil
    // effects: returns statements that output the derivation data
    private Stm outputDerivs(Map derivs) {
	Util.assert(!derivs.isEmpty());
	List stmlist = new ArrayList();
	// number of derivations
	stmlist.add(_DATUM(new CONST(tf, null, derivs.size())));
	// handle each derivation
	for(Iterator keys=derivs.keySet().iterator(); keys.hasNext(); ) {
	    Set key = (Set)keys.next();
	    // number of locations of the derived pointer (int)
	    stmlist.add(_DATUM(new CONST(tf, null, key.size())));
	    for(Iterator it=key.iterator(); it.hasNext(); ) {
		CommonLoc cl = (CommonLoc)it.next();
		int loc = 0;
		switch(cl.kind()) {
		case StackOffsetLoc.KIND: 
		    loc = ((StackOffsetLoc)cl).stackOffset(); break;
		case MachineRegLoc.KIND:
		    loc = ((MachineRegLoc)cl).regIndex(); break;
		default: Util.assert(false);
		}
		// location of derived pointer
		stmlist.add(_DATUM(new CONST(tf, null, loc)));
	    }
	    GCInfo.DLoc dl = (GCInfo.DLoc)derivs.get(key);
	    // register bits are laid out in (data, sign) pairs
	    {
		final int numInts = (2 * numRegs + INT_BITS - 1) / INT_BITS;
		int[] data = new int[numInts];
		for(int index=0; index < dl.regLocs.length; index++) {
		    int regIndex = dl.regLocs[index].regIndex();
		    int i = (2 * regIndex) / INT_BITS;
		    int j = (2 * regIndex) % INT_BITS;
		    data[i] |= 1 << (INT_BITS - j - 1);
		    data[i] |= 
			(dl.regSigns[index] ? 0 : 1) << (INT_BITS - j - 2);
		}
		for(int k=0; k < numInts; k++)
		    stmlist.add(_DATUM(new CONST(tf, null, data[k])));
	    }
	    // number of entries
	    stmlist.add(_DATUM(new CONST(tf, null, dl.stackLocs.length)));
	    // only output if we have non-zero entries
	    if (dl.stackLocs.length != 0) {
		final int numInts = (dl.stackLocs.length+INT_BITS-1)/INT_BITS;
		int[] sign = new int[numInts];
		for(int index=0; index < dl.stackLocs.length; index++) {
		    int stackOffset = dl.stackLocs[index].stackOffset();
		    stmlist.add(_DATUM(new CONST(tf, null, stackOffset)));
		    int i = index / INT_BITS;
		    int j = index % INT_BITS;
		    sign[i] |= 
			(dl.stackSigns[index] ? 0 : 1) << (INT_BITS - j - 1);
		}
		for(int k=0; k < numInts; k++)
		    stmlist.add(_DATUM(new CONST(tf, null, sign[k])));
	    }
	}
	return Stm.toStm(stmlist);
    }
    final private boolean DEBUG = true;
    // convenient debugging utility
    private void report(String str) {
	if (DEBUG) System.out.println(str);
    }
}












