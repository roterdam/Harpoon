// ToTree.java, created Tue Feb 16 16:46:36 1999 by duncan
// Copyright (C) 1998 Duncan Bryce <duncan@lcs.mit.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.IR.Tree;

import harpoon.Analysis.DefMap;
import harpoon.Analysis.ReachingDefs;
import harpoon.Analysis.ReachingDefsImpl;
import harpoon.Analysis.Maps.Derivation;
import harpoon.Analysis.Maps.Derivation.DList;
import harpoon.Analysis.Maps.TypeMap;
import harpoon.Backend.Generic.Frame;
import harpoon.Backend.Generic.Runtime;
import harpoon.Backend.Maps.NameMap;
import harpoon.ClassFile.HClass;
import harpoon.ClassFile.HCode;
import harpoon.ClassFile.HCodeElement;
import harpoon.ClassFile.HField;
import harpoon.ClassFile.HMethod;
import harpoon.IR.LowQuad.LowQuadFactory;
import harpoon.IR.LowQuad.LowQuadNoSSA;
import harpoon.IR.LowQuad.LowQuadSSA;
import harpoon.IR.LowQuad.LowQuadVisitor;
import harpoon.IR.LowQuad.LQop;
import harpoon.IR.LowQuad.PAOFFSET;
import harpoon.IR.LowQuad.PARRAY;
import harpoon.IR.LowQuad.PCALL;
import harpoon.IR.LowQuad.PCONST;
import harpoon.IR.LowQuad.PFCONST;
import harpoon.IR.LowQuad.PFIELD;
import harpoon.IR.LowQuad.PFOFFSET;
import harpoon.IR.LowQuad.PGET;
import harpoon.IR.LowQuad.PMCONST;
import harpoon.IR.LowQuad.PMETHOD;
import harpoon.IR.LowQuad.PMOFFSET;
import harpoon.IR.LowQuad.POPER;
import harpoon.IR.LowQuad.PPTR;
import harpoon.IR.LowQuad.PSET;
import harpoon.IR.Properties.UseDef;
import harpoon.IR.Quads.Edge;
import harpoon.IR.Quads.Qop;
import harpoon.IR.Quads.Quad;
import harpoon.IR.Quads.QuadFactory;
import harpoon.IR.Quads.QuadKind;
import harpoon.IR.Quads.QuadVisitor;
import harpoon.Temp.CloningTempMap;
import harpoon.Temp.Label;
import harpoon.Temp.Temp;
import harpoon.Temp.TempFactory;
import harpoon.Temp.TempMap;
import harpoon.Util.Default;
import harpoon.Util.HClassUtil;
import harpoon.Util.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Stack;

/**
 * The ToTree class is used to translate low-quad code to tree code.
 * 
 * @author  Duncan Bryce <duncan@lcs.mit.edu>
 * @version $Id: ToTree.java,v 1.1.2.67 2000-02-12 17:47:58 cananian Exp $
 */
class ToTree {
    private Tree        m_tree;
    private DerivationGenerator m_dg = new DerivationGenerator();
   
    /** Class constructor.  Uses the default <code>EdgeOracle</code>
     *  and <code>ReachingDefs</code> for <code>LowQuadNoSSA</code>. */
    public ToTree(final TreeFactory tf, LowQuadNoSSA code) {
	this(tf, code,
	     new ToTreeHelpers.DefaultEdgeOracle(),
	     new ToTreeHelpers.DefaultFoldNanny(),
	     new ReachingDefsImpl(code));
    }
    public ToTree(final TreeFactory tf, final LowQuadSSA code) {
	this(tf, code,
	     new ToTreeHelpers.MinMaxEdgeOracle(code),
	     new ToTreeHelpers.DefaultFoldNanny(),
	     new ToTreeHelpers.SSIReachingDefs(code));
    }
    /** Class constructor. */
    public ToTree(final TreeFactory tf, harpoon.IR.LowQuad.Code code,
		  EdgeOracle eo, FoldNanny fn, ReachingDefs rd) {
	Util.assert(((Code.TreeFactory)tf).getParent()
		    .getName().equals("tree"));
	translate(tf, code, eo, fn, rd);
    }
    
    /** Returns a <code>TreeDerivation</code> object for the
     *  generated <code>Tree</code> form. */
    public TreeDerivation getTreeDerivation() { return m_dg; }

    /** Returns the root of the generated tree code */
    public Tree getTree() {
	return m_tree;
    }

    private void translate(TreeFactory tf, harpoon.IR.LowQuad.Code code,
			   EdgeOracle eo, FoldNanny fn, ReachingDefs rd) {

	Quad root = (Quad)code.getRootElement();
	TempMap ctm = new CloningTempMap
	    (root.getFactory().tempFactory(),tf.tempFactory());

	// Construct a list of harpoon.IR.Tree.Stm objects
	TranslationVisitor tv = new TranslationVisitor
	    (tf, rd, (Derivation) code, eo, fn, m_dg, ctm);
						       
	// traverse, starting with the METHOD quad.
	dfsTraverse((harpoon.IR.Quads.METHOD)root.next(1), 0,
		    tv, new HashSet());

	// Assign member variables
	m_tree       = ((TranslationVisitor)tv).getTree();
    }
    
