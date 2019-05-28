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
import org.apache.ivy.util.FileUtil;
import org.apache.ivy.util.Message;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;

/**
 *
 */
public class BasicURLHandler extends AbstractURLHandler implements TimeoutConstrainedURLHandler {

    private static final int BUFFER_SIZE = 64 * 1024;

    private static final class HttpStatus {
        static final int SC_OK = 200;

        static final int SC_PROXY_AUTHENTICATION_REQUIRED = 407;

        private HttpStatus() {
        }
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
    public boolean isReachable(final URL url, final TimeoutConstraint timeoutConstraint) {
        return this.getURLInfo(url, timeoutConstraint).isReachable();
    }

    @SuppressWarnings("deprecation")
    @Override
    public long getContentLength(final URL url, final TimeoutConstraint timeoutConstraint) {
        return this.getURLInfo(url, timeoutConstraint).getContentLength();
    }

    @SuppressWarnings("deprecation")
    @Override
    public long getLastModified(final URL url, final TimeoutConstraint timeoutConstraint) {
        return this.getURLInfo(url, timeoutConstraint).getLastModified();
    }

    @SuppressWarnings("deprecation")
    @Override
    public URLInfo getURLInfo(final URL url, final TimeoutConstraint timeoutConstraint) {
        // Install the IvyAuthenticator
        if ("http".equals(url.getProtocol()) || "https".equals(url.getProtocol())) {
            IvyAuthenticator.install();
        }
        final int connectionTimeout = (timeoutConstraint == null || timeoutConstraint.getConnectionTimeout() < 0) ? 0 : timeoutConstraint.getConnectionTimeout();
        final int readTimeout = (timeoutConstraint == null || timeoutConstraint.getReadTimeout() < 0) ? 0 : timeoutConstraint.getReadTimeout();
        URLConnection con = null;
        try {
            final URL normalizedURL = normalizeToURL(url);
            con = normalizedURL.openConnection();
            con.setConnectTimeout(connectionTimeout);
            con.setReadTimeout(readTimeout);
            con.setRequestProperty("User-Agent", getUserAgent());
            if (con instanceof HttpURLConnection) {
                HttpURLConnection httpCon = (HttpURLConnection) con;
                if (getRequestMethod() == TimeoutConstrainedURLHandler.REQUEST_METHOD_HEAD) {
                    httpCon.setRequestMethod("HEAD");
                }
                if (checkStatusCode(normalizedURL, httpCon)) {
                    String bodyCharset = getCharSetFromContentType(con.getContentType());
                    return new URLInfo(true, httpCon.getContentLength(), con.getLastModified(),
                            bodyCharset);
                }
            } else {
                int contentLength = con.getContentLength();
                if (contentLength <= 0) {
                    return UNAVAILABLE;
                } else {
                    // TODO: not HTTP... maybe we *don't* want to default to ISO-8559-1 here?
                    String bodyCharset = getCharSetFromContentType(con.getContentType());
                    return new URLInfo(true, contentLength, con.getLastModified(), bodyCharset);
                }
            }
        } catch (UnknownHostException e) {
            Message.warn("Host " + e.getMessage() + " not found. url=" + url);
            Message.info("You probably access the destination server through "
                    + "a proxy server that is not well configured.");
        } catch (IOException e) {
            Message.error("Server access error at url " + url, e);
        } finally {
            disconnect(con);
        }
        return UNAVAILABLE;
    }

    /**
     * Extract the charset from the Content-Type header string, or default to ISO-8859-1 as per
     * rfc2616-sec3.html#sec3.7.1 .
     *
     * @param contentType the Content-Type header string
     * @return the charset as specified in the content type, or ISO-8859-1 if unspecified.
     */
    public static String getCharSetFromContentType(String contentType) {

        String charSet = null;

        if (contentType != null) {
            for (String el : contentType.split(";")) {
                String element = el.trim();
                if (element.toLowerCase().startsWith("charset=")) {
                    charSet = element.substring("charset=".length());
                }
            }
        }

        if (charSet == null || charSet.length() == 0) {
            // default to ISO-8859-1 as per rfc2616-sec3.html#sec3.7.1
            charSet = "ISO-8859-1";
        }

        return charSet;
    }

