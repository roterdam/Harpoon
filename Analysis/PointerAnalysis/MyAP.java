// MyAP.java, created Mon Apr  3 18:22:05 2000 by salcianu
// Copyright (C) 2000 Alexandru SALCIANU <salcianu@MIT.EDU>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Analysis.PointerAnalysis;

import harpoon.Analysis.Maps.AllocationInformation;
import harpoon.Temp.Temp;

import harpoon.Util.Util;

/**
 * <code>MyAP</code> is my own implementation for the
 <code>AllocationProperties</code>. 
 * 
 * @author  Alexandru SALCIANU <salcianu@MIT.EDU>
 * @version $Id: MyAP.java,v 1.1.2.4 2000-05-17 20:24:17 salcianu Exp $
 */
public class MyAP implements AllocationInformation.AllocationProperties,
			     java.io.Serializable {
    
    // hasInteriorPointers
    public boolean hip = true;
    // canBeStackAllocated
    public boolean sa  = false;
    // canBeThreadAllocated
    public boolean ta  = false;
    // useOwnHeap
    public boolean uoh = false;
    // make heap (true at the thread object creation sites)
    public boolean mh  = false;

    // the Temp pointing to the thread object on whose stack
    // the NEW quad is going to allocate the object; "null"
    // to indicate the current thread.
    public Temp ah = null;

    /** Creates a <code>MyAP</code>. */
    public MyAP() {}


    public boolean hasInteriorPointers(){
	return hip;
    }

    public boolean canBeStackAllocated(){
	return sa;
    }

    public boolean canBeThreadAllocated(){
	return ta;
    }

    public boolean makeHeap(){
	return mh;
    }

    public Temp allocationHeap(){
	return ah;
    }

    /** Pretty printer for debug. */
    public String toString(){
	String hipstr = 
	    hasInteriorPointers() ? "interior ptrs; " : "no interior ptrs; ";

	if(makeHeap()) hipstr = hipstr + " [make heap] ";

	if(canBeStackAllocated())
	    return hipstr + "Stack allocation";

	if(canBeThreadAllocated())
	    if(allocationHeap() != null)
		return (hipstr + "Thread allocation on the heap of " +
			allocationHeap());
	    else
		return
		    hipstr + "Thread allocation on the current thread's heap";

	return hipstr + "Global heap allocation";
    }
}
