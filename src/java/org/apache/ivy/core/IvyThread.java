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
package org.apache.ivy.core;

/**
 * A simple thread subclass associated the same IvyContext as the thread in which it is
 * instanciated. If you override the run target, then you will have to call initContext() to do the
 * association with the original IvyContext.
 * 
 * @see IvyContext
 */
public class IvyThread extends Thread {
    private IvyContext context = IvyContext.getContext();

    public IvyThread() {
        super();
    }

    public IvyThread(Runnable target, String name) {
        super(target, name);
    }

    public IvyThread(Runnable target) {
        super(target);
    }

    public IvyThread(String name) {
        super(name);
    }

    public IvyThread(ThreadGroup group, Runnable target, String name, long stackSize) {
        super(group, target, name, stackSize);
    }

    public IvyThread(ThreadGroup group, Runnable target, String name) {
        super(group, target, name);
    }

    public IvyThread(ThreadGroup group, Runnable target) {
        super(group, target);
    }

    public IvyThread(ThreadGroup group, String name) {
        super(group, name);
    }

    public void run() {
        initContext();
        super.run();
    }

    protected void initContext() {
        IvyContext.pushContext(context);
    }
}
