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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.cache.ArtifactOrigin;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.License;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolveEngine;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.plugins.circular.CircularDependencyException;
import org.apache.ivy.plugins.parser.ModuleDescriptorParser;
import org.apache.ivy.plugins.parser.ParserSettings;
import org.apache.ivy.plugins.parser.m2.PomModuleDescriptorBuilder.PomDependencyDescriptor;
import org.apache.ivy.plugins.parser.m2.PomReader.PomDependencyData;
import org.apache.ivy.plugins.parser.m2.PomReader.PomDependencyMgtElement;
import org.apache.ivy.plugins.parser.m2.PomReader.PomPluginElement;
import org.apache.ivy.plugins.parser.m2.PomReader.PomProfileElement;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorWriter;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.plugins.repository.url.URLResource;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.util.Message;
import org.xml.sax.SAXException;

import static org.apache.ivy.core.module.descriptor.Configuration.Visibility.PUBLIC;
import static org.apache.ivy.plugins.namespace.NameSpaceHelper.toSystem;
import static org.apache.ivy.plugins.parser.m2.PomModuleDescriptorBuilder.MAVEN2_CONFIGURATIONS;
import static org.apache.ivy.plugins.parser.m2.PomModuleDescriptorBuilder.extractPomProperties;
import static org.apache.ivy.plugins.parser.m2.PomModuleDescriptorBuilder.getDependencyManagements;
import static org.apache.ivy.plugins.parser.m2.PomModuleDescriptorBuilder.getPlugins;

/**
 * A parser for Maven 2 POM.
 * <p>
 * The configurations used in the generated module descriptor mimics the behavior defined by Maven 2
 * scopes, as documented <a href=
 * "https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html">here</a>.
 * The PomModuleDescriptorParser use a PomDomReader to read the pom, and the
 * PomModuleDescriptorBuilder to write the ivy module descriptor using the info read by the
 * PomDomReader.
 * </p>
 */
public final class PomModuleDescriptorParser implements ModuleDescriptorParser {

    private static final PomModuleDescriptorParser INSTANCE = new PomModuleDescriptorParser();

    private static final String PARENT_MAP_KEY = PomModuleDescriptorParser.class.getName() + ".parentMap";

    public static PomModuleDescriptorParser getInstance() {
        return INSTANCE;
    }

    private PomModuleDescriptorParser() {
    }

