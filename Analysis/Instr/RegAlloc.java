// RegAlloc.java, created Mon Mar 29 16:47:25 1999 by pnkfelix
// Copyright (C) 1999 Felix S Klock <pnkfelix@mit.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Analysis.Instr;

import harpoon.Temp.Temp;
import harpoon.IR.Assem.Instr;
import harpoon.IR.Assem.InstrEdge;
import harpoon.IR.Assem.InstrFactory;
import harpoon.IR.Assem.InstrMEM;
import harpoon.IR.Assem.InstrVisitor;
import harpoon.IR.Properties.UseDef;
import harpoon.IR.Properties.CFGrapher;
import harpoon.Backend.Generic.Frame;
import harpoon.Backend.Generic.Code;
import harpoon.Backend.Generic.RegFileInfo;
import harpoon.Backend.Generic.RegFileInfo.SpillException;
import harpoon.Backend.Generic.RegFileInfo.TempLocator;
import harpoon.Backend.Generic.RegFileInfo.MachineRegLoc;
import harpoon.Backend.Generic.RegFileInfo.StackOffsetLoc;
import harpoon.Backend.Generic.InstrBuilder;
import harpoon.Analysis.UseMap;
import harpoon.Analysis.BasicBlock;
import harpoon.Analysis.Maps.Derivation;
import harpoon.ClassFile.HCodeFactory;
import harpoon.ClassFile.HCode;
import harpoon.ClassFile.HCodeElement;
import harpoon.ClassFile.HMethod;
import harpoon.Util.Util;
import harpoon.Util.Default;
import harpoon.Util.LinearMap;
import harpoon.Util.Collections.MultiMap;
import harpoon.Util.Collections.GenericMultiMap;

import harpoon.Analysis.DataFlow.ReachingDefs;
import harpoon.Analysis.DataFlow.ForwardDataFlowBasicBlockVisitor;
import harpoon.Analysis.DataFlow.InstrSolver;

import java.util.Hashtable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.Vector;
import java.util.List;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;


/**
 * <code>RegAlloc</code> performs Register Allocation for a set of
 * <code>Instr</code>s in a <code>Backend.Generic.Code</code>.  After
 * register allocation is completed for a set of <code>Instr</code>s,
 * references to non-register <code>Temp</code>s in the
 * <code>Instr</code>s will have been replaced by references to
 * machine registers.  Since the number of simultaneously live
 * temporaries will exceed the space in the register file, spill code
 * will also be inserted to maintain the state of the register file at
 * each instruction, storing values to the stack and reloading them as
 * needed. 
 * 
 * <BR> <B>DESIGN NOTE:</B> The <code>abstractCodeFactory</code>
 * method relies on the subclasses of <code>RegAlloc</code> to perform
 * actual allocation.  This causes a cycle in our module dependency
 * graph, which, while not strictly illegal, tends to be a sign of a
 * design flaw. Consider moving the code factory generator out of the
 * <code>RegAlloc</code> class into a seperate class to get rid of the
 * cycle.  In the meantime, any new <code>RegAlloc</code> subclasses
 * can be incorporated into this method to be used in the compiler.
 * Perhaps should also design a way to parameterize which
 * <code>RegAlloc</code> subclasses will be used.
 * 
 * @author  Felix S Klock <pnkfelix@mit.edu>
 * @version $Id: RegAlloc.java,v 1.1.2.84 2000-02-25 03:53:32 pnkfelix Exp $ 
 */
public abstract class RegAlloc  {
    
    private static final boolean BRAIN_DEAD = false;
    public static final boolean DEBUG = false;

    protected Frame frame;
    protected Code code;
    protected BasicBlock.Factory bbFact;


    private static String getSrcStr(int num) {
	String s = "`s0";
	for(int i=1; i<num; i++) {
	    s += ", `s"+i;
	}
	return s;
    }
    private static String getDstStr(int num) {
	String s = "`d0";
	for(int i=1; i<num; i++) {
	    s += ", `d"+i;
	}
	return s;
    }

