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
package org.apache.ivy.plugins.parser.m2;

import java.util.*;

import org.apache.ivy.core.cache.ArtifactOrigin;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.DefaultDependencyArtifactDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultExcludeRule;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ExtraInfoHolder;
import org.apache.ivy.core.module.descriptor.License;
import org.apache.ivy.core.module.descriptor.MDArtifact;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.descriptor.OverrideDependencyDescriptorMediator;
import org.apache.ivy.core.module.id.ArtifactId;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.matcher.ExactPatternMatcher;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.ivy.plugins.parser.ModuleDescriptorParser;
import org.apache.ivy.plugins.parser.ParserSettings;
import org.apache.ivy.plugins.parser.m2.PomReader.PomDependencyData;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.util.Message;

import static org.apache.ivy.core.module.descriptor.Configuration.Visibility.PUBLIC;
import static org.apache.ivy.util.StringUtils.defaultIfEmpty;
import static org.apache.ivy.util.StringUtils.isNullOrEmpty;

/**
 * Build a module descriptor. This class handle the complexity of the structure of an ivy
 * ModuleDescriptor and isolate the PomModuleDescriptorParser from it.
 */
public class PomModuleDescriptorBuilder {

    /**
     * The namespace URI which is used to refer to Maven (pom) specific elements within a
     * Ivy module descriptor file (ivy.xml)
     */
    private static final String IVY_XML_MAVEN_NAMESPACE_URI = "http://ant.apache.org/ivy/maven";

    private static final int DEPENDENCY_MANAGEMENT_KEY_PARTS_COUNT = 4;

    public static final Configuration[] MAVEN2_CONFIGURATIONS = new Configuration[] {
            new Configuration("default", PUBLIC,
                    "runtime dependencies and master artifact can be used with this conf",
                    new String[] {"runtime", "master"}, true, null),
            new Configuration("master", PUBLIC,
                    "contains only the artifact published by this module itself, "
                            + "with no transitive dependencies", new String[0], true, null),
            new Configuration("compile", PUBLIC,
                    "this is the default scope, used if none is specified. "
                            + "Compile dependencies are available in all classpaths.",
                    new String[0], true, null),
            new Configuration("provided", PUBLIC,
                    "this is much like compile, but indicates you expect the JDK or a container "
                            + "to provide it. "
                            + "It is only available on the compilation classpath, and is not transitive.",
                    new String[0], true, null),
            new Configuration("runtime", PUBLIC,
                    "this scope indicates that the dependency is not required for compilation, "
                            + "but is for execution. It is in the runtime and test classpaths, "
                            + "but not the compile classpath.", new String[] {"compile"}, true,
                    null),
            new Configuration("test", PUBLIC,
                    "this scope indicates that the dependency is not required for normal use of "
                            + "the application, and is only available for the test compilation and "
                            + "execution phases.", new String[] {"runtime"}, true, null),
            new Configuration("system", PUBLIC,
                    "this scope is similar to provided except that you have to provide the JAR "
                            + "which contains it explicitly. The artifact is always available and is not "
                            + "looked up in a repository.", new String[0], true, null),
            new Configuration("sources", PUBLIC,
                    "this configuration contains the source artifact of this module, if any.",
                    new String[0], true, null),
            new Configuration("javadoc", PUBLIC,
                    "this configuration contains the javadoc artifact of this module, if any.",
                    new String[0], true, null),
            new Configuration("optional", PUBLIC, "contains all optional dependencies",
                    new String[0], true, null)};

    static final Map<String, ConfMapper> MAVEN2_CONF_MAPPING = new HashMap<>();

    private static final String DEPENDENCY_MANAGEMENT = "m:dependency.management";

    private static final String PROPERTIES = "m:properties";

    private static final String EXTRA_INFO_DELIMITER = "__";

    private static final Collection<String> JAR_PACKAGINGS = Arrays.asList("ejb",
            "bundle", "maven-plugin", "eclipse-plugin", "jbi-component", "jbi-shared-library",
            "orbit", "hk2-jar");

    interface ConfMapper {
        void addMappingConfs(DefaultDependencyDescriptor dd, boolean isOptional);
    }

