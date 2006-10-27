/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.xml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import fr.jayasoft.ivy.ConflictManager;
import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.LatestStrategy;
import fr.jayasoft.ivy.ModuleId;
import fr.jayasoft.ivy.circular.CircularDependencyStrategy;
import fr.jayasoft.ivy.matcher.PatternMatcher;
import fr.jayasoft.ivy.status.StatusManager;
import fr.jayasoft.ivy.url.URLHandlerRegistry;
import fr.jayasoft.ivy.util.Configurator;
import fr.jayasoft.ivy.util.Message;

/**
 * @author Hanin
 *
 */
public class XmlIvyConfigurationParser extends DefaultHandler {
	private Configurator _configurator;
    private List _configuratorTags = Arrays.asList(new String[] {"resolvers", "namespaces", "parsers", "latest-strategies", "conflict-managers", "outputters", "version-matchers", "statuses", "circular-dependency-strategies", "triggers"});

    private Ivy _ivy;

    private String _defaultResolver;
    private String _defaultCM;
    private String _defaultLatest;
    private String _defaultCircular;
    private String _currentConfiguratorTag;

    public XmlIvyConfigurationParser(Ivy ivy) {
        _ivy = ivy;
	}

    public void parse(URL configuration) throws ParseException, IOException {
        _configurator = new Configurator();
        // put every type definition from ivy to configurator
        Map typeDefs = _ivy.getTypeDefs();
        for (Iterator iter = typeDefs.keySet().iterator(); iter.hasNext();) {
            String name = (String) iter.next();
            _configurator.typeDef(name, (Class)typeDefs.get(name));
        }
        
        doParse(configuration);
    }

