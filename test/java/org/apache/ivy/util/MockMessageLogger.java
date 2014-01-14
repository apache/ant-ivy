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

import junit.framework.AssertionFailedError;

public class MockMessageLogger extends AbstractMessageLogger {

    private List _endProgress = new ArrayList();

    private List _logs = new ArrayList();

    private List _rawLogs = new ArrayList();

    private int _progressCalls;

    public void doEndProgress(String msg) {
        _endProgress.add(msg);
    }

    public void log(String msg, int level) {
        _logs.add(level + " " + msg);
    }

    public void doProgress() {
        _progressCalls++;
    }

    public void rawlog(String msg, int level) {
        _rawLogs.add(level + " " + msg);
    }

    public List getEndProgress() {
        return _endProgress;
    }

    public List getLogs() {
        return _logs;
    }

    public int getProgressCalls() {
        return _progressCalls;
    }

    public List getRawLogs() {
        return _rawLogs;
    }

    public void clear() {
        _logs.clear();
        _rawLogs.clear();
        _endProgress.clear();
        _progressCalls = 0;
    }

    public void assertLogContains(String message) {
        for (Iterator iter = _logs.iterator(); iter.hasNext();) {
            String log = (String) iter.next();
            if (log.indexOf(message) != -1) {
                return;
            }
        }
        throw new AssertionFailedError("logs do not contain expected message: expected='" + message
                + "' logs='\n" + join(_logs) + "'");
    }

    public void assertLogDoesntContain(String message) {
        for (Iterator iter = _logs.iterator(); iter.hasNext();) {
            String log = (String) iter.next();
            if (log.indexOf(message) != -1) {
                throw new AssertionFailedError("logs contain unexpected message: '" + message
                        + "' logs='\n" + join(_logs) + "'");
            }
        }
    }

    public void assertLogVerboseContains(String message) {
        assertLogContains(Message.MSG_VERBOSE + " " + message);
    }

    public void assertLogInfoContains(String message) {
        assertLogContains(Message.MSG_INFO + " " + message);
    }

    public void assertLogWarningContains(String message) {
        Message.sumupProblems();
        assertLogContains(Message.MSG_WARN + " \t" + message);
    }

    public void assertLogErrorContains(String message) {
        Message.sumupProblems();
        assertLogContains(Message.MSG_ERR + " " + message);
    }

    private String join(List logs) {
        StringBuffer sb = new StringBuffer();
        for (Iterator iter = logs.iterator(); iter.hasNext();) {
            String log = (String) iter.next();
            sb.append(log).append("\n");
        }
        return sb.toString();
    }

}
