/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.xml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import fr.jayasoft.ivy.ArtifactId;
import fr.jayasoft.ivy.Configuration;
import fr.jayasoft.ivy.ConflictManager;
import fr.jayasoft.ivy.DefaultDependencyArtifactDescriptor;
import fr.jayasoft.ivy.DefaultDependencyDescriptor;
import fr.jayasoft.ivy.DefaultModuleDescriptor;
import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.License;
import fr.jayasoft.ivy.MDArtifact;
import fr.jayasoft.ivy.ModuleDescriptor;
import fr.jayasoft.ivy.ModuleId;
import fr.jayasoft.ivy.ModuleRevisionId;
import fr.jayasoft.ivy.conflict.FixedConflictManager;
import fr.jayasoft.ivy.extendable.ExtendableItemHelper;
import fr.jayasoft.ivy.matcher.PatternMatcher;
import fr.jayasoft.ivy.namespace.Namespace;
import fr.jayasoft.ivy.parser.AbstractModuleDescriptorParser;
import fr.jayasoft.ivy.parser.ModuleDescriptorParser;
import fr.jayasoft.ivy.repository.Resource;
import fr.jayasoft.ivy.repository.url.URLResource;
import fr.jayasoft.ivy.util.Message;
import fr.jayasoft.ivy.util.XMLHelper;

/**
 * Parses an xml ivy file and output a ModuleDescriptor.
 * For dependency and performance reasons, it uses
 * only the SAX API, which makes the parsing code harder
 * to understand.
 * 
 * @author x.hanin
 *
 */
public class XmlModuleDescriptorParser extends AbstractModuleDescriptorParser {
    private static XmlModuleDescriptorParser INSTANCE = new XmlModuleDescriptorParser();
    
    public static XmlModuleDescriptorParser getInstance() {
        return INSTANCE;
    }
    
    private XmlModuleDescriptorParser() {
        
    }
    
    /**
     * 
     * @param ivy
     * @param xmlURL the url pointing to the file to parse
     * @param res the real resource to parse, used for log only
     * @param validate
     * @return
     * @throws ParseException
     * @throws IOException
     */
    public ModuleDescriptor parseDescriptor(Ivy ivy, URL xmlURL, Resource res, boolean validate) throws ParseException, IOException {
        Parser parser = new Parser(this, ivy, validate);
        parser.parse(xmlURL, res, validate);
        return parser.getModuleDescriptor();
    }

    public boolean accept(Resource res) {
        return true; // this the default parser, it thus accepts all resources
    }

    public void toIvyFile(InputStream is, Resource res, File destFile, ModuleDescriptor md) throws IOException, ParseException {
        try {
            Namespace ns = null;
            if (md instanceof DefaultModuleDescriptor) {
                DefaultModuleDescriptor dmd = (DefaultModuleDescriptor)md;
                ns = dmd.getNamespace();
            }
            XmlModuleDescriptorUpdater.update(
                    null,
                    is, 
                    destFile, 
                    Collections.EMPTY_MAP, 
                    md.getStatus(), 
                    md.getResolvedModuleRevisionId().getRevision(), 
                    md.getResolvedPublicationDate(),
                    ns,
                    false);
        } catch (SAXException e) {
            ParseException ex = new ParseException("exception occured while parsing "+res, 0);
            ex.initCause(e);
            throw ex;
        } finally {
        	if (is != null) {
        		is.close();
        	}
        }
    }

    private static class Parser extends AbstractParser {

    private static final List ALLOWED_VERSIONS = Arrays.asList(new String[] {"1.0", "1.1", "1.2", "1.3", "1.4"});
    
    private DefaultDependencyDescriptor _dd;
    private DefaultDependencyArtifactDescriptor _dad;
    private MDArtifact _artifact;
    private String _conf;
    private boolean _validate = true;
    private Ivy _ivy;
    private boolean _artifactsDeclared = false;
    private PatternMatcher _defaultMatcher; 

    private static final int NONE = 0;
    private static final int INFO = 1;
    private static final int CONF = 2;
    private static final int PUB = 3;
    private static final int DEP = 4;
    private static final int ARTIFACT_INCLUDE = 5;
    private static final int ARTIFACT_EXCLUDE = 6;
    private static final int CONFLICT = 7;
    private int _state = NONE;