    private boolean checkStatusCode(URL url, HttpURLConnection con) throws IOException {
        int status = con.getResponseCode();
        if (status == HttpStatus.SC_OK) {
            return true;
        }

        // IVY-1328: some servers return a 204 on a HEAD request
        if ("HEAD".equals(con.getRequestMethod()) && (status == 204)) {
            return true;
        }

        Message.debug("HTTP response status: " + status + " url=" + url);
        if (status == HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED) {
            Message.warn("Your proxy requires authentication.");
        } else if (String.valueOf(status).startsWith("4")) {
            Message.verbose("CLIENT ERROR: " + con.getResponseMessage() + " url=" + url);
        } else if (String.valueOf(status).startsWith("5")) {
            Message.error("SERVER ERROR: " + con.getResponseMessage() + " url=" + url);
        }
        return false;
    }

    @Override
    public InputStream openStream(final URL url) throws IOException {
        return this.openStream(url, null);
    }

    @Override
    public InputStream openStream(final URL url, final TimeoutConstraint timeoutConstraint) throws IOException {
        // Install the IvyAuthenticator
        if ("http".equals(url.getProtocol()) || "https".equals(url.getProtocol())) {
            IvyAuthenticator.install();
        }
        final int connectionTimeout = (timeoutConstraint == null || timeoutConstraint.getConnectionTimeout() < 0) ? 0 : timeoutConstraint.getConnectionTimeout();
        final int readTimeout = (timeoutConstraint == null || timeoutConstraint.getReadTimeout() < 0) ? 0 : timeoutConstraint.getReadTimeout();

        URLConnection conn = null;
        try {
            final URL normalizedURL = normalizeToURL(url);
            conn = normalizedURL.openConnection();
            conn.setConnectTimeout(connectionTimeout);
            conn.setReadTimeout(readTimeout);
            conn.setRequestProperty("User-Agent", getUserAgent());
            conn.setRequestProperty("Accept-Encoding", "gzip,deflate");
            if (conn instanceof HttpURLConnection) {
                HttpURLConnection httpCon = (HttpURLConnection) conn;
                if (!checkStatusCode(normalizedURL, httpCon)) {
                    throw new IOException("The HTTP response code for " + normalizedURL
                            + " did not indicate a success." + " See log for more detail.");
                }
            }
            InputStream inStream = getDecodingInputStream(conn.getContentEncoding(),
                    conn.getInputStream());
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();

            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = inStream.read(buffer)) > 0) {
                outStream.write(buffer, 0, len);
            }
            return new ByteArrayInputStream(outStream.toByteArray());
        } finally {
            disconnect(conn);
        }
    }

    @Override
    public void download(final URL src, final File dest, final CopyProgressListener l) throws IOException {
        this.download(src, dest, l, null);
    }

    @Override
    public void download(final URL src, final File dest, final CopyProgressListener listener,
                         final TimeoutConstraint timeoutConstraint) throws IOException {

        // Install the IvyAuthenticator
        if ("http".equals(src.getProtocol()) || "https".equals(src.getProtocol())) {
            IvyAuthenticator.install();
        }
        final int connectionTimeout = (timeoutConstraint == null || timeoutConstraint.getConnectionTimeout() < 0) ? 0 : timeoutConstraint.getConnectionTimeout();
        final int readTimeout = (timeoutConstraint == null || timeoutConstraint.getReadTimeout() < 0) ? 0 : timeoutConstraint.getReadTimeout();

        URLConnection srcConn = null;
        try {
            final URL normalizedURL = normalizeToURL(src);
            srcConn = normalizedURL.openConnection();
            srcConn.setConnectTimeout(connectionTimeout);
            srcConn.setReadTimeout(readTimeout);
            srcConn.setRequestProperty("User-Agent", getUserAgent());
            srcConn.setRequestProperty("Accept-Encoding", "gzip,deflate");
            if (srcConn instanceof HttpURLConnection) {
                HttpURLConnection httpCon = (HttpURLConnection) srcConn;
                if (!checkStatusCode(normalizedURL, httpCon)) {
                    throw new IOException("The HTTP response code for " + normalizedURL
                            + " did not indicate a success." + " See log for more detail.");
                }
            }

            // do the download
            InputStream inStream = getDecodingInputStream(srcConn.getContentEncoding(),
                    srcConn.getInputStream());
            FileUtil.copy(inStream, dest, listener);

            // check content length only if content was not encoded
            if (srcConn.getContentEncoding() == null) {
                final int contentLength = srcConn.getContentLength();
                final long destFileSize = dest.length();
                if (contentLength != -1 && destFileSize != contentLength) {
                    dest.delete();
                    throw new IOException(
                            "Downloaded file size (" + destFileSize + ") doesn't match expected " +
                                    "Content Length (" + contentLength + ") for " + normalizedURL + ". Please retry.");
                }
            }

            // update modification date
            long lastModified = srcConn.getLastModified();
            if (lastModified > 0) {
                dest.setLastModified(lastModified);
            }
        } finally {
            disconnect(srcConn);
        }
    }

    @Override
    public void upload(final File source, final URL dest, final CopyProgressListener l) throws IOException {
        this.upload(source, dest, l, null);
    }

    @Override
    public void upload(final File src, final URL dest, final CopyProgressListener listener,
                       final TimeoutConstraint timeoutConstraint) throws IOException {

        if (!"http".equals(dest.getProtocol()) && !"https".equals(dest.getProtocol())) {
            throw new UnsupportedOperationException(
                    "URL repository only support HTTP PUT at the moment");
        }

        // Install the IvyAuthenticator
        IvyAuthenticator.install();

        final int connectionTimeout = (timeoutConstraint == null || timeoutConstraint.getConnectionTimeout() < 0) ? 0 : timeoutConstraint.getConnectionTimeout();
        HttpURLConnection conn = null;
        try {
            final URL normalizedDestURL = normalizeToURL(dest);
            conn = (HttpURLConnection) normalizedDestURL.openConnection();
            conn.setDoOutput(true);
            conn.setConnectTimeout(connectionTimeout);
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("User-Agent", getUserAgent());
            conn.setRequestProperty("Content-type", "application/octet-stream");
            conn.setRequestProperty("Content-length", Long.toString(src.length()));
            conn.setInstanceFollowRedirects(true);

            try (final InputStream in = new FileInputStream(src)) {
                final OutputStream os = conn.getOutputStream();
                FileUtil.copy(in, os, listener);
            }
            validatePutStatusCode(normalizedDestURL, conn.getResponseCode(), conn.getResponseMessage());
        } finally {
            disconnect(conn);
        }

    }

    private void disconnect(URLConnection con) {
        if (con instanceof HttpURLConnection) {
            if (!"HEAD".equals(((HttpURLConnection) con).getRequestMethod())) {
                // We must read the response body before disconnecting!
                // Cfr. http://java.sun.com/j2se/1.5.0/docs/guide/net/http-keepalive.html
                // [quote]Do not abandon a connection by ignoring the response body. Doing
                // so may results in idle TCP connections.[/quote]
                readResponseBody((HttpURLConnection) con);
            }

            ((HttpURLConnection) con).disconnect();
        } else if (con != null) {
            try {
                con.getInputStream().close();
            } catch (IOException e) {
                // ignored
            }
        }
    }

    /**
     * Read and ignore the response body.
     */
    private void readResponseBody(HttpURLConnection conn) {
        byte[] buffer = new byte[BUFFER_SIZE];

        try (InputStream inStream = conn.getInputStream()) {
            while (inStream.read(buffer) > 0) {
                // Skip content
            }
        } catch (IOException e) {
            // ignore
        }

        InputStream errStream = conn.getErrorStream();
        if (errStream != null) {
            try {
                while (errStream.read(buffer) > 0) {
                    // Skip content
                }
            } catch (IOException e) {
                // ignore
            } finally {
                try {
                    errStream.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }
}
