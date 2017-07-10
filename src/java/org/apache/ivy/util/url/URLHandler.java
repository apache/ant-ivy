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

/**
 * This interface is responsible for handling some URL manipulation (stream opening, downloading,
 * check reachability, ...).
 */
public interface URLHandler {

    /**
     * Using the slower REQUEST method for getting the basic URL infos. Use this when getting errors
     * behind a problematic/special proxy or firewall chain.
     */
    int REQUEST_METHOD_GET = 1;

    /**
     * Using the faster HEAD method for getting the basic URL infos. Works for most common networks.
     */
    int REQUEST_METHOD_HEAD = 2;

    class URLInfo {
        private long contentLength;

        private long lastModified;

        private boolean available;

        private String bodyCharset;

        protected URLInfo(boolean available, long contentLength, long lastModified) {
            this(available, contentLength, lastModified, null);
        }

        protected URLInfo(boolean available, long contentLength, long lastModified,
                          String bodyCharset) {
            this.available = available;
            this.contentLength = contentLength;
            this.lastModified = lastModified;
            this.bodyCharset = bodyCharset;
        }

        public boolean isReachable() {
            return available;
        }

        public long getContentLength() {
            return contentLength;
        }

        public long getLastModified() {
            return lastModified;
        }

        public String getBodyCharset() {
            return bodyCharset;
        }
    }

    URLInfo UNAVAILABLE = new URLInfo(false, 0, 0);

    /**
     * Please prefer getURLInfo when several infos are needed.
     *
     * @param url the url to check
     * @return true if the target is reachable
     * @deprecated Use {@link #isReachable(URL, TimeoutConstraint)} instead
     */
    @Deprecated
    boolean isReachable(URL url);

    /**
     * Please prefer getURLInfo when several infos are needed.
     *
     * @param url     the url to check
     * @param timeout the timeout in milliseconds
     * @return true if the target is reachable
     * @deprecated Use {@link #isReachable(URL, TimeoutConstraint)} instead
     */
    @Deprecated
    boolean isReachable(URL url, int timeout);

    /**
     * Returns true if the passed <code>URL</code> is reachable. Else returns false. Uses the
     * passed <code>timeoutConstraint</code> for determining the connectivity to the URL.
     * <p>
     * Please use {@link #getURLInfo(URL, TimeoutConstraint)} if more one information about the <code>url</code>
     * is needed
     *
     * @param url                The URL to access
     * @param timeoutConstraint The connectivity timeout constraints. Can be null, in which case the timeouts
     *                           are implementation specific
     * @return
     * @since 2.5
     */
    boolean isReachable(URL url, TimeoutConstraint timeoutConstraint);

    /**
     * Please prefer getURLInfo when several infos are needed.
     *
     * @param url the url to check
     * @return the length of the target if the given url is reachable, 0 otherwise. No error code in
     * case of http urls.
     * @deprecated Use {@link #getContentLength(URL, TimeoutConstraint)} instead
     */
    @Deprecated
    long getContentLength(URL url);

    /**
     * @param url     the url to check
     * @param timeout the maximum time before considering an url is not reachable a
     *                timeout of zero indicates no timeout
     * @return the length of the target if the given url is reachable, 0 otherwise. No error code in
     * case of http urls.
     * @deprecated Use {@link #getContentLength(URL, TimeoutConstraint)} instead
     */
    @Deprecated
    long getContentLength(URL url, int timeout);

    /**
     * Returns the number of bytes of data that's available for the resource at the passed <code>url</code>.
     * Returns 0 if the passed <code>url</code> isn't reachable
     *
     * @param url                The URL to access
     * @param timeoutConstraint The connectivity timeout constraints. Can be null, in which case the timeouts
     *                           are implementation specific
     * @return
     * @since 2.5
     */
    long getContentLength(URL url, TimeoutConstraint timeoutConstraint);

    /**
     * Please prefer getURLInfo when several infos are needed.
     *
     * @param url the url to check
     * @return last modified timestamp of the given url
     * @deprecated Use {@link #getLastModified(URL, TimeoutConstraint)} instead
     */
    @Deprecated
    long getLastModified(URL url);

    /**
     * Please prefer getURLInfo when several infos are needed.
     *
     * @param url     the url to check
     * @param timeout the timeout in milliseconds
     * @return last modified timestamp of the given url
     * @deprecated Use {@link #getLastModified(URL, TimeoutConstraint)} instead
     */
    @Deprecated
    long getLastModified(URL url, int timeout);

