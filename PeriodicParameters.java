// PeriodicParameters.java, created by cata
// Copyright (C) 2001 Catalin Francu <cata@mit.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.

package javax.realtime;

import java.util.Date;
import java.util.LinkedList;
import java.util.Iterator;

/** This release parameter indicates that the <code>waitForNextPeriod()</code>
 *  method on the associated <code>Schedulable</code> object will be unblocked
 *  at the start of each period. When a reference to a
 *  <code>PeriodicParameters</code> object is given as a parameter to a
 *  constructor, the <code>PeriodicParameters</code> object becomes bound to
 *  the object being created. Changes to the values in the
 *  <code>PeriodicParameters</code> object affect the constructed object. If
 *  fiben to more than one constructor then changes to the values in the
 *  <code>PeriodicParameters</code> object affect <i>all</i> the associated objects.
 *  Note that this is a one-to-many relationship, <i>not</i> many-to-many.
 */
public class PeriodicParameters extends ReleaseParameters {
    
    HighResolutionTime start;
    RelativeTime period;
    LinkedList schList = new LinkedList();
    
    public PeriodicParameters(HighResolutionTime start, RelativeTime period,
			      RelativeTime cost, RelativeTime deadline,
			      AsyncEventHandler overrunHandler,
			      AsyncEventHandler missHandler) {
	super(cost, (deadline == null) ? period : deadline,
	      overrunHandler, missHandler);
	
	// For now, we leave start as it is. When start() is called on a thread,
	// then we will set start accordingly. The problem is what do we do when
	// several threads share the same ReleaseParameters.
	this.start = start;
	this.period = new RelativeTime(period);
    }

    /** Get the period. */
    public RelativeTime getPeriod() {
	return period;
    }

    /** Get the start. */
    public HighResolutionTime getStart() {
	return start;
    }

    /** Returns true if, after considering the values of the parameters, the task set
     *  would still be feasible. In this case the values of the parameters are changed.
     *  Returns false if, after considering the values of the parameters, the task set
     *  would not be feasible. In this case the values of the parameters are not changed.
     */
    public boolean setIfFeasible(RelativeTime period,
				 RelativeTime cost,
				 RelativeTime deadline) {
	boolean b = true;
	for (Iterator it = schList.iterator(); it.hasNext(); ) {
	    Schedulable sch = (Schedulable)it.next();
	    Scheduler sched = sch.getScheduler();
	    if (!sched.isFeasible(sch, new PeriodicParameters(getStart(), period, cost, deadline,
							      overrunHandler, missHandler))) {
		b = false;
		break;
	    }
	}

	if (b) {
	    setPeriod(period);
	    setCost(cost);
	    setDeadline(deadline);
	}
	return b;
    }

    /** Set the period. */
    public void setPeriod(RelativeTime period) {
	this.period.set(period);
    }

    /** Set the start time. */
    public void setStart(HighResolutionTime start) {
	this.start.set(start);
    }

    /** Informs <code>this</code> that there is one more instance of <code>Schedulable</code>
     *  that uses <code>this</code> as its <code>ReleaseParameters</code>.
     */
    public boolean bindSchedulable(Schedulable sch) {
	return schList.add(sch);
    }

    /** Informs <code>this</code> that <code>Schedulable sch</code>
     *  that uses <code>this</code> as its <code>ReleaseParameters</code>.
     */
    public boolean unbindSchedulable(Schedulable sch) {
	return schList.remove(sch);
    }
}
