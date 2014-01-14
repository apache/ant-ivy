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

import java.util.List;

/**
 * A MessageLogger is used to log messages.
 * <p>
 * Where the messages are logged is depending on the implementation.
 * </p>
 * <p>
 * This interface provides both level specific methods ({@link #info(String)}, {@link #warn(String)}
 * , ...) and generic methods ({@link #log(String, int)}, {@link #rawlog(String, int)}). Note that
 * calling level specific methods is usually not equivalent to calling the generic method with the
 * corresponding level. Indeed, for warn and error level, the implementation will actually log the
 * message at a lower level (usually {@link Message#MSG_VERBOSE}) and log the message at the actual
 * level only when {@link #sumupProblems()} is called.
 * </p>
 * 
 * @see Message
 */
public interface MessageLogger {
    /**
     * Logs a message at the given level.
     * <p>
     * <code>level</code> constants are defined in the {@link Message} class.
     * </p>
     * 
     * @param msg
     *            the message to log
     * @param level
     *            the level at which the message should be logged.
     * @see Message#MSG_DEBUG
     * @see Message#MSG_VERBOSE
     * @see Message#MSG_INFO
     * @see Message#MSG_WARN
     * @see Message#MSG_ERROR
     */
    public abstract void log(String msg, int level);

    /**
     * Same as {@link #log(String, int)}, but without adding any contextual information to the
     * message.
     * 
     * @param msg
     *            the message to log
     * @param level
     *            the level at which the message should be logged.
     */
    public abstract void rawlog(String msg, int level);

    public abstract void debug(String msg);

    public abstract void verbose(String msg);

    public abstract void deprecated(String msg);

    public abstract void info(String msg);

    public abstract void rawinfo(String msg);

    public abstract void warn(String msg);

    public abstract void error(String msg);

    public abstract List<String> getProblems();

    public abstract List<String> getWarns();

    public abstract List<String> getErrors();

    /**
     * Clears the list of problems, warns and errors.
     */
    public abstract void clearProblems();

    /**
     * Sumup all problems encountered so far, and clear them.
     */
    public abstract void sumupProblems();

    public abstract void progress();

    public abstract void endProgress();

    public abstract void endProgress(String msg);

    public abstract boolean isShowProgress();

    public abstract void setShowProgress(boolean progress);

}