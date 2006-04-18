/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import fr.jayasoft.ivy.matcher.MatcherHelper;
import fr.jayasoft.ivy.matcher.PatternMatcher;
import fr.jayasoft.ivy.namespace.NameSpaceHelper;
import fr.jayasoft.ivy.namespace.Namespace;
import fr.jayasoft.ivy.namespace.NamespaceTransformer;
import fr.jayasoft.ivy.util.Message;

/**
 * @author X.Hanin
 *
 */
public class DefaultModuleDescriptor implements ModuleDescriptor {
    
    public static DefaultModuleDescriptor newDefaultInstance(ModuleRevisionId mrid) {
        return newDefaultInstance(mrid, null);
    }
    
    public static DefaultModuleDescriptor newDefaultInstance(ModuleRevisionId mrid, DependencyArtifactDescriptor[] artifacts) {
        DefaultModuleDescriptor moduleDescriptor = new DefaultModuleDescriptor(mrid, "release", null, true);
        moduleDescriptor.addConfiguration(new Configuration(DEFAULT_CONFIGURATION));
        if (artifacts != null && artifacts.length > 0) {
            for (int i = 0; i < artifacts.length; i++) {
                moduleDescriptor.addArtifact(DEFAULT_CONFIGURATION, new MDArtifact(moduleDescriptor, artifacts[i].getName(), artifacts[i].getType(), artifacts[i].getExt()));
            }
        } else {
            moduleDescriptor.addArtifact(DEFAULT_CONFIGURATION, new MDArtifact(moduleDescriptor, mrid.getName(), "jar", "jar"));
        }
        moduleDescriptor.setLastModified(System.currentTimeMillis());
        return moduleDescriptor;
    }

    /**
     * Transforms the given module descriptor of the given namespace and return
     * a new module descriptor in the system namespace.
     * 
     * <i>Note that dependency exclude rules are not converted in system namespace, because they aren't 
     * transformable (the name space hasn't the ability to convert regular expressions)</i>
     * 
     * @param md
     * @param ns
     * @return
     */
    public static ModuleDescriptor transformInstance(ModuleDescriptor md, Namespace ns) {
        NamespaceTransformer t = ns.getToSystemTransformer();
        if (t.isIdentity()) {
            return md;
        }
        DefaultModuleDescriptor nmd = new DefaultModuleDescriptor();
        nmd._revId = t.transform(md.getModuleRevisionId());
        nmd._resolvedRevId = t.transform(md.getResolvedModuleRevisionId());
        nmd._status = md.getStatus();
        nmd._publicationDate = md.getPublicationDate();
        nmd._resolvedPublicationDate = md.getResolvedPublicationDate();
        DependencyDescriptor[] dd = md.getDependencies();
        for (int i = 0; i < dd.length; i++) {
            nmd._dependencies.add(NameSpaceHelper.toSystem(dd[i], ns));
        }
        Configuration[] confs = md.getConfigurations();
        for (int i = 0; i < confs.length; i++) {
            nmd._configurations.put(confs[i].getName(), confs[i]);
            Artifact[] arts = md.getArtifacts(confs[i].getName());
            for (int j = 0; j < arts.length; j++) {
                nmd.addArtifact(confs[i].getName(), NameSpaceHelper.transform(arts[j], t));
            }
        }
        nmd.setDefault(md.isDefault());
        if (md instanceof DefaultModuleDescriptor) {
            DefaultModuleDescriptor dmd = (DefaultModuleDescriptor)md;
            nmd._conflictManagers.putAll(dmd._conflictManagers);
        } else {
            Message.warn("transformed module descriptor is not a default module descriptor: impossible to copy conflict manager configuration: "+md);
        }
        nmd._licenses.addAll(Arrays.asList(md.getLicenses()));
        nmd._homePage = md.getHomePage();
        nmd._lastModified = md.getLastModified();
        nmd._namespace = ns;
        
        return nmd;
    }
    

	private ModuleRevisionId _revId;
	private ModuleRevisionId _resolvedRevId;
	private String _status = Status.DEFAULT_STATUS;
	private Date _publicationDate;
	private Date _resolvedPublicationDate;
	private List _dependencies = new ArrayList();
	private Map _configurations = new LinkedHashMap(); // Map(String conf -> Configuration)
    private Map _artifacts = new HashMap(); // Map (String conf -> Collection(Artifact))
    private boolean _isDefault = false;
    private Map _conflictManagers = new LinkedHashMap(); // Map (ModuleId -> )
    private List _licenses = new ArrayList(); // List(License)
    private String _homePage;
    private long _lastModified = 0;
    private Namespace _namespace;
	private boolean _mappingOverride;

