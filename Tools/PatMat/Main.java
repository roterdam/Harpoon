// Main.java, created Wed Feb 17 22:05:18 1999 by cananian
// Copyright (C) 1999 C. Scott Ananian <cananian@alumni.princeton.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Tools.PatMat;

import harpoon.Util.Util;

import java.io.Reader;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.IOException;

/* Main entry point for the instruction selection tool.
 *
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: Main.java,v 1.1.2.7 1999-06-30 05:50:18 pnkfelix Exp $
 */
public class Main {
    private static boolean DEBUG_parser = false;
    private static boolean DEBUG_production = false;
    private static boolean DEBUG_maxMunch = true;


    public static void main(String args[]) throws Exception {
	Reader r = new BufferedReader(new FileReader(args[0]));
	ErrorMsg e = new ErrorMsg(args[0]);
	Lexer l = new Lexer(r, e);
	Parser p = new Parser(l, e);
	Spec s = (Spec) p.parse().value;
	//	Util.assert(!e.anyErrors, "Error in parsing " + args[0]);

	if (DEBUG_parser) {
	    System.out.println(s);
	} else if (DEBUG_production) {
	    PrintWriter pw = new PrintWriter(System.out);
	    TestCGG t = new TestCGG(s);
	    t.outputJavaFile(pw);
	} else if (DEBUG_maxMunch) {
	    PrintWriter pw = new PrintWriter(System.out);
	    MaximalMunchCGG t = new MaximalMunchCGG(s, "CodeGen");
	    t.outputJavaFile(pw); 
	}
    }

    static class TestCGG extends CodeGeneratorGenerator {
	TestCGG(Spec s) {
	    super(s, "TestCggClassDontUse");
	}

	public void outputSelectionMethod(PrintWriter out) {
	    out.println();
	    out.println(spec);
	    out.println();
	}
    }
 
}

