// ServoThread.java, created by wbeebee
// Copyright (C) 2003 Wes Beebee <wbeebee@mit.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package ipaq;

import imagerec.util.Servo;
/**
 * {@link ServoThread} is a thread which controls the servos.
 *
 * @see Servo
 *
 * @author Wes Beebee <<a href="mailto:wbeebee@mit.edu">wbeebee@mit.edu</a>>
 */
public class ServoThread extends Thread {
    private final int servo;
    private int start, stop;
    private long time;
    private static final IPaqServoController car = new IPaqServoController();
    
    /** 
     * Construct a new servo controlling thread and start it.
     *
     * @param servo the servo number to control (1-8).
     */
    public ServoThread(int servo) { 
	this.servo = servo;
	start(); 
    }
    
    /** 
     * Execute a command on the servo.
     */
    public synchronized void run() {
	while (true) {
	    try {
		wait();
		//System.out.println(" ServoThread #"+servo+": notified, running commands");
		car.moveLocal(servo, start);
		sleep(time);
		car.moveLocal(servo, stop);
		//System.out.println(" ServoThread#"+servo+": done running commands");
	    } catch (InterruptedException e) {
		System.out.println(e.toString());
		/* Keep on goin' */
	    }
	}
    }
    
    /** 
     * Send a command to the servo.
     *
     * @param start The starting motion.
     * @param time How long to do it (in milliseconds).
     * @param stop The stopping motion.
     */
    public synchronized void command(int start, long time, int stop) {
	//System.out.println("ServoThread #"+servo+" got command:");
	//System.out.println("   start="+start+" time="+time+" stop="+stop);
	this.start = start;
	this.time = time;
	this.stop = stop;
	notify();
    }

    public synchronized void command2(int start, long time, int stop) {
	car.moveDelayMoveLocal(servo, start, time, stop);
    }
}

