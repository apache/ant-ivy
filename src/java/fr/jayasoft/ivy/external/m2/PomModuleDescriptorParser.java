/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.external.m2;

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
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import fr.jayasoft.ivy.ArtifactId;
import fr.jayasoft.ivy.Configuration;
import fr.jayasoft.ivy.DefaultArtifact;
import fr.jayasoft.ivy.DefaultDependencyArtifactDescriptor;
import fr.jayasoft.ivy.DefaultDependencyDescriptor;
import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.ModuleDescriptor;
import fr.jayasoft.ivy.ModuleId;
import fr.jayasoft.ivy.ModuleRevisionId;
import fr.jayasoft.ivy.Configuration.Visibility;
import fr.jayasoft.ivy.matcher.ExactPatternMatcher;
import fr.jayasoft.ivy.matcher.PatternMatcher;
import fr.jayasoft.ivy.parser.AbstractModuleDescriptorParser;
import fr.jayasoft.ivy.parser.ModuleDescriptorParser;
import fr.jayasoft.ivy.repository.Resource;
import fr.jayasoft.ivy.util.IvyPatternHelper;
import fr.jayasoft.ivy.util.Message;
import fr.jayasoft.ivy.util.XMLHelper;
import fr.jayasoft.ivy.xml.XmlModuleDescriptorWriter;

public class PomModuleDescriptorParser extends AbstractModuleDescriptorParser {
    public static final Configuration[] MAVEN2_CONFIGURATIONS = new Configuration[] {
        new Configuration("default", Visibility.PUBLIC, "runtime dependencies and master artifact can be used with this conf", new String[] {"runtime", "master"}),
        new Configuration("master", Visibility.PUBLIC, "contains only the artifact published by this module itself, with no transitive dependencies", new String[0]),
        new Configuration("compile", Visibility.PUBLIC, "this is the default scope, used if none is specified. Compile dependencies are available in all classpaths.", new String[0]),
        new Configuration("provided", Visibility.PUBLIC, "this is much like compile, but indicates you expect the JDK or a container to provide it. It is only available on the compilation classpath, and is not transitive.", new String[0]),
        new Configuration("runtime", Visibility.PUBLIC, "this scope indicates that the dependency is not required for compilation, but is for execution. It is in the runtime and test classpaths, but not the compile classpath.", new String[] {"compile"}),
        new Configuration("test", Visibility.PRIVATE, "this scope indicates that the dependency is not required for normal use of the application, and is only available for the test compilation and execution phases.", new String[0]),
        new Configuration("system", Visibility.PUBLIC, "this scope is similar to provided except that you have to provide the JAR which contains it explicitly. The artifact is always available and is not looked up in a repository.", new String[0]),
    };
    private static final Configuration OPTIONAL_CONFIGURATION = new Configuration("optional", Visibility.PUBLIC, "contains all optional dependencies", new String[0]);
    private static final Map MAVEN2_CONF_MAPPING = new HashMap();
    
    static {
        MAVEN2_CONF_MAPPING.put("compile", "compile->@(*),master(*);runtime->@(*)");
        MAVEN2_CONF_MAPPING.put("provided", "provided->compile(*),provided(*),runtime(*),master(*)");
        MAVEN2_CONF_MAPPING.put("runtime", "runtime->compile(*),runtime(*),master(*)");
        MAVEN2_CONF_MAPPING.put("test", "test->compile(*),runtime(*),master(*)");
        MAVEN2_CONF_MAPPING.put("system", "system->master(*)");
    }
    
    private static final class Parser extends AbstractParser {
        private Ivy _ivy;
        private Stack _contextStack = new Stack();
        private String _organisation;
        private String _module;
        private String _revision;
        private String _scope;
        private boolean _optional = false;
        private List _exclusions = new ArrayList();
        private DefaultDependencyDescriptor _dd;
        private Map _properties = new HashMap();

        public Parser(ModuleDescriptorParser parser, Ivy ivy, Resource res) {
        	super(parser);
            _ivy = ivy;
            setResource(res);
            _md.setResolvedPublicationDate(new Date(res.getLastModified()));
            for (int i = 0; i < MAVEN2_CONFIGURATIONS.length; i++) {
                _md.addConfiguration(MAVEN2_CONFIGURATIONS[i]);
            }            
        }

        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            _contextStack.push(qName);
            if ("optional".equals(qName)) {
                _optional = true;
            } else if ("exclusions".equals(qName)) {
                if (_dd == null) {
                    // stores dd now cause exclusions will override org and module
                    _dd = new DefaultDependencyDescriptor(_md, ModuleRevisionId.newInstance(_organisation, _module, _revision), true, false, true);
                    _organisation = null;
                    _module = null;
                    _revision = null;
                }
            } else if (_md.getModuleRevisionId() == null  && ("project/dependencies/dependency".equals(getContext()))) {
                fillMrid();
            }
        }

