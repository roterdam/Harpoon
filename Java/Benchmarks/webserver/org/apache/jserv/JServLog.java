/*
 * Copyright (c) 1997-1999 The Java Apache Project.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. All advertising materials mentioning features or use of this
 *    software must display the following acknowledgment:
 *    "This product includes software developed by the Java Apache 
 *    Project for use in the Apache JServ servlet engine project
 *    <http://java.apache.org/>."
 *
 * 4. The names "Apache JServ", "Apache JServ Servlet Engine" and 
 *    "Java Apache Project" must not be used to endorse or promote products 
 *    derived from this software without prior written permission.
 *
 * 5. Products derived from this software may not be called "Apache JServ"
 *    nor may "Apache" nor "Apache JServ" appear in their names without 
 *    prior written permission of the Java Apache Project.
 *
 * 6. Redistributions of any form whatsoever must retain the following
 *    acknowledgment:
 *    "This product includes software developed by the Java Apache 
 *    Project for use in the Apache JServ servlet engine project
 *    <http://java.apache.org/>."
 *    
 * THIS SOFTWARE IS PROVIDED BY THE JAVA APACHE PROJECT "AS IS" AND ANY
 * EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE JAVA APACHE PROJECT OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Java Apache Group. For more information
 * on the Java Apache Project and the Apache JServ Servlet Engine project,
 * please see <http://java.apache.org/>.
 *
 */

package org.apache.jserv;

import java.io.IOException;
import java.io.Writer;
import org.apache.java.util.Configurations;
import org.apache.java.io.LogWriter;
import org.apache.java.io.Logger;

/**
 * This class is used to trace and report errors and execution.
 * Made it a delegating class in order to ignore errors opening
 * the logfile (hen)
 * @author Stefano Mazzocchi
 * @version $Revision: 1.1 $ $Date: 2000-06-29 01:41:56 $
 * @see org.apache.java.io.LogWriter
 */
final class JServLog implements JServSendError, Logger {

    private String errorMessage = null;
    private LogWriter logger = null;

    /*
     * this is just here for compatibility reasons.
     * throuhout the code, this public field is
     * asked directly ..
     * If this is not much a performace leak 
     * (this implementation is 'final')
     */
    public boolean active = false;

    /**
     * Construct this class
     */
    public JServLog(String identifier, Configurations confs) {
        logger = null;
        try {
            logger = new LogWriter (identifier, confs);
            active = logger.active;
        }
        catch (IOException e) {
            e.printStackTrace();
            errorMessage = e.getMessage();
        }
    }
    
    /** --- Logger interface --- **/
     // this isn't used yet since everyone gets the public field
     // 'active' directly ..
    /**
     * Tells if it is active.
     *
     */
    public boolean isActive() { 
        return (logger != null) ? logger.active : false;
    }

    /**
     * Tells if the given channel is active.
     *
     * @param  channel  the channel to test.
     */
    public boolean isActive(String channel) {
        return (logger != null) ? logger.isActive (channel) : false;
    }

    /**
     * Prints the log message on the right channel.
     * <p>
     * A "channel" is a virtual log that may be enabled or disabled by
     * setting the property "identifier".channel.???=true where ??? is the
     * channel identifier that must be passed with the message.
     * If a channel is not recognized or its property is set to false
     * the message is not written.
     *
     * @param   channel       the channel to put the message on.
     * @param   name          the message to log.
     */
    public void log(String channel, String message) {
        if (logger != null) logger.log (channel, message);
    }

    /**
     * Prints the error message and stack trace if channel enabled.
     *
     * @param t the error thrown.
     */
    public void log(String channel, Throwable t) {
        if (logger != null) logger.log (channel, t);
    }

    /**
     * Flush the log.
     *
     * Write any pending messages into the log media.
     */
    public void flush() {
        if (logger != null) logger.flush();
    }

    /** --- JServSendError interface --- **/
    /**
     * Report a problem encountered while initializing.
     */
    public void sendError(int sc, String msg) {
        log(null, JServConnection.findStatusString(sc) + ": " + msg);
    }

    /**
     * Report an exception or error encountered while loading a servlet.
     */
    public void sendError(Throwable error) {
        log(null, error);
    }

    /** --- function to test this subsystem. Should be made an Interface --- **/
    /**
     * returns the last Error occured in this subsystem
     */
    public String getSubsystemError () {
        return errorMessage;
    }

    /**
     * returns true if this subsystem is sane
     */
    public boolean isSane () {
        return (logger != null);
    }
}