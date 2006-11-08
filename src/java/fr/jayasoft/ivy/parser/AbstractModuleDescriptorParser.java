/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.parser;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import fr.jayasoft.ivy.Configuration;
import fr.jayasoft.ivy.DefaultDependencyDescriptor;
import fr.jayasoft.ivy.DefaultModuleDescriptor;
import fr.jayasoft.ivy.DependencyDescriptor;
import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.ModuleDescriptor;
import fr.jayasoft.ivy.ModuleRevisionId;
import fr.jayasoft.ivy.repository.Resource;
import fr.jayasoft.ivy.repository.url.URLResource;
import fr.jayasoft.ivy.util.Message;

public abstract class AbstractModuleDescriptorParser implements ModuleDescriptorParser {
    public ModuleDescriptor parseDescriptor(Ivy ivy, URL descriptorURL, boolean validate) throws ParseException, IOException {
        return parseDescriptor(ivy, descriptorURL, new URLResource(descriptorURL), validate);
    }
    
    protected abstract static class AbstractParser extends DefaultHandler {
        private static final String DEFAULT_CONF_MAPPING = "*->*";
        private String _defaultConf; // used only as defaultconf, not used for
                                     // guesssing right side part of a mapping
        private String _defaultConfMapping; // same as default conf but is used
                                            // for guesssing right side part of a mapping                                    
        private DefaultDependencyDescriptor _defaultConfMappingDescriptor;
        private Resource _res;
        private List _errors = new ArrayList();
        protected DefaultModuleDescriptor _md;
		private ModuleDescriptorParser _parser;
        
        protected AbstractParser(ModuleDescriptorParser parser) {
        	_parser = parser;
        }
        
        public ModuleDescriptorParser getModuleDescriptorParser() {
        	return _parser;
        }
        
        protected void checkErrors() throws ParseException {
            if (!_errors.isEmpty()) {
                throw new ParseException(_errors.toString(), 0);
            }
        }
        
        protected void setResource(Resource res) {
            _res = res; // used for log and date only
            _md = new DefaultModuleDescriptor(_parser, res);
            _md.setLastModified(getLastModified());
        }
        
        protected Resource getResource() {
            return _res;
        }
        
        protected String getDefaultConfMapping() {
            return _defaultConfMapping;
        }
        
        protected void setDefaultConfMapping(String defaultConf) {
            _defaultConfMapping = defaultConf;
        }        
        
        
        protected void parseDepsConfs(String confs, DefaultDependencyDescriptor dd) {
            parseDepsConfs(confs, dd, _defaultConfMapping != null);        
        }
        protected void parseDepsConfs(String confs, DefaultDependencyDescriptor dd, boolean useDefaultMappingToGuessRightOperande) {
        	parseDepsConfs(confs, dd, useDefaultMappingToGuessRightOperande, true);
        }
        protected void parseDepsConfs(String confs, DefaultDependencyDescriptor dd, boolean useDefaultMappingToGuessRightOperande, boolean evaluateConditions) {
        	if (confs == null) {
        		return;
        	}
        	
            String[] conf = confs.split(";");
            parseDepsConfs(conf, dd, useDefaultMappingToGuessRightOperande, evaluateConditions);
        }
        protected void parseDepsConfs(String[] conf, DefaultDependencyDescriptor dd, boolean useDefaultMappingToGuessRightOperande) {
        	parseDepsConfs(conf, dd, useDefaultMappingToGuessRightOperande, true);
        }
        protected void parseDepsConfs(String[] conf, DefaultDependencyDescriptor dd, boolean useDefaultMappingToGuessRightOperande, boolean evaluateConditions) {
        	replaceConfigurationWildcards(_md);
            for (int i = 0; i < conf.length; i++) {
                String[] ops = conf[i].split("->");
                if (ops.length == 1) {
                    String[] modConfs = ops[0].split(",");
                    if (!useDefaultMappingToGuessRightOperande) {
                        for (int j = 0; j < modConfs.length; j++) {
                            dd.addDependencyConfiguration(modConfs[j].trim(), modConfs[j].trim());
                        }
                    } else {
                        for (int j = 0; j < modConfs.length; j++) {
                            String[] depConfs = getDefaultConfMappingDescriptor().getDependencyConfigurations(modConfs[j]);
                            if (depConfs.length > 0) {
                                for (int k = 0; k < depConfs.length; k++) {
                                	String mappedDependency = evaluateConditions ? evaluateCondition(depConfs[k].trim(), dd): depConfs[k].trim();
                                	if (mappedDependency != null) {
                                        dd.addDependencyConfiguration(modConfs[j].trim(), mappedDependency);
                                	}
                                }
                            } else {
                                // no default mapping found for this configuration, map configuration to itself
                                dd.addDependencyConfiguration(modConfs[j].trim(), modConfs[j].trim());
                            }
                        }
                    }
                } else if (ops.length == 2) {
                    String[] modConfs = ops[0].split(",");
                    String[] depConfs = ops[1].split(",");
                    for (int j = 0; j < modConfs.length; j++) {
                        for (int k = 0; k < depConfs.length; k++) {
                        	String mappedDependency = evaluateConditions ? evaluateCondition(depConfs[k].trim(), dd): depConfs[k].trim();
                        	if (mappedDependency != null) {
                                dd.addDependencyConfiguration(modConfs[j].trim(), mappedDependency);
                        	}
                        }
                    }
                } else {
                    addError("invalid conf "+conf[i]+" for "+dd.getDependencyRevisionId());                        
                }
            }
            
            if (_md.isMappingOverride()) {
            	addExtendingConfigurations(conf, dd, useDefaultMappingToGuessRightOperande);
            }
        }
        /**
         * Evaluate the optional condition in the given configuration, like "[org=MYORG]confX".
         * If the condition evaluates to true, the configuration is returned, if the condition
         * evaluatate to false, null is returned.
         * If there are no conditions, the configuration itself is returned.
         * 
		 * @param conf the configuration to evaluate
		 * @param dd the dependencydescriptor to which the configuration will be added
		 * @return the evaluated condition
		 */
		private String evaluateCondition(String conf, DefaultDependencyDescriptor dd) {
			if (conf.charAt(0) != '[') {
				return conf;
			}
			
			int endConditionIndex = conf.indexOf(']');
			if (endConditionIndex == -1) {
				addError("invalid conf " + conf + " for " + dd.getDependencyRevisionId());
                return null;
			}
			
			String condition = conf.substring(1, endConditionIndex);
			
			int notEqualIndex = condition.indexOf("!=");
			if (notEqualIndex == -1) {
				int equalIndex = condition.indexOf('=');
				if (equalIndex == -1) { 
					addError("invalid conf " + conf + " for " + dd.getDependencyRevisionId());
                    return null;
				}
				
				String leftOp = condition.substring(0, equalIndex).trim();
				String rightOp = condition.substring(equalIndex + 1).trim();
				
				// allow organisation synonyms, like 'org' or 'organization'
				if (leftOp.equals("org") || leftOp.equals("organization")) {
					leftOp = "organisation";
				}
				
				String attrValue = dd.getAttribute(leftOp);
				if (!rightOp.equals(attrValue)) {
					return null;
				}
			} else {
				String leftOp = condition.substring(0, notEqualIndex).trim();
				String rightOp = condition.substring(notEqualIndex + 2).trim();
				
				// allow organisation synonyms, like 'org' or 'organization'
				if (leftOp.equals("org") || leftOp.equals("organization")) {
					leftOp = "organisation";
				}
				
				String attrValue = dd.getAttribute(leftOp);
				if (rightOp.equals(attrValue)) {
					return null;
				}
			}
			
			return conf.substring(endConditionIndex + 1);
		}

