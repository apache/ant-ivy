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
package org.apache.ivy.plugins.parser.m2;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.DefaultDependencyArtifactDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultExcludeRule;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.descriptor.Configuration.Visibility;
import org.apache.ivy.core.module.id.ArtifactId;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolveEngine;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.plugins.matcher.ExactPatternMatcher;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.ivy.plugins.parser.AbstractModuleDescriptorParser;
import org.apache.ivy.plugins.parser.ModuleDescriptorParser;
import org.apache.ivy.plugins.parser.ParserSettings;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorWriter;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.plugins.resolver.util.ResolvedResource;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.XMLHelper;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * A parser for Maven 2 POM.
 * <p>
 * The configurations used in the generated module descriptor mimics the behavior defined by maven 2
 * scopes, as documented here:<br/>
 * http://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html
 * 
 */
public final class PomModuleDescriptorParser extends AbstractModuleDescriptorParser {
    public static final Configuration[] MAVEN2_CONFIGURATIONS = new Configuration[] {
            new Configuration("default", Visibility.PUBLIC,
                    "runtime dependencies and master artifact can be used with this conf",
                    new String[] {"runtime", "master"}, true, null),
            new Configuration(
                    "master",
                    Visibility.PUBLIC,
                    "contains only the artifact published by this module itself, "
                    + "with no transitive dependencies",
                    new String[0], true, null),
            new Configuration(
                    "compile",
                    Visibility.PUBLIC,
                    "this is the default scope, used if none is specified. "
                    + "Compile dependencies are available in all classpaths.",
                    new String[0], true, null),
            new Configuration(
                    "provided",
                    Visibility.PUBLIC,
                    "this is much like compile, but indicates you expect the JDK or a container "
                    + "to provide it. "
                    + "It is only available on the compilation classpath, and is not transitive.",
                    new String[0], true, null),
            new Configuration(
                    "runtime",
                    Visibility.PUBLIC,
                    "this scope indicates that the dependency is not required for compilation, "
                    + "but is for execution. It is in the runtime and test classpaths, "
                    + "but not the compile classpath.",
                    new String[] {"compile"}, true, null),
            new Configuration(
                    "test",
                    Visibility.PRIVATE,
                    "this scope indicates that the dependency is not required for normal use of "
                    + "the application, and is only available for the test compilation and "
                    + "execution phases.",
                    new String[0], true, null),
            new Configuration(
                    "system",
                    Visibility.PUBLIC,
                    "this scope is similar to provided except that you have to provide the JAR "
                    + "which contains it explicitly. The artifact is always available and is not "
                    + "looked up in a repository.",
                    new String[0], true, null),
                    };

    private static final Configuration OPTIONAL_CONFIGURATION = new Configuration("optional",
            Visibility.PUBLIC, "contains all optional dependencies", new String[0], true, null);

    private static final Map MAVEN2_CONF_MAPPING = new HashMap();

    static {
        MAVEN2_CONF_MAPPING.put("compile", "compile->@(*),master(*);runtime->@(*)");
        MAVEN2_CONF_MAPPING
                .put("provided", "provided->compile(*),provided(*),runtime(*),master(*)");
        MAVEN2_CONF_MAPPING.put("runtime", "runtime->compile(*),runtime(*),master(*)");
        MAVEN2_CONF_MAPPING.put("test", "test->compile(*),runtime(*),master(*)");
        MAVEN2_CONF_MAPPING.put("system", "system->master(*)");
    }

    private static final class Parser extends AbstractParser {
        private static final String JAR_EXTENSION = "jar";
        
        private static final String DEPENDENCY_MANAGEMENT = "dependency.management";
        
        private static final String DEPENDENCY_MANAGEMENT_DELIMITER = "__";
        
        private ParserSettings settings;

        private Stack contextStack = new Stack();

        private String organisation;
        
        private String module;

        private String revision;

        private String scope;

        private String classifier;

        private String type;

        private String ext;

        private boolean optional = false;

