/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.url;

import java.net.URL;

public abstract class AbstractURLHandler implements URLHandler {
    public boolean isReachable(URL url) {
        return getURLInfo(url).isReachable();
    }
    public boolean isReachable(URL url, int timeout) {
        return getURLInfo(url, timeout).isReachable();
    }
    public long getContentLength(URL url) {
        return getURLInfo(url).getContentLength();
    }
    public long getContentLength(URL url, int timeout) {
        return getURLInfo(url, timeout).getContentLength();        
    }
    
    public long getLastModified(URL url) {
        return getURLInfo(url).getLastModified();
    }
    public long getLastModified(URL url, int timeout) {
        return getURLInfo(url, timeout).getLastModified();        
    }
}
