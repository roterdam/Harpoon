// Liveness.java, created Thu Oct 28 00:31:56 1999 by kkz
// Copyright (C) 1999 Karen K. Zee <kkzee@alum.mit.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Analysis;

import harpoon.ClassFile.HCode;
import harpoon.ClassFile.HCodeElement;

import java.util.Set;

/**
 * <code>Liveness</code> defines an abstract class for live variable analysis.
 * 
 * @author Karen K. Zee <kkzee@alum.mit.edu>
 * @version $Id: Liveness.java,v 1.1.2.2 2000-02-01 03:06:35 pnkfelix Exp $
 */
public abstract class Liveness {
    protected final HCode hc;

    /** Creates a <code>Liveness</code> object from
     *  provided <code>HCode</code>.
     */
    public Liveness(HCode hc) {
	this.hc = hc;
    }
    
    /** Returns the <code>Set</code> of <code>Temp</code>s 
     *  that are live-in at the <code>HCodeElement</code>. 
     */
    public abstract Set getLiveIn(HCodeElement hce);

    /** Returns the <code>Set</code> of <code>Temp</code>s 
     *  that are live-out at the <code>HCodeElement</code>. 
     */
    public abstract Set getLiveOut(HCodeElement hce);
}