    public Parser(ModuleDescriptorParser parser, Ivy ivy, boolean validate) {
    	super(parser);
        _ivy = ivy;
        _validate = validate;
    }

    private void parse(URL xmlURL, Resource res, boolean validate) throws ParseException, IOException {
        try {
            setResource(res);
            URL schemaURL = validate?getClass().getResource("ivy.xsd"):null;
            XMLHelper.parse(xmlURL, schemaURL, this);
            checkConfigurations();
            replaceConfigurationWildcards();
            if (!_artifactsDeclared) {
                String[] confs = _md.getConfigurationsNames();
                for (int i = 0; i < confs.length; i++) {
                    _md.addArtifact(confs[i], new MDArtifact(_md, _md.getModuleRevisionId().getName(), "jar", "jar"));
                }
            }
            _md.check();
        } catch (ParserConfigurationException ex) {
            IllegalStateException ise = new IllegalStateException(ex.getMessage()+" in "+xmlURL);
            ise.initCause(ex);
            throw ise;
        } catch (Exception ex) {
            checkErrors();
            ParseException pe = new ParseException(ex.getMessage()+" in "+xmlURL, 0);
            pe.initCause(ex);
            throw pe;
        }
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        try {
            if ("ivy-module".equals(qName)) {
                String version = attributes.getValue("version");
                int versionIndex = ALLOWED_VERSIONS.indexOf(version);
                if (versionIndex == -1) {
                    addError("invalid version "+version);
                    throw new SAXException("invalid version "+version);
                }
                if (versionIndex >= ALLOWED_VERSIONS.indexOf("1.3")) {
                    Message.debug("post 1.3 ivy file: using "+PatternMatcher.EXACT+" as default matcher");
                    _defaultMatcher = _ivy.getMatcher(PatternMatcher.EXACT);
                } else {
                    Message.debug("pre 1.3 ivy file: using "+PatternMatcher.EXACT_OR_REGEXP+" as default matcher");
                    _defaultMatcher = _ivy.getMatcher(PatternMatcher.EXACT_OR_REGEXP);
                }
            } else if ("info".equals(qName)) {
                _state = INFO;
                String org = _ivy.substitute(attributes.getValue("organisation"));
                String module = _ivy.substitute(attributes.getValue("module"));
                String revision = _ivy.substitute(attributes.getValue("revision"));
                String branch = _ivy.substitute(attributes.getValue("branch"));
                _md.setModuleRevisionId(ModuleRevisionId.newInstance(org, module, branch, revision, ExtendableItemHelper.getExtraAttributes(attributes, new String[] {"organisation", "module", "revision", "status", "publication", "namespace", "default", "resolver"})));

                String namespace = _ivy.substitute(attributes.getValue("namespace"));
                if (namespace != null) {
                    Namespace ns = _ivy.getNamespace(namespace);
                    if (ns == null) {
                        Message.warn("namespace not found for "+_md.getModuleRevisionId()+": "+namespace);
                    } else {
                        _md.setNamespace(ns);
                    }
                }
                
                String status = _ivy.substitute(attributes.getValue("status"));
                _md.setStatus(status == null ? _ivy.getStatusManager().getDefaultStatus() : status);
                _md.setDefault(Boolean.valueOf(_ivy.substitute(attributes.getValue("default"))).booleanValue());
                String pubDate = _ivy.substitute(attributes.getValue("publication"));
                if (pubDate != null && pubDate.length() > 0) {
                    try {
                        _md.setPublicationDate(Ivy.DATE_FORMAT.parse(pubDate));
                    } catch (ParseException e) {
                        addError("invalid publication date format: "+pubDate);
                        _md.setPublicationDate(getDefaultPubDate());
                    }
                } else {
                    _md.setPublicationDate(getDefaultPubDate());                    
                }
                
            } else if ("license".equals(qName)) {
                _md.addLicense(new License(_ivy.substitute(attributes.getValue("name")), _ivy.substitute(attributes.getValue("url"))));
            } else if ("description".equals(qName)) {
                _md.setHomePage(_ivy.substitute(attributes.getValue("homepage")));
            } else if ("configurations".equals(qName)) {
                _state = CONF;
                setDefaultConfMapping(_ivy.substitute(attributes.getValue("defaultconfmapping")));
                _md.setMappingOverride(Boolean.valueOf(_ivy.substitute(attributes.getValue("confmappingoverride"))).booleanValue());
            } else if ("publications".equals(qName)) {
                _state = PUB;
                _artifactsDeclared = true;
                checkConfigurations();
            } else if ("dependencies".equals(qName)) {
                _state = DEP;
                String defaultConf = _ivy.substitute(attributes.getValue("defaultconf"));
                if (defaultConf != null) {
                    setDefaultConf(defaultConf);
                }
                defaultConf = _ivy.substitute(attributes.getValue("defaultconfmapping"));
                if (defaultConf != null) {
                    setDefaultConfMapping(defaultConf);
                }
                String confMappingOverride = _ivy.substitute(attributes.getValue("confmappingoverride"));
                if (confMappingOverride != null) {
                   _md.setMappingOverride(Boolean.valueOf(confMappingOverride).booleanValue());
                }
                checkConfigurations();
            } else if ("conflicts".equals(qName)) {
                _state = CONFLICT;
                checkConfigurations();
            } else if ("artifact".equals(qName)) {
                if (_state == PUB) {
                    // this is a published artifact
                    String artName = _ivy.substitute(attributes.getValue("name"));
                    artName = artName == null ? _md.getModuleRevisionId().getName() : artName;
                    String type = _ivy.substitute(attributes.getValue("type"));
                    type = type == null ? "jar" : type;
                    String ext = _ivy.substitute(attributes.getValue("ext"));
                    ext = ext != null ? ext : type;
                    String url = _ivy.substitute(attributes.getValue("url"));
                    _artifact = new MDArtifact(_md, artName, type, ext, url  == null ? null : new URL(url), ExtendableItemHelper.getExtraAttributes(attributes, new String[] {"ext", "type", "name", "conf"}));
                    String confs = _ivy.substitute(attributes.getValue("conf"));
                    // only add confs if they are specified. if they aren't, endElement will handle this
                    // only if there are no conf defined in sub elements
                    if (confs != null && confs.length() > 0) {
                        String[] conf;
                        if ("*".equals(confs)) {
                            conf = _md.getConfigurationsNames();
                        } else {
                            conf = confs.split(",");
                        }
                        for (int i = 0; i < conf.length; i++) {
                            _artifact.addConfiguration(conf[i].trim());
                            _md.addArtifact(conf[i].trim(), _artifact);
                        }
                    }
                } else if (_state == DEP) {
                    // this is an artifact asked for a particular dependency
                    addDependencyArtifacts(qName, attributes);
                } else if (_validate) {
                    addError("artifact tag found in invalid tag: "+_state);
                }
            } else if ("include".equals(qName) && _state == DEP) {
                addDependencyArtifactsIncludes(qName, attributes);
            } else if ("exclude".equals(qName)) {
                addDependencyArtifactsExcludes(qName, attributes);
            } else if ("dependency".equals(qName)) {
                String org = _ivy.substitute(attributes.getValue("org"));
                if (org == null) { 
                    org = _md.getModuleRevisionId().getOrganisation();
                }
                boolean force = Boolean.valueOf(_ivy.substitute(attributes.getValue("force"))).booleanValue();
                boolean changing = Boolean.valueOf(_ivy.substitute(attributes.getValue("changing"))).booleanValue();

                String transitiveValue = _ivy.substitute(attributes.getValue("transitive"));
                boolean transitive = (transitiveValue == null) ? true : Boolean.valueOf(attributes.getValue("transitive")).booleanValue();
                
                String name = _ivy.substitute(attributes.getValue("name"));
                String branch = _ivy.substitute(attributes.getValue("branch"));
                String rev = _ivy.substitute(attributes.getValue("rev"));
                _dd = new DefaultDependencyDescriptor(_md, ModuleRevisionId.newInstance(org, name, branch, rev, ExtendableItemHelper.getExtraAttributes(attributes, new String[] {"org", "name", "rev", "force", "transitive", "changing", "conf"})), force, changing, transitive);
                _md.addDependency(_dd);
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
                    boolean transitive = (transitiveValue == null) ? true : Boolean.valueOf(attributes.getValue("transitive")).booleanValue();
                    Configuration configuration = new Configuration(
                            conf, 
                            Configuration.Visibility.getVisibility(visibility == null ? "public":visibility),
                            _ivy.substitute(attributes.getValue("description")),
                            ext==null?null:ext.split(","),
                            transitive);
                    ExtendableItemHelper.fillExtraAttributes(configuration, attributes, new String[] {"name", "visibility", "extends", "transitive", "description"});
                	_md.addConfiguration(configuration);
                	break;
            	case PUB:
            	    if ("*".equals(conf)) {
            	        String[] confs = _md.getConfigurationsNames();
            	        for (int i = 0; i < confs.length; i++) {
        	                _artifact.addConfiguration(confs[i]);
        	                _md.addArtifact(confs[i], _artifact);
                        }
            	    } else {
    	                _artifact.addConfiguration(conf);
    	                _md.addArtifact(conf, _artifact);
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
                case ARTIFACT_INCLUDE:
                case ARTIFACT_EXCLUDE:
                    _dad.addConfiguration(conf);
                    break;
                default:
                    if (_validate) {
                        addError("conf tag found in invalid tag: "+_state);
                    }
                	break;
                }
            } else if ("mapped".equals(qName)) {
                _dd.addDependencyConfiguration(_conf, _ivy.substitute(attributes.getValue("name")));
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
                        addError("unknown conflict manager: "+name);
                        return;
                    }
                } else {
                    addError("bad conflict manager: no name nor rev");
                    return;
                }
                String matcherName = _ivy.substitute(attributes.getValue("matcher"));
                PatternMatcher matcher = matcherName == null ? _defaultMatcher : _ivy.getMatcher(matcherName);
                if (matcher == null) {
                    addError("unknown matcher: "+matcherName);
                    return;
                }
                _md.addConflictManager(new ModuleId(org, mod), matcher, cm);
            } else if ("include".equals(qName) && _state == CONF) {
                URL url;
                String fileName = _ivy.substitute(attributes.getValue("file"));
                if (fileName == null) {
                    String urlStr = _ivy.substitute(attributes.getValue("url"));
                    url = new URL(urlStr);
                } else {
                    url = new File(fileName).toURL();
                }                
                
                // create a new temporary parser to read the configurations from
                // the specified file.
                Parser parser = new Parser(getModuleDescriptorParser(), _ivy, false);
                parser._md = new DefaultModuleDescriptor(getModuleDescriptorParser(), new URLResource(url));
                XMLHelper.parse(url, null, parser);
                
                // add the configurations from this temporary parser to this module descriptor
                Configuration[] configs = parser.getModuleDescriptor().getConfigurations();
                for (int i = 0; i < configs.length; i++) {
                    _md.addConfiguration(configs[i]);
                }
                if (parser.getDefaultConfMapping() != null) {
                    Message.debug("setting default conf from imported configurations file: "+parser.getDefaultConfMapping());
                    setDefaultConfMapping(parser.getDefaultConfMapping());
                }
                if (parser._md.isMappingOverride()) {
                    Message.debug("enabling mapping-override from imported configurations file");
                    _md.setMappingOverride(true);
                }
            } else if (_validate && _state != INFO) {
                addError("unknwon tag "+qName);
            }
        } catch (Exception ex) {
            if (ex instanceof SAXException) {
                throw (SAXException)ex;
            }
            throw new SAXException("problem occured while parsing ivy file. message: "+ex.getMessage(), ex);
        }
    }

    private void addDependencyArtifacts(String tag, Attributes attributes) throws MalformedURLException {
        _state = ARTIFACT_INCLUDE;
        addDependencyArtifact(tag, attributes, true);
    }

    private void addDependencyArtifactsIncludes(String tag, Attributes attributes) throws MalformedURLException {
        _state = ARTIFACT_INCLUDE;
        addDependencyArtifact(tag, attributes, true);
    }

    private void addDependencyArtifactsExcludes(String tag, Attributes attributes) throws MalformedURLException {
        _state = ARTIFACT_EXCLUDE;
        addDependencyArtifact(tag, attributes, false);
    }   
    
    private void addDependencyArtifact(String tag, Attributes attributes, boolean includes) throws MalformedURLException {
        String name = _ivy.substitute(attributes.getValue("name"));
        if (name == null) {
        	name = "artifact".equals(tag)?_dd.getDependencyId().getName() : PatternMatcher.ANY_EXPRESSION;
        }
        String type = _ivy.substitute(attributes.getValue("type"));
        if (type == null) {
        	type = "artifact".equals(tag)?"jar" : PatternMatcher.ANY_EXPRESSION;
        }
        String ext = _ivy.substitute(attributes.getValue("ext"));
        ext = ext != null?ext:type;
        String matcherName = _ivy.substitute(attributes.getValue("matcher"));
        PatternMatcher matcher = matcherName == null ? _defaultMatcher : _ivy.getMatcher(matcherName);
        if (matcher == null) {
            addError("unknown matcher "+matcherName);
            return;
        }
        if (includes) {
            String url = _ivy.substitute(attributes.getValue("url"));
            _dad = new DefaultDependencyArtifactDescriptor(_dd, name, type, ext, url==null?null:new URL(url), includes, matcher);
        } else {
            String org = _ivy.substitute(attributes.getValue("org"));
            org = org == null ? PatternMatcher.ANY_EXPRESSION : org;
            String module = _ivy.substitute(attributes.getValue("module"));
            module = module == null ? PatternMatcher.ANY_EXPRESSION : module;
            ArtifactId aid = new ArtifactId(new ModuleId(org, module), name, type, ext);
            _dad = new DefaultDependencyArtifactDescriptor(_dd, aid, includes, matcher);
        }
        String confs = _ivy.substitute(attributes.getValue("conf"));
        // only add confs if they are specified. if they aren't, endElement will handle this
        // only if there are no conf defined in sub elements
        if (confs != null && confs.length() > 0) {
            String[] conf;
            if ("*".equals(confs)) {
                conf = _md.getConfigurationsNames();
            } else {
                conf = confs.split(",");
            }
            for (int i = 0; i < conf.length; i++) {
                _dad.addConfiguration(conf[i].trim());
            }
        }
    }   
    
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (_state == PUB && "artifact".equals(qName) && _artifact.getConfigurations().length == 0) {
            String[] confs = _md.getConfigurationsNames();
            for (int i = 0; i < confs.length; i++) {
                _artifact.addConfiguration(confs[i]);
                _md.addArtifact(confs[i], _artifact);
            }
        } else if ("configurations".equals(qName)) {
            checkConfigurations();
        } else if ((_state == ARTIFACT_INCLUDE && ("artifact".equals(qName) || "include".equals(qName)))
                || (_state == ARTIFACT_EXCLUDE && "exclude".equals(qName))){
            _state = DEP;
            if (_dad.getConfigurations().length == 0) {
                String[] confs = _md.getConfigurationsNames();
                for (int i = 0; i < confs.length; i++) {
                    _dad.addConfiguration(confs[i]);
                }                
            }
        } else if ("dependency".equals(qName) && _dd.getModuleConfigurations().length == 0) {
            parseDepsConfs(getDefaultConf(), _dd);
        }
    }

    private void checkConfigurations() {
        if (_md.getConfigurations().length == 0) {
            _md.addConfiguration(new Configuration("default"));
        }
    }

    private void replaceConfigurationWildcards() {
        Configuration[] configs = _md.getConfigurations();
        for (int i = 0; i < configs.length; i++) {
            configs[i].replaceWildcards(_md);
        }
    }
    

    }

    public String toString() {
        return "ivy parser";
    }

    public static void main(String[] args) throws Exception {
        System.out.println(getInstance().parseDescriptor(new Ivy(), new File("test/xml/module1/module1.ivy.xml").toURL(), true));
    }
}