    public DefaultModuleDescriptor(ModuleRevisionId id, String status, Date pubDate) {
        this(id, status, pubDate, false);
    }
    
    public DefaultModuleDescriptor(ModuleRevisionId id, String status, Date pubDate, boolean isDefault) {
        if (id == null) {
            throw new NullPointerException("null module revision id not allowed");
        }
        if (status == null) {
            throw new NullPointerException("null status not allowed");
        }
        _revId = id;
        _resolvedRevId = id;
        _status = status;
        _publicationDate = pubDate;
        _resolvedPublicationDate = _publicationDate==null?new Date():_publicationDate;
        _isDefault = isDefault;
    }
    
	/**
	 * IMPORTANT : at least call setModuleRevisionId and setResolvedPublicationDate
	 * with instances created by this constructor !
	 *
	 */
	public DefaultModuleDescriptor() {
    }
    
    public boolean isDefault() {
        return _isDefault;
    }
	
    public void setPublicationDate(Date publicationDate) {
        _publicationDate = publicationDate;
        if (_resolvedPublicationDate == null) {
            _resolvedPublicationDate = _publicationDate==null?new Date():_publicationDate;
        }
    }
	public Date getPublicationDate() {
        return _publicationDate;
    }
    public void setResolvedPublicationDate(Date publicationDate) {
        if (publicationDate == null) {
            throw new NullPointerException("null publication date not allowed");
        }
        _resolvedPublicationDate = publicationDate;
    }
    public Date getResolvedPublicationDate() {
        return _resolvedPublicationDate;
    }
	
    public void setModuleRevisionId(ModuleRevisionId revId) {
        if (revId == null) {
            throw new NullPointerException("null module revision id not allowed");
        }
        _revId = revId;
        if (_resolvedRevId == null) {
            _resolvedRevId = _revId;
        }
    }

    public void setResolvedModuleRevisionId(ModuleRevisionId revId) {
        _resolvedRevId = revId;
    }

    public void setStatus(String status) {
        _status = status;
    }

    public void addDependency(DependencyDescriptor dependency) {
		_dependencies.add(dependency);
	}
	
    public void addConfiguration(Configuration conf) {
		_configurations.put(conf.getName(), conf);
	}
    
    /**
     * Artifact configurations are not used since added artifact may not be
     * entirely completed, so its configurations data may not be accurate
     * @param conf
     * @param artifact
     */
    public void addArtifact(String conf, Artifact artifact) {
        Collection artifacts = (Collection)_artifacts.get(conf);
        if (artifacts == null) {
            artifacts = new ArrayList();
            _artifacts.put(conf, artifacts);
        }
        artifacts.add(artifact);
    }
	
	public ModuleRevisionId getModuleRevisionId() {
		return _revId;
	}

	public ModuleRevisionId getResolvedModuleRevisionId() {
		return _resolvedRevId;
	}

	public String getStatus() {
		return _status;
	}

    public Configuration[] getConfigurations() {
        return (Configuration[])_configurations.values().toArray(new Configuration[_configurations.size()]);
    }
    
    public String[] getConfigurationsNames() {
        return (String[])_configurations.keySet().toArray(new String[_configurations.size()]);
    }
    
    public String[] getPublicConfigurationsNames() {
        List ret = new ArrayList();
        for (Iterator iter = _configurations.values().iterator(); iter.hasNext();) {
            Configuration conf = (Configuration)iter.next();
            if (conf.getVisibility() == Configuration.Visibility.PUBLIC) {
                ret.add(conf.getName());
            }
        }
        return (String[])ret.toArray(new String[ret.size()]);
    }
    
    /**
     * Returns the configuration object with the given name in the current module descriptor, null
     * if not found.
     */
    public Configuration getConfiguration(String confName) {
        return (Configuration)_configurations.get(confName);
    }

