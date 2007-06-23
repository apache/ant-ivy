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

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.cache.CacheManager;
import org.apache.ivy.core.event.EventManager;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.circular.CircularDependencyStrategy;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.util.MessageImpl;

/**
 * This class represents an execution context of an Ivy action. It contains several getters to
 * retrieve information, like the used Ivy instance, the cache location...
 * 
 * @see IvyThread
 */
public class IvyContext {

    private static ThreadLocal current = new ThreadLocal();

    private Ivy defaultIvy;

    private WeakReference ivy = new WeakReference(null);

    private File cache;

    private MessageImpl messageImpl;

    private Stack resolver = new Stack(); // Stack(DependencyResolver)

    private Map contextMap = new HashMap();

    private Thread operatingThread;

    public static IvyContext getContext() {
        IvyContext cur = (IvyContext) current.get();
        if (cur == null) {
            cur = new IvyContext();
            current.set(cur);
        }
        return cur;
    }

    /**
     * Changes the context associated with this thread. This is especially useful when launching a
     * new thread, to associate it with the same context as the initial one.
     * 
     * @param context
     *            the new context to use in this thread.
     */
    public static void setContext(IvyContext context) {
        current.set(context);
    }

    /**
     * Returns the current ivy instance. When calling any public ivy method on an ivy instance, a
     * reference to this instance is put in this context, and thus accessible using this method,
     * until no code reference this instance and the garbage collector collects it. Then, or if no
     * ivy method has been called, a default ivy instance is returned by this method, so that it
     * never returns null.
     * 
     * @return the current ivy instance
     */
    public Ivy getIvy() {
        Ivy ivy = (Ivy) this.ivy.get();
        return ivy == null ? getDefaultIvy() : ivy;
    }

    private Ivy getDefaultIvy() {
        if (defaultIvy == null) {
            defaultIvy = Ivy.newInstance();
            try {
                defaultIvy.configureDefault();
            } catch (Exception e) {
                //???
            }
        }
        return defaultIvy;
    }

    public void setIvy(Ivy ivy) {
        this.ivy = new WeakReference(ivy);
        operatingThread = Thread.currentThread();
    }

    public File getCache() {
        return cache == null ? getSettings().getDefaultCache() : cache;
    }

    public void setCache(File cache) {
        this.cache = cache;
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

    /*
     * NB : The messageImpl is only used by Message. It should be better to place it there.
     * Alternatively, the Message itself could be placed here, bu this is has a major impact because
     * Message is used at a lot of place.
     */
    public MessageImpl getMessageImpl() {
        return messageImpl;
    }

    public void setMessageImpl(MessageImpl impl) {
        messageImpl = impl;
    }

    public EventManager getEventManager() {
        return getIvy().getEventManager();
    }

    public CacheManager getCacheManager() {
        return CacheManager.getInstance(getSettings(), getCache());
    }

    public void checkInterrupted() {
        getIvy().checkInterrupted();
    }

    public DependencyResolver getResolver() {
        return (DependencyResolver) resolver.peek();
    }

    public void pushResolver(DependencyResolver resolver) {
        this.resolver.push(resolver);
    }

    public void popResolver() {
        resolver.pop();
    }

    // should be better to use context to store this kind of information, but not yet ready to do
    // so...
    // private WeakReference _root = new WeakReference(null);
    // private String _rootModuleConf = null;
    // public IvyNode getRoot() {
    // return (IvyNode) _root.get();
    // }
    //
    // public void setRoot(IvyNode root) {
    // _root = new WeakReference(root);
    // }
    //
    // public String getRootModuleConf() {
    // return _rootModuleConf;
    // }
    //
    // public void setRootModuleConf(String rootModuleConf) {
    // _rootModuleConf = rootModuleConf;
    // }

}
