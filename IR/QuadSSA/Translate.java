// Translate.java, created Sat Aug  8 10:53:03 1998 by cananian
package harpoon.IR.QuadSSA;

import harpoon.Temp.Temp;
import harpoon.ClassFile.*;
import harpoon.ClassFile.Bytecode.Op;
import harpoon.ClassFile.Bytecode.Operand;
import harpoon.ClassFile.Bytecode.OpClass;
import harpoon.ClassFile.Bytecode.OpConstant;
import harpoon.ClassFile.Bytecode.OpLocalVariable;
import harpoon.ClassFile.Bytecode.Instr;
import harpoon.ClassFile.Bytecode.InGen;
import harpoon.ClassFile.Bytecode.InCti;
import harpoon.ClassFile.Bytecode.InMerge;
import harpoon.ClassFile.Bytecode.InSwitch;
import harpoon.ClassFile.Bytecode.Code.ExceptionEntry;

import java.lang.reflect.Modifier;
import java.util.Hashtable;
/**
 * <code>Translate</code> is a utility class to implement the
 * actual Bytecode-to-QuadSSA translation.
 * 
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: Translate.java,v 1.6 1998-08-24 20:59:13 cananian Exp $
 */

/*
 * To do: figure out interface for trans(State, ...)
 *        up to, but not including GETFIELD.
 */

/* State holds state *after* execution of corresponding instr. */
class Translate  { // not public.
    static class State { // inner class
	/** Current temps used for each position of stack.
	 *  <code>null</code>s are valid placeholders for empty spaces
	 *  in double-word representations. */
	Temp stack[];
	/** Current temps used for local variables */
	Temp lv[];
	/** Current try/catch contexts */
	ExceptionEntry tryBlock[];

	private State(Temp stack[], Temp lv[], ExceptionEntry tryBlock[]) {
	    this.stack = stack; this.lv = lv; this.tryBlock = tryBlock;
	}
	/** Make new state by popping top of stack */
	State pop() { return pop(1); }
	/** Make new state by popping multiple entries off top of stack */
	State pop(int n) {
	    Temp stk[] = new Temp[this.stack.length-n];
	    System.arraycopy(this.stack, n, stk, 0, stk.length);
	    return new State(stk, lv, tryBlock);
	}
	/** Make new state by pushing temp onto top of stack */
	State push(Temp t) {
	    Temp stk[] = new Temp[this.stack.length+1];
	    System.arraycopy(this.stack, 0, stk, 1, this.stack.length);
	    stk[0] = t;
	    return new State(stk, lv, tryBlock);
	}
	/** Make new state by exiting innermost try/catch context. */
	State exitTry() {
	    ExceptionEntry tb[] = new ExceptionEntry[this.tryBlock.length-1];
	    System.arraycopy(this.tryBlock, 1, tb, 0, tb.length);
	    return new State(stack, lv, tb);
	}
	/** Make new state by entering a new try/catch context. */
	State enterTry(ExceptionEntry ee) {
	    ExceptionEntry tb[] = new ExceptionEntry[this.tryBlock.length+1];
	    System.arraycopy(this.tryBlock, 0, tb, 1, this.tryBlock.length);
	    tb[0] = ee;
	    return new State(stack, lv, tb);
	}
	/** Make new state by changing the temp corresponding to an lv. */
	State assignLV(int lv_index, Temp t) {
	    Temp nlv[] = (Temp[]) lv.clone();
	    nlv[lv_index] = t;
	    return new State(stack, nlv, tryBlock);
	}
	/** Initialize state with temps corresponding to parameters. */
	State(Temp[] locals) {
	    this(new Temp[0], locals, new ExceptionEntry[0]);
	}
	/** Creates a new State object identical to this one. */
	public Object clone() {
	    return new State((Temp[]) stack.clone(), 
			     (Temp[]) lv.clone(), 
			     (ExceptionEntry[]) tryBlock.clone());
	}
    }

    /** Associates State objects with Instrs. */
    class StateMap {
	Hashtable map;
	StateMap() { map = new Hashtable(); }
	void put(Instr in, State s) { map.put(in, s); }
	State get(Instr in) { return (State) map.get(in); }
    }

