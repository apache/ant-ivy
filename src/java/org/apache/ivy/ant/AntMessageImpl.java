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

import org.apache.ivy.util.Message;
import org.apache.ivy.util.MessageImpl;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Task;


/**
 * Implementation of the simple message facility for ant.
 * 
 * @author Xavier Hanin
 */
public class AntMessageImpl implements MessageImpl {
    private Task _task;

    private static long _lastProgressFlush = 0;
    private static StringBuffer _buf = new StringBuffer();

    /**
     * @param task
     */
    public AntMessageImpl(Task task) {
        _task = task;
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
            	//NB: There is somtimes task created by an other task
            	//in that case, we should not uninit Message.  The log should stay associated
            	//with the initial task, except if it was an antcall, ant or subant target
            	//NB2 : Testing the identity of the task is not enought, event.getTask() return 
            	//an instance of UnknownElement is wrapping the concrete instance
            	if (stackDepth==0) {
            		Message.uninit();
            		event.getProject().removeBuildListener(this);
            	}
            	stackDepth--;
            }
            public void messageLogged(BuildEvent event) {
            }
        }); 
    }

    public void log(String msg, int level) {
    	_task.log(msg, level);
    }
    
    public void rawlog(String msg, int level) {
    	_task.getProject().log(msg, level);
    }

    public void progress() {
        _buf.append(".");
        if (_lastProgressFlush == 0) {
            _lastProgressFlush = System.currentTimeMillis();
        }
        if (_task != null) {
            // log with ant causes a new line -> we do it only once in a while
            if (System.currentTimeMillis() - _lastProgressFlush > 1500) {
                _task.log(_buf.toString());
                _buf.setLength(0);
                _lastProgressFlush = System.currentTimeMillis();
            }
        }
    }
    
    public void endProgress(String msg) {
        _task.log(_buf + msg);
        _buf.setLength(0);
        _lastProgressFlush = 0;
    }
}
