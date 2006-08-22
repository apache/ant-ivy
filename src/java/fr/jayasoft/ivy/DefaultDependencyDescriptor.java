/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.jayasoft.ivy.matcher.MatcherHelper;
import fr.jayasoft.ivy.namespace.NameSpaceHelper;
import fr.jayasoft.ivy.namespace.Namespace;
import fr.jayasoft.ivy.namespace.NamespaceTransformer;

/**
 * This class can be used as the default implementation for DependencyDescriptor.
 * It implements required methods and enables to fill dependency information
 * with the addDependencyConfiguration method.
 * 
 * @author Xavier Hanin
 *
 */
public class DefaultDependencyDescriptor implements DependencyDescriptor {
    private static final Pattern SELF_FALLBACK_PATTERN = Pattern.compile("@(\\(.*\\))?");
    private static final Pattern THIS_FALLBACK_PATTERN = Pattern.compile("#(\\(.*\\))?");
    
    /**
     * Transforms the given dependency descriptor of the given namespace and return
     * a new dependency descriptor in the system namespace.
     * 
     * <i>Note that exclude rules are not converted in system namespace, because they aren't 
     * transformable (the name space hasn't the ability to convert regular expressions).
     * However, method doesExclude will work with system artifacts.</i>
     * 
     * @param md
     * @param ns
     * @return
     */
    public static DependencyDescriptor transformInstance(DependencyDescriptor dd, Namespace ns) {
        NamespaceTransformer t = ns.getToSystemTransformer();
        if (t.isIdentity()) {
            return dd;
        }
        DefaultDependencyDescriptor newdd = transformInstance(dd, t, false);
        newdd._namespace = ns;
        return newdd;
    }
    
    /**
     * Transforms a dependency descriptor using the given transformer.
     * 
     * Note that no namespace info will be attached to the transformed dependency descriptor, 
     * so calling doesExclude is not recommended (doesExclude only works when namespace is properly set)
     * @param dd
     * @param t
     * @return
     */
    public static DefaultDependencyDescriptor transformInstance(DependencyDescriptor dd, NamespaceTransformer t, boolean fromSystem) {
        ModuleRevisionId transformParentId = t.transform(dd.getParentRevisionId());
        ModuleRevisionId transformMrid = t.transform(dd.getDependencyRevisionId());
        DefaultDependencyDescriptor newdd = new DefaultDependencyDescriptor();
        newdd._parentId = transformParentId;
        newdd._revId = transformMrid;
        newdd._force = dd.isForce();
        newdd._changing = dd.isChanging();
        newdd._transitive = dd.isTransitive();
        String[] moduleConfs = dd.getModuleConfigurations();
        if (moduleConfs.length == 1 && "*".equals(moduleConfs[0])) {
            if (dd instanceof DefaultDependencyDescriptor) {
                DefaultDependencyDescriptor ddd = (DefaultDependencyDescriptor)dd;
                newdd._confs = new HashMap(ddd._confs);
                newdd._artifactsExcludes = new HashMap(ddd._artifactsExcludes);
                newdd._artifactsIncludes = new HashMap(ddd._artifactsIncludes);
            } else {
                throw new IllegalArgumentException("dependency descriptor transformation does not support * module confs with descriptors which aren't DefaultDependencyDescriptor");
            }
        } else {
            for (int i = 0; i < moduleConfs.length; i++) {
                newdd._confs.put(moduleConfs[i], new ArrayList(Arrays.asList(dd.getDependencyConfigurations(moduleConfs[i]))));
                newdd._artifactsExcludes.put(moduleConfs[i], new ArrayList(Arrays.asList(dd.getDependencyArtifactsExcludes(moduleConfs[i]))));
                newdd._artifactsIncludes.put(moduleConfs[i], new ArrayList(Arrays.asList(dd.getDependencyArtifactsIncludes(moduleConfs[i]))));
            }
        }
        if (fromSystem) {
        	newdd._asSystem = dd;
        }
        return newdd;
    }
    
