/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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

public class MockMessageLogger extends AbstractMessageLogger {

    private final List<String> endProgress = new ArrayList<>();

    private final List<String> logs = new ArrayList<>();

    private final List<String> rawLogs = new ArrayList<>();

    private int progressCalls;

    public void doEndProgress(String msg) {
        endProgress.add(msg);
    }

    public void log(String msg, int level) {
        logs.add(level + " " + msg);
    }

    public void doProgress() {
        progressCalls++;
    }

    public void rawlog(String msg, int level) {
        rawLogs.add(level + " " + msg);
    }

    public List<String> getEndProgress() {
        return endProgress;
    }

    public List<String> getLogs() {
        return logs;
    }

    public int getProgressCalls() {
        return progressCalls;
    }

    public List<String> getRawLogs() {
        return rawLogs;
    }

    public void clear() {
        super.clearProblems();
        logs.clear();
        rawLogs.clear();
        endProgress.clear();
        progressCalls = 0;
    }

    public void assertLogContains(String message) {
        for (String log : logs) {
            if (log.contains(message)) {
                return;
            }
        }
        throw new AssertionError("logs do not contain expected message: expected='" + message
                + "' logs='\n" + join(logs) + "'");
    }

    public void assertLogDoesntContain(String message) {
        for (String log : logs) {
            if (log.contains(message)) {
                throw new AssertionError("logs contain unexpected message: '" + message
                        + "' logs='\n" + join(logs) + "'");
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

    private String join(List<String> logs) {
        StringBuilder sb = new StringBuilder();
        for (String log : logs) {
            sb.append(log).append("\n");
        }
        return sb.toString();
    }

}
