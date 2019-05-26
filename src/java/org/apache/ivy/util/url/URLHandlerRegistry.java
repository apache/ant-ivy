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

import org.apache.ivy.util.Message;

import java.lang.reflect.Field;

/**
 *
 */
public final class URLHandlerRegistry {
    private URLHandlerRegistry() {
    }

    @SuppressWarnings("deprecation")
    private static URLHandler defaultHandler = new BasicURLHandler();

    @SuppressWarnings("deprecation")
    public static URLHandler getDefault() {
        return defaultHandler;
    }

    @SuppressWarnings("deprecation")
    public static void setDefault(final URLHandler def) {
        defaultHandler = def;
    }

    /**
     * This method is used to get appropriate http downloader depending on HttpComponents
     * HttpClient availability in classpath, or simply use jdk url handling in other cases.
     *
     * @return most accurate http downloader
     */
    public static TimeoutConstrainedURLHandler getHttp() {
        try {
            // check for the presence of HttpComponents HttpClient
            Class.forName("org.apache.http.client.HttpClient");
            // load our (wrapper) http handler
            final Class<?> handler = Class.forName("org.apache.ivy.util.url.HttpClientHandler");
            // we always use just one instance which is internally registered to be closed
            // when the JVM exits
            final Field instance = handler.getDeclaredField("DELETE_ON_EXIT_INSTANCE");
            return (TimeoutConstrainedURLHandler) instance.get(null);
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            Message.verbose("Using JDK backed URL handler for HTTP interaction since the "
                    + "Apache HttpComponents HttpClient backed handler couldn't be created due to: "
                    + e.getMessage());
            return new BasicURLHandler();
        }
    }

}