    private ModuleRevisionId _revId;
    private Map _confs = new HashMap();
    private Map _artifactsIncludes = new HashMap(); // Map (String masterConf -> Collection(DependencyArtifactDescriptor))
    private Map _artifactsExcludes = new HashMap(); // Map (String masterConf -> Collection(DependencyArtifactDescriptor))
    private Set _extends = new HashSet();
    
    /**
     * Used to indicate that this revision must be used in case of conflicts, independently
     * of conflicts manager
     */
    private boolean _force;
    /**
     * Used to indicate that the dependency is a changing one, i.e. that ivy should not rely on the version to know if it can trust artifacts in cache
     */
    private boolean _changing; 
    private ModuleRevisionId _parentId;
    
    private boolean _transitive = true;
    
    /**
     * This namespace should be used to check 
     */
    private Namespace _namespace = null;
    private ModuleDescriptor _md;
	private DependencyDescriptor _asSystem = this; 
    
    public DefaultDependencyDescriptor(DependencyDescriptor dd, String revision) {
        _parentId = dd.getParentRevisionId();
        _revId = ModuleRevisionId.newInstance(dd.getDependencyRevisionId(), revision);
        _force = dd.isForce();
        _changing = dd.isChanging();
        _transitive = dd.isTransitive();
        String[] moduleConfs = dd.getModuleConfigurations();
        for (int i = 0; i < moduleConfs.length; i++) {
            _confs.put(moduleConfs[i], new ArrayList(Arrays.asList(dd.getDependencyConfigurations(moduleConfs[i]))));
            _artifactsExcludes.put(moduleConfs[i], new ArrayList(Arrays.asList(dd.getDependencyArtifactsExcludes(moduleConfs[i]))));
            _artifactsIncludes.put(moduleConfs[i], new ArrayList(Arrays.asList(dd.getDependencyArtifactsIncludes(moduleConfs[i]))));
        }
    }
    
    public DefaultDependencyDescriptor(ModuleDescriptor md, ModuleRevisionId mrid, boolean force, boolean changing, boolean transitive) {
        _md = md;
        _revId = mrid;
        _force = force;
        _changing = changing;
        _transitive = transitive;
    }
    
    public DefaultDependencyDescriptor(ModuleRevisionId mrid, boolean force) {
        this(mrid, force, false);
    }
    
    public DefaultDependencyDescriptor(ModuleRevisionId mrid, boolean force, boolean changing) {
        _revId = mrid;
        _force = force;
        _changing = changing;
    }
    
    private DefaultDependencyDescriptor() {        
    }
    
    public ModuleId getDependencyId() {
        return getDependencyRevisionId().getModuleId();
    }
    
    public ModuleRevisionId getDependencyRevisionId() {
        return _revId;
    }
    
    public String[] getModuleConfigurations() {
        return (String[]) _confs.keySet().toArray(new String[_confs.keySet().size()]);
    }
    
    public String[] getDependencyConfigurations(String moduleConfiguration) {
        return getDependencyConfigurations(moduleConfiguration, moduleConfiguration);
    }
    
    /**
     * Return the dependency configurations mapped to the given moduleConfiguration, actually resolved
     * because of the given requestedConfiguration
     * 
     * Usually requestedConfiguration and moduleConfiguration are the same, except when
     * a conf extends another, then the moduleConfiguration is the configuration currently resolved 
     * (the extended one), and requestedConfiguration is the one actually requested initially (the
     * extending one).
     * 
     * Both moduleConfiguration and requestedConfiguration are configurations of the caller,
     * the array returned is composed of the required configurations of the dependency described by this
     * descriptor.
     */
    public String[] getDependencyConfigurations(String moduleConfiguration, String requestedConfiguration) {
        List confs = (List)_confs.get(moduleConfiguration);
        if (confs == null) {
            // there is no mapping defined for this configuration, add the 'other' mappings.
            confs = (List)_confs.get("%");
        }
        List defConfs = (List)_confs.get("*");
        Collection ret = new LinkedHashSet();
        if (confs != null) {
            ret.addAll(confs);
        }
        if (defConfs != null) {
            ret.addAll(defConfs);
        }
        
        Collection replacedRet = new LinkedHashSet();
        for (Iterator iter = ret.iterator(); iter.hasNext();) {
            String c = (String)iter.next();
            String replacedConf = replaceSelfFallbackPattern( c, moduleConfiguration);
            if (replacedConf==null) {
                replacedConf = replaceThisFallbackPattern( c, requestedConfiguration);
            }
            if (replacedConf!=null) {
                c = replacedConf;
            }
            replacedRet.add(c);
        }
        ret = replacedRet;
        if (ret.remove("*")) {
            StringBuffer r = new StringBuffer("*");
            // merge excluded configurations as one conf like *!A!B
            for (Iterator iter = ret.iterator(); iter.hasNext();) {
                String c = (String)iter.next();
                if (c.startsWith("!")) {
                    r.append(c);
                }
            }
            return new String[] {r.toString()};
        }
        return (String[])ret.toArray(new String[ret.size()]);
    }
    