    // Translates the Quad graph in a depth-first order.
    private void dfsTraverse(Quad q, int which_pred,
			     TranslationVisitor tv, Set visited) {
	// if this is a phi function, translate by emitting appropriate MOVEs
	if (q instanceof harpoon.IR.Quads.PHI)
	    tv.emitPhiFixup((harpoon.IR.Quads.PHI)q, which_pred);
	// if we've already translated this quad, goto the translation.
	if (visited.contains(q)) {
	    tv.emitGoto(tv.label(q), /*for line # info:*/q.prev(which_pred));
	    return;
	} else visited.add(q);
	// label phis.
	if (q instanceof harpoon.IR.Quads.PHI)
	    tv.emitLabel(tv.label(q), /* for line # info:*/q);
	// translate this instruction.
	q.accept(tv);
	// translate successors.
	int n = q.nextLength();
	int def = tv.edgeOracle.defaultEdge(q);
	for (int i=0; i<n; i++) {
	    // permute edges such that default always comes first.
	    //  think:  0 1[2]3 4  (if 2 is the default)
	    //         [2]0 1 3 4
	    Edge edge = q.nextEdge((i==0)?def:(i<=def)?i-1:i);
	    if (n > 1) { // label sigma outputs, and emit proper fixup code
		tv.emitLabel(tv.label(edge), /*for line # info:*/q);
		tv.emitSigmaFixup((harpoon.IR.Quads.SIGMA)q,
				  edge.which_succ());
	    }
	    // recurse.
	    if (!(edge.to() instanceof harpoon.IR.Quads.FOOTER))
		dfsTraverse((Quad)edge.to(), edge.which_pred(), tv, visited);
	}
	// done, yay.
    }

    // don't close class: all of the following are inner classes,
    // even if they don't look that way.  I'm just don't feel like
    // reindenting all of this existing code. [CSA]
  
// Translates the LowQuadNoSSA code into tree form. 
//
static class TranslationVisitor extends LowQuadVisitor {
    private final TempMap     m_ctm;          // Clones Temps to new tf
    private final NameMap     m_nm;           // Runtime-specific label naming
    private final List        m_stmList;      // Holds translated statements
    private final TreeFactory m_tf;           // The new TreeFactory
    private Temp              m_handler = null; 
    private final Runtime.TreeBuilder m_rtb;

    private final Derivation quadDeriv;
    private final DerivationGenerator treeDeriv;
    public final EdgeOracle edgeOracle;
    public final FoldNanny  foldNanny;
    public final ReachingDefs reachingDefs;

    public TranslationVisitor(TreeFactory tf,
			      ReachingDefs reachingDefs,
			      Derivation quadDeriv,
			      EdgeOracle edgeOracle,
			      FoldNanny foldNanny,
			      DerivationGenerator treeDeriv,
			      TempMap ctm) {
	m_ctm          = ctm;
	m_tf           = tf; 
	m_nm           = m_tf.getFrame().getRuntime().nameMap;
	m_stmList      = new ArrayList();
	m_rtb	       = m_tf.getFrame().getRuntime().treeBuilder;
	this.quadDeriv      = quadDeriv;
	this.treeDeriv	    = treeDeriv;
	this.edgeOracle     = edgeOracle;
	this.foldNanny      = foldNanny;
	this.reachingDefs   = reachingDefs;
    }

    Tree getTree() { return Stm.toStm(m_stmList); } 

    // label maker ----------------
    private final Map labelMap = new HashMap() {
	public Object get(Object key) {
	    if (!containsKey(key)) { put(key, new Label()); }
	    return super.get(key);
	}
    };
    public Label label(Quad q) {
	Util.assert(q instanceof harpoon.IR.Quads.PHI);
	return (Label) labelMap.get(q);
    }
    public Label label(Edge e) {
	Util.assert(e.from() instanceof harpoon.IR.Quads.SIGMA);
	return (Label) labelMap.get(e);
    }
    // end label maker --------------

    // labels and phis and sigmas, oh my! ------------
    public void emitGoto(Label target, HCodeElement src) {
	addStmt(new JUMP(m_tf, src, target));
    }
    public void emitLabel(Label label, HCodeElement src) {
	addStmt(new LABEL(m_tf, src, label, false));
    }
    public void emitPhiFixup(harpoon.IR.Quads.PHI q, int which_pred) {
	for (int i=0; i<q.numPhis(); i++)
	    addMove(q, q.dst(i), _TEMPte(q.src(i, which_pred), q));
    }
    public void emitSigmaFixup(harpoon.IR.Quads.SIGMA q, int which_succ) {
	for (int i=0; i<q.numSigmas(); i++)
	    addMove(q, q.dst(i, which_succ), _TEMPte(q.src(i), q));
    }
    // end labels and phis and sigmas, oh my! --------

    public void visit(Quad q) { Util.assert(false); /* not handled! */ }

    public void visit(harpoon.IR.Quads.ALENGTH q) {
	addMove
	    (q, q.dst(),
	     m_rtb.arrayLength
	     (m_tf, q, _TEMPte(q.objectref(), q))
	     );
    }

