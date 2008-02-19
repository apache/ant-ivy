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

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ivy.core.module.id.ArtifactId;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.module.status.StatusManager;
import org.apache.ivy.plugins.conflict.ConflictManager;
import org.apache.ivy.plugins.matcher.MatcherHelper;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.ivy.plugins.namespace.NameSpaceHelper;
import org.apache.ivy.plugins.namespace.Namespace;
import org.apache.ivy.plugins.namespace.NamespaceTransformer;
import org.apache.ivy.plugins.parser.ModuleDescriptorParser;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorWriter;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.plugins.version.VersionMatcher;
import org.apache.ivy.util.Message;

/**
 *
 */
public class DefaultModuleDescriptor implements ModuleDescriptor {

    public static DefaultModuleDescriptor newDefaultInstance(ModuleRevisionId mrid) {
        return newDefaultInstance(mrid, null);
    }

    public static DefaultModuleDescriptor newCallerInstance(ModuleRevisionId mrid, String[] confs,
            boolean transitive, boolean changing) {
        DefaultModuleDescriptor moduleDescriptor = new DefaultModuleDescriptor(ModuleRevisionId
                .newInstance(mrid.getOrganisation(), mrid.getName() + "-caller", "working"),
                "integration", null, true);
        for (int i = 0; i < confs.length; i++) {
            moduleDescriptor.addConfiguration(new Configuration(confs[i]));
        }
        moduleDescriptor.setLastModified(System.currentTimeMillis());
        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(moduleDescriptor, mrid,
                true, changing, transitive);
        for (int j = 0; j < confs.length; j++) {
            dd.addDependencyConfiguration(confs[j], confs[j]);
        }
        moduleDescriptor.addDependency(dd);

        return moduleDescriptor;
    }

    public static DefaultModuleDescriptor newCallerInstance(ModuleRevisionId[] mrid,
            boolean transitive, boolean changing) {
        DefaultModuleDescriptor moduleDescriptor = new DefaultModuleDescriptor(ModuleRevisionId
                .newInstance("caller", "all-caller", "working"), "integration", null, true);
        moduleDescriptor.addConfiguration(new Configuration(DEFAULT_CONFIGURATION));
        moduleDescriptor.setLastModified(System.currentTimeMillis());
        for (int i = 0; i < mrid.length; i++) {
            DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(moduleDescriptor,
                    mrid[i], true, changing, transitive);
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
            for (int i = 0; i < artifacts.length; i++) {
                moduleDescriptor.addArtifact(DEFAULT_CONFIGURATION, new MDArtifact(
                        moduleDescriptor, artifacts[i].getName(), artifacts[i].getType(),
                        artifacts[i].getExt(), artifacts[i].getUrl(), null));
            }
        } else {
            moduleDescriptor.addArtifact(DEFAULT_CONFIGURATION, new MDArtifact(moduleDescriptor,
                    mrid.getName(), "jar", "jar"));
        }
        moduleDescriptor.setLastModified(System.currentTimeMillis());
        return moduleDescriptor;
    }

    public static DefaultModuleDescriptor newBasicInstance(ModuleRevisionId mrid,
            Date publicationDate) {
        DefaultModuleDescriptor moduleDescriptor = new DefaultModuleDescriptor(mrid, "release",
                publicationDate, false);
        moduleDescriptor.addConfiguration(new Configuration(DEFAULT_CONFIGURATION));
        moduleDescriptor.addArtifact(DEFAULT_CONFIGURATION, new MDArtifact(moduleDescriptor, mrid
                .getName(), "jar", "jar"));
        return moduleDescriptor;
    }

