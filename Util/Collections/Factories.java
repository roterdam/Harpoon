// Factories.java, created Tue Oct 19 23:21:25 1999 by pnkfelix
// Copyright (C) 1999 Felix S. Klock II <pnkfelix@mit.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Util.Collections;

import harpoon.Util.Util;

import java.util.Iterator;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.List;
import java.util.Set;

/** <code>Factories</code> consists exclusively of static methods that
    operate on or return <code>CollectionFactory</code>s. 
 
    @author  Felix S. Klock II <pnkfelix@mit.edu>
    @version $Id: Factories.java,v 1.1.2.4 2000-02-01 05:30:57 cananian Exp $
 */
public final class Factories {
    
    /** Private ctor so no one will instantiate this class. */
    private Factories() {
        
    }
    
    /** Returns a <code>MapFactory</code> that generates
	<code>HashMap</code>s. */ 
    public static MapFactory hashMapFactory() {
	return new MapFactory() {
	    public java.util.Map makeMap(java.util.Map map) {
		return new java.util.HashMap(map);
	    }
	};
    }
    
    /** Returns a <code>SetFactory</code> that generates
	<code>HashSet</code>s. */
    public static SetFactory hashSetFactory() {
	return new SetFactory() {
	    public java.util.Set makeSet(java.util.Collection c) {
		return new java.util.HashSet(c);
	    }
	};
    }
    
    /** Returns a <code>SetFactory</code> that generates
	<code>WorkSet</code>s. */
    public static SetFactory workSetFactory() { return workSetFactory; }
    private static final SetFactory workSetFactory = new SetFactory() {
	    public java.util.Set makeSet(java.util.Collection c) {
		return new harpoon.Util.WorkSet(c);
	    }
    };
    
    /** Returns a <code>ListFactory</code> that generates
	<code>LinkedList</code>s. */
    public static ListFactory linkedListFactory() {
	return new ListFactory() {
	    public java.util.List makeList(java.util.Collection c) {
		return new java.util.LinkedList(c);
	    }
	};
    }

    /** Returns a <code>ListFactory</code> that generates
	<code>ArrayList</code>s. */
    public static ListFactory arrayListFactory() {
	return new ListFactory() {
	    public java.util.List makeList(java.util.Collection c) {
		return new java.util.ArrayList(c);
	    }
	};
    }
    
    /** Returns a <code>CollectionFactory</code> that generates
	synchronized (thread-safe) <code>Collection</code>s.  
	The <code>Collection</code>s generated are backed by the 
	<code>Collection</code>s generated by <code>cf</code>. 
	@see Collections#synchronizedCollection
    */
    public static CollectionFactory
	synchronizedCollectionFactory(final CollectionFactory cf) { 
	return new CollectionFactory() {
	    public java.util.Collection makeCollection(Collection c) {
		return Collections.synchronizedCollection
		    (cf.makeCollection(c));
	    }
	};
    }

    /** Returns a <code>SetFactory</code> that generates synchronized
	(thread-safe) <code>Set</code>s.  The <code>Set</code>s
	generated are backed by the <code>Set</code>s generated by
	<code>sf</code>. 
	@see Collections#synchronizedSet
    */
    public static SetFactory 
	synchronizedSetFactory(final SetFactory sf) {
	return new SetFactory() {
	    public java.util.Set makeSet(Collection c) {
		return Collections.synchronizedSet(sf.makeSet(c));
	    }
	};
    }

    /** Returns a <code>ListFactory</code> that generates synchronized
	(thread-safe) <code>List</code>s.   The <code>List</code>s
	generated are backed by the <code>List</code>s generated by
	<code>lf</code>. 
	@see Collections#synchronizedList
    */
    public static ListFactory
	synchronizedListFactory(final ListFactory lf) {
	return new ListFactory() {
	    public java.util.List makeList(Collection c) {
		return Collections.synchronizedList(lf.makeList(c));
	    }
	};
    }

    /** Returns a <code>MapFactory</code> that generates synchronized
	(thread-safe) <code>Map</code>s.  The <code>Map</code>s
	generated are backed by the <code>Map</code> generated by
	<code>mf</code>.
	@see Collections#synchronizedMap
    */
    public static MapFactory
	synchronizedMapFactory(final MapFactory mf) {
	return new MapFactory() {
	    public java.util.Map makeMap(java.util.Map map) {
		return Collections.synchronizedMap(mf.makeMap(map));
	    }
	};
    }

    public static CollectionFactory 
	noNullCollectionFactory(final CollectionFactory cf) {
	return new CollectionFactory() {
	    public java.util.Collection makeCollection(final Collection c) {
		Util.assert(noNull(c));
		final Collection back = cf.makeCollection(c);
		return new CollectionWrapper(back) {
		    public boolean add(Object o) {
			Util.assert(o != null);
			return super.add(o);
		    }
		    public boolean addAll(Collection c2) {
			Util.assert(Factories.noNull(c2));
			return super.addAll(c2);
		    }
		};
	    }
	};
    }

    private static boolean noNull(Collection c) {
	Iterator iter = c.iterator();
	while(iter.hasNext()) {
	    if(iter.next() == null) return false;
	}
	return true;
    }

}