    /** Class for <code>RegAlloc</code> usage in loading registers. 
	
	Note that the constructors automagically put in the
	"appropriate" `d# and `s# operands.
	

	REP INVARIANT: SpillLoads have only one src Temp.	
    */
    public class SpillLoad extends InstrMEM {
	SpillLoad(InstrFactory inf, Instr i, String assem, Temp dst, Temp src) {
	    super(inf, i, assem + " `d0, `s0", 
		  new Temp[]{dst}, new Temp[]{src});
	}
	SpillLoad(Instr i, String assem, Temp dst, Temp src) {
	    this(i.getFactory(), i, assem, dst, src);
	}

	// Note that the order that 'dsts' will appear in is the order
	// that its iterator returns the Temps in.
	SpillLoad(InstrFactory inf, Instr i, String assem, Collection dsts, Temp src) {
	    super(inf, i, assem + " " + 
		  getDstStr(dsts.size()) + ", `s0", 
		  (Temp[])dsts.toArray(new Temp[dsts.size()]), 
		  new Temp[]{src});
	}
	SpillLoad(Instr i, String assem, Collection dsts, Temp src) {
	    this(i.getFactory(), i, assem, dsts, src);
	}

    }

    /** Class for <code>RegAlloc</code> usage in spilling registers. 
	
	Note that the constructors automagically put in the
	"appropriate" `d# and `s# operands.

	REP INVARIANT: SpillStores have only one dst Temp.	

    */
    public class SpillStore extends InstrMEM {
	SpillStore(Instr i, String assem, Temp dst, Temp src) {
	    this(i.getFactory(), i, assem, dst, src);
	}

	SpillStore(InstrFactory inf, HCodeElement hce, 
		String assem, Temp dst, Temp src) {
	    super(inf, hce, assem, 
		  new Temp[]{dst}, new Temp[]{src});
	}

	// Note that the order that 'dsts' will appear in is the order
	// that its iterator returns the Temps in.
	SpillStore(InstrFactory inf, HCodeElement hce,
		 String assem, Temp dst, Collection srcs) {
	    super(inf, hce, assem + " `d0, " +
		  getSrcStr(srcs.size()),
		  new Temp[]{dst}, 
		  (Temp[])srcs.toArray(new Temp[srcs.size()]));
	}

	SpillStore(Instr i, String assem, Temp dst, Collection srcs) {
	    this(i.getFactory(), i, assem, dst, srcs);
	}
    }

    /** Creates a <code>RegAlloc</code>.  <code>RegAlloc</code>s are
	each associated with a unique <code>Code</code> which they are
	responsible for performing register allocation and assignment
	for. 
    */
    protected RegAlloc(Code code) {
        this.frame = code.getFrame();
	this.code = code;
	bbFact = new BasicBlock.Factory(code, CFGrapher.DEFAULT);
    }

    
    /** Assigns registers in the code for <code>this</code>.
	
	<BR> <B>effects:</B> Partially or completely allocates
	     registers for the values defined and used in the code for
	     <code>this</code>.  Values will be preserved in the code;
	     any live value will be stored before its assigned
	     register is overwritten.  
	     <BR> Loads and Stores in general
	     are added in the form of <code>SpillLoad</code>s and
	     <code>SpillStore</code>s; the main <code>RegAlloc</code>
	     class will use <code>resolveOutstandingTemps(HCode</code> 
	     to replace these "fake" loads and stores with frame
	     specified Memory instructions.

	@see RegAlloc#resolveOutstandingTemps(HCode)
    */
    protected abstract void generateRegAssignment();

    /** Creates a register-allocating <code>HCodeFactory</code> for
	"instr" form.
	<BR> <B>requires:</B> <code>parentFactory</code> produces code
	     in a derivative of "instr" form.
	<BR> <B>effects:</B> Produces an <code>HCodeFactory</code>
	     which allocates registers in the code produced by
	     <code>parentFactory</code> using the machine properties
	     specified in <code>frame</code>.
     */
    public static HCodeFactory codeFactory(final HCodeFactory parentFactory, 
					   final Frame frame) {
	return concreteSpillFactory
	    (abstractSpillFactory(parentFactory, frame), frame);
    }

