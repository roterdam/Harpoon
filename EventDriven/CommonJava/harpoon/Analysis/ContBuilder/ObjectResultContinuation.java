// ObjectResultContinuation.java, created Fri Nov  5 14:34:24 1999 by kkz
// Copyright (C) 1999 Karen K. Zee <kkzee@alum.mit.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Analysis.ContBuilder;

/**
 * <code>ObjectResultContinuation</code>
 * 
 * @author Karen K. Zee <kkzee@alum.mit.edu>
 * @version $Id: ObjectResultContinuation.java,v 1.1 2000-03-17 18:49:06 bdemsky Exp $
 */
public interface ObjectResultContinuation extends Continuation {

    public void resume(Object result);

}
