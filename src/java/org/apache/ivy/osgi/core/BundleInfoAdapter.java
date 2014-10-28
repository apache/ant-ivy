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
package org.apache.ivy.osgi.core;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.jar.Manifest;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.Configuration.Visibility;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultExcludeRule;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.ExtraInfoHolder;
import org.apache.ivy.core.module.id.ArtifactId;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.osgi.util.Version;
import org.apache.ivy.osgi.util.VersionRange;
import org.apache.ivy.plugins.matcher.ExactOrRegexpPatternMatcher;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.ivy.plugins.parser.ModuleDescriptorParser;

public class BundleInfoAdapter {

    public static final String CONF_NAME_DEFAULT = "default";

    public static final Configuration CONF_DEFAULT = new Configuration(CONF_NAME_DEFAULT);

    public static final String CONF_NAME_OPTIONAL = "optional";

    public static final Configuration CONF_OPTIONAL = new Configuration(CONF_NAME_OPTIONAL,
            Visibility.PUBLIC, "Optional dependencies", new String[] {CONF_NAME_DEFAULT}, true,
            null);

    public static final String CONF_NAME_TRANSITIVE_OPTIONAL = "transitive-optional";

    public static final Configuration CONF_TRANSITIVE_OPTIONAL = new Configuration(
            CONF_NAME_TRANSITIVE_OPTIONAL, Visibility.PUBLIC, "Optional dependencies",
            new String[] {CONF_NAME_OPTIONAL}, true, null);

    public static final String CONF_USE_PREFIX = "use_";

    public static final String EXTRA_INFO_EXPORT_PREFIX = "_osgi_export_";

    public static DefaultModuleDescriptor toModuleDescriptor(ModuleDescriptorParser parser,
            URI baseUri, BundleInfo bundle, ExecutionEnvironmentProfileProvider profileProvider) {
        return toModuleDescriptor(parser, baseUri, bundle, null, profileProvider);
    }

    /**
     * 
     * @param baseUri
     *            uri to help build the absolute url if the bundle info has a relative uri.
     * @return
     * @throws ProfileNotFoundException
     */
    public static DefaultModuleDescriptor toModuleDescriptor(ModuleDescriptorParser parser,
            URI baseUri, BundleInfo bundle, Manifest manifest,
            ExecutionEnvironmentProfileProvider profileProvider) throws ProfileNotFoundException {
        DefaultModuleDescriptor md = new DefaultModuleDescriptor(parser, null);
        md.addExtraAttributeNamespace("o", Ivy.getIvyHomeURL() + "osgi");
        ModuleRevisionId mrid = asMrid(BundleInfo.BUNDLE_TYPE, bundle.getSymbolicName(),
            bundle.getVersion());
        md.setResolvedPublicationDate(new Date());
        md.setModuleRevisionId(mrid);

        md.addConfiguration(CONF_DEFAULT);
        md.addConfiguration(CONF_OPTIONAL);
        md.addConfiguration(CONF_TRANSITIVE_OPTIONAL);

        Set<String> exportedPkgNames = new HashSet<String>(bundle.getExports().size());
        for (ExportPackage exportPackage : bundle.getExports()) {
            md.getExtraInfos().add(
                new ExtraInfoHolder(EXTRA_INFO_EXPORT_PREFIX + exportPackage.getName(),
                        exportPackage.getVersion().toString()));
            exportedPkgNames.add(exportPackage.getName());
            String[] confDependencies = new String[exportPackage.getUses().size() + 1];
            int i = 0;
            for (String use : exportPackage.getUses()) {
                confDependencies[i++] = CONF_USE_PREFIX + use;
            }
            confDependencies[i] = CONF_NAME_DEFAULT;
            md.addConfiguration(new Configuration(CONF_USE_PREFIX + exportPackage.getName(),
                    Visibility.PUBLIC, "Exported package " + exportPackage.getName(),
                    confDependencies, true, null));
        }

        requirementAsDependency(md, bundle, exportedPkgNames);

        if (baseUri != null) {
            for (BundleArtifact bundleArtifact : bundle.getArtifacts()) {
                String type = "jar";
                String ext = "jar";
                String packaging = null;
                if (bundle.hasInnerClasspath() && !bundleArtifact.isSource()) {
                    packaging = "bundle";
                }
                if ("packed".equals(bundleArtifact.getFormat())) {
                    ext = "jar.pack.gz";
                    if (packaging != null) {
                        packaging += ",pack200";
                    } else {
                        packaging = "pack200";
                    }
                }
                if (bundleArtifact.isSource()) {
                    type = "source";
                }
                URI uri = bundleArtifact.getUri();
                if (uri != null) {
                    DefaultArtifact artifact = buildArtifact(mrid, baseUri, uri, type, ext,
                        packaging);
                    md.addArtifact(CONF_NAME_DEFAULT, artifact);
                }
            }
        }

        if (profileProvider != null) {
            for (String env : bundle.getExecutionEnvironments()) {
                ExecutionEnvironmentProfile profile = profileProvider.getProfile(env);
                if (profile == null) {
                    throw new ProfileNotFoundException("Execution environment profile " + env
                            + " not found");
                }
                for (String pkg : profile.getPkgNames()) {
                    ArtifactId id = new ArtifactId(ModuleId.newInstance(BundleInfo.PACKAGE_TYPE,
                        pkg), PatternMatcher.ANY_EXPRESSION, PatternMatcher.ANY_EXPRESSION,
                            PatternMatcher.ANY_EXPRESSION);
                    DefaultExcludeRule rule = new DefaultExcludeRule(id,
                            ExactOrRegexpPatternMatcher.INSTANCE, null);
                    String[] confs = md.getConfigurationsNames();
                    for (int i = 0; i < confs.length; i++) {
                        rule.addConfiguration(confs[i]);
                    }
                    md.addExcludeRule(rule);
                }
            }
        }

        if (manifest != null) {
            for (Entry<Object, Object> entries : manifest.getMainAttributes().entrySet()) {
                md.addExtraInfo(new ExtraInfoHolder(entries.getKey().toString(), entries.getValue()
                        .toString()));
            }
        }

        return md;
    }