    static final Quad trans(harpoon.ClassFile.Bytecode.Code bytecode) {
	// set up initial state.
	String[] paramNames = bytecode.getMethod().getParameterNames();
	Temp[] params = new Temp[paramNames.length];
	for (int i=0; i<params.length; i++)
	    params[i] = new Temp((paramNames[i]==null)?"param":paramNames[i]);

	Temp[] locals = new Temp[bytecode.getMaxLocals()];
	if (!Modifier.isStatic(bytecode.getMethod().getModifiers())) {
	    locals[0] = new Temp("this");
	    System.arraycopy(params, 0, locals, 1, params.length);
	    for (int i=params.length+1; i<locals.length; i++)
		locals[i] = new Temp("lv");
	} else {
	    System.arraycopy(params, 0, locals, 0, params.length);
	    for (int i=params.length; i<locals.length; i++)
		locals[i] = new Temp("lv");
	}

	State s = new State(locals);

	Quad quads = new METHODHEADER(params);

	// translate using state.
	//trans(s); // FIXME

	// return result.
	return quads;
    }
    static final void trans(StateMap s) {
	// FIXME do schtuff here.
    }

    static final Instr[] transBasicBlock(StateMap s, Instr in) {
	return null; // FIXME
    }
    static final Quad transInstr(StateMap s, Instr in) {
	if (in instanceof InGen) return transInstr(s, (InGen) in);
	if (in instanceof InCti) return transInstr(s, (InCti) in);
	if (in instanceof InMerge) return transInstr(s, (InMerge) in);
	throw new Error("Unknown Instr type.");
    }

    static final HClass objArray = HClass.forClass(Object[].class);
    static final HClass byteArray= HClass.forClass(byte[].class);
    static final HClass charArray= HClass.forClass(char[].class);
    static final HClass dblArray = HClass.forClass(double[].class);
    static final HClass fltArray = HClass.forClass(float[].class);

    static HMethod objArrayGet,  objArrayPut;
    static HMethod byteArrayGet, byteArrayPut;
    static HMethod charArrayGet, charArrayPut;
    static HMethod dblArrayGet,  dblArrayPut;
    static HMethod fltArrayGet,  fltArrayPut;

    static {
	objArrayGet = objArray.getMethod("get", new HClass[] {HClass.Int});
	objArrayPut = objArray.getMethod("put", 
		      new HClass[] {HClass.Int,HClass.forClass(Object.class)});
	byteArrayGet= byteArray.getMethod("get", new HClass[] {HClass.Int});
	byteArrayPut= byteArray.getMethod("put", 
		      new HClass[] {HClass.Int, HClass.forClass(byte.class)});
	charArrayGet= charArray.getMethod("get", new HClass[] {HClass.Int});
	charArrayPut= charArray.getMethod("put", 
		      new HClass[] {HClass.Int, HClass.forClass(char.class)});
	dblArrayGet = dblArray.getMethod("get", new HClass[] {HClass.Int});
	dblArrayPut = dblArray.getMethod("put", 
		      new HClass[] {HClass.Int,HClass.forClass(double.class)});
	fltArrayGet = fltArray.getMethod("get", new HClass[] {HClass.Int});
	fltArrayPut = fltArray.getMethod("put", 
		      new HClass[] {HClass.Int,HClass.forClass(float.class)});
    }

