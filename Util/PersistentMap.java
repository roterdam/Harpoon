// PersistentTree.java, created Wed Mar 31 18:41:03 1999 by cananian
// Copyright (C) 1999 C. Scott Ananian <cananian@alumni.princeton.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Util;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;
/**
 * <code>PersistentMap</code> implements a persistent map, based on a
 * binary search tree.
 * 
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: PersistentMap.java,v 1.1.2.1 1999-04-08 06:56:39 cananian Exp $
 */
public class PersistentMap  {
    final PersistentTreeNode root;
    final Comparator c;

    /** Creates an empty <code>PersistentMap</code> whose
     *  key objects will all implement <code>java.lang.Comparable</code>. 
     */
    public PersistentMap() {
	this.root = null; this.c = Default.comparator;
    }
    /** Creates an empty <code>PersistentMap</code> whose
     *  key objects are ordered by the given <code>Comparator</code>.
     */
    public PersistentMap(Comparator c) {
	this.root = null; this.c = c;
    }
    /** Creates a <code>PersistentMap</code> from a root <code>Node</code>
     *  and a <code>Comparator</code>.*/
    private PersistentMap(PersistentTreeNode root, Comparator c) {
	this.root = root; this.c = c;
    }

    /** Determines if this <code>PersistentMap</code> has any mappings. */
    public boolean isEmpty() { return (root==null); }

    /** Count the number of key->value mappings in this
     *  <code>PersistentMap</code>. */
    public int size() { return PersistentTreeNode.size(root); }

    /** Creates and returns a new <code>PersistantMap</code> identical to
     *  this one, except it contains a mapping from <code>key</code> to
     *  <code>value. */
    public PersistentMap put(Object key, Object value) {
	PersistentTreeNode new_root =
	    PersistentTreeNode.put(this.root, this.c, key, value);
	return (this.root == new_root) ? this :
	    new PersistentMap(new_root, c);
    }
    /** Gets the value which <code>key</code> maps to. */
    public Object get(Object key) {
	PersistentTreeNode np = 
	    PersistentTreeNode.get(this.root, this.c, key);
	return (np==null)?null:np.value;
    }
    /** Determines if there is a mapping for the given <code>key</code>. */
    public boolean containsKey(Object key) {
	return (PersistentTreeNode.get(this.root, this.c, key)!=null);
    }

    /** Make a new <code>PersistentMap</code> identical to this one,
     *  except that it does not contain a mapping for <code>key</code>. */
    public PersistentMap remove(Object key) {
	PersistentTreeNode new_root = 
	    PersistentTreeNode.remove(this.root, this.c, key);
	return (this.root == new_root) ? this :
	    new PersistentMap(new_root, c);
    }
    
    /** Human-readable representation of the map. */
    public String toString() { return asMap().toString(); }

    /*---------------------------------------------------------------*/
    /** <code>java.util.Collection</code>s view of the mapping. */
    public Map asMap() {
	return new AbstractMap() {
	    public boolean containsKey(Object key) {
		return PersistentMap.this.containsKey(key);
	    }
	    public Object get(Object key) {
		return PersistentMap.this.get(key);
	    }
	    public boolean isEmpty() {
		return PersistentMap.this.isEmpty();
	    }
	    public int size() {
		return PersistentMap.this.size();
	    }
	    public Set entrySet() {
		return new AbstractSet() {
		    public int size() {
			return PersistentMap.this.size();
		    }
		    public Iterator iterator() {
			final Stack s = new Stack();
			if (root!=null) s.push(root);

			return new Iterator() {
			    public boolean hasNext() {
				return !s.isEmpty();
			    }
			    public Object next() {
				if (s.isEmpty())
				    throw new NoSuchElementException();
				final PersistentTreeNode n =
				    (PersistentTreeNode) s.pop();
				if (n.right!=null) s.push(n.right);
				if (n.left!=null)  s.push(n.left);
				return (Map.Entry) n;
			    }
			    public void remove() {
				throw new UnsupportedOperationException();
			    }
			};
		    }
		};
	    }
	};
    }
}