		private void addExtendingConfigurations(String[] confs, DefaultDependencyDescriptor dd, boolean useDefaultMappingToGuessRightOperande) {
        	for (int i = 0; i < confs.length; i++) {
        		addExtendingConfigurations(confs[i], dd, useDefaultMappingToGuessRightOperande);
        	}
        }        
        private void addExtendingConfigurations(String conf, DefaultDependencyDescriptor dd, boolean useDefaultMappingToGuessRightOperande) {
        	Set configsToAdd = new HashSet();
        	Configuration[] configs = _md.getConfigurations();
        	for (int i = 0; i < configs.length; i++) {
        		String[] ext = configs[i].getExtends();
        		for (int j = 0; j < ext.length; j++) {
        			if (conf.equals(ext[j])) {
        				String configName = configs[i].getName();
//                		if (getDefaultConfMappingDescriptor().getDependencyConfigurations(configName).length > 0) {
            				configsToAdd.add(configName);
//                		} else {
                			addExtendingConfigurations(configName, dd, useDefaultMappingToGuessRightOperande);
//                		}
        			}
        		}
        	}
        	
        	String[] confs = (String[]) configsToAdd.toArray(new String[configsToAdd.size()]);
        	parseDepsConfs(confs, dd, useDefaultMappingToGuessRightOperande);
        }
        
        protected DependencyDescriptor getDefaultConfMappingDescriptor() {
            if (_defaultConfMappingDescriptor == null) {
                _defaultConfMappingDescriptor = new DefaultDependencyDescriptor(ModuleRevisionId.newInstance("", "", ""), false);
                parseDepsConfs(_defaultConfMapping, _defaultConfMappingDescriptor, false, false);
            }
            return _defaultConfMappingDescriptor;
        }
        
        protected void addError(String msg) {
            if (_res != null) {
                _errors.add(msg+" in "+_res+"\n");
            } else {
                _errors.add(msg+"\n");
            }
        }
        public void warning(SAXParseException ex) {
            Message.warn("xml parsing: " +
                    getLocationString(ex)+": "+
                    ex.getMessage());
        }
        
        public void error(SAXParseException ex) {
            addError("xml parsing: " +
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
            } else if (getResource() != null) {
                str.append(getResource().toString());
            }
            str.append(':');
            str.append(ex.getLineNumber());
            str.append(':');
            str.append(ex.getColumnNumber());
            
            return str.toString();
            
        } // getLocationString(SAXParseException):String
        
        protected String getDefaultConf() {
            return _defaultConfMapping != null ? _defaultConfMapping : (_defaultConf != null ? _defaultConf : DEFAULT_CONF_MAPPING);
        }
        protected void setDefaultConf(String defaultConf) {
            _defaultConf = defaultConf;
        }
        public ModuleDescriptor getModuleDescriptor() throws ParseException {
            checkErrors();
            return _md;
        }
        protected Date getDefaultPubDate() {
            return new Date(_md.getLastModified());
        }
        protected long getLastModified() {
            long last = getResource().getLastModified();
            if (last > 0) {
                return  last;
            } else {
                Message.debug("impossible to get date for "+getResource()+": using 'now'");
                return System.currentTimeMillis();
            }
        }
        
        private void replaceConfigurationWildcards(ModuleDescriptor md) {
            Configuration[] configs = md.getConfigurations();
            for (int i = 0; i < configs.length; i++) {
                configs[i].replaceWildcards(md);
            }
        }

    }    
}
