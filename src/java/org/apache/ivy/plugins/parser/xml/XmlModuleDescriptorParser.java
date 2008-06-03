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
package org.apache.ivy.plugins.parser.xml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.ConfigurationAware;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.DefaultDependencyArtifactDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultExcludeRule;
import org.apache.ivy.core.module.descriptor.DefaultIncludeRule;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyArtifactDescriptor;
import org.apache.ivy.core.module.descriptor.ExcludeRule;
import org.apache.ivy.core.module.descriptor.IncludeRule;
import org.apache.ivy.core.module.descriptor.License;
import org.apache.ivy.core.module.descriptor.MDArtifact;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.descriptor.OverrideDependencyDescriptorMediator;
import org.apache.ivy.core.module.id.ArtifactId;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.conflict.ConflictManager;
import org.apache.ivy.plugins.conflict.FixedConflictManager;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.ivy.plugins.namespace.Namespace;
import org.apache.ivy.plugins.parser.AbstractModuleDescriptorParser;
import org.apache.ivy.plugins.parser.ModuleDescriptorParser;
import org.apache.ivy.plugins.parser.ParserSettings;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.plugins.repository.url.URLResource;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.XMLHelper;
import org.apache.ivy.util.extendable.ExtendableItemHelper;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * Parses an xml ivy file and output a ModuleDescriptor. For dependency and performance reasons, it
 * uses only the SAX API, which makes the parsing code harder to understand.
 */
public final class XmlModuleDescriptorParser extends AbstractModuleDescriptorParser {
    static final String[] DEPENDENCY_REGULAR_ATTRIBUTES = new String[] {"org", "name", "branch",
            "branchConstraint", "rev", "revConstraint", "force", "transitive", "changing", "conf"};

    private static final XmlModuleDescriptorParser INSTANCE = new XmlModuleDescriptorParser();

    public static XmlModuleDescriptorParser getInstance() {
        return INSTANCE;
    }

    private XmlModuleDescriptorParser() {

    }

    /**
     * @param ivy
     * @param xmlURL
     *            the url pointing to the file to parse
     * @param res
     *            the real resource to parse, used for log only
     * @param validate
     * @return
     * @throws ParseException
     * @throws IOException
     */
    public ModuleDescriptor parseDescriptor(ParserSettings ivySettings, URL xmlURL, Resource res,
            boolean validate) throws ParseException, IOException {
        Parser parser = new Parser(this, ivySettings, validate, xmlURL);
        parser.parse(res, validate);
        return parser.getModuleDescriptor();
    }

    /** Used for test purpose */
    ModuleDescriptor parseDescriptor(ParserSettings ivySettings, InputStream descriptor,
            Resource res, boolean validate) throws ParseException, IOException {
        Parser parser = new Parser(this, ivySettings, validate, null);
        parser.parse(descriptor, res, validate);
        return parser.getModuleDescriptor();
    }

    public boolean accept(Resource res) {
        return true; // this the default parser, it thus accepts all resources
    }

