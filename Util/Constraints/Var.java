// Var.java, created Sun Apr  7 18:56:08 2002 by salcianu
// Copyright (C) 2000 Alexandru SALCIANU <salcianu@MIT.EDU>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Util.Constraints;

/** <code>Var</code> is a variable that may appear in a constraint.
    It is just an <code>Object</code> with an unique id to make debug
    easier.

    @author  Alexandru SALCIANU <salcianu@MIT.EDU>
    @version $Id: Var.java,v 1.2 2004-03-05 15:38:10 salcianu Exp $ */
public class Var implements Comparable<Var> {
    // counter used to generate unique ids (for debug)
    private static int counter;
    // thread-safe way of getting a unique id!
    private static synchronized int get_id() { return counter++; }

    // due to this unique id, a Var is equal only to itself
    private int id = get_id();
    
    /** String representation of <code>this</code> variable:
	\quot;V<i>id</i>&quot; where <i>id</i> is a unique integer
	id. */
    public String toString() { return "V" + id; }

    public int compareTo(Var v) {
	if(id < v.id) return -1;
	if(id > v.id) return 1;
	return 0;
    }
}
