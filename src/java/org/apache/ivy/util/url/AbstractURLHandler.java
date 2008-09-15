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

import java.net.URL;
import java.net.HttpURLConnection;
import java.io.IOException;

public abstract class AbstractURLHandler implements URLHandler {
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

    protected void validatePutStatusCode(
            URL dest, int statusCode, String statusMessage) throws IOException {
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
}