    public void visit(harpoon.IR.Quads.ANEW q) {
	// create and zero fill a multi-dimensional array.
	int dl = q.dimsLength();
	Util.assert(dl>0);
	// temps to hold each part of the array;
	// arrayTemps[i] holds an (dl-i)-dimensional array.
	// arrayClasses[i] is the type of arrayTemps[i]
	Temp[] arrayTemps = new Temp[dl+1];
	HClass[] arrayClasses = new HClass[dl+1];
	arrayTemps[0] = m_ctm.tempMap(q.dst());
	arrayClasses[0] = q.hclass();
	for (int i=1; i<=dl; i++) {
	    arrayTemps[i] = new Temp(arrayTemps[i-1]);
	    arrayClasses[i] = arrayClasses[i-1].getComponentType();
	    Util.assert(arrayClasses[i]!=null);
	}
	// temps standing for size of each dimension.
	Temp[] dimTemps = new Temp[dl];
	for (int i=0; i<dl; i++) {
	    dimTemps[i] = m_ctm.tempMap(q.dims(i));
	    // move (possibly folded) values into dimTemps, as we will
	    // be evaluating the dimensions multiple times.
	    addStmt(new MOVE(m_tf, q,
			     _TEMP(q, HClass.Int, dimTemps[i]),
			     _TEMP(q.dims(i), q)));
	}
	// temps used to index each dimension
	Temp[] indexTemps = new Temp[dl];
	for (int i=0; i<dl; i++)
	    indexTemps[i] = new Temp(m_tf.tempFactory(), "idx");
	// labels for loop top, test, and end.
	Label[] testLabel = new Label[dl];
	Label[] loopLabel = new Label[dl];
	Label[] doneLabel = new Label[dl];
	for (int i=0; i<dl; i++) {
	    testLabel[i] = new Label();
	    loopLabel[i] = new Label();
	    doneLabel[i] = new Label();
	}
	// okay.  Now do the translation:
	//  d1 = new array(ilen);
	//  for (i=0; i<ilen; i++) {
	//    d2 = d1[i] = new array(jlen);
	//    for (j=0; j<jlen; j++) {
	//      d2[i] = 0;
	//    }
	//  }
	for (int i=0; i<=dl; i++) { // write the loop tops.
	    Exp initializer;
	    if (i==dl) // bottom out with elements set to zero/null.
		initializer = constZero(q, arrayClasses[i]);
	    else
		initializer = m_rtb.arrayNew
		    (m_tf, q, arrayClasses[i],
		     new Translation.Ex
		     (_TEMP(q, HClass.Int, dimTemps[i]))).unEx(m_tf);
	    // output: d1[i] = d2 = initializer.
	    Stm s1 = new MOVE // d2 = initializer
		(m_tf, q,
		 _TEMP(q, arrayClasses[i], arrayTemps[i]),
		 initializer);
	    if (i>0) // suppress the "d1[i]" part for outermost
		s1 = new MOVE
		    (m_tf, q,
		     makeMEM // d1[i] = ...
		     (q, arrayClasses[i],
		      new BINOP
		      (m_tf, q, Type.POINTER, Bop.ADD,
		       m_rtb.arrayBase
		       (m_tf, q,
			new Translation.Ex
			(_TEMP(q, arrayClasses[i-1], arrayTemps[i-1])))
		       .unEx(m_tf),
		       m_rtb.arrayOffset
		       (m_tf, q, arrayClasses[i-1],
			new Translation.Ex
			(_TEMP(q, HClass.Int, indexTemps[i-1])))
			.unEx(m_tf)
			)),
		     new ESEQ // ... (d2 = initializer)
		     (m_tf, q, s1,
		      _TEMP(q, arrayClasses[i], arrayTemps[i])));
		      
	    addStmt(s1);
	    // for (i=0; i<ilen; i++) ...
	    if (i<dl) { // skip loop for innermost
		addStmt(new MOVE // i=0.
			(m_tf, q,
			 _TEMP(q, HClass.Int, indexTemps[i]),
			 new CONST(m_tf, q, 0)));
		addStmt(new JUMP(m_tf, q, testLabel[i]));
		addStmt(new LABEL(m_tf, q, loopLabel[i], false));
	    }
	}
	// okay, write the loop bottoms in reverse order.
	for (int i=dl-1; i>=0; i--) {
	    // increment the loop counter
	    addStmt(new MOVE // i++
		    (m_tf, q,
		     _TEMP(q, HClass.Int, indexTemps[i]),
		     new BINOP(m_tf, q, Type.INT, Bop.ADD,
			       _TEMP(q, HClass.Int, indexTemps[i]),
			       new CONST(m_tf, q, 1))));
	    // loop test label
	    addStmt(new LABEL(m_tf, q, testLabel[i], false));
	    // test and branch: if i<ilen goto LOOP else goto DONE;
	    addStmt(new CJUMP
		    (m_tf, q,
		     new BINOP(m_tf, q, Type.INT, Bop.CMPLT,
			       _TEMP(q, HClass.Int, indexTemps[i]),
			       _TEMP(q, HClass.Int, dimTemps[i])),
		     loopLabel[i]/*iftrue*/, doneLabel[i]/*iffalse*/));
	    addStmt(new LABEL(m_tf, q, doneLabel[i], false));
	}
	// ta-da!
	// add bogus move to placate folding code
	addMove(q, q.dst(), _TEMP(q, arrayClasses[0], arrayTemps[0]));
    }

