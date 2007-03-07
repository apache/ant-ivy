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
package org.apache.ivy.core.module.id;

import java.util.HashMap;
import java.util.Map;

import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.util.extendable.UnmodifiableExtendableItem;


/**
 * identifies an artifact in a particular module revision
 */
public class ArtifactRevisionId extends UnmodifiableExtendableItem {
    public static ArtifactRevisionId newInstance(ModuleRevisionId mrid, String name, String type, String ext) {
        return newInstance(mrid, name, type, ext, null);
    }
    
    public static ArtifactRevisionId newInstance(ModuleRevisionId mrid, String name, String type, String ext, Map extraAttributes) {
    	// we inject module extra attributes as extra attributes for the artifacts too
    	if (extraAttributes == null) {
    		extraAttributes = mrid.getExtraAttributes();
    	} else {
    		extraAttributes = new HashMap(extraAttributes);
    		extraAttributes.putAll(mrid.getExtraAttributes());
    	}
        return new ArtifactRevisionId(new ArtifactId(mrid.getModuleId(), name, type, ext), mrid, extraAttributes);
    }
    
    private ArtifactId _artifactId;
    private ModuleRevisionId _mrid;
    
    
    public ArtifactRevisionId(ArtifactId artifactId, ModuleRevisionId mrid) {
        this(artifactId, mrid, null);
    }
    public ArtifactRevisionId(ArtifactId artifactId, ModuleRevisionId mrid, Map extraAttributes) {
        super(null, extraAttributes);
        _artifactId = artifactId;
        _mrid = mrid;
        
        setStandardAttribute(IvyPatternHelper.ORGANISATION_KEY, getModuleRevisionId().getOrganisation());
        setStandardAttribute(IvyPatternHelper.MODULE_KEY, getModuleRevisionId().getName());
        setStandardAttribute(IvyPatternHelper.REVISION_KEY, getModuleRevisionId().getRevision());
        setStandardAttribute(IvyPatternHelper.ARTIFACT_KEY, getName());
        setStandardAttribute(IvyPatternHelper.TYPE_KEY, getType());
        setStandardAttribute(IvyPatternHelper.EXT_KEY, getExt());
    }
    
    public boolean equals(Object obj) {
        if (! (obj instanceof ArtifactRevisionId)) {
            return false;
        }
        ArtifactRevisionId arid = (ArtifactRevisionId)obj;
        return getArtifactId().equals(arid.getArtifactId()) 
            && getModuleRevisionId().equals(arid.getModuleRevisionId())
            && getExtraAttributes().equals(arid.getExtraAttributes());
    }
    
    public int hashCode() {
        int hash = 17;
        hash += getArtifactId().hashCode() * 37;
        hash += getModuleRevisionId().hashCode() * 37;
        hash += getExtraAttributes().hashCode() * 37;
        return hash;
    }
    
    public String toString() {
        return "[ "+getModuleRevisionId().getOrganisation()+" | "+ getModuleRevisionId().getName()+" | "+getModuleRevisionId().getRevision()+" :: "+getName()+" . "+getExt()+" ( "+getType()+" ) ]";
    }
    
    /**
     * @return Returns the artifactId.
     */
    public ArtifactId getArtifactId() {
        return _artifactId;
    }
    
    public ModuleRevisionId getModuleRevisionId() {
        return _mrid;
    }

    public String getName() {
        return _artifactId.getName();
    }
    
    public String getType() {
        return _artifactId.getType();
    }
    
    public String getExt() {
        return _artifactId.getExt();
    }
    
    /**
     * @return Returns the revision.
     */
    public String getRevision() {
        return _mrid.getRevision();
    }
    
}
