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
package org.apache.ivy.core.module.descriptor;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.util.Checks;
import org.apache.ivy.util.extendable.UnmodifiableExtendableItem;

public class DefaultDependencyArtifactDescriptor extends UnmodifiableExtendableItem implements
        DependencyArtifactDescriptor, ConfigurationAware {

    private Collection confs = new ArrayList();

    private URL url;

    private String name;

    private String type;

    private String ext;

    private DependencyDescriptor dd;

    /**
     * @param dd
     * @param name
     * @param type
     * @param url
     */
    public DefaultDependencyArtifactDescriptor(DependencyDescriptor dd, String name, String type,
            String ext, URL url, Map extraAttributes) {
        super(null, extraAttributes);
        Checks.checkNotNull(dd, "dd");
        Checks.checkNotNull(name, "name");
        Checks.checkNotNull(type, "type");
        Checks.checkNotNull(ext, "ext");
        this.dd = dd;
        this.name = name;
        this.type = type;
        this.ext = ext;
        this.url = url;
        initStandardAttributes();
    }

    private void initStandardAttributes() {
        setStandardAttribute(IvyPatternHelper.ARTIFACT_KEY, getName());
        setStandardAttribute(IvyPatternHelper.TYPE_KEY, getType());
        setStandardAttribute(IvyPatternHelper.EXT_KEY, getExt());
        setStandardAttribute("url", url != null ? String.valueOf(url) : "");
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof DependencyArtifactDescriptor)) {
            return false;
        }
        DependencyArtifactDescriptor dad = (DependencyArtifactDescriptor) obj;
        return getAttributes().equals(dad.getAttributes());
    }

    public int hashCode() {
        return getAttributes().hashCode();
    }

    /**
     * Add a configuration for this artifact.
     * 
     * @param conf
     */
    public void addConfiguration(String conf) {
        confs.add(conf);
    }

    public DependencyDescriptor getDependencyDescriptor() {
        return dd;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getExt() {
        return ext;
    }

    public String[] getConfigurations() {
        return (String[]) confs.toArray(new String[confs.size()]);
    }

    public URL getUrl() {
        return url;
    }

    public String toString() {
        return "DA:" + name + "." + ext + "(" + type + ") " + "(" + confs + ")"
                + (url == null ? "" : url.toString());
    }
}
