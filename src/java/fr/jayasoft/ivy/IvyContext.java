/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import fr.jayasoft.ivy.circular.CircularDependencyStrategy;
import fr.jayasoft.ivy.util.IvyThread;
import fr.jayasoft.ivy.util.MessageImpl;


/**
 * This class represents an execution context of an Ivy action.
 * It contains several getters to retrieve information, like the used Ivy instance, the
 * cache location... 
 * 
 * @see IvyThread
 * 
 * @author Xavier Hanin
 * @author Maarten Coene
 */
public class IvyContext {

    private static ThreadLocal _current = new ThreadLocal();
    
    private Ivy _defaultIvy;
    private WeakReference _ivy = new WeakReference(null); 
    private File _cache;
    private MessageImpl _messageImpl;
    
    private Map _contextMap = new HashMap();

	private Thread _operatingThread;

    
    public static IvyContext getContext() {
    	IvyContext cur = (IvyContext)_current.get();
        if (cur == null) {
            cur = new IvyContext();
            _current.set(cur);
        }
        return cur;
    }
    
    /**
     * Changes the context associated with this thread.
     * This is especially useful when launching a new thread, to associate it with the same context as the initial one.
     * 
     * @param context the new context to use in this thread.
     */
    public static void setContext(IvyContext context) {
    	_current.set(context);
    }
    
    /**
     * Returns the current ivy instance.
     * When calling any public ivy method on an ivy instance, a reference to this instance is 
     * put in this context, and thus accessible using this method, until no code reference
     * this instance and the garbage collector collects it.
     * Then, or if no ivy method has been called, a default ivy instance is returned
     * by this method, so that it never returns null. 
     * @return the current ivy instance
     */
    public Ivy getIvy() {
    	Ivy ivy = (Ivy)_ivy.get();
        return ivy == null ? getDefaultIvy() : ivy;
    }

    private Ivy getDefaultIvy() {
        if (_defaultIvy == null) {
            _defaultIvy = new Ivy();
            try {
                getDefaultIvy().configureDefault();
            } catch (Exception e) {
            }            
        }
        return _defaultIvy;
    }
    void setIvy(Ivy ivy) {
    	_ivy = new WeakReference(ivy);
    	_operatingThread = Thread.currentThread();
    }
    public File getCache() {
    	return _cache == null ? getIvy().getDefaultCache() : _cache;
    }
    void setCache(File cache) {
    	_cache = cache;
    }

	public CircularDependencyStrategy getCircularDependencyStrategy() {
		return getIvy().getCircularDependencyStrategy();
	}

	public Object get(String key) {
		WeakReference ref = (WeakReference) _contextMap.get(key);
		return ref == null ? null : ref.get();
	}

	public void set(String key, Object value) {
		_contextMap.put(key, new WeakReference(value));
	}

	public Thread getOperatingThread() {
		return _operatingThread;
	}

	
	/* NB : The messageImpl is only used by Message.  It should be better to place it there.
	 * Alternatively, the Message itself could be placed here, bu this is has a major impact
	 * because Message is used at a lot of place.
	 */ 
	public MessageImpl getMessageImpl() {
		return _messageImpl;
	}
	
	public void setMessageImpl(MessageImpl impl) {
		_messageImpl = impl;
	}
	
	// should be better to use context to store this kind of information, but not yet ready to do so...
//    private WeakReference _root = new WeakReference(null); 
//    private String _rootModuleConf = null;
//	public IvyNode getRoot() {
//		return (IvyNode) _root.get();
//	}
//	
//	public void setRoot(IvyNode root) {
//		_root = new WeakReference(root);
//	}
//
//	public String getRootModuleConf() {
//		return _rootModuleConf;
//	}
//
//	public void setRootModuleConf(String rootModuleConf) {
//		_rootModuleConf = rootModuleConf;
//	}
	
	
}
