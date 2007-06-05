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
package org.apache.ivy.plugins.repository.url;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.util.url.URLHandlerRegistry;
import org.apache.ivy.util.url.URLHandler.URLInfo;

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
            throw new IllegalArgumentException(
                    "bad clone name provided: not suitable for an URLResource: " + cloneName);
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
