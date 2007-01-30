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
 * @author Hanin
 *
 */
public class DefaultArtifact extends AbstractArtifact {

    public static Artifact newIvyArtifact(ModuleRevisionId mrid, Date pubDate) {
        return new DefaultArtifact(mrid, pubDate, "ivy", "ivy", "xml");
    }
    
    public static Artifact newPomArtifact(ModuleRevisionId mrid, Date pubDate) {
        return new DefaultArtifact(mrid, pubDate, mrid.getName(), "pom", "pom");
    }
    
    public static Artifact cloneWithAnotherType(Artifact artifact, String newType) {
        return new DefaultArtifact(artifact.getModuleRevisionId(), artifact.getPublicationDate(), artifact.getName(), newType, artifact.getExt(), artifact.getUrl(), artifact.getExtraAttributes());
    }
    
    public static Artifact cloneWithAnotherTypeAndExt(Artifact artifact, String newType, String newExt) {
        return new DefaultArtifact(artifact.getModuleRevisionId(), artifact.getPublicationDate(), artifact.getName(), newType, newExt, artifact.getUrl(), artifact.getExtraAttributes());
    }

	public static Artifact cloneWithAnotherMrid(Artifact artifact, ModuleRevisionId mrid) {
        return new DefaultArtifact(mrid, artifact.getPublicationDate(), artifact.getName(), artifact.getType(), artifact.getExt(), artifact.getUrl(), artifact.getExtraAttributes());
	}
    
    Date _publicationDate;
    ArtifactRevisionId _arid;
    URL _url;
    
    public DefaultArtifact(ModuleRevisionId mrid, Date publicationDate, String name, String type, String ext) {
        this(mrid, publicationDate, name, type, ext, null, null);
    }
    
    public DefaultArtifact(ModuleRevisionId mrid, Date publicationDate, String name, String type, String ext, Map extraAttributes) {
    	this(mrid, publicationDate, name, type, ext, null, extraAttributes);
    }
    public DefaultArtifact(ModuleRevisionId mrid, Date publicationDate, String name, String type, String ext, URL url, Map extraAttributes) {
        if (mrid == null) {
            throw new NullPointerException("null mrid not allowed");
        }
        if (publicationDate == null) {
            publicationDate = new Date();
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
        _publicationDate = publicationDate;
        _arid = ArtifactRevisionId.newInstance(mrid, name, type, ext, extraAttributes);
        _url = url;
    }


	public ModuleRevisionId getModuleRevisionId() {
        return _arid.getModuleRevisionId();
    }
    public String getName() {
        return _arid.getName();
    }
    public Date getPublicationDate() {
        return _publicationDate;
    }
    public String getType() {
        return _arid.getType();
    }
    public String getExt() {
        return _arid.getExt();
    }
    public ArtifactRevisionId getId() {
        return _arid;
    }

    public String[] getConfigurations() {
        return new String[0];
    }

	public URL getUrl() {
		return _url;
	}

}
