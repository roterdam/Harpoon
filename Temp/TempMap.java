// TempMap.java, created Sat Sep 12 21:13:23 1998 by cananian
// Copyright (C) 1998 C. Scott Ananian <cananian@alumni.princeton.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Temp;

/**
 * A <code>TempMap</code> maps one temp to another temp.  It is typically
 * used to represent a set of variable renamings.
 * 
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: TempMap.java,v 1.2.2.1 1998-12-11 22:21:06 cananian Exp $
 */

public interface TempMap  {
    /** Rename a single <code>Temp</code>. */
    public Temp tempMap(Temp t);
}