    public void visit(harpoon.IR.Quads.ARRAYINIT q) {
	HClass arrayType = HClassUtil.arrayClass(q.type(), 1);
	Stm s0, s1, s2;

	// Create a pointer which we'll use to initialize the array
	Temp  nextPtr = new Temp(m_tf.tempFactory(), "nxt");
	// make sure base pointer lives in a treetemp. (may be folded instead)
	Temp   objTemp = m_ctm.tempMap(q.objectref());
	addStmt(new MOVE(m_tf, q, 
			 _TEMP(q, arrayType, objTemp), 
			 _TEMP(q.objectref(), q)));
	// Create derivation information for the new TEMP
	DList dl = new DList(objTemp, true, null);

	// set nextPtr to point to arrayBase + q.offset() * size.
	s0 = new MOVE
	    (m_tf, q, 
	     _TEMP(q, dl, nextPtr),
	     new BINOP
	     (m_tf, q, Type.POINTER, Bop.ADD,
	      m_rtb.arrayBase
	      (m_tf, q, new Translation.Ex(_TEMP(q, arrayType, objTemp)))
	      .unEx(m_tf),
	      m_rtb.arrayOffset
	      (m_tf, q, arrayType,
	       new Translation.Ex(new CONST(m_tf, q, q.offset()))).unEx(m_tf)
	      ));

	addStmt(s0);
    
	for (int i=0; i<q.value().length; i++) {
	    Exp c = mapconst(q, q.value()[i], q.type());
	    MEM m = makeMEM(q, q.type(), _TEMP(q, dl, nextPtr));
	    s0 = new MOVE(m_tf, q, m, c);
	    s1 = new MOVE
		(m_tf, q, 
		 _TEMP(q, dl, nextPtr), 
		 new BINOP
		 (m_tf, q, Type.POINTER, Bop.ADD, 
		  _TEMP(q, dl, nextPtr), 
		  m_rtb.arrayOffset
		  (m_tf, q, arrayType,
		   new Translation.Ex(new CONST(m_tf, q, 1))).unEx(m_tf)
		  ));

	    addStmt(new SEQ(m_tf, q, s0, s1));
	}
    }

    public void visit(harpoon.IR.Quads.CJMP q) { 
	addStmt(_TEMPte(q.test(), q).unCx
		(m_tf, label(q.nextEdge(1)), label(q.nextEdge(0))));
    }
  
    public void visit(harpoon.IR.Quads.COMPONENTOF q) {
	addMove(q, q.dst(),
		m_rtb.componentOf(m_tf, q,
				  _TEMPte(q.arrayref(), q),
				  _TEMPte(q.objectref(), q)));
    }

    public void visit(harpoon.IR.Quads.CONST q) {
	addMove(q, q.dst(), mapconst(q, q.value(), q.type()));
    }
  
    public void visit(harpoon.IR.Quads.INSTANCEOF q) {
	addMove
	    (q, q.dst(),
	     m_rtb.instanceOf(m_tf, q, 
			      _TEMPte(q.src(), q), q.hclass()));
    }

    public void visit(harpoon.IR.Quads.METHOD q) {
	METHOD method; SEGMENT segment;
	Temp   params[]  = q.params(); 
	TEMP   mParams[] = new TEMP[params.length+1];
	
	segment = new SEGMENT(m_tf, q, SEGMENT.CODE);
	for (int i=0; i<params.length; i++) { 
	    mParams[i+1] = (TEMP) _TEMP(params[i], q);
	}
	Util.assert(m_handler==null);
	m_handler = new Temp(m_tf.tempFactory(), "handler");
	mParams[0] = _TEMP(q, HClass.Void, m_handler);
	method    = new METHOD(m_tf, q, mParams);
	addStmt(segment);
	addStmt(method);
    }

    public void visit(harpoon.IR.Quads.MONITORENTER q) {
	addStmt(m_rtb.monitorEnter(m_tf, q, _TEMPte(q.lock(), q))
		     .unNx(m_tf));
    }

    public void visit(harpoon.IR.Quads.MONITOREXIT q) {
	addStmt(m_rtb.monitorExit(m_tf, q, _TEMPte(q.lock(), q))
		     .unNx(m_tf));
    }

    public void visit(harpoon.IR.Quads.MOVE q) {
	addMove(q, q.dst(), _TEMPte(q.src(), q));
    }

    public void visit(harpoon.IR.Quads.NEW q) { 
	addMove(q, q.dst(),
		m_rtb.objectNew(m_tf, q, q.hclass(), true));
    }
	
    public void visit(harpoon.IR.Quads.PHI q) {
	// do nothing!
    }

    public void visit(harpoon.IR.Quads.RETURN q) {
	Exp retval;
    
	if (q.retval()==null) {
	    retval = new CONST(m_tf, q, 0);
	}
	else {
	    retval = _TEMP(q.retval(), q);
	}

	Stm s0 = new RETURN(m_tf, q, retval);    
	addStmt(s0);
    }

