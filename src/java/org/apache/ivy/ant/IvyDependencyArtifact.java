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
package org.apache.ivy.ant;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.ivy.core.module.descriptor.DefaultDependencyArtifactDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.tools.ant.BuildException;

public class IvyDependencyArtifact {

    private String name;

    private String type;

    private String ext;

    private String url;

    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    void addArtifact(DefaultDependencyDescriptor dd, String masterConf) {
        String typePattern = type == null ? PatternMatcher.ANY_EXPRESSION : type;
        String extPattern = ext == null ? typePattern : ext;
        URL u;
        try {
            u = url == null ? null : new URL(url);
        } catch (MalformedURLException e) {
            throw new BuildException("Malformed url in the artifact: " + e.getMessage(), e);
        }
        DefaultDependencyArtifactDescriptor dad = new DefaultDependencyArtifactDescriptor(dd, name,
                typePattern, extPattern, u, null);
        dd.addDependencyArtifact(masterConf, dad);
    }
}
