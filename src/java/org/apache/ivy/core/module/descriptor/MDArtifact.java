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
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.ivy.core.module.id.ArtifactRevisionId;
import org.apache.ivy.core.module.id.ModuleRevisionId;

/**
 *
 */
public class MDArtifact extends AbstractArtifact {

    public static Artifact newIvyArtifact(ModuleDescriptor md) {
        return new MDArtifact(md, "ivy", "ivy", "xml", true);
    }

    private ModuleDescriptor md;

    private String name;

    private String type;

    private String ext;

    private List/* <String> */confs = new ArrayList();

    private Map extraAttributes = null;

    private URL url;

    private boolean isMetadata = false;

    public MDArtifact(ModuleDescriptor md, String name, String type, String ext) {
        this(md, name, type, ext, null, null);
    }

    public MDArtifact(ModuleDescriptor md, String name, String type, String ext, boolean isMetadata) {
        this(md, name, type, ext, null, null);
        this.isMetadata = isMetadata;
    }

    public MDArtifact(ModuleDescriptor md, String name, String type, String ext, URL url,
            Map extraAttributes) {
        if (md == null) {
            throw new NullPointerException("null module descriptor not allowed");
        }
        if (name == null) {
            throw new NullPointerException("null name not allowed");
        }
        if (type == null) {
            throw new NullPointerException("null type not allowed");
        }
        if (ext == null) {
            throw new NullPointerException("null ext not allowed");
        }
        this.md = md;
        this.name = name;
        this.type = type;
        this.ext = ext;
        this.url = url;
        this.extraAttributes = extraAttributes;
    }

    public ModuleRevisionId getModuleRevisionId() {
        return md.getResolvedModuleRevisionId();
    }

    public Date getPublicationDate() {
        return md.getResolvedPublicationDate();
    }

    public ArtifactRevisionId getId() {
        // do not cache the result because the resolvedModuleRevisionId can change!
        return ArtifactRevisionId.newInstance(md.getResolvedModuleRevisionId(), name, type, ext,
            extraAttributes);
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

    public void addConfiguration(String conf) {
        confs.add(conf);
    }

    public URL getUrl() {
        return url;
    }

    public boolean isMetadata() {
        return isMetadata;
    }
}