    static {
        MAVEN2_CONF_MAPPING.put("compile", new ConfMapper() {
            public void addMappingConfs(DefaultDependencyDescriptor dd, boolean isOptional) {
                if (isOptional) {
                    dd.addDependencyConfiguration("optional", "compile(*)");
                    // dd.addDependencyConfiguration("optional", "provided(*)");
                    dd.addDependencyConfiguration("optional", "master(*)");

                } else {
                    dd.addDependencyConfiguration("compile", "compile(*)");
                    // dd.addDependencyConfiguration("compile", "provided(*)");
                    dd.addDependencyConfiguration("compile", "master(*)");
                    dd.addDependencyConfiguration("runtime", "runtime(*)");
                }
            }
        });
        MAVEN2_CONF_MAPPING.put("provided", new ConfMapper() {
            public void addMappingConfs(DefaultDependencyDescriptor dd, boolean isOptional) {
                if (isOptional) {
                    dd.addDependencyConfiguration("optional", "compile(*)");
                    dd.addDependencyConfiguration("optional", "provided(*)");
                    dd.addDependencyConfiguration("optional", "runtime(*)");
                    dd.addDependencyConfiguration("optional", "master(*)");
                } else {
                    dd.addDependencyConfiguration("provided", "compile(*)");
                    dd.addDependencyConfiguration("provided", "provided(*)");
                    dd.addDependencyConfiguration("provided", "runtime(*)");
                    dd.addDependencyConfiguration("provided", "master(*)");
                }
            }
        });
        MAVEN2_CONF_MAPPING.put("runtime", new ConfMapper() {
            public void addMappingConfs(DefaultDependencyDescriptor dd, boolean isOptional) {
                if (isOptional) {
                    dd.addDependencyConfiguration("optional", "compile(*)");
                    dd.addDependencyConfiguration("optional", "provided(*)");
                    dd.addDependencyConfiguration("optional", "master(*)");

                } else {
                    dd.addDependencyConfiguration("runtime", "compile(*)");
                    dd.addDependencyConfiguration("runtime", "runtime(*)");
                    dd.addDependencyConfiguration("runtime", "master(*)");
                }
            }
        });
        MAVEN2_CONF_MAPPING.put("test", new ConfMapper() {
            public void addMappingConfs(DefaultDependencyDescriptor dd, boolean isOptional) {
                // optional doesn't make sense in the test scope
                dd.addDependencyConfiguration("test", "runtime(*)");
                dd.addDependencyConfiguration("test", "master(*)");
            }
        });
        MAVEN2_CONF_MAPPING.put("system", new ConfMapper() {
            public void addMappingConfs(DefaultDependencyDescriptor dd, boolean isOptional) {
                // optional doesn't make sense in the system scope
                dd.addDependencyConfiguration("system", "master(*)");
            }
        });
    }

    private final PomModuleDescriptor ivyModuleDescriptor;

    private ModuleRevisionId mrid;

    private DefaultArtifact mainArtifact;

    private ParserSettings parserSettings;

    private static final String WRONG_NUMBER_OF_PARTS_MSG = "what seemed to be a dependency "
            + "management extra info exclusion had the wrong number of parts (should have 2) ";

    public PomModuleDescriptorBuilder(ModuleDescriptorParser parser, Resource res,
            ParserSettings ivySettings) {
        ivyModuleDescriptor = new PomModuleDescriptor(parser, res);
        ivyModuleDescriptor.setResolvedPublicationDate(new Date(res.getLastModified()));
        for (Configuration m2conf : MAVEN2_CONFIGURATIONS) {
            ivyModuleDescriptor.addConfiguration(m2conf);
        }
        ivyModuleDescriptor.setMappingOverride(true);
        ivyModuleDescriptor.addExtraAttributeNamespace("m", IVY_XML_MAVEN_NAMESPACE_URI);
        parserSettings = ivySettings;
    }

    public ModuleDescriptor getModuleDescriptor() {
        return ivyModuleDescriptor;
    }

