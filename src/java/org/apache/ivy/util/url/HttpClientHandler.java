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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.NTCredentials;
import org.apache.commons.httpclient.auth.AuthPolicy;
import org.apache.commons.httpclient.auth.AuthScheme;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.auth.CredentialsNotAvailableException;
import org.apache.commons.httpclient.auth.CredentialsProvider;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.ivy.Ivy;
import org.apache.ivy.util.CopyProgressListener;
import org.apache.ivy.util.FileUtil;
import org.apache.ivy.util.HostUtil;
import org.apache.ivy.util.Message;

/**
 *
 */
public class HttpClientHandler extends AbstractURLHandler {
    private static final SimpleDateFormat LAST_MODIFIED_FORMAT = new SimpleDateFormat(
            "EEE, d MMM yyyy HH:mm:ss z", Locale.US);

    // proxy configuration: obtain from system properties
    private int proxyPort;

    private String proxyHost = null;

    private String proxyUserName = null;

    private String proxyPasswd = null;

    private HttpClientHelper httpClientHelper;

    private static HttpClient httpClient;

    public HttpClientHandler() {
        configureProxy();
    }

    private void configureProxy() {
        proxyHost = System.getProperty("http.proxyHost");
        // TODO constant is better ...
        if (useProxy()) {
            proxyPort = Integer.parseInt(System.getProperty("http.proxyPort", "80"));
            proxyUserName = System.getProperty("http.proxyUser");
            proxyPasswd = System.getProperty("http.proxyPassword");
            // It seems there is no equivalent in HttpClient for
            // 'http.nonProxyHosts' property
            Message.verbose("proxy configured: host=" + proxyHost + " port=" + proxyPort + " user="
                    + proxyUserName);
        } else {
            Message.verbose("no proxy configured");
        }
    }

    public InputStream openStream(URL url) throws IOException {
        GetMethod get = doGet(url, 0);
        if (!checkStatusCode(url, get)) {
            get.releaseConnection();
            throw new IOException("The HTTP response code for " + url
                    + " did not indicate a success." + " See log for more detail.");
        }

        Header encoding = get.getResponseHeader("Content-Encoding");
        return getDecodingInputStream(encoding == null ? null : encoding.getValue(),
            get.getResponseBodyAsStream());
    }

    public void download(URL src, File dest, CopyProgressListener l) throws IOException {
        GetMethod get = doGet(src, 0);
        try {
            // We can only figure the content we got is want we want if the status is success.
            if (!checkStatusCode(src, get)) {
                throw new IOException("The HTTP response code for " + src
                        + " did not indicate a success." + " See log for more detail.");
            }

            Header encoding = get.getResponseHeader("Content-Encoding");
            InputStream is = getDecodingInputStream(encoding == null ? null : encoding.getValue(),
                get.getResponseBodyAsStream());
            FileUtil.copy(is, dest, l);
            dest.setLastModified(getLastModified(get));
        } finally {
            get.releaseConnection();
        }
    }

    public void upload(File src, URL dest, CopyProgressListener l) throws IOException {
        HttpClient client = getClient();

        PutMethod put = new PutMethod(normalizeToString(dest));
        put.setDoAuthentication(useAuthentication(dest) || useProxyAuthentication());
        put.getParams().setBooleanParameter("http.protocol.expect-continue", true);
        try {
            put.setRequestEntity(new FileRequestEntity(src));
            int statusCode = client.executeMethod(put);
            validatePutStatusCode(dest, statusCode, null);
        } finally {
            put.releaseConnection();
        }
    }

    public URLInfo getURLInfo(URL url) {
        return getURLInfo(url, 0);
    }

    public URLInfo getURLInfo(URL url, int timeout) {
        HttpMethodBase method = null;
        try {
            if (getRequestMethod() == URLHandler.REQUEST_METHOD_HEAD) {
                method = doHead(url, timeout);
            } else {
                method = doGet(url, timeout);
            }
            if (checkStatusCode(url, method)) {
                return new URLInfo(true, getResponseContentLength(method), getLastModified(method),
                        method.getRequestCharSet());
            }
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
            if (method != null) {
                method.releaseConnection();
            }
        }
        return UNAVAILABLE;
    }

    private boolean checkStatusCode(URL url, HttpMethodBase method) throws IOException {
        int status = method.getStatusCode();
        if (status == HttpStatus.SC_OK) {
            return true;
        }

        // IVY-1328: some servers return a 204 on a HEAD request
        if ("HEAD".equals(method.getName()) && (status == 204)) {
            return true;
        }

        Message.debug("HTTP response status: " + status + " url=" + url);
        if (status == HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED) {
            Message.warn("Your proxy requires authentication.");
        } else if (String.valueOf(status).startsWith("4")) {
            Message.verbose("CLIENT ERROR: " + method.getStatusText() + " url=" + url);
        } else if (String.valueOf(status).startsWith("5")) {
            Message.error("SERVER ERROR: " + method.getStatusText() + " url=" + url);
        }

        return false;
    }

