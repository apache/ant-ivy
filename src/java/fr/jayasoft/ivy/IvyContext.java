/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy;

import java.io.File;


/**
 * This class represents an execution context of an Ivy action.
 * It contains several getters to retrieve information, like the used Ivy instance, the
 * cache location... 
 * 
 * @author x.hanin
 * @author Maarten Coene
 */
public class IvyContext {

    private static ThreadLocal _current = new ThreadLocal();
    
    private Ivy _ivy;
    private File _cache;
    
    public static IvyContext getContext() {
    	IvyContext cur = (IvyContext)_current.get();
        if (cur == null) {
            cur = new IvyContext();
            _current.set(cur);
        }
        return cur;
    }
    
    public Ivy getIvy() {
    	return _ivy;
    }
    void setIvy(Ivy ivy) {
    	_ivy = ivy;
    }
    public File getCache() {
    	return _cache;
    }
    void setCache(File cache) {
    	_cache = cache;
    }
}
