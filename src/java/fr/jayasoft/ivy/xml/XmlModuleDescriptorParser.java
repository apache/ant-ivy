/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.xml;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

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
import fr.jayasoft.ivy.Status;
import fr.jayasoft.ivy.conflict.FixedConflictManager;
import fr.jayasoft.ivy.repository.Resource;
import fr.jayasoft.ivy.repository.url.URLResource;
import fr.jayasoft.ivy.util.Message;
import fr.jayasoft.ivy.util.XMLHelper;

/**
 * Parses an xml ivy file and output a ModuleDescriptor.
 * For dependency and performance reasons, it does uses
 * only the SAX API, which makes the parsing code harder
 * to understand.
 * 
 * @author x.hanin
 *
 */
public class XmlModuleDescriptorParser extends DefaultHandler {
    private static final Collection ALLOWED_VERSIONS = Arrays.asList(new String[] {"1.0", "1.1", "1.2"});
    
    private DefaultModuleDescriptor _md;
    private DefaultDependencyDescriptor _dd;
    private DefaultDependencyArtifactDescriptor _dad;
    private MDArtifact _artifact;
    private List _errors = new ArrayList();
    private String _conf;
    private boolean _validate = true;
    private Ivy _ivy;
    private boolean _artifactsDeclared = false;

    private static final int NONE = 0;
    private static final int INFO = 1;
    private static final int CONF = 2;
    private static final int PUB = 3;
    private static final int DEP = 4;
    private static final int ARTIFACT_INCLUDE = 5;
    private static final int ARTIFACT_EXCLUDE = 6;
    private static final int CONFLICT = 7;
    private int _state = NONE;
    private Resource _res;
    private String _defaultConf;

    public XmlModuleDescriptorParser(Ivy ivy, boolean validate) {
        _ivy = ivy;
        _validate = validate;
    }

    public static ModuleDescriptor parseDescriptor(Ivy ivy, URL xmlURL, boolean validate) throws ParseException, IOException {
        return parseDescriptor(ivy, xmlURL, new URLResource(xmlURL), validate);
    }

    /**
     * 
     * @param ivy
     * @param xmlURL the url pointing to the file to parse
     * @param realURL the real url of the file to parse, used for log only
     * @param validate
     * @return
     * @throws ParseException
     * @throws IOException
     */
    public static ModuleDescriptor parseDescriptor(Ivy ivy, URL xmlURL, Resource res, boolean validate) throws ParseException, IOException {
        XmlModuleDescriptorParser parser = new XmlModuleDescriptorParser(ivy, validate);
        parser.parse(xmlURL, res, validate);
        return parser.getModuleDescriptor();
    }

    private ModuleDescriptor getModuleDescriptor() throws ParseException {
        if (!_errors.isEmpty()) {
            throw new ParseException(_errors.toString(), 0);
        }
        return _md;
    }

    private void parse(URL xmlURL, Resource res, boolean validate) throws ParseException, IOException {
        try {
            _md = new DefaultModuleDescriptor();
            _res = res; // used for log and date only
            _md.setLastModified(getLastModified());
            URL schemaURL = validate?getClass().getResource("ivy.xsd"):null;
            XMLHelper.parse(xmlURL, schemaURL, this);
            checkConfigurations();
            if (!_artifactsDeclared) {
                String[] confs = _md.getConfigurationsNames();
                for (int i = 0; i < confs.length; i++) {
                    _md.addArtifact(confs[i], new MDArtifact(_md, _md.getModuleRevisionId().getName(), "jar", "jar"));
                }
            }
        } catch (SAXException ex) {
            ParseException pe = new ParseException(ex.getMessage()+" in "+xmlURL, 0);
            pe.initCause(ex);
            throw pe;
        } catch (ParserConfigurationException ex) {
            IllegalStateException ise = new IllegalStateException(ex.getMessage()+" in "+xmlURL);
            ise.initCause(ex);
            throw ise;
        }
    }
    
    private Date getDefaultPubDate() {
        return new Date(_md.getLastModified());
    }

