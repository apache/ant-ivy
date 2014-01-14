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
import java.util.Date;
import java.util.Map;

import org.apache.ivy.core.module.id.ArtifactRevisionId;
import org.apache.ivy.core.module.id.ModuleRevisionId;

/**
 *
 */
public class DefaultArtifact extends AbstractArtifact {

    public static Artifact newIvyArtifact(ModuleRevisionId mrid, Date pubDate) {
        return new DefaultArtifact(mrid, pubDate, "ivy", "ivy", "xml", true);
    }

    public static Artifact newPomArtifact(ModuleRevisionId mrid, Date pubDate) {
        return new DefaultArtifact(mrid, pubDate, mrid.getName(), "pom", "pom", true);
    }

    public static Artifact cloneWithAnotherExt(Artifact artifact, String newExt) {
        return cloneWithAnotherTypeAndExt(artifact, artifact.getType(), newExt);
    }

    public static Artifact cloneWithAnotherType(Artifact artifact, String newType) {
        return cloneWithAnotherTypeAndExt(artifact, newType, artifact.getExt());
    }

    public static Artifact cloneWithAnotherTypeAndExt(Artifact artifact, String newType,
            String newExt) {
        return new DefaultArtifact(ArtifactRevisionId.newInstance(artifact.getModuleRevisionId(),
            artifact.getName(), newType, newExt, artifact.getQualifiedExtraAttributes()),
                artifact.getPublicationDate(), artifact.getUrl(), artifact.isMetadata());
    }

    public static Artifact cloneWithAnotherName(Artifact artifact, String name) {
        return new DefaultArtifact(ArtifactRevisionId.newInstance(artifact.getModuleRevisionId(),
            name, artifact.getType(), artifact.getExt(), artifact.getQualifiedExtraAttributes()),
                artifact.getPublicationDate(), artifact.getUrl(), artifact.isMetadata());
    }

    public static Artifact cloneWithAnotherMrid(Artifact artifact, ModuleRevisionId mrid) {
        return new DefaultArtifact(ArtifactRevisionId.newInstance(mrid, artifact.getName(),
            artifact.getType(), artifact.getExt(), artifact.getQualifiedExtraAttributes()),
                artifact.getPublicationDate(), artifact.getUrl(), artifact.isMetadata());
    }

    private Date publicationDate;

    private ArtifactRevisionId arid;

    private URL url;

    private boolean isMetadata = false;

    public DefaultArtifact(ModuleRevisionId mrid, Date publicationDate, String name, String type,
            String ext) {
        this(mrid, publicationDate, name, type, ext, null, null);
    }

    public DefaultArtifact(ModuleRevisionId mrid, Date publicationDate, String name, String type,
            String ext, boolean isMetadata) {
        this(mrid, publicationDate, name, type, ext, null, null);
        this.isMetadata = isMetadata;
    }

    public DefaultArtifact(ModuleRevisionId mrid, Date publicationDate, String name, String type,
            String ext, Map extraAttributes) {
        this(mrid, publicationDate, name, type, ext, null, extraAttributes);
    }

    public DefaultArtifact(ModuleRevisionId mrid, Date publicationDate, String name, String type,
            String ext, URL url, Map extraAttributes) {
        this(ArtifactRevisionId.newInstance(mrid, name, type, ext, extraAttributes),
                publicationDate, url, false);
    }

    public DefaultArtifact(ArtifactRevisionId arid, Date publicationDate, URL url,
            boolean isMetadata) {
        if (arid == null) {
            throw new NullPointerException("null arid not allowed");
        }
        if (publicationDate == null) {
            publicationDate = new Date();
        }
        this.publicationDate = publicationDate;
        this.arid = arid;
        this.url = url;
        this.isMetadata = isMetadata;
    }

    public ModuleRevisionId getModuleRevisionId() {
        return arid.getModuleRevisionId();
    }

    public String getName() {
        return arid.getName();
    }

    public Date getPublicationDate() {
        return publicationDate;
    }

    public String getType() {
        return arid.getType();
    }

    public String getExt() {
        return arid.getExt();
    }

    public ArtifactRevisionId getId() {
        return arid;
    }

    public String[] getConfigurations() {
        return new String[0];
    }

    public URL getUrl() {
        return url;
    }

    public boolean isMetadata() {
        return isMetadata;
    }
}
