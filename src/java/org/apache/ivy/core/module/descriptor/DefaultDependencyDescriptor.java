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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ivy.core.module.id.ArtifactId;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.matcher.MatcherHelper;
import org.apache.ivy.plugins.namespace.NameSpaceHelper;
import org.apache.ivy.plugins.namespace.Namespace;
import org.apache.ivy.plugins.namespace.NamespaceTransformer;

/**
 * This class can be used as the default implementation for DependencyDescriptor. It implements
 * required methods and enables to fill dependency information with the addDependencyConfiguration
 * method.
 */
public class DefaultDependencyDescriptor implements DependencyDescriptor {
    private static final Pattern SELF_FALLBACK_PATTERN = Pattern.compile("@(\\(.*\\))?");

    private static final Pattern THIS_FALLBACK_PATTERN = Pattern.compile("#(\\(.*\\))?");

    /**
     * Transforms the given dependency descriptor of the given namespace and return a new dependency
     * descriptor in the system namespace. <i>Note that exclude rules are not converted in system
     * namespace, because they aren't transformable (the name space hasn't the ability to convert
     * regular expressions). However, method doesExclude will work with system artifacts.</i>
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
     * Transforms a dependency descriptor using the given transformer. Note that no namespace info
     * will be attached to the transformed dependency descriptor, so calling doesExclude is not
     * recommended (doesExclude only works when namespace is properly set)
     * 
     * @param dd
     * @param t
     * @return
     */
    public static DefaultDependencyDescriptor transformInstance(DependencyDescriptor dd,
            NamespaceTransformer t, boolean fromSystem) {
        ModuleRevisionId transformParentId = t.transform(dd.getParentRevisionId());
        ModuleRevisionId transformMrid = t.transform(dd.getDependencyRevisionId());
        DefaultDependencyDescriptor newdd = new DefaultDependencyDescriptor(null, transformMrid, dd
                .isForce(), dd.isChanging(), dd.isTransitive());
        newdd._parentId = transformParentId;
        String[] moduleConfs = dd.getModuleConfigurations();
        if (moduleConfs.length == 1 && "*".equals(moduleConfs[0])) {
            if (dd instanceof DefaultDependencyDescriptor) {
                DefaultDependencyDescriptor ddd = (DefaultDependencyDescriptor) dd;
                newdd._confs = new LinkedHashMap(ddd._confs);
                newdd._excludeRules = new LinkedHashMap(ddd._excludeRules);
                newdd._includeRules = new LinkedHashMap(ddd._includeRules);
                newdd._dependencyArtifacts = new LinkedHashMap(ddd._dependencyArtifacts);
            } else {
                throw new IllegalArgumentException(
                        "dependency descriptor transformation does not support * module confs with descriptors which aren't DefaultDependencyDescriptor");
            }
        } else {
            for (int i = 0; i < moduleConfs.length; i++) {
                newdd._confs.put(moduleConfs[i], new ArrayList(Arrays.asList(dd
                        .getDependencyConfigurations(moduleConfs[i]))));
                newdd._excludeRules.put(moduleConfs[i], new ArrayList(Arrays.asList(dd
                        .getExcludeRules(moduleConfs[i]))));
                newdd._includeRules.put(moduleConfs[i], new ArrayList(Arrays.asList(dd
                        .getIncludeRules(moduleConfs[i]))));
                newdd._dependencyArtifacts.put(moduleConfs[i], new ArrayList(Arrays.asList(dd
                        .getDependencyArtifacts(moduleConfs[i]))));
            }
        }
        if (fromSystem) {
            newdd._asSystem = dd;
        }
        return newdd;
    }

    private ModuleRevisionId _revId;

    private Map _confs = new LinkedHashMap();

    private Map _dependencyArtifacts = new LinkedHashMap(); // Map (String masterConf ->

    // Collection(DependencyArtifactDescriptor))

    private Map _includeRules = new LinkedHashMap(); // Map (String masterConf ->

    // Collection(IncludeRule))

    private Map _excludeRules = new LinkedHashMap(); // Map (String masterConf ->

    // Collection(ExcludeRule))

    private Set _extends = new LinkedHashSet();

    /**
     * Used to indicate that this revision must be used in case of conflicts, independently of
     * conflicts manager
     */
    private boolean _force;

    /**
     * Used to indicate that the dependency is a changing one, i.e. that ivy should not rely on the
     * version to know if it can trust artifacts in cache
     */
    private boolean _changing;

    private ModuleRevisionId _parentId;

    private boolean _transitive = true;

    /**
     * This namespace should be used to check
     */
    private Namespace _namespace = null;

    private final ModuleDescriptor _md;

    private DependencyDescriptor _asSystem = this;