    protected static String replaceSelfFallbackPattern(final String conf, final String moduleConfiguration) {
        return replaceFallbackConfigurationPattern(SELF_FALLBACK_PATTERN, conf, moduleConfiguration);
    }
    
    protected static String replaceThisFallbackPattern(final String conf, final String requestedConfiguration) {
        return replaceFallbackConfigurationPattern(THIS_FALLBACK_PATTERN, conf, requestedConfiguration);
    }
    
    /**
     * Replaces fallback patterns with correct values if fallback pattern exists.
     * @param pattern pattern to look for
     * @param conf configuration mapping from dependency element
     * @param moduleConfiguration module's configuration to use for replacement
     * @return Replaced string if pattern matched. Otherwise null.
     */
    protected static String replaceFallbackConfigurationPattern(final Pattern pattern, final String conf, final String moduleConfiguration) {
        Matcher matcher = pattern.matcher(conf);
        if (matcher.matches()) {
            if (matcher.group(1) != null) {
                return moduleConfiguration+matcher.group(1);
            } else {
                return moduleConfiguration;
            }
        }
        return null;
    }
    
    public String[] getDependencyConfigurations(String[] moduleConfigurations) {
        Set confs = new LinkedHashSet();
        for (int i = 0; i < moduleConfigurations.length; i++) {
            confs.addAll(Arrays.asList(getDependencyConfigurations(moduleConfigurations[i])));
        }
        if (confs.contains("*")) {
            return new String[] {"*"};
        }
        return (String[]) confs.toArray(new String[confs.size()]);
    }
    
    public DependencyArtifactDescriptor[] getDependencyArtifactsIncludes(String moduleConfiguration) {
        return getDependencyArtifacts(moduleConfiguration, _artifactsIncludes);
    }
    
    public DependencyArtifactDescriptor[] getDependencyArtifactsExcludes(String moduleConfiguration) {
        return getDependencyArtifacts(moduleConfiguration, _artifactsExcludes);
    }
    
    private DependencyArtifactDescriptor[] getDependencyArtifacts(String moduleConfiguration, Map artifactsMap) {
        if (artifactsMap.isEmpty()) { 
            return new DependencyArtifactDescriptor[0];
        }
        Collection artifacts = (Collection)artifactsMap.get(moduleConfiguration);
        Collection defArtifacts = (Collection)artifactsMap.get("*");
        Set ret = new HashSet();
        if (artifacts != null) {
            ret.addAll(artifacts);
        }
        if (defArtifacts != null) {
            ret.addAll(defArtifacts);
        }
        return (DependencyArtifactDescriptor[])ret.toArray(new DependencyArtifactDescriptor[ret.size()]);
    }
    
    public DependencyArtifactDescriptor[] getDependencyArtifactsIncludes(String[] moduleConfigurations) {
        Set artifacts = new HashSet();
        for (int i = 0; i < moduleConfigurations.length; i++) {
            artifacts.addAll(Arrays.asList(getDependencyArtifactsIncludes(moduleConfigurations[i])));
        }
        return (DependencyArtifactDescriptor[]) artifacts.toArray(new DependencyArtifactDescriptor[artifacts.size()]);
    }
    
