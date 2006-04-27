/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy;

import java.util.Map;

/**
 * @author Hanin
 *
 */
public abstract class AbstractArtifact implements Artifact {
    public AbstractArtifact() {
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Artifact)) {
            return false;
        }
        Artifact art = (Artifact)obj;
        return getModuleRevisionId().equals(art.getModuleRevisionId())
        	&& getPublicationDate()==null?true:getPublicationDate().equals(art.getPublicationDate())
            && getName().equals(art.getName())
            && getExt().equals(art.getExt())
        	&& getType().equals(art.getType())
            && getExtraAttributes().equals(art.getExtraAttributes());
    }
    
    public int hashCode() {
        int hash = 33;
        hash = hash * 17 + getModuleRevisionId().hashCode();
        if (getPublicationDate() != null) {
            hash = hash * 17 + getPublicationDate().hashCode();
        }
        hash = hash * 17 + getName().hashCode();
        hash = hash * 17 + getExt().hashCode();
        hash = hash * 17 + getType().hashCode();
        hash = hash * 17 + getExtraAttributes().hashCode();
        return hash;
    }
    
    public String toString() {
        return getModuleRevisionId()+"/"+getName()+"."+getExt()+"["+getType()+"]";
    }

    public String getAttribute(String attName) {
        return getId().getAttribute(attName);
    }

    public Map getAttributes() {
        return getId().getAttributes();
    }

    public String getExtraAttribute(String attName) {
        return getId().getExtraAttribute(attName);
    }

    public Map getExtraAttributes() {
        return getId().getExtraAttributes();
    }

    public String getStandardAttribute(String attName) {
        return getId().getStandardAttribute(attName);
    }

    public Map getStandardAttributes() {
        return getId().getStandardAttributes();
    }
    
    
}
