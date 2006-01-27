/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy;


/**
 * @author x.hanin
 *
 */
public class ModuleRevisionId {
    private static final String ENCODE_SEPARATOR = ModuleId.ENCODE_SEPARATOR;
    
    public static ModuleRevisionId newInstance(String organisation, String name, String revision) {
        return new ModuleRevisionId(new ModuleId(organisation, name), revision);
    }
    
    private ModuleId _moduleId;
    private String _revision;
    private int _hash;
    
    public ModuleRevisionId(ModuleId moduleId, String revision) {
        _moduleId = moduleId;
        _revision = revision;
        _hash = _hashCode(); //stored for performance reasons, has code is very used in many maps
    }
    public ModuleId getModuleId() {
        return _moduleId;
    }
    public String getName() {
        return getModuleId().getName();
    }
    public String getOrganisation() {
        return getModuleId().getOrganisation();
    }
    public String getRevision() {
        return _revision;
    }
    
    public boolean equals(Object obj) {
        if (! (obj instanceof ModuleRevisionId)) {
            return false;
        }
        ModuleRevisionId other = (ModuleRevisionId)obj;
        return (other.getRevision() == null ? getRevision() == null : other.getRevision().equals(getRevision())) 
        	&& other.getModuleId().equals(getModuleId());
    }
    public int hashCode() {
        return _hash;
    }
    public int _hashCode() {
        int hash = 31;
        hash = hash * 13 + (getRevision() == null ? 0 : getRevision().hashCode());
        hash = hash * 13 + getModuleId().hashCode();
        return hash;
    }
    
    public String toString() {
        return "[ "+_moduleId.getOrganisation()+" | "+_moduleId.getName()+" | "+_revision+" ]";
    }
    /**
     * Returns true if the given revision can be considered as a revision of this module revision id.
     * This is the case if the revision is equal to the current revision, or if the
     * current revision is a 'latest.' one, or if it is a xx+ one matching the given one.
     * @param revision
     * @return true if the given revision can be considered as a revision of this module revision id.
     */
    public boolean acceptRevision(String revision) {
        return acceptRevision(_revision, revision);
    }
    /**
     * @return true if the revision is an exact one, i.e. not a 'latest.' nor a xx+ one.
     */
    public boolean isExactRevision() {
        return isExactRevision(_revision);
    }
    
    public static boolean acceptRevision(String askedRevision, String revision) {
        if (askedRevision.equals(revision)) {
            return true;
        }
        if (askedRevision.startsWith("latest.")) {
            return true;
        }
        if (askedRevision.endsWith("+") && revision.startsWith(askedRevision.substring(0, askedRevision.length() - 1))) {
            return true;
        }
        return false;        
    }

    /**
     * @return true if the revision is an exact one, i.e. not a 'latest.' nor a xx+ one.
     */
    public static boolean isExactRevision(String revision) {
        return !revision.startsWith("latest.") && !revision.endsWith("+");
    }
    public String encodeToString() {
        return getOrganisation() + ENCODE_SEPARATOR+getName()+ ENCODE_SEPARATOR+getRevision();
    }
    public static ModuleRevisionId decode(String encoded) {
        String[] parts = encoded.split(ENCODE_SEPARATOR);
        if (parts.length != 3) {
            throw new IllegalArgumentException("badly encoded module revision id: '"+encoded+"'");
        }
        return newInstance(parts[0], parts[1], parts[2]);
    }
}
