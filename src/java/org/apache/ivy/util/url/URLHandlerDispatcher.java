/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivy.util.url;

import org.apache.ivy.core.settings.TimeoutConstraint;
import org.apache.ivy.util.CopyProgressListener;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * An implementation of {@link URLHandler} which uses an underlying {@link URLHandler} per protocol
 * and a fallback default {@link URLHandler} for dealing with downloads, uploads and
 * general reachability checks
 */
public class URLHandlerDispatcher implements TimeoutConstrainedURLHandler {
    @SuppressWarnings("deprecation")
    private final Map<String, URLHandler> handlers = new HashMap<>();

    @SuppressWarnings("deprecation")
    private URLHandler defaultHandler = new BasicURLHandler();

    public URLHandlerDispatcher() {
    }

    @Override
    public boolean isReachable(final URL url) {
        return this.isReachable(url, null);
    }

    @Override
    public boolean isReachable(final URL url, final int timeout) {
        return this.isReachable(url, createTimeoutConstraints(timeout));
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean isReachable(final URL url, final TimeoutConstraint timeoutConstraint) {
        final URLHandler handler = this.getHandler(url.getProtocol());
        if (handler instanceof TimeoutConstrainedURLHandler) {
            return ((TimeoutConstrainedURLHandler) handler).isReachable(url, timeoutConstraint);
        }
        return handler.isReachable(url, timeoutConstraint != null ? timeoutConstraint.getConnectionTimeout() : 0);
    }

    @Override
    public long getContentLength(final URL url) {
        return this.getContentLength(url, null);
    }

    @Override
    public long getContentLength(final URL url, final int timeout) {
        return this.getContentLength(url, createTimeoutConstraints(timeout));
    }

    @SuppressWarnings("deprecation")
    @Override
    public long getContentLength(final URL url, final TimeoutConstraint timeoutConstraint) {
        final URLHandler handler = this.getHandler(url.getProtocol());
        if (handler instanceof TimeoutConstrainedURLHandler) {
            return ((TimeoutConstrainedURLHandler) handler).getContentLength(url, timeoutConstraint);
        }
        return handler.getContentLength(url, timeoutConstraint != null ? timeoutConstraint.getConnectionTimeout() : 0);
    }

    @Override
    public long getLastModified(final URL url) {
        return this.getLastModified(url, null);
    }

    @Override
    public long getLastModified(final URL url, final int timeout) {
        return this.getLastModified(url, createTimeoutConstraints(timeout));
    }

    @SuppressWarnings("deprecation")
    @Override
    public long getLastModified(final URL url, final TimeoutConstraint timeoutConstraint) {
        final URLHandler handler = this.getHandler(url.getProtocol());
        if (handler instanceof TimeoutConstrainedURLHandler) {
            return ((TimeoutConstrainedURLHandler) handler).getLastModified(url, timeoutConstraint);
        }
        return handler.getLastModified(url, timeoutConstraint != null ? timeoutConstraint.getConnectionTimeout() : 0);
    }

    @SuppressWarnings("deprecation")
    @Override
    public URLInfo getURLInfo(final URL url) {
        return this.getURLInfo(url, null);
    }

    @SuppressWarnings("deprecation")
    @Override
    public URLInfo getURLInfo(final URL url, final int timeout) {
        return this.getURLInfo(url, createTimeoutConstraints(timeout));
    }

    @SuppressWarnings("deprecation")
    @Override
    public URLInfo getURLInfo(final URL url, final TimeoutConstraint timeoutConstraint) {
        final URLHandler handler = this.getHandler(url.getProtocol());
        if (handler instanceof TimeoutConstrainedURLHandler) {
            return ((TimeoutConstrainedURLHandler) handler).getURLInfo(url, timeoutConstraint);
        }
        return handler.getURLInfo(url, timeoutConstraint != null ? timeoutConstraint.getConnectionTimeout() : 0);
    }

    @Override
    public InputStream openStream(final URL url) throws IOException {
        return this.openStream(url, null);
    }

    @SuppressWarnings("deprecation")
    @Override
    public InputStream openStream(final URL url, final TimeoutConstraint timeoutConstraint) throws IOException {
        final URLHandler handler = this.getHandler(url.getProtocol());
        if (handler instanceof TimeoutConstrainedURLHandler) {
            return ((TimeoutConstrainedURLHandler) handler).openStream(url, timeoutConstraint);
        }
        return handler.openStream(url);
    }

    @Override
    public void download(final URL src, final File dest, final CopyProgressListener l) throws IOException {
        this.download(src, dest, l, null);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void download(final URL src, final File dest, final CopyProgressListener listener, final TimeoutConstraint timeoutConstraint) throws IOException {
        final URLHandler handler = this.getHandler(src.getProtocol());
        if (handler instanceof TimeoutConstrainedURLHandler) {
            ((TimeoutConstrainedURLHandler) handler).download(src, dest, listener, timeoutConstraint);
            return;
        }
        handler.download(src, dest, listener);
    }

    @Override
    public void upload(final File src, final URL dest, final CopyProgressListener l) throws IOException {
        this.upload(src, dest, l, null);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void upload(final File src, final URL dest, final CopyProgressListener listener, final TimeoutConstraint timeoutConstraint) throws IOException {
        final URLHandler handler = this.getHandler(dest.getProtocol());
        if (handler instanceof TimeoutConstrainedURLHandler) {
            ((TimeoutConstrainedURLHandler) handler).upload(src, dest, listener, timeoutConstraint);
            return;
        }
        handler.upload(src, dest, listener);
    }

    @SuppressWarnings("deprecation")
    public void setRequestMethod(int requestMethod) {
        defaultHandler.setRequestMethod(requestMethod);
        for (URLHandler handler : handlers.values()) {
            handler.setRequestMethod(requestMethod);
        }
    }

    @SuppressWarnings("deprecation")
    public void setDownloader(String protocol, URLHandler downloader) {
        handlers.put(protocol, downloader);
    }

    @SuppressWarnings("deprecation")
    public URLHandler getHandler(String protocol) {
        URLHandler downloader = handlers.get(protocol);
        return downloader == null ? defaultHandler : downloader;
    }

    @SuppressWarnings("deprecation")
    public URLHandler getDefault() {
        return defaultHandler;
    }

    @SuppressWarnings("deprecation")
    public void setDefault(URLHandler default1) {
        defaultHandler = default1;
    }

    private static TimeoutConstraint createTimeoutConstraints(final int connectionTimeout) {
        return new TimeoutConstraint() {
            @Override
            public int getConnectionTimeout() {
                return connectionTimeout;
            }

            @Override
            public int getReadTimeout() {
                return -1;
            }

        };
    }
}
