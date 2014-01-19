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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.ivy.plugins.repository.LocalizableResource;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.util.url.URLHandler.URLInfo;
import org.apache.ivy.util.url.URLHandlerRegistry;

public class URLResource implements LocalizableResource {
    private URL url;

    private boolean init = false;

    private long lastModified;

    private long contentLength;

    private boolean exists;

    public URLResource(URL url) {
        this.url = url;
    }

    public String getName() {
        return url.toExternalForm();
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
        if (!init) {
            init();
        }
        return lastModified;
    }

    private void init() {
        URLInfo info = URLHandlerRegistry.getDefault().getURLInfo(url);
        contentLength = info.getContentLength();
        lastModified = info.getLastModified();
        exists = info.isReachable();
        init = true;
    }

    public long getContentLength() {
        if (!init) {
            init();
        }
        return contentLength;
    }

    public boolean exists() {
        if (!init) {
            init();
        }
        return exists;
    }

    public URL getURL() {
        return url;
    }

    public String toString() {
        return getName();
    }

    public boolean isLocal() {
        return url.getProtocol().equals("file");
    }

    public InputStream openStream() throws IOException {
        return URLHandlerRegistry.getDefault().openStream(url);
    }

    public File getFile() {
        if (!isLocal()) {
            throw new IllegalStateException("Cannot get the local file for the not local resource "
                    + url);
        }
        try {
            return new File(url.toURI());
        } catch (URISyntaxException e) {
            return new File(url.getPath());
        }
    }
}
