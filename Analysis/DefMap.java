// DefMap.java, created Fri Jan 29 17:16:38 1999 by pnkfelix
// Copyright (C) 1999 Felix S Klock <pnkfelix@mit.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Analysis;

import harpoon.Temp.Temp;
import harpoon.ClassFile.HCode;
import harpoon.ClassFile.HCodeElement;
import harpoon.Util.Util;
import harpoon.Util.Set;
import harpoon.IR.Properties.UseDef;

import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Vector;


/**
 * <code>DefMap</code> maps <code>Temp</code>s to the
 * <code>HCodeElement</code>s which define them.  The
 * <code>DefMap</code> caches its results, so you should throw away
 * the current <code>DefMap</code> object and make another one if you
 * make modifications to the IR for the <code>HCode</code> associated
 * with it. 
 * 
 * @author  Felix S Klock <pnkfelix@mit.edu>
 * @version $Id: DefMap.java,v 1.1.2.1 1999-01-30 23:29:04 pnkfelix Exp $
 */
public class DefMap extends TempToHceArrayMap {

    private Temp[] allDefs;

    /** Creates a <code>DefMap</code> for <code>hc</code>. 
	<BR> requires: <code>hc</code>'s internal representation
	        implements <code>harpoon.IR.Properties.UseDef</code>.
	<BR> effects: creates a <code>DefMap</code> for
	        <code>hc</code>, performing the necessary analysis
		during construction.
    */
    public DefMap( HCode hc ) {
	super(hc);
	analyze();
    }
    
    /* Helper method for analysis of <code>this.hcode</code> during
       construction.  
       <BR> requires: <code>hc</code>'s internal representation
                implements <code>harpoon.IR.Properties.UseDef</code>.
       <BR> effects: performs Variable->Def analysis on
                <code>this.hcode</code> 
    */
    private void analyze() {
	HCodeElement[] hces = hcode.getElements();
	Util.assert(hces instanceof harpoon.IR.Properties.UseDef[],
		    hcode.getName() + " does not implement UseDef");
	harpoon.IR.Properties.UseDef[] udl = 
	    (harpoon.IR.Properties.UseDef[]) hces;

	Hashtable tmpDef = new Hashtable();

	// scan HCodeElements, associating defs with their
	// HCodeElement. 
	for(int i=0; i<hces.length; i++) {
	    associate( hces[i], udl[i].def(), tmpDef);
	}

	// Store the final set in the main map.
	Enumeration e=tmpDef.keys();
	Vector defs = new Vector();
	while ( e.hasMoreElements() ) {
	    Temp def = (Temp) e.nextElement();
 	    storeTempMapping(def, (Set) tmpDef.get(def));
	    defs.addElement(def);
	}
	allDefs = new Temp[defs.size()];
	defs.copyInto( allDefs );
    }
    
    /** Return the <code>HCodeElement</code>s which define a given
	<code>Temp</code> in the <code>HCode</code> associated with
	this.
	<BR> effects: Returns an <code>HCodeElement</code> array of
	              all definitions of <code>t</code>.
    */
    public HCodeElement[] defMap(Temp t) {
	HCodeElement[] r = extractTempMapping(t);
	return (r == null) ? 
	    (HCodeElement[]) hcode.elementArrayFactory().newArray(0) :
	    (HCodeElement[]) Util.safeCopy(hcode.elementArrayFactory(), r);
    }
    
    /** Returns an array of all <code>Temp</code> defined in the
	<code>HCode</code> associated with this.  
    */
    public Temp[] allDefs() {
	return (Temp[]) Util.safeCopy(Temp.arrayFactory, allDefs);
    }

}



