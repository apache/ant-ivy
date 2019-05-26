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

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ivy.core.module.id.ArtifactId;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.module.id.ModuleRules;
import org.apache.ivy.core.module.status.StatusManager;
import org.apache.ivy.plugins.conflict.ConflictManager;
import org.apache.ivy.plugins.matcher.MapMatcher;
import org.apache.ivy.plugins.matcher.MatcherHelper;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.ivy.plugins.namespace.NameSpaceHelper;
import org.apache.ivy.plugins.namespace.Namespace;
import org.apache.ivy.plugins.namespace.NamespaceTransformer;
import org.apache.ivy.plugins.parser.ModuleDescriptorParser;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorParser;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorWriter;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.plugins.version.VersionMatcher;
import org.apache.ivy.util.Message;

import static org.apache.ivy.core.module.descriptor.Configuration.Visibility.PUBLIC;

import static org.apache.ivy.core.module.descriptor.Configuration.findConfigurationExtending;

/**
 *
 */
public class DefaultModuleDescriptor implements ModuleDescriptor {

    public static DefaultModuleDescriptor newDefaultInstance(ModuleRevisionId mrid) {
        return newDefaultInstance(mrid, null);
    }

    public static DefaultModuleDescriptor newCallerInstance(ModuleRevisionId mrid, String[] confs,
            boolean transitive, boolean changing) {
        DefaultModuleDescriptor moduleDescriptor = new DefaultModuleDescriptor(
                ModuleRevisionId.newInstance(mrid.getOrganisation(), mrid.getName() + "-caller",
                    "working"), "integration", null, true);
        for (String conf : confs) {
            moduleDescriptor.addConfiguration(new Configuration(conf));
        }
        moduleDescriptor.setLastModified(System.currentTimeMillis());
        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(moduleDescriptor, mrid,
                true, changing, transitive);
        for (String conf : confs) {
            dd.addDependencyConfiguration(conf, conf);
        }
        moduleDescriptor.addDependency(dd);

        return moduleDescriptor;
    }

    public static DefaultModuleDescriptor newCallerInstance(ModuleRevisionId[] mrids,
            boolean transitive, boolean changing) {
        DefaultModuleDescriptor moduleDescriptor = new DefaultModuleDescriptor(
                ModuleRevisionId.newInstance("caller", "all-caller", "working"), "integration",
                null, true);
        moduleDescriptor.addConfiguration(new Configuration(DEFAULT_CONFIGURATION));
        moduleDescriptor.setLastModified(System.currentTimeMillis());
        for (ModuleRevisionId mrid : mrids) {
            DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(moduleDescriptor,
                    mrid, true, changing, transitive);
            dd.addDependencyConfiguration(DEFAULT_CONFIGURATION, "*");
            moduleDescriptor.addDependency(dd);
        }

        return moduleDescriptor;
    }

    public static DefaultModuleDescriptor newDefaultInstance(ModuleRevisionId mrid,
            DependencyArtifactDescriptor[] artifacts) {
        DefaultModuleDescriptor moduleDescriptor = new DefaultModuleDescriptor(mrid, "release",
                null, true);
        moduleDescriptor.addConfiguration(new Configuration(DEFAULT_CONFIGURATION));
        if (artifacts != null && artifacts.length > 0) {
            for (DependencyArtifactDescriptor artifact : artifacts) {
                moduleDescriptor.addArtifact(DEFAULT_CONFIGURATION,
                    new MDArtifact(moduleDescriptor, artifact.getName(), artifact.getType(),
                            artifact.getExt(), artifact.getUrl(), artifact.getExtraAttributes()));
            }
        } else {
            moduleDescriptor.addArtifact(DEFAULT_CONFIGURATION,
                new MDArtifact(moduleDescriptor, mrid.getName(), "jar", "jar"));
        }
        moduleDescriptor.setLastModified(System.currentTimeMillis());
        return moduleDescriptor;
    }

    public static DefaultModuleDescriptor newBasicInstance(ModuleRevisionId mrid,
            Date publicationDate) {
        DefaultModuleDescriptor moduleDescriptor = new DefaultModuleDescriptor(mrid, "release",
                publicationDate, false);
        moduleDescriptor.addConfiguration(new Configuration(DEFAULT_CONFIGURATION));
        moduleDescriptor.addArtifact(DEFAULT_CONFIGURATION,
            new MDArtifact(moduleDescriptor, mrid.getName(), "jar", "jar"));
        return moduleDescriptor;
    }