    public static DefaultArtifact buildArtifact(ModuleRevisionId mrid, URI baseUri, URI uri,
            String type, String ext, String packaging) {
        DefaultArtifact artifact;
        if ("ivy".equals(uri.getScheme())) {
            artifact = decodeIvyURI(uri);
        } else {
            if (!uri.isAbsolute()) {
                uri = baseUri.resolve(uri);
            }
            Map<String, String> extraAtt = new HashMap<String, String>();
            if (packaging != null) {
                extraAtt.put("packaging", packaging);
            }
            try {
                artifact = new DefaultArtifact(mrid, null, mrid.getName(), type, ext, new URL(
                        uri.toString()), extraAtt);
            } catch (MalformedURLException e) {
                throw new RuntimeException("Unable to make the uri into the url", e);
            }
        }
        return artifact;
    }

    public static List<String> getConfigurations(BundleInfo bundle) {
        List<String> confs = new ArrayList<String>();
        confs.add(CONF_NAME_DEFAULT);
        confs.add(CONF_NAME_OPTIONAL);
        confs.add(CONF_NAME_TRANSITIVE_OPTIONAL);

        for (ExportPackage exportPackage : bundle.getExports()) {
            confs.add(CONF_USE_PREFIX + exportPackage.getName());
        }

        return confs;
    }

    public static URI buildIvyURI(Artifact artifact) {
        ModuleRevisionId mrid = artifact.getModuleRevisionId();
        return asIvyURI(mrid.getOrganisation(), mrid.getName(), mrid.getBranch(),
            mrid.getRevision(), artifact.getType(), artifact.getName(), artifact.getExt());
    }

    private static URI asIvyURI(String org, String name, String branch, String rev, String type,
            String art, String ext) {
        StringBuffer builder = new StringBuffer();
        builder.append("ivy:///");
        builder.append(org);
        builder.append('/');
        builder.append(name);
        builder.append('?');
        if (branch != null) {
            builder.append("branch=");
            builder.append(branch);
        }
        if (rev != null) {
            builder.append("&rev=");
            builder.append(rev);
        }
        if (type != null) {
            builder.append("&type=");
            builder.append(type);
        }
        if (art != null) {
            builder.append("&art=");
            builder.append(art);
        }
        if (ext != null) {
            builder.append("&ext=");
            builder.append(ext);
        }
        try {
            return new URI(builder.toString());
        } catch (URISyntaxException e) {
            throw new RuntimeException("illformed ivy url", e);
        }
    }