    private long getLastModified() {
        long last = _res.getLastModified();
        if (last > 0) {
            return  last;
        } else {
            Message.debug("impossible to get date for "+_res+": using 'now'");
            return System.currentTimeMillis();
        }
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        try {
            if ("ivy-module".equals(qName)) {
                String version = attributes.getValue("version");
                if (!ALLOWED_VERSIONS.contains(version)) {
                    addError("invalid version "+version);
                    throw new SAXException("invalid version "+version);
                }
            } else if ("info".equals(qName)) {
                _state = INFO;
                String org = _ivy.substitute(attributes.getValue("organisation"));
                String module = _ivy.substitute(attributes.getValue("module"));
                String revision = _ivy.substitute(attributes.getValue("revision"));
                _md.setModuleRevisionId(ModuleRevisionId.newInstance(org, module, revision));
                String status = _ivy.substitute(attributes.getValue("status"));
                _md.setStatus(status == null ? Status.DEFAULT_STATUS : status);
                _md.setResolverName(_ivy.substitute(attributes.getValue("resolver")));
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
                _md.addLicense(new License(attributes.getValue("name"), attributes.getValue("url")));
            } else if ("description".equals(qName)) {
                _md.setHomePage(attributes.getValue("homepage"));
            } else if ("configurations".equals(qName)) {
                _state = CONF;
            } else if ("publications".equals(qName)) {
                _state = PUB;
                _artifactsDeclared = true;
                checkConfigurations();
            } else if ("dependencies".equals(qName)) {
                _state = DEP;
                _defaultConf = attributes.getValue("defaultconf");
                _defaultConf = _defaultConf == null ? "*->*" : _defaultConf;
                checkConfigurations();
            } else if ("conflicts".equals(qName)) {
                _state = CONFLICT;
                checkConfigurations();
            } else if ("artifact".equals(qName)) {
                if (_state == PUB) {
                    // this is a published artifact
                    String ext = attributes.getValue("ext");
                    ext = ext != null?ext:attributes.getValue("type");
                    _artifact = new MDArtifact(_md, attributes.getValue("name"), attributes.getValue("type"), ext);
                    String confs = attributes.getValue("conf");
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
                    addDependencyArtifactsIncludes(attributes);
                } else if (_validate) {
                    addError("artifact tag found in invalid tag: "+_state);
                }
            } else if ("include".equals(qName)) {
                addDependencyArtifactsIncludes(attributes);
            } else if ("exclude".equals(qName)) {
                addDependencyArtifactsExcludes(attributes);
            } else if ("dependency".equals(qName)) {
                String org = _ivy.substitute(attributes.getValue("org"));
                if (org == null) { 
                    org = _md.getModuleRevisionId().getOrganisation();
                }
                boolean force = Boolean.valueOf(attributes.getValue("force")).booleanValue();
                boolean changing = Boolean.valueOf(attributes.getValue("changing")).booleanValue();

                String transitiveValue = attributes.getValue("transitive");
                boolean transitive = (transitiveValue == null) ? true : Boolean.valueOf(attributes.getValue("transitive")).booleanValue();
                
                String name = _ivy.substitute(attributes.getValue("name"));
                String rev = _ivy.substitute(attributes.getValue("rev"));
                _dd = new DefaultDependencyDescriptor(_md, ModuleRevisionId.newInstance(org, name, rev), force, changing, transitive);
                _md.addDependency(_dd);
                String confs = attributes.getValue("conf");
                if (confs != null && confs.length() > 0) {
                    parseDepsConfs(confs);
                }
            } else if ("conf".equals(qName)) {
        	    String conf = attributes.getValue("name");
                switch (_state) {
            	case CONF:
                    String visibility = attributes.getValue("visibility");
                    String ext = attributes.getValue("extends");
                	_md.addConfiguration(new Configuration(
                            conf, 
                            Configuration.Visibility.getVisibility(visibility == null ? "public":visibility),
                            attributes.getValue("description"),
                            ext==null?null:ext.split(",")));
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
                    String mappeds = attributes.getValue("mapped");
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
                _dd.addDependencyConfiguration(_conf, attributes.getValue("name"));
            } else if ("manager".equals(qName) && _state == CONFLICT) {
                String org = _ivy.substitute(attributes.getValue("org"));
                org = org == null ? ".*" : org;
                String mod = _ivy.substitute(attributes.getValue("module"));
                mod = mod == null ? ".*" : mod;
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
                _md.addConflictManager(new ModuleId(org, mod), cm);
            } else if (_validate && _state != INFO) {
                addError("unknwon tag "+qName);
            }
        } catch (Exception ex) {
            addError("exception while parsing: "+ex.getMessage());
            throw new SAXException("exception while parsing: "+ex.getMessage(), ex);
        }
    }

    private void parseDepsConfs(String confs) {
        String[] conf = confs.split(";");
        for (int i = 0; i < conf.length; i++) {
            String[] ops = conf[i].split("->");
            if (ops.length == 1) {
                if (ops[0].indexOf(",") != -1) {
                    addError("invalid conf "+conf[i]+" for "+_dd.getDependencyRevisionId()+": mapping required in a list of confs");                        
                } else {
                    _dd.addDependencyConfiguration(ops[0].trim(), ops[0].trim());
                }
            } else if (ops.length == 2) {
                String[] modConfs = ops[0].split(",");
                String[] depConfs = ops[1].split(",");
                for (int j = 0; j < modConfs.length; j++) {
                    for (int k = 0; k < depConfs.length; k++) {
                        _dd.addDependencyConfiguration(modConfs[j].trim(), depConfs[k].trim());
                    }
                }
            } else {
                addError("invalid conf "+conf[i]+" for "+_dd.getDependencyRevisionId());                        
            }
        }
    }

    private void addDependencyArtifactsIncludes(Attributes attributes) {
        _state = ARTIFACT_INCLUDE;
        addDependencyArtifact(attributes, true);
    }

    private void addDependencyArtifactsExcludes(Attributes attributes) {
        _state = ARTIFACT_EXCLUDE;
        addDependencyArtifact(attributes, false);
    }   
    
    private void addDependencyArtifact(Attributes attributes, boolean includes) {
        String name = attributes.getValue("name");
        name = name == null ? ".*" : name;
        String type = attributes.getValue("type");
        type = type == null ? ".*" : type;
        String ext = attributes.getValue("ext");
        ext = ext != null?ext:type;
        if (includes) {
            _dad = new DefaultDependencyArtifactDescriptor(_dd, name, type, ext, includes);
        } else {
            String org = attributes.getValue("org");
            org = org == null ? ".*" : org;
            String module = attributes.getValue("module");
            module = module == null ? ".*" : module;
            ArtifactId aid = new ArtifactId(new ModuleId(org, module), name, type, ext);
            _dad = new DefaultDependencyArtifactDescriptor(_dd, aid, includes);
        }
        String confs = attributes.getValue("conf");
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
            parseDepsConfs(_defaultConf);
        }
    }

