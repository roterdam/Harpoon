// CachingCodeFactory.java, created Sun Jan 31 16:45:26 1999 by cananian
// Copyright (C) 1999 C. Scott Ananian <cananian@alumni.princeton.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.ClassFile;

import java.util.HashMap;
import java.util.Map;

/**
 * A <code>CachingCodeFactory</code> caches the conversions performed by
 * a parent <code>HCodeFactory</code>; the cache can also be directly
 * modified in order to add or replace method implementations.
 * 
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: CachingCodeFactory.java,v 1.5 2005-09-30 19:01:47 salcianu Exp $
 */
public class CachingCodeFactory implements SerializableCodeFactory {
    /** Parent code factory. Creates the representations this
     *  <code>CachingCodeFactory</code> caches.
     * @serial */
    public final HCodeFactory parent;
    /** Representation cache. */
    private final Map h = new HashMap(); // map impl must support null vals
    /** Flush the representation cache before being saving to disk? */
    private final boolean flushBeforeSave;

    /** Creates a <code>CachingCodeFactory</code> using the conversions
     *  performed by <code>parent</code>.  Cached <code>HCode</code>s
     *  will be flushed before this code factory is serialized. */
    public CachingCodeFactory(HCodeFactory parent) {
	this(parent, false);
    }
    /** Creates a <code>CachingCodeFactory</code> using the conversions
     *  performed by <code>parent</code>.  If <code>saveCode</code> is
     *  <code>true</code>, cached <code>HCode</code>s will *not* be
     *  flushed before serialization. */
    public CachingCodeFactory(HCodeFactory parent, boolean saveCode) {
        this.parent = parent;
	this.flushBeforeSave = !saveCode;
    }
    /** Serializable interface. */
    private void writeObject(java.io.ObjectOutputStream out)
	throws java.io.IOException {
	if (flushBeforeSave) h.clear();
	out.defaultWriteObject();
    }
    /** Returns the name of the <code>HCode</code>s generated by this
     *  <code>CachingCodeFactory</code>.  Returns the same name as
     *  <code>parent</code> does. */
    public String getCodeName() { return parent.getCodeName(); }
    /** Convert a method to an <code>HCode</code>, caching the result.
     *  Cached representations of <code>m</code> in <code>parent</code> are
     *  cleared when this <code>CachingCodeFactory</code> adds the
     *  converted representation of <code>m</code> to its cache. */
    public HCode convert(HMethod m) {
	if (h.containsKey(m)) return (HCode) h.get(m); // even if get()==null.
	HCode hc = parent.convert(m);
	put(m, hc);
	return hc;
    }
    /** Remove the cached representation of <code>m</code> from this
     *  <code>CachingCodeFactory</code> and its <code>parent</code>.
     */
    public void clear(HMethod m) {
	h.remove(m);
	parent.clear(m);
    }
    /** Add or replace a representation for <code>m</code> to this
     *  <code>CachingCodeFactory</code>.  This can be used to update
     *  the representation of a method with an optimized or otherwise
     *  modified version.  As a side-effect, clears any cached representations
     *  of <code>m</code> from <code>parent</code>. */
    public void put(HMethod m, HCode hc) {
	h.put(m, hc); // works even if hc==null
	// [AS] do we really need this?
	parent.clear(m);
    }
}