    /** <code>IntermediateCodeFactory</code> produces code which is
	register allocated but the architecture-specific spill
	instructions have not been inserted yet.  Stack Offsets have
	been determined and are stored in the <code>Temp</code>s for
	the spill code, but the output needs to be passed through
	<code>RegAlloc.concreteSpillFactory()</code> before it will be
	executable. 
	@see RegAlloc#abstractSpillFactory
	@see RegAlloc#concreteSpillFactory
    */
    public static interface IntermediateCodeFactory extends HCodeFactory {
	harpoon.Backend.Generic.RegFileInfo.TempLocator getTempLocator();
    }
    
    /** Produces an <code>IntermediateCodeFactory</code> which can be
	used to extract Derivation information about code it
	generates.
	<BR> <B>requires:</B> <code>parentFactory</code> produces code
	     in a derivative of "instr" form.
	<BR> <B>effects:</B> Produces an
	     <code>IntermediateCodeFactory</code> which allocates
	     registers in the code produced by
	     <code>parentFactory</code> using the machine properties
	     specified in <code>frame</code>.  Spilled temporarys are
	     assigned a stack offset but the actual code does not have
	     the concrete load and store instructions necessary for the
	     spilling; the <code>IntermediateCodeFactory</code>
	     returned should be passed to
	     <code>concreteSpillFactory()</code> to produce a code
	     factory suitable for generating runnable assembly code. 
    */
    public static 
	IntermediateCodeFactory abstractSpillFactory(final HCodeFactory parent,
						     final Frame frame) {
	return new IntermediateCodeFactory() {
	    HCodeFactory p = parent;

	    

	    public HCode convert(HMethod m) { 
		Code preAllocCode = (Code) p.convert(m);
		if (preAllocCode == null) {
		    return null;
		}
		
		RegAlloc localCode, globalCode;

		localCode = new LocalCffRegAlloc(preAllocCode);
		
		globalCode = localCode; // no global reg alloc yet
		
		globalCode.generateRegAssignment();
		
		List pair = globalCode.resolveOutstandingTemps();
		final Instr instr = (Instr) pair.get(0);
		final RegFileInfo.TempLocator tl = (RegFileInfo.TempLocator) pair.get(1);
		final Code mycode = globalCode.code;

		Util.assert(mycode != null);

	        return new Code(mycode, instr,
				mycode.getDerivation(),
				mycode.getName()) {
		    public String getName() { return mycode.getName(); }
		    public String getRegisterName(Instr i, Temp t,
						  String s) {
			return mycode.getRegisterName(i, t, s);
		    }
		    public void assignRegister(Instr i, Temp t, 
					       List l) {
			mycode.assignRegister(i, t, l);
		    }
		    public boolean registerAssigned(Instr i, Temp t) {
			return mycode.registerAssigned(i, t);
		    }
		    public void removeAssignment(Instr i, Temp t) {
			mycode.removeAssignment(i, t);
		    }
		};
	    }

	    public String getCodeName() { return p.getCodeName(); }
	    public void clear(HMethod m) { p.clear(m); }
	    public RegFileInfo.TempLocator getTempLocator() { 
		return null; 
	    }
	};
    }
    
