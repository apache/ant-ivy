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
package org.apache.ivy.plugins.resolver.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileURLLister implements URLLister {
    private File basedir;

    public FileURLLister() {
        this(null);
    }

    public FileURLLister(File baseDir) {
        this.basedir = baseDir;
    }

    public boolean accept(String pattern) {
        return pattern.startsWith("file");
    }

    public List<URL> listAll(URL url) throws IOException {
        String path;
        try {
            path = new File(new URI(url.toExternalForm())).getPath();
        } catch (URISyntaxException e) {
            // unexpected try to get the best of it
            path = url.getPath();
        }
        File file = basedir == null ? new File(path) : new File(basedir, path);
        if (file.exists() && file.isDirectory()) {
            String[] files = file.list();
            List<URL> ret = new ArrayList<URL>(files.length);
            URL context = url.getPath().endsWith("/") ? url : new URL(url.toExternalForm() + "/");
            for (int i = 0; i < files.length; i++) {
                ret.add(new URL(context, files[i]));
            }
            return ret;
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public String toString() {
        return "file lister";
    }
}
