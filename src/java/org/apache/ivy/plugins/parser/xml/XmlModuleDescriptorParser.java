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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.ConfigurationAware;
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
import org.apache.ivy.core.module.id.ArtifactId;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.conflict.ConflictManager;
import org.apache.ivy.plugins.conflict.FixedConflictManager;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.ivy.plugins.namespace.Namespace;
import org.apache.ivy.plugins.parser.AbstractModuleDescriptorParser;
import org.apache.ivy.plugins.parser.ModuleDescriptorParser;
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
public class XmlModuleDescriptorParser extends AbstractModuleDescriptorParser {
    static final String[] DEPENDENCY_REGULAR_ATTRIBUTES = new String[] {"org", "name", "branch",
            "rev", "force", "transitive", "changing", "conf"};

    private static XmlModuleDescriptorParser INSTANCE = new XmlModuleDescriptorParser();

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
    public ModuleDescriptor parseDescriptor(IvySettings ivySettings, URL xmlURL, Resource res,
            boolean validate) throws ParseException, IOException {
        Parser parser = new Parser(this, ivySettings, validate, xmlURL);
        parser.parse(res, validate);
        return parser.getModuleDescriptor();
    }

    /** Used for test purpose */
    ModuleDescriptor parseDescriptor(IvySettings ivySettings, InputStream descriptor,
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
            XmlModuleDescriptorUpdater.update(IvyContext.getContext().getSettings(), is, res, 
                    destFile, Collections.EMPTY_MAP, md.getStatus(), 
                    md.getResolvedModuleRevisionId().getRevision(), 
                    md.getResolvedPublicationDate(), ns, true, null);
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

        private DefaultDependencyDescriptor _dd;

        private ConfigurationAware _confAware;

        private MDArtifact _artifact;

        private String _conf;

        private boolean _validate = true;

        private IvySettings _ivy;

        private boolean _artifactsDeclared = false;

        private PatternMatcher _defaultMatcher;

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

        private int _state = NONE;

        private final URL xmlURL;

        public Parser(ModuleDescriptorParser parser, IvySettings ivySettings, boolean validate, URL xmlURL) {
            super(parser);
            _ivy = ivySettings;
            _validate = validate;
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
                if (!_artifactsDeclared) {
                    String[] confs = md.getConfigurationsNames();
                    for (int i = 0; i < confs.length; i++) {
                        md.addArtifact(confs[i], new MDArtifact(md, md.getModuleRevisionId()
                                .getName(), "jar", "jar"));
                    }
                }
                md.check();
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
                if (!_artifactsDeclared) {
                    String[] confs = md.getConfigurationsNames();
                    for (int i = 0; i < confs.length; i++) {
                        md.addArtifact(confs[i], new MDArtifact(md, md.getModuleRevisionId()
                                .getName(), "jar", "jar"));
                    }
                }
                md.check();
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
                if ("ivy-module".equals(qName)) {
                    String version = attributes.getValue("version");
                    int versionIndex = ALLOWED_VERSIONS.indexOf(version);
                    if (versionIndex == -1) {
                        addError("invalid version " + version);
                        throw new SAXException("invalid version " + version);
                    }
                    if (versionIndex >= ALLOWED_VERSIONS.indexOf("1.3")) {
                        Message.debug("post 1.3 ivy file: using " + PatternMatcher.EXACT
                                + " as default matcher");
                        _defaultMatcher = _ivy.getMatcher(PatternMatcher.EXACT);
                    } else {
                        Message.debug("pre 1.3 ivy file: using " + PatternMatcher.EXACT_OR_REGEXP
                                + " as default matcher");
                        _defaultMatcher = _ivy.getMatcher(PatternMatcher.EXACT_OR_REGEXP);
                    }
                } else if ("info".equals(qName)) {
                    _state = INFO;
                    String org = _ivy.substitute(attributes.getValue("organisation"));
                    String module = _ivy.substitute(attributes.getValue("module"));
                    String revision = _ivy.substitute(attributes.getValue("revision"));
                    String branch = _ivy.substitute(attributes.getValue("branch"));
                    md.setModuleRevisionId(ModuleRevisionId.newInstance(org, module, branch,
                        revision, ExtendableItemHelper.getExtraAttributes(attributes, new String[] {
                                "organisation", "module", "revision", "status", "publication",
                                "namespace", "default", "resolver"})));

                    String namespace = _ivy.substitute(attributes.getValue("namespace"));
                    if (namespace != null) {
                        Namespace ns = _ivy.getNamespace(namespace);
                        if (ns == null) {
                            Message.warn("namespace not found for " + md.getModuleRevisionId()
                                    + ": " + namespace);
                        } else {
                            md.setNamespace(ns);
                        }
                    }

                    String status = _ivy.substitute(attributes.getValue("status"));
                    md.setStatus(status == null ? _ivy.getStatusManager().getDefaultStatus()
                            : status);
                    md.setDefault(Boolean.valueOf(_ivy.substitute(attributes.getValue("default")))
                            .booleanValue());
                    String pubDate = _ivy.substitute(attributes.getValue("publication"));
                    if (pubDate != null && pubDate.length() > 0) {
                        try {
                            md.setPublicationDate(Ivy.DATE_FORMAT.parse(pubDate));
                        } catch (ParseException e) {
                            addError("invalid publication date format: " + pubDate);
                            md.setPublicationDate(getDefaultPubDate());
                        }
                    } else {
                        md.setPublicationDate(getDefaultPubDate());
                    }

                } else if ("license".equals(qName)) {
                    md.addLicense(new License(_ivy.substitute(attributes.getValue("name")), _ivy
                            .substitute(attributes.getValue("url"))));
                } else if ("description".equals(qName)) {
                    md.setHomePage(_ivy.substitute(attributes.getValue("homepage")));
                } else if ("configurations".equals(qName)) {
                    _state = CONF;
                    setDefaultConfMapping(_ivy
                            .substitute(attributes.getValue("defaultconfmapping")));
                    md
                            .setMappingOverride(Boolean.valueOf(
                                _ivy.substitute(attributes.getValue("confmappingoverride")))
                                    .booleanValue());
                } else if ("publications".equals(qName)) {
                    _state = PUB;
                    _artifactsDeclared = true;
                    checkConfigurations();
                } else if ("dependencies".equals(qName)) {
                    _state = DEPS;
                    String defaultConf = _ivy.substitute(attributes.getValue("defaultconf"));
                    if (defaultConf != null) {
                        setDefaultConf(defaultConf);
                    }
                    defaultConf = _ivy.substitute(attributes.getValue("defaultconfmapping"));
                    if (defaultConf != null) {
                        setDefaultConfMapping(defaultConf);
                    }
                    String confMappingOverride = _ivy.substitute(attributes
                            .getValue("confmappingoverride"));
                    if (confMappingOverride != null) {
                        md.setMappingOverride(Boolean.valueOf(confMappingOverride).booleanValue());
                    }
                    checkConfigurations();
                } else if ("conflicts".equals(qName)) {
                    _state = CONFLICT;
                    checkConfigurations();
                } else if ("artifact".equals(qName)) {
                    if (_state == PUB) {
                        // this is a published artifact
                        String artName = _ivy.substitute(attributes.getValue("name"));
                        artName = artName == null ? md.getModuleRevisionId().getName() : artName;
                        String type = _ivy.substitute(attributes.getValue("type"));
                        type = type == null ? "jar" : type;
                        String ext = _ivy.substitute(attributes.getValue("ext"));
                        ext = ext != null ? ext : type;
                        String url = _ivy.substitute(attributes.getValue("url"));
                        _artifact = new MDArtifact(md, artName, type, ext, url == null ? null
                                : new URL(url), ExtendableItemHelper.getExtraAttributes(attributes,
                            new String[] {"ext", "type", "name", "conf"}));
                        String confs = _ivy.substitute(attributes.getValue("conf"));
                        // only add confs if they are specified. if they aren't, endElement will
                        // handle this
                        // only if there are no conf defined in sub elements
                        if (confs != null && confs.length() > 0) {
                            String[] conf;
                            if ("*".equals(confs)) {
                                conf = md.getConfigurationsNames();
                            } else {
                                conf = confs.split(",");
                            }
                            for (int i = 0; i < conf.length; i++) {
                                _artifact.addConfiguration(conf[i].trim());
                                md.addArtifact(conf[i].trim(), _artifact);
                            }
                        }
                    } else if (_state == DEP) {
                        // this is an artifact asked for a particular dependency
                        addDependencyArtifacts(qName, attributes);
                    } else if (_validate) {
                        addError("artifact tag found in invalid tag: " + _state);
                    }
                } else if ("include".equals(qName) && _state == DEP) {
                    addIncludeRule(qName, attributes);
                } else if ("exclude".equals(qName) && _state == DEP) {
                    addExcludeRule(qName, attributes);
                } else if ("exclude".equals(qName) && _state == DEPS) {
                    _state = EXCLUDE;
                    parseRule(qName, attributes);
                    md.addExcludeRule((ExcludeRule) _confAware);
               } else if ("dependency".equals(qName)) {
                    _state = DEP;
                    String org = _ivy.substitute(attributes.getValue("org"));
                    if (org == null) {
                        org = md.getModuleRevisionId().getOrganisation();
                    }
                    boolean force = Boolean.valueOf(_ivy.substitute(attributes.getValue("force")))
                            .booleanValue();
                    boolean changing = Boolean.valueOf(
                        _ivy.substitute(attributes.getValue("changing"))).booleanValue();

                    String transitiveValue = _ivy.substitute(attributes.getValue("transitive"));
                    boolean transitive = (transitiveValue == null) ? true : Boolean.valueOf(
                        attributes.getValue("transitive")).booleanValue();

                    String name = _ivy.substitute(attributes.getValue("name"));
                    String branch = _ivy.substitute(attributes.getValue("branch"));
                    String rev = _ivy.substitute(attributes.getValue("rev"));
                    _dd = new DefaultDependencyDescriptor(md, ModuleRevisionId.newInstance(org,
                        name, branch, rev, ExtendableItemHelper.getExtraAttributes(attributes,
                            DEPENDENCY_REGULAR_ATTRIBUTES)), force, changing, transitive);
                    md.addDependency(_dd);
                    String confs = _ivy.substitute(attributes.getValue("conf"));
                    if (confs != null && confs.length() > 0) {
                        parseDepsConfs(confs, _dd);
                    }
                } else if ("conf".equals(qName)) {
                    String conf = _ivy.substitute(attributes.getValue("name"));
                    switch (_state) {
                        case CONF:
                            String visibility = _ivy.substitute(attributes.getValue("visibility"));
                            String ext = _ivy.substitute(attributes.getValue("extends"));
                            String transitiveValue = attributes.getValue("transitive");
                            boolean transitive = (transitiveValue == null) ? true : Boolean
                                    .valueOf(attributes.getValue("transitive")).booleanValue();
                            Configuration configuration = new Configuration(conf,
                                    Configuration.Visibility
                                            .getVisibility(visibility == null ? "public"
                                                    : visibility), _ivy.substitute(attributes
                                            .getValue("description")), ext == null ? null : ext
                                            .split(","), transitive);
                            ExtendableItemHelper.fillExtraAttributes(configuration, attributes,
                                new String[] {"name", "visibility", "extends", "transitive",
                                        "description"});
                            md.addConfiguration(configuration);
                            break;
                        case PUB:
                            if ("*".equals(conf)) {
                                String[] confs = md.getConfigurationsNames();
                                for (int i = 0; i < confs.length; i++) {
                                    _artifact.addConfiguration(confs[i]);
                                    md.addArtifact(confs[i], _artifact);
                                }
                            } else {
                                _artifact.addConfiguration(conf);
                                md.addArtifact(conf, _artifact);
                            }
                            break;
                        case DEP:
                            _conf = conf;
                            String mappeds = _ivy.substitute(attributes.getValue("mapped"));
                            if (mappeds != null) {
                                String[] mapped = mappeds.split(",");
                                for (int i = 0; i < mapped.length; i++) {
                                    _dd.addDependencyConfiguration(_conf, mapped[i].trim());
                                }
                            }
                            break;
                        case DEP_ARTIFACT:
                        case ARTIFACT_INCLUDE:
                        case ARTIFACT_EXCLUDE:
                            addConfiguration(conf);
                            break;
                        default:
                            if (_validate) {
                                addError("conf tag found in invalid tag: " + _state);
                            }
                            break;
                    }
                } else if ("mapped".equals(qName)) {
                    _dd.addDependencyConfiguration(_conf, _ivy.substitute(attributes
                            .getValue("name")));
                } else if ("manager".equals(qName) && _state == CONFLICT) {
                    String org = _ivy.substitute(attributes.getValue("org"));
                    org = org == null ? PatternMatcher.ANY_EXPRESSION : org;
                    String mod = _ivy.substitute(attributes.getValue("module"));
                    mod = mod == null ? PatternMatcher.ANY_EXPRESSION : mod;
                    ConflictManager cm;
                    String name = _ivy.substitute(attributes.getValue("name"));
                    String rev = _ivy.substitute(attributes.getValue("rev"));
                    if (rev != null) {
                        String[] revs = rev.split(",");
                        for (int i = 0; i < revs.length; i++) {
                            revs[i] = revs[i].trim();
                        }
                        cm = new FixedConflictManager(revs);
                    } else if (name != null) {
                        cm = _ivy.getConflictManager(name);
                        if (cm == null) {
                            addError("unknown conflict manager: " + name);
                            return;
                        }
                    } else {
                        addError("bad conflict manager: no name nor rev");
                        return;
                    }
                    String matcherName = _ivy.substitute(attributes.getValue("matcher"));
                    PatternMatcher matcher = matcherName == null ? _defaultMatcher : _ivy
                            .getMatcher(matcherName);
                    if (matcher == null) {
                        addError("unknown matcher: " + matcherName);
                        return;
                    }
                    md.addConflictManager(new ModuleId(org, mod), matcher, cm);
                 } else if ("include".equals(qName) && _state == CONF) {
                    URL url = _ivy.getRelativeUrlResolver().getURL(xmlURL,
                            _ivy.substitute(attributes.getValue("file")),
                            _ivy.substitute(attributes.getValue("url")));
                    
                    if (url == null) {
                        throw new SAXException("include tag must have a file or an url attribute");
                    }
                    
                    // create a new temporary parser to read the configurations from
                    // the specified file.
                    Parser parser = new Parser(getModuleDescriptorParser(), _ivy, false, url);
                    parser.md = new DefaultModuleDescriptor(getModuleDescriptorParser(),
                            new URLResource(url));
                    XMLHelper.parse(url , null, parser);

                    // add the configurations from this temporary parser to this module descriptor
                    Configuration[] configs = parser.getModuleDescriptor().getConfigurations();
                    for (int i = 0; i < configs.length; i++) {
                        md.addConfiguration(configs[i]);
                    }
                    if (parser.getDefaultConfMapping() != null) {
                        Message.debug("setting default conf from imported configurations file: "
                                + parser.getDefaultConfMapping());
                        setDefaultConfMapping(parser.getDefaultConfMapping());
                    }
                    if (parser.md.isMappingOverride()) {
                        Message.debug("enabling mapping-override from imported configurations" 
                                + " file");
                        md.setMappingOverride(true);
                    }
                } else if (_validate && _state != INFO) {
                    addError("unknwon tag " + qName);
                }
            } catch (Exception ex) {
                if (ex instanceof SAXException) {
                    throw (SAXException) ex;
                }
                throw new SAXException("problem occured while parsing ivy file. message: "
                        + ex.getMessage(), ex);
            }
        }

