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
package fr.jayasoft.ivy;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import fr.jayasoft.ivy.util.Message;

public class ResolvedURL implements ArtifactInfo {
    URL url;
    String revision;
    private Long _lastModified;
    
    public ResolvedURL(URL url, String revision) {
        this.url = url;
        this.revision = revision;
    }
    
    public String toString() {
        return url + "(" + revision + ")";
    }

    public String getRevision() {
        return revision;
    }

    public long getLastModified() {
        if (_lastModified == null) {
            URLConnection con = null;
            try {
                con = url.openConnection();
                _lastModified = new Long(con.getLastModified());
            } catch (IOException e) {
                Message.warn("impossible to open connection to "+url+": "+e.getMessage());
                _lastModified = new Long(0);
            } finally {
                if (con instanceof HttpURLConnection) {
                    ((HttpURLConnection)con).disconnect();
                }
            }
        }
        return _lastModified.longValue();
    }

    public URL getURL() {
        return url;
    }
}
