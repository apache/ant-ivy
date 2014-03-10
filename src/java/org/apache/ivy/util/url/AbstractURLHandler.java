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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import org.apache.ivy.Ivy;

public abstract class AbstractURLHandler implements URLHandler {

    private static final Pattern ESCAPE_PATTERN = Pattern.compile("%25([0-9a-fA-F][0-9a-fA-F])");

    // the request method to use. TODO: don't use a static here
    private static int requestMethod = REQUEST_METHOD_HEAD;

    public boolean isReachable(URL url) {
        return getURLInfo(url).isReachable();
    }

    public boolean isReachable(URL url, int timeout) {
        return getURLInfo(url, timeout).isReachable();
    }

    public long getContentLength(URL url) {
        return getURLInfo(url).getContentLength();
    }

    public long getContentLength(URL url, int timeout) {
        return getURLInfo(url, timeout).getContentLength();
    }

    public long getLastModified(URL url) {
        return getURLInfo(url).getLastModified();
    }

    public long getLastModified(URL url, int timeout) {
        return getURLInfo(url, timeout).getLastModified();
    }

    protected String getUserAgent() {
        String userAgent = System.getProperty("http.agent");
        if (userAgent == null) {
            userAgent = "Apache Ivy/" + Ivy.getIvyVersion();
        }
        return userAgent;
    }

    protected void validatePutStatusCode(URL dest, int statusCode, String statusMessage)
            throws IOException {
        switch (statusCode) {
            case HttpURLConnection.HTTP_OK:
                /* intentional fallthrough */
            case HttpURLConnection.HTTP_CREATED:
                /* intentional fallthrough */
            case HttpURLConnection.HTTP_ACCEPTED:
                /* intentional fallthrough */
            case HttpURLConnection.HTTP_NO_CONTENT:
                break;
            case HttpURLConnection.HTTP_UNAUTHORIZED:
                /* intentional fallthrough */
            case HttpURLConnection.HTTP_FORBIDDEN:
                throw new IOException("Access to URL " + dest + " was refused by the server"
                        + (statusMessage == null ? "" : ": " + statusMessage));
            default:
                throw new IOException("PUT operation to URL " + dest + " failed with status code "
                        + statusCode + (statusMessage == null ? "" : ": " + statusMessage));
        }
    }

    public void setRequestMethod(int requestMethod) {
        AbstractURLHandler.requestMethod = requestMethod;
    }

    public int getRequestMethod() {
        return requestMethod;
    }

    protected String normalizeToString(URL url) throws IOException {
        if (!"http".equals(url.getProtocol()) && !"https".equals(url.getProtocol())) {
            return url.toExternalForm();
        }

        try {
            URI uri = new URI(url.getProtocol(), url.getAuthority(), url.getPath(), url.getQuery(),
                    url.getRef());

            // it is possible that the original url was already (partial) escaped,
            // so we must unescape all '%' followed by 2 hexadecimals...
            String uriString = uri.normalize().toASCIIString();

            // manually escape the '+' character
            uriString = uriString.replaceAll("\\+", "%2B");

            return ESCAPE_PATTERN.matcher(uriString).replaceAll("%$1");
        } catch (URISyntaxException e) {
            IOException ioe = new MalformedURLException("Couldn't convert '" + url.toString()
                    + "' to a valid URI");
            ioe.initCause(e);
            throw ioe;
        }
    }

    protected URL normalizeToURL(URL url) throws IOException {
        if (!"http".equals(url.getProtocol()) && !"https".equals(url.getProtocol())) {
            return url;
        }

        return new URL(normalizeToString(url));
    }

    protected InputStream getDecodingInputStream(String encoding, InputStream in)
            throws IOException {
        InputStream result = null;

        if ("gzip".equals(encoding) || "x-gzip".equals(encoding)) {
            result = new GZIPInputStream(in);
        } else if ("deflate".equals(encoding)) {
            // There seems to be 2 variants of the "deflate"-encoding.
            // I couldn't find a way to auto-detect which variant was
            // used, so as (a not really good) work-around we try do
            // decompress the first 100 bytes using the "zlib"-variant.
            BufferedInputStream bStream = new BufferedInputStream(in);
            bStream.mark(100);
            byte[] bytes = new byte[100];
            int nbBytes = bStream.read(bytes);
            bStream.reset();

            Inflater inflater = new Inflater();
            inflater.setInput(bytes, 0, nbBytes);
            try {
                inflater.inflate(new byte[1000]);

                // no error decompressing the first 100 bytes, so we
                // assume the "zlib"-variant was used.
                result = new InflaterInputStream(bStream);
            } catch (DataFormatException e) {
                // there was an error decompressing the first 100 bytes,
                // so we assume the "gzip/raw"-variant was used.
                result = new InflaterInputStream(bStream, new Inflater(true));
            } finally {
                inflater.end();
            }
        } else {
            result = in;
        }

        return result;
    }

}