    private static DefaultArtifact decodeIvyURI(final URI uri) {
        String org = null;
        String name = null;
        String branch = null;
        String rev = null;
        String art = null;
        String type = null;
        String ext = null;

        String path = uri.getPath();
        if (!path.startsWith("/")) {
            throw new IllegalArgumentException(
                    "An ivy url should be of the form ivy:///org/module but was : " + uri);
        }
        int i = path.indexOf('/', 1);
        if (i < 0) {
            throw new IllegalArgumentException("Expecting an organisation in the ivy url: " + uri);
        }
        org = path.substring(1, i);
        name = path.substring(i + 1);

        String query = uri.getQuery();
        String[] parameters = query.split("&");
        for (int j = 0; j < parameters.length; j++) {
            String parameter = parameters[j];
            if (parameter.length() == 0) {
                continue;
            }
            String[] nameAndValue = parameter.split("=");
            if (nameAndValue.length != 2) {
                throw new IllegalArgumentException("Malformed query string in the ivy url: " + uri);
            } else if (nameAndValue[0].equals("branch")) {
                branch = nameAndValue[1];
            } else if (nameAndValue[0].equals("rev")) {
                rev = nameAndValue[1];
            } else if (nameAndValue[0].equals("art")) {
                art = nameAndValue[1];
            } else if (nameAndValue[0].equals("type")) {
                type = nameAndValue[1];
            } else if (nameAndValue[0].equals("ext")) {
                ext = nameAndValue[1];
            } else {
                throw new IllegalArgumentException("Unrecognized parameter '" + nameAndValue[0]
                        + " in the query string of the ivy url: " + uri);
            }
        }

        ModuleRevisionId amrid = ModuleRevisionId.newInstance(org, name, branch, rev);
        DefaultArtifact artifact = new DefaultArtifact(amrid, null, art, type, ext);
        return artifact;
    }

    private static void requirementAsDependency(DefaultModuleDescriptor md, BundleInfo bundleInfo,
            Set<String> exportedPkgNames) {
        for (BundleRequirement requirement : bundleInfo.getRequirements()) {
            String type = requirement.getType();
            String name = requirement.getName();

            if (BundleInfo.PACKAGE_TYPE.equals(type) && exportedPkgNames.contains(name)) {
                // don't declare package exported by the current bundle
                continue;
            }

            if (BundleInfo.EXECUTION_ENVIRONMENT_TYPE.equals(type)) {
                // execution environment are handled elsewhere
                continue;
            }

            ModuleRevisionId ddmrid = asMrid(type, name, requirement.getVersion());
            DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(ddmrid, false);

            String conf = CONF_NAME_DEFAULT;
            if (BundleInfo.PACKAGE_TYPE.equals(type)) {
                // declare the configuration for the package
                conf = CONF_USE_PREFIX + name;
                md.addConfiguration(new Configuration(CONF_USE_PREFIX + name, Visibility.PUBLIC,
                        "Exported package " + name, new String[] {CONF_NAME_DEFAULT}, true, null));
                dd.addDependencyConfiguration(conf, conf);
            }

            if ("optional".equals(requirement.getResolution())) {
                dd.addDependencyConfiguration(CONF_NAME_OPTIONAL, conf);
                dd.addDependencyConfiguration(CONF_NAME_TRANSITIVE_OPTIONAL,
                    CONF_NAME_TRANSITIVE_OPTIONAL);
            } else {
                dd.addDependencyConfiguration(CONF_NAME_DEFAULT, conf);
            }

            md.addDependency(dd);
        }

    }

    public static ModuleRevisionId asMrid(String type, String name, Version v) {
        return ModuleRevisionId.newInstance(type, name, v == null ? null : v.toString());
    }

    public static ModuleRevisionId asMrid(String type, String name, VersionRange v) {
        String revision;
        if (v == null) {
            revision = "[0,)";
        } else {
            revision = v.toIvyRevision();
        }
        return ModuleRevisionId.newInstance(type, name, revision);
    }

    public static class ProfileNotFoundException extends RuntimeException {

        public ProfileNotFoundException(String msg) {
            super(msg);
        }

    }
}