        private void addDependencyArtifacts(String tag, Attributes attributes)
                throws MalformedURLException {
            _state = DEP_ARTIFACT;
            parseRule(tag, attributes);
        }

        private void addIncludeRule(String tag, Attributes attributes) throws MalformedURLException {
            _state = ARTIFACT_INCLUDE;
            parseRule(tag, attributes);
        }

        private void addExcludeRule(String tag, Attributes attributes) throws MalformedURLException {
            _state = ARTIFACT_EXCLUDE;
            parseRule(tag, attributes);
        }

        private void parseRule(String tag, Attributes attributes) throws MalformedURLException {
            String name = _ivy.substitute(attributes.getValue("name"));
            if (name == null) {
                name = _ivy.substitute(attributes.getValue("artifact"));
                if (name == null) {
                    name = "artifact".equals(tag) ? _dd.getDependencyId().getName()
                            : PatternMatcher.ANY_EXPRESSION;
                }
            }
            String type = _ivy.substitute(attributes.getValue("type"));
            if (type == null) {
                type = "artifact".equals(tag) ? "jar" : PatternMatcher.ANY_EXPRESSION;
            }
            String ext = _ivy.substitute(attributes.getValue("ext"));
            ext = ext != null ? ext : type;
            if (_state == DEP_ARTIFACT) {
                String url = _ivy.substitute(attributes.getValue("url"));
                Map extraAtt = ExtendableItemHelper.getExtraAttributes(attributes, new String[] {
                        "name", "type", "ext", "url", "conf"});
                _confAware = new DefaultDependencyArtifactDescriptor(name, type, ext,
                        url == null ? null : new URL(url), extraAtt);
            } else if (_state == ARTIFACT_INCLUDE) {
                PatternMatcher matcher = getPatternMatcher(attributes.getValue("matcher"));
                String org = _ivy.substitute(attributes.getValue("org"));
                org = org == null ? PatternMatcher.ANY_EXPRESSION : org;
                String module = _ivy.substitute(attributes.getValue("module"));
                module = module == null ? PatternMatcher.ANY_EXPRESSION : module;
                ArtifactId aid = new ArtifactId(new ModuleId(org, module), name, type, ext);
                Map extraAtt = ExtendableItemHelper.getExtraAttributes(attributes, new String[] {
                        "org", "module", "name", "type", "ext", "matcher", "conf"});
                _confAware = new DefaultIncludeRule(aid, matcher, extraAtt);
            } else { // _state == ARTIFACT_EXCLUDE || EXCLUDE
                PatternMatcher matcher = getPatternMatcher(attributes.getValue("matcher"));
                String org = _ivy.substitute(attributes.getValue("org"));
                org = org == null ? PatternMatcher.ANY_EXPRESSION : org;
                String module = _ivy.substitute(attributes.getValue("module"));
                module = module == null ? PatternMatcher.ANY_EXPRESSION : module;
                ArtifactId aid = new ArtifactId(new ModuleId(org, module), name, type, ext);
                Map extraAtt = ExtendableItemHelper.getExtraAttributes(attributes, new String[] {
                        "org", "module", "name", "type", "ext", "matcher", "conf"});
                _confAware = new DefaultExcludeRule(aid, matcher, extraAtt);
            }
            String confs = _ivy.substitute(attributes.getValue("conf"));
            // only add confs if they are specified. if they aren't, endElement will handle this
            // only if there are no conf defined in sub elements
            if (confs != null && confs.length() > 0) {
                String[] conf;
                if ("*".equals(confs)) {
                    conf = md.getConfigurationsNames();
                } else {
                    conf = confs.split(",");
                }
                for (int i = 0; i < conf.length; i++) {
                    addConfiguration(conf[i].trim());
                }
            }
        }