        private void fillMrid() throws SAXException {
            if (_organisation == null) {
                throw new SAXException("no groupId found in pom");
            }
            if (_module == null) {
                throw new SAXException("no artifactId found in pom");
            }
            if (_revision == null) {
                _revision = "SNAPSHOT";
            }
            ModuleRevisionId mrid = ModuleRevisionId.newInstance(_organisation, _module, _revision);
            _properties.put("pom.version", _revision);
            _md.setModuleRevisionId(mrid);
            _md.addArtifact("master", new DefaultArtifact(mrid, getDefaultPubDate(),_module, "jar", "jar"));
            _organisation = null;
            _module = null;
            _revision = null;
        }
        
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (_md.getModuleRevisionId() == null  && ("project".equals(getContext()))) {
                fillMrid();                
            } else if (((_organisation != null && _module != null && _revision != null) || _dd != null) && "project/dependencies/dependency".equals(getContext())) {
                if (_dd == null) {
                    _dd = new DefaultDependencyDescriptor(_md, ModuleRevisionId.newInstance(_organisation, _module, _revision), true, false, true);
                }
                _scope = _scope == null ? "compile" : _scope;
                if (_optional && "compile".equals(_scope)) {
                    _scope = "runtime";
                }
                String mapping = (String)MAVEN2_CONF_MAPPING.get(_scope);
                if (mapping == null) {
                    Message.verbose("unknown scope "+_scope+" in "+getResource());
                    mapping = (String)MAVEN2_CONF_MAPPING.get("compile");
                }
                if (_optional) {
                    mapping = mapping.replaceAll(_scope+"\\-\\>", "optional->");
                    if (_md.getConfiguration("optional") == null) {
                        _md.addConfiguration(OPTIONAL_CONFIGURATION);
                    }
                }
                parseDepsConfs(mapping, _dd);
                
                for (Iterator iter = _exclusions.iterator(); iter.hasNext();) {
                    ModuleId mid = (ModuleId)iter.next();
                    String[] confs = _dd.getModuleConfigurations();
                    for (int i = 0; i < confs.length; i++) {
                        _dd.addDependencyArtifactExcludes(confs[i], new DefaultDependencyArtifactDescriptor(_dd, new ArtifactId(mid, PatternMatcher.ANY_EXPRESSION, PatternMatcher.ANY_EXPRESSION, PatternMatcher.ANY_EXPRESSION), false, ExactPatternMatcher.getInstance()));
                    }
                }
                _md.addDependency(_dd);
                _dd = null;
            } else if ((_organisation != null && _module != null) && "project/dependencies/dependency/exclusions/exclusion".equals(getContext())) {
                _exclusions.add(new ModuleId(_organisation, _module));
                _organisation = null;
                _module = null;
            }
            if ("dependency".equals(qName)) {
                _organisation = null;
                _module = null;
                _revision = null;
                _scope = null;
                _optional = false;
                _exclusions.clear();
            }
            _contextStack.pop();
        }
        
        public void characters(char[] ch, int start, int length) throws SAXException {
            String txt = IvyPatternHelper.substituteVariables(new String(ch, start, length).trim(), _properties);
            if (txt.trim().length() == 0) {
            	return;
            }
            String context = getContext();
            if (context.equals("project/parent/groupId") && _organisation == null) {
            	_organisation = txt;
                return;
            }
            if (context.startsWith("project/parent")) {
                return;
            }
            if (_md.getModuleRevisionId() == null || context.startsWith("project/dependencies/dependency")) {
                if (context.equals("project/groupId")) {
                    _organisation = txt;
                } else if (_organisation == null && context.endsWith("groupId")) {
                	_organisation = txt;
                } else if (_module == null && context.endsWith("artifactId")) {
                    _module = txt;
                } else if (_revision == null && context.endsWith("version")) {
                    _revision = txt;
                } else if (_scope == null && context.endsWith("scope")) {
                    _scope = txt;
                }
            }
        }
        
        

        private String getContext() {
            StringBuffer buf = new StringBuffer();
            for (Iterator iter = _contextStack.iterator(); iter.hasNext();) {
                String ctx = (String)iter.next();
                buf.append(ctx).append("/");
            }
            if (buf.length() > 0) {
                buf.setLength(buf.length() - 1);
            }
            return buf.toString();
        }

        public ModuleDescriptor getDescriptor() {
            if (_md.getModuleRevisionId() == null) {
                return null;
            }
            return _md;
        }
    }

    private static PomModuleDescriptorParser INSTANCE = new PomModuleDescriptorParser();
    
    public static PomModuleDescriptorParser getInstance() {
        return INSTANCE;
    }
    
    private PomModuleDescriptorParser() {
        
    }

    public ModuleDescriptor parseDescriptor(Ivy ivy, URL descriptorURL, Resource res, boolean validate) throws ParseException, IOException {
        Parser parser = new Parser(this, ivy, res);
        try {
            XMLHelper.parse(descriptorURL, null, parser);
        } catch (SAXException ex) {
            ParseException pe = new ParseException(ex.getMessage()+" in "+descriptorURL, 0);
            pe.initCause(ex);
            throw pe;
        } catch (ParserConfigurationException ex) {
            IllegalStateException ise = new IllegalStateException(ex.getMessage()+" in "+descriptorURL);
            ise.initCause(ex);
            throw ise;
        }
        return parser.getDescriptor();
    }

    public void toIvyFile(InputStream is, Resource res, File destFile, ModuleDescriptor md) throws ParseException, IOException {
    	try {
    		XmlModuleDescriptorWriter.write(md, destFile);
    	} finally {
    		if (is != null) {
    			is.close();
    		}
    	}
    }

    public boolean accept(Resource res) {
        return res.getName().endsWith(".pom") || res.getName().endsWith("pom.xml") || res.getName().endsWith("project.xml");
    }

    public String toString() {
        return "pom parser";
    }
}
