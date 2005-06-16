/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy;

/**
 * Identifies an artifact in a module, without revision information
 */
public class ArtifactId {
    private ModuleId _mid;
    private String _name;
    private String _type;
    private String _ext;
    
    
    /**
     * @param mid
     * @param name
     * @param type
     */
    public ArtifactId(ModuleId mid, String name, String type, String ext) {
        _mid = mid;
        _name = name;
        _type = type;
        _ext = ext;
    }
    
    public boolean equals(Object obj) {
        if (! (obj instanceof ArtifactId)) {
            return false;
        }
        ArtifactId aid = (ArtifactId)obj;
        return getModuleId().equals(aid.getModuleId()) 
            && getName().equals(aid.getName()) 
            && getExt().equals(aid.getExt()) 
            && getType().equals(aid.getType());
    }
    
    public int hashCode() {
        int hash = 17;
        hash += getModuleId().hashCode() * 37;
        hash += getName().hashCode() * 37;
        hash += getType().hashCode() * 37;
        return hash;
    }
    
    public String toString() {
        return getModuleId()+" "+getName()+"."+getType();
    }
    
    /**
     * @return Returns the module id.
     */
    public ModuleId getModuleId() {
        return _mid;
    }
    /**
     * @return Returns the name.
     */
    public String getName() {
        return _name;
    }
    /**
     * @return Returns the type.
     */
    public String getType() {
        return _type;
    }
    /**
     * @return Returns the ext.
     */
    public String getExt() {
        return _ext;
    }
}
