// WriteBarrier.java, created Wed Aug 15 19:21:17 2001 by kkz
// Copyright (C) 2000 Karen Zee <kkz@tmi.lcs.mit.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Runtime.PreciseGC;

/**
 * <code>WriteBarrier</code> is an abstract class that provides
 * a dummy write-barrier for generational garbage collection.
 * 
 * @author  Karen Zee <kkz@tmi.lcs.mit.edu>
 * @version $Id: WriteBarrier.java,v 1.2 2002-02-25 21:06:40 cananian Exp $
 */
public abstract class WriteBarrier {
    
    /** dummy write barrier for PSETs */
    public static native void storeCheck(Object o);
    
    /** dummy write barrier for SETs*/
    public static native void fsc(Object o, java.lang.reflect.Field f, 
				  Object val, int id);

    /** dummy write barrier for ASETs */
    public static native void asc(Object o, int index, Object val, int id);
}