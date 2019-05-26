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

/**
 * A enhanced version of {@link URLHandler} which respects {@link TimeoutConstraint}s on
 * the operations dealing with download, upload, reachability checks etc...
 */
@SuppressWarnings("deprecation")
public interface TimeoutConstrainedURLHandler extends URLHandler {

    /**
     * Returns true if the passed <code>URL</code> is reachable. Else returns false. Uses the
     * passed <code>timeoutConstraint</code> for determining the connectivity to the URL.
     * <p>
     * Please use {@link #getURLInfo(URL, TimeoutConstraint)} if more one information about the
     * <code>url</code> is needed
     * </p>
     *
     * @param url               The URL to access
     * @param timeoutConstraint The connectivity timeout constraints. Can be null, in which case
     *                          the timeouts are implementation specific
     * @return boolean
     * @since 2.5
     */
    boolean isReachable(URL url, TimeoutConstraint timeoutConstraint);

    /**
     * Returns the number of bytes of data that's available for the resource at the passed
     * <code>url</code>. Returns 0 if the passed <code>url</code> isn't reachable
     *
     * @param url               The URL to access
     * @param timeoutConstraint The connectivity timeout constraints. Can be null, in which case
     *                          the timeouts are implementation specific
     * @return long
     * @since 2.5
     */
    long getContentLength(URL url, TimeoutConstraint timeoutConstraint);

    /**
     * Returns the last modified timestamp of the resource accessible at the passed
     * <code>url</code>.
     * <p>
     * Please use {@link #getURLInfo(URL, TimeoutConstraint)} if more one information about the
     * <code>url</code> is needed
     * </p>
     *
     * @param url               The URL to access
     * @param timeoutConstraint The connectivity timeout constraints. Can be null, in which case
     *                          the timeouts are implementation specific
     * @return long
     * @since 2.5
     */
    long getLastModified(URL url, TimeoutConstraint timeoutConstraint);

    /**
     * Returns the {@link URLInfo} extracted from the given url, or {@link #UNAVAILABLE} when the
     * url is not reachable. Never returns null.
     *
     * @param url               The URL for which the information is to be retrieved
     * @param timeoutConstraint The connectivity timeout constraints. Can be null, in which case
     *                          the timeouts are implementation specific
     * @return URLInfo
     * @since 2.5
     */
    URLInfo getURLInfo(URL url, TimeoutConstraint timeoutConstraint);

    /**
     * Opens and returns an {@link InputStream} to the passed <code>url</code>.
     *
     * @param url               The URL to which an {@link InputStream} has to be opened
     * @param timeoutConstraint The connectivity timeout constraints. Can be null, in which case
     *                          the timeouts are implementation specific
     * @return InputStream
     * @throws IOException if something goes wrong
     * @since 2.5
     */
    InputStream openStream(URL url, TimeoutConstraint timeoutConstraint) throws IOException;

    /**
     * Downloads the resource available at <code>src</code> to the target <code>dest</code>
     *
     * @param src               The source URL to download the resource from
     * @param dest              The destination {@link File} to download the resource to
     * @param listener          The listener that will be notified of the download progress
     * @param timeoutConstraint The connectivity timeout constraints. Can be null, in which case
     *                          the timeouts are implementation specific
     * @throws IOException if something goes wrong
     * @since 2.5
     */
    void download(URL src, File dest, CopyProgressListener listener, TimeoutConstraint timeoutConstraint) throws IOException;

    /**
     * Uploads the <code>src</code> {@link File} to the target <code>dest</code> {@link URL}
     *
     * @param src               The source {@link File} to upload
     * @param dest              The target URL where the {@link File} has to be uploaded
     * @param listener          The listener that will be notified of the upload progress
     * @param timeoutConstraint The connectivity timeout constraints. Can be null, in which case
     *                          the timeouts are implementation specific
     * @throws IOException if something goes wrong
     * @since 2.5
     */
    void upload(File src, URL dest, CopyProgressListener listener, TimeoutConstraint timeoutConstraint) throws IOException;
}