    /**
     * Transforms the given module descriptor of the given namespace and return a new module
     * descriptor in the system namespace. <i>Note that dependency exclude rules are not converted
     * in system namespace, because they aren't transformable (the name space hasn't the ability to
     * convert regular expressions)</i>
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
        DefaultModuleDescriptor nmd = new DefaultModuleDescriptor(md.getParser(), md.getResource());
        nmd.revId = t.transform(md.getModuleRevisionId());
        nmd.resolvedRevId = t.transform(md.getResolvedModuleRevisionId());
        nmd.status = md.getStatus();
        nmd.publicationDate = md.getPublicationDate();
        nmd.resolvedPublicationDate = md.getResolvedPublicationDate();
        DependencyDescriptor[] dd = md.getDependencies();
        for (int i = 0; i < dd.length; i++) {
            nmd.dependencies.add(NameSpaceHelper.toSystem(dd[i], ns));
        }
        Configuration[] confs = md.getConfigurations();
        for (int i = 0; i < confs.length; i++) {
            nmd.configurations.put(confs[i].getName(), confs[i]);
            Artifact[] arts = md.getArtifacts(confs[i].getName());
            for (int j = 0; j < arts.length; j++) {
                nmd.addArtifact(confs[i].getName(), NameSpaceHelper.transform(arts[j], t));
            }
        }
        nmd.setDefault(md.isDefault());
        if (md instanceof DefaultModuleDescriptor) {
            DefaultModuleDescriptor dmd = (DefaultModuleDescriptor) md;
            nmd.conflictManagers.putAll(dmd.conflictManagers);
        } else {
            Message.warn(
                "transformed module descriptor is not a default module descriptor: "
                + "impossible to copy conflict manager configuration: " + md);
        }
        nmd.licenses.addAll(Arrays.asList(md.getLicenses()));
        nmd.homePage = md.getHomePage();
        nmd.lastModified = md.getLastModified();
        nmd.extraAttributesNamespaces = md.getExtraAttributesNamespaces();
        nmd.extraInfo = md.getExtraInfo();
        nmd.namespace = ns;

        return nmd;
    }

    private ModuleRevisionId revId;

    private ModuleRevisionId resolvedRevId;

    private String status = StatusManager.getCurrent().getDefaultStatus();

    private Date publicationDate;

    private Date resolvedPublicationDate;

    private List dependencies = new ArrayList(); // List (DependencyDescriptor)

    private Map configurations = new LinkedHashMap(); // Map(String conf -> Configuration)

    private Map artifactsByConf = new HashMap(); // Map (String conf -> Collection(Artifact))

    private Collection artifacts = new LinkedHashSet(); // Collection(Artifact) 
    // all artifacts could also be found in the artifactsByConf map, but here we can
    // preserve the order

    private boolean isDefault = false;

    private Map conflictManagers = new LinkedHashMap(); // Map (ModuleId -> )

    private List licenses = new ArrayList(); // List(License)

    private String homePage;

    private long lastModified = 0;

    private Namespace namespace;

    private boolean mappingOverride;

    private ModuleDescriptorParser parser;

    private Resource resource;
    
    private List excludeRules = new ArrayList(); // List(ExcludeRule)

    private Artifact metadataArtifact;
    
    private Map/*<String,String>*/ extraAttributesNamespaces = new LinkedHashMap();

