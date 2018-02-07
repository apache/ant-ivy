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
public class URLHandlerDispatcher implements URLHandler {
    private final Map<String, URLHandler> handlers = new HashMap<>();

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

    @Override
    public boolean isReachable(final URL url, final TimeoutConstraint timeoutConstraint) {
        return this.getHandler(url.getProtocol()).isReachable(url, timeoutConstraint);
    }

    @Override
    public long getContentLength(final URL url) {
        return this.getContentLength(url, null);
    }

    @Override
    public long getContentLength(final URL url, final int timeout) {
        return this.getContentLength(url, createTimeoutConstraints(timeout));
    }

    @Override
    public long getContentLength(final URL url, final TimeoutConstraint timeoutConstraint) {
        return this.getHandler(url.getProtocol()).getContentLength(url, timeoutConstraint);
    }

    @Override
    public long getLastModified(final URL url) {
        return this.getLastModified(url, null);
    }

    @Override
    public long getLastModified(final URL url, final int timeout) {
        return this.getLastModified(url, createTimeoutConstraints(timeout));
    }

    @Override
    public long getLastModified(final URL url, final TimeoutConstraint timeoutConstraint) {
        return this.getHandler(url.getProtocol()).getLastModified(url, timeoutConstraint);
    }

    @Override
    public URLInfo getURLInfo(final URL url) {
        return this.getURLInfo(url, null);
    }

    @Override
    public URLInfo getURLInfo(final URL url, final int timeout) {
        return this.getURLInfo(url, createTimeoutConstraints(timeout));
    }

    @Override
    public URLInfo getURLInfo(final URL url, final TimeoutConstraint timeoutConstraint) {
        return this.getHandler(url.getProtocol()).getURLInfo(url, timeoutConstraint);
    }

    @Override
    public InputStream openStream(final URL url) throws IOException {
        return this.openStream(url, null);
    }

    @Override
    public InputStream openStream(final URL url, final TimeoutConstraint timeoutConstraint) throws IOException {
        return this.getHandler(url.getProtocol()).openStream(url, timeoutConstraint);
    }

    @Override
    public void download(final URL src, final File dest, final CopyProgressListener l) throws IOException {
        this.download(src, dest, l, null);
    }

    @Override
    public void download(final URL src, final File dest, final CopyProgressListener listener, final TimeoutConstraint timeoutConstraint) throws IOException {
        this.getHandler(src.getProtocol()).download(src, dest, listener, timeoutConstraint);
    }

    @Override
    public void upload(final File src, final URL dest, final CopyProgressListener l) throws IOException {
        this.upload(src, dest, l, null);
    }

    @Override
    public void upload(final File src, final URL dest, final CopyProgressListener listener, final TimeoutConstraint timeoutConstraint) throws IOException {
        this.getHandler(dest.getProtocol()).upload(src, dest, listener, timeoutConstraint);
    }

    public void setRequestMethod(int requestMethod) {
        defaultHandler.setRequestMethod(requestMethod);
        for (URLHandler handler : handlers.values()) {
            handler.setRequestMethod(requestMethod);
        }
    }

    public void setDownloader(String protocol, URLHandler downloader) {
        handlers.put(protocol, downloader);
    }

    public URLHandler getHandler(String protocol) {
        URLHandler downloader = handlers.get(protocol);
        return downloader == null ? defaultHandler : downloader;
    }

    public URLHandler getDefault() {
        return defaultHandler;
    }

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