    /**
     * Transforms the given module descriptor of the given namespace and return a new module
     * descriptor in the system namespace. <i>Note that dependency exclude rules are not converted
     * in system namespace, because they aren't transformable (the namespace lacks the ability to
     * convert regular expressions)</i>
     *
     * @param md ModuleDescriptor
     * @param ns Namespace
     * @return ModuleDescriptor
     */
    public static ModuleDescriptor transformInstance(ModuleDescriptor md, Namespace ns) {
        NamespaceTransformer t = ns.getToSystemTransformer();
        if (t.isIdentity()) {
            return md;
        }
        DefaultModuleDescriptor nmd = new DefaultModuleDescriptor(md.getParser(), md.getResource());
        nmd.revId = t.transform(md.getModuleRevisionId());
        nmd.resolvedRevId = t.transform(md.getResolvedModuleRevisionId());
        nmd.status = md.getStatus();
        nmd.publicationDate = md.getPublicationDate();
        nmd.resolvedPublicationDate = md.getResolvedPublicationDate();

        for (ExtendsDescriptor ed : md.getInheritedDescriptors()) {
            ModuleDescriptor parentMd = ed.getParentMd();
            DefaultModuleDescriptor parentNmd = new DefaultModuleDescriptor(parentMd.getParser(),
                    parentMd.getResource());
            parentNmd.revId = t.transform(parentMd.getModuleRevisionId());
            parentNmd.resolvedRevId = t.transform(parentMd.getResolvedModuleRevisionId());
            parentNmd.status = parentMd.getStatus();
            parentNmd.publicationDate = parentMd.getPublicationDate();
            parentNmd.resolvedPublicationDate = parentMd.getResolvedPublicationDate();

            nmd.inheritedDescriptors.add(new DefaultExtendsDescriptor(parentNmd, ed.getLocation(),
                    ed.getExtendsTypes()));
        }

        for (DependencyDescriptor dd : md.getDependencies()) {
            nmd.dependencies.add(NameSpaceHelper.toSystem(dd, ns));
        }

        for (Configuration conf : md.getConfigurations()) {
            final String confName = conf.getName();
            nmd.configurations.put(confName, conf);
            for (Artifact art : md.getArtifacts(confName)) {
                nmd.addArtifact(confName, NameSpaceHelper.transform(art, t));
            }
        }
        nmd.setDefault(md.isDefault());
        if (md instanceof DefaultModuleDescriptor) {
            DefaultModuleDescriptor dmd = (DefaultModuleDescriptor) md;
            nmd.conflictManagers = dmd.conflictManagers.clone();
            nmd.dependencyDescriptorMediators = dmd.dependencyDescriptorMediators.clone();
        } else {
            Message.warn("transformed module descriptor is not a default module descriptor: "
                    + "impossible to copy conflict manager and version mediation configuration: "
                    + md);
        }
        nmd.licenses.addAll(Arrays.asList(md.getLicenses()));
        nmd.homePage = md.getHomePage();
        nmd.description = md.getDescription();
        nmd.lastModified = md.getLastModified();
        nmd.extraAttributesNamespaces = md.getExtraAttributesNamespaces();
        nmd.extraInfos = md.getExtraInfos();
        nmd.namespace = ns;

        return nmd;
    }

    private ModuleRevisionId revId;

    private ModuleRevisionId resolvedRevId;

    private String status = StatusManager.getCurrent().getDefaultStatus();

    private Date publicationDate;

    private Date resolvedPublicationDate;

    private List<DependencyDescriptor> dependencies = new ArrayList<>();

    private Map<String, Configuration> configurations = new LinkedHashMap<>();

    private Map<String, Collection<Artifact>> artifactsByConf = new HashMap<>();

    private Collection<Artifact> artifacts = new LinkedHashSet<>();

    // all artifacts could also be found in the artifactsByConf map, but here we can
    // preserve the order

    private boolean isDefault = false;

    private ModuleRules<ConflictManager> conflictManagers = new ModuleRules<>();

    private ModuleRules<DependencyDescriptorMediator> dependencyDescriptorMediators = new ModuleRules<>();

    private List<License> licenses = new ArrayList<>();

    private String homePage;

    private String description = "";

    private long lastModified = 0;

    private Namespace namespace;

    private String defaultConf;