    public void toIvyFile(InputStream is, Resource res, File destFile, ModuleDescriptor md)
            throws IOException, ParseException {
        try {
            Namespace ns = null;
            if (md instanceof DefaultModuleDescriptor) {
                DefaultModuleDescriptor dmd = (DefaultModuleDescriptor) md;
                ns = dmd.getNamespace();
            }
            XmlModuleDescriptorUpdater.update(is, res, destFile, 
                    new UpdateOptions()
                        .setSettings(IvyContext.getContext().getSettings())
                        .setStatus(md.getStatus()) 
                        .setRevision(md.getResolvedModuleRevisionId().getRevision()) 
                        .setPubdate(md.getResolvedPublicationDate())
                        .setNamespace(ns));
        } catch (SAXException e) {
            ParseException ex = new ParseException("exception occured while parsing " + res, 0);
            ex.initCause(e);
            throw ex;
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    private static class Parser extends AbstractParser {

        private static final List ALLOWED_VERSIONS = Arrays.asList(
            new String[] {"1.0", "1.1", "1.2", "1.3", "1.4", "2.0"});

        private DefaultDependencyDescriptor dd;

        private ConfigurationAware confAware;

        private MDArtifact artifact;

        private String conf;

        private boolean validate = true;

        private ParserSettings ivy;

        private boolean artifactsDeclared = false;

        private PatternMatcher defaultMatcher;

        private static final int NONE = 0;

        private static final int INFO = 1;

        private static final int CONF = 2;

        private static final int PUB = 3;

        private static final int DEP = 4;

        private static final int DEP_ARTIFACT = 5;

        private static final int ARTIFACT_INCLUDE = 6;

        private static final int ARTIFACT_EXCLUDE = 7;

        private static final int CONFLICT = 8;

        private static final int EXCLUDE = 9;

        private static final int DEPS = 10;
        
        private static final int DESCRIPTION = 11;

        private static final int EXTRA_INFO = 12;
        
        private int state = NONE;

        private final URL xmlURL;

        private StringBuffer buffer;

        private String descriptorVersion;
        
        

        public Parser(ModuleDescriptorParser parser, ParserSettings ivySettings, boolean validate,
                URL xmlURL) {
            super(parser);
            ivy = ivySettings;
            this.validate = validate;
            this.xmlURL = xmlURL;
        }

        private void parse(Resource res, boolean validate) throws ParseException,
                IOException {
            try {
                setResource(res);
                URL schemaURL = validate ? getClass().getResource("ivy.xsd") : null;
                XMLHelper.parse(xmlURL, schemaURL, this);
                checkConfigurations();
                replaceConfigurationWildcards();
                getMd().setModuleArtifact(
                    DefaultArtifact.newIvyArtifact(
                        getMd().getResolvedModuleRevisionId(), getMd().getPublicationDate()));
                if (!artifactsDeclared) {
                    String[] confs = getMd().getConfigurationsNames();
                    for (int i = 0; i < confs.length; i++) {
                        getMd().addArtifact(confs[i], 
                            new MDArtifact(getMd(), getMd().getModuleRevisionId().getName(), 
                                "jar", "jar"));
                    }
                }
                getMd().check();
            } catch (ParserConfigurationException ex) {
                IllegalStateException ise = new IllegalStateException(ex.getMessage() + " in "
                        + xmlURL);
                ise.initCause(ex);
                throw ise;
            } catch (Exception ex) {
                checkErrors();
                ParseException pe = new ParseException(ex.getMessage() + " in " + xmlURL, 0);
                pe.initCause(ex);
                throw pe;
            }
        }

        private void parse(InputStream descriptor, Resource res, boolean validate)
                throws ParseException, IOException {
            try {
                setResource(res);
                URL schemaURL = validate ? getClass().getResource("ivy.xsd") : null;
                XMLHelper.parse(descriptor, schemaURL, this, null);
                checkConfigurations();
                replaceConfigurationWildcards();
                if (!artifactsDeclared) {
                    String[] confs = getMd().getConfigurationsNames();
                    for (int i = 0; i < confs.length; i++) {
                        getMd().addArtifact(confs[i], 
                            new MDArtifact(getMd(), getMd().getModuleRevisionId().getName(), 
                                "jar", "jar"));
                    }
                }
                getMd().check();
            } catch (ParserConfigurationException ex) {
                IllegalStateException ise = new IllegalStateException(ex.getMessage());
                ise.initCause(ex);
                throw ise;
            } catch (Exception ex) {
                checkErrors();
                ParseException pe = new ParseException(ex.getMessage(), 0);
                pe.initCause(ex);
                throw pe;
            }
        }

        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            try {
                if (state == DESCRIPTION) {
                    //make sure we don't interpret any tag while in description tag 
                    buffer.append("<" + qName + ">");
                    return;
                } else if ("ivy-module".equals(qName)) {
                    ivyModuleStarted(attributes);
                } else if ("info".equals(qName)) {
                    infoStarted(attributes);
                } else if (state == INFO && "license".equals(qName)) {
                    getMd().addLicense(new License(ivy.substitute(attributes.getValue("name")), ivy
                            .substitute(attributes.getValue("url"))));
                } else if (state == INFO && "description".equals(qName)) {
                    getMd().setHomePage(ivy.substitute(attributes.getValue("homepage")));
                    state = DESCRIPTION;
                    buffer = new StringBuffer();
                } else if (state == INFO && "ivyauthor".equals(qName)) {
                    // nothing to do, we don't store this
                } else if (state == INFO && "repository".equals(qName)) {
                    // nothing to do, we don't store this
                } else if (state == INFO && isOtherNamespace(qName)) {
                    buffer = new StringBuffer();
                    state = EXTRA_INFO;
                } else if ("configurations".equals(qName)) {
                    configurationStarted(attributes);
                } else if ("publications".equals(qName)) {
                    state = PUB;
                    artifactsDeclared = true;
                    checkConfigurations();
                } else if ("dependencies".equals(qName)) {
                    dependenciesStarted(attributes);
                } else if ("conflicts".equals(qName)) {
                    if (!descriptorVersion.startsWith("1.")) {
                        Message.deprecated("using conflicts section is deprecated: "
                            + "please use hints section instead. Ivy file URL: " + xmlURL);
                    }
                    state = CONFLICT;
                    checkConfigurations();
                } else if ("artifact".equals(qName)) {
                    artifactStarted(qName, attributes);
                } else if ("include".equals(qName) && state == DEP) {
                    addIncludeRule(qName, attributes);
                } else if ("exclude".equals(qName) && state == DEP) {
                    addExcludeRule(qName, attributes);
                } else if ("exclude".equals(qName) && state == DEPS) {
                    state = EXCLUDE;
                    parseRule(qName, attributes);
                    getMd().addExcludeRule((ExcludeRule) confAware);
                } else if ("dependency".equals(qName)) {
                    dependencyStarted(attributes);
                } else if ("conf".equals(qName)) {
                    confStarted(attributes);
                } else if ("mapped".equals(qName)) {
                    dd.addDependencyConfiguration(conf, ivy.substitute(attributes
                            .getValue("name")));
                } else if (("conflict".equals(qName) && state == DEPS)
                        || "manager".equals(qName) && state == CONFLICT) {
                    managerStarted(attributes, state == CONFLICT ? "name" : "manager");
                } else if ("override".equals(qName) && state == DEPS) {
                    mediationOverrideStarted(attributes);
                } else if ("include".equals(qName) && state == CONF) {
                    includeConfStarted(attributes);
                } else if (validate && state != EXTRA_INFO && state != DESCRIPTION) {
                    addError("unknown tag " + qName);
                }
            } catch (Exception ex) {
                if (ex instanceof SAXException) {
                    throw (SAXException) ex;
                }
                SAXException sax = new SAXException("Problem occured while parsing ivy file: "
                        + ex.getMessage(), ex);
                sax.initCause(ex);
                throw sax;
            }
        }

        private boolean isOtherNamespace(String qName) {
            return qName.indexOf(':') != -1;
        }

        private void managerStarted(Attributes attributes, String managerAtt) {
            String org = ivy.substitute(attributes.getValue("org"));
            org = org == null ? PatternMatcher.ANY_EXPRESSION : org;
            String mod = ivy.substitute(attributes.getValue("module"));
            mod = mod == null ? PatternMatcher.ANY_EXPRESSION : mod;
            ConflictManager cm;
            String name = ivy.substitute(attributes.getValue(managerAtt));
            String rev = ivy.substitute(attributes.getValue("rev"));
            if (rev != null) {
                String[] revs = rev.split(",");
                for (int i = 0; i < revs.length; i++) {
                    revs[i] = revs[i].trim();
                }
                cm = new FixedConflictManager(revs);
            } else if (name != null) {
                cm = ivy.getConflictManager(name);
                if (cm == null) {
                    addError("unknown conflict manager: " + name);
                    return;
                }
            } else {
                addError("bad conflict manager: no manager nor rev");
                return;
            }
            String matcherName = ivy.substitute(attributes.getValue("matcher"));
            PatternMatcher matcher = matcherName == null ? defaultMatcher : ivy
                    .getMatcher(matcherName);
            if (matcher == null) {
                addError("unknown matcher: " + matcherName);
                return;
            }
            getMd().addConflictManager(new ModuleId(org, mod), matcher, cm);
        }

        private void mediationOverrideStarted(Attributes attributes) {
            String org = ivy.substitute(attributes.getValue("org"));
            org = org == null ? PatternMatcher.ANY_EXPRESSION : org;
            String mod = ivy.substitute(attributes.getValue("module"));
            mod = mod == null ? PatternMatcher.ANY_EXPRESSION : mod;
            String rev = ivy.substitute(attributes.getValue("rev"));
            String branch = ivy.substitute(attributes.getValue("branch"));
            String matcherName = ivy.substitute(attributes.getValue("matcher"));
            PatternMatcher matcher = matcherName == null ? defaultMatcher : ivy
                    .getMatcher(matcherName);
            if (matcher == null) {
                addError("unknown matcher: " + matcherName);
                return;
            }
            getMd().addDependencyDescriptorMediator(
                new ModuleId(org, mod), matcher, 
                new OverrideDependencyDescriptorMediator(branch, rev));
        }

        private void includeConfStarted(Attributes attributes) 
                throws SAXException, IOException, ParserConfigurationException, ParseException {
            URL url = ivy.getRelativeUrlResolver().getURL(xmlURL,
                    ivy.substitute(attributes.getValue("file")),
                    ivy.substitute(attributes.getValue("url")));
            
            if (url == null) {
                throw new SAXException("include tag must have a file or an url attribute");
            }
            
            // create a new temporary parser to read the configurations from
            // the specified file.
            Parser parser = new Parser(getModuleDescriptorParser(), ivy, false, url);
            parser.setMd(new DefaultModuleDescriptor(getModuleDescriptorParser(),
                    new URLResource(url)));
            XMLHelper.parse(url , null, parser);

            // add the configurations from this temporary parser to this module descriptor
            Configuration[] configs = parser.getModuleDescriptor().getConfigurations();
            for (int i = 0; i < configs.length; i++) {
                getMd().addConfiguration(configs[i]);
            }
            if (parser.getDefaultConfMapping() != null) {
                Message.debug("setting default conf from imported configurations file: "
                        + parser.getDefaultConfMapping());
                setDefaultConfMapping(parser.getDefaultConfMapping());
            }
            if (parser.getMd().isMappingOverride()) {
                Message.debug("enabling mapping-override from imported configurations" 
                        + " file");
                getMd().setMappingOverride(true);
            }
        }

        private void confStarted(Attributes attributes) {
            String conf = ivy.substitute(attributes.getValue("name"));
            switch (state) {
                case CONF:
                    String visibility = ivy.substitute(attributes.getValue("visibility"));
                    String ext = ivy.substitute(attributes.getValue("extends"));
                    String transitiveValue = attributes.getValue("transitive");
                    boolean transitive = (transitiveValue == null) ? true : Boolean
                            .valueOf(attributes.getValue("transitive")).booleanValue();
                    String deprecated = attributes.getValue("deprecated");
                    Configuration configuration = new Configuration(conf,
                            Configuration.Visibility
                                    .getVisibility(visibility == null ? "public"
                                            : visibility), ivy.substitute(attributes
                                    .getValue("description")), ext == null ? null : ext
                                    .split(","), transitive, deprecated);
                    ExtendableItemHelper.fillExtraAttributes(ivy, configuration, attributes,
                        new String[] {"name", "visibility", "extends", "transitive",
                                "description", "deprecated"});
                    getMd().addConfiguration(configuration);
                    break;
                case PUB:
                    if ("*".equals(conf)) {
                        String[] confs = getMd().getConfigurationsNames();
                        for (int i = 0; i < confs.length; i++) {
                            artifact.addConfiguration(confs[i]);
                            getMd().addArtifact(confs[i], artifact);
                        }
                    } else {
                        artifact.addConfiguration(conf);
                        getMd().addArtifact(conf, artifact);
                    }
                    break;
                case DEP:
                    this.conf = conf;
                    String mappeds = ivy.substitute(attributes.getValue("mapped"));
                    if (mappeds != null) {
                        String[] mapped = mappeds.split(",");
                        for (int i = 0; i < mapped.length; i++) {
                            dd.addDependencyConfiguration(conf, mapped[i].trim());
                        }
                    }
                    break;
                case DEP_ARTIFACT:
                case ARTIFACT_INCLUDE:
                case ARTIFACT_EXCLUDE:
                    addConfiguration(conf);
                    break;
                default:
                    if (validate) {
                        addError("conf tag found in invalid tag: " + state);
                    }
                    break;
            }
        }

        private void dependencyStarted(Attributes attributes) {
            state = DEP;
            String org = ivy.substitute(attributes.getValue("org"));
            if (org == null) {
                org = getMd().getModuleRevisionId().getOrganisation();
            }
            boolean force = Boolean.valueOf(ivy.substitute(attributes.getValue("force")))
                    .booleanValue();
            boolean changing = Boolean.valueOf(
                ivy.substitute(attributes.getValue("changing"))).booleanValue();

            String transitiveValue = ivy.substitute(attributes.getValue("transitive"));
            boolean transitive = (transitiveValue == null) ? true : Boolean.valueOf(
                attributes.getValue("transitive")).booleanValue();

            String name = ivy.substitute(attributes.getValue("name"));
            String branch = ivy.substitute(attributes.getValue("branch"));
            String branchConstraint = ivy.substitute(attributes.getValue("branchConstraint"));
            String rev = ivy.substitute(attributes.getValue("rev"));
            String revConstraint = ivy.substitute(attributes.getValue("revConstraint"));
            revConstraint = revConstraint == null ? rev : revConstraint;
            Map extraAttributes = ExtendableItemHelper.getExtraAttributes(
                ivy, attributes, DEPENDENCY_REGULAR_ATTRIBUTES);
            dd = new DefaultDependencyDescriptor(
                getMd(), 
                ModuleRevisionId.newInstance(org, name, branch, rev, extraAttributes), 
                ModuleRevisionId.newInstance(
                    org, name, branchConstraint, revConstraint, extraAttributes), 
                force, changing, transitive);
            getMd().addDependency(dd);
            String confs = ivy.substitute(attributes.getValue("conf"));
            if (confs != null && confs.length() > 0) {
                parseDepsConfs(confs, dd);
            }
        }

        private void artifactStarted(String qName, Attributes attributes) 
                throws MalformedURLException {
            if (state == PUB) {
                // this is a published artifact
                String artName = ivy.substitute(attributes.getValue("name"));
                artName = artName == null ? getMd().getModuleRevisionId().getName() : artName;
                String type = ivy.substitute(attributes.getValue("type"));
                type = type == null ? "jar" : type;
                String ext = ivy.substitute(attributes.getValue("ext"));
                ext = ext != null ? ext : type;
                String url = ivy.substitute(attributes.getValue("url"));
                artifact = new MDArtifact(getMd(), artName, type, ext, url == null ? null
                        : new URL(url), ExtendableItemHelper.getExtraAttributes(ivy, attributes,
                    new String[] {"ext", "type", "name", "conf"}));
                String confs = ivy.substitute(attributes.getValue("conf"));
                // only add confs if they are specified. if they aren't, endElement will
                // handle this
                // only if there are no conf defined in sub elements
                if (confs != null && confs.length() > 0) {
                    String[] conf;
                    if ("*".equals(confs)) {
                        conf = getMd().getConfigurationsNames();
                    } else {
                        conf = confs.split(",");
                    }
                    for (int i = 0; i < conf.length; i++) {
                        artifact.addConfiguration(conf[i].trim());
                        getMd().addArtifact(conf[i].trim(), artifact);
                    }
                }
            } else if (state == DEP) {
                // this is an artifact asked for a particular dependency
                addDependencyArtifacts(qName, attributes);
            } else if (validate) {
                addError("artifact tag found in invalid tag: " + state);
            }
        }

        private void dependenciesStarted(Attributes attributes) {
            state = DEPS;
            String defaultConf = ivy.substitute(attributes.getValue("defaultconf"));
            if (defaultConf != null) {
                setDefaultConf(defaultConf);
            }
            defaultConf = ivy.substitute(attributes.getValue("defaultconfmapping"));
            if (defaultConf != null) {
                setDefaultConfMapping(defaultConf);
            }
            String confMappingOverride = ivy.substitute(attributes
                    .getValue("confmappingoverride"));
            if (confMappingOverride != null) {
                getMd().setMappingOverride(Boolean.valueOf(confMappingOverride).booleanValue());
            }
            checkConfigurations();
        }

        private void configurationStarted(Attributes attributes) {
            state = CONF;
            setDefaultConfMapping(ivy
                    .substitute(attributes.getValue("defaultconfmapping")));
            getMd()
                    .setMappingOverride(Boolean.valueOf(
                        ivy.substitute(attributes.getValue("confmappingoverride")))
                            .booleanValue());
        }

        private void infoStarted(Attributes attributes) {
            state = INFO;
            String org = ivy.substitute(attributes.getValue("organisation"));
            String module = ivy.substitute(attributes.getValue("module"));
            String revision = ivy.substitute(attributes.getValue("revision"));
            String branch = ivy.substitute(attributes.getValue("branch"));
            getMd().setModuleRevisionId(ModuleRevisionId.newInstance(org, module, branch,
                revision, ExtendableItemHelper.getExtraAttributes(ivy, attributes, new String[] {
                        "organisation", "module", "revision", "status", "publication",
                        "branch", "namespace", "default", "resolver"})));

            String namespace = ivy.substitute(attributes.getValue("namespace"));
            if (namespace != null) {
                Namespace ns = ivy.getNamespace(namespace);
                if (ns == null) {
                    Message.warn("namespace not found for " + getMd().getModuleRevisionId()
                            + ": " + namespace);
                } else {
                    getMd().setNamespace(ns);
                }
            }

            String status = ivy.substitute(attributes.getValue("status"));
            getMd().setStatus(status == null ? ivy.getStatusManager().getDefaultStatus()
                    : status);
            getMd().setDefault(Boolean.valueOf(ivy.substitute(attributes.getValue("default")))
                    .booleanValue());
            String pubDate = ivy.substitute(attributes.getValue("publication"));
            if (pubDate != null && pubDate.length() > 0) {
                try {
                    getMd().setPublicationDate(Ivy.DATE_FORMAT.parse(pubDate));
                } catch (ParseException e) {
                    addError("invalid publication date format: " + pubDate);
                    getMd().setPublicationDate(getDefaultPubDate());
                }
            } else {
                getMd().setPublicationDate(getDefaultPubDate());
            }
        }

        private void ivyModuleStarted(Attributes attributes) throws SAXException {
            descriptorVersion = attributes.getValue("version");
            int versionIndex = ALLOWED_VERSIONS.indexOf(descriptorVersion);
            if (versionIndex == -1) {
                addError("invalid version " + descriptorVersion);
                throw new SAXException("invalid version " + descriptorVersion);
            }
            if (versionIndex >= ALLOWED_VERSIONS.indexOf("1.3")) {
                Message.debug("post 1.3 ivy file: using " + PatternMatcher.EXACT
                        + " as default matcher");
                defaultMatcher = ivy.getMatcher(PatternMatcher.EXACT);
            } else {
                Message.debug("pre 1.3 ivy file: using " + PatternMatcher.EXACT_OR_REGEXP
                        + " as default matcher");
                defaultMatcher = ivy.getMatcher(PatternMatcher.EXACT_OR_REGEXP);
            }
            
            for (int i = 0; i < attributes.getLength(); i++) {
                if (attributes.getQName(i).startsWith("xmlns:")) {
                    getMd().addExtraAttributeNamespace(
                        attributes.getQName(i).substring("xmlns:".length()), 
                        attributes.getValue(i));
                }
            }
        }

        private void addDependencyArtifacts(String tag, Attributes attributes)
                throws MalformedURLException {
            state = DEP_ARTIFACT;
            parseRule(tag, attributes);
        }

        private void addIncludeRule(String tag, Attributes attributes) 
                throws MalformedURLException {
            state = ARTIFACT_INCLUDE;
            parseRule(tag, attributes);
        }

        private void addExcludeRule(String tag, Attributes attributes) 
                throws MalformedURLException {
            state = ARTIFACT_EXCLUDE;
            parseRule(tag, attributes);
        }

        private void parseRule(String tag, Attributes attributes) throws MalformedURLException {
            String name = ivy.substitute(attributes.getValue("name"));
            if (name == null) {
                name = ivy.substitute(attributes.getValue("artifact"));
                if (name == null) {
                    name = "artifact".equals(tag) ? dd.getDependencyId().getName()
                            : PatternMatcher.ANY_EXPRESSION;
                }
            }
            String type = ivy.substitute(attributes.getValue("type"));
            if (type == null) {
                type = "artifact".equals(tag) ? "jar" : PatternMatcher.ANY_EXPRESSION;
            }
            String ext = ivy.substitute(attributes.getValue("ext"));
            ext = ext != null ? ext : type;
            if (state == DEP_ARTIFACT) {
                String url = ivy.substitute(attributes.getValue("url"));
                Map extraAtt = ExtendableItemHelper.getExtraAttributes(ivy, attributes, 
                    new String[] {"name", "type", "ext", "url", "conf"});
                confAware = new DefaultDependencyArtifactDescriptor(dd, name, type, ext,
                        url == null ? null : new URL(url), extraAtt);
            } else if (state == ARTIFACT_INCLUDE) {
                PatternMatcher matcher = getPatternMatcher(attributes.getValue("matcher"));
                String org = ivy.substitute(attributes.getValue("org"));
                org = org == null ? PatternMatcher.ANY_EXPRESSION : org;
                String module = ivy.substitute(attributes.getValue("module"));
                module = module == null ? PatternMatcher.ANY_EXPRESSION : module;
                ArtifactId aid = new ArtifactId(new ModuleId(org, module), name, type, ext);
                Map extraAtt = ExtendableItemHelper.getExtraAttributes(ivy, attributes, 
                    new String[] {"org", "module", "name", "type", "ext", "matcher", "conf"});
                confAware = new DefaultIncludeRule(aid, matcher, extraAtt);
            } else { // _state == ARTIFACT_EXCLUDE || EXCLUDE
                PatternMatcher matcher = getPatternMatcher(attributes.getValue("matcher"));
                String org = ivy.substitute(attributes.getValue("org"));
                org = org == null ? PatternMatcher.ANY_EXPRESSION : org;
                String module = ivy.substitute(attributes.getValue("module"));
                module = module == null ? PatternMatcher.ANY_EXPRESSION : module;
                ArtifactId aid = new ArtifactId(new ModuleId(org, module), name, type, ext);
                Map extraAtt = ExtendableItemHelper.getExtraAttributes(ivy, attributes, 
                    new String[] {"org", "module", "name", "type", "ext", "matcher", "conf"});
                confAware = new DefaultExcludeRule(aid, matcher, extraAtt);
            }
            String confs = ivy.substitute(attributes.getValue("conf"));
            // only add confs if they are specified. if they aren't, endElement will handle this
            // only if there are no conf defined in sub elements
            if (confs != null && confs.length() > 0) {
                String[] conf;
                if ("*".equals(confs)) {
                    conf = getMd().getConfigurationsNames();
                } else {
                    conf = confs.split(",");
                }
                for (int i = 0; i < conf.length; i++) {
                    addConfiguration(conf[i].trim());
                }
            }
        }

        private void addConfiguration(String c) {
            confAware.addConfiguration(c);
            if (state == EXCLUDE) {
                // we are adding a configuration to a module wide exclude rule
                // we have nothing special to do here, the rule has already been added to the module
                // descriptor
            } else {
                // we are currently adding a configuration to either an include, exclude or artifact
                // element
                // of a dependency. This means that we have to add this element to the corresponding
                // conf
                // of the current dependency descriptor
                if (confAware instanceof DependencyArtifactDescriptor) {
                    dd.addDependencyArtifact(c, (DependencyArtifactDescriptor) confAware);
                } else if (confAware instanceof IncludeRule) {
                    dd.addIncludeRule(c, (IncludeRule) confAware);
                } else if (confAware instanceof ExcludeRule) {
                    dd.addExcludeRule(c, (ExcludeRule) confAware);
                }
            }
        }

        private PatternMatcher getPatternMatcher(String m) {
            String matcherName = ivy.substitute(m);
            PatternMatcher matcher = matcherName == null ? defaultMatcher : ivy
                    .getMatcher(matcherName);
            if (matcher == null) {
                throw new IllegalArgumentException("unknown matcher " + matcherName);
            }
            return matcher;
        }

        
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (buffer != null) {
                buffer.append(ch, start, length);
            }            
        }

        
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (state == PUB && "artifact".equals(qName)
                    && artifact.getConfigurations().length == 0) {
                String[] confs = getMd().getConfigurationsNames();
                for (int i = 0; i < confs.length; i++) {
                    artifact.addConfiguration(confs[i]);
                    getMd().addArtifact(confs[i], artifact);
                }
            } else if ("configurations".equals(qName)) {
                checkConfigurations();
            } else if ((state == DEP_ARTIFACT && "artifact".equals(qName))
                    || (state == ARTIFACT_INCLUDE && "include".equals(qName))
                    || (state == ARTIFACT_EXCLUDE && "exclude".equals(qName))) {
                state = DEP;
                if (confAware.getConfigurations().length == 0) {
                    String[] confs = getMd().getConfigurationsNames();
                    for (int i = 0; i < confs.length; i++) {
                        addConfiguration(confs[i]);
                    }
                }
                confAware = null;
            } else if ("exclude".equals(qName) && state == EXCLUDE) {
                if (confAware.getConfigurations().length == 0) {
                    String[] confs = getMd().getConfigurationsNames();
                    for (int i = 0; i < confs.length; i++) {
                        addConfiguration(confs[i]);
                    }
                }
                confAware = null;
                state = DEPS;
            } else if ("dependency".equals(qName) && state == DEP) {
                if (dd.getModuleConfigurations().length == 0) {
                    parseDepsConfs(getDefaultConf(), dd);
                }
                state = DEPS;
            } else if ("dependencies".equals(qName) && state == DEPS) {
                state = NONE;
            } else if (state == INFO && "info".equals(qName)) {
                state = NONE;
            } else if (state == DESCRIPTION && "description".equals(qName)) {
                getMd().setDescription(buffer == null ? "" : buffer.toString().trim());
                buffer = null;
                state = INFO;
            } else if (state == EXTRA_INFO) {
                getMd().addExtraInfo(qName, buffer == null ? "" : buffer.toString());
                buffer = null;
                state = INFO;
            } else if (state == DESCRIPTION) {
                if (buffer.toString().endsWith("<" + qName + ">")) {
                    buffer.deleteCharAt(buffer.length() - 1);
                    buffer.append("/>");
                } else {
                    buffer.append("</" + qName + ">");
                }
            }
        }

        private void checkConfigurations() {
            if (getMd().getConfigurations().length == 0) {
                getMd().addConfiguration(new Configuration("default"));
            }
        }

        private void replaceConfigurationWildcards() {
            Configuration[] configs = getMd().getConfigurations();
            for (int i = 0; i < configs.length; i++) {
                configs[i].replaceWildcards(getMd());
            }
        }

    }

    public String toString() {
        return "ivy parser";
    }
}