    private Map/*<String,String>*/ extraInfo = new HashMap();

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
        revId = id;
        resolvedRevId = id;
        this.status = status;
        publicationDate = pubDate;
        resolvedPublicationDate = publicationDate == null ? new Date() : publicationDate;
        this.isDefault = isDefault;
    }

    /**
     * IMPORTANT : at least call setModuleRevisionId and setResolvedPublicationDate with instances
     * created by this constructor !
     */
    public DefaultModuleDescriptor(ModuleDescriptorParser parser, Resource res) {
        this.parser = parser;
        resource = res;
    }
    
    public Artifact getMetadataArtifact() {
        if (metadataArtifact == null) {
            metadataArtifact = DefaultArtifact.newIvyArtifact(
                resolvedRevId, resolvedPublicationDate);
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
     * @param conf
     * @param artifact
     */
    public void addArtifact(String conf, Artifact artifact) {
        if (!configurations.containsKey(conf)) {
            throw new IllegalArgumentException("Configuration '" + conf
                    + "' doesn't exist in module " + this);
        }

        Collection artifacts = (Collection) artifactsByConf.get(conf);
        if (artifacts == null) {
            artifacts = new ArrayList();
            artifactsByConf.put(conf, artifacts);
        }
        artifacts.add(artifact);
        this.artifacts.add(artifact);
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

    public Configuration[] getConfigurations() {
        return (Configuration[]) configurations.values().toArray(
            new Configuration[configurations.size()]);
    }

    public String[] getConfigurationsNames() {
        return (String[]) configurations.keySet().toArray(new String[configurations.size()]);
    }

    public String[] getPublicConfigurationsNames() {
        List ret = new ArrayList();
        for (Iterator iter = configurations.values().iterator(); iter.hasNext();) {
            Configuration conf = (Configuration) iter.next();
            if (conf.getVisibility() == Configuration.Visibility.PUBLIC) {
                ret.add(conf.getName());
            }
        }
        return (String[]) ret.toArray(new String[ret.size()]);
    }

    /**
     * Returns the configuration object with the given name in the current module descriptor, null
     * if not found.
     */
    public Configuration getConfiguration(String confName) {
        return (Configuration) configurations.get(confName);
    }

    public Artifact[] getArtifacts(String conf) {
        Collection artifacts = (Collection) artifactsByConf.get(conf);
        if (artifacts == null) {
            return new Artifact[0];
        } else {
            return (Artifact[]) artifacts.toArray(new Artifact[artifacts.size()]);
        }
    }

    public Artifact[] getAllArtifacts() {
        return (Artifact[]) artifacts.toArray(new Artifact[artifacts.size()]);
    }

    public DependencyDescriptor[] getDependencies() {
        return (DependencyDescriptor[]) dependencies
                .toArray(new DependencyDescriptor[dependencies.size()]);
    }

    public boolean dependsOn(VersionMatcher matcher, ModuleDescriptor md) {
        for (Iterator iter = dependencies.iterator(); iter.hasNext();) {
            DependencyDescriptor dd = (DependencyDescriptor) iter.next();
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

    public String toString() {
        return "module: " + revId + " status=" + status + " publication=" + publicationDate
                + " configurations=" + configurations + " artifacts=" + artifactsByConf
                + " dependencies=" + dependencies;
    }

    public void setDefault(boolean b) {
        isDefault = b;
    }

    private static class ModuleIdMatcher {
        private PatternMatcher matcher;

        private ModuleId mid;

        public ModuleIdMatcher(PatternMatcher matcher, ModuleId mid) {
            this.matcher = matcher;
            this.mid = mid;
        }

        public boolean matches(ModuleId mid) {
            return MatcherHelper.matches(matcher, this.mid, mid);
        }
    }

    /**
     * regular expressions as explained in Pattern class may be used in ModuleId organisation and
     * name
     * 
     * @param moduleId
     * @param matcher
     * @param resolverName
     */
    public void addConflictManager(ModuleId moduleId, PatternMatcher matcher,
            ConflictManager manager) {
        conflictManagers.put(new ModuleIdMatcher(matcher, moduleId), manager);
    }

    public ConflictManager getConflictManager(ModuleId moduleId) {
        for (Iterator iter = conflictManagers.keySet().iterator(); iter.hasNext();) {
            ModuleIdMatcher matcher = (ModuleIdMatcher) iter.next();
            if (matcher.matches(moduleId)) {
                return (ConflictManager) conflictManagers.get(matcher);
            }
        }
        return null;
    }

    public void addLicense(License license) {
        licenses.add(license);
    }

    public License[] getLicenses() {
        return (License[]) licenses.toArray(new License[licenses.size()]);
    }

    public String getHomePage() {
        return homePage;
    }

    public void setHomePage(String homePage) {
        this.homePage = homePage;
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
        for (Iterator iter = dependencies.iterator(); iter.hasNext();) {
            DependencyDescriptor dd = (DependencyDescriptor) iter.next();
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
     * Throws an exception if the module descriptor is inconsistent For the moment, only extended
     * configurations existence is checked
     */
    public void check() {
        for (Iterator iter = configurations.values().iterator(); iter.hasNext();) {
            Configuration conf = (Configuration) iter.next();
            String[] ext = conf.getExtends();
            for (int i = 0; i < ext.length; i++) {
                if (!configurations.containsKey(ext[i].trim())) {
                    throw new IllegalStateException("unknown configuration '" + ext[i]
                            + "'. It is extended by " + conf.getName());
                }
            }
        }
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

    public Map getAttributes() {
        return resolvedRevId.getAttributes();
    }

    public String getExtraAttribute(String attName) {
        return resolvedRevId.getExtraAttribute(attName);
    }

    public Map getExtraAttributes() {
        return resolvedRevId.getExtraAttributes();
    }

    public Map getQualifiedExtraAttributes() {
        return resolvedRevId.getQualifiedExtraAttributes();
    }

    public String getStandardAttribute(String attName) {
        return resolvedRevId.getStandardAttribute(attName);
    }

    public Map getStandardAttributes() {
        return resolvedRevId.getStandardAttributes();
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

    public ExcludeRule[] getAllExcludeRules() {
        return (ExcludeRule[]) excludeRules.toArray(new ExcludeRule[excludeRules.size()]);
    }

    public ExcludeRule[] getExcludeRules(String[] moduleConfigurations) {
        Set rules = new LinkedHashSet();
        for (Iterator iter = excludeRules.iterator(); iter.hasNext();) {
            ExcludeRule rule = (ExcludeRule) iter.next();
            String[] ruleConfs = rule.getConfigurations();
            if (containsAny(ruleConfs, moduleConfigurations)) {
                rules.add(rule);
            }
        }
        return (ExcludeRule[]) rules.toArray(new ExcludeRule[rules.size()]);
    }

    private boolean containsAny(String[] arr1, String[] arr2) {
        return new ArrayList(Arrays.asList(arr1)).removeAll(Arrays.asList(arr2));
    }
    
    public Map getExtraAttributesNamespaces() {
        return extraAttributesNamespaces;
    }

    public void addExtraAttributeNamespace(String prefix, String namespace) {
        extraAttributesNamespaces.put(prefix, namespace);
    }

   
    
    public void addExtraInfo(String infoKey, String value) {
        extraInfo.put(infoKey, value);
    }
    
    public Map getExtraInfo() {
        return extraInfo;
    }
}