    private String defaultConfMapping;

    private boolean mappingOverride;

    private ModuleDescriptorParser parser;

    private Resource resource;

    private List<ExcludeRule> excludeRules = new ArrayList<>();

    private Artifact metadataArtifact;

    private List<ExtendsDescriptor> inheritedDescriptors = new ArrayList<>();

    private Map<String, String> extraAttributesNamespaces = new LinkedHashMap<>();

    private List<ExtraInfoHolder> extraInfos = new ArrayList<>();

    public DefaultModuleDescriptor(ModuleRevisionId id, String status, Date pubDate) {
        this(id, status, pubDate, false);
    }

    public DefaultModuleDescriptor(ModuleRevisionId id, String status, Date pubDate,
            boolean isDefault) {
        if (id == null) {
            throw new NullPointerException("null module revision id not allowed");
        }
        if (status == null) {
            throw new NullPointerException("null status not allowed");
        }
        this.revId = id;
        this.resolvedRevId = id;
        this.status = status;
        this.publicationDate = pubDate;
        this.resolvedPublicationDate = publicationDate == null ? new Date() : publicationDate;
        this.isDefault = isDefault;
        this.parser = XmlModuleDescriptorParser.getInstance();
    }

    /**
     * IMPORTANT : at least call setModuleRevisionId and setResolvedPublicationDate with instances
     * created by this constructor !
     *
     * @param parser ModuleDescriptorParser
     * @param res Resource
     */
    public DefaultModuleDescriptor(ModuleDescriptorParser parser, Resource res) {
        this.parser = parser;
        resource = res;
    }

    public Artifact getMetadataArtifact() {
        if (metadataArtifact == null) {
            metadataArtifact = DefaultArtifact.newIvyArtifact(resolvedRevId,
                resolvedPublicationDate);
        }
        return metadataArtifact;
    }

