/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy;

/**
 * identifies an artifact in a particular module revision
 */
public class ArtifactRevisionId {
    public static ArtifactRevisionId newInstance(ModuleRevisionId mrid, String name, String type, String ext) {
        return new ArtifactRevisionId(new ArtifactId(mrid.getModuleId(), name, type, ext), mrid);
    }
    
    private ArtifactId _artifactId;
    private ModuleRevisionId _mrid;
    
    
    /**
     * @param revision
     * @param artifactId
     */
    public ArtifactRevisionId(ArtifactId artifactId, ModuleRevisionId mrid) {
        _artifactId = artifactId;
        _mrid = mrid;
    }
    
    public boolean equals(Object obj) {
        if (! (obj instanceof ArtifactRevisionId)) {
            return false;
        }
        ArtifactRevisionId arid = (ArtifactRevisionId)obj;
        return getArtifactId().equals(arid.getArtifactId()) 
            && getModuleRevisionId().equals(arid.getModuleRevisionId());
    }
    
    public int hashCode() {
        int hash = 17;
        hash += getArtifactId().hashCode() * 37;
        hash += getModuleRevisionId().hashCode() * 37;
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