    public Artifact[] getArtifacts(String conf) {
        Collection artifacts = (Collection)_artifacts.get(conf);
        if (artifacts == null) {
            return new Artifact[0];
        } else {
            return (Artifact[])artifacts.toArray(new Artifact[artifacts.size()]);
        }
    }
    public Artifact[] getAllArtifacts() {
        Collection ret = new ArrayList();
        for (Iterator iter = _artifacts.keySet().iterator(); iter.hasNext();) {
            String conf = (String)iter.next();
            ret.addAll((Collection)_artifacts.get(conf));
        }
        return (Artifact[])ret.toArray(new Artifact[ret.size()]);
    }
	public DependencyDescriptor[] getDependencies() {
		return (DependencyDescriptor[])_dependencies.toArray(new DependencyDescriptor[_dependencies.size()]);
	}
	
	public boolean dependsOn(ModuleDescriptor md) {
	    for (Iterator iter = _dependencies.iterator(); iter.hasNext();) {
            DependencyDescriptor dd = (DependencyDescriptor)iter.next();
            if (dd.getDependencyId().equals(md.getModuleRevisionId().getModuleId())) {
                if (md.getResolvedModuleRevisionId().getRevision() == null) {
                    return true;
                } else if (dd.getDependencyRevisionId().acceptRevision(md.getResolvedModuleRevisionId().getRevision())) {
                    return true;
                }
            }
        }
        return false;
    }

    public String toString() {
        return "module: "+_revId+" status="+_status+" publication="+_publicationDate+" configurations="+_configurations+" artifacts="+_artifacts+" dependencies="+_dependencies;
    }

    public void setDefault(boolean b) {
        _isDefault = b;
    }
    
    private static class ModuleIdMatcher {
        private PatternMatcher _matcher;
        private ModuleId _mid;
        public ModuleIdMatcher(PatternMatcher matcher, ModuleId mid) {
            _matcher = matcher;
            _mid = mid;
        }
        public boolean matches(ModuleId mid) {
            return MatcherHelper.matches(_matcher, _mid, mid);
        }
    }

    /**
     * regular expressions as explained in Pattern class may be used in ModuleId
     * organisation and name
     * 
     * @param moduleId
     * @param matcher 
     * @param resolverName
     */
    public void addConflictManager(ModuleId moduleId, PatternMatcher matcher, ConflictManager manager) {
        _conflictManagers.put(new ModuleIdMatcher(matcher, moduleId), manager);
    }
    public ConflictManager getConflictManager(ModuleId moduleId) {
        for (Iterator iter = _conflictManagers.keySet().iterator(); iter.hasNext();) {
            ModuleIdMatcher matcher = (ModuleIdMatcher)iter.next();
            if (matcher.matches(moduleId)) {
                return (ConflictManager)_conflictManagers.get(matcher);
            }
        }
        return null;
    }
    
    public void addLicense(License license) {
        _licenses.add(license);
    }

    public License[] getLicenses() {
        return (License[])_licenses.toArray(new License[_licenses.size()]);
    }

    public String getHomePage() {
        return _homePage;
    }
    

    public void setHomePage(String homePage) {
        _homePage = homePage;
    }

    public long getLastModified() {
        return _lastModified;
    }
    

    public void setLastModified(long lastModified) {
        _lastModified = lastModified;
    }

    public Namespace getNamespace() {
        if (_namespace == null || !isNamespaceUseful()) {
            return null;
        }
        return _namespace;
    }

    public boolean isNamespaceUseful() {
        for (Iterator iter = _dependencies.iterator(); iter.hasNext();) {
            DependencyDescriptor dd = (DependencyDescriptor)iter.next();
            if (dd.getAllDependencyArtifactsExcludes().length > 0) {
                return true;
            }
        }
        return false;
    }

    public void setNamespace(Namespace ns) {
        _namespace = ns;
    }

    /**
     * Throws an exception if the module descriptor is inconsistent
     * For the moment, only extended configurations existence is checked
     */
    public void check() {
        for (Iterator iter = _configurations.values().iterator(); iter.hasNext();) {
            Configuration conf = (Configuration)iter.next();
            String[] ext = conf.getExtends();
            for (int i = 0; i < ext.length; i++) {
                if (!_configurations.containsKey(ext[i].trim())) {
                    throw new IllegalStateException("unknown configuration '"+ext[i]+"'. It is extended by "+conf.getName());
                }
            }
        }
    }

	public void setMappingOverride(boolean override) {
		_mappingOverride = override;
	}
	
	public boolean isMappingOverride() {
		return _mappingOverride;
	}
}