    public void setModuleArtifact(Artifact moduleArtifact) {
        this.metadataArtifact = moduleArtifact;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setPublicationDate(Date publicationDate) {
        this.publicationDate = publicationDate;
        if (resolvedPublicationDate == null) {
            resolvedPublicationDate = publicationDate == null ? new Date() : publicationDate;
        }
    }

    public Date getPublicationDate() {
        return publicationDate;
    }

    public void setResolvedPublicationDate(Date publicationDate) {
        if (publicationDate == null) {
            throw new NullPointerException("null publication date not allowed");
        }
        resolvedPublicationDate = publicationDate;
    }

    public Date getResolvedPublicationDate() {
        return resolvedPublicationDate;
    }

    public String getRevision() {
        return getResolvedModuleRevisionId().getRevision();
    }

    public void setModuleRevisionId(ModuleRevisionId revId) {
        if (revId == null) {
            throw new NullPointerException("null module revision id not allowed");
        }
        this.revId = revId;
        if (resolvedRevId == null) {
            resolvedRevId = revId;
        }
    }

    public void setResolvedModuleRevisionId(ModuleRevisionId revId) {
        resolvedRevId = revId;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void addInheritedDescriptor(ExtendsDescriptor descriptor) {
        inheritedDescriptors.add(descriptor);
    }

    public void addDependency(DependencyDescriptor dependency) {
        dependencies.add(dependency);
    }

    public void addConfiguration(Configuration conf) {
        configurations.put(conf.getName(), conf);
    }

    /**
     * Artifact configurations are not used since added artifact may not be entirely completed, so
     * its configurations data may not be accurate
     *
     * @param conf ditto
     * @param artifact ditto
     */
    public void addArtifact(String conf, Artifact artifact) {
        Configuration c = getConfiguration(conf);
        if (c == null) {
            throw new IllegalArgumentException("Cannot add artifact '"
                    + artifact.getId().getArtifactId().getShortDescription()
                    + "' to configuration '" + conf + "' of module " + revId
                    + " because this configuration doesn't exist!");
        }
        if (c instanceof ConfigurationGroup) {
            ConfigurationGroup group = (ConfigurationGroup) c;
            for (String member : group.getMembersConfigurationNames()) {
                addArtifact(member, artifact);
            }
        } else {
            Collection<Artifact> artifacts = artifactsByConf.get(conf);
            if (artifacts == null) {
                artifacts = new ArrayList<>();
                artifactsByConf.put(conf, artifacts);
            }
            artifacts.add(artifact);
            this.artifacts.add(artifact);
        }
    }

    public ModuleRevisionId getModuleRevisionId() {
        return revId;
    }

    public ModuleRevisionId getResolvedModuleRevisionId() {
        return resolvedRevId;
    }

    public String getStatus() {
        return status;
    }

    public ExtendsDescriptor[] getInheritedDescriptors() {
        return inheritedDescriptors.toArray(new ExtendsDescriptor[inheritedDescriptors.size()]);
    }

    public Configuration[] getConfigurations() {
        return configurations.values().toArray(new Configuration[configurations.size()]);
    }

    public String[] getConfigurationsNames() {
        return configurations.keySet().toArray(new String[configurations.size()]);
    }

    public String[] getPublicConfigurationsNames() {
        List<String> ret = new ArrayList<>();
        for (Configuration conf : configurations.values()) {
            if (PUBLIC.equals(conf.getVisibility())) {
                ret.add(conf.getName());
            }
        }
        return ret.toArray(new String[ret.size()]);
    }

    /**
     * @param confName String
     * @return the configuration object with the given name in the current module descriptor, null
     * if not found.
     */
    public Configuration getConfiguration(String confName) {
        Configuration configuration = configurations.get(confName);
        if (configuration == null && confName != null) {
            // let's first check if the configuration is a conf group
            Matcher m = Pattern.compile("\\*\\[([^=]+)\\=([^\\]]+)\\]").matcher(confName);
            if (m.matches()) {
                String attName = m.group(1);
                String attValue = m.group(2);

                // this is a conf group, let's search for its members
                Map<String, Configuration> members = new LinkedHashMap<>();
                for (Configuration conf : configurations.values()) {
                    if (attValue.equals(conf.getAttribute(attName))) {
                        members.put(conf.getName(), conf);
                    }
                }
                return new ConfigurationGroup(confName, members);
            }

            // let's see if a configuration intersection is requested
            String[] confs = confName.split("\\+");
            if (confs.length <= 1) {
                return null;
            }
            Map<String, Configuration> intersectedConfs = new LinkedHashMap<>();
            for (String conf : confs) {
                Configuration c = configurations.get(conf);
                if (c == null) {
                    Message.verbose("missing configuration '" + conf + "' from intersection "
                            + confName + " in " + this);
                    return null;
                }
                intersectedConfs.put(conf, c);
            }
            return new ConfigurationIntersection(confName, intersectedConfs);
        }
        return configuration;
    }

    public Artifact[] getArtifacts(String conf) {
        Configuration c = getConfiguration(conf);
        if (c == null) {
            return new Artifact[0];
        }
        Collection<Artifact> artifacts = artifactsByConf.get(conf);
        if (c instanceof ConfigurationIntersection) {
            ConfigurationIntersection intersection = (ConfigurationIntersection) c;
            Set<Artifact> intersectedArtifacts = new LinkedHashSet<>();
            for (String intersect : intersection.getIntersectedConfigurationNames()) {
                Collection<Artifact> arts = getArtifactsIncludingExtending(intersect);
                if (intersectedArtifacts.isEmpty()) {
                    intersectedArtifacts.addAll(arts);
                } else {
                    intersectedArtifacts.retainAll(arts);
                }
            }
            if (artifacts != null) {
                intersectedArtifacts.addAll(artifacts);
            }
            return intersectedArtifacts.toArray(new Artifact[intersectedArtifacts.size()]);
        } else if (c instanceof ConfigurationGroup) {
            ConfigurationGroup group = (ConfigurationGroup) c;
            Set<Artifact> groupArtifacts = new LinkedHashSet<>();
            for (String member : group.getMembersConfigurationNames()) {
                groupArtifacts.addAll(getArtifactsIncludingExtending(member));
            }
            if (artifacts != null) {
                groupArtifacts.addAll(artifacts);
            }
            return groupArtifacts.toArray(new Artifact[groupArtifacts.size()]);
        } else {
            if (artifacts == null) {
                return new Artifact[0];
            } else {
                return artifacts.toArray(new Artifact[artifacts.size()]);
            }
        }
    }

    private Collection<Artifact> getArtifactsIncludingExtending(String conf) {
        Set<Artifact> artifacts = new LinkedHashSet<>();
        Collection<Artifact> arts = artifactsByConf.get(conf);
        if (arts != null) {
            artifacts.addAll(arts);
        }
        for (Configuration extendingConf : findConfigurationExtending(conf, getConfigurations())) {
            arts = artifactsByConf.get(extendingConf.getName());
            if (arts != null) {
                artifacts.addAll(arts);
            }
        }
        return artifacts;
    }

    public Artifact[] getAllArtifacts() {
        return artifacts.toArray(new Artifact[artifacts.size()]);
    }

    public DependencyDescriptor[] getDependencies() {
        return dependencies.toArray(new DependencyDescriptor[dependencies.size()]);
    }

    public boolean dependsOn(VersionMatcher matcher, ModuleDescriptor md) {
        for (DependencyDescriptor dd : dependencies) {
            if (dd.getDependencyId().equals(md.getModuleRevisionId().getModuleId())) {
                if (md.getResolvedModuleRevisionId().getRevision() == null) {
                    return true;
                } else if (matcher.accept(dd.getDependencyRevisionId(), md)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void toIvyFile(File destFile) throws ParseException, IOException {
        if (parser != null && resource != null) {
            parser.toIvyFile(resource.openStream(), resource, destFile, this);
        } else {
            XmlModuleDescriptorWriter.write(this, destFile);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((revId == null) ? 0 : revId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DefaultModuleDescriptor)) {
            return false;
        }
        DefaultModuleDescriptor other = (DefaultModuleDescriptor) obj;
        return revId == null ? other.revId == null : revId.equals(other.revId);
    }

    @Override
    public String toString() {
        return "module: " + revId + " status=" + status + " publication=" + publicationDate
                + " configurations=" + configurations + " artifacts=" + artifactsByConf
                + " dependencies=" + dependencies;
    }

    public void setDefault(boolean b) {
        isDefault = b;
    }

    /**
     * regular expressions as explained in Pattern class may be used in ModuleId organisation and
     * name
     *
     * @param moduleId ditto
     * @param matcher PatternMatcher
     * @param manager ConflictManager
     */
    public void addConflictManager(ModuleId moduleId, PatternMatcher matcher,
            ConflictManager manager) {
        conflictManagers.defineRule(new MapMatcher(moduleId.getAttributes(), matcher), manager);
    }

    public ConflictManager getConflictManager(ModuleId moduleId) {
        return conflictManagers.getRule(moduleId);
    }

    public void addDependencyDescriptorMediator(ModuleId moduleId, PatternMatcher matcher,
            DependencyDescriptorMediator ddm) {
        dependencyDescriptorMediators.defineRule(new MapMatcher(moduleId.getAttributes(), matcher),
            ddm);
    }

    public DependencyDescriptor mediate(DependencyDescriptor dd) {
        for (DependencyDescriptorMediator mediator : dependencyDescriptorMediators.getRules(dd.getDependencyId())) {
            dd = mediator.mediate(dd);
        }
        return dd;
    }

    public ModuleRules<DependencyDescriptorMediator> getAllDependencyDescriptorMediators() {
        return dependencyDescriptorMediators.clone();
    }

    public void addLicense(License license) {
        licenses.add(license);
    }

    public License[] getLicenses() {
        return licenses.toArray(new License[licenses.size()]);
    }

    public String getHomePage() {
        return homePage;
    }

    public void setHomePage(String homePage) {
        this.homePage = homePage;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public Namespace getNamespace() {
        return namespace;
    }

    public boolean isNamespaceUseful() {
        for (DependencyDescriptor dd : dependencies) {
            if (dd.getAllExcludeRules().length > 0) {
                return true;
            }
        }
        return false;
    }

    public void setNamespace(Namespace ns) {
        namespace = ns;
    }

    /**
     * Throws an exception if the module descriptor is inconsistent. For the moment, only extended
     * configurations existence and cycles are checked
     */
    public void check() {
        Stack<String> confs = new Stack<>();
        for (Configuration conf : configurations.values()) {
            for (String ext : conf.getExtends()) {
                confs.push(conf.getName());
                checkConf(confs, ext.trim());
                confs.pop();
            }
        }
    }

    private void checkConf(Stack<String> confs, String confName) {
        int index = confs.indexOf(confName);
        if (index != -1) {
            StringBuilder cycle = new StringBuilder();
            for (; index < confs.size(); index++) {
                cycle.append(confs.get(index)).append(" => ");
            }
            cycle.append(confName);
            throw new IllegalStateException("illegal cycle detected in configuration extension: "
                    + cycle);
        }
        Configuration conf = getConfiguration(confName);
        if (conf == null) {
            throw new IllegalStateException("unknown configuration '" + confName
                    + "'. It is extended by " + confs.get(confs.size() - 1));
        }
        for (String ext : conf.getExtends()) {
            confs.push(conf.getName());
            checkConf(confs, ext.trim());
            confs.pop();
        }
    }

    public String getDefaultConf() {
        return defaultConf;
    }

    public void setDefaultConf(String defaultConf) {
        this.defaultConf = defaultConf;
    }

    public String getDefaultConfMapping() {
        return defaultConfMapping;
    }

    public void setDefaultConfMapping(String defaultConfMapping) {
        this.defaultConfMapping = defaultConfMapping;
    }

    public void setMappingOverride(boolean override) {
        mappingOverride = override;
    }

    public boolean isMappingOverride() {
        return mappingOverride;
    }

    public String getAttribute(String attName) {
        return resolvedRevId.getAttribute(attName);
    }

    public Map<String, String> getAttributes() {
        return resolvedRevId.getAttributes();
    }

    public String getExtraAttribute(String attName) {
        return resolvedRevId.getExtraAttribute(attName);
    }

    public Map<String, String> getExtraAttributes() {
        return resolvedRevId.getExtraAttributes();
    }

    public Map<String, String> getQualifiedExtraAttributes() {
        return resolvedRevId.getQualifiedExtraAttributes();
    }

    public ModuleDescriptorParser getParser() {
        return parser;
    }

    public Resource getResource() {
        return resource;
    }

    public void addExcludeRule(ExcludeRule rule) {
        excludeRules.add(rule);
    }

    public boolean canExclude() {
        return !excludeRules.isEmpty();
    }

    /**
     * Only works when namespace is properly set. The behaviour is not specified if namespace is
     * not set.
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

    public ExcludeRule[] getAllExcludeRules() {
        return excludeRules.toArray(new ExcludeRule[excludeRules.size()]);
    }

    public ExcludeRule[] getExcludeRules(String[] moduleConfigurations) {
        Set<ExcludeRule> rules = new LinkedHashSet<>();
        for (ExcludeRule rule : excludeRules) {
            String[] ruleConfs = rule.getConfigurations();
            if (containsAny(ruleConfs, moduleConfigurations)) {
                rules.add(rule);
            }
        }
        return rules.toArray(new ExcludeRule[rules.size()]);
    }

    private boolean containsAny(String[] arr1, String[] arr2) {
        return new ArrayList<>(Arrays.asList(arr1)).removeAll(Arrays.asList(arr2));
    }

    public Map<String, String> getExtraAttributesNamespaces() {
        return extraAttributesNamespaces;
    }

    public void addExtraAttributeNamespace(String prefix, String namespace) {
        extraAttributesNamespaces.put(prefix, namespace);
    }

    @Deprecated
    public void addExtraInfo(String infoKey, String value) {
        extraInfos.add(new ExtraInfoHolder(infoKey, value));
    }

    @Deprecated
    public Map<String, String> getExtraInfo() {
        Map<String, String> map = new HashMap<>();
        for (ExtraInfoHolder extraInfo : extraInfos) {
            populateExtraInfoMap(map, extraInfo);
        }
        return map;
    }

    private void populateExtraInfoMap(Map<String, String> map, ExtraInfoHolder extraInfo) {
        map.put(extraInfo.getName(), extraInfo.getContent());
        for (ExtraInfoHolder nested : extraInfo.getNestedExtraInfoHolder()) {
            populateExtraInfoMap(map, nested);
        }
    }

    public List<ExtraInfoHolder> getExtraInfos() {
        return extraInfos;
    }

    public void addExtraInfo(ExtraInfoHolder extraInfo) {
        extraInfos.add(extraInfo);
    }

    public String getExtraInfoContentByTagName(String tagName) {
        ExtraInfoHolder extraInfoByTagName = getExtraInfoByTagName(tagName);
        if (extraInfoByTagName != null) {
            return extraInfoByTagName.getContent();
        }
        return null;
    }

    public ExtraInfoHolder getExtraInfoByTagName(String tagName) {
        for (ExtraInfoHolder extraInfoHolder : extraInfos) {
            if (extraInfoHolder.getName().equals(tagName)) {
                return extraInfoHolder;
            }
        }
        return null;
    }
}