        private List exclusions = new ArrayList();

        private DefaultDependencyDescriptor dd;

        private Map properties = new HashMap();
        
        private StringBuffer buffer = new StringBuffer();

        private String relocationOrganisation = null;
        
        private String relocationModule;

        private String relocationRevision;
        
        private String dmGroupId;
        
        private String dmArtifactId;
        
        private String dmVersion;


        public Parser(ModuleDescriptorParser parser, Resource res, ParserSettings settings) {
            super(parser);
            setResource(res);
            this.settings = settings;
            md.setResolvedPublicationDate(new Date(res.getLastModified()));
            for (int i = 0; i < MAVEN2_CONFIGURATIONS.length; i++) {
                md.addConfiguration(MAVEN2_CONFIGURATIONS[i]);
            }
        }

        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            contextStack.push(qName);
            String context = getContext();
            if ("optional".equals(qName)) {
                optional = true;
            } else if ("project/dependencies/dependency/exclusions".equals(context)) {
                if (dd == null) {
                    // stores dd now cause exclusions will override org and module
                    dd = new DefaultDependencyDescriptor(md, ModuleRevisionId.newInstance(
                        organisation, module, revision), true, false, true);
                    organisation = null;
                    module = null;
                    revision = null;
                }
            } else if (md.getModuleRevisionId() == null) {
                if ("project/dependencies".equals(context) || "project/profiles".equals(context)
                        || "project/build".equals(context)) {
                    fillMrid();
                }
            }
        }

        private void fillMrid() throws SAXException {
            if (organisation == null) {
                throw new SAXException("no groupId found in pom");
            }
            if (module == null) {
                throw new SAXException("no artifactId found in pom");
            }
            if (revision == null) {
                revision = "SNAPSHOT";
            }
            ModuleRevisionId mrid = ModuleRevisionId.newInstance(organisation, module, revision);
            properties.put("project.groupId", organisation);
            properties.put("pom.groupId", organisation);
            properties.put("project.artifactId", module);
            properties.put("pom.artifactId", module);
            properties.put("project.version", revision);
            properties.put("pom.version", revision);
            properties.put("version", revision);
            md.setModuleRevisionId(mrid);
            if (type == null) {
                type = JAR_EXTENSION;
                ext = JAR_EXTENSION;
            }
            md.setModuleArtifact(
                DefaultArtifact.newPomArtifact(mrid, getDefaultPubDate()));
            md.addArtifact("master", 
                new DefaultArtifact(mrid, getDefaultPubDate(), module, type, ext));
            organisation = null;
            module = null;
            revision = null;
        }