    // Naive implementation
    public void visit(harpoon.IR.Quads.SWITCH q) { 
	// move (possibly folded) discriminant into Temp, since we'll be
	// evaluating it multiple times.
	Temp index = m_ctm.tempMap(q.index());
	addStmt(new MOVE(m_tf, q,
			 _TEMP(q, HClass.Int, index),
			 _TEMP(q.index(), q)));
	// okay, now translate SWITCH
	CJUMP branch;
	for (int i=0; i<q.keysLength(); i++) {
	    Label lNext = (i+1 < q.keysLength()) ? new Label() :
		label(q.nextEdge(q.keysLength())); // handle default case
	    branch = new CJUMP
		(m_tf, q, new BINOP(m_tf, q, Type.INT, Bop.CMPEQ, 
				    _TEMP(q, HClass.Int, index), 
				    new CONST(m_tf, q, q.keys(i))),
		 label(q.nextEdge(i)),
		 lNext);
	    addStmt(branch);
	    if (i+1 < q.keysLength())
		addStmt(new LABEL(m_tf, q, lNext, false));
	}
    }
  
    public void visit(harpoon.IR.Quads.THROW q) { 
	Util.assert(m_handler!=null);
	addStmt(new THROW(m_tf, q,
			  _TEMP(q.throwable(), q),
			  _TEMP(q, HClass.Void, m_handler)));
    }

