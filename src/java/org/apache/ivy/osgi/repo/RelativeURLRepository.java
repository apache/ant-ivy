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
package org.apache.ivy.osgi.repo;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.plugins.repository.url.URLRepository;
import org.apache.ivy.plugins.repository.url.URLResource;

public class RelativeURLRepository extends URLRepository {

    private final URL baseUrl;

    public RelativeURLRepository() {
        super();
        baseUrl = null;
    }

    public RelativeURLRepository(URL baseUrl) {
        super();
        this.baseUrl = baseUrl;
    }

    private Map<String, Resource> resourcesCache = new HashMap<String, Resource>();

    public Resource getResource(String source) throws IOException {
        source = encode(source);
        Resource res = resourcesCache.get(source);
        if (res == null) {
            URI uri;
            try {
                uri = new URI(source);
            } catch (URISyntaxException e) {
                // very wierd URL, let's assume it is absolute
                uri = null;
            }
            if (uri == null || uri.isAbsolute()) {
                res = new URLResource(new URL(source));
            } else {
                res = new URLResource(new URL(baseUrl + source));
            }
            resourcesCache.put(source, res);
        }
        return res;
    }

    private static String encode(String source) {
        // TODO: add some more URL encodings here
        return source.trim().replaceAll(" ", "%20");
    }

}
