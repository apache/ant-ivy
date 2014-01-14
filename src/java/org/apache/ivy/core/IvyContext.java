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

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.event.EventManager;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.circular.CircularDependencyStrategy;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.MessageLogger;

/**
 * This class represents an execution context of an Ivy action. It contains several getters to
 * retrieve information, like the used Ivy instance, the cache location...
 * 
 * @see IvyThread
 */
public class IvyContext {

    private static ThreadLocal/* <Stack<IvyContext>> */current = new ThreadLocal();

    private Ivy defaultIvy;

    private WeakReference/* <Ivy> */ivy = new WeakReference(null);

    private Map contextMap = new HashMap();

    private Thread operatingThread;

    private ResolveData resolveData;

    private DependencyDescriptor dd;

    public IvyContext() {
    }

    public IvyContext(IvyContext ctx) {
        defaultIvy = ctx.defaultIvy;
        ivy = ctx.ivy;
        contextMap = new HashMap(ctx.contextMap);
        operatingThread = ctx.operatingThread;
        resolveData = ctx.resolveData;
        dd = ctx.dd;
    }

    public static IvyContext getContext() {
        Stack cur = getCurrentStack();
        if (cur.isEmpty()) {
            cur.push(new IvyContext());
        }
        return (IvyContext) cur.peek();
    }

    private static Stack/* <IvyContext> */getCurrentStack() {
        Stack cur = (Stack) current.get();
        if (cur == null) {
            cur = new Stack();
            current.set(cur);
        }
        return cur;
    }

    /**
     * Creates a new IvyContext and pushes it as the current context in the current thread.
     * <p>
     * {@link #popContext()} should usually be called when the job for which this context has been
     * pushed is finished.
     * </p>
     * 
     * @return the newly pushed context
     */
    public static IvyContext pushNewContext() {
        return pushContext(new IvyContext());
    }

    /**
     * Creates a new IvyContext as a copy of the current one and pushes it as the current context in
     * the current thread.
     * <p>
     * {@link #popContext()} should usually be called when the job for which this context has been
     * pushed is finished.
     * </p>
     * 
     * @return the newly pushed context
     */
    public static IvyContext pushNewCopyContext() {
        return pushContext(new IvyContext(getContext()));
    }

    /**
     * Changes the context associated with this thread. This is especially useful when launching a
     * new thread, to associate it with the same context as the initial one. Do not forget to call
     * {@link #popContext()} when done.
     * 
     * @param context
     *            the new context to use in this thread.
     * @return the pushed context
     */
    public static IvyContext pushContext(IvyContext context) {
        getCurrentStack().push(context);
        return context;
    }

    /**
     * Pops one context used with this thread. This is usually called after having finished a task
     * for which a call to {@link #pushNewContext()} or {@link #pushContext(IvyContext)} was done
     * prior to beginning the task.
     * 
     * @return the popped context
     */
    public static IvyContext popContext() {
        return (IvyContext) getCurrentStack().pop();
    }

    /**
     * Reads the first object from the list saved under given key in the first context from the
     * context stack in which this key is defined. If value under key in any of the contexts form
     * the stack represents non List object then a RuntimeException is thrown.
     * <p>
     * This methods does a similar job to {@link #peek(String)}, except that it considers the whole
     * context stack and not only one instance.
     * </p>
     * 
     * @param key
     *            context key for the string
     * @return top object from the list (index 0) of the first context in the stack containing this
     *         key or null if no key or list empty in all contexts from the context stack
     * @see #peek(String)
     */
    public static Object peekInContextStack(String key) {
        Object value = null;
        Stack contextStack = getCurrentStack();
        for (int i = contextStack.size() - 1; i >= 0 && value == null; i--) {
            IvyContext ctx = (IvyContext) contextStack.get(i);
            value = ctx.peek(key);
        }
        return value;
    }

    /**
     * Returns the current ivy instance.
     * <p>
     * When calling any public ivy method on an ivy instance, a reference to this instance is put in
     * this context, and thus accessible using this method, until no code reference this instance
     * and the garbage collector collects it.
     * </p>
     * <p>
     * Then, or if no ivy method has been called, a default ivy instance is returned by this method,
     * so that it never returns <code>null</code>.
     * </p>
     * 
     * @return the current ivy instance
     */
    public Ivy getIvy() {
        Ivy ivy = peekIvy();
        return ivy == null ? getDefaultIvy() : ivy;
    }

    /**
     * Returns the Ivy instance associated with this context, or <code>null</code> if no such
     * instance is currently associated with this context.
     * <p>
     * If you want get a default Ivy instance in case no instance if currently associated, use
     * {@link #getIvy()}.
     * </p>
     * 
     * @return the current ivy instance, or <code>null</code> if there is no current ivy instance.
     */
    public Ivy peekIvy() {
        Ivy ivy = (Ivy) this.ivy.get();
        return ivy;
    }

