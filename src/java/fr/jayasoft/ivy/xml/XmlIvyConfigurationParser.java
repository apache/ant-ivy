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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.ModuleId;
import fr.jayasoft.ivy.url.URLHandlerRegistry;
import fr.jayasoft.ivy.util.Configurator;
import fr.jayasoft.ivy.util.Message;

/**
 * @author Hanin
 *
 */
public class XmlIvyConfigurationParser extends DefaultHandler {
	private Configurator _configurator = new Configurator();
    private List _configuratorTags = Arrays.asList(new String[] {"resolvers", "latest-strategies", "conflict-managers"});

    private Ivy _ivy;

    private String _defaultResolver;
    private String _defaultCM;
    private String _defaultLatest;
    private String _currentConfiguratorTag;

    public XmlIvyConfigurationParser(Ivy ivy) {
        _ivy = ivy;
	}

    public void parse(URL configuration) throws ParseException, IOException {
        // put every type definition from ivy to configurator
        Map typeDefs = _ivy.getTypeDefs();
        for (Iterator iter = typeDefs.keySet().iterator(); iter.hasNext();) {
            String name = (String) iter.next();
            _configurator.typeDef(name, (Class)typeDefs.get(name));
        }
        
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

    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        try {
            if (_configurator.getCurrent() != null) {
                if (attributes.getValue("ref") != null) {
                    if (attributes.getLength() != 1) {
                        throw new IllegalArgumentException("ref attribute should be the only one ! found "+attributes.getLength()+" in "+qName);
                    }
                    String name = attributes.getValue("ref");
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
                    for (int i=0; i<attributes.getLength(); i++) {
                        _configurator.setAttribute(attributes.getQName(i), _ivy.substitute(attributes.getValue(i)));
                    }
                }
            } else if ("typedef".equals(qName)) {
                String name = _ivy.substitute(attributes.getValue("name"));
                String className = _ivy.substitute(attributes.getValue("classname"));
                Class clazz = Class.forName(className);
                _ivy.typeDef(name, clazz);
                _configurator.typeDef(name, clazz);
            } else if ("properties".equals(qName)) {
                String propFilePath = _ivy.substitute(attributes.getValue("file"));
                try {
                    Message.verbose("loading properties: "+propFilePath);
                    _ivy.loadProperties(new File(propFilePath));
                } catch (Exception ex) {
                    Message.verbose("failed to load properties as file: trying as url: "+propFilePath);
                    _ivy.loadProperties(new URL(propFilePath));
                }
            } else if ("conf".equals(qName)) {
                String cache = attributes.getValue("defaultCache");
                if (cache != null) {
                    _ivy.setDefaultCache(new File(_ivy.substitute(cache)));
                }
                String validate = attributes.getValue("validate");
                if (validate != null) {
                    _ivy.setValidate(Boolean.valueOf(_ivy.substitute(validate)).booleanValue());
                }
                String up2d = attributes.getValue("checkUpToDate");
                if (up2d != null) {
                    _ivy.setCheckUpToDate(Boolean.valueOf(_ivy.substitute(up2d)).booleanValue());
                }
                String cacheIvyPattern = attributes.getValue("cacheIvyPattern");
                if (cacheIvyPattern != null) {
                    _ivy.setCacheIvyPattern(_ivy.substitute(cacheIvyPattern));
                }
                String cacheArtPattern = attributes.getValue("cacheArtifactPattern");
                if (cacheArtPattern != null) {
                    _ivy.setCacheArtifactPattern(_ivy.substitute(cacheArtPattern));
                }
                String useRemoteConfig = attributes.getValue("useRemoteConfig");
                if (useRemoteConfig != null) {
                    _ivy.setUseRemoteConfig(Boolean.valueOf(_ivy.substitute(useRemoteConfig)).booleanValue());
                }

                // we do not set following defaults here since no instances has been registered yet
                _defaultResolver = attributes.getValue("defaultResolver");
                _defaultCM = attributes.getValue("defaultConflictManager");
                _defaultLatest = attributes.getValue("defaultLatestStrategy");

            } else if (_configuratorTags.contains(qName)) {
                _currentConfiguratorTag = qName;
                _configurator.setRoot(_ivy);
            } else if ("module".equals(qName)) {
                String organisation = _ivy.substitute(attributes.getValue("organisation"));
                String module = _ivy.substitute(attributes.getValue("name"));
                String resolver = _ivy.substitute(attributes.getValue("resolver"));
                _ivy.addModuleConfiguration(new ModuleId(organisation, module), resolver);
            }
        } catch (Exception ex) {
            throw new SAXException("problem in config file", ex);
        }
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (_configurator.getCurrent() != null) {
            if (_configuratorTags.contains(qName) && _configurator.getDepth() == 1) {
                _configurator.clear();
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
            _ivy.setDefaultConflictManager(_ivy.getConflictManager(_ivy.substitute(_defaultCM)));
        }
        if (_defaultLatest != null) {
            _ivy.setDefaultLatestStrategy(_ivy.getLatestStrategy(_ivy.substitute(_defaultLatest)));
        }
    }
}