    private void doParse(URL configuration) throws IOException, ParseException {
        InputStream stream = null;
        try {
            stream = URLHandlerRegistry.getDefault().openStream(configuration);
            SAXParserFactory.newInstance().newSAXParser().parse(
                stream,
                this);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            ParseException pe = new ParseException("failed to configure with "+configuration+": "+e.getMessage(), 0);
            pe.initCause(e);
            throw pe;
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private void parse(Configurator configurator, URL configuration) throws IOException, ParseException {
        _configurator = configurator;
        doParse(configuration);        
    }

    public void startElement(String uri, String localName, String qName, Attributes att) throws SAXException {
        // we first copy attributes in a Map to be able to modify them
        Map attributes = new HashMap();
        for (int i=0; i<att.getLength(); i++) {
            attributes.put(att.getQName(i), att.getValue(i));
        }
        
        try {
            if (_configurator.getCurrent() != null) {
                if ("macrodef".equals(_currentConfiguratorTag) && _configurator.getTypeDef(qName) != null) {
                    String name = (String)attributes.get("name");
                    if (name == null) {
                        attributes.put("name", "@{name}");
                    } else if (_configurator.isTopLevelMacroRecord() && name.indexOf("@{name}") != -1) {
                    	attributes.put("name", name);
                    } else {
                    	attributes.put("name", "@{name}-"+name);
                    }
                }
                if (attributes.get("ref") != null) {
                    if (attributes.size() != 1) {
                        throw new IllegalArgumentException("ref attribute should be the only one ! found "+attributes.size()+" in "+qName);
                    }
                    String name = (String)attributes.get("ref");
                    Object child = null;
                    if ("resolvers".equals(_currentConfiguratorTag)) {
                        child = _ivy.getResolver(name);
                        if (child == null) {
                            throw new IllegalArgumentException("unknown resolver "+name+": resolver should be defined before being referenced");
                        }
                    } else if ("latest-strategies".equals(_currentConfiguratorTag)) {
                        child = _ivy.getLatestStrategy(name);
                        if (child == null) {
                            throw new IllegalArgumentException("unknown latest strategy "+name+": latest strategy should be defined before being referenced");
                        }
                    } else if ("conflict-managers".equals(_currentConfiguratorTag)) {
                        child = _ivy.getConflictManager(name);
                        if (child == null) {
                            throw new IllegalArgumentException("unknown conflict manager "+name+": conflict manager should be defined before being referenced");
                        }
                    }
                    if (child == null) {
                        throw new IllegalArgumentException("bad reference "+name);
                    }
                    _configurator.addChild(qName, child);
                } else {
                    _configurator.startCreateChild(qName);
                    for (Iterator iter = attributes.keySet().iterator(); iter.hasNext();) {
                        String attName = (String)iter.next();
                        _configurator.setAttribute(attName, _ivy.substitute((String)attributes.get(attName)));
                    }
                }
            } else if ("classpath".equals(qName)) {
                String urlStr = _ivy.substitute((String)attributes.get("url"));
                URL url = null;
                if (urlStr == null) {
                    String file = _ivy.substitute((String)attributes.get("file"));
                    if (file == null) {
                        throw new IllegalArgumentException("either url or file should be given for classpath element");
                    } else {
                        url = new File(file).toURL();
                    }
                } else {
                    url = new URL(urlStr);
                }
                _ivy.addClasspathURL(url);
            } else if ("typedef".equals(qName)) {
                String name = _ivy.substitute((String)attributes.get("name"));
                String className = _ivy.substitute((String)attributes.get("classname"));
                Class clazz = _ivy.typeDef(name, className);
                _configurator.typeDef(name, clazz);
            } else if ("property".equals(qName)) {
                String name = _ivy.substitute((String)attributes.get("name"));
                String value = _ivy.substitute((String)attributes.get("value"));
                String override = _ivy.substitute((String)attributes.get("override"));
                if (name == null) {
                    throw new IllegalArgumentException("missing attribute name on property tag");
                }
                if (value == null) {
                    throw new IllegalArgumentException("missing attribute value on property tag");
                }
                _ivy.setVariable(name, value, override == null ? true : Boolean.valueOf(override).booleanValue());
            } else if ("properties".equals(qName)) {
                String propFilePath = _ivy.substitute((String)attributes.get("file"));
                String override = _ivy.substitute((String)attributes.get("override"));
                try {
                    Message.verbose("loading properties: "+propFilePath);
                    _ivy.loadProperties(new File(propFilePath), override == null ? true : Boolean.valueOf(override).booleanValue());
                } catch (Exception fileEx) {
                    Message.verbose("failed to load properties as file: trying as url: "+propFilePath);
                    try {
                    	_ivy.loadProperties(new URL(propFilePath), override == null ? true : Boolean.valueOf(override).booleanValue());
                    } catch (Exception urlEx) {
                    	throw new IllegalArgumentException("unable to load properties from "+propFilePath+". Tried both as an url and a file, with no success. File exception: "+fileEx+". URL exception: "+urlEx);
                    }
                }
            } else if ("include".equals(qName)) {
                Map variables = new HashMap(_ivy.getVariables());
                try {
                    String propFilePath = _ivy.substitute((String)attributes.get("file"));
                    URL ivyconfURL = null; 
                    if (propFilePath == null) {
                        propFilePath = _ivy.substitute((String)attributes.get("url"));
                        if (propFilePath == null) {
                            Message.error("bad include tag: specify file or url to include");
                            return;
                        } else {
                            Message.verbose("including url: "+propFilePath);
                            ivyconfURL = new URL(propFilePath);
                            _ivy.setConfigurationVariables(ivyconfURL);
                        }
                    } else {
                        File incFile = new File(propFilePath);
                        if (!incFile.exists()) {
                            Message.error("impossible to include "+incFile+": file does not exist");
                            return;
                        } else {
                            Message.verbose("including file: "+propFilePath);
                            _ivy.setConfigurationVariables(incFile);
                            ivyconfURL = incFile.toURL();
                        }
                    }
                    new XmlIvyConfigurationParser(_ivy).parse(_configurator, ivyconfURL);
                } finally {
                    _ivy.setVariables(variables);
                }
            } else if ("conf".equals(qName)) {
                String cache = (String)attributes.get("defaultCache");
                if (cache != null) {
                    _ivy.setDefaultCache(new File(_ivy.substitute(cache)));
                }
                String defaultBranch = (String)attributes.get("defaultBranch");
                if (defaultBranch != null) {
                    _ivy.setDefaultBranch(_ivy.substitute(defaultBranch));
                }
                String validate = (String)attributes.get("validate");
                if (validate != null) {
                    _ivy.setValidate(Boolean.valueOf(_ivy.substitute(validate)).booleanValue());
                }
                String up2d = (String)attributes.get("checkUpToDate");
                if (up2d != null) {
                    _ivy.setCheckUpToDate(Boolean.valueOf(_ivy.substitute(up2d)).booleanValue());
                }
                String cacheIvyPattern = (String)attributes.get("cacheIvyPattern");
                if (cacheIvyPattern != null) {
                    _ivy.setCacheIvyPattern(_ivy.substitute(cacheIvyPattern));
                }
                String cacheArtPattern = (String)attributes.get("cacheArtifactPattern");
                if (cacheArtPattern != null) {
                    _ivy.setCacheArtifactPattern(_ivy.substitute(cacheArtPattern));
                }
                String useRemoteConfig = (String)attributes.get("useRemoteConfig");
                if (useRemoteConfig != null) {
                    _ivy.setUseRemoteConfig(Boolean.valueOf(_ivy.substitute(useRemoteConfig)).booleanValue());
                }

                // we do not set following defaults here since no instances has been registered yet
                _defaultResolver = (String)attributes.get("defaultResolver");
                _defaultCM = (String)attributes.get("defaultConflictManager");
                _defaultLatest = (String)attributes.get("defaultLatestStrategy");
                _defaultCircular = (String)attributes.get("circularDependencyStrategy");

            } else if ("version-matchers".equals(qName)) {
                _currentConfiguratorTag = qName;
                _configurator.setRoot(_ivy);
                if ("true".equals(_ivy.substitute((String)attributes.get("usedefaults")))) {
                    _ivy.configureDefaultVersionMatcher();
                }
            } else if ("statuses".equals(qName)) {
                _currentConfiguratorTag = qName;
                StatusManager m = new StatusManager();
                String defaultStatus = _ivy.substitute((String)attributes.get("default"));
                if (defaultStatus != null) {
                    m.setDefaultStatus(defaultStatus);
                }
                _ivy.setStatusManager(m);
                _configurator.setRoot(m);
            } else if (_configuratorTags.contains(qName)) {
                _currentConfiguratorTag = qName;
                _configurator.setRoot(_ivy);
            } else if ("macrodef".equals(qName)) {
                _currentConfiguratorTag = qName;
                Configurator.MacroDef macrodef = _configurator.startMacroDef((String)attributes.get("name"));
                macrodef.addAttribute("name", null);
            } else if ("module".equals(qName)) {
                String organisation = _ivy.substitute((String)attributes.get("organisation"));
                String module = _ivy.substitute((String)attributes.get("name"));
                String resolver = _ivy.substitute((String)attributes.get("resolver"));
                String branch = _ivy.substitute((String)attributes.get("branch"));
                String cm = _ivy.substitute((String)attributes.get("conflict-manager"));
                String matcher = _ivy.substitute((String)attributes.get("matcher"));
                matcher = matcher == null ? PatternMatcher.EXACT_OR_REGEXP : matcher;
                _ivy.addModuleConfiguration(new ModuleId(organisation, module), _ivy.getMatcher(matcher), resolver, branch, cm);
            }
        } catch (Exception ex) {
            throw new SAXException("problem in config file: "+ex.getMessage(), ex);
        }
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (_configurator.getCurrent() != null) {
            if (_configuratorTags.contains(qName) && _configurator.getDepth() == 1) {
                _configurator.clear();
                _currentConfiguratorTag = null;
            } else if ("macrodef".equals(qName) && _configurator.getDepth() == 1) {
                _configurator.endMacroDef();
                _currentConfiguratorTag = null;
            } else {
                _configurator.endCreateChild();
            }
        }
    }
    
    public void endDocument() throws SAXException {
        if (_defaultResolver != null) {
            _ivy.setDefaultResolver(_ivy.substitute(_defaultResolver));
        }
        if (_defaultCM != null) {
            ConflictManager conflictManager = _ivy.getConflictManager(_ivy.substitute(_defaultCM));
            if (conflictManager == null) {
                throw new IllegalArgumentException("unknown conflict manager "+_ivy.substitute(_defaultCM));
            }
            _ivy.setDefaultConflictManager(conflictManager);
        }
        if (_defaultLatest != null) {
            LatestStrategy latestStrategy = _ivy.getLatestStrategy(_ivy.substitute(_defaultLatest));
            if (latestStrategy == null) {
                throw new IllegalArgumentException("unknown latest strategy " + _ivy.substitute(_defaultLatest));
            }
            _ivy.setDefaultLatestStrategy(latestStrategy);
        }
        if (_defaultCircular != null) {
            CircularDependencyStrategy strategy = _ivy.getCircularDependencyStrategy(_ivy.substitute(_defaultCircular));
            if (strategy == null) {
                throw new IllegalArgumentException("unknown circular dependency strategy " + _ivy.substitute(_defaultCircular));
            }
            _ivy.setCircularDependencyStrategy(strategy);
        }
    }
}
