/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import fr.jayasoft.ivy.extendable.UnmodifiableExtendableItem;
import fr.jayasoft.ivy.util.IvyPatternHelper;


/**
 * @author x.hanin
 *
 */
public class ModuleRevisionId extends UnmodifiableExtendableItem {
    private static final String ENCODE_SEPARATOR = ModuleId.ENCODE_SEPARATOR;
    private static final String ENCODE_PREFIX = "+";
    private static final String NULL_ENCODE = "@#:NULL:#@";
    
    public static ModuleRevisionId newInstance(String organisation, String name, String revision) {
        return new ModuleRevisionId(new ModuleId(organisation, name), revision);
    }
    public static ModuleRevisionId newInstance(String organisation, String name, String revision, Map extraAttributes) {
        return new ModuleRevisionId(new ModuleId(organisation, name), revision, extraAttributes);
    }
    public static ModuleRevisionId newInstance(String organisation, String name, String branch, String revision) {
        return new ModuleRevisionId(new ModuleId(organisation, name), branch, revision);
    }
    public static ModuleRevisionId newInstance(String organisation, String name, String branch, String revision, Map extraAttributes) {
        return new ModuleRevisionId(new ModuleId(organisation, name), branch, revision, extraAttributes);
    }
	public static ModuleRevisionId newInstance(ModuleRevisionId mrid, String rev) {
		return new ModuleRevisionId(mrid.getModuleId(), mrid.getBranch(), rev, mrid.getExtraAttributes());
	}
    
    private ModuleId _moduleId;
    private String _branch;
    private String _revision;
    private int _hash;
    
    public ModuleRevisionId(ModuleId moduleId, String revision) {
        this(moduleId, null, revision, null);
    }
    public ModuleRevisionId(ModuleId moduleId, String branch, String revision) {
        this(moduleId, branch, revision, null);
    }
    public ModuleRevisionId(ModuleId moduleId, String revision, Map extraAttributes) {
    	this(moduleId, null, revision, extraAttributes);
    }
    public ModuleRevisionId(ModuleId moduleId, String branch, String revision, Map extraAttributes) {
        super(null, extraAttributes);
        _moduleId = moduleId;
        _branch = branch == null ? IvyContext.getContext().getIvy().getDefaultBranch(moduleId) : branch;
        _revision = revision;
        _hash = _hashCode(); //stored for performance reasons, hashCode is very used in many maps
        setStandardAttribute(IvyPatternHelper.ORGANISATION_KEY, _moduleId.getOrganisation());
        setStandardAttribute(IvyPatternHelper.MODULE_KEY, _moduleId.getName());
        setStandardAttribute(IvyPatternHelper.BRANCH_KEY, _branch);
        setStandardAttribute(IvyPatternHelper.REVISION_KEY, _revision);
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
        	&& (other.getBranch() == null ? getBranch() == null : other.getBranch().equals(getBranch()))
        	&& other.getModuleId().equals(getModuleId())
            && other.getExtraAttributes().equals(getExtraAttributes());
    }
    public int hashCode() {
        return _hash;
    }
    public int _hashCode() {
        int hash = 31;
        hash = hash * 13 + (getBranch() == null ? 0 : getBranch().hashCode());
        hash = hash * 13 + (getRevision() == null ? 0 : getRevision().hashCode());
        hash = hash * 13 + getModuleId().hashCode();
        hash = hash * 13 + getAttributes().hashCode();
        return hash;
    }
    
    public String toString() {
        return "[ "+_moduleId.getOrganisation()+" | "+_moduleId.getName()+(_branch == null || _branch.length() == 0 ?"":" | "+_branch)+" | "+(_revision == null?"NONE":_revision)+" ]";
    }
    
    public String encodeToString() {
        StringBuffer buf = new StringBuffer();
        Map attributes = getAttributes();
        for (Iterator iter = attributes.keySet().iterator(); iter.hasNext();) {
            String attName = (String)iter.next();
            String value = (String) attributes.get(attName);
            value = value == null ? NULL_ENCODE : value;
            buf.append(ENCODE_PREFIX).append(attName).append(ENCODE_SEPARATOR).append(ENCODE_PREFIX).append(value).append(ENCODE_SEPARATOR);
        }
        return buf.toString();
    }
    
    public static ModuleRevisionId decode(String encoded) {
        String[] parts = encoded.split(ENCODE_SEPARATOR);
        if (parts.length % 2 != 0) {
            throw new IllegalArgumentException("badly encoded module revision id: '"+encoded+"'");
        }
        Map attributes = new HashMap();
        for (int i = 0; i < parts.length; i+=2) {
            String attName = parts[i];
            if (!attName.startsWith(ENCODE_PREFIX)) {
                throw new IllegalArgumentException("badly encoded module revision id: '"+encoded+"': "+attName+" doesn't start with "+ENCODE_PREFIX);
            } else {
                attName = attName.substring(1);
            }
            String attValue = parts[i+1];
            if (!attValue.startsWith(ENCODE_PREFIX)) {
                throw new IllegalArgumentException("badly encoded module revision id: '"+encoded+"': "+attValue+" doesn't start with "+ENCODE_PREFIX);
            } else {
                attValue = attValue.substring(1);
            }
            if (NULL_ENCODE.equals(attValue)) {
            	attValue = null;
            }
            attributes.put(attName, attValue);
        }
        String org = (String)attributes.remove(IvyPatternHelper.ORGANISATION_KEY);
        String mod = (String)attributes.remove(IvyPatternHelper.MODULE_KEY);
        String rev = (String)attributes.remove(IvyPatternHelper.REVISION_KEY);
        String branch = (String)attributes.remove(IvyPatternHelper.BRANCH_KEY);
        if (org == null) {
            throw new IllegalArgumentException("badly encoded module revision id: '"+encoded+"': no organisation");
        }
        if (mod == null) {
            throw new IllegalArgumentException("badly encoded module revision id: '"+encoded+"': no module name");
        }
        if (rev == null) {
            throw new IllegalArgumentException("badly encoded module revision id: '"+encoded+"': no revision");
        }
        return newInstance(org, mod, branch, rev, attributes);
    }
	public String getBranch() {
		return _branch;
	}
}