        public void endElement(String uri, String localName, String qName) throws SAXException {
            processTextContent();
            
            String context = getContext();
            if (md.getModuleRevisionId() == null && ("project".equals(context))) {
                fillMrid();
            } else if ("project/parent/version".equals(context)) {
                properties.put("parent.version", revision);
            } else if ("project/parent/groupId".equals(context)) {
                properties.put("parent.groupId", organisation);
            } else if (context.equals("project/parent")) {
                parseParentPom();
            } else if (((organisation != null && module != null) || dd != null)
                    && "project/dependencies/dependency".equals(context)) {
                if (revision == null) {
                    // if the revision is null, see if we can get it from the dependency management
                    String key = DEPENDENCY_MANAGEMENT + DEPENDENCY_MANAGEMENT_DELIMITER 
                        + organisation + DEPENDENCY_MANAGEMENT_DELIMITER + module;
                    revision = (String) properties.get(key);
                }
                if (dd == null) {
                    // if we still don't have revision, then we are done.
                    if (revision == null) {
                        return;
                    }
                    dd = new DefaultDependencyDescriptor(md, ModuleRevisionId.newInstance(
                        organisation, module, revision), true, false, true);
                }
                scope = scope == null ? "compile" : scope;
                if (optional && "compile".equals(scope)) {
                    scope = "runtime";
                }
                String mapping = (String) MAVEN2_CONF_MAPPING.get(scope);
                if (mapping == null) {
                    Message.verbose("unknown scope " + scope + " in " + getResource());
                    mapping = (String) MAVEN2_CONF_MAPPING.get("compile");
                }
                if (optional) {
                    mapping = mapping.replaceAll(scope + "\\-\\>", "optional->");
                    if (md.getConfiguration("optional") == null) {
                        md.addConfiguration(OPTIONAL_CONFIGURATION);
                    }
                }
                parseDepsConfs(mapping, dd);

                if (classifier != null) {
                    // we deal with classifiers by setting an extra attribute and forcing the
                    // dependency to assume such an artifact is published
                    Map extraAtt = new HashMap();
                    extraAtt.put("classifier", classifier);
                    String[] confs = dd.getModuleConfigurations();
                    for (int i = 0; i < confs.length; i++) {
                        dd.addDependencyArtifact(confs[i],
                            new DefaultDependencyArtifactDescriptor(
                                    dd.getDependencyId().getName(), JAR_EXTENSION, JAR_EXTENSION, 
                                    /*
                                     * here we have to assume a type and ext for the artifact, so
                                     * this is a limitation compared to how m2 behave with
                                     * classifiers
                                     */
                                    null, extraAtt));
                    }
                }
                for (Iterator iter = exclusions.iterator(); iter.hasNext();) {
                    ModuleId mid = (ModuleId) iter.next();
                    String[] confs = dd.getModuleConfigurations();
                    for (int i = 0; i < confs.length; i++) {
                        dd
                                .addExcludeRule(confs[i], new DefaultExcludeRule(new ArtifactId(
                                        mid, PatternMatcher.ANY_EXPRESSION,
                                        PatternMatcher.ANY_EXPRESSION,
                                        PatternMatcher.ANY_EXPRESSION),
                                        ExactPatternMatcher.INSTANCE, null));
                    }
                }
                md.addDependency(dd);
                dd = null;
            } else if ((organisation != null && module != null)
                   && "project/dependencies/dependency/exclusions/exclusion".equals(context)) {
                exclusions.add(new ModuleId(organisation, module));
                organisation = null;
                module = null;
            } else if ("project/distributionManagement/relocation".equals(context)) {
                if (relocationOrganisation == null) {
                    relocationOrganisation = organisation;
                }
                if (relocationModule == null) {
                    relocationModule = module;
                }
                if (relocationRevision == null) {
                    relocationRevision = revision;
                }
                ModuleRevisionId myModuleRev = ModuleRevisionId.newInstance(
                    organisation, module, revision);
                ModuleRevisionId relocationeModuleRev = ModuleRevisionId.newInstance(
                    relocationOrganisation, relocationModule, relocationRevision);
                md.setModuleRevisionId(myModuleRev);
                if (relocationOrganisation.equals(organisation) 
                        && relocationModule.equals(module)) {                    
                    Message.error("Relocation to an other version number not supported in ivy : "
                        + myModuleRev + " relocated to " + relocationModule 
                        + ". Please update your dependency to directly use the right version.");
                    Message.warn("Resolution will only pick dependencies of the relocated element." 
                        + "  Artefact and other metadata will be ignored.");
                    Parser relocationParser = parserOtherPom(relocationeModuleRev);
                    if (relocationParser == null) {
                        throw new SAXException("Relocation can not be found : " + relocationModule);
                    }
                    DependencyDescriptor[] dependencies = relocationParser.md.getDependencies();
                    for (int i = 0; i < dependencies.length; i++) {
                        md.addDependency(dependencies[i]);
                    }                    
                } else {
                    Message.info(myModuleRev.toString() + " is relocated to " 
                        + relocationeModuleRev + ". Please update your dependencies.");
                    Message.verbose("Relocated module will be considered as a dependency");
                    dd = new DefaultDependencyDescriptor(md, relocationeModuleRev,
                        true, false, true);
                    dd.addDependencyConfiguration("*", "@");
                    md.addDependency(dd);
                    dd = null;
                }
            } else if ("project/dependencyManagement/dependencies/dependency".equals(context)) {
                if (dmGroupId != null && dmArtifactId != null && dmVersion != null) {
                   // Note: we can't use substitute pattern, fillMrid has not been called yet.
                   String key = DEPENDENCY_MANAGEMENT + DEPENDENCY_MANAGEMENT_DELIMITER 
                       + dmGroupId + DEPENDENCY_MANAGEMENT_DELIMITER + dmArtifactId;
                   properties.put(key, dmVersion);
                }
            }
            if ("project/dependencies/dependency".equals(context)) {
                organisation = null;
                module = null;
                revision = null;
                scope = null;
                classifier = null;
                optional = false;
                exclusions.clear();
            }
            contextStack.pop();
        }

