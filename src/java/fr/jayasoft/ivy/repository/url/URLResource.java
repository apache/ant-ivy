/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.repository.url;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import fr.jayasoft.ivy.repository.Resource;
import fr.jayasoft.ivy.url.URLHandlerRegistry;
import fr.jayasoft.ivy.util.Message;

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

    public long getLastModified() {
        if (!_init) {
            init();
        }
        return _lastModified;
    }

    private void init() {
        URLConnection con = null;
        try {
            _lastModified = 0;
            _contentLength = 0;
            _exists = URLHandlerRegistry.getDefault().isReachable(_url);
            if (_exists) {
                con = _url.openConnection();
                _lastModified = con.getLastModified();
                _contentLength = con.getContentLength();
            }
        } catch (IOException e) {
            Message.verbose("impossible to open connection to "+_url+":"+e.getMessage());
            _exists = false;
        } finally {
            if (con instanceof HttpURLConnection) {
                ((HttpURLConnection)con).disconnect();
            }
        }
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
}
