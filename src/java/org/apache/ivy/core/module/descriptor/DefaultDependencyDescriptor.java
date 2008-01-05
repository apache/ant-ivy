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
     * @param dd
     * @param ns
     * @return
     */
    public static DependencyDescriptor transformInstance(DependencyDescriptor dd, Namespace ns) {
        NamespaceTransformer t = ns.getToSystemTransformer();
        if (t.isIdentity()) {
            return dd;
        }
        DefaultDependencyDescriptor newdd = transformInstance(dd, t, false);
        newdd.namespace = ns;
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
        newdd.parentId = transformParentId;
        String[] moduleConfs = dd.getModuleConfigurations();
        if (moduleConfs.length == 1 && "*".equals(moduleConfs[0])) {
            if (dd instanceof DefaultDependencyDescriptor) {
                DefaultDependencyDescriptor ddd = (DefaultDependencyDescriptor) dd;
                newdd.confs = new LinkedHashMap(ddd.confs);
                newdd.setExcludeRules(new LinkedHashMap(ddd.getExcludeRules()));
                newdd.setIncludeRules(new LinkedHashMap(ddd.getIncludeRules()));
                newdd.setDependencyArtifacts(new LinkedHashMap(ddd.getDependencyArtifacts()));
            } else {
                throw new IllegalArgumentException(
                        "dependency descriptor transformation does not support * module confs "
                        + "with descriptors which aren't DefaultDependencyDescriptor");
            }
        } else {
            for (int i = 0; i < moduleConfs.length; i++) {
                newdd.confs.put(moduleConfs[i], new ArrayList(Arrays.asList(dd
                        .getDependencyConfigurations(moduleConfs[i]))));
                newdd.getExcludeRules().put(moduleConfs[i], new ArrayList(Arrays.asList(dd
                        .getExcludeRules(moduleConfs[i]))));
                newdd.getIncludeRules().put(moduleConfs[i], new ArrayList(Arrays.asList(dd
                        .getIncludeRules(moduleConfs[i]))));
                newdd.getDependencyArtifacts().put(moduleConfs[i], new ArrayList(Arrays.asList(dd
                        .getDependencyArtifacts(moduleConfs[i]))));
            }
        }
        if (fromSystem) {
            newdd.asSystem = dd;
        }
        return newdd;
    }

    private ModuleRevisionId revId;

    private Map confs = new LinkedHashMap();

    // Map (String masterConf -> Collection(DependencyArtifactDescriptor))
    private Map dependencyArtifacts; // initialized on demand only for memory consumption reason

    // Map (String masterConf -> Collection(IncludeRule))
    private Map includeRules;  // initialized on demand only for memory consumption reason

    // Map (String masterConf -> Collection(ExcludeRule))
    private Map excludeRules;  // initialized on demand only for memory consumption reason

    /**
     * Used to indicate that this revision must be used in case of conflicts, independently of
     * conflicts manager
     */
    private boolean isForce;

    /**
     * Used to indicate that the dependency is a changing one, i.e. that ivy should not rely on the
     * version to know if it can trust artifacts in cache
     */
    private boolean isChanging;

    private ModuleRevisionId parentId;

    private boolean isTransitive = true;

    /**
     * This namespace should be used to check
     */
    private Namespace namespace = null;

    private final ModuleDescriptor md;

    private DependencyDescriptor asSystem = this;

    public DefaultDependencyDescriptor(DependencyDescriptor dd, String revision) {
        md = null;
        parentId = dd.getParentRevisionId();
        revId = ModuleRevisionId.newInstance(dd.getDependencyRevisionId(), revision);
        isForce = dd.isForce();
        isChanging = dd.isChanging();
        isTransitive = dd.isTransitive();
        String[] moduleConfs = dd.getModuleConfigurations();
        Map excludeRules = getExcludeRules();
        Map includeRules = getIncludeRules();
        for (int i = 0; i < moduleConfs.length; i++) {
            confs.put(moduleConfs[i], new ArrayList(Arrays.asList(dd
                    .getDependencyConfigurations(moduleConfs[i]))));
            excludeRules.put(moduleConfs[i], new ArrayList(Arrays.asList(dd
                    .getExcludeRules(moduleConfs[i]))));
            includeRules.put(moduleConfs[i], new ArrayList(Arrays.asList(dd
                    .getIncludeRules(moduleConfs[i]))));
        }
    }

    public DefaultDependencyDescriptor(ModuleDescriptor md, ModuleRevisionId mrid, boolean force,
            boolean changing, boolean transitive) {
        this.md = md;
        revId = mrid;
        isForce = force;
        isChanging = changing;
        isTransitive = transitive;
    }

    public DefaultDependencyDescriptor(ModuleRevisionId mrid, boolean force) {
        this(mrid, force, false);
    }

    public DefaultDependencyDescriptor(ModuleRevisionId mrid, boolean force, boolean changing) {
        md = null;
        revId = mrid;
        isForce = force;
        isChanging = changing;
    }

    public ModuleId getDependencyId() {
        return getDependencyRevisionId().getModuleId();
    }

    public ModuleRevisionId getDependencyRevisionId() {
        return revId;
    }

    public String[] getModuleConfigurations() {
        return (String[]) confs.keySet().toArray(new String[confs.keySet().size()]);
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
        List confsList = (List) confs.get(moduleConfiguration);
        if (confsList == null) {
            // there is no mapping defined for this configuration, add the 'other' mappings.
            confsList = (List) confs.get("%");
        }
        List defConfs = (List) confs.get("*");
        Collection ret = new LinkedHashSet();
        if (confsList != null) {
            ret.addAll(confsList);
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
        return replaceFallbackConfigurationPattern(
            SELF_FALLBACK_PATTERN, conf, moduleConfiguration);
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
            dependencyArtifacts);
        return (DependencyArtifactDescriptor[]) artifacts
                .toArray(new DependencyArtifactDescriptor[artifacts.size()]);
    }

    public IncludeRule[] getIncludeRules(String moduleConfiguration) {
        Collection rules = getCollectionForConfiguration(moduleConfiguration, includeRules);
        return (IncludeRule[]) rules.toArray(new IncludeRule[rules.size()]);
    }

    public ExcludeRule[] getExcludeRules(String moduleConfiguration) {
        Collection rules = getCollectionForConfiguration(moduleConfiguration, excludeRules);
        return (ExcludeRule[]) rules.toArray(new ExcludeRule[rules.size()]);
    }

    private Set getCollectionForConfiguration(String moduleConfiguration, Map collectionMap) {
        if (collectionMap == null || collectionMap.isEmpty()) {
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
        if (dependencyArtifacts == null) {
            return new DependencyArtifactDescriptor[0];
        }
        Set ret = mergeAll(dependencyArtifacts);
        return (DependencyArtifactDescriptor[]) ret.toArray(new DependencyArtifactDescriptor[ret
                .size()]);
    }

    public IncludeRule[] getAllIncludeRules() {
        if (includeRules == null) {
            return new IncludeRule[0];
        }
        Set ret = mergeAll(includeRules);
        return (IncludeRule[]) ret.toArray(new IncludeRule[ret.size()]);
    }

    public ExcludeRule[] getAllExcludeRules() {
        if (excludeRules == null) {
            return new ExcludeRule[0];
        }
        Set ret = mergeAll(excludeRules);
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
        if ((md != null) && !"*".equals(masterConf) && !"%".equals(masterConf)) {
            Configuration config = md.getConfiguration(masterConf);
            if (config == null) {
                throw new IllegalArgumentException("Configuration '" + masterConf
                        + "' does not exist in module " + md);
            }
        }

        List confsList = (List) confs.get(masterConf);
        if (confsList == null) {
            confsList = new ArrayList();
            confs.put(masterConf, confsList);
        }
        if (!confsList.contains(depConf)) {
            confsList.add(depConf);
        }
    }

    public void addDependencyArtifact(String masterConf, DependencyArtifactDescriptor dad) {
        addObjectToConfiguration(masterConf, dad, getDependencyArtifacts());
    }

    public void addIncludeRule(String masterConf, IncludeRule rule) {
        addObjectToConfiguration(masterConf, rule, getIncludeRules());
    }

    public void addExcludeRule(String masterConf, ExcludeRule rule) {
        addObjectToConfiguration(masterConf, rule, getExcludeRules());
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
        if (namespace != null) {
            artifactId = NameSpaceHelper.transform(artifactId, namespace
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
        return excludeRules != null && !excludeRules.isEmpty();
    }

    public String toString() {
        return "dependency: " + revId + " " + confs;
    }

    public boolean isForce() {
        return isForce;
    }

    public ModuleRevisionId getParentRevisionId() {
        return md != null ? md.getResolvedModuleRevisionId() : parentId;
    }

    public boolean isChanging() {
        return isChanging;
    }

    public boolean isTransitive() {
        return isTransitive;
    }

    public Namespace getNamespace() {
        return namespace;
    }

    public String getAttribute(String attName) {
        return revId.getAttribute(attName);
    }

    public Map getAttributes() {
        return revId.getAttributes();
    }

    public String getExtraAttribute(String attName) {
        return revId.getExtraAttribute(attName);
    }

    public Map getExtraAttributes() {
        return revId.getExtraAttributes();
    }

    public Map getQualifiedExtraAttributes() {
        return revId.getQualifiedExtraAttributes();
    }

    public String getStandardAttribute(String attName) {
        return revId.getStandardAttribute(attName);
    }

    public Map getStandardAttributes() {
        return revId.getStandardAttributes();
    }

    public DependencyDescriptor asSystem() {
        return asSystem;
    }

    private void setDependencyArtifacts(Map dependencyArtifacts) {
        this.dependencyArtifacts = dependencyArtifacts;
    }

    private Map getDependencyArtifacts() {
        if (dependencyArtifacts == null) {
            dependencyArtifacts = new LinkedHashMap();
        }
        return dependencyArtifacts;
    }

    private void setIncludeRules(Map includeRules) {
        this.includeRules = includeRules;
    }

    private Map getIncludeRules() {
        if (includeRules == null) {
            includeRules = new LinkedHashMap();
        }
        return includeRules;
    }

    private void setExcludeRules(Map excludeRules) {
        this.excludeRules = excludeRules;
    }

    private Map getExcludeRules() {
        if (excludeRules == null) {
            excludeRules = new LinkedHashMap();
        }
        return excludeRules;
    }

}
