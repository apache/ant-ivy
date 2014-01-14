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
package org.apache.ivy.plugins.repository;

import java.io.File;
import java.net.MalformedURLException;

import org.apache.ivy.plugins.repository.file.FileResource;
import org.apache.ivy.plugins.repository.url.URLResource;
import org.apache.ivy.util.Message;

public final class ResourceHelper {

    private ResourceHelper() {

    }

    public static boolean equals(Resource res, File f) {
        if (res == null && f == null) {
            return true;
        }
        if (res == null || f == null) {
            return false;
        }
        if (res instanceof FileResource) {
            return new File(res.getName()).equals(f);
        } else if (res instanceof URLResource) {
            try {
                return f.toURI().toURL().toExternalForm().equals(res.getName());
            } catch (MalformedURLException e) {
                return false;
            }
        }
        return false;
    }

    public static long getLastModifiedOrDefault(Resource res) {
        long last = res.getLastModified();
        if (last > 0) {
            return last;
        } else {
            Message.debug("impossible to get date for " + res + ": using 'now'");
            return System.currentTimeMillis();
        }
    }
}