    public void setModuleRevId(String groupId, String artifactId, String version) {
        mrid = ModuleRevisionId.newInstance(groupId, artifactId, version);
        ivyModuleDescriptor.setModuleRevisionId(mrid);

        if (version == null || version.endsWith("SNAPSHOT")) {
            ivyModuleDescriptor.setStatus("integration");
        } else {
            ivyModuleDescriptor.setStatus("release");
        }
    }

    public void setHomePage(String homePage) {
        ivyModuleDescriptor.setHomePage(homePage);
    }

    public void setDescription(String description) {
        ivyModuleDescriptor.setDescription(description);
    }

    public void setLicenses(License[] licenses) {
        for (License license : licenses) {
            ivyModuleDescriptor.addLicense(license);
        }
    }

    public void addMainArtifact(String artifactId, String packaging) {
        String ext;

        /*
         * TODO: we should make packaging to ext mapping configurable, since it's not possible to
         * cover all cases.
         */
        if ("pom".equals(packaging)) {
            // no artifact defined! Add the default artifact if it exist.
            DependencyResolver resolver = parserSettings.getResolver(mrid);

            if (resolver != null) {
                DefaultArtifact artifact = new DefaultArtifact(mrid, new Date(), artifactId, "jar",
                        "jar");
                ArtifactOrigin artifactOrigin = resolver.locate(artifact);

                if (!ArtifactOrigin.isUnknown(artifactOrigin)) {
                    mainArtifact = artifact;
                    ivyModuleDescriptor.addArtifact("master", mainArtifact);
                }
            }

            return;
        } else if (JAR_PACKAGINGS.contains(packaging)) {
            ext = "jar";
        } else if ("pear".equals(packaging)) {
            ext = "phar";
        } else {
            ext = packaging;
        }

        mainArtifact = new DefaultArtifact(mrid, new Date(), artifactId, packaging, ext);
        ivyModuleDescriptor.addArtifact("master", mainArtifact);
    }