    public void toIvyFile(InputStream is, Resource res, File destFile, ModuleDescriptor md)
            throws ParseException, IOException {
        try {
            XmlModuleDescriptorWriter.write(md, destFile);
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    public boolean accept(Resource res) {
        return res.getName().endsWith(".pom") || res.getName().endsWith("pom.xml")
                || res.getName().endsWith("project.xml");
    }

    public String toString() {
        return "pom parser";
    }

    public Artifact getMetadataArtifact(ModuleRevisionId mrid, Resource res) {
        return DefaultArtifact.newPomArtifact(mrid, new Date(res.getLastModified()));
    }

    public String getType() {
        return "pom";
    }

    public ModuleDescriptor parseDescriptor(ParserSettings ivySettings, URL descriptorURL,
            boolean validate) throws ParseException, IOException {
        URLResource resource = new URLResource(descriptorURL);
        return parseDescriptor(ivySettings, descriptorURL, resource, validate);
    }

    public ModuleDescriptor parseDescriptor(ParserSettings ivySettings, URL descriptorURL,
            Resource res, boolean validate) throws ParseException, IOException {

        PomModuleDescriptorBuilder mdBuilder = new PomModuleDescriptorBuilder(this, res,
                ivySettings);

        try {
            final IvyContext ivyContext = IvyContext.pushNewCopyContext();
            Set<ModuleRevisionId> parents = ivyContext.get(PARENT_MAP_KEY);
            if (parents == null) {
                parents = new LinkedHashSet<>();
                ivyContext.set(PARENT_MAP_KEY, parents);
            }

            PomReader domReader = new PomReader(descriptorURL, res);
            domReader.setProperty("parent.version", domReader.getParentVersion());
            domReader.setProperty("parent.groupId", domReader.getParentGroupId());
            domReader.setProperty("project.parent.version", domReader.getParentVersion());
            domReader.setProperty("project.parent.groupId", domReader.getParentGroupId());

            Message.debug("parent.groupId: " + domReader.getParentGroupId());
            Message.debug("parent.artifactId: " + domReader.getParentArtifactId());
            Message.debug("parent.version: " + domReader.getParentVersion());

            for (final Map.Entry<String, String> prop : domReader.getPomProperties().entrySet()) {
                domReader.setProperty(prop.getKey(), prop.getValue());
                mdBuilder.addProperty(prop.getKey(), prop.getValue());
            }
            final List<PomProfileElement> activeProfiles = new ArrayList<>();
            // add profile specific properties
            for (final PomProfileElement profile : domReader.getProfiles()) {
                if (!profile.isActive()) {
                    continue;
                }
                // keep track of this active profile for later use
                activeProfiles.add(profile);

                final Map<String, String> profileProps = profile.getProfileProperties();
                if (profileProps.isEmpty()) {
                    continue;
                }
                for (final Map.Entry<String, String> entry : profileProps.entrySet()) {
                    domReader.setProperty(entry.getKey(), entry.getValue());
                    mdBuilder.addProperty(entry.getKey(), entry.getValue());
                }
            }

            ModuleDescriptor parentDescr = null;
            if (domReader.hasParent()) {
                // Is there any other parent properties?

                ModuleRevisionId parentModRevID = ModuleRevisionId.newInstance(
                    domReader.getParentGroupId(), domReader.getParentArtifactId(),
                    domReader.getParentVersion());

                // check for cycles
                if (parents.contains(parentModRevID)) {
                    throw new CircularDependencyException(parents);
                } else {
                    parents.add(parentModRevID);
                }

                final ResolvedModuleRevision parentModule = parseOtherPom(ivySettings, parentModRevID, true);
                if (parentModule == null) {
                    throw new IOException("Impossible to load parent for " + res.getName()
                            + ". Parent=" + parentModRevID);
                }
                parentDescr = parentModule.getDescriptor();
                if (parentDescr != null) {
                    for (Map.Entry<String, String> prop
                            : extractPomProperties(parentDescr.getExtraInfos()).entrySet()) {
                        domReader.setProperty(prop.getKey(), prop.getValue());
                    }
                }
            }

            String groupId = domReader.getGroupId();
            String artifactId = domReader.getArtifactId();
            String version = domReader.getVersion();
            mdBuilder.setModuleRevId(groupId, artifactId, version);

            mdBuilder.setHomePage(domReader.getHomePage());
            mdBuilder.setDescription(domReader.getDescription());
            // if this module doesn't have an explicit license, use the parent's license (if any)
            final License[] licenses = domReader.getLicenses();
            if (licenses != null && licenses.length > 0) {
                mdBuilder.setLicenses(licenses);
            } else if (parentDescr != null) {
                mdBuilder.setLicenses(parentDescr.getLicenses());
            }

            ModuleRevisionId relocation = domReader.getRelocation();

            if (relocation != null) {
                if (groupId != null && artifactId != null && artifactId.equals(relocation.getName())
                        && groupId.equals(relocation.getOrganisation())) {
                    Message.error("Relocation to an other version number not supported in ivy : "
                            + mdBuilder.getModuleDescriptor().getModuleRevisionId()
                            + " relocated to " + relocation
                            + ". Please update your dependency to directly use the right version.");
                    Message.warn("Resolution will only pick dependencies of the relocated element."
                            + "  Artifact and other metadata will be ignored.");
                    ResolvedModuleRevision relocatedModule = parseOtherPom(ivySettings, relocation, false);
                    if (relocatedModule == null) {
                        throw new ParseException(
                                "impossible to load module " + relocation + " to which "
                                        + mdBuilder.getModuleDescriptor().getModuleRevisionId()
                                        + " has been relocated",
                                0);
                    }
                    for (DependencyDescriptor dd : relocatedModule.getDescriptor()
                            .getDependencies()) {
                        mdBuilder.addDependency(dd);
                    }
                } else {
                    Message.info(
                        mdBuilder.getModuleDescriptor().getModuleRevisionId() + " is relocated to "
                                + relocation + ". Please update your dependencies.");
                    Message.verbose("Relocated module will be considered as a dependency");
                    DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(
                            mdBuilder.getModuleDescriptor(), relocation, true, false, true);
                    /* Map all public dependencies */
                    for (Configuration m2Conf : MAVEN2_CONFIGURATIONS) {
                        if (PUBLIC.equals(m2Conf.getVisibility())) {
                            dd.addDependencyConfiguration(m2Conf.getName(), m2Conf.getName());
                        }
                    }
                    mdBuilder.addDependency(dd);
                }
            } else {
                domReader.setProperty("project.groupId", groupId);
                domReader.setProperty("pom.groupId", groupId);
                domReader.setProperty("groupId", groupId);
                domReader.setProperty("project.artifactId", artifactId);
                domReader.setProperty("pom.artifactId", artifactId);
                domReader.setProperty("artifactId", artifactId);
                domReader.setProperty("project.version", version);
                domReader.setProperty("pom.version", version);
                domReader.setProperty("version", version);

                if (parentDescr != null) {
                    mdBuilder.addExtraInfos(parentDescr.getExtraInfos());

                    // add dependency management info from parent
                    for (PomDependencyMgt dep : getDependencyManagements(parentDescr)) {
                        if (dep instanceof PomDependencyMgtElement) {
                            dep = domReader.new PomDependencyMgtElement(
                                    (PomDependencyMgtElement) dep);
                        }
                        mdBuilder.addDependencyMgt(dep);
                    }

                    // add plugins from parent
                    for (PomDependencyMgt pomDependencyMgt : getPlugins(parentDescr)) {
                        mdBuilder.addPlugin(pomDependencyMgt);
                    }
                }

                for (PomDependencyMgt dep : domReader.getDependencyMgt()) {
                    addTo(mdBuilder, dep, ivySettings);
                }
                for (PomDependencyData dep : domReader.getDependencies()) {
                    mdBuilder.addDependency(res, dep);
                }

                for (PomPluginElement plugin : domReader.getPlugins()) {
                    mdBuilder.addPlugin(plugin);
                }

                // consult active profiles:
                for (final PomProfileElement activeProfile : activeProfiles) {
                    for (PomDependencyMgt dep : activeProfile.getDependencyMgt()) {
                        addTo(mdBuilder, dep, ivySettings);
                    }
                    for (PomDependencyData dep : activeProfile.getDependencies()) {
                        mdBuilder.addDependency(res, dep);
                    }
                    for (PomPluginElement plugin : activeProfile.getPlugins()) {
                        mdBuilder.addPlugin(plugin);
                    }
                }

                if (parentDescr != null) {
                    for (DependencyDescriptor descriptor : parentDescr.getDependencies()) {
                        if (descriptor instanceof PomDependencyDescriptor) {
                            PomDependencyData parentDep = ((PomDependencyDescriptor) descriptor)
                                    .getPomDependencyData();
                            PomDependencyData dep = domReader.new PomDependencyData(parentDep);
                            mdBuilder.addDependency(res, dep);
                        } else {
                            mdBuilder.addDependency(descriptor);
                        }
                    }
                }

                mdBuilder.addMainArtifact(artifactId, domReader.getPackaging());

                addSourcesAndJavadocArtifactsIfPresent(mdBuilder, ivySettings);
            }
        } catch (SAXException e) {
            throw newParserException(e);
        } finally {
            IvyContext.popContext();
        }

        return mdBuilder.getModuleDescriptor();
    }

    private void addTo(PomModuleDescriptorBuilder mdBuilder, PomDependencyMgt dep,
            ParserSettings ivySettings) throws ParseException, IOException {
        if ("import".equals(dep.getScope())) {
            // In Maven, "import" scope semantics are equivalent to getting (only) the
            // dependency management section of the imported module, into the current
            // module, so that those "managed dependency versions" are usable/applicable
            // in the current module's dependencies
            ModuleRevisionId importModRevID = ModuleRevisionId.newInstance(dep.getGroupId(),
                    dep.getArtifactId(), dep.getVersion());
            ResolvedModuleRevision importModule = parseOtherPom(ivySettings, importModRevID, false);
            if (importModule == null) {
                throw new IOException("Impossible to import module for "
                        + mdBuilder.getModuleDescriptor().getResource().getName() + ". Import="
                        + importModRevID);
            }
            ModuleDescriptor importDescr = importModule.getDescriptor();

            // add dependency management info from imported module
            for (PomDependencyMgt importedDepMgt : getDependencyManagements(importDescr)) {
                mdBuilder.addDependencyMgt(new DefaultPomDependencyMgt(importedDepMgt.getGroupId(),
                        importedDepMgt.getArtifactId(), importedDepMgt.getVersion(),
                        importedDepMgt.getScope(), importedDepMgt.getExcludedModules()));
            }
        } else {
            mdBuilder.addDependencyMgt(dep);
        }

    }

    private void addSourcesAndJavadocArtifactsIfPresent(PomModuleDescriptorBuilder mdBuilder,
            ParserSettings ivySettings) {
        if (mdBuilder.getMainArtifact() == null) {
            // no main artifact in pom, we don't need to search for meta artifacts
            return;
        }

        boolean sourcesLookup = !"false"
                .equals(ivySettings.getVariable("ivy.maven.lookup.sources"));
        boolean javadocLookup = !"false"
                .equals(ivySettings.getVariable("ivy.maven.lookup.javadoc"));
        if (!sourcesLookup && !javadocLookup) {
            Message.debug("Sources and javadocs lookup disabled");
            return;
        }

        ModuleDescriptor md = mdBuilder.getModuleDescriptor();
        ModuleRevisionId mrid = md.getModuleRevisionId();
        DependencyResolver resolver = ivySettings.getResolver(mrid);

        if (resolver == null) {
            Message.debug(
                "no resolver found for " + mrid + ": no source or javadoc artifact lookup");
        } else {
            ArtifactOrigin mainArtifact = resolver.locate(mdBuilder.getMainArtifact());

            if (!ArtifactOrigin.isUnknown(mainArtifact)) {
                String mainArtifactLocation = mainArtifact.getLocation();

                if (sourcesLookup) {
                    ArtifactOrigin sourceArtifact = resolver.locate(mdBuilder.getSourceArtifact());
                    if (!ArtifactOrigin.isUnknown(sourceArtifact)
                            && !sourceArtifact.getLocation().equals(mainArtifactLocation)) {
                        Message.debug("source artifact found for " + mrid);
                        mdBuilder.addSourceArtifact();
                    } else {
                        // it seems that sometimes the 'src' classifier is used instead of 'sources'
                        // Cfr. IVY-1138
                        ArtifactOrigin srcArtifact = resolver.locate(mdBuilder.getSrcArtifact());
                        if (!ArtifactOrigin.isUnknown(srcArtifact)
                                && !srcArtifact.getLocation().equals(mainArtifactLocation)) {
                            Message.debug("source artifact found for " + mrid);
                            mdBuilder.addSrcArtifact();
                        } else {
                            Message.debug("no source artifact found for " + mrid);
                        }
                    }
                } else {
                    Message.debug("sources lookup disabled");
                }

                if (javadocLookup) {
                    ArtifactOrigin javadocArtifact = resolver
                            .locate(mdBuilder.getJavadocArtifact());
                    if (!ArtifactOrigin.isUnknown(javadocArtifact)
                            && !javadocArtifact.getLocation().equals(mainArtifactLocation)) {
                        Message.debug("javadoc artifact found for " + mrid);
                        mdBuilder.addJavadocArtifact();
                    } else {
                        Message.debug("no javadoc artifact found for " + mrid);
                    }
                } else {
                    Message.debug("javadocs lookup disabled");
                }
            }
        }
    }

    private ResolvedModuleRevision parseOtherPom(final ParserSettings ivySettings,
            final ModuleRevisionId parentModRevID, final boolean isParentPom) throws ParseException {

        Set<ModuleRevisionId> previousParents = null;
        if (!isParentPom) {
            // IVY-1588: we "reset" the parent tracking, since the parent tracking should only be
            // non-null when we are parsing a parent pom.
            previousParents = IvyContext.getContext().get(PARENT_MAP_KEY);
            if (previousParents != null) {
                IvyContext.getContext().set(PARENT_MAP_KEY, null);
            }
        }
        try {
            DependencyDescriptor dd = new DefaultDependencyDescriptor(parentModRevID, true);
            ResolveData data = IvyContext.getContext().getResolveData();
            if (data == null) {
                ResolveEngine engine = IvyContext.getContext().getIvy().getResolveEngine();
                ResolveOptions options = new ResolveOptions();
                options.setDownload(false);
                data = new ResolveData(engine, options);
            }

            DependencyResolver resolver = ivySettings.getResolver(parentModRevID);
            if (resolver == null) {
                // TODO: Throw exception here?
                return null;
            }
            dd = toSystem(dd, ivySettings.getContextNamespace());
            return resolver.getDependency(dd, data);
        } finally {
            if (!isParentPom) {
                // switch back to the previous state of the parent tracking
                IvyContext.getContext().set(PARENT_MAP_KEY, previousParents);
            }
        }
    }

    private ParseException newParserException(Exception e) {
        Message.error(e.getMessage());
        ParseException pe = new ParseException(e.getMessage(), 0);
        pe.initCause(e);
        return pe;
    }

}