    /** Produces an <code>HCodeFactory</code> which will transform the
	abstract spills into concrete spills.
	<BR> <B>effects:</B> Produces an <code>HCodeFactory</code>
	     which takes the codes produced by <code>parent</code>,
	     finds the code spilling abstract stack-offset Temps
	     (generated by <code>parent</code>) and replaces it with
	     concrete, architecture-dependant spill code.
    */
    public static HCodeFactory concreteSpillFactory(final IntermediateCodeFactory parent, 
						    final Frame frame) { 
	// Not sure how to handle multiple Temp references in one
	// InstrMEM...for now will assume that there is only one
	// memory references per InstrMEM...
	class InstrReplacer extends InstrVisitor {
	    // Make these SMARTER: get rid of requirement that Loads
	    // and Stores have only one references to memory (to
	    // allow for StrongARMs ldm* instructions : starting doing
	    // this, but I think TempFinder above relies on some parts
	    // of it, at least when assigning offsets.  Check over
	    // this. 
	    private void visitStore(SpillStore m) {
		StackOffsetTemp def = (StackOffsetTemp) m.def()[0];
		List instrs = frame.getInstrBuilder().
		    makeStore(Arrays.asList(m.use()), def.offset, m);
		Instr.replaceInstrList(m, instrs);		
	    }
	    
	    private void visitLoad(SpillLoad m) {
		StackOffsetTemp use = (StackOffsetTemp) m.use()[0];
		List instrs = frame.getInstrBuilder().
		    makeLoad(Arrays.asList(m.def()), use.offset, m);
		Instr.replaceInstrList(m, instrs);
	    }
	    
	    public void visit(Instr i) {
		// do nothing 
	    }

	    public void visit(InstrMEM i) {
		if (i instanceof SpillStore) {
		    visitStore((SpillStore) i);
		} else if (i instanceof SpillLoad) {
		    visitLoad((SpillLoad) i);
		} 
	    }
	}

	return new HCodeFactory() {
	    HCodeFactory p = parent;
	    public HCode convert(HMethod m) {
		Code absCode = (Code) p.convert(m);
		if (absCode == null) {
		    return null;
		}
		
		InstrReplacer replace = new InstrReplacer();
		
		Iterator instrs = absCode.getElementsI();
		while(instrs.hasNext()) {
		    Instr i = (Instr) instrs.next();
		    i.accept(replace);
		}

		if (DEBUG) {
		    instrs = absCode.getElementsI(); // debug check
		    while(instrs.hasNext()) {
			Instr i = (Instr) instrs.next();
			Util.assert(!(i instanceof SpillLoad), "SpillLoad in i-list!");
			Util.assert(!(i instanceof SpillStore), 
				    "SpillStore in i-list! "+
				    i.getPrev() + " " +
				    i + " " + i.getNext());
		    }
		}	    
		

		return absCode; 
	    }
	    
	    public String getCodeName() { return p.getCodeName(); }
	    public void clear(HMethod m) { p.clear(m); }
	};
    }


    /** Temp Wrapper that incorporates a stack offset. */
    private static class StackOffsetTemp extends Temp 
	implements RegFileInfo.StackOffsetLoc { 
	
	Temp wrappedTemp; // FSK: is this field needed?
	int offset;
	StackOffsetTemp(Temp t, int stackOffset) {
	    super(t);
	    wrappedTemp = t;
	    offset = stackOffset;
	}

	public int kind() {
	    return RegFileInfo.StackOffsetLoc.KIND; 
	}
	public int stackOffset() { return offset; }
    }