        private void processTextContent() {
            if (buffer != null) {
                String txt = IvyPatternHelper.substituteVariables(buffer.toString(), 
                        properties).trim();
                buffer = null;
                
                if (txt.length() == 0) {
                    return;
                }
                
                String context = getContext();
                if (context.equals("project/parent/groupId") && organisation == null) {
                    organisation = txt;
                    return;
                }
                if (context.equals("project/parent/version") && revision == null) {
                    revision = txt;
                    return;
                } 
                if (context.equals("project/parent/artifactId")) {
                    properties.put("parent.artifactId", txt);
                }
                if (context.equals("project/parent/packaging") && type == null) {
                    type = txt;
                    ext = txt;
                    return;
                }
                if (context.equals("project/distributionManagement/relocation/groupId")) {
                    relocationOrganisation = txt;
                    return;
                }
                if (context.equals("project/distributionManagement/relocation/artifactId")) {
                    relocationModule = txt;
                    return;
                }
                if (context.equals("project/distributionManagement/relocation/version")) {
                    relocationRevision = txt;
                    return;
                }
                if (context.startsWith("project/parent")) {
                    return;
                } 
                if (context.equals(
                    "project/dependencyManagement/dependencies/dependency/groupId")) {
                    dmGroupId = txt;
                    return;
                }
                if (context.equals(
                    "project/dependencyManagement/dependencies/dependency/artifactId")) {
                    dmArtifactId = txt;
                    return;
                }
                if (context.equals(
                    "project/dependencyManagement/dependencies/dependency/version")) {
                    dmVersion = txt;
                   return;
                }
                if (context.startsWith("project/properties")) {
                    String key = context.substring("project/properties/".length());
                    properties.put(key, txt);
                }
                if (md.getModuleRevisionId() == null
                        || context.startsWith("project/dependencies/dependency")) {
                    if (context.equals("project/groupId")) {
                        organisation = txt;
                    } else if (organisation == null && context.endsWith("groupId")) {
                        organisation = txt;
                    } else if (module == null && context.endsWith("artifactId")) {
                        module = txt;
                    } else if (context.equals("project/version")
                            || (revision == null && context.endsWith("version"))) {
                        revision = txt;
                    } else if (revision == null && context.endsWith("version")) {
                        revision = txt;
                    } else if (type == null && context.endsWith("packaging")) {
                        type = txt;
                        ext = txt;
                    } else if (scope == null && context.endsWith("scope")) {
                        scope = txt;
                    } else if (classifier == null && context.endsWith("dependency/classifier")) {
                        classifier = txt;
                    }
                }
            }
        }

        public void characters(char[] ch, int start, int length) throws SAXException {
            if (buffer == null) {
                buffer = new StringBuffer();
            }
            buffer.append(ch, start, length);
        }

        private String getContext() {
            StringBuffer buf = new StringBuffer();
            for (Iterator iter = contextStack.iterator(); iter.hasNext();) {
                String ctx = (String) iter.next();
                buf.append(ctx).append("/");
            }
            if (buf.length() > 0) {
                buf.setLength(buf.length() - 1);
            }
            return buf.toString();
        }