    public DefaultDependencyDescriptor(DependencyDescriptor dd, String revision) {
        _md = null;
        _parentId = dd.getParentRevisionId();
        _revId = ModuleRevisionId.newInstance(dd.getDependencyRevisionId(), revision);
        _force = dd.isForce();
        _changing = dd.isChanging();
        _transitive = dd.isTransitive();
        String[] moduleConfs = dd.getModuleConfigurations();
        for (int i = 0; i < moduleConfs.length; i++) {
            _confs.put(moduleConfs[i], new ArrayList(Arrays.asList(dd
                    .getDependencyConfigurations(moduleConfs[i]))));
            _excludeRules.put(moduleConfs[i], new ArrayList(Arrays.asList(dd
                    .getExcludeRules(moduleConfs[i]))));
            _includeRules.put(moduleConfs[i], new ArrayList(Arrays.asList(dd
                    .getIncludeRules(moduleConfs[i]))));
        }
    }

    public DefaultDependencyDescriptor(ModuleDescriptor md, ModuleRevisionId mrid, boolean force,
            boolean changing, boolean transitive) {
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
        _md = null;
        _revId = mrid;
        _force = force;
        _changing = changing;
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
     * Return the dependency configurations mapped to the given moduleConfiguration, actually
     * resolved because of the given requestedConfiguration Usually requestedConfiguration and
     * moduleConfiguration are the same, except when a conf extends another, then the
     * moduleConfiguration is the configuration currently resolved (the extended one), and
     * requestedConfiguration is the one actually requested initially (the extending one). Both
     * moduleConfiguration and requestedConfiguration are configurations of the caller, the array
     * returned is composed of the required configurations of the dependency described by this
     * descriptor.
     */
    public String[] getDependencyConfigurations(String moduleConfiguration,
            String requestedConfiguration) {
        List confs = (List) _confs.get(moduleConfiguration);
        if (confs == null) {
            // there is no mapping defined for this configuration, add the 'other' mappings.
            confs = (List) _confs.get("%");
        }
        List defConfs = (List) _confs.get("*");
        Collection ret = new LinkedHashSet();
        if (confs != null) {
            ret.addAll(confs);
        }
        if (defConfs != null) {
            ret.addAll(defConfs);
        }

        Collection replacedRet = new LinkedHashSet();
        for (Iterator iter = ret.iterator(); iter.hasNext();) {
            String c = (String) iter.next();
            String replacedConf = replaceSelfFallbackPattern(c, moduleConfiguration);
            if (replacedConf == null) {
                replacedConf = replaceThisFallbackPattern(c, requestedConfiguration);
            }
            if (replacedConf != null) {
                c = replacedConf;
            }
            replacedRet.add(c);
        }
        ret = replacedRet;
        if (ret.remove("*")) {
            StringBuffer r = new StringBuffer("*");
            // merge excluded configurations as one conf like *!A!B
            for (Iterator iter = ret.iterator(); iter.hasNext();) {
                String c = (String) iter.next();
                if (c.startsWith("!")) {
                    r.append(c);
                }
            }
            return new String[] {r.toString()};
        }
        return (String[]) ret.toArray(new String[ret.size()]);
    }

    protected static String replaceSelfFallbackPattern(final String conf,
            final String moduleConfiguration) {
        return replaceFallbackConfigurationPattern(SELF_FALLBACK_PATTERN, conf, moduleConfiguration);
    }

    protected static String replaceThisFallbackPattern(final String conf,
            final String requestedConfiguration) {
        return replaceFallbackConfigurationPattern(THIS_FALLBACK_PATTERN, conf,
            requestedConfiguration);
    }

    /**
     * Replaces fallback patterns with correct values if fallback pattern exists.
     * 
     * @param pattern
     *            pattern to look for
     * @param conf
     *            configuration mapping from dependency element
     * @param moduleConfiguration
     *            module's configuration to use for replacement
     * @return Replaced string if pattern matched. Otherwise null.
     */
    protected static String replaceFallbackConfigurationPattern(final Pattern pattern,
            final String conf, final String moduleConfiguration) {
        Matcher matcher = pattern.matcher(conf);
        if (matcher.matches()) {
            if (matcher.group(1) != null) {
                return moduleConfiguration + matcher.group(1);
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

    public DependencyArtifactDescriptor[] getDependencyArtifacts(String moduleConfiguration) {
        Collection artifacts = getCollectionForConfiguration(moduleConfiguration,
            _dependencyArtifacts);
        return (DependencyArtifactDescriptor[]) artifacts
                .toArray(new DependencyArtifactDescriptor[artifacts.size()]);
    }

    public IncludeRule[] getIncludeRules(String moduleConfiguration) {
        Collection rules = getCollectionForConfiguration(moduleConfiguration, _includeRules);
        return (IncludeRule[]) rules.toArray(new IncludeRule[rules.size()]);
    }

    public ExcludeRule[] getExcludeRules(String moduleConfiguration) {
        Collection rules = getCollectionForConfiguration(moduleConfiguration, _excludeRules);
        return (ExcludeRule[]) rules.toArray(new ExcludeRule[rules.size()]);
    }

    private Set getCollectionForConfiguration(String moduleConfiguration, Map collectionMap) {
        if (collectionMap.isEmpty()) {
            return Collections.EMPTY_SET;
        }
        Collection artifacts = (Collection) collectionMap.get(moduleConfiguration);
        Collection defArtifacts = (Collection) collectionMap.get("*");
        Set ret = new LinkedHashSet();
        if (artifacts != null) {
            ret.addAll(artifacts);
        }
        if (defArtifacts != null) {
            ret.addAll(defArtifacts);
        }
        return ret;
    }

    public DependencyArtifactDescriptor[] getDependencyArtifacts(String[] moduleConfigurations) {
        Set artifacts = new LinkedHashSet();
        for (int i = 0; i < moduleConfigurations.length; i++) {
            artifacts.addAll(Arrays.asList(getDependencyArtifacts(moduleConfigurations[i])));
        }
        return (DependencyArtifactDescriptor[]) artifacts
                .toArray(new DependencyArtifactDescriptor[artifacts.size()]);
    }

    public IncludeRule[] getIncludeRules(String[] moduleConfigurations) {
        Set rules = new LinkedHashSet();
        for (int i = 0; i < moduleConfigurations.length; i++) {
            rules.addAll(Arrays.asList(getIncludeRules(moduleConfigurations[i])));
        }
        return (IncludeRule[]) rules.toArray(new IncludeRule[rules.size()]);
    }

    public ExcludeRule[] getExcludeRules(String[] moduleConfigurations) {
        Set rules = new LinkedHashSet();
        for (int i = 0; i < moduleConfigurations.length; i++) {
            rules.addAll(Arrays.asList(getExcludeRules(moduleConfigurations[i])));
        }
        return (ExcludeRule[]) rules.toArray(new ExcludeRule[rules.size()]);
    }

    public DependencyArtifactDescriptor[] getAllDependencyArtifacts() {
        Set ret = mergeAll(_dependencyArtifacts);
        return (DependencyArtifactDescriptor[]) ret.toArray(new DependencyArtifactDescriptor[ret
                .size()]);
    }

    public IncludeRule[] getAllIncludeRules() {
        Set ret = mergeAll(_includeRules);
        return (IncludeRule[]) ret.toArray(new IncludeRule[ret.size()]);
    }

    public ExcludeRule[] getAllExcludeRules() {
        Set ret = mergeAll(_excludeRules);
        return (ExcludeRule[]) ret.toArray(new ExcludeRule[ret.size()]);
    }

    private Set mergeAll(Map artifactsMap) {
        Set ret = new LinkedHashSet();
        for (Iterator it = artifactsMap.values().iterator(); it.hasNext();) {
            Collection artifacts = (Collection) it.next();
            ret.addAll(artifacts);
        }
        return ret;
    }

    public void addDependencyConfiguration(String masterConf, String depConf) {
        if ((_md != null) && !"*".equals(masterConf) && !"%".equals(masterConf)) {
            Configuration config = _md.getConfiguration(masterConf);
            if (config == null) {
                throw new IllegalArgumentException("Configuration '" + masterConf
                        + "' does not exist in module " + _md);
            }
        }

        List confs = (List) _confs.get(masterConf);
        if (confs == null) {
            confs = new ArrayList();
            _confs.put(masterConf, confs);
        }
        if (!confs.contains(depConf)) {
            confs.add(depConf);
        }
    }

    public void addDependencyArtifact(String masterConf, DependencyArtifactDescriptor dad) {
        addObjectToConfiguration(masterConf, dad, _dependencyArtifacts);
    }

    public void addIncludeRule(String masterConf, IncludeRule rule) {
        addObjectToConfiguration(masterConf, rule, _includeRules);
    }

    public void addExcludeRule(String masterConf, ExcludeRule rule) {
        addObjectToConfiguration(masterConf, rule, _excludeRules);
    }

    private void addObjectToConfiguration(String callerConf, Object toAdd, Map confsMap) {
        Collection col = (Collection) confsMap.get(callerConf);
        if (col == null) {
            col = new ArrayList();
            confsMap.put(callerConf, col);
        }
        col.add(toAdd);
    }

    /**
     * only works when namespace is properly set. The behaviour is not specified if namespace is not
     * set
     */
    public boolean doesExclude(String[] moduleConfigurations, ArtifactId artifactId) {
        if (_namespace != null) {
            artifactId = NameSpaceHelper.transform(artifactId, _namespace
                    .getFromSystemTransformer());
        }
        ExcludeRule[] rules = getExcludeRules(moduleConfigurations);
        for (int i = 0; i < rules.length; i++) {
            if (MatcherHelper.matches(rules[i].getMatcher(), rules[i].getId(), artifactId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if this descriptor contains any exclusion rule
     * 
     * @return
     */
    public boolean canExclude() {
        return !_excludeRules.isEmpty();
    }

    public void addExtends(String conf) {
        _extends.add(conf);
    }

    public String toString() {
        return "dependency: " + _revId + " " + _confs;
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