    /** Transforms Temp references for <code>this</code> into appropriate offsets
	from the Stack Pointer in the Memory. 
        <BR> <B>modifies:</B> this
	<BR> <B>effects:</B> Replaces the <code>SpillLoad</code> and
	     <code>SpillStore</code>s with memory instructions for the
	     appropriate <code>Frame</code>.  Returns a two-elem list
	     pair (Instr, TempLocator)
    */
    protected final List resolveOutstandingTemps() {
	// This implementation is REALLY braindead.  Fix to do a
	// smarter Graph-Coloring stack offset allocator
	Code in = code;
	final MultiMap tempXinstrToCommonLoc = new GenericMultiMap();
	Util.assert(in != null, "Don't try to resolve Temps for null HCodes");

	class TempFinder extends InstrVisitor {
	    HashMap tempsToOffsets = new HashMap();
	    int nextOffset = 1;

	    private void visitLoad(SpillLoad m) {
		// look for non-Register Temps in use, adding
		// them to internal map
		Iterator uses = m.useC().iterator();
		while(uses.hasNext()) {
		    Temp use = (Temp) uses.next();
		    if(!isTempRegister(use)) {
			if (tempsToOffsets.get(use)==null){
			    tempsToOffsets.put(use, new Integer(nextOffset));
			    nextOffset += frame.getInstrBuilder().getSize(use);
			} 

			int off = ((Integer)tempsToOffsets.get(use)).intValue();
			// replace 'use' with StackOffsetTemp
			Temp stkOff = new StackOffsetTemp(use, off);

			SpillLoad newi = 
			    new SpillLoad(m, m.getAssem(), 
					  m.defC(), stkOff);
			Instr.replace(m, newi);
			List dxi = Default.pair(use, newi);
			tempXinstrToCommonLoc.add(dxi, stkOff);

		    }
		}
	    } 
	    private void visitStore(SpillStore m) {
		// look for non-Register Temps in def, adding
		// them to internal map
		Iterator defs = m.defC().iterator();
		while(defs.hasNext()) {
		    Temp def = (Temp) defs.next();
		    if(!isTempRegister(def)) {
			if (tempsToOffsets.get(def)==null){
			    tempsToOffsets.put(def, new Integer(nextOffset)); 
			    nextOffset += frame.getInstrBuilder().getSize(def);
			} 

			int off = ((Integer)tempsToOffsets.get(def)).intValue();
			// replace 'def' with StackOffsetTemp
			StackOffsetTemp stkOff = 
			    new StackOffsetTemp(def, off); 

			SpillStore newi = 
			    new SpillStore(m, m.getAssem(), 
					   stkOff, m.useC());
			Instr.replace(m, newi);
			List dxi = Default.pair(def, newi);
			tempXinstrToCommonLoc.add(dxi, stkOff);
		    }
		}
	    } 

	    public void visit(Instr i) {
		// lookup CommonLocs for defs in 'i'
		Iterator defs = i.defC().iterator();
		while(defs.hasNext()) {
		    Temp def = (Temp) defs.next();
		    List dxi = Default.pair(def, i);
		    if (isTempRegister(def)) {
			tempXinstrToCommonLoc.add(dxi, def);
		    } else {
			Collection regs = code.getRegisters(i, def);
			tempXinstrToCommonLoc.addAll(dxi, regs);
		    }
		}
	    }

	    public void visit(InstrMEM m) {
		if (m instanceof SpillStore)
		    visitStore((SpillStore) m); 
		else if (m instanceof SpillLoad) {
		    visitLoad((SpillLoad) m); 
		}
	    }
	}
	
	TempFinder tf = new TempFinder();
	Iterator instrs = in.getElementsI();
	while(instrs.hasNext()) {
	    Instr i = (Instr) instrs.next();
	    i.accept(tf);
	}
	// now 'instrs' has spill instructions which reference Temps
	// that are associated with stack offsets 

	Instr instr = (Instr) in.getRootElement();

	final int locals = tf.nextOffset - 1; 

	instr = frame.getCodeGen().
	    procFixup(in.getMethod(), instr, locals, 
		      computeUsedRegs(instr));

	// FSK: fix above code to maintain info for
	// RegFileInfo.TempLocator 
	
	TempLocator tl = new TempLocator() {
	    public Set locate(Temp t, Instr i) {
		return (Set)
		tempXinstrToCommonLoc.getValues(Default.pair(t,i));
	    }
	};
	
	return Default.pair(instr, tl);
    }
    
    private Set computeUsedRegs(Instr instrs) {
	Set s = new HashSet();
	for (Instr il = instrs; il!=null; il=il.getNext()) {
	    if (il instanceof SpillStore) continue;
	    Temp[] d = il.def();
	    for (int i=0; i<d.length; i++) {
		if (isTempRegister(d[i])) {
		    s.add(d[i]); 
		} else {
		    Collection c = code.getRegisters(il, d[i]);
		    s.addAll(c);
		}
	    }
	}
	return Collections.unmodifiableSet(s);
    }


    /** Checks if <code>t</code> is a register (Helper method).
	<BR> <B>effects:</B> If <code>t</code> is a register for the
	     <code>frame</code> associated with <code>this</code>,
	     then returns true.  Else returns false.   
    */ 
    protected boolean isTempRegister(Temp t) {
	return frame.getRegFileInfo().isRegister(t);
        
	// Temp[] allRegs = frame.getAllRegisters();
	// boolean itIs = false;
	// for (int i=0; i < allRegs.length; i++) {
	//    if (t.equals(allRegs[i])) {
	//	itIs = true;
	//	break;
	//    }
	// }
	// return itIs;
    }