    public void visit(harpoon.IR.Quads.TYPECAST q) {
	throw new Error("Use INSTANCEOF instead of TYPECAST");
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*
     *                                                          *
     *                   LowQuad Translator                     *
     *                                                          *
     *+++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
		  
    // FIXME THIS IS WHERE I'M AT.
    public void visit(PAOFFSET q) {
	addMove
	    (q, q.dst(),
	     m_rtb.arrayOffset(m_tf, q, q.arrayType(),
			       _TEMPte(q.index(), q)));
    }

    public void visit(PARRAY q) {
	addMove
	    (q, q.dst(),
	     m_rtb.arrayBase(m_tf, q,
			     _TEMPte(q.objectref(), q)));
    }


    // runtime-independent
    public void visit(PCALL q) { 
	ExpList params; Temp[] qParams;
	Temp retval, retex; // tree temps for destination variables
	TEMP retvalT, retexT; // TEMP expressions for the above.
	Exp func, ptr;

	Util.assert(q.retex()!=null && q.ptr()!=null);

	// If q.retval() is null, the 'retval' in Tree.CALL is also null.
	if (q.retval()==null) {
	    retval = null;
	    retvalT = null;
	}
	else {
	    retval = m_ctm.tempMap(q.retval()); // a tree temp.
	    // (return value should never have a derived type)
	    Util.assert(quadDeriv.typeMap(q, q.retval())!=null);
	    retvalT = _TEMP(q, quadDeriv.typeMap(q, q.retval()), retval);
	}
      
	// clone & type retex.
	retex = m_ctm.tempMap(q.retex());
	// (exception value should never have a derived type)
	Util.assert(quadDeriv.typeMap(q, q.retex())!=null);
	retexT= _TEMP(q, quadDeriv.typeMap(q, q.retex()), retex);

	// deal with function pointer.
	func  = _TEMP(q.ptr(), q);
	ptr = q.isVirtual() ? // extra dereference for virtual functions.
	    (Exp) makeMEM(q, HClass.Void, func) : (Exp) func;
	    
	qParams = q.params(); params = null; 
	for (int i=qParams.length-1; i >= 0; i--) {
	    params = new ExpList(_TEMP(qParams[i], q), params);      
	}

	addStmt(new CALL
	    (m_tf, q, 
	     retvalT, retexT,
	     ptr, params,
	     new NAME(m_tf, q, label(q.nextEdge(1/*exception edge*/))),
	     q.isTailCall()));
	if (edgeOracle.defaultEdge(q)!=0)
	    addStmt(new JUMP(m_tf, q, label(q.nextEdge(0))));

	// RESULTS OF CALLS SHOULD NEVER BE FOLDED! (assert this here?)
    }

    // just refer to the runtime's NameMap
    public void visit(PFCONST q) {
	addMove
	    (q, q.dst(),
	     new NAME(m_tf, q, m_nm.label(q.field())));
    }

    public void visit(PFIELD q) { 
	addMove
	    (q, q.dst(),
	     m_rtb.fieldBase(m_tf, q,
			     _TEMPte(q.objectref(), q)));
    }
  
    public void visit(PFOFFSET q) {
	addMove
	    (q, q.dst(),
	     m_rtb.fieldOffset(m_tf, q, q.field()));
    }

    // runtime-independent
    public void visit(PGET q) {
	MEM m = makeMEM(q, q.type(), _TEMP(q.ptr(), q));
	addMove(q, q.dst(), m);
    }
  
    // just refer to the runtime's NameMap
    public void visit(PMCONST q) { 
	addMove(q, q.dst(),
		new NAME(m_tf, q, m_nm.label(q.method())));
    }

    public void visit(PMETHOD q) {
	addMove
	    (q, q.dst(),
	     m_rtb.methodBase(m_tf, q,
			      _TEMPte(q.objectref(), q)));
    }

    public void visit(PMOFFSET q) {
	addMove(q, q.dst(),
		m_rtb.methodOffset(m_tf, q, q.method()));
    }

    public void visit(POPER q) {
	Exp oper = null; int optype; 
	Stm s0;
	Temp[] operands = q.operands();
	TEMP dst;
  
	// Convert optype to a Bop or a Uop
	switch(q.opcode()) {
	case Qop.ACMPEQ:
	case Qop.DCMPEQ:
	case Qop.FCMPEQ:
	case Qop.ICMPEQ:
	case Qop.LCMPEQ:
	case LQop.PCMPEQ: optype = Bop.CMPEQ; break;
	case Qop.D2F:
	case Qop.I2F:
	case Qop.L2F: optype = Uop._2F; break;
	case Qop.D2I:
	case Qop.F2I:
	case Qop.L2I: optype = Uop._2I; break;
	case Qop.D2L:
	case Qop.F2L:
	case Qop.I2L: optype = Uop._2L; break; 
	case Qop.I2D:
	case Qop.F2D:
	case Qop.L2D: optype = Uop._2D; break;
	case Qop.DADD: 
	case Qop.FADD:
	case Qop.IADD:
	case Qop.LADD: 
	case LQop.PADD: optype = Bop.ADD; break;
	case Qop.DCMPGE:
	case Qop.FCMPGE: optype = Bop.CMPGE; break;
	case Qop.DCMPGT:
	case Qop.FCMPGT:
	case Qop.ICMPGT:
	case Qop.LCMPGT: 
	case LQop.PCMPGT: optype = Bop.CMPGT; break;
	case Qop.DDIV:
	case Qop.FDIV:
	case Qop.IDIV:
	case Qop.LDIV: optype = Bop.DIV; break;
	case Qop.DMUL:
	case Qop.FMUL:
	case Qop.IMUL:
	case Qop.LMUL: optype = Bop.MUL; break;
	case Qop.DNEG:   
	case Qop.FNEG: 
	case Qop.INEG:
	case Qop.LNEG:
	case LQop.PNEG: optype = Uop.NEG; break;
	case Qop.DREM:
	case Qop.FREM:
	case Qop.IREM:
	case Qop.LREM: optype = Bop.REM; break;
	case Qop.I2B: optype = Uop._2B; break;
	case Qop.I2C: optype = Uop._2C; break;
	case Qop.I2S: optype = Uop._2S; break;
	case Qop.IAND:
	case Qop.LAND: optype = Bop.AND; break;
	case Qop.IOR:
	case Qop.LOR: optype = Bop.OR; break;
	case Qop.IXOR:
	case Qop.LXOR: optype = Bop.XOR; break;
	case Qop.ISHL:
	case Qop.LSHL: 
	case Qop.ISHR:
	case Qop.LSHR: 
	case Qop.IUSHR:
	case Qop.LUSHR: 
	    visitShiftOper(q); return;  // Special case
 	default: 
	    throw new Error("Unknown optype in ToTree: "+q.opcode());
	}
    
	if (operands.length==1) {
	    Exp op0 = _TEMP(operands[0], q);
	    oper = new UNOP(m_tf, q, op0.type(), optype, op0);
	}
	else if (operands.length==2) {
	    Exp op0 = _TEMP(operands[0], q), op1 = _TEMP(operands[1], q);
	    oper = new BINOP
		(m_tf, q, 
		 MERGE_TYPE(op0.type(), op1.type()),
		 optype,
		 op0, 
		 op1);
	}
	else 
	    throw new Error("Unexpected # of operands: " + q);
    
	addMove(q, q.dst(), oper);
    }

    private void visitShiftOper(POPER q) { 
	int optype; OPER oper;
	Temp[] operands = q.operands();

	switch (q.opcode()) { 
	case Qop.ISHL:
	case Qop.LSHL: 
	    optype = Bop.SHL; break;
	case Qop.LUSHR: 
	case Qop.IUSHR:
	    optype = Bop.USHR; break;
	case Qop.ISHR:
	case Qop.LSHR: 
	    optype = Bop.SHR; break;
	default: 
	    throw new Error("Not a shift optype: " + q.opcode());
	}

	Exp op0 = _TEMP(operands[0], q), op1 = _TEMP(operands[1], q);
	oper = new BINOP(m_tf, q, op0.type(), optype, op0, op1);
	addMove(q, q.dst(), oper);
    }
  
    public void visit(PSET q) {
	Exp src = _TEMP(q.src(), q), ptr = _TEMP(q.ptr(), q);
	MEM m = makeMEM(q, q.type(), ptr);
	Stm s0 = new MOVE(m_tf, q, m, src);
	addStmt(s0);
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*
     *                                                          *
     *                   Utility Functions                      *
     *                                                          *
     *+++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/

    private void addStmt(Stm stm) { 
        m_stmList.add(stm);
    }

    // foldable _TEMP
    private Translation.Exp _TEMPte(Temp quadTemp, Quad useSite) {
	// this constructor takes quad temps.
	Util.assert(quadTemp.tempFactory()!=m_tf.tempFactory(),
		    "Temp should be from LowQuad factory, not Tree factory.");
	// use reachingDefs to find definition sites.
	Set defSites = reachingDefs.reachingDefs(useSite, quadTemp);
	if (defSites.size()==1) {
	    HCodeElement hce = (HCodeElement) defSites.iterator().next();
	    if (foldNanny.canFold(hce, quadTemp))
		// fold this use!
		return (Translation.Exp)
		    foldMap.remove(Default.pair(hce,quadTemp));
	}
	TypeBundle tb = mergeTypes(quadTemp, defSites);
	Temp treeTemp = m_ctm.tempMap(quadTemp);
	TEMP result = new TEMP(m_tf, useSite, tb.simpleType, treeTemp);
	if (tb.classType!=null)
	    treeDeriv.putTypeAndTemp(result, tb.classType, treeTemp);
	else
	    treeDeriv.putDerivation(result, tb.derivation);
	return new Translation.Ex(result);
    }
    // creates a properly typed TEMP -- may fold this use!
    private Exp _TEMP(Temp quadTemp, Quad useSite) {
	return _TEMPte(quadTemp, useSite).unEx(m_tf);
    }
    private TEMP _TEMP(HCodeElement src, HClass type, Temp treeTemp) {
	// this constructor takes TreeTemps.
	Util.assert(treeTemp.tempFactory()==m_tf.tempFactory(),
		    "Temp should be from Tree factory.");
	TEMP result = new TEMP(m_tf, src, TYPE(type), treeTemp);
	treeDeriv.putTypeAndTemp(result, type, treeTemp);
	return result;
    }
    private TEMP _TEMP(HCodeElement src, DList deriv, Temp treeTemp) {
	// this constructor takes TreeTemps.
	Util.assert(treeTemp.tempFactory()==m_tf.tempFactory(),
		    "Temp should be from Tree factory.");
	TEMP result = new TEMP(m_tf, src, Type.POINTER, treeTemp);
	treeDeriv.putDerivation(result, deriv);
	return result;
    }
    // make a move.  unless, of course, the expression should be folded.
    private void addMove(Quad defSite, Temp quadTemp, Translation.Exp value) {
	// this constructor takes quad temps.
	Util.assert(quadTemp.tempFactory()!=m_tf.tempFactory(),
		    "Temp should be from LowQuad factory, not Tree factory.");
	if (foldNanny.canFold(defSite, quadTemp)) {
	    foldMap.put(Default.pair(defSite, quadTemp), value);
	    return;
	}
	// otherwise... make Tree.MOVE
	HClass type = quadDeriv.typeMap(defSite, quadTemp);
	Temp treeTemp = m_ctm.tempMap(quadTemp);
	TEMP dst = new TEMP(m_tf, defSite, TYPE(type), treeTemp);
	MOVE m = new MOVE(m_tf, defSite, dst, value.unEx(m_tf));
	if (type!=null)
	    treeDeriv.putTypeAndTemp(dst, type, treeTemp);
	else
	    treeDeriv.putDerivation(dst, 
				    quadDeriv.derivation(defSite, quadTemp));
	addStmt(m);
	return;
    }
    private void addMove(Quad defSite, Temp quadTemp, Exp value) {
	addMove(defSite, quadTemp, new Translation.Ex(value));
    }
    // storage for folded definitions.
    private final Map foldMap = new HashMap();

    private TypeBundle mergeTypes(Temp t, Set defSites) {
	Util.assert(defSites.size() > 0);
	
	TypeBundle tb = null;
	for (Iterator it=defSites.iterator(); it.hasNext(); ) {
	    Quad def = (Quad) it.next();
	    TypeBundle tb2 = (quadDeriv.typeMap(def, t)!=null) ?
		new TypeBundle(quadDeriv.typeMap(def, t)) :
		new TypeBundle(quadDeriv.derivation(def, t));
	    tb = (tb==null) ? tb2 : tb.merge(tb2);
	}
	return tb;
    }

    // make a properly-sized MEM
    private MEM makeMEM(HCodeElement source, HClass type, Exp ptr) {
	MEM result;
	if (type.equals(HClass.Boolean) || type.equals(HClass.Byte))
	    result = new MEM(m_tf, source, 8, true, ptr);
	else if (type.equals(HClass.Char))
	    result = new MEM(m_tf, source, 16, false, ptr);
	else if (type.equals(HClass.Short))
	    result = new MEM(m_tf, source, 16, true, ptr);
	else
	    result = new MEM(m_tf, source, maptype(type), ptr);
	treeDeriv.putType(result, type);// update type information!
	return result;
    }

    // Implmentation of binary numeric promotion found in the Java
    // language spec. 
    private int MERGE_TYPE(int type1, int type2) { 
	boolean longptrs = m_tf.getFrame().pointersAreLong();
	if (type1==type2) return type1;
	else { 
	    if (type1==Type.DOUBLE || type2==Type.DOUBLE) { 
		Util.assert(type1!=Type.POINTER && type2!=Type.POINTER);
		return Type.DOUBLE;
	    }
	    else if (type1==Type.FLOAT || type2==Type.FLOAT) { 
		Util.assert(type1!=Type.POINTER && type2!=Type.POINTER);
		return Type.FLOAT;
	    }
	    else if (type1==Type.LONG || type2==Type.LONG) { 
		if (type1==Type.POINTER || type2==Type.POINTER) { 
		    Util.assert(longptrs); return Type.POINTER;
		}
		return Type.LONG;
	    }
	    else if (type1==Type.POINTER || type2==Type.POINTER) { 
		return Type.POINTER;
	    }
	    else {
		return Type.INT;  // Should not get this far
	    }
	}
    }

    private Exp mapconst(HCodeElement src, Object value, HClass type) {
	Exp constant;

	if (type==HClass.Void) // HClass.Void reserved for null constants
	    constant = new CONST(m_tf, src);
	/* CSA: Sub-int types only seen in ARRAYINIT */
	else if (type==HClass.Boolean)
	    constant = new CONST
		(m_tf, src, ((Boolean)value).booleanValue()?1:0);
	else if (type==HClass.Byte)
	    constant = new CONST(m_tf, src, ((Byte)value).intValue()); 
	else if (type==HClass.Char)
	    constant = new CONST 
		(m_tf, src, 
		 (int)(((Character)value).charValue())); 
	else if (type==HClass.Short)
	    constant = new CONST(m_tf, src, ((Short)value).intValue()); 
	else if(type==HClass.Int) 
	    constant = new CONST(m_tf, src, ((Integer)value).intValue()); 
	else if (type==HClass.Long)
	    constant = new CONST(m_tf, src, ((Long)value).longValue());
	else if (type==HClass.Float)
	    constant = new CONST(m_tf, src, ((Float)value).floatValue()); 
	else if (type==HClass.Double)
	    constant = new CONST(m_tf, src, ((Double)value).doubleValue());
	else if (type==type.getLinker().forName("java.lang.String"))
	    constant = m_rtb.stringConst(m_tf, src, (String)value).unEx(m_tf);
	else 
	    throw new Error("Bad type for CONST: " + type); 
	return constant;
    }
    private CONST constZero(HCodeElement src, HClass type) {
	if (type==HClass.Boolean || type==HClass.Byte ||
	    type==HClass.Char || type==HClass.Short ||
	    type==HClass.Int) 
	    return new CONST(m_tf, src, (int)0);
	else if (type==HClass.Long)
	    return new CONST(m_tf, src, (long)0);
	else if (type==HClass.Float)
	    return new CONST(m_tf, src, (float)0);
	else if (type==HClass.Double)
	    return new CONST(m_tf, src, (double)0);
	else
	    return new CONST(m_tf, src); // null.
    }
}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
//                                                            //
//                    Utility classes                         //
//                                                            //
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//

    /** An edge oracle tells you which edge out of an
     *  <code>HCodeElement</code> wants to be the default
     *  (ie, non-branching) edge. */
    static interface EdgeOracle {
	public int defaultEdge(HCodeElement hce);
    }
    /** A fold nanny tells you whether or not you can fold a
     *  particular definition of a <code>Temp</code>. */
    static interface FoldNanny {
	public boolean canFold(HCodeElement defSite, Temp t);
    }
	
    /** A <code>TypeBundle</code> rolls together the <code>HClass</code>
     *  type or <code>DList</code> derivation of a value, along with
     *  the integer <code>Tree.Type</code>. */
    private static class TypeBundle {
	final int simpleType;
	final HClass classType;
	final DList derivation;
	TypeBundle(HClass hc) {
	    this.simpleType = TYPE(hc);
	    this.classType = hc;
	    this.derivation = null;
	}
	TypeBundle(DList deriv) {
	    this.simpleType = Type.POINTER;
	    this.classType = null;
	    this.derivation = deriv;
	}
	TypeBundle merge(TypeBundle tb) {
	    if (this.derivation!=null) {
		Util.assert(this.equals(tb));
		return this;
	    }
	    Util.assert(false);return null;
	}
	public boolean equals(Object o) {
	    if (!(o instanceof TypeBundle)) return false;
	    TypeBundle tb = (TypeBundle) o;
	    if (this.simpleType != tb.simpleType) return false;
	    if (this.classType != null)
		return (this.classType == tb.classType);
	    Util.assert(this.derivation != null);
	    if (tb.derivation == null) return false;
	    return this.derivation.equals(tb.derivation);
	}
    }

    // UTILITY METHODS........

    private static int TYPE(HClass hc) { 
	if (hc==null || hc==HClass.Void) return Type.POINTER;
	return maptype(hc);
    }

    private static int maptype(HClass hc) {
	if (hc==HClass.Boolean ||
	    hc==HClass.Byte    ||
	    hc==HClass.Char    ||
	    hc==HClass.Short   ||
	    hc==HClass.Int)          return Type.INT;
	else if (hc==HClass.Long)    return Type.LONG;
	else if (hc==HClass.Float)   return Type.FLOAT;
	else if (hc==HClass.Double)  return Type.DOUBLE;
	else                         return Type.POINTER;
    }

} // end of public class ToTree