    public void addDependency(Resource res, PomDependencyData dep) {
        String scope = dep.getScope();
        if (!isNullOrEmpty(scope) && !MAVEN2_CONF_MAPPING.containsKey(scope)) {
            // unknown scope, defaulting to 'compile'
            scope = "compile";
        }

        String version = dep.getVersion();
        if (isNullOrEmpty(version)) {
            version = getDefaultVersion(dep);
        }
        ModuleRevisionId moduleRevId = ModuleRevisionId.newInstance(dep.getGroupId(),
            dep.getArtifactId(), version);

        // Some POMs depend on themselves; Ivy doesn't allow this. Don't add this dependency!
        // Example: https://repo1.maven.org/maven2/net/jini/jsk-platform/2.1/jsk-platform-2.1.pom
        ModuleRevisionId mRevId = ivyModuleDescriptor.getModuleRevisionId();
        if (mRevId != null && mRevId.getModuleId().equals(moduleRevId.getModuleId())) {
            return;
        }
        // experimentation shows the following, excluded modules are
        // inherited from parent POMs if either of the following is true:
        // the <exclusions> element is missing or the <exclusions> element
        // is present, but empty.
        List<ModuleId> excluded = dep.getExcludedModules();
        if (excluded.isEmpty()) {
            excluded = getDependencyMgtExclusions(ivyModuleDescriptor, dep);
        }
        final boolean excludeAllTransitiveDeps = shouldExcludeAllTransitiveDeps(excluded);
        // the same dependency mrid could appear twice in the module descriptor,
        // so we check if we already have created a dependency descriptor for the dependency mrid
        final DependencyDescriptor existing = this.ivyModuleDescriptor.depDescriptors.get(moduleRevId);
        final String[] existingConfigurations = existing == null ? new String[0]
            : existing.getModuleConfigurations();
        final DefaultDependencyDescriptor dd = (existing != null && existing instanceof DefaultDependencyDescriptor)
                ? (DefaultDependencyDescriptor) existing
                : new PomDependencyDescriptor(dep, ivyModuleDescriptor, moduleRevId, !excludeAllTransitiveDeps);
        if (isNullOrEmpty(scope)) {
            scope = getDefaultScope(dep);
        }
        ConfMapper mapping = MAVEN2_CONF_MAPPING.get(scope);
        mapping.addMappingConfs(dd, dep.isOptional());
        Map<String, String> extraAtt = new HashMap<>();
        final String optionalizedScope = dep.isOptional() ? "optional" : scope;
        if (isNonDefaultArtifact(dep)) {
            if (existing != null && existing.getAllDependencyArtifacts().length == 0) {
                String moduleConfiguration = existingConfigurations.length == 1
                    ? existingConfigurations[0] : optionalizedScope;
                // previously added dependency has been the "default artifact"
                dd.addDependencyArtifact(moduleConfiguration, createDefaultArtifact(dd));
            }
            String type = "jar";
            if (dep.getType() != null) {
                type = dep.getType();
            }
            String ext = type;

            // if type is 'test-jar', the extension is 'jar' and the classifier is 'tests'
            // Cfr. http://maven.apache.org/guides/mini/guide-attached-tests.html
            if ("test-jar".equals(type)) {
                ext = "jar";
                extraAtt.put("m:classifier", "tests");
            } else if (JAR_PACKAGINGS.contains(type)) {
                ext = "jar";
            }

            // we deal with classifiers by setting an extra attribute and forcing the
            // dependency to assume such an artifact is published
            if (dep.getClassifier() != null) {
                extraAtt.put("m:classifier", dep.getClassifier());
            }
            final DefaultDependencyArtifactDescriptor depArtifact = new DefaultDependencyArtifactDescriptor(
                    dd, dd.getDependencyId().getName(), type, ext, null, extraAtt);
            // here we have to assume a type and ext for the artifact, so this is a limitation
            // compared to how m2 behave with classifiers
            depArtifact.addConfiguration(optionalizedScope);
            dd.addDependencyArtifact(optionalizedScope, depArtifact);
        } else if (existing != null) {
            // this is the "default" artifact and some non-default artifact has already been added
            dd.addDependencyArtifact(optionalizedScope, createDefaultArtifact(dd));
        }

        for (ModuleId excludedModule : excluded) {
            // This represents exclude all transitive dependencies, which we have already taken
            // in account while defining the DefaultDependencyDescriptor itself
            if ("*".equals(excludedModule.getOrganisation()) && "*".equals(excludedModule.getName())) {
                continue;
            }
            for (String conf : dd.getModuleConfigurations()) {
                dd.addExcludeRule(conf, new DefaultExcludeRule(new ArtifactId(excludedModule,
                        PatternMatcher.ANY_EXPRESSION, PatternMatcher.ANY_EXPRESSION,
                        PatternMatcher.ANY_EXPRESSION), ExactPatternMatcher.INSTANCE, null));
            }
        }
        // intentional identity check to make sure we don't re-add the same dependency
        if (existing != dd) {
            ivyModuleDescriptor.addDependency(dd);
        }
    }

    private boolean isNonDefaultArtifact(PomDependencyData dep) {
        return dep.getClassifier() != null || dep.getType() != null && !"jar".equals(dep.getType());
    }

    private DefaultDependencyArtifactDescriptor createDefaultArtifact(DefaultDependencyDescriptor dd) {
        return new DefaultDependencyArtifactDescriptor(dd, dd.getDependencyId().getName(),
            "jar", "jar", null, null);
    }

    private static boolean shouldExcludeAllTransitiveDeps(final List<ModuleId> exclusions) {
        if (exclusions == null || exclusions.isEmpty()) {
            return false;
        }
        for (final ModuleId exclusion : exclusions) {
            if (exclusion == null) {
                continue;
            }
            if ("*".equals(exclusion.getOrganisation()) && "*".equals(exclusion.getName())) {
                return true;
            }
        }
        return false;
    }

    public void addDependency(DependencyDescriptor descriptor) {
        // Some POMs depend on themselves through their parent pom, don't add this dependency
        // since Ivy doesn't allow this!
        // Example:
        // https://repo1.maven.org/maven2/com/atomikos/atomikos-util/3.6.4/atomikos-util-3.6.4.pom
        ModuleId dependencyId = descriptor.getDependencyId();
        ModuleRevisionId mRevId = ivyModuleDescriptor.getModuleRevisionId();
        if (mRevId != null && mRevId.getModuleId().equals(dependencyId)) {
            return;
        }

        ivyModuleDescriptor.addDependency(descriptor);
    }