    /** Checks if <code>i</code> is last use of <code>reg</code> in
	the block of instructions listed in <code>iter</code>.  
	
	<BR> <B>requires:</B> 
	     <BR> 1. <code>i</code> is an element in <code>iter</code> 
	     <BR> 2. <code>iter</code> is an <code>Iterator</code> of
	             a linear series of <code>Instr</code>s in the
		     order that they will be executed.
	     <BR> 3. <code>iter</code> is currently indexed at
	             <code>i</code> 
	<BR> <B>modifies:</B> <code>iter</code>
	<BR> <B>effects:</B> 
             <BR> 1. Returns true if no instruction after
	             <code>i</code> in <code>iter</code> uses
		     <code>reg</code> before <code>reg</code> is
		     redefined (<code>i</code> redefining
		     <code>reg</code> is sufficient).  Else returns
		     false. 
	     <BR> 2. <code>iter</code> is left at an undetermined
	             index. 
    */
    protected static boolean lastUse(Temp reg, UseDef i, Iterator iter) {
	UseDef curr = i;
	boolean r = true;
	while (iter.hasNext() && ! contained( curr.def(), reg ) ) {
	    curr = (UseDef) iter.next();
	    if (contained( curr.use(), reg )) {
		r = false;
		break;
	    }
	}
	return r;
    } 

    private static boolean contained(Object[] array, Object o) {
	boolean yes = false;
	for (int i=0; i<array.length; i++) {
	    if (array[i] == o) {
		yes = true;
		break;
	    }
	}
	return yes;
    }
    
}

// note that this doesn't even work any more because it does not
// support garbage collection.
class BrainDeadLocalAlloc extends RegAlloc {
    BrainDeadLocalAlloc(Code code) {
	super(code);
    }
	
    class BrainDeadInstrVisitor extends InstrVisitor {
	Temp[] regs = frame.getRegFileInfo().getGeneralRegisters();
		
	public void visit(Instr instr) {
	    InstrFactory inf = instr.getFactory();
	    
	    try {
		// in this (dumb) model, each instruction will
		// load all uses and store all defs, so we can
		// treat the register file as being empty for each
		// instruction
		
		Map regFile = new LinearMap();

		// load srcs
		for(int i=0; i<instr.use().length; i++) {
		    Temp preg = instr.use()[i];
		    if (!isTempRegister(preg)) {
			Iterator iter =
			    frame.getRegFileInfo().suggestRegAssignment(preg, regFile); 
			List regList = (List) iter.next();
			InstrMEM loadSrcs = 
			    new SpillLoad(inf, null, "FSK-LOAD", regList, preg); 
			loadSrcs.insertAt(new InstrEdge(instr.getPrev(), instr));
			code.assignRegister(instr, preg, regList);
			Iterator regIter = regList.iterator();
			while(regIter.hasNext()) {
			    Temp r = (Temp) regIter.next();
			    regFile.put(r, preg);
			}
		    }
		}
		// store dsts
		for(int i=0; i<instr.def().length; i++) {
		    Temp preg = instr.def()[i];
		    if(!isTempRegister(preg)) {
			Iterator iter =
			    frame.getRegFileInfo().suggestRegAssignment(preg, regFile); 
			List regList = (List) iter.next();
			InstrMEM storeDsts = 
			    new SpillStore(inf, null, "FSK-STORE",
					 preg, regList);

			// NOTE: if this assertion fails, we can write
			// code to work around this requirement.  But
			// for now this implementation is simple and
			// seems to work.  
			Util.assert(instr.succC().size() == 1, 
				    "Instr: "+instr+" should have only"+
				    " one control flow successor");

			storeDsts.insertAt
			    (new InstrEdge
			     (instr, (Instr)instr.succ()[0].to()));

			// I'm not certain this code will handle 
			// "add t0, t0, t1" properly 
			code.assignRegister(instr, preg, regList);
			Iterator regIter = regList.iterator();
			while(regIter.hasNext()) {
			    Temp r = (Temp) regIter.next();
			    regFile.put(r, preg);
			}
		    }
		}
	    } catch (SpillException e) {
		// actually...this doesn't necessarily imply that we
		// have to fail; if a TwoWordTemp can't be fitted, we
		// could potentially shift the register files contents
		// around to make room for it.   While we STILL
		// shouldn't ever encounter this problem (after all,
		// we have an empty register file and usually at most
		// three pseudo registers to assign (potentially six
		// though, with double worded operands)) I should give
		// some thought on how to handle this case 
		
		Util.assert(false, "Either a TwoWordTemp screwed us, or "+
			    "One Instr uses/defines more "+
			    "registers than "+frame+" can accomidate "+
			    "in Register File!"); 
	    }
	}
	
    }
    
