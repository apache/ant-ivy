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
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthPolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.ivy.util.CopyProgressListener;
import org.apache.ivy.util.Credentials;
import org.apache.ivy.util.FileUtil;
import org.apache.ivy.util.Message;

/**
 *
 */
public class HttpClientHandler extends AbstractURLHandler {
    private static final SimpleDateFormat LAST_MODIFIED_FORMAT = new SimpleDateFormat(
            "EEE, d MMM yyyy HH:mm:ss z", Locale.US);

    // proxy configuration: obtain from system properties
    private int proxyPort;

    private String proxyRealm = null;

    private String proxyHost = null;

    private String proxyUserName = null;

    private String proxyPasswd = null;

    private HttpClientHelper httpClientHelper;

    public HttpClientHandler() {
        configureProxy();
    }

    private void configureProxy() {
        proxyRealm = null;
        // no equivalent for realm in jdk proxy support ?
        proxyHost = System.getProperty("http.proxyHost");
        // TODO constant is better ...
        if (useProxy()) {
            proxyPort = Integer.parseInt(System.getProperty("http.proxyPort", "80"));
            proxyUserName = System.getProperty("http.proxyUser");
            proxyPasswd = System.getProperty("http.proxyPassword");
            // It seems there is no equivalent in HttpClient for
            // 'http.nonProxyHosts' property
            Message.verbose("proxy configured: host=" + proxyHost + " port=" + proxyPort
                    + " user=" + proxyUserName);
        } else {
            Message.verbose("no proxy configured");
        }
    }

    public InputStream openStream(URL url) throws IOException {
        GetMethod get = doGet(url);
        return new GETInputStream(get);
    }

    public void download(URL src, File dest, CopyProgressListener l) throws IOException {
        GetMethod get = doGet(src);
        FileUtil.copy(get.getResponseBodyAsStream(), dest, l);
        get.releaseConnection();
    }

    public URLInfo getURLInfo(URL url) {
        return getURLInfo(url, 0);
    }

    public URLInfo getURLInfo(URL url, int timeout) {
        HeadMethod head = null;
        try {
            head = doHead(url, timeout);
            int status = head.getStatusCode();
            head.releaseConnection();
            if (status == HttpStatus.SC_OK) {
                return new URLInfo(true, getResponseContentLength(head), getLastModified(head));
            }
            if (status == HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED) {
                Message.error("Your proxy requires authentication.");
            } else if (String.valueOf(status).startsWith("4")) {
                Message.verbose("CLIENT ERROR: " + head.getStatusText() + " url=" + url);
            } else if (String.valueOf(status).startsWith("5")) {
                Message.warn("SERVER ERROR: " + head.getStatusText() + " url=" + url);
            }
            Message.debug("HTTP response status: " + status + "=" + head.getStatusText() + " url="
                    + url);
        } catch (HttpException e) {
            Message.error("HttpClientHandler: " + e.getMessage() + ":" + e.getReasonCode() + "="
                    + e.getReason() + " url=" + url);
        } catch (UnknownHostException e) {
            Message.warn("Host " + e.getMessage() + " not found. url=" + url);
            Message.info("You probably access the destination server through "
                + "a proxy server that is not well configured.");
        } catch (IOException e) {
            Message.error("HttpClientHandler: " + e.getMessage() + " url=" + url);
        } catch (IllegalArgumentException e) {
            // thrown by HttpClient to indicate the URL is not valid, this happens for instance
            // when trying to download a dynamic version (cfr IVY-390)
        } finally {
            if (head != null) {
                head.releaseConnection();
            }
        }
        return UNAVAILABLE;
    }

    private long getLastModified(HeadMethod head) {
        Header header = head.getResponseHeader("last-modified");
        if (header != null) {
            String lastModified = header.getValue();
            try {
                return LAST_MODIFIED_FORMAT.parse(lastModified).getTime();
            } catch (ParseException e) {
                // ignored
            }
            return System.currentTimeMillis();
        } else {
            return System.currentTimeMillis();
        }
    }

    private long getResponseContentLength(HeadMethod head) {
        return getHttpClientHelper().getResponseContentLength(head);
    }

    private HttpClientHelper getHttpClientHelper() {
        if (httpClientHelper == null) {
            // use commons httpclient 3.0 if available
            try {
                HttpMethodBase.class.getMethod("getResponseContentLength", new Class[0]);
                httpClientHelper = new HttpClientHelper3x();
                Message.verbose("using commons httpclient 3.x helper");
            } catch (SecurityException e) {
                Message.verbose("unable to get access to getResponseContentLength of "
                    + "commons-httpclient HeadMethod. Please use commons-httpclient 3.0 or "
                    + "use ivy with sufficient security permissions.");
                Message.verbose("exception: " + e.getMessage());
                httpClientHelper = new HttpClientHelper2x();
                Message.verbose("using commons httpclient 2.x helper");
            } catch (NoSuchMethodException e) {
                httpClientHelper = new HttpClientHelper2x();
                Message.verbose("using commons httpclient 2.x helper");
            }
        }
        return httpClientHelper;
    }

