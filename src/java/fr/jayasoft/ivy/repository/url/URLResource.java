/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.repository.url;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import fr.jayasoft.ivy.repository.Resource;
import fr.jayasoft.ivy.url.URLHandlerRegistry;
import fr.jayasoft.ivy.url.URLHandler.URLInfo;

public class URLResource implements Resource {
    private URL _url;
    private boolean _init = false;
    private long _lastModified;
    private long _contentLength;
    private boolean _exists;

    public URLResource(URL url) {
        _url = url;
    }

    public String getName() {
        return _url.toExternalForm();
    }
    
    public Resource clone(String cloneName) {
    	try {
			return new URLResource(new URL(cloneName));
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("bad clone name provided: not suitable for an URLResource: "+cloneName);
		}
    }

    public long getLastModified() {
        if (!_init) {
            init();
        }
        return _lastModified;
    }

    private void init() {
        URLInfo info = URLHandlerRegistry.getDefault().getURLInfo(_url);
        _contentLength = info.getContentLength();
        _lastModified = info.getLastModified();
        _exists = info.isReachable();
        _init = true;
    }

    public long getContentLength() {
        if (!_init) {
            init();
        }
        return _contentLength;
    }

    public boolean exists() {
        if (!_init) {
            init();
        }
        return _exists;
    }

    public URL getURL() {
        return _url;
    }
    public String toString() {
        return getName();
    }
    
    public boolean isLocal() {
        return false;
    }

	public InputStream openStream() throws IOException {
		return _url.openStream();
	}
}