    static final Quad transInstr(StateMap sm, InGen in) {
	State s = sm.get(in.prev()[0]);
	State ns;
	Quad q;
	switch(in.getOpcode()) {
	case Op.AALOAD:
	    ns = s.pop(2).push(new Temp());
	    q = new CALL(in, objArrayGet, s.stack[1],
			 new Temp[] {s.stack[0]}, ns.stack[0]);
	    break;
	case Op.AASTORE:
	    ns = s.pop(3);
	    q = new CALL(in, objArrayPut, s.stack[2],
			 new Temp[] {s.stack[1], s.stack[0]});
	    break;
	case Op.ACONST_NULL:
	    ns = s.push(new Temp("null"));
	    q = new LET(in, ns.stack[0], new LeafConst(null, HClass.Void));
	    break;
	case Op.ALOAD:
	case Op.ALOAD_0:
	case Op.ALOAD_1:
	case Op.ALOAD_2:
	case Op.ALOAD_3:
	    {
		OpLocalVariable opd = (OpLocalVariable) in.getOperand(0);
		ns = s.push(s.lv[opd.getIndex()]);
		q = null;
		// Alternate implementation:
		//ns = s.push(new Temp());
		//q = new LET(in, ns.stack[0], 
		//	    new LeafTemp(s.lv[opd.getIndex()]));
		break;
	    }
	case Op.ANEWARRAY:
	    {
		OpClass opd = (OpClass) in.getOperand(0);
		HClass hc = HClass.forDescriptor("[" + 
						 opd.value().getDescriptor());
		ns = s.pop().push(new Temp());
		q = new NEW(in, ns.stack[0], hc);
		// XXX APPEND FIXME
		q /*+*/= new CALL(in, hc.getMethod("<init>","(I)V"),
			      ns.stack[0], new Temp[] {s.stack[0]});
		break;
	    }
	case Op.ARRAYLENGTH:
	    ns = s.pop().push(new Temp());
	    q = new GET(in, ns.stack[0], s.stack[0],
			objArray.getField("length")); // XXX BOGUS
	    // What if it's not an Object Array?
	    break;
	case Op.ASTORE:
	case Op.ASTORE_0:
	case Op.ASTORE_1:
	case Op.ASTORE_2:
	case Op.ASTORE_3:
	    {
	    OpLocalVariable opd = (OpLocalVariable) in.getOperand(0);
	    ns = s.pop().assignLV(opd.getIndex(), 
				  new Temp(s.lv[opd.getIndex()]));
	    q = new LET(in, ns.lv[opd.getIndex()], s.stack[0]);
	    break;
	    }
	case Op.BALOAD:
	    ns = s.pop(2).push(new Temp());
	    q = new CALL(in, byteArrayGet, s.stack[1],
			 new Temp[] {s.stack[0]}, ns.stack[0]);
	    break;
	case Op.BASTORE:
	    ns = s.pop(3);
	    q = new CALL(in, byteArrayPut, s.stack[2],
			 new Temp[] {s.stack[1], s.stack[0]});
	    break;
	case Op.BIPUSH:
	    {
		OpConstant opd = (OpConstant) in.getOperand(0);
		int val = ((Byte)opd.getValue()).intValue();
		ns = s.push(new Temp("iconst"));
		q = new LET(in, ns.stack[0], 
			    new LeafConst(new Integer(val), HClass.Int));
		break;
	    }
	case Op.CALOAD:
	    ns = s.pop(2).push(new Temp());
	    q = new CALL(in, charArrayGet, s.stack[1],
			 new Temp[] {s.stack[0]}, ns.stack[0]);
	    break;
	case Op.CASTORE:
	    ns = s.pop(3);
	    q = new CALL(in, charArrayPut, s.stack[2],
			 new Temp[] {s.stack[1], s.stack[0]});
	    break;
	case Op.CHECKCAST:
	    // translate as:
	    //  if (!(obj instanceof class))
	    //     throw new ClassCastException();
	    // XXX FIXME XXX
	    throw new Error("CHECKCAST unimplemented as of yet.");
	case Op.D2F:
	case Op.D2I:
	    ns = s.pop(2).push(new Temp());
	    q = new OPER(in, Op.toString(in.getOpcode()) /* "d2f" or "d2i" */,
			 ns.stack[0], new Temp[] { s.stack[0] });
	    break;
	case Op.D2L:
	    ns = s.pop(2).push(null).push(new Temp());
	    q = new OPER(in, "d2l", ns.stack[0], 
			 new Temp[] { s.stack[0] });
	    break;
	case Op.DADD:
	case Op.DDIV:
	case Op.DMUL:
	case Op.DREM:
	case Op.DSUB:
	    ns = s.pop(4).push(null).push(new Temp());
	    q = new OPER(in, Op.toString(in.getOpcode()), // dadd, ddiv or dmul
			 ns.stack[0], new Temp[] { s.stack[2], s.stack[0] });
	    break;
	case Op.DALOAD:
	    ns = s.pop(2).push(null).push(new Temp());
	    q = new CALL(in, dblArrayGet, s.stack[1],
			 new Temp[] {s.stack[0]}, ns.stack[0]);
	    break;
	case Op.DASTORE:
	    ns = s.pop(4);
	    q = new CALL(in, dblArrayPut, s.stack[3],
			 new Temp[] {s.stack[2], s.stack[0]});
	    break;
	case Op.DCMPG:
	case Op.DCMPL:
	    ns = s.pop(4).push(new Temp());
	    q = new OPER(in, Op.toString(in.getOpcode())/*"dcmpg" or "dcmpl"*/,
			 ns.stack[0], new Temp[] { s.stack[2], s.stack[0] });
	    break;
	case Op.DCONST_0:
	case Op.DCONST_1:
	    {
		OpConstant opd = (OpConstant) in.getOperand(0);
		ns = s.push(null).push(new Temp("dconst"));
		q = new LET(in, ns.stack[0],
			    new LeafConst(opd.getValue(), opd.getType()));
		break;
	    }
	case Op.DLOAD:
	case Op.DLOAD_0:
	case Op.DLOAD_1:
	case Op.DLOAD_2:
	case Op.DLOAD_3:
	    {
		OpLocalVariable opd = (OpLocalVariable) in.getOperand(0);
		ns = s.push(null).push(s.lv[opd.getIndex()]);
		q = null;
		break;
	    }
	case Op.DNEG:
	    ns = s.pop(2).push(null).push(new Temp());
	    q = new OPER(in, "dneg", ns.stack[0], new Temp[] {s.stack[0]});
	    break;
	case Op.DSTORE:
	case Op.DSTORE_0:
	case Op.DSTORE_1:
	case Op.DSTORE_2:
	case Op.DSTORE_3:
	    {
	    OpLocalVariable opd = (OpLocalVariable) in.getOperand(0);
	    ns = s.pop(2).assignLV(opd.getIndex(), 
				  new Temp(s.lv[opd.getIndex()]));
	    q = new LET(in, ns.lv[opd.getIndex()], s.stack[0]);
	    break;
	    }
	case Op.DUP:
	    ns = s.push(s.stack[0]);
	    q = null;
	    break;
	case Op.DUP_X1:
	    ns = s.pop(2).push(s.stack[0]).push(s.stack[1]).push(s.stack[0]);
	    q = null;
	    break;
	case Op.DUP_X2:
	    ns = s.pop(3).push(s.stack[0]).push(s.stack[2]).push(s.stack[1])
		.push(s.stack[0]);
	    q = null;
	    break;
	case Op.DUP2:
	    ns = s.push(s.stack[1]).push(s.stack[0]);
	    q = null;
	    break;
	case Op.DUP2_X1:
	    ns = s.pop(3).push(s.stack[1]).push(s.stack[0])
		.push(s.stack[2]).push(s.stack[1]).push(s.stack[0]);
	    q = null;
	    break;
	case Op.DUP2_X2:
	    ns = s.pop(4).push(s.stack[1]).push(s.stack[0])
		.push(s.stack[3]).push(s.stack[2])
		.push(s.stack[1]).push(s.stack[0]);
	    q = null;
	    break;
	case Op.F2D:
	case Op.F2L:
	case Op.I2D:
	case Op.I2L:
	    ns = s.pop().push(null).push(new Temp());
	    q = new OPER(in, Op.toString(in.getOpcode()), // "f2d" or "f2l"
			 ns.stack[0], new Temp[] {s.stack[0]});
	    break;
	case Op.F2I:
	case Op.I2B:
	case Op.I2C:
	case Op.I2F:
	case Op.I2S:
	    ns = s.pop().push(new Temp());
	    q = new OPER(in, Op.toString(in.getOpcode()),
			 ns.stack[0], new Temp[] {s.stack[0]});
	    break;
	case Op.FADD:
	case Op.FDIV:
	case Op.FMUL:
	case Op.FREM:
	case Op.FSUB:
	case Op.IADD:
	case Op.IAND:
	case Op.IDIV:
	    ns = s.pop(2).push(new Temp());
	    q = new OPER(in, Op.toString(in.getOpcode()), // fadd, fdiv, ...
			 ns.stack[0], new Temp[] {s.stack[1], s.stack[0]});
	    break;
	case Op.FALOAD:
	    ns = s.pop(2).push(new Temp());
	    q = new CALL(in, fltArrayGet, s.stack[1],
			 new Temp[] {s.stack[0]}, ns.stack[0]);
	    break;
	case Op.FASTORE:
	    ns = s.pop(3);
	    q = new CALL(in, fltArrayPut, s.stack[2],
			 new Temp[] {s.stack[1], s.stack[0]});
	    break;
	case Op.FCMPG:
	case Op.FCMPL:
	    ns = s.pop(2).push(new Temp());
	    q = new OPER(in, Op.toString(in.getOpcode())/*"fcmpg" or "fcmpl"*/,
			 ns.stack[0], new Temp[] { s.stack[1], s.stack[0] });
	    break;
	case Op.FCONST_0:
	case Op.FCONST_1:
	case Op.FCONST_2:
	case Op.ICONST_M1:
	case Op.ICONST_0:
	case Op.ICONST_1:
	case Op.ICONST_2:
	case Op.ICONST_3:
	case Op.ICONST_4:
	case Op.ICONST_5:
	    {
		OpConstant opd = (OpConstant) in.getOperand(0);
		ns = s.push(new Temp("const"));
		q = new LET(in, ns.stack[0],
			    new LeafConst(opd.getValue(), opd.getType()));
		break;
	    }
	case Op.FLOAD:
	case Op.FLOAD_0:
	case Op.FLOAD_1:
	case Op.FLOAD_2:
	case Op.FLOAD_3:
	case Op.ILOAD:
	case Op.ILOAD_0:
	case Op.ILOAD_1:
	case Op.ILOAD_2:
	case Op.ILOAD_3:
	    {
		OpLocalVariable opd = (OpLocalVariable) in.getOperand(0);
		ns = s.push(s.lv[opd.getIndex()]);
		q = null;
		break;
	    }
	case Op.FNEG:
	    ns = s.pop(2).push(new Temp());
	    q = new OPER(in, "fneg", ns.stack[0], new Temp[] {s.stack[0]});
	    break;
	case Op.FSTORE:
	case Op.FSTORE_0:
	case Op.FSTORE_1:
	case Op.FSTORE_2:
	case Op.FSTORE_3:
	    {
	    OpLocalVariable opd = (OpLocalVariable) in.getOperand(0);
	    ns = s.pop().assignLV(opd.getIndex(), 
				  new Temp(s.lv[opd.getIndex()]));
	    q = new LET(in, ns.lv[opd.getIndex()], s.stack[0]);
	    break;
	    }
	case Op.GETFIELD:
	case Op.GETSTATIC:
	    {
	    OpField opd = (OpField) in.getOperand(0);
	    if (opd.value().getType() == HClass.Double ||
		opd.value().getType() == HClass.Long) // 64-bit value.
		ns = s.pop().push(null).push(new Temp());
	    else // 32-bit value.
		ns = s.pop().push(new Temp());
	    q = new GET(in, ns.stack[0], s.stack[0], opd.value());
	    break;
	    }
	case Op.IALOAD:
	    ns = s.pop(2).push(new Temp());
	    q = new CALL(in, intArrayGet, s.stack[1],
			 new Temp[] {s.stack[0]}, ns.stack[0]);
	    break;
	case Op.IASTORE:
	    ns = s.pop(3);
	    q = new CALL(in, intArrayPut, s.stack[2],
			 new Temp[] {s.stack[1], s.stack[0]});
	    break;
	case Op.IF_ACMPEQ:
	case Op.IF_ACMPNE:
	case Op.IF_ICMPEQ:
	case Op.IF_ICMPNE:
	case Op.IF_ICMPLT:
	case Op.IF_ICMPGE:
	case Op.IF_ICMPGT:
	case Op.IF_ICMPLE:
	case Op.IFEQ:
	case Op.IFNE:
	case Op.IFLT:
	case Op.IFGE:
	case Op.IFGT:
	case Op.IFLE:
	case Op.IFNONNULL:
	case Op.IFNULL:
	    throw new Error("Ack!"); // FIXME
	    break;
	case Op.IINC:
	    {
		OpLocalVariable opd0 = (OpLocalVariable) in.getOperand(0);
		OpConstant opd1 = (OpConstant) in.getOperand(1);
		Temp constant = new Temp("const");
		ns = s.assignLV(opd0.getIndex(),
				new Temp(s.lv[opd0.getIndex()]));
		q = new CONST(in, constant, opd1.getValue(), opd1.getType());
		// FIXME append.
		q /*+*/= new OPER(in, "iadd", ns.lv[opd0.getIndex()],
				  new Temp[] { s.lv[opd0.getIndex()], constant});
		break;
	    }
	default:
	    throw new Error("Unknown InGen opcode.");
	}
	sm.put(in, ns);
	return q;
    }
    static final Quad transInstr(StateMap s, InCti in) {
	/*
	if (in instanceof InSwitch) {
	} else {
	    switch(in.getOpcode()) {
	    case Op.ARETURN:
	    case Op.DRETURN:
	    case Op.FRETURN:
	    case Op.ATHROW:
		ns = s.pop();
		q = new THROW(in, s.stack[0]);
		break;
	case Op.GOTO:
	case Op.GOTO_W:
	    ns = s;
	    q = new JMP(in);
	    break;
	    default:
	    }
	}
	*/
	return null;
    }
    static final Quad transInstr(StateMap s, InMerge in) {
	return null;
    }
}