        private void addConfiguration(String c) {
            _confAware.addConfiguration(c);
            if (_state == EXCLUDE) {
                // we are adding a configuration to a module wide exclude rule
                // we have nothing special to do here, the rule has already been added to the module
                // descriptor
            } else {
                // we are currently adding a configuration to either an include, exclude or artifact
                // element
                // of a dependency. This means that we have to add this element to the corresponding
                // conf
                // of the current dependency descriptor
                if (_confAware instanceof DependencyArtifactDescriptor) {
                    _dd.addDependencyArtifact(c, (DependencyArtifactDescriptor) _confAware);
                } else if (_confAware instanceof IncludeRule) {
                    _dd.addIncludeRule(c, (IncludeRule) _confAware);
                } else if (_confAware instanceof ExcludeRule) {
                    _dd.addExcludeRule(c, (ExcludeRule) _confAware);
                }
            }
        }

        private PatternMatcher getPatternMatcher(String m) {
            String matcherName = _ivy.substitute(m);
            PatternMatcher matcher = matcherName == null ? _defaultMatcher : _ivy
                    .getMatcher(matcherName);
            if (matcher == null) {
                throw new IllegalArgumentException("unknown matcher " + matcherName);
            }
            return matcher;
        }

        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (_state == PUB && "artifact".equals(qName)
                    && _artifact.getConfigurations().length == 0) {
                String[] confs = md.getConfigurationsNames();
                for (int i = 0; i < confs.length; i++) {
                    _artifact.addConfiguration(confs[i]);
                    md.addArtifact(confs[i], _artifact);
                }
            } else if ("configurations".equals(qName)) {
                checkConfigurations();
            } else if ((_state == DEP_ARTIFACT && "artifact".equals(qName))
                    || (_state == ARTIFACT_INCLUDE && "include".equals(qName))
                    || (_state == ARTIFACT_EXCLUDE && "exclude".equals(qName))) {
                _state = DEP;
                if (_confAware.getConfigurations().length == 0) {
                    String[] confs = md.getConfigurationsNames();
                    for (int i = 0; i < confs.length; i++) {
                        addConfiguration(confs[i]);
                    }
                }
                _confAware = null;
            } else if (_state == EXCLUDE) {
                if (_confAware.getConfigurations().length == 0) {
                    String[] confs = md.getConfigurationsNames();
                    for (int i = 0; i < confs.length; i++) {
                        addConfiguration(confs[i]);
                    }
                }
                _confAware = null;
                _state = DEPS;
            } else if ("dependency".equals(qName)) {
                if (_dd.getModuleConfigurations().length == 0) {
                    parseDepsConfs(getDefaultConf(), _dd);
                }
                _state = DEPS;
            } else if ("dependencies".equals(qName)) {
                _state = NONE;
            }
        }

        private void checkConfigurations() {
            if (md.getConfigurations().length == 0) {
                md.addConfiguration(new Configuration("default"));
            }
        }

        private void replaceConfigurationWildcards() {
            Configuration[] configs = md.getConfigurations();
            for (int i = 0; i < configs.length; i++) {
                configs[i].replaceWildcards(md);
            }
        }

    }

    public String toString() {
        return "ivy parser";
    }
}
