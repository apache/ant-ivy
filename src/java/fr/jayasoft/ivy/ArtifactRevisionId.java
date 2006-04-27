/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy;

import java.util.Map;

import fr.jayasoft.ivy.extendable.UnmodifiableExtendableItem;
import fr.jayasoft.ivy.util.IvyPatternHelper;

/**
 * identifies an artifact in a particular module revision
 */
public class ArtifactRevisionId extends UnmodifiableExtendableItem {
    public static ArtifactRevisionId newInstance(ModuleRevisionId mrid, String name, String type, String ext) {
        return newInstance(mrid, name, type, ext, null);
    }
    
    public static ArtifactRevisionId newInstance(ModuleRevisionId mrid, String name, String type, String ext, Map extraAttributes) {
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
