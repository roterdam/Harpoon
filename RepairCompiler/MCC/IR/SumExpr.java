package MCC.IR;

import java.util.*;

public class SumExpr extends Expr {

    SetDescriptor sd;
    RelationDescriptor rd;


    public SumExpr(SetDescriptor sd, RelationDescriptor rd) {
        if (sd == null||rd==null) {
            throw new NullPointerException();
        }
        this.sd=sd;
        this.rd=rd;
    }

    public String name() {
	return "sum("+sd.toString()+"."+rd.toString()+")";
    }

    public boolean equals(Map remap, Expr e) {
	if (e==null||!(e instanceof SumExpr))
	    return false;
	SumExpr se=(SumExpr)e;
	return (se.sd==sd)&&(se.rd==rd);
    }

    public boolean usesDescriptor(Descriptor d) {
        return (sd==d)||(rd==d);
    }

    public Set useDescriptor(Descriptor d) {
        HashSet newset=new HashSet();
        if ((d==sd)||(d==rd))
            newset.add(this);
        return newset;
    }

    public Descriptor getDescriptor() {
        throw new Error("Sum shouldn't appear on left hand side!");
    }

    public boolean inverted() {
	return false;
    }

    public Set getRequiredDescriptors() {
        HashSet v=new HashSet();
        v.add(sd);
        v.add(rd);
        return v;
    }

    public void generate(CodeWriter writer, VarDescriptor dest) {
        writer.outputline("int "+dest.getSafeSymbol()+"=0;");

        VarDescriptor itvd=VarDescriptor.makeNew("iterator");
        writer.outputline("struct SimpleIterator "+itvd.getSafeSymbol()+";");
        writer.outputline("SimpleHashiterator("+sd.getSafeSymbol()+"_hash , &"+itvd.getSafeSymbol()+");");
        writer.outputline("while (hasNext(&"+itvd.getSafeSymbol()+")) {");
        VarDescriptor keyvd=VarDescriptor.makeNew("key");
        writer.outputline("int "+keyvd.getSafeSymbol()+"=next(&"+itvd.getSafeSymbol()+");");
        VarDescriptor tmpvar=VarDescriptor.makeNew("tmp");
        writer.outputline("int "+tmpvar.getSafeSymbol()+";");
        writer.outputline("SimpleHashget("+rd.getSafeSymbol()+"_hash, "+keyvd.getSafeSymbol()+", &"+tmpvar.getSafeSymbol()+");");
        writer.outputline(dest.getSafeSymbol()+"+="+tmpvar.getSafeSymbol()+";");
        writer.outputline("}");
    }

    public void prettyPrint(PrettyPrinter pp) {
        pp.output("sum(");
        pp.output(sd.toString());
        pp.output(".");
        pp.output(rd.toString());
        pp.output(")");
    }

    public TypeDescriptor typecheck(SemanticAnalyzer sa) {
        this.td = ReservedTypeDescriptor.INT;
        return this.td;
    }

    public Set getInversedRelations() {
        return new HashSet();
    }

}