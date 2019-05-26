/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import java.util.HashSet;
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
import org.apache.ivy.util.Checks;

/**
 * This class can be used as the default implementation for DependencyDescriptor. It implements
 * required methods and enables to fill dependency information with the addDependencyConfiguration
 * method.
 */
public class DefaultDependencyDescriptor implements DependencyDescriptor {
    private static final Pattern SELF_FALLBACK_PATTERN = Pattern
            .compile("@(\\+[^\\(]+)?(\\(.*\\))?");

    private static final Pattern THIS_FALLBACK_PATTERN = Pattern
            .compile("#(\\+[^\\(]+)?(\\(.*\\))?");

    /**
     * Transforms the given dependency descriptor of the given namespace and return a new dependency
     * descriptor in the system namespace. <i>Note that exclude rules are not converted in system
     * namespace, because they aren't transformable (the name space hasn't the ability to convert
     * regular expressions). However, method doesExclude will work with system artifacts.</i>
     *
     * @param dd DependencyDescriptor
     * @param ns Namespace
     * @return DependencyDescriptor
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
     * @param dd DependencyDescriptor
     * @param t NamespaceTransformer
     * @param fromSystem boolean
     * @return DefaultDependencyDescriptor
     */
    public static DefaultDependencyDescriptor transformInstance(DependencyDescriptor dd,
            NamespaceTransformer t, boolean fromSystem) {
        ModuleRevisionId transformParentId = t.transform(dd.getParentRevisionId());
        ModuleRevisionId transformMrid = t.transform(dd.getDependencyRevisionId());
        ModuleRevisionId transformDynamicMrid = t.transform(dd
                .getDynamicConstraintDependencyRevisionId());
        DefaultDependencyDescriptor newdd = new DefaultDependencyDescriptor(null, transformMrid,
                transformDynamicMrid, dd.isForce(), dd.isChanging(), dd.isTransitive());

        newdd.parentId = transformParentId;
        ModuleRevisionId sourceModule = dd.getSourceModule();
        if (sourceModule != null) {
            newdd.sourceModule = t.transform(sourceModule);
        }

        String[] moduleConfs = dd.getModuleConfigurations();
        if (moduleConfs.length == 1 && "*".equals(moduleConfs[0])) {
            if (dd instanceof DefaultDependencyDescriptor) {
                DefaultDependencyDescriptor ddd = (DefaultDependencyDescriptor) dd;
                newdd.confs = new LinkedHashMap<>(ddd.confs);
                newdd.setExcludeRules(new LinkedHashMap<>(ddd.getExcludeRules()));
                newdd.setIncludeRules(new LinkedHashMap<>(ddd.getIncludeRules()));
                newdd.setDependencyArtifacts(new LinkedHashMap<>(ddd.getDependencyArtifacts()));
            } else {
                throw new IllegalArgumentException(
                        "dependency descriptor transformation does not support * module confs "
                                + "with descriptors which aren't DefaultDependencyDescriptor");
            }
        } else {
            for (String moduleConf : moduleConfs) {
                newdd.confs.put(moduleConf,
                    new ArrayList<>(Arrays.asList(dd.getDependencyConfigurations(moduleConf))));
                newdd.getExcludeRules().put(moduleConf,
                    new ArrayList<>(Arrays.asList(dd.getExcludeRules(moduleConf))));
                newdd.getIncludeRules().put(moduleConf,
                    new ArrayList<>(Arrays.asList(dd.getIncludeRules(moduleConf))));
                newdd.getDependencyArtifacts().put(moduleConf,
                    new ArrayList<>(Arrays.asList(dd.getDependencyArtifacts(moduleConf))));
            }
        }
        if (fromSystem) {
            newdd.asSystem = dd;
        }
        return newdd;
    }

    private final ModuleRevisionId revId;

    private ModuleRevisionId dynamicRevId;

    private Map<String, List<String>> confs = new LinkedHashMap<>();

    // Map (String masterConf -> Collection(DependencyArtifactDescriptor))
    // initialized on demand only for memory consumption reasons
    private Map<String, Collection<DependencyArtifactDescriptor>> dependencyArtifacts;

    // Map (String masterConf -> Collection(IncludeRule))
    // initialized on demand only for memory consumption reasons
    private Map<String, Collection<IncludeRule>> includeRules;

    // Map (String masterConf -> Collection(ExcludeRule))
    // initialized on demand only for memory consumption reasons
    private Map<String, Collection<ExcludeRule>> excludeRules;

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

    private ModuleRevisionId sourceModule;

    private DefaultDependencyDescriptor(DefaultDependencyDescriptor dd, ModuleRevisionId revision) {
        Checks.checkNotNull(dd, "dd");
        Checks.checkNotNull(revision, "revision");

        if (!revision.getModuleId().equals(dd.getDependencyId())) {
            throw new IllegalArgumentException(
                    "new ModuleRevisionId MUST have the same ModuleId as original one."
                            + " original = " + dd.getDependencyId() + " new = "
                            + revision.getModuleId());
        }
        md = dd.md;
        parentId = dd.parentId;
        revId = revision;
        dynamicRevId = dd.dynamicRevId;
        isForce = dd.isForce;
        isChanging = dd.isChanging;
        isTransitive = dd.isTransitive;
        namespace = dd.namespace;
        confs.putAll(dd.confs);
        excludeRules = (dd.excludeRules == null) ? null : new LinkedHashMap<>(dd.excludeRules);
        includeRules = (dd.includeRules == null) ? null : new LinkedHashMap<>(dd.includeRules);
        dependencyArtifacts = (dd.dependencyArtifacts == null) ? null
                : new LinkedHashMap<>(dd.dependencyArtifacts);
        sourceModule = dd.sourceModule;
    }

    public DefaultDependencyDescriptor(ModuleDescriptor md, ModuleRevisionId mrid, boolean force,
            boolean changing, boolean transitive) {
        this(md, mrid, mrid, force, changing, transitive);
    }

    public DefaultDependencyDescriptor(ModuleRevisionId mrid, boolean force) {
        this(mrid, force, false);
    }

    public DefaultDependencyDescriptor(ModuleRevisionId mrid, boolean force, boolean changing) {
        this(null, mrid, mrid, force, changing, true);
    }

    public DefaultDependencyDescriptor(ModuleDescriptor md, ModuleRevisionId mrid,
            ModuleRevisionId dynamicConstraint, boolean force, boolean changing, boolean transitive) {
        Checks.checkNotNull(mrid, "mrid");
        Checks.checkNotNull(dynamicConstraint, "dynamicConstraint");

        this.md = md;
        revId = mrid;
        dynamicRevId = dynamicConstraint;
        isForce = force;
        isChanging = changing;
        isTransitive = transitive;
        sourceModule = md == null ? null : md.getModuleRevisionId();
    }

    public ModuleId getDependencyId() {
        return getDependencyRevisionId().getModuleId();
    }

    public ModuleRevisionId getDependencyRevisionId() {
        return revId;
    }

    public ModuleRevisionId getDynamicConstraintDependencyRevisionId() {
        return dynamicRevId;
    }

    public String[] getModuleConfigurations() {
        return confs.keySet().toArray(new String[confs.keySet().size()]);
    }

    public String[] getDependencyConfigurations(String moduleConfiguration) {
        return getDependencyConfigurations(moduleConfiguration, moduleConfiguration);
    }

    /**
     * Return the dependency configurations mapped to the given moduleConfiguration, actually
     * resolved because of the given requestedConfiguration
     * <p>
     * Usually requestedConfiguration and moduleConfiguration are the same, except when a conf
     * extends another, then the moduleConfiguration is the configuration currently resolved (the
     * extended one), and requestedConfiguration is the one actually requested initially (the
     * extending one). Both moduleConfiguration and requestedConfiguration are configurations of the
     * caller, the array returned is composed of the required configurations of the dependency
     * described by this descriptor.
     * </p>
     *
     * @param moduleConfiguration String
     * @param requestedConfiguration String
     * @return String[]
     */
    public String[] getDependencyConfigurations(String moduleConfiguration,
            String requestedConfiguration) {
        if (md != null) {
            Configuration c = md.getConfiguration(moduleConfiguration);
            if (c instanceof ConfigurationIntersection) {
                ConfigurationIntersection intersection = (ConfigurationIntersection) c;
                Set<String> intersectedDepConfs = new HashSet<>();
                for (String intersect : intersection.getIntersectedConfigurationNames()) {
                    Collection<String> depConfs = getDependencyConfigurationsIncludingExtending(
                        intersect, requestedConfiguration);
                    if (intersectedDepConfs.isEmpty()) {
                        intersectedDepConfs.addAll(depConfs);
                    } else {
                        if (intersectedDepConfs.contains("*")) {
                            intersectedDepConfs.remove("*");
                            intersectedDepConfs.addAll(depConfs);
                        } else if (depConfs.contains("*")) {
                            // nothing to do, intersection of 'something'
                            // with 'everything' is 'something'
                        } else {
                            Set<String> intersectedDepConfsCopy = intersectedDepConfs;
                            intersectedDepConfs = new HashSet<>();
                            for (String intersectedDepConf : intersectedDepConfsCopy) {
                                if (depConfs.contains(intersectedDepConf)) {
                                    // the conf is present in both sets,
                                    // so it is in the intersection
                                    intersectedDepConfs.add(intersectedDepConf);
                                    continue;
                                }
                                /*
                                 * we do not handle special confs like *!sg or [cond]* in right hand
                                 * confs yet: it would require supporting parenthesis grouping in
                                 * configurations intersection interpretation
                                 *
                                 * for (String depConf : depConfs) {
                                 *    if (depConf.startsWith("*")) {
                                 *       if (intersectedDepConf.contains("(" + depConf + ")")) {
                                 *          intersectedDepConfs.add(intersectedDepConf);
                                 *       } else {
                                 *          intersectedDepConfs.add("(" + intersectedDepConf + ")+(" + depConf + ")");
                                 *       }
                                 *    } else if (intersectedDepConf.startsWith("*")) {
                                 *       if (depConf.contains("(" + intersectedDepConf + ")")) {
                                 *          intersectedDepConfs.add(depConf);
                                 *       } else {
                                 *          intersectedDepConfs.add(depConf + "+" + intersectedDepConf);
                                 *       }
                                 *    }
                                 * }
                                 */
                            }
                        }
                    }
                }
                List<String> confsList = confs.get(moduleConfiguration);
                if (confsList != null) {
                    intersectedDepConfs.addAll(confsList);
                }
                if (intersectedDepConfs.isEmpty()) {
                    List<String> defConfs = confs.get("*");
                    if (defConfs != null) {
                        for (String mappedConf : defConfs) {
                            if (mappedConf != null && mappedConf.startsWith("@+")) {
                                return new String[] {moduleConfiguration + mappedConf.substring(1)};
                            } else if (mappedConf != null && mappedConf.equals("@")) {
                                return new String[] {moduleConfiguration};
                            }
                        }
                    }
                }
                return intersectedDepConfs.toArray(new String[intersectedDepConfs.size()]);
            } else if (c instanceof ConfigurationGroup) {
                ConfigurationGroup group = (ConfigurationGroup) c;
                Set<String> groupDepConfs = new HashSet<>();
                for (String member : group.getMembersConfigurationNames()) {
                    Collection<String> depConfs = getDependencyConfigurationsIncludingExtending(
                        member, requestedConfiguration);
                    groupDepConfs.addAll(depConfs);
                }
                return groupDepConfs.toArray(new String[groupDepConfs.size()]);
            }
        }

        List<String> confsList = confs.get(moduleConfiguration);
        if (confsList == null) {
            // there is no mapping defined for this configuration, add the 'other' mappings.
            confsList = confs.get("%");
        }
        List<String> defConfs = confs.get("*");
        Collection<String> ret = new LinkedHashSet<>();
        if (confsList != null) {
            ret.addAll(confsList);
        }
        if (defConfs != null) {
            ret.addAll(defConfs);

            // Fixes bugs IVY-1547, IVY-982 which have to do with
            // negation (e.g. `*, !foo`) not working on the left side of the maps-to operator.
            List<String> excludedConfs = confs.get("!" + moduleConfiguration);
            if (excludedConfs != null)
                ret.removeAll(excludedConfs);
        }

        Collection<String> replacedRet = new LinkedHashSet<>();
        for (String c : ret) {
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
            StringBuilder r = new StringBuilder("*");
            // merge excluded configurations as one conf like *!A!B
            for (String c : ret) {
                if (c.startsWith("!")) {
                    r.append(c);
                }
            }
            return new String[] {r.toString()};
        }
        return ret.toArray(new String[ret.size()]);
    }

    private Collection<String> getDependencyConfigurationsIncludingExtending(String conf,
            String requestedConfiguration) {
        Set<String> allDepConfs = new LinkedHashSet<>(Arrays.asList(getDependencyConfigurations(conf,
                requestedConfiguration)));

        Collection<Configuration> extendingConfs = Configuration.findConfigurationExtending(conf,
            md.getConfigurations());
        for (Configuration extendingConf : extendingConfs) {
            allDepConfs.addAll(Arrays.asList(
                getDependencyConfigurations(extendingConf.getName(), requestedConfiguration)));
        }
        return allDepConfs;
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
            String mappedConf = moduleConfiguration;
            if (matcher.group(1) != null) {
                mappedConf += matcher.group(1);
            }
            if (matcher.group(2) != null) {
                mappedConf += matcher.group(2);
            }
            return mappedConf;
        }
        return null;
    }

    public String[] getDependencyConfigurations(String[] moduleConfigurations) {
        Set<String> confs = new LinkedHashSet<>();
        for (String moduleConfiguration : moduleConfigurations) {
            confs.addAll(Arrays.asList(getDependencyConfigurations(moduleConfiguration)));
        }
        if (confs.contains("*")) {
            return new String[] {"*"};
        }
        return confs.toArray(new String[confs.size()]);
    }

    public DependencyArtifactDescriptor[] getDependencyArtifacts(String moduleConfiguration) {
        Collection<DependencyArtifactDescriptor> artifacts = getCollectionForConfiguration(
            moduleConfiguration, dependencyArtifacts);
        return artifacts.toArray(new DependencyArtifactDescriptor[artifacts.size()]);
    }

    public IncludeRule[] getIncludeRules(String moduleConfiguration) {
        Collection<IncludeRule> rules = getCollectionForConfiguration(moduleConfiguration,
            includeRules);
        return rules.toArray(new IncludeRule[rules.size()]);
    }

    public ExcludeRule[] getExcludeRules(String moduleConfiguration) {
        Collection<ExcludeRule> rules = getCollectionForConfiguration(moduleConfiguration,
            excludeRules);
        return rules.toArray(new ExcludeRule[rules.size()]);
    }

    private <T> Set<T> getCollectionForConfiguration(String moduleConfiguration,
            Map<String, Collection<T>> collectionMap) {
        if (collectionMap == null || collectionMap.isEmpty()) {
            return Collections.emptySet();
        }
        Collection<T> artifacts = collectionMap.get(moduleConfiguration);
        Collection<T> defArtifacts = collectionMap.get("*");
        Set<T> ret = new LinkedHashSet<>();
        if (artifacts != null) {
            ret.addAll(artifacts);
        }
        if (defArtifacts != null) {
            ret.addAll(defArtifacts);
        }
        return ret;
    }

    public DependencyArtifactDescriptor[] getDependencyArtifacts(String[] moduleConfigurations) {
        Set<DependencyArtifactDescriptor> artifacts = new LinkedHashSet<>();
        for (String moduleConfiguration : moduleConfigurations) {
            artifacts.addAll(Arrays.asList(getDependencyArtifacts(moduleConfiguration)));
        }
        return artifacts.toArray(new DependencyArtifactDescriptor[artifacts.size()]);
    }

    public IncludeRule[] getIncludeRules(String[] moduleConfigurations) {
        Set<IncludeRule> rules = new LinkedHashSet<>();
        for (String moduleConfiguration : moduleConfigurations) {
            rules.addAll(Arrays.asList(getIncludeRules(moduleConfiguration)));
        }
        return rules.toArray(new IncludeRule[rules.size()]);
    }

    public ExcludeRule[] getExcludeRules(String[] moduleConfigurations) {
        Set<ExcludeRule> rules = new LinkedHashSet<>();
        for (String moduleConfiguration : moduleConfigurations) {
            rules.addAll(Arrays.asList(getExcludeRules(moduleConfiguration)));
        }
        return rules.toArray(new ExcludeRule[rules.size()]);
    }

    public DependencyArtifactDescriptor[] getAllDependencyArtifacts() {
        if (dependencyArtifacts == null) {
            return new DependencyArtifactDescriptor[0];
        }
        Set<DependencyArtifactDescriptor> ret = mergeAll(dependencyArtifacts);
        return ret.toArray(new DependencyArtifactDescriptor[ret.size()]);
    }

    public IncludeRule[] getAllIncludeRules() {
        if (includeRules == null) {
            return new IncludeRule[0];
        }
        Set<IncludeRule> ret = mergeAll(includeRules);
        return ret.toArray(new IncludeRule[ret.size()]);
    }

    public ExcludeRule[] getAllExcludeRules() {
        if (excludeRules == null) {
            return new ExcludeRule[0];
        }
        Set<ExcludeRule> ret = mergeAll(excludeRules);
        return ret.toArray(new ExcludeRule[ret.size()]);
    }

    private <T> Set<T> mergeAll(Map<String, Collection<T>> artifactsMap) {
        Set<T> ret = new LinkedHashSet<>();
        for (Collection<T> artifacts : artifactsMap.values()) {
            ret.addAll(artifacts);
        }
        return ret;
    }

    public void addDependencyConfiguration(String masterConf, String depConf) {
        if (md != null && !"*".equals(masterConf) && !"%".equals(masterConf)) {
            Configuration config;
            if (masterConf.startsWith("!")) {
                config = md.getConfiguration(masterConf.substring(1));
            } else {
                config = md.getConfiguration(masterConf);
            }
            if (config == null) {
                throw new IllegalArgumentException("Cannot add dependency '" + revId
                        + "' to configuration '" + masterConf + "' of module "
                        + md.getModuleRevisionId() + " because this configuration doesn't exist!");
            }
            if (config instanceof ConfigurationGroup) {
                ConfigurationGroup group = (ConfigurationGroup) config;
                for (String member : group.getMembersConfigurationNames()) {
                    addDependencyConfiguration(member, depConf);
                }
                return;
            }
        }

        List<String> confsList = confs.get(masterConf);
        if (confsList == null) {
            confsList = new ArrayList<>();
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

    private <T> void addObjectToConfiguration(String callerConf, T toAdd,
            Map<String, Collection<T>> confsMap) {
        Collection<T> col = confsMap.get(callerConf);
        if (col == null) {
            col = new ArrayList<>();
            confsMap.put(callerConf, col);
        }
        col.add(toAdd);
    }

    /**
     * only works when namespace is properly set. The behaviour is not specified if namespace is not
     * set.
     *
     * @param moduleConfigurations String[]
     * @param artifactId ditto
     * @return boolean
     */
    public boolean doesExclude(String[] moduleConfigurations, ArtifactId artifactId) {
        if (namespace != null) {
            artifactId = NameSpaceHelper
                    .transform(artifactId, namespace.getFromSystemTransformer());
        }
        for (ExcludeRule rule : getExcludeRules(moduleConfigurations)) {
            if (MatcherHelper.matches(rule.getMatcher(), rule.getId(), artifactId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if this descriptor contains any exclusion rule
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

    public Map<String, String> getAttributes() {
        return revId.getAttributes();
    }

    public String getExtraAttribute(String attName) {
        return revId.getExtraAttribute(attName);
    }

    public Map<String, String> getExtraAttributes() {
        return revId.getExtraAttributes();
    }

    public Map<String, String> getQualifiedExtraAttributes() {
        return revId.getQualifiedExtraAttributes();
    }

    public DependencyDescriptor asSystem() {
        return asSystem;
    }

    private void setDependencyArtifacts(
            Map<String, Collection<DependencyArtifactDescriptor>> dependencyArtifacts) {
        this.dependencyArtifacts = dependencyArtifacts;
    }

    private Map<String, Collection<DependencyArtifactDescriptor>> getDependencyArtifacts() {
        if (dependencyArtifacts == null) {
            dependencyArtifacts = new LinkedHashMap<>();
        }
        return dependencyArtifacts;
    }

    private void setIncludeRules(Map<String, Collection<IncludeRule>> includeRules) {
        this.includeRules = includeRules;
    }

    private Map<String, Collection<IncludeRule>> getIncludeRules() {
        if (includeRules == null) {
            includeRules = new LinkedHashMap<>();
        }
        return includeRules;
    }

    private void setExcludeRules(Map<String, Collection<ExcludeRule>> excludeRules) {
        this.excludeRules = excludeRules;
    }

    private Map<String, Collection<ExcludeRule>> getExcludeRules() {
        if (excludeRules == null) {
            excludeRules = new LinkedHashMap<>();
        }
        return excludeRules;
    }

    public ModuleRevisionId getSourceModule() {
        return sourceModule;
    }

    public DependencyDescriptor clone(ModuleRevisionId revision) {
        return new DefaultDependencyDescriptor(this, revision);
    }
}