    private void checkConfigurations() {
        if (_md.getConfigurations().length == 0) {
            _md.addConfiguration(new Configuration("default"));
        }
    }
    
    public void warning(SAXParseException ex) {
        Message.warn("xml parsing: " +
                           getLocationString(ex)+": "+
                           ex.getMessage());
    }

    public void error(SAXParseException ex) {
        Message.error("xml parsing: " +
                getLocationString(ex)+": "+
                ex.getMessage());
    }

    public void fatalError(SAXParseException ex) throws SAXException {
        addError("[Fatal Error] "+
                           getLocationString(ex)+": "+
                           ex.getMessage());
    }

    /** Returns a string of the location. */
    private String getLocationString(SAXParseException ex) {
        StringBuffer str = new StringBuffer();

        String systemId = ex.getSystemId();
        if (systemId != null) {
            int index = systemId.lastIndexOf('/');
            if (index != -1)
                systemId = systemId.substring(index + 1);
            str.append(systemId);
        } else if (_res != null) {
            str.append(_res.toString());
        }
        str.append(':');
        str.append(ex.getLineNumber());
        str.append(':');
        str.append(ex.getColumnNumber());

        return str.toString();

    } // getLocationString(SAXParseException):String

    private void addError(String msg) {
        if (_res != null) {
            _errors.add(msg+" in "+_res+"\n");
        } else {
            _errors.add(msg+"\n");
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println(parseDescriptor(new Ivy(), new File("test/xml/module1/module1.ivy.xml").toURL(), true));
    }
}
