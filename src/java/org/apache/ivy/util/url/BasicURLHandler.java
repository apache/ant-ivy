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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;

import org.apache.ivy.util.CopyProgressListener;
import org.apache.ivy.util.FileUtil;
import org.apache.ivy.util.Message;

/**
 * 
 */
public class BasicURLHandler extends AbstractURLHandler {

    private static final int BUFFER_SIZE = 64 * 1024;

    private static class HttpStatus {
        static final int SC_OK = 200;

        static final int SC_PROXY_AUTHENTICATION_REQUIRED = 407;
    }

    public BasicURLHandler() {
        Message.debug("installing " + IvyAuthenticator.INSTANCE.getClass()); // do not remove,
        // ensure
        // IvyAuthenticator
        // class loading!
    }

    public URLInfo getURLInfo(URL url) {
        return getURLInfo(url, 0);
    }

    public URLInfo getURLInfo(URL url, int timeout) {
        URLConnection con = null;
        try {
            con = url.openConnection();
            if (con instanceof HttpURLConnection) {
                ((HttpURLConnection) con).setRequestMethod("HEAD");
                int status = ((HttpURLConnection) con).getResponseCode();
                if (status == HttpStatus.SC_OK) {
                    return new URLInfo(true, ((HttpURLConnection) con).getContentLength(), con
                            .getLastModified());
                }
                if (status == HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED) {
                    Message.warn("Your proxy requires authentication.");
                } else if (String.valueOf(status).startsWith("4")) {
                    Message.verbose("CLIENT ERROR: "
                            + ((HttpURLConnection) con).getResponseMessage() + " url=" + url);
                } else if (String.valueOf(status).startsWith("5")) {
                    Message.error("SERVER ERROR: " + ((HttpURLConnection) con).getResponseMessage()
                            + " url=" + url);
                }
                Message.debug("HTTP response status: " + status + " url=" + url);
            } else {
                int contentLength = con.getContentLength();
                if (contentLength <= 0) {
                    return UNAVAILABLE;
                } else {
                    return new URLInfo(true, contentLength, con.getLastModified());
                }
            }
        } catch (UnknownHostException e) {
            Message.warn("Host " + e.getMessage() + " not found. url=" + url);
            Message.info("You probably access the destination server through "
                + "a proxy server that is not well configured.");
        } catch (IOException e) {
            Message.error("Server access Error: " + e.getMessage() + " url=" + url);
        } finally {
            disconnect(con);
        }
        return UNAVAILABLE;
    }

    public InputStream openStream(URL url) throws IOException {
        URLConnection conn = null;
        InputStream inStream = null;
        try {
            conn = url.openConnection();
            inStream = conn.getInputStream();
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();

            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = inStream.read(buffer)) > 0) {
                outStream.write(buffer, 0, len);
            }
            return new ByteArrayInputStream(outStream.toByteArray());
        } finally {
            if (inStream != null) {
                inStream.close();
            }

            disconnect(conn);
        }
    }

    public void download(URL src, File dest, CopyProgressListener l) throws IOException {
        URLConnection srcConn = null;
        try {
            srcConn = src.openConnection();
            int contentLength = srcConn.getContentLength();
            FileUtil.copy(srcConn.getInputStream(), dest, l);
            if (dest.length() != contentLength && contentLength != -1) {
                dest.delete();
                throw new IOException(
                        "Downloaded file size doesn't match expected Content Length for " + src
                                + ". Please retry.");
            }
        } finally {
            disconnect(srcConn);
        }
    }

    private void disconnect(URLConnection con) {
        if (con instanceof HttpURLConnection) {
            ((HttpURLConnection) con).disconnect();
        } else if (con != null
                && "sun.net.www.protocol.file.FileURLConnection".equals(con.getClass().getName())) {
            // ugly fix for a sun jre bug:
            // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4257700
            //
            // getting header info on the fileurlconnection opens the connection,
            // which opens a file input stream without closing it.
            try {
                con.getInputStream().close();
            } catch (IOException e) {
                // ignored
            }
        }
    }

}