    public void addDependencyMgt(PomDependencyMgt dep) {
        ivyModuleDescriptor.addDependencyManagement(dep);

        String key = getDependencyMgtExtraInfoKeyForVersion(dep);
        overwriteExtraInfoIfExists(key, dep.getVersion());
        if (dep.getScope() != null) {
            String scopeKey = getDependencyMgtExtraInfoKeyForScope(dep);
            overwriteExtraInfoIfExists(scopeKey, dep.getScope());
        }
        if (!dep.getExcludedModules().isEmpty()) {
            String exclusionPrefix = getDependencyMgtExtraInfoPrefixForExclusion(dep);
            int index = 0;
            for (ModuleId excludedModule : dep.getExcludedModules()) {
                overwriteExtraInfoIfExists(
                    exclusionPrefix + index,
                    excludedModule.getOrganisation() + EXTRA_INFO_DELIMITER
                            + excludedModule.getName());
                index++;
            }
        }
        // dependency management info is also used for version mediation of transitive dependencies
        ivyModuleDescriptor.addDependencyDescriptorMediator(
            ModuleId.newInstance(dep.getGroupId(), dep.getArtifactId()),
            ExactPatternMatcher.INSTANCE,
            new OverrideDependencyDescriptorMediator(null, dep.getVersion()));
    }

    public void addPlugin(PomDependencyMgt plugin) {
        String pluginValue = plugin.getGroupId() + EXTRA_INFO_DELIMITER + plugin.getArtifactId()
                + EXTRA_INFO_DELIMITER + plugin.getVersion();
        ExtraInfoHolder extraInfoByTagName = ivyModuleDescriptor
                .getExtraInfoByTagName("m:maven.plugins");
        if (extraInfoByTagName == null) {
            extraInfoByTagName = new ExtraInfoHolder();
            extraInfoByTagName.setName("m:maven.plugins");
            ivyModuleDescriptor.addExtraInfo(extraInfoByTagName);
        }
        String pluginExtraInfo = extraInfoByTagName.getContent();
        if (pluginExtraInfo == null) {
            pluginExtraInfo = pluginValue;
        } else {
            pluginExtraInfo += "|" + pluginValue;
        }
        extraInfoByTagName.setContent(pluginExtraInfo);
    }

    public static List<PomDependencyMgt> getPlugins(ModuleDescriptor md) {
        List<PomDependencyMgt> result = new ArrayList<>();
        String plugins = md.getExtraInfoContentByTagName("m:maven.plugins");
        if (plugins == null) {
            return new ArrayList<>();
        }
        for (String plugin : plugins.split("\\|")) {
            String[] parts = plugin.split(EXTRA_INFO_DELIMITER);
            result.add(new PomPluginElement(parts[0], parts[1], parts[2]));
        }

        return result;
    }

    private static class PomPluginElement implements PomDependencyMgt {
        private String groupId;

        private String artifactId;

        private String version;

        public PomPluginElement(String groupId, String artifactId, String version) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
        }

        public String getGroupId() {
            return groupId;
        }

        public String getArtifactId() {
            return artifactId;
        }

        public String getType() {
            return null;
        }

        public String getClassifier() {
            return null;
        }

        public String getVersion() {
            return version;
        }

        public String getScope() {
            return null;
        }

