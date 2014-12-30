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

/**
 * An abstract base class to ease {@link MessageLogger} implementation.
 */
public abstract class AbstractMessageLogger implements MessageLogger {
    private List<String> problems = new ArrayList<String>();

    private List<String> warns = new ArrayList<String>();

    private List<String> errors = new ArrayList<String>();

    private boolean showProgress = true;

    public void debug(String msg) {
        log(msg, Message.MSG_DEBUG);
    }

    public void verbose(String msg) {
        log(msg, Message.MSG_VERBOSE);
    }

    public void deprecated(String msg) {
        log("DEPRECATED: " + msg, Message.MSG_WARN);
    }

    public void info(String msg) {
        log(msg, Message.MSG_INFO);
    }

    public void rawinfo(String msg) {
        rawlog(msg, Message.MSG_INFO);
    }

    public void warn(String msg) {
        log("WARN: " + msg, Message.MSG_VERBOSE);
        problems.add("WARN:  " + msg);
        getWarns().add(msg);
    }

    public void error(String msg) {
        // log in verbose mode because message is appended as a problem, and will be
        // logged at the end at error level
        log("ERROR: " + msg, Message.MSG_VERBOSE);
        problems.add("\tERROR: " + msg);
        getErrors().add(msg);
    }

    public List<String> getProblems() {
        return problems;
    }

    public void sumupProblems() {
        MessageLoggerHelper.sumupProblems(this);
        clearProblems();
    }

    public void clearProblems() {
        problems.clear();
        warns.clear();
        errors.clear();
    }

    public List<String> getErrors() {
        return errors;
    }

    public List<String> getWarns() {
        return warns;
    }

    public void progress() {
        if (showProgress) {
            doProgress();
        }
    }

    public void endProgress() {
        endProgress("");
    }

    public void endProgress(String msg) {
        if (showProgress) {
            doEndProgress(msg);
        }
    }

    public boolean isShowProgress() {
        return showProgress;
    }

    public void setShowProgress(boolean progress) {
        showProgress = progress;
    }

    /**
     * Indicates a progression for a long running task
     */
    protected abstract void doProgress();

    /**
     * Indicates the end of a long running task
     * 
     * @param msg
     *            the message associated with long running task end.
     */
    protected abstract void doEndProgress(String msg);

}
