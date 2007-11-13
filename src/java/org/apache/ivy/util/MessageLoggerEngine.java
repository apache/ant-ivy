/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivy.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

/**
 * A {@link MessageLogger} implementation delegating the work to the current top logger on a stack.
 * <p>
 * When the logger stack is empty, it delegates the work to a default logger, which by default is
 * the {@link Message#getDefaultLogger()}.
 * </p>
 * <p>
 * {@link #pushLogger(MessageLogger)} should be called to delegate to a new logger, and
 * {@link #popLogger()} should be called when the context of this logger is finished.
 * </p>
 */
public class MessageLoggerEngine implements MessageLogger {
    private final Stack/*<MessageLogger>*/ loggerStack = new Stack();
    
    private MessageLogger defaultLogger = Message.getDefaultLogger();
    
    public MessageLoggerEngine() {
    }

    /**
     * Sets the logger used when the stack is empty.
     * 
     * @param defaultLogger the logger to use when the stack is empty.
     */
    public void setDefaultLogger(MessageLogger defaultLogger) {
        this.defaultLogger = defaultLogger;
    }



    /**
     * Push a logger on the stack.
     * 
     * @param logger
     *            the logger to push. Must not be <code>null</code>.
     */
    public void pushLogger(MessageLogger logger) {
        Checks.checkNotNull(logger, "logger");
        loggerStack.push(logger);
    }
    
    /**
     * Pops a logger from the logger stack.
     * <p>
     * Does nothing if the logger stack is empty
     * </p>
     */
    public void popLogger() {
        if (!loggerStack.isEmpty()) {
            loggerStack.pop();
        }
    }

    /**
     * Returns the current logger, or the default one if there is no logger in the stack
     * @return the current logger, or the default one if there is no logger in the stack
     */
    private MessageLogger peekLogger() {
        if (loggerStack.isEmpty()) {
            return defaultLogger;
        }
        return (MessageLogger) loggerStack.peek();
    }

    // consolidated methods
    
    public List getErrors() {
        List errors = new ArrayList();
        errors.addAll(defaultLogger.getErrors());
        for (Iterator iter = loggerStack.iterator(); iter.hasNext();) {
            MessageLogger l = (MessageLogger) iter.next();
            errors.addAll(l.getErrors());
        }
        return errors;
    }

    public List getProblems() {
        List problems = new ArrayList();
        problems.addAll(defaultLogger.getProblems());
        for (Iterator iter = loggerStack.iterator(); iter.hasNext();) {
            MessageLogger l = (MessageLogger) iter.next();
            problems.addAll(l.getProblems());
        }
        return problems;
    }

    public List getWarns() {
        List warns = new ArrayList();
        warns.addAll(defaultLogger.getWarns());
        for (Iterator iter = loggerStack.iterator(); iter.hasNext();) {
            MessageLogger l = (MessageLogger) iter.next();
            warns.addAll(l.getWarns());
        }
        return warns;
    }

    public void sumupProblems() {
        MessageLoggerHelper.sumupProblems(this);
        clearProblems();
    }
    
    public void clearProblems() {
        defaultLogger.clearProblems();
        for (Iterator iter = loggerStack.iterator(); iter.hasNext();) {
            MessageLogger l = (MessageLogger) iter.next();
            l.clearProblems();
        }
    }

    public void setShowProgress(boolean progress) {
        defaultLogger.setShowProgress(progress);
        // updates all loggers in the stack
        for (Iterator iter = loggerStack.iterator(); iter.hasNext();) {
            MessageLogger l = (MessageLogger) iter.next();
            l.setShowProgress(progress);
        }
    }
    
    public boolean isShowProgress() {
        // testing the default logger is enough, all loggers should be in sync
        return defaultLogger.isShowProgress();
    }

    // delegation methods
    
    public void debug(String msg) {
        peekLogger().debug(msg);
    }
    
    public void deprecated(String msg) {
        peekLogger().deprecated(msg);
    }

    public void endProgress() {
        peekLogger().endProgress();
    }

    public void endProgress(String msg) {
        peekLogger().endProgress(msg);
    }

    public void error(String msg) {
        peekLogger().error(msg);
    }

    public void info(String msg) {
        peekLogger().info(msg);
    }

    public void rawinfo(String msg) {
        peekLogger().rawinfo(msg);
    }

    public void log(String msg, int level) {
        peekLogger().log(msg, level);
    }

    public void progress() {
        peekLogger().progress();
    }

    public void rawlog(String msg, int level) {
        peekLogger().rawlog(msg, level);
    }

    public void verbose(String msg) {
        peekLogger().verbose(msg);
    }

    public void warn(String msg) {
        peekLogger().warn(msg);
    }

    
}