    /** For each instruction:
	1. Load every use from memory into the register file.
	2. Execute the instruction
	3. Store every dest from the register file
	regFile will be clean in between each instruction in this
	(very dumb) allocation strategy. 
    */
    protected void generateRegAssignment() {
	Iterator instrs = code.getElementsI();
	InstrVisitor memVisitor = new BrainDeadInstrVisitor();
	
	while(instrs.hasNext()) {
	    ((Instr)instrs.next()).accept(memVisitor);
	}

    }

}

/** Visits <code>BasicBlock</code>s of <code>Instr</code>s and
    uses the <code>SpillLoad</code> and <code>SpillStore</code>
    instructions to construct <code>Web</code>s for this method,
    These webs will need to be run through a merging dataflow
    analysis pass.  This is effectively ReachingDefs, but I
    couldn't figure out how to easily adapt Whaley's version of
    ReachingDefs to my needs (note to FSK, either figure out
    Whaley's version or rewrite it in a form thats at least as
    useful as the LiveVars class) (also note to FSK: the current
    implementation is soft and flabby (space and time
    inefficient); look into really fixing up ReachingDefs and
    LiveVars to be fast AND easy-to-use)
*/
class MakeWebsDumb extends ForwardDataFlowBasicBlockVisitor {
    /** struct class: 
	'in' maps a Temp to a Web that is defined somewhere above
	     this block. 
	'out' maps a Temp to a Web that is defined somewhere above
	      or within this block (note that the Web defined IN
	      this block is a distinct object from the one defined
	      ABOVE this block)
	'use' maps a Temp to the Set of Instrs that refer to it up
	      until (and not including) that Temp's LAST definition
	      in the block
	'def' maps a Temp to the Set of Instrs from that Temp's
	      LAST definition in the basic block through all of its
	      subsequent uses in the block
    */
    class WebInfo {
	MultiMap.Factory mmf = new MultiMap.Factory();
	
	MultiMap in = new GenericMultiMap(); // Map[Temp, [Web] ]
	MultiMap out = new GenericMultiMap(); // Map[Temp, [Web] ]
	MultiMap use = new GenericMultiMap(new MySetFactory(), // Map[Temp, [Instr] ]
					harpoon.Util.Collections.Factories.hashMapFactory());
	MultiMap def = new GenericMultiMap(new MySetFactory(), // Map[Temp, [Instr] ]
					harpoon.Util.Collections.Factories.hashMapFactory());
	
	class MySetFactory extends harpoon.Util.Collections.SetFactory {
	    public Set makeSet(java.util.Collection c) {
		return new java.util.HashSet(c) {
		    /** temporarily overriding this.toString() to
			give dense description of Set's contents. */  
		    public String toString() {
			StringBuffer str = new StringBuffer("{ ");
			Iterator iter = iterator();
			while(iter.hasNext()) {
			    Instr i = (Instr) iter.next();
			    str.append( i.getID() );
			    if (iter.hasNext()) str.append(", ");
			}
			str.append(" }");
			return str.toString();
		    }		    
		};
	    }
	}

	
	void foundLoad(RegAlloc.SpillLoad instr) {
	    Iterator uses = instr.useC().iterator();
	    while(uses.hasNext()) {
		Temp t = (Temp) uses.next();

		Util.assert(t != null, "No nulls for Temps");
		
		if ((def.getValues(t)).isEmpty()) {
		    // if it uses a variable defined in
		    // another block, then add to USE
		    use.add(t, instr);
		} else {
		    // put it in the DEF set; we'll move the DEF
		    // set into the USE set later if we have to.
		    def.add(t, instr);
		}
	    }
	}
		