    private long getLastModified(HttpMethodBase method) {
        Header header = method.getResponseHeader("last-modified");
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

    private long getResponseContentLength(HttpMethodBase head) {
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

    private GetMethod doGet(URL url, int timeout) throws IOException {
        HttpClient client = getClient();
        client.setTimeout(timeout);

        GetMethod get = new GetMethod(normalizeToString(url));
        get.setDoAuthentication(useAuthentication(url) || useProxyAuthentication());
        get.setRequestHeader("Accept-Encoding", "gzip,deflate");
        client.executeMethod(get);
        return get;
    }

    private HeadMethod doHead(URL url, int timeout) throws IOException {
        HttpClient client = getClient();
        client.setTimeout(timeout);

        HeadMethod head = new HeadMethod(normalizeToString(url));
        head.setDoAuthentication(useAuthentication(url) || useProxyAuthentication());
        client.executeMethod(head);
        return head;
    }

    private HttpClient getClient() {
        if (httpClient == null) {
            final MultiThreadedHttpConnectionManager connManager = new MultiThreadedHttpConnectionManager();
            httpClient = new HttpClient(connManager);

            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                public void run() {
                    connManager.shutdown();
                }
            }));

            List authPrefs = new ArrayList(3);
            authPrefs.add(AuthPolicy.DIGEST);
            authPrefs.add(AuthPolicy.BASIC);
            authPrefs.add(AuthPolicy.NTLM); // put it at the end to give less priority (IVY-213)
            httpClient.getParams().setParameter(AuthPolicy.AUTH_SCHEME_PRIORITY, authPrefs);

            if (useProxy()) {
                httpClient.getHostConfiguration().setProxy(proxyHost, proxyPort);
                if (useProxyAuthentication()) {
                    httpClient.getState().setProxyCredentials(
                        new AuthScope(proxyHost, proxyPort, AuthScope.ANY_REALM),
                        createCredentials(proxyUserName, proxyPasswd));
                }
            }

            // user-agent
            httpClient.getParams().setParameter(HttpMethodParams.USER_AGENT,
                getUserAgent());

            // authentication
            httpClient.getParams().setParameter(CredentialsProvider.PROVIDER,
                new IvyCredentialsProvider());
        }

        return httpClient;
    }

    private boolean useProxy() {
        return proxyHost != null && proxyHost.trim().length() > 0;
    }

    private boolean useAuthentication(URL url) {
        return CredentialsStore.INSTANCE.hasCredentials(url.getHost());
    }

    private boolean useProxyAuthentication() {
        return (proxyUserName != null && proxyUserName.trim().length() > 0);
    }

    private static final class HttpClientHelper3x implements HttpClientHelper {
        private static final int VERSION = 3;

        private HttpClientHelper3x() {
        }

        public long getResponseContentLength(HttpMethodBase method) {
            return method.getResponseContentLength();
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

        public long getResponseContentLength(HttpMethodBase method) {
            Header header = method.getResponseHeader("Content-Length");
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
        long getResponseContentLength(HttpMethodBase method);

        int getHttpClientMajorVersion();
    }

    private static class IvyCredentialsProvider implements CredentialsProvider {

        public Credentials getCredentials(AuthScheme scheme, String host, int port, boolean proxy)
                throws CredentialsNotAvailableException {
            String realm = scheme.getRealm();

            org.apache.ivy.util.Credentials c = CredentialsStore.INSTANCE.getCredentials(realm,
                host);
            if (c != null) {
                return createCredentials(c.getUserName(), c.getPasswd());
            }

            return null;
        }
    }

    private static Credentials createCredentials(String username, String password) {
        String user;
        String domain;

        int backslashIndex = username.indexOf('\\');
        if (backslashIndex >= 0) {
            user = username.substring(backslashIndex + 1);
            domain = username.substring(0, backslashIndex);
        } else {
            user = username;
            domain = System.getProperty("http.auth.ntlm.domain", "");
        }

        return new NTCredentials(user, password, HostUtil.getLocalHostName(), domain);
    }

    private static class FileRequestEntity implements RequestEntity {
        private File file;

        public FileRequestEntity(File file) {
            this.file = file;
        }

        public long getContentLength() {
            return file.length();
        }

        public String getContentType() {
            return null;
        }

        public boolean isRepeatable() {
            return true;
        }

        public void writeRequest(OutputStream out) throws IOException {
            InputStream instream = new FileInputStream(file);
            try {
                FileUtil.copy(instream, out, null, false);
            } finally {
                instream.close();
            }
        }
    }

}
