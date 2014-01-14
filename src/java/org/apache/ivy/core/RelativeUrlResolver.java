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
package org.apache.ivy.core;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Resolve an file or url relatively to its context.
 */
public abstract class RelativeUrlResolver {

    /**
     * Resolve the url in the context of context.
     * 
     * @param context
     *            The URL of the ressource containing the reference url
     * @param url
     *            a relative or absolution url string
     * @throws MalformedURLException
     */
    public abstract URL getURL(URL context, String url) throws MalformedURLException;

    /**
     * Relsovle file or url path relatively to a context. file is considered first. If file is not
     * defined, url will be considered.
     * 
     * @param context
     *            The URL of the ressource containing the reference file or url
     * @param file
     *            a relative or absolute path
     * @param url
     *            a relative or absolution url string
     * @return the resulting url or null if faile and url are null.
     * @throws MalformedURLException
     */
    public URL getURL(URL context, String file, String url) throws MalformedURLException {
        if (file != null) {
            File f = new File(file);
            if (f.isAbsolute()) {
                return f.toURI().toURL();
            } else {
                return getURL(context, file);
            }
        } else if (url != null) {
            return getURL(context, url);
        } else {
            return null;
        }
    }
}