    public int getHttpClientMajorVersion() {
        HttpClientHelper helper = getHttpClientHelper();
        return helper.getHttpClientMajorVersion();
    }

    private GetMethod doGet(URL url) throws IOException {
        HttpClient client = getClient(url);

        GetMethod get = new GetMethod(url.toExternalForm());
        get.setDoAuthentication(useAuthentication(url) || useProxyAuthentication());
        client.executeMethod(get);
        return get;
    }

    private HeadMethod doHead(URL url, int timeout) throws IOException {
        HttpClient client = getClient(url);
        client.setTimeout(timeout);

        HeadMethod head = new HeadMethod(url.toExternalForm());
        head.setDoAuthentication(useAuthentication(url) || useProxyAuthentication());
        client.executeMethod(head);
        return head;
    }

    private HttpClient getClient(URL url) {
        HttpClient client = new HttpClient();

        List authPrefs = new ArrayList(2);
        authPrefs.add(AuthPolicy.DIGEST);
        authPrefs.add(AuthPolicy.BASIC);
        // Exclude the NTLM authentication scheme because it is not supported by this class
        client.getParams().setParameter(AuthPolicy.AUTH_SCHEME_PRIORITY, authPrefs);

        if (useProxy()) {
            client.getHostConfiguration().setProxy(proxyHost, proxyPort);
            if (useProxyAuthentication()) {
                client.getState().setProxyCredentials(proxyRealm, proxyHost,
                    new UsernamePasswordCredentials(proxyUserName, proxyPasswd));
            }
        }
        Credentials c = getCredentials(url);
        if (c != null) {
            Message.debug("found credentials for " + url + ": " + c);
            client.getState().setCredentials(c.getRealm(), c.getHost(),
                new UsernamePasswordCredentials(c.getUserName(), c.getPasswd()));
        }
        return client;
    }

    private boolean useProxy() {
        return proxyHost != null && proxyHost.trim().length() > 0;
    }

    private boolean useAuthentication(URL url) {
        return getCredentials(url) != null;
    }

    private Credentials getCredentials(URL url) {
        return CredentialsStore.INSTANCE.getCredentials(null, url.getHost());
    }

    private boolean useProxyAuthentication() {
        return (proxyUserName != null && proxyUserName.trim().length() > 0);
    }

    private static final class GETInputStream extends InputStream {
        private InputStream is;

        private GetMethod get;

        private GETInputStream(GetMethod get) throws IOException {
            this.get = get;
            is = get.getResponseBodyAsStream();
        }

        public int available() throws IOException {
            return is.available();
        }

        public void close() throws IOException {
            is.close();
            get.releaseConnection();
        }

        public boolean equals(Object obj) {
            return is.equals(obj);
        }

        public int hashCode() {
            return is.hashCode();
        }

        public void mark(int readlimit) {
            is.mark(readlimit);
        }

        public boolean markSupported() {
            return is.markSupported();
        }

        public int read() throws IOException {
            return is.read();
        }

        public int read(byte[] b, int off, int len) throws IOException {
            return is.read(b, off, len);
        }

        public int read(byte[] b) throws IOException {
            return is.read(b);
        }

        public void reset() throws IOException {
            is.reset();
        }

        public long skip(long n) throws IOException {
            return is.skip(n);
        }

        public String toString() {
            return is.toString();
        }
    }

    private static final class HttpClientHelper3x implements HttpClientHelper {
        private static final int VERSION = 3;

        private HttpClientHelper3x() {
        }

        public long getResponseContentLength(HeadMethod head) {
            return head.getResponseContentLength();
        }

        /**
         * {@inheritDoc}
         */
        public int getHttpClientMajorVersion() {
            return VERSION;
        }
    }

    private static final class HttpClientHelper2x implements HttpClientHelper {
        private static final int VERSION = 2;

        private HttpClientHelper2x() {
        }

        public long getResponseContentLength(HeadMethod head) {
            Header header = head.getResponseHeader("Content-Length");
            if (header != null) {
                try {
                    return Integer.parseInt(header.getValue());
                } catch (NumberFormatException e) {
                    Message.verbose("Invalid content-length value: " + e.getMessage());
                }
            }
            return 0;
        }

        /**
         * {@inheritDoc}
         */
        public int getHttpClientMajorVersion() {
            return VERSION;
        }
    }

    public interface HttpClientHelper {
        long getResponseContentLength(HeadMethod head);

        int getHttpClientMajorVersion();
    }
}
