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

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.config.Lookup;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.auth.BasicSchemeFactory;
import org.apache.http.impl.auth.DigestSchemeFactory;
import org.apache.http.impl.auth.NTLMSchemeFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.conn.SystemDefaultRoutePlanner;
import org.apache.ivy.core.settings.TimeoutConstraint;
import org.apache.ivy.util.CopyProgressListener;
import org.apache.ivy.util.FileUtil;
import org.apache.ivy.util.HostUtil;
import org.apache.ivy.util.Message;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ProxySelector;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class HttpClientHandler extends AbstractURLHandler implements TimeoutConstrainedURLHandler, AutoCloseable {
    private static final SimpleDateFormat LAST_MODIFIED_FORMAT = new SimpleDateFormat(
            "EEE, d MMM yyyy HH:mm:ss z", Locale.US);

    // A instance of the HttpClientHandler which gets registered to be closed
    // when the JVM exits
    static final HttpClientHandler DELETE_ON_EXIT_INSTANCE;

    static {
        DELETE_ON_EXIT_INSTANCE = new HttpClientHandler();
        final Thread shutdownHook = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    DELETE_ON_EXIT_INSTANCE.close();
                } catch (Exception e) {
                    // ignore since this is anyway happening during shutdown of the JVM
                }
            }
        });
        shutdownHook.setName("ivy-httpclient-shutdown-handler");
        shutdownHook.setDaemon(true);
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    private final CloseableHttpClient httpClient;

    public HttpClientHandler() {
        this.httpClient = buildUnderlyingClient();
    }

    private CloseableHttpClient buildUnderlyingClient() {
        return HttpClients.custom()
                .setConnectionManager(createConnectionManager())
                .setRoutePlanner(createProxyRoutePlanner())
                .setUserAgent(this.getUserAgent())
                .setDefaultAuthSchemeRegistry(createAuthSchemeRegistry())
                .setDefaultCredentialsProvider(new IvyCredentialsProvider())
                .build();
    }

    private static HttpRoutePlanner createProxyRoutePlanner() {
        // use the standard JRE ProxySelector to get proxy information
        Message.verbose("Using JRE standard ProxySelector for configuring HTTP proxy");
        return new SystemDefaultRoutePlanner(ProxySelector.getDefault());
    }

    private static Lookup<AuthSchemeProvider> createAuthSchemeRegistry() {
        return RegistryBuilder.<AuthSchemeProvider>create().register(AuthSchemes.DIGEST, new DigestSchemeFactory())
                .register(AuthSchemes.BASIC, new BasicSchemeFactory())
                .register(AuthSchemes.NTLM, new NTLMSchemeFactory())
                .build();
    }

    private static HttpClientConnectionManager createConnectionManager() {
        return new PoolingHttpClientConnectionManager();
    }

    private static List<String> getAuthSchemePreferredOrder() {
        return Arrays.asList(AuthSchemes.DIGEST, AuthSchemes.BASIC, AuthSchemes.NTLM);
    }

    @Override
    public InputStream openStream(final URL url) throws IOException {
        return this.openStream(url, null);
    }

    @Override
    public InputStream openStream(final URL url, final TimeoutConstraint timeoutConstraint) throws IOException {
        final int connectionTimeout = (timeoutConstraint == null || timeoutConstraint.getConnectionTimeout() < 0) ? 0 : timeoutConstraint.getConnectionTimeout();
        final int readTimeout = (timeoutConstraint == null || timeoutConstraint.getReadTimeout() < 0) ? 0 : timeoutConstraint.getReadTimeout();
        final CloseableHttpResponse response = doGet(url, connectionTimeout, readTimeout);
        this.requireSuccessStatus(HttpGet.METHOD_NAME, url, response);
        final Header encoding = this.getContentEncoding(response);
        return getDecodingInputStream(encoding == null ? null : encoding.getValue(), response.getEntity().getContent());
    }

    @Override
    public void download(final URL src, final File dest, final CopyProgressListener l) throws IOException {
        this.download(src, dest, l, null);
    }

    @Override
    public void download(final URL src, final File dest, final CopyProgressListener listener,
                         final TimeoutConstraint timeoutConstraint) throws IOException {

        final int connectionTimeout = (timeoutConstraint == null || timeoutConstraint.getConnectionTimeout() < 0) ? 0 : timeoutConstraint.getConnectionTimeout();
        final int readTimeout = (timeoutConstraint == null || timeoutConstraint.getReadTimeout() < 0) ? 0 : timeoutConstraint.getReadTimeout();
        try (final CloseableHttpResponse response = doGet(src, connectionTimeout, readTimeout)) {
            // We can only figure the content we got is want we want if the status is success.
            this.requireSuccessStatus(HttpGet.METHOD_NAME, src, response);
            final Header encoding = this.getContentEncoding(response);
            try (final InputStream is = getDecodingInputStream(encoding == null ? null : encoding.getValue(),
                    response.getEntity().getContent())) {
                FileUtil.copy(is, dest, listener);
            }
            dest.setLastModified(getLastModified(response));
        }
    }

    @Override
    public void upload(final File src, final URL dest, final CopyProgressListener l) throws IOException {
        this.upload(src, dest, l, null);
    }

    @Override
    public void upload(final File src, final URL dest, final CopyProgressListener listener, final TimeoutConstraint timeoutConstraint) throws IOException {
        final int connectionTimeout = (timeoutConstraint == null || timeoutConstraint.getConnectionTimeout() < 0) ? 0 : timeoutConstraint.getConnectionTimeout();
        final int readTimeout = (timeoutConstraint == null || timeoutConstraint.getReadTimeout() < 0) ? 0 : timeoutConstraint.getReadTimeout();
        final RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(readTimeout)
                .setConnectTimeout(connectionTimeout)
                .setAuthenticationEnabled(hasCredentialsConfigured(dest))
                .setTargetPreferredAuthSchemes(getAuthSchemePreferredOrder())
                .setProxyPreferredAuthSchemes(getAuthSchemePreferredOrder())
                .setExpectContinueEnabled(true)
                .build();
        final HttpPut put = new HttpPut(normalizeToString(dest));
        put.setConfig(requestConfig);
        put.setEntity(new FileEntity(src));
        try (final CloseableHttpResponse response = this.httpClient.execute(put)) {
            validatePutStatusCode(dest, response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase());
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
        final int connectionTimeout = (timeoutConstraint == null || timeoutConstraint.getConnectionTimeout() < 0) ? 0 : timeoutConstraint.getConnectionTimeout();
        final int readTimeout = (timeoutConstraint == null || timeoutConstraint.getReadTimeout() < 0) ? 0 : timeoutConstraint.getReadTimeout();
        CloseableHttpResponse response = null;
        try {
            final String httpMethod;
            if (getRequestMethod() == TimeoutConstrainedURLHandler.REQUEST_METHOD_HEAD) {
                httpMethod = HttpHead.METHOD_NAME;
                response = doHead(url, connectionTimeout, readTimeout);
            } else {
                httpMethod = HttpGet.METHOD_NAME;
                response = doGet(url, connectionTimeout, readTimeout);
            }
            if (checkStatusCode(httpMethod, url, response)) {
                final HttpEntity responseEntity = response.getEntity();
                final Charset charSet = ContentType.getOrDefault(responseEntity).getCharset();
                return new URLInfo(true, responseEntity == null ? 0 : responseEntity.getContentLength(),
                        getLastModified(response), charSet.name());
            }
        } catch (IOException | IllegalArgumentException e) {
            // IllegalArgumentException is thrown by HttpClient library to indicate the URL is not valid,
            // this happens for instance when trying to download a dynamic version (cfr IVY-390)
            Message.error("HttpClientHandler: " + e.getMessage() + " url=" + url);
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        return UNAVAILABLE;
    }

    private boolean checkStatusCode(final String httpMethod, final URL sourceURL, final HttpResponse response) {
        final int status = response.getStatusLine().getStatusCode();
        if (status == HttpStatus.SC_OK) {
            return true;
        }
        // IVY-1328: some servers return a 204 on a HEAD request
        if (HttpHead.METHOD_NAME.equals(httpMethod) && (status == 204)) {
            return true;
        }

        Message.debug("HTTP response status: " + status + " url=" + sourceURL);
        if (status == HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED) {
            Message.warn("Your proxy requires authentication.");
        } else if (String.valueOf(status).startsWith("4")) {
            Message.verbose("CLIENT ERROR: " + response.getStatusLine().getReasonPhrase() + " url=" + sourceURL);
        } else if (String.valueOf(status).startsWith("5")) {
            Message.error("SERVER ERROR: " + response.getStatusLine().getReasonPhrase() + " url=" + sourceURL);
        }
        return false;
    }

    /**
     * Checks the status code of the response and if it's considered as successful response, then
     * this method just returns back. Else it {@link CloseableHttpResponse#close() closes the
     * response} and throws an {@link IOException} for the unsuccessful response.
     *
     * @param httpMethod The HTTP method that was used for the source request
     * @param sourceURL  The URL of the source request
     * @param response   The response to the source request
     * @throws IOException Thrown if the response was considered unsuccessful
     */
    private void requireSuccessStatus(final String httpMethod, final URL sourceURL, final CloseableHttpResponse response) throws IOException {
        if (this.checkStatusCode(httpMethod, sourceURL, response)) {
            return;
        }
        // this is now considered an unsuccessful response, so close the response and throw an exception
        try {
            response.close();
        } catch (Exception e) {
            // log and move on
            Message.debug("Could not close the HTTP response for url=" + sourceURL, e);
        }
        throw new IOException("Failed response to request '" + httpMethod + " " + sourceURL + "' " + response.getStatusLine().getStatusCode()
                + " - '" + response.getStatusLine().getReasonPhrase());
    }

    private Header getContentEncoding(final HttpResponse response) {
        return response.getFirstHeader("Content-Encoding");
    }

    private long getLastModified(final HttpResponse response) {
        final Header header = response.getFirstHeader("last-modified");
        if (header == null) {
            return System.currentTimeMillis();
        }
        final String lastModified = header.getValue();
        try {
            return LAST_MODIFIED_FORMAT.parse(lastModified).getTime();
        } catch (ParseException e) {
            // ignored
        }
        return System.currentTimeMillis();
    }

    private CloseableHttpResponse doGet(final URL url, final int connectionTimeout, final int readTimeout) throws IOException {
        final RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(readTimeout)
                .setConnectTimeout(connectionTimeout)
                .setAuthenticationEnabled(hasCredentialsConfigured(url))
                .setTargetPreferredAuthSchemes(getAuthSchemePreferredOrder())
                .setProxyPreferredAuthSchemes(getAuthSchemePreferredOrder())
                .build();
        final HttpGet httpGet = new HttpGet(normalizeToString(url));
        httpGet.setConfig(requestConfig);
        httpGet.addHeader("Accept-Encoding", "gzip,deflate");
        return this.httpClient.execute(httpGet);
    }

    private CloseableHttpResponse doHead(final URL url, final int connectionTimeout, final int readTimeout) throws IOException {
        final RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(readTimeout)
                .setConnectTimeout(connectionTimeout)
                .setAuthenticationEnabled(hasCredentialsConfigured(url))
                .setTargetPreferredAuthSchemes(getAuthSchemePreferredOrder())
                .setProxyPreferredAuthSchemes(getAuthSchemePreferredOrder())
                .build();
        final HttpHead httpHead = new HttpHead(normalizeToString(url));
        httpHead.setConfig(requestConfig);
        return this.httpClient.execute(httpHead);
    }

    private boolean hasCredentialsConfigured(final URL url) {
        return CredentialsStore.INSTANCE.hasCredentials(url.getHost());
    }

    @Override
    public void close() throws Exception {
        if (this.httpClient != null) {
            this.httpClient.close();
        }
    }

    private static class IvyCredentialsProvider implements CredentialsProvider {

        private final ConcurrentHashMap<AuthScope, Credentials> cachedCreds = new ConcurrentHashMap<>();

        @Override
        public void setCredentials(final AuthScope authscope, final Credentials credentials) {
            if (authscope == null) {
                throw new IllegalArgumentException("AuthScope cannot be null");
            }
            this.cachedCreds.put(authscope, credentials);
        }

        @Override
        public Credentials getCredentials(final AuthScope authscope) {
            if (authscope == null) {
                return null;
            }
            final String realm = authscope.getRealm();
            final String host = authscope.getHost();
            final org.apache.ivy.util.Credentials ivyConfiguredCred = CredentialsStore.INSTANCE.getCredentials(realm, host);
            if (ivyConfiguredCred == null) {
                return null;
            }
            return createCredentials(ivyConfiguredCred.getUserName(), ivyConfiguredCred.getPasswd());
        }

        @Override
        public void clear() {
            this.cachedCreds.clear();
        }

        private static Credentials createCredentials(final String username, final String password) {
            final String user;
            final String domain;
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
    }
}