        public ModuleDescriptor getDescriptor() {
            if (md.getModuleRevisionId() == null) {
                return null;
            }
            
            return md;
        }
        
        public void parseParentPom() throws SAXException {
            String parentOrg = (String) properties.get("parent.groupId");
            String parentName = (String) properties.get("parent.artifactId");
            String parentVersion = (String) properties.get("parent.version");
            
            if (parentOrg != null && parentName != null && parentVersion != null) {
                ModuleRevisionId parent = ModuleRevisionId.newInstance(parentOrg, parentName, 
                                           parentVersion);
                Parser parser = parserOtherPom(parent);
                if (parser == null) {
                    //see comments in parserOtherPom for case where parser==nul 
                    return;
                }
                
                // move the parent properties into ours
                Map parentProps = parser.properties;
                Set keys = parentProps.keySet();
                for (Iterator iter = keys.iterator(); iter.hasNext();) {
                    String key = iter.next().toString();
                    if (key.startsWith("pom")) {
                        // don't see a need to copy pom values from parent...
                        // ignore
                    } else if (key.startsWith("parent")) {
                        // don't see a need to copy parent values from parent...
                        // ignore
                    } else {
                        // the key may need the groupId substituted
                        String fullKey = IvyPatternHelper.substituteVariables(key, 
                                parentProps).trim();
                        String fullValue = IvyPatternHelper.substituteVariables(
                                parentProps.get(key).toString(), parentProps).trim();
                        properties.put(fullKey, fullValue);
                    }
                }

            }
        }

        private Parser parserOtherPom(ModuleRevisionId other) throws SAXException {
            DependencyResolver resolver = settings.getResolver(other.getModuleId());
            if (resolver == null) {
                // TODO: Maybe log warning or throw exception here?
                return null;
            }
            
            DependencyDescriptor dd = new DefaultDependencyDescriptor(other, true);
            ResolveData data = IvyContext.getContext().getResolveData();
            if (data == null) {
                ResolveEngine engine = IvyContext.getContext().getIvy().getResolveEngine();
                ResolveOptions options = new ResolveOptions();
                options.setCache(IvyContext.getContext().getCacheManager());
                options.setDownload(false);
                data = new ResolveData(engine, options);
            }
            
            ResolvedResource rr = resolver.findIvyFileRef(dd, data);
            
            if (rr == null) {
                // parent not found. Maybe we should throw an exception here?
                return null;
            }
            
            Parser parser = new Parser(getModuleDescriptorParser(), rr.getResource(), settings);
            InputStream pomStream = null;
            try {
                pomStream = rr.getResource().openStream();
                XMLHelper.parse(pomStream, null, parser, null);
            } catch (IOException e) {
                throw new SAXException("Error occurred while parsing parent", e);
            } catch (ParserConfigurationException e) {
                throw new SAXException("Error occurred while parsing parent", e);
            } finally {
                if (pomStream != null) {
                    try {
                        pomStream.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
            return parser;
        }
    }

    private static final PomModuleDescriptorParser INSTANCE = new PomModuleDescriptorParser();

    public static PomModuleDescriptorParser getInstance() {
        return INSTANCE;
    }

    private PomModuleDescriptorParser() {
    }

    public ModuleDescriptor parseDescriptor(ParserSettings settings, URL descriptorURL, 
            Resource res, boolean validate) throws ParseException, IOException {
        Parser parser = new Parser(this, res, settings);
        try {
            XMLHelper.parse(descriptorURL, null, parser);
        } catch (SAXException ex) {
            ParseException pe = new ParseException(ex.getMessage() + " in " + descriptorURL, 0);
            pe.initCause(ex);
            throw pe;
        } catch (ParserConfigurationException ex) {
            IllegalStateException ise = new IllegalStateException(ex.getMessage() + " in "
                    + descriptorURL);
            ise.initCause(ex);
            throw ise;
        }
        return parser.getDescriptor();
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

}
