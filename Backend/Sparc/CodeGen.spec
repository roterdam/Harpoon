// CodeGen.spec, created Mon Jun 28 23:00:48 1999 by cananian -*- Java -*-
// Copyright (C) 1999 C. Scott Ananian <cananian@alumni.princeton.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Backend.Sparc;

import harpoon.ClassFile.HCodeElement;
import harpoon.IR.Assem.Instr;
import harpoon.IR.Assem.InstrFactory;
import harpoon.IR.Tree.Type;
import harpoon.IR.Tree.Typed;
/**
 * <code>CodeGen</code> is a code-generator for the Sparc architecture.
 * 
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: CodeGen.spec,v 1.1.2.2 1999-06-29 05:24:53 cananian Exp $
 */
%%
    // INNER CLASSES
    /** Sub-class to represent delay-slots.
     * <code>optimize()</code> uses this class information to determine that
     * it should rearrange code to try to eliminate these instructions.
     * @author C. Scott Ananian
     * @see Sparc.CodeGen#optimize
     */
    public class DELAYSLOT extends Instr {
	// a nop to fill the delay slot
	public DELAYSLOT(InstrFactory if, HCodeElement source) {
	    super(if, source, "\t nop \t ! delay slot \n");
	}
    }
    /** Sub-class to indicate dependencies on the condition code register
     *  for later optimization.  This prevents putting the <code>cmp</code>
     *  instruction in the delay-slot of its branch-conditional.
     */
    public class CC extends Instr {
	//a new instr type to indicate dependency on the condition code reg
	public CC(InstrFactory if, HCodeElement source, String assem,
	          Temp[] dst, Temp src[] src) {
	    super(if, source, assem, dst, src);
	}
	/** FIXME: How does Instr represent LabelLists? */
    }

    /** Determine whether a constant can fit in the immediate field of
     *  a SPARC instruction. */
    static boolean is13bit(Number n) {
	if (n instanceof Double || n instanceof Float) return false;
	return ((-4096<=n.longValue()) && (n.longValue()<=4095));
    }
    /** Return proper suffix based on type. */
    static String suffix(Typed t) {
      String r="";
      if (Type.isDoubleWord(t.type())) r+="d";
      if (Type.isFloatingPoint(t.type())) r+="f";
      return r;
    }
%%
/* this comment will be eaten by the spec lexer (unlike the ones above) */

// patterns with MOVE at their root.
//  32- and 64-bit integer and floating-point STOREs:
MOVE(MEM(CONST(c)), e) %pred %( is13bit(c) )% %{
 String op=((Typed) ROOT.dst).isDoubleWord()?"std":"st";
 emit(new InstrMEM(if, ROOT,
		   "\t st"+suffix((Typed)ROOT.dst)+" `s0, ["+c+"]\n",
		   null, new Temp[] { e }));
}%
MOVE(MEM(BINOP(ADD, CONST(c), e1)), e2) %pred %( is13bit(c) )% %{
 String op=((Typed) ROOT.dst).isDoubleWord()?"std":"st";
 emit(new InstrMEM(if, ROOT,
		   "\t st"+suffix((Typed)ROOT.dst)+" `s1, [`s0+"+c+"]\n",
		   null, new Temp[] { e1, e2 }));
}%
MOVE(MEM(BINOP(ADD, e1, CONST(c))), e2) %pred %( is13bit(c) )% %{
 String op=((Typed) ROOT.dst).isDoubleWord()?"std":"st";
 emit(new InstrMEM(if, ROOT,
		   "\t st"+suffix((Typed)ROOT.dst)+" `s1, [`s0+"+c+"]\n", null,
		   new Temp[] { e1, e2 }));
}%
MOVE(MEM(BINOP(ADD, e1, e2)), e3) %{
 String op=((Typed) ROOT.dst).isDoubleWord()?"std":"st";
 emit(new InstrMEM(if, ROOT,
		   "\t st"+suffix((Typed)ROOT.dst)+" `s2, [`s0+`s1]\n", null,
                   new Temp[] { e1, e2, e3 }));
}% 
MOVE(MEM(e1), e2) %{
 emit(new InstrMEM(if, ROOT,
		   "\t st"+suffix((Typed)ROOT.dst)+" `s1, [`s0]\n", null,
		   new Temp[] { e1, e2 }));
}%
// other MOVEs
MOVE(e1, CONST<i,p>(c)) %pred %( is13bit(c) )% %{
 emit(new Instr(if, ROOT, "\t mov "+c+", `d0\n",
		new Temp[] { e1 }, null));
}%
MOVE(e1, e2) %{ /* catch-all */
 emit(new Instr(if, ROOT, "\t mov `s0, `d0\n",
		new Temp[] { e1 }, new Temp[] { e2 }));
}%

// patterns with EXP at their root.
EXP(e1) %{
  /* throw away temp e1 (not used) */
}%

// patterns with JUMP at their root.
JUMP(NAME(l)) %{
 emit(new Instr(if, ROOT, "\t b "+l+"\n", null, null)); /* target list?! */
 emit(new DELAYSLOT(if, ROOT));
}%
JUMP(BINOP(ADD, e1, e2)) %{
 emit(new Instr(if, ROOT, "\t jmpl `s0+`s1, %g0\n", null,
		new Temp[] { e1, e2 })); /* target list?! */
 emit(new DELAYSLOT(if, ROOT));
}%
JUMP(e1) %{
 emit(new Instr(if, ROOT, "\t jmpl `s0, %g0\n", null,
		new Temp[] { e1 })); /* target list?! */
 emit(new DELAYSLOT(if, ROOT));
}%

// patterns with a CJUMP at their root.
CJUMP(e, true_label, false_label) %{
 emit(new CC(if, ROOT, "\t cmp `s0, 0\n", null,
	     new Temp[] { e1 }));
 emit(new CC(if, ROOT, "\t bne "+true_label+"\n", null, null));/*target!?*/
 emit(new DELAYSLOT(if, ROOT));
 /* the next two lines can hopefully be left out. */
 emit(new CC(if, ROOT, "\t ba "+false_label+"\n", null, null));/*target?!*/
 emit(new DELAYSLOT(if, ROOT));
}%

// patterns with SEQ at their root should be handled by the generator magically

// labels.
LABEL(l) %{
 emit(new InstrLABEL(if, ROOT, l.toString()+":\n", ROOT.label));
}%

// expressions
CONST<i,l,p>(0)=r %{
 emit(new Instr(if, ROOT, "\t mov %g0, `d0\n", new Temp[] {r}, null));
}%
CONST<i,p>(c)=r %{
 if (is13bit(c))
   emit(new Instr(if, ROOT, "\t set "+c+", `d0\n", new Temp[] {r}, null));
 else {
   emit(new Instr(if, ROOT, "\t sethi %hi("+c+"), `d0\n", new Temp[]{r},null));
   emit(new Instr(if, ROOT, "\t or `s0, %lo("+c+"), `d0\n",
		  new Temp[] { r }, new Temp[] { r }));
 }
}%
    // FIXME: long and floating-point constants (change section?)

NAME(s)=r %{
 emit(new Instr(if, ROOT, "\t sethi %hi("+s+"), `d0\n",
		new Temp[]{r},null));
 emit(new Instr(if, ROOT, "\t or `s0, %lo("+s+"), `d0\n",
		new Temp[] { r }, new Temp[] { r }));
}%
/*TEMP(t)=r should be handled by c-g-g. */

