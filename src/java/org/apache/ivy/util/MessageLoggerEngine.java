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
    private final ThreadLocal<Stack<MessageLogger>> loggerStacks = new ThreadLocal<Stack<MessageLogger>>();

    private MessageLogger defaultLogger = null;

    private List<String> problems = new ArrayList<String>();

    private List<String> warns = new ArrayList<String>();

    private List<String> errors = new ArrayList<String>();

    private Stack<MessageLogger> getLoggerStack() {
        Stack<MessageLogger> stack = loggerStacks.get();
        if (stack == null) {
            stack = new Stack<MessageLogger>();
            loggerStacks.set(stack);
        }
        return stack;
    }

    public MessageLoggerEngine() {
    }

    /**
     * Sets the logger used when the stack is empty.
     * 
     * @param defaultLogger
     *            the logger to use when the stack is empty.
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
        getLoggerStack().push(logger);
    }

    /**
     * Pops a logger from the logger stack.
     * <p>
     * Does nothing if the logger stack is empty
     * </p>
     */
    public void popLogger() {
        if (!getLoggerStack().isEmpty()) {
            getLoggerStack().pop();
        }
    }

    /**
     * Returns the current logger, or the default one if there is no logger in the stack
     * 
     * @return the current logger, or the default one if there is no logger in the stack
     */
    public MessageLogger peekLogger() {
        if (getLoggerStack().isEmpty()) {
            return getDefaultLogger();
        }
        return getLoggerStack().peek();
    }

    private MessageLogger getDefaultLogger() {
        // we don't store the logger returned by Message.getDefaultLogger() to always stay in sync
        // as long as our default logger has not been set explicitly with setDefaultLogger()
        return defaultLogger == null ? Message.getDefaultLogger() : defaultLogger;
    }

    // consolidated methods
    public void warn(String msg) {
        peekLogger().warn(msg);
        problems.add("WARN:  " + msg);
        warns.add(msg);
    }

    public void error(String msg) {
        peekLogger().error(msg);
        problems.add("\tERROR: " + msg);
        errors.add(msg);
    }

    public List<String> getErrors() {
        return errors;
    }

    public List<String> getProblems() {
        return problems;
    }

    public List<String> getWarns() {
        return warns;
    }

    public void sumupProblems() {
        MessageLoggerHelper.sumupProblems(this);
        clearProblems();
    }

    public void clearProblems() {
        getDefaultLogger().clearProblems();
        for (MessageLogger l : getLoggerStack()) {
            l.clearProblems();
        }
        problems.clear();
        errors.clear();
        warns.clear();
    }

    public void setShowProgress(boolean progress) {
        getDefaultLogger().setShowProgress(progress);
        // updates all loggers in the stack
        for (MessageLogger l : getLoggerStack()) {
            l.setShowProgress(progress);
        }
    }

    public boolean isShowProgress() {
        // testing the default logger is enough, all loggers should be in sync
        return getDefaultLogger().isShowProgress();
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

}