    private Ivy getDefaultIvy() {
        if (defaultIvy == null) {
            defaultIvy = Ivy.newInstance();
            try {
                defaultIvy.configureDefault();
            } catch (Exception e) {
                Message.debug(e);
                // ???
            }
        }
        return defaultIvy;
    }

    public void setIvy(Ivy ivy) {
        this.ivy = new WeakReference(ivy);
        operatingThread = Thread.currentThread();
    }

    public IvySettings getSettings() {
        return getIvy().getSettings();
    }

    public CircularDependencyStrategy getCircularDependencyStrategy() {
        return getSettings().getCircularDependencyStrategy();
    }

    public Object get(String key) {
        WeakReference ref = (WeakReference) contextMap.get(key);
        return ref == null ? null : ref.get();
    }

    public void set(String key, Object value) {
        contextMap.put(key, new WeakReference(value));
    }

    /**
     * Reads the first object from the list saved under given key in the context. If value under key
     * represents non List object then a RuntimeException is thrown.
     * 
     * @param key
     *            context key for the string
     * @return top object from the list (index 0) or null if no key or list empty
     */
    public Object peek(String key) {
        synchronized (contextMap) {
            Object o = contextMap.get(key);
            if (o == null) {
                return null;
            }
            if (o instanceof List) {
                if (((List) o).size() == 0) {
                    return null;
                }
                Object ret = ((List) o).get(0);
                return ret;
            } else {
                throw new RuntimeException("Cannot top from non List object " + o);
            }
        }
    }

    /**
     * Removes and returns first object from the list saved under given key in the context. If value
     * under key represents non List object then a RuntimeException is thrown.
     * 
     * @param key
     *            context key for the string
     * @return top object from the list (index 0) or null if no key or list empty
     */
    public Object pop(String key) {
        synchronized (contextMap) {
            Object o = contextMap.get(key);
            if (o == null) {
                return null;
            }
            if (o instanceof List) {
                if (((List) o).size() == 0) {
                    return null;
                }
                Object ret = ((List) o).remove(0);
                return ret;
            } else {
                throw new RuntimeException("Cannot pop from non List object " + o);
            }
        }
    }

    /**
     * Removes and returns first object from the list saved under given key in the context but only
     * if it equals the given expectedValue - if not a false value is returned. If value under key
     * represents non List object then a RuntimeException is thrown.
     * 
     * @param key
     *            context key for the string
     * @return true if the r
     */
    public boolean pop(String key, Object expectedValue) {
        synchronized (contextMap) {
            Object o = contextMap.get(key);
            if (o == null) {
                return false;
            }
            if (o instanceof List) {
                if (((List) o).size() == 0) {
                    return false;
                }
                Object top = ((List) o).get(0);
                if (!top.equals(expectedValue)) {
                    return false;
                }
                ((List) o).remove(0);
                return true;
            } else {
                throw new RuntimeException("Cannot pop from non List object " + o);
            }
        }
    }

    /**
     * Puts a new object at the start of the list saved under given key in the context. If value
     * under key represents non List object then a RuntimeException is thrown. If no list exists
     * under given key a new LinkedList is created. This is kept without WeakReference in opposite
     * to the put() results.
     * 
     * @param key
     *            key context key for the string
     * @param value
     *            value to be saved under the key
     */
    public void push(String key, Object value) {
        synchronized (contextMap) {
            if (!contextMap.containsKey(key)) {
                contextMap.put(key, new LinkedList());
            }
            Object o = contextMap.get(key);
            if (o instanceof List) {
                ((List) o).add(0, value);
            } else {
                throw new RuntimeException("Cannot push to non List object " + o);
            }
        }
    }

    public Thread getOperatingThread() {
        return operatingThread;
    }

    public MessageLogger getMessageLogger() {
        // calling getIvy() instead of peekIvy() is not possible here: it will initialize a default
        // Ivy instance, with default settings, but settings themselves may log messages and lead to
        // a call to this method. So we use the current Ivy instance if any, or the default Ivy
        // instance, or the default MessageLogger.
        Ivy ivy = peekIvy();
        if (ivy == null) {
            if (defaultIvy == null) {
                return Message.getDefaultLogger();
            } else {
                return defaultIvy.getLoggerEngine();
            }
        } else {
            return ivy.getLoggerEngine();
        }
    }

    public EventManager getEventManager() {
        return getIvy().getEventManager();
    }

    public void checkInterrupted() {
        getIvy().checkInterrupted();
    }

    public void setResolveData(ResolveData data) {
        this.resolveData = data;
    }

    public ResolveData getResolveData() {
        return resolveData;
    }

    public void setDependencyDescriptor(DependencyDescriptor dd) {
        this.dd = dd;
    }

    public DependencyDescriptor getDependencyDescriptor() {
        return dd;
    }

}
