// AbstrCallGraph.java, created Wed Apr 10 23:37:29 2002 by salcianu
// Copyright (C) 2000 Alexandru SALCIANU <salcianu@MIT.EDU>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Analysis.Quads;

import java.util.Map;
import java.util.HashMap;
import java.util.List;

import harpoon.ClassFile.HMethod;
import harpoon.ClassFile.HCodeFactory;
import harpoon.IR.Quads.Code;
import harpoon.IR.Quads.Quad;
import harpoon.IR.Quads.CALL;


/**
 * <code>AbstrCallGraph</code> contains some common code for several
 * implementations of <code>CallGraph</code>.
 * 
 * @author  Alexandru SALCIANU <salcianu@MIT.EDU>
 * @version $Id: AbstrCallGraph.java,v 1.2 2002-04-12 06:00:56 salcianu Exp $
 */
abstract class AbstrCallGraph implements CallGraph {

    // the code factory that produces the code of the methods
    protected HCodeFactory hcf;
    
    /** Returns a list of all <code>CALL</code>s quads in the code 
	of <code>hm</code>. */
    public CALL[] getCallSites(final HMethod hm) {
	CALL[] retval = (CALL[]) cache_cs.get(hm);
	if(retval == null) {
	    Code code = (Code) hcf.convert(hm);
	    if(code == null) {
		retval = new CALL[0];
	    }
	    else {
		List<Quad> l = code.selectCALLs();
		retval = l.toArray(new CALL[l.size()]);
	    }
	    cache_cs.put(hm, retval);
	}
	return retval;
    }
    final private Map cache_cs = new HashMap();
    final private static CALL[] empty_array = new CALL[0];
}
