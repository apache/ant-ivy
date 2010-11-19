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
package org.apache.ivy.osgi.ivy;

import static java.lang.String.format;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.osgi.core.BundleInfo;
import org.apache.ivy.osgi.core.BundleRequirement;
import org.apache.ivy.osgi.core.ExportPackage;
import org.apache.ivy.osgi.core.ManifestParser;
import org.apache.ivy.osgi.ivy.internal.PackageRegistry;
import org.apache.ivy.osgi.util.NameUtil;
import org.apache.ivy.osgi.util.NameUtil.OrgAndName;
import org.apache.ivy.plugins.parser.AbstractModuleDescriptorParser;
import org.apache.ivy.plugins.parser.ModuleDescriptorParser;
import org.apache.ivy.plugins.parser.ParserSettings;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorWriter;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.util.Message;
import org.apache.tools.ant.types.resources.FileResource;


/**
 * An parser for OSGi Manifest descriptor.
 * 
 * @author jerome@benois.fr
 * @author alex@radeski.net
 */
public class OsgiManifestParser extends AbstractModuleDescriptorParser {

    public static final String PACKAGE = ".package";

    protected static final Pattern PATH_REGEX = Pattern
            .compile("(.*/)?([\\w\\.]+)[\\-_](\\d\\.\\d\\.\\d)[\\.]?([\\w]+)?");

    private static final OsgiManifestParser INSTANCE = new OsgiManifestParser();

    public static OsgiManifestParser getInstance() {
        return INSTANCE;
    }

    public boolean accept(Resource res) {
        if (res == null || res.getName() == null || res.getName().trim().equals("")) {
            return false;
        }
        return res.getName().toUpperCase().endsWith("META-INF/MANIFEST.MF")
                || res.getName().toUpperCase().endsWith(".JAR") || res.getName().toUpperCase().endsWith(".PKGREF");
    }

    public ModuleDescriptor parseDescriptor(ParserSettings ivySettings, URL descriptorURL, Resource res,
            boolean validate) throws ParseException, IOException {

        Message.debug(format("\tparse descriptor: resource=%s", res));

        final InternalParser parser = new InternalParser(this);
        parser.parse(res, ivySettings);
        return parser.getModuleDescriptor();
    }

    public ModuleDescriptor parseExports(Resource res) throws ParseException, IOException {

        Message.debug(format("\tparse descriptor: resource=%s", res));

        final InternalParser parser = new InternalParser(this);
        parser.parse(res, true);
        return parser.getModuleDescriptor();
    }

    public void toIvyFile(InputStream is, Resource res, File destFile, ModuleDescriptor md) throws ParseException,
            IOException {
        try {
            XmlModuleDescriptorWriter.write(md, destFile);
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    public static class InternalParser extends AbstractParser {

        protected InternalParser(ModuleDescriptorParser parser) {
            super(parser);
        }

        public void parse(Resource res, ParserSettings ivySettings) throws IOException {
            parse(res, false);
        }

        public void parse(Resource res, boolean useExports) throws IOException {
            Manifest manifest;
            if ((res instanceof FileResource) && ((FileResource) res).isDirectory()) {
                manifest = new Manifest(new FileInputStream(res.getName() + "/META-INF/MANIFEST.MF"));
            } else if (res.getName().toUpperCase().endsWith(".JAR")) {
                manifest = new JarInputStream(res.openStream()).getManifest();
            } else {
                manifest = new Manifest(res.openStream());
            }

            if (manifest == null) {
                return;
            }

            BundleInfo info;
            try {
                info = ManifestParser.parseManifest(manifest);
            } catch (ParseException e) {
                IOException ioe = new IOException();
                ioe.initCause(e);
                throw ioe;
            }

            setResource(res);

            // Set Info
            OrgAndName orgAndName = NameUtil.instance().asOrgAndName(info.getSymbolicName());
            final ModuleRevisionId mrid = ModuleRevisionId.newInstance(orgAndName.org, orgAndName.name, info
                    .getVersion().toString());

            getMd().setDescription(info.getDescription());
            getMd().setResolvedPublicationDate(new Date(res.getLastModified()));
            getMd().setModuleRevisionId(mrid);
            getMd().addConfiguration(new Configuration("default"));
            getMd().addConfiguration(new Configuration("optional"));
            getMd().addConfiguration(new Configuration("source"));
            getMd().setStatus("release");
            getMd().addArtifact("default",
                    new DefaultArtifact(mrid, getDefaultPubDate(), info.getSymbolicName(), "jar", "jar"));
            getMd().addArtifact("source",
                    new DefaultArtifact(mrid, getDefaultPubDate(), info.getSymbolicName() + ".source", "src", "jar"));

            if (useExports) {
                addExports(getMd(), info.getExports());
            } else {
                Set<ModuleRevisionId> processedDeps = new HashSet<ModuleRevisionId>();
                processedDeps.add(mrid);

                addRequiredBundles(getMd(), info.getRequires(), processedDeps);
                addImports(getMd(), info.getImports(), processedDeps);
            }

            Message.debug(format("\t\tparse: bundle info: %s", info.toString()));
        }

        protected void addExports(DefaultModuleDescriptor parent, Set<ExportPackage> bundleDependencies) {
            for (final ExportPackage dep : bundleDependencies) {
                String rev = "";
                if (dep.getVersion() != null) {
                    rev = dep.getVersion().toString();
                }

                final ModuleRevisionId depMrid = ModuleRevisionId.newInstance(PACKAGE, dep.getName(), rev);
                final DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(parent, depMrid, false, false,
                        true);
                dd.addDependencyConfiguration("default", "default");
                parent.addDependency(dd);
            }
        }

        protected void addRequiredBundles(DefaultModuleDescriptor parent, Set<BundleRequirement> bundleDependencies,
                Set<ModuleRevisionId> processedDeps) {
            for (final BundleRequirement dep : bundleDependencies) {
                String rev = "";
                if (dep.getVersion() != null) {
                    rev = dep.getVersion().toIvyRevision();
                }

                OrgAndName orgAndName = NameUtil.instance().asOrgAndName(dep.getName());
                final ModuleRevisionId depMrid = ModuleRevisionId.newInstance(orgAndName.org, orgAndName.name, rev);

                if (processedDeps.contains(depMrid)) {
                    return;
                }

                processedDeps.add(depMrid);

                final DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(parent, depMrid, false, false,
                        true);
                if (dep.getResolution() == null) {
                    dd.addDependencyConfiguration("default", "default");
                } else {
                    dd.addDependencyConfiguration(dep.getResolution(), dep.getResolution());
                }
                parent.addDependency(dd);
            }

        }

        protected void addImports(DefaultModuleDescriptor parent, Set<BundleRequirement> bundleDependencies,
                Set<ModuleRevisionId> processedDeps) {
            for (final BundleRequirement dep : bundleDependencies) {
                final ModuleRevisionId pkgMrid = PackageRegistry.getInstance().processImports(dep.getName(), dep.getVersion());

                if (processedDeps.contains(pkgMrid)) {
                    return;
                }

                processedDeps.add(pkgMrid);

                if (pkgMrid != null) {
                    final DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(parent, pkgMrid, false,
                            false, true);
                    if (dep.getResolution() == null) {
                        dd.addDependencyConfiguration("default", "default");
                    } else {
                        dd.addDependencyConfiguration(dep.getResolution(), dep.getResolution());
                    }
                    parent.addDependency(dd);
                } else {
                    Message.error("Failed to resolve imported package: " + dep);
                }
            }
        }
    }
}