        public List<ModuleId> getExcludedModules() {
            return Collections.emptyList(); // probably not used?
        }
    }

    private String getDefaultVersion(PomDependencyData dep) {
        DependencyMgtKey key = new DependencyMgtKey(dep);
        if (ivyModuleDescriptor.getDependencyManagementMap().containsKey(key)) {
            return ivyModuleDescriptor.getDependencyManagementMap().get(key).getVersion();
        }
        String tagName = getDependencyMgtExtraInfoKeyForVersion(dep);
        return ivyModuleDescriptor.getExtraInfoContentByTagName(tagName);
    }

    private String getDefaultScope(PomDependencyData dep) {
        String result;
        DependencyMgtKey key = new DependencyMgtKey(dep);
        if (ivyModuleDescriptor.getDependencyManagementMap().containsKey(key)) {
            result = ivyModuleDescriptor.getDependencyManagementMap().get(key).getScope();
        } else {
            String tagname = getDependencyMgtExtraInfoKeyForScope(dep);
            result = ivyModuleDescriptor.getExtraInfoContentByTagName(tagname);
        }
        if (result == null || !MAVEN2_CONF_MAPPING.containsKey(result)) {
            result = "compile";
        }
        return result;
    }

    private static String getDependencyMgtExtraInfoKeyForVersion(PomDependencyMgt dep) {
        return getDependencyMgtExtraInfoKeyForProperty(dep, "version");
    }

    private static String getDependencyMgtExtraInfoKeyForScope(PomDependencyMgt dep) {
        return getDependencyMgtExtraInfoKeyForProperty(dep, "scope");
    }

    private static String getDependencyMgtExtraInfoPrefixForExclusion(PomDependencyMgt dep) {
        return getDependencyMgtExtraInfoKeyForProperty(dep, "exclusion_");
    }

    private static String getDependencyMgtExtraInfoKeyForProperty(PomDependencyMgt dep, String propertyName) {
        StringBuilder result = new StringBuilder(DEPENDENCY_MANAGEMENT);
        result.append(EXTRA_INFO_DELIMITER)
              .append(dep.getGroupId())
              .append(EXTRA_INFO_DELIMITER)
              .append(dep.getArtifactId());

        // If the type and classifier are the default values, we don't need to include them in the key.
        // However, if the type is the default value, but the classifier is not, we also do add the type.
        // Otherwise, we can't distinguish between a
        //  - default type with a non-default classifier; and
        //  - non-default type with a default classifier.
        String type = defaultIfEmpty(dep.getType(), DependencyMgtKey.DEFAULT_TYPE);
        String classifier = dep.getClassifier();
        if (classifier != null || !DependencyMgtKey.DEFAULT_TYPE.equals(type)) {
            result.append(EXTRA_INFO_DELIMITER)
                  .append(type);
        }
        if (classifier != null) {
            result.append(EXTRA_INFO_DELIMITER)
                  .append(classifier);
        }
        result.append(EXTRA_INFO_DELIMITER)
              .append(propertyName);
        return result.toString();
    }

    private static String getPropertyExtraInfoKey(String propertyName) {
        return PROPERTIES + EXTRA_INFO_DELIMITER + propertyName;
    }

    private static List<ModuleId> getDependencyMgtExclusions(ModuleDescriptor descriptor, PomDependencyMgt dep) {
        if (descriptor instanceof PomModuleDescriptor) {
            PomDependencyMgt dependencyMgt = ((PomModuleDescriptor) descriptor)
                    .getDependencyManagementMap().get(new DependencyMgtKey(dep));
            if (dependencyMgt != null) {
                return dependencyMgt.getExcludedModules();
            }
        }
        String exclusionPrefix = getDependencyMgtExtraInfoPrefixForExclusion(dep);
        List<ModuleId> exclusionIds = new LinkedList<>();
        for (ExtraInfoHolder extraInfoHolder : descriptor.getExtraInfos()) {
            String key = extraInfoHolder.getName();
            if (key.startsWith(exclusionPrefix)) {
                String fullExclusion = extraInfoHolder.getContent();
                String[] exclusionParts = fullExclusion.split(EXTRA_INFO_DELIMITER);
                if (exclusionParts.length != 2) {
                    Message.error(WRONG_NUMBER_OF_PARTS_MSG + exclusionParts.length + " : "
                            + fullExclusion);
                    continue;
                }
                exclusionIds.add(ModuleId.newInstance(exclusionParts[0], exclusionParts[1]));
            }
        }
        return exclusionIds;
    }

    public static List<PomDependencyMgt> getDependencyManagements(ModuleDescriptor md) {
        List<PomDependencyMgt> result = new ArrayList<>();

        if (md instanceof PomModuleDescriptor) {
            result.addAll(((PomModuleDescriptor) md).getDependencyManagementMap().values());
        } else {
            for (ExtraInfoHolder extraInfoHolder : md.getExtraInfos()) {
                String key = extraInfoHolder.getName();
                if (key.startsWith(DEPENDENCY_MANAGEMENT)) {
                    String[] parts = key.split(EXTRA_INFO_DELIMITER);
                    if (parts.length < DEPENDENCY_MANAGEMENT_KEY_PARTS_COUNT || parts.length > DEPENDENCY_MANAGEMENT_KEY_PARTS_COUNT + 2) {
                        Message.warn("what seem to be a dependency management extra info "
                                + "doesn't match expected pattern: " + key);
                    } else {
                        //m:dependency.management__groupId__artifactId[__type[__classifier]]__version
                        String groupId = parts[1];
                        String artifactId = parts[2];
                        String type = null;
                        String classifier = null;

                        if (parts.length == DEPENDENCY_MANAGEMENT_KEY_PARTS_COUNT + 1) {
                            type = parts[3];
                        } else if (parts.length == DEPENDENCY_MANAGEMENT_KEY_PARTS_COUNT + 2) {
                            type = parts[3];
                            classifier = parts[4];
                        }

                        PomDependencyMgt dep = new DefaultPomDependencyMgt(groupId, artifactId, type, classifier, null, null, null);
                        String versionKey = getDependencyMgtExtraInfoKeyForVersion(dep);
                        String scopeKey = getDependencyMgtExtraInfoKeyForScope(dep);

                        String version = md.getExtraInfoContentByTagName(versionKey);
                        String scope = md.getExtraInfoContentByTagName(scopeKey);
                        List<ModuleId> exclusions = getDependencyMgtExclusions(md, dep);
                        result.add(new DefaultPomDependencyMgt(groupId, artifactId, type, classifier, version, scope, exclusions));
                    }
                }
            }
        }
        return result;
    }

    @Deprecated
    public void addExtraInfos(Map<String, String> extraAttributes) {
        for (Map.Entry<String, String> entry : extraAttributes.entrySet()) {
            addExtraInfo(entry.getKey(), entry.getValue());
        }
    }

    private void addExtraInfo(String key, String value) {
        if (ivyModuleDescriptor.getExtraInfoByTagName(key) == null) {
            ivyModuleDescriptor.getExtraInfos().add(new ExtraInfoHolder(key, value));
        }
    }

    private void overwriteExtraInfoIfExists(String key, String value) {
        boolean found = false;
        for (ExtraInfoHolder extraInfoHolder : ivyModuleDescriptor.getExtraInfos()) {
            if (extraInfoHolder.getName().equals(key)) {
                extraInfoHolder.setContent(value);
                found = true;
            }
        }
        if (!found) {
            ivyModuleDescriptor.getExtraInfos().add(new ExtraInfoHolder(key, value));
        }
    }

    public void addExtraInfos(List<ExtraInfoHolder> extraInfosHolder) {
        for (ExtraInfoHolder extraInfoHolder : extraInfosHolder) {
            addExtraInfo(extraInfoHolder.getName(), extraInfoHolder.getContent());
        }
    }

    @Deprecated
    public static Map<String, String> extractPomProperties(Map<String, String> extraInfo) {
        Map<String, String> r = new HashMap<>();
        for (Map.Entry<String, String> extraInfoEntry : extraInfo.entrySet()) {
            if (extraInfoEntry.getKey().startsWith(PROPERTIES)) {
                String prop = extraInfoEntry.getKey().substring(PROPERTIES.length()
                        + EXTRA_INFO_DELIMITER.length());
                r.put(prop, extraInfoEntry.getValue());
            }
        }
        return r;
    }

    public static Map<String, String> extractPomProperties(List<ExtraInfoHolder> extraInfos) {
        Map<String, String> r = new HashMap<>();
        for (ExtraInfoHolder extraInfoHolder : extraInfos) {
            if (extraInfoHolder.getName().startsWith(PROPERTIES)) {
                String prop = extraInfoHolder.getName().substring(PROPERTIES.length()
                        + EXTRA_INFO_DELIMITER.length());
                r.put(prop, extraInfoHolder.getContent());
            }
        }
        return r;
    }

    public void addProperty(String propertyName, String value) {
        addExtraInfo(getPropertyExtraInfoKey(propertyName), value);
    }

    public Artifact getMainArtifact() {
        return mainArtifact;
    }

    public Artifact getSourceArtifact() {
        return new MDArtifact(ivyModuleDescriptor, mrid.getName(), "source", "jar", null,
                Collections.singletonMap("m:classifier", "sources"));
    }

    public Artifact getSrcArtifact() {
        return new MDArtifact(ivyModuleDescriptor, mrid.getName(), "source", "jar", null,
                Collections.singletonMap("m:classifier", "src"));
    }

    public Artifact getJavadocArtifact() {
        return new MDArtifact(ivyModuleDescriptor, mrid.getName(), "javadoc", "jar", null,
                Collections.singletonMap("m:classifier", "javadoc"));
    }

    public void addSourceArtifact() {
        ivyModuleDescriptor.addArtifact("sources", getSourceArtifact());
    }

    public void addSrcArtifact() {
        ivyModuleDescriptor.addArtifact("sources", getSrcArtifact());
    }

    public void addJavadocArtifact() {
        ivyModuleDescriptor.addArtifact("javadoc", getJavadocArtifact());
    }

    /**
     * <code>DependencyDescriptor</code> that provides access to the original
     * <code>PomDependencyData</code>.
     */
    public static class PomDependencyDescriptor extends DefaultDependencyDescriptor {
        private final PomDependencyData pomDependencyData;

        private PomDependencyDescriptor(PomDependencyData pomDependencyData,
                ModuleDescriptor moduleDescriptor, ModuleRevisionId revisionId, final boolean transitive) {
            super(moduleDescriptor, revisionId, true, false, transitive);
            this.pomDependencyData = pomDependencyData;
        }

        /**
         * Get PomDependencyData.
         *
         * @return PomDependencyData
         */
        public PomDependencyData getPomDependencyData() {
            return pomDependencyData;
        }
    }

    public static class PomModuleDescriptor extends DefaultModuleDescriptor {
        private final Map<DependencyMgtKey, PomDependencyMgt> dependencyManagementMap = new LinkedHashMap<>();
        // dependency descriptor keyed by its dependency revision id
        private final Map<ModuleRevisionId, DependencyDescriptor> depDescriptors = new HashMap<>();

        public PomModuleDescriptor(ModuleDescriptorParser parser, Resource res) {
            super(parser, res);
        }

        public void addDependencyManagement(PomDependencyMgt dependencyMgt) {
            dependencyManagementMap.put(new DependencyMgtKey(dependencyMgt), dependencyMgt);
        }

        public Map<DependencyMgtKey, PomDependencyMgt> getDependencyManagementMap() {
            return dependencyManagementMap;
        }

        @Override
        public void addDependency(final DependencyDescriptor dependency) {
            super.addDependency(dependency);
            this.depDescriptors.put(dependency.getDependencyRevisionId(), dependency);
        }
    }

    public static class DependencyMgtKey {

        private static final String DEFAULT_TYPE = "jar";

        private final ModuleId moduleId;
        private final String type; // default to 'jar' if not specified
        private final String classifier; // default to null if not specified

        public DependencyMgtKey(PomDependencyMgt dependencyMgt) {
            moduleId = ModuleId.newInstance(dependencyMgt.getGroupId(), dependencyMgt.getArtifactId());
            type = defaultIfEmpty(dependencyMgt.getType(), DEFAULT_TYPE);
            classifier = dependencyMgt.getClassifier();
        }

        public String getGroupId() {
            return moduleId.getOrganisation();
        }

        public String getArtifactId() {
            return moduleId.getName();
        }

        public String getType() {
            return type;
        }

        public String getClassifier() {
            return classifier;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof DependencyMgtKey)) {
                return false;
            }

            DependencyMgtKey that = (DependencyMgtKey) obj;
            return Objects.equals(moduleId, that.moduleId)
                   && Objects.equals(type, that.type)
                   && Objects.equals(classifier, that.classifier);
        }

        @Override
        public int hashCode() {
            return Objects.hash(moduleId, type, classifier);
        }
    }
}