    public DependencyArtifactDescriptor[] getDependencyArtifactsExcludes(String[] moduleConfigurations) {
        Set artifacts = new HashSet();
        for (int i = 0; i < moduleConfigurations.length; i++) {
            artifacts.addAll(Arrays.asList(getDependencyArtifactsExcludes(moduleConfigurations[i])));
        }
        return (DependencyArtifactDescriptor[]) artifacts.toArray(new DependencyArtifactDescriptor[artifacts.size()]);
    }
    
    public DependencyArtifactDescriptor[] getAllDependencyArtifactsIncludes() {
        return getAllDependencyArtifacts(_artifactsIncludes);
    }
    
    public DependencyArtifactDescriptor[] getAllDependencyArtifactsExcludes() {
        return getAllDependencyArtifacts(_artifactsExcludes);
    }
    
    private DependencyArtifactDescriptor[] getAllDependencyArtifacts(Map artifactsMap) {
        Set ret = new HashSet();
        for (Iterator it = artifactsMap.values().iterator(); it.hasNext();) {
            Collection artifacts = (Collection)it.next();
            ret.addAll(artifacts);
        }
        return (DependencyArtifactDescriptor[])ret.toArray(new DependencyArtifactDescriptor[ret.size()]);
    }
    
    public void addDependencyConfiguration(String masterConf, String depConf) {
        List confs = (List)_confs.get(masterConf);
        if (confs == null) {
            confs = new ArrayList();
            _confs.put(masterConf, confs);
        }
        if (!confs.contains(depConf)) {
            confs.add(depConf);
        }
    }
    
    public void addDependencyArtifactIncludes(String masterConf, DependencyArtifactDescriptor dad) {
        addDependencyArtifacts(masterConf, dad, _artifactsIncludes);
    }
    
    public void addDependencyArtifactExcludes(String masterConf, DependencyArtifactDescriptor dad) {
        addDependencyArtifacts(masterConf, dad, _artifactsExcludes);
    }
    
    private void addDependencyArtifacts(String masterConf, DependencyArtifactDescriptor dad, Map artifactsMap) {
        Collection artifacts = (Collection)artifactsMap.get(masterConf);
        if (artifacts == null) {
            artifacts = new ArrayList();
            artifactsMap.put(masterConf, artifacts);
        }
        artifacts.add(dad);
    }
    
    /**
     * only works when namespace is properly set. The behaviour is not specified if namespace is not set
     */
    public boolean doesExclude(String[] moduleConfigurations, ArtifactId artifactId) {
        if (_namespace != null) {
            artifactId = NameSpaceHelper.transform(artifactId, _namespace.getFromSystemTransformer());
        }
        DependencyArtifactDescriptor[] dads = getDependencyArtifactsExcludes(moduleConfigurations);
        for (int i = 0; i < dads.length; i++) {
            if (MatcherHelper.matches(dads[i].getMatcher(), dads[i].getId(), artifactId)) {
                return true;
            }
        }        
        return false;
    }
    
    /**
     * Returns true if this descriptor contains any exclusion rule
     * @return
     */
    public boolean canExclude() {
        return !_artifactsExcludes.isEmpty();
    }
    
    public void addExtends(String conf) {
        _extends.add(conf);
    }
    
    public String toString() {
        return "dependency: "+_revId+" "+_confs;
    }
    
    public boolean isForce() {
        return _force;
    }
    
    public ModuleRevisionId getParentRevisionId() {
        return _md != null ? _md.getResolvedModuleRevisionId() : _parentId;
    }
    
    public boolean isChanging() {
        return _changing;
    }
    
    public boolean isTransitive() {
        return _transitive;
    }
    
    public Namespace getNamespace() {
        return _namespace;
    }

    public String getAttribute(String attName) {
        return _revId.getAttribute(attName);
    }

    public Map getAttributes() {
        return _revId.getAttributes();
    }

    public String getExtraAttribute(String attName) {
        return _revId.getExtraAttribute(attName);
    }

    public Map getExtraAttributes() {
        return _revId.getExtraAttributes();
    }

    public String getStandardAttribute(String attName) {
        return _revId.getStandardAttribute(attName);
    }

    public Map getStandardAttributes() {
        return _revId.getStandardAttributes();
    }

	public DependencyDescriptor asSystem() {
		return _asSystem;
	}
    
}