	void foundStore(RegAlloc.SpillStore instr) {
	    Iterator defs = instr.defC().iterator();
	    while(defs.hasNext()) {
		Temp t = (Temp) defs.next();

		Util.assert(t != null, "No nulls for Temps");
		
		if (!(def.getValues(t)).isEmpty()) {
		    // We have seen a DEF for t in this block
		    // before; need to move all those instrs over
		    // before putting this in the DEF set
		    Iterator instrs =
			(def.getValues(t)).iterator();
		    while(instrs.hasNext()) {
			use.add(t, instrs.next());
		    }
		}
		def.add(t, instr);
	    }
	}
    }
    
    HashMap bbInfoMap;
    
    MakeWebsDumb(Iterator basicBlocks) {
	bbInfoMap = new HashMap();
	
	// initialize USE/DEF info
	while(basicBlocks.hasNext()) {
	    BasicBlock bb = (BasicBlock) basicBlocks.next();
	    WebInfo info = new WebInfo();
	    bbInfoMap.put(bb, info);
	    
	    ListIterator instrs = bb.statements().listIterator();
	    while(instrs.hasNext()) {
		Instr instr = (Instr) instrs.next();
		if (instr instanceof RegAlloc.SpillLoad) {
		    // LOAD FROM MEM
		    info.foundLoad((RegAlloc.SpillLoad) instr);
		} else if (instr instanceof RegAlloc.SpillStore) { 
		    // STORE TO MEM
		    info.foundStore((RegAlloc.SpillStore) instr);
		}
	    }		
	}
    }
    
    public boolean merge(BasicBlock from, BasicBlock to) {

	WebInfo fromInfo = (WebInfo) bbInfoMap.get(from);
	WebInfo toInfo = (WebInfo) bbInfoMap.get(to);
	
	// FSK: can't just use putAll(); need to track if a change
	// FSK: occurred. 
	// toInfo.in.putAll(fromInfo.out);
	boolean changed = false;
	Iterator keys = fromInfo.out.keySet().iterator();
	while(keys.hasNext()) {
	    Object key = keys.next();
	    java.util.Collection newVals = fromInfo.out.getValues(key);
	    changed = changed || toInfo.in.addAll(key, newVals);
	}

	// System.out.println("\t\t\tMerging from " + from +
	//		   " to " + to + ":" + 
	//		   (changed?"changed":"nochange")); 
	
	return changed;
    }
    
    public void visit(BasicBlock b) {
	// System.out.println("\t\t\tVisiting " + b);

	WebInfo webInfo = (WebInfo) bbInfoMap.get(b);
	
	Iterator inEntries = webInfo.in.entrySet().iterator();
	while(inEntries.hasNext()) {
	    Map.Entry entry = (Map.Entry) inEntries.next();
	    Temp t = (Temp) entry.getKey();
	    Web web = (Web) entry.getValue();
	    Iterator instrs = webInfo.use.getValues(t).iterator();

	    // System.out.println("\t\t\t\t IN -> OUT : [" + 
	    //		       web + ", "+b+"]" );

	    while(instrs.hasNext()) {
		web.refs.add(instrs.next());
	    }
	    
	    webInfo.out.put(t, web);
	}
	
	Iterator defKeys = webInfo.def.keySet().iterator();
	while(defKeys.hasNext()) {
	    Temp t = (Temp) defKeys.next();
	    Set instrs = (Set) webInfo.def.getValues(t);

	    Web web = new Web(t, instrs);
	    
	    // Note that this will replace any web in OUT that was
	    // in IN with the last defined web (with the same temp) in
	    // this Basic Block (if one exists).  This is the correct
	    // behavior. 
	    webInfo.out.put(t, web);

	}
    }
}

