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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.ivy.util.CopyProgressListener;

/**
 * This interface is responsible for handling some URL manipulation (stream opening, downloading,
 * check reachability, ...).
 */
public interface URLHandler {

    /**
     * Using the slower REQUEST method for getting the basic URL infos. Use this when getting errors
     * behind a problematic/special proxy or firewall chain.
     */
    public static final int REQUEST_METHOD_GET = 1;

    /**
     * Using the faster HEAD method for getting the basic URL infos. Works for most common networks.
     */
    public static final int REQUEST_METHOD_HEAD = 2;

    public static class URLInfo {
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

    public static final URLInfo UNAVAILABLE = new URLInfo(false, 0, 0);

    /**
     * Please prefer getURLInfo when several infos are needed.
     * 
     * @param url
     *            the url to check
     * @return true if the target is reachable
     */
    public boolean isReachable(URL url);

    /**
     * Please prefer getURLInfo when several infos are needed.
     * 
     * @param url
     *            the url to check
     * @return true if the target is reachable
     */
    public boolean isReachable(URL url, int timeout);

    /**
     * Returns the length of the target if the given url is reachable, and without error code in
     * case of http urls. Please prefer getURLInfo when several infos are needed.
     * 
     * @param url
     *            the url to check
     * @return the length of the target if available, 0 if not reachable
     */
    public long getContentLength(URL url);

    /**
     * Returns the length of the target if the given url is reachable, and without error code in
     * case of http urls.
     * 
     * @param url
     *            the url to check
     * @param timeout
     *            the maximum time before considering an url is not reachable a timeout of zero
     *            indicates no timeout
     * @return the length of the target if available, 0 if not reachable
     */
    public long getContentLength(URL url, int timeout);

    /**
     * Please prefer getURLInfo when several infos are needed.
     * 
     * @param url
     *            the url to check
     * @return last modified timestamp of the given url
     */
    public long getLastModified(URL url);

    /**
     * Please prefer getURLInfo when several infos are needed.
     * 
     * @param url
     *            the url to check
     * @return last modified timestamp of the given url
     */
    public long getLastModified(URL url, int timeout);

    /**
     * Returns the URLInfo of the given url or a {@link #UNAVAILABLE} instance, if the url is not
     * reachable.
     * 
     * @param url
     *            The url from which information is retrieved.
     * @return The URLInfo extracted from the given url, or {@link #UNAVAILABLE} when the url is not
     *         available.
     */
    public URLInfo getURLInfo(URL url);

    /**
     * never returns null, return UNAVAILABLE when url is not reachable
     * 
     * @param url
     *            The url from which information is retrieved.
     * @param timeout
     *            The timeout in milliseconds.
     * @return The URLInfo extracted from the given url, or {@link #UNAVAILABLE} when the url is not
     *         available.
     */
    public URLInfo getURLInfo(URL url, int timeout);

    public InputStream openStream(URL url) throws IOException;

    public void download(URL src, File dest, CopyProgressListener l) throws IOException;

    public void upload(File src, URL dest, CopyProgressListener l) throws IOException;

    public void setRequestMethod(int requestMethod);
}