    /**
     * Returns the last modified timestamp of the resource accessible at the passed <code>url</code>.
     * <p>
     * Please use {@link #getURLInfo(URL, TimeoutConstraint)} if more one information about the <code>url</code>
     * is needed
     *
     * @param url                The URL to access
     * @param timeoutConstraint The connectivity timeout constraints. Can be null, in which case the timeouts
     *                           are implementation specific
     * @return
     * @since 2.5
     */
    long getLastModified(URL url, TimeoutConstraint timeoutConstraint);

    /**
     * @param url The url from which information is retrieved.
     * @return The URLInfo extracted from the given url, or {@link #UNAVAILABLE} instance when the
     * url is not reachable.
     * @deprecated Use {@link #getURLInfo(URL, TimeoutConstraint)} instead
     */
    @Deprecated
    URLInfo getURLInfo(URL url);

    /**
     * @param url     The url from which information is retrieved.
     * @param timeout The timeout in milliseconds.
     * @return The URLInfo extracted from the given url, or {@link #UNAVAILABLE} when the url is not
     * reachable, never null.
     * @deprecated Use {@link #getURLInfo(URL, TimeoutConstraint)} instead
     */
    @Deprecated
    URLInfo getURLInfo(URL url, int timeout);

    /**
     * Returns the {@link URLInfo} extracted from the given url, or {@link #UNAVAILABLE} when the url is not
     * reachable. Never returns null.
     *
     * @param url                The URL for which the information is to be retrieved
     * @param timeoutConstraint The connectivity timeout constraints. Can be null, in which case the timeouts
     *                           are implementation specific
     * @return
     * @since 2.5
     */
    URLInfo getURLInfo(URL url, TimeoutConstraint timeoutConstraint);

    /**
     * @param url
     * @return
     * @throws IOException
     * @deprecated Use {@link #openStream(URL, TimeoutConstraint)} instead
     */
    @Deprecated
    InputStream openStream(URL url) throws IOException;

    /**
     * Opens and returns an {@link InputStream} to the passed <code>url</code>.
     *
     * @param url                The URL to which an {@link InputStream} has to be opened
     * @param timeoutConstraint The connectivity timeout constraints. Can be null, in which case the timeouts
     *                           are implementation specific
     * @return
     * @throws IOException
     * @since 2.5
     */
    InputStream openStream(URL url, TimeoutConstraint timeoutConstraint) throws IOException;

    /**
     * @param src
     * @param dest
     * @param l
     * @throws IOException
     * @deprecated Use {@link #download(URL, File, CopyProgressListener, TimeoutConstraint)} instead
     */
    @Deprecated
    void download(URL src, File dest, CopyProgressListener l) throws IOException;

    /**
     * Downloads the resource available at <code>src</code> to the target <code>dest</code>
     *
     * @param src                The source URL to download the resource from
     * @param dest               The destination {@link File} to download the resource to
     * @param listener           The listener that will be notified of the download progress
     * @param timeoutConstraint The connectivity timeout constraints. Can be null, in which case the timeouts
     *                           are implementation specific
     * @throws IOException
     * @since 2.5
     */
    void download(URL src, File dest, CopyProgressListener listener, TimeoutConstraint timeoutConstraint) throws IOException;

    /**
     * @param src
     * @param dest
     * @param l
     * @throws IOException
     * @deprecated Use {@link #upload(File, URL, CopyProgressListener, TimeoutConstraint)} instead
     */
    @Deprecated
    void upload(File src, URL dest, CopyProgressListener l) throws IOException;

    /**
     * Uploads the <code>src</code> {@link File} to the target <code>dest</code> {@link URL}
     *
     * @param src                The source {@link File} to upload
     * @param dest               The target URL where the {@link File} has to be uploaded
     * @param listener           The listener that will be notified of the upload progress
     * @param timeoutConstraint The connectivity timeout constraints. Can be null, in which case the timeouts
     *                           are implementation specific
     * @throws IOException
     * @since 2.5
     */
    void upload(File src, URL dest, CopyProgressListener listener, TimeoutConstraint timeoutConstraint) throws IOException;

    void setRequestMethod(int requestMethod);
}
