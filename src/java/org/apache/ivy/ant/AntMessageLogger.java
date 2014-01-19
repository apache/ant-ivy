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
package org.apache.ivy.ant;

import org.apache.ivy.Ivy;
import org.apache.ivy.util.AbstractMessageLogger;
import org.apache.ivy.util.Checks;
import org.apache.ivy.util.MessageLogger;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.Task;

/**
 * Implementation of the simple message facility for ant.
 */
public class AntMessageLogger extends AbstractMessageLogger {
    private static final int PROGRESS_LOG_PERIOD = 1500;

    /**
     * Creates and register an {@link AntMessageLogger} for the given {@link Task}, with the given
     * {@link Ivy} instance.
     * <p>
     * The created instance will automatically be unregistered from the Ivy instance when the task
     * finishes.
     * </p>
     * 
     * @param task
     *            the task the logger should use for logging
     * @param ivy
     *            the ivy instance on which the logger should be registered
     */
    public static void register(ProjectComponent task, final Ivy ivy) {
        MessageLogger current = ivy.getLoggerEngine().peekLogger();
        if (current instanceof AntMessageLogger && task instanceof Task
                && ((AntMessageLogger) current).task instanceof Task) {
            Task currentTask = (Task) ((AntMessageLogger) current).task;

            if ((currentTask.getTaskName() != null)
                    && currentTask.getTaskName().equals(((Task) task).getTaskName())) {
                // The current AntMessageLogger already logs with the same
                // prefix as the given task. So we shouldn't do anything...
                return;
            }
        }

        AntMessageLogger logger = new AntMessageLogger(task);
        ivy.getLoggerEngine().pushLogger(logger);
        task.getProject().addBuildListener(new BuildListener() {
            private int stackDepth = 0;

            public void buildFinished(BuildEvent event) {
            }

            public void buildStarted(BuildEvent event) {
            }

            public void targetStarted(BuildEvent event) {
            }

            public void targetFinished(BuildEvent event) {
            }

            public void taskStarted(BuildEvent event) {
                stackDepth++;
            }

            public void taskFinished(BuildEvent event) {
                // NB: There is somtimes task created by an other task
                // in that case, we should not uninit Message. The log should stay associated
                // with the initial task, except if it was an antcall, ant or subant target
                // NB2 : Testing the identity of the task is not enought, event.getTask() return
                // an instance of UnknownElement is wrapping the concrete instance
                stackDepth--;
                if (stackDepth == -1) {
                    ivy.getLoggerEngine().popLogger();
                    event.getProject().removeBuildListener(this);
                }
            }

            public void messageLogged(BuildEvent event) {
            }
        });

    }

    private ProjectComponent task;

    private long lastProgressFlush = 0;

    private StringBuffer buf = new StringBuffer();

    /**
     * Constructs a new AntMEssageImpl instance.
     * 
     * @param antProjectComponent
     *            the ant project component this message implementation should use for logging. Must
     *            not be <code>null</code>.
     */
    protected AntMessageLogger(ProjectComponent task) {
        Checks.checkNotNull(task, "task");
        this.task = task;
    }

    public void log(String msg, int level) {
        task.log(msg, level);
    }

    public void rawlog(String msg, int level) {
        task.getProject().log(msg, level);
    }

    public void doProgress() {
        buf.append(".");
        if (lastProgressFlush == 0) {
            lastProgressFlush = System.currentTimeMillis();
        }
        // log with ant causes a new line -> we do it only once in a while
        if (System.currentTimeMillis() - lastProgressFlush > PROGRESS_LOG_PERIOD) {
            task.log(buf.toString());
            buf.setLength(0);
            lastProgressFlush = System.currentTimeMillis();
        }
    }

    public void doEndProgress(String msg) {
        task.log(buf + msg);
        buf.setLength(0);
        lastProgressFlush = 0;
    }

    public String toString() {
        return "AntMessageLogger:" + task;
    }
}
