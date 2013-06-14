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
package org.apache.ivy.ant.testutil;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;

/**
 * Our own personal build listener.
 */
public class AntTestListener implements BuildListener {
    private int logLevel;

    private StringBuffer buildLog = new StringBuffer();

    /**
     * Constructs a test listener which will ignore log events above the given level.
     */
    public AntTestListener(int logLevel) {
        this.logLevel = logLevel;
    }

    /**
     * Fired before any targets are started.
     */
    public void buildStarted(BuildEvent event) {
    }

    /**
     * Fired after the last target has finished. This event will still be thrown if an error
     * occurred during the build.
     * 
     * @see BuildEvent#getException()
     */
    public void buildFinished(BuildEvent event) {
    }

    /**
     * Fired when a target is started.
     * 
     * @see BuildEvent#getTarget()
     */
    public void targetStarted(BuildEvent event) {
        // System.out.println("targetStarted " + event.getTarget().getName());
    }

    /**
     * Fired when a target has finished. This event will still be thrown if an error occurred during
     * the build.
     * 
     * @see BuildEvent#getException()
     */
    public void targetFinished(BuildEvent event) {
        // System.out.println("targetFinished " + event.getTarget().getName());
    }

    /**
     * Fired when a task is started.
     * 
     * @see BuildEvent#getTask()
     */
    public void taskStarted(BuildEvent event) {
        // System.out.println("taskStarted " + event.getTask().getTaskName());
    }

    /**
     * Fired when a task has finished. This event will still be throw if an error occurred during
     * the build.
     * 
     * @see BuildEvent#getException()
     */
    public void taskFinished(BuildEvent event) {
        // System.out.println("taskFinished " + event.getTask().getTaskName());
    }

    /**
     * Fired whenever a message is logged.
     * 
     * @see BuildEvent#getMessage()
     * @see BuildEvent#getPriority()
     */
    public void messageLogged(BuildEvent event) {
        if (event.getPriority() > logLevel) {
            // ignore event
            return;
        }

        buildLog.append(event.getMessage());
    }

    public String getLog() {
        return buildLog.toString();
    }

}
