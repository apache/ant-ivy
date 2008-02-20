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

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.util.XMLHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;



/**
 * Provides the method to read some data out of the DOM tree of a pom 
 * file.
 */
public class PomReader {
    
    private static final String PACKAGING = "packaging";
    private static final String DEPENDENCY = "dependency";
    private static final String DEPENDENCIES = "dependencies";
    private static final String DEPENDENCY_MGT = "dependencyManagement";
    private static final String PROJECT = "project";
    private static final String GROUP_ID = "groupId";
    private static final String ARTIFACT_ID = "artifactId";
    private static final String VERSION = "version";
    private static final String PARENT = "parent";
    private static final String SCOPE = "scope";
    private static final String CLASSIFIER = "classifier";
    private static final String OPTIONAL = "optional";
    private static final String EXCLUSIONS = "exclusions";
    private static final String EXCLUSION = "exclusion";
    private static final String DISTRIBUTION_MGT = "distributionManagement";
    private static final String RELOCATION = "relocation";
    private static final String PROPERTIES = "properties";
    
    
    

    private HashMap properties = new HashMap();
    
    private final Element projectElement;
    private final Element parentElement;
    
    public PomReader(URL descriptorURL, Resource res) throws IOException, SAXException {
        Document pomDomDoc = XMLHelper.parseToDom(descriptorURL, res);
        projectElement = pomDomDoc.getDocumentElement();
        if (!PROJECT.equals(projectElement.getNodeName())) {
            throw new SAXParseException("project must be the root tag" , res.getName() , 
                                        res.getName(), 0, 0);
        }
        parentElement = getFirstChildElement(projectElement , PARENT);
        //TODO read the properties because it must be used to interpret every other field
    }


    public boolean hasParent() {
        return parentElement != null;
    }
    
    /**
     * Add a property if not yet set and value is not null.
     * This garantee that property keep the first value that is put on it and that the properties
     * are never null.
     */
    public void setProperty(String prop, String val) {
        if (!properties.containsKey(prop) && val != null) {
            properties.put(prop, val);
        }
    }

    
    public String getGroupId() {
        String groupId = getFirstChildText(projectElement , GROUP_ID);
        if (groupId == null) {
            groupId = getFirstChildText(parentElement, GROUP_ID);
        } 
        return replaceProps(groupId);

    }

    public String getParentGroupId() {
        String groupId = getFirstChildText(parentElement , GROUP_ID);
        if (groupId == null) {
            groupId = getFirstChildText(projectElement, GROUP_ID);
        }
        return replaceProps(groupId);
    }


    
    public String getArtifactId() {
        String val = getFirstChildText(projectElement , ARTIFACT_ID);
        if (val == null) {
            val = getFirstChildText(parentElement, ARTIFACT_ID);
        }
        return replaceProps(val);
    }

    public String getParentArtifactId() {
        String val = getFirstChildText(parentElement , ARTIFACT_ID);
        if (val == null) {
            val = getFirstChildText(projectElement, ARTIFACT_ID);
        } 
        return replaceProps(val);
    }


    public String getVersion() {
        String val = getFirstChildText(projectElement , VERSION);
        if (val == null) {
            val = getFirstChildText(parentElement, VERSION);
        } 
        return replaceProps(val);
    }

    public String getParentVersion() {
        String val = getFirstChildText(parentElement , VERSION);
        if (val == null) {
            val = getFirstChildText(projectElement, VERSION);
        } 
        return replaceProps(val);
    }

    
    public String getPackaging() {
        String val = getFirstChildText(projectElement , PACKAGING);
        if (val == null) {
            val = "jar";
        }
        return val;
    }

    
    public ModuleRevisionId getRelocation() {
        Element distrMgt = getFirstChildElement(projectElement, DISTRIBUTION_MGT);
        Element relocation = getFirstChildElement(distrMgt , RELOCATION);
        if (relocation == null) {
            return null;
        } else {
            String relocGroupId = getFirstChildText(relocation, GROUP_ID);
            String relocArtId = getFirstChildText(relocation, ARTIFACT_ID);
            String relocVersion = getFirstChildText(relocation, VERSION);
            relocGroupId = relocGroupId == null ? getGroupId() : relocGroupId;
            relocArtId = relocArtId == null ? getArtifactId() : relocArtId;
            relocVersion = relocVersion == null ? getVersion() : relocVersion;
            return ModuleRevisionId.newInstance(relocGroupId, relocArtId, relocVersion);
        }
    }
    
    public List /* <PomDependencyData> */ getDependencies() {
        Element dependenciesElement = getFirstChildElement(projectElement, DEPENDENCIES);
        LinkedList dependencies = new LinkedList();
        if (dependenciesElement != null) {
            NodeList childs = dependenciesElement.getChildNodes();
            for (int i = 0; i < childs.getLength(); i++) {
                Node node = childs.item(i);
                if (node instanceof Element && DEPENDENCY.equals(node.getNodeName())) {
                    dependencies.add(new PomDependencyData((Element) node));
                }
            }
        }
        return dependencies;
    }
    

    public List /* <PomDependencyMgt> */ getDependencyMgt() {
        Element dependenciesElement = getFirstChildElement(projectElement, DEPENDENCY_MGT);
        dependenciesElement = getFirstChildElement(dependenciesElement, DEPENDENCIES);
        LinkedList dependencies = new LinkedList();
        if (dependenciesElement != null) {
            NodeList childs = dependenciesElement.getChildNodes();
            for (int i = 0; i < childs.getLength(); i++) {
                Node node = childs.item(i);
                if (node instanceof Element && DEPENDENCY.equals(node.getNodeName())) {
                    dependencies.add(new PomDependencyMgt((Element) node));
                }
            }
        }
        return dependencies;
    }

    
    public class PomDependencyMgt {
        private final Element depElement;
        
        PomDependencyMgt(Element depElement) {
            this.depElement = depElement; 
        }
        
        public String getGroupId() {
            String val = getFirstChildText(depElement , GROUP_ID);
            return replaceProps(val);
        }

        public String getArtifaceId() {
            String val = getFirstChildText(depElement , ARTIFACT_ID);
            return replaceProps(val);
        }

        public String getVersion() {
            String val = getFirstChildText(depElement , VERSION);
            return replaceProps(val);
        }
        
    }
    
    
    public class PomDependencyData extends PomDependencyMgt {
        private final Element depElement;
        PomDependencyData(Element depElement) {
            super(depElement);
            this.depElement = depElement;
        }
        
        public String getScope() {
            String val = getFirstChildText(depElement , SCOPE);
            if (val == null) {
                return "compile";
            } else {
                return replaceProps(val);
            }
        }
        
        public String getClassifier() {
            String val = getFirstChildText(depElement , CLASSIFIER);
            return replaceProps(val);
        }

        public boolean isOptional() {
            return getFirstChildElement(depElement, OPTIONAL) != null;
        }
        
        public List /*<ModuleId>*/ getExcludedModules() {
            Element exclusionsElement = getFirstChildElement(depElement, EXCLUSIONS);
            LinkedList exclusions = new LinkedList();
            if (exclusionsElement != null) {
                NodeList childs = exclusionsElement.getChildNodes();
                for (int i = 0; i < childs.getLength(); i++) {
                    Node node = childs.item(i);
                    if (node instanceof Element && EXCLUSION.equals(node.getNodeName())) {
                        String groupId = getFirstChildText((Element) node, GROUP_ID);
                        String arteficatId = getFirstChildText((Element) node, ARTIFACT_ID);
                        exclusions.add(ModuleId.newInstance(groupId, arteficatId));
                    }
                }
            }
            return exclusions;            
        }

    }
    
    /**
     * @return the content of the properties tag into the pom.
     */
    public Map/* <String,String> */getPomProperties() {
        Map pomProperties = new HashMap();
        Element propsEl = getFirstChildElement(projectElement, PROPERTIES);
        if (propsEl != null) {
            propsEl.normalize();
        }
        for (Iterator it = getAllChilds(propsEl).iterator(); it.hasNext();) {
            Element prop = (Element) it.next();
            pomProperties.put(prop.getNodeName(), getTextContent(prop));
        }
        return pomProperties;
    }
    
   
    private String replaceProps(String val) {
        if (val == null) {
            return null;
        } else {
            return IvyPatternHelper.substituteVariables(val, properties).trim();
        }
    }

    private static String getTextContent(Element element) {
        StringBuffer result = new StringBuffer();
        
        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node child = childNodes.item(i);
            
            switch (child.getNodeType()) {
                case Node.CDATA_SECTION_NODE:
                case Node.TEXT_NODE:
                    result.append(child.getNodeValue());
                    break;
            }
        }
        
        return result.toString();
    }
    
    private static String getFirstChildText(Element parentElem, String name) {
        Element node = getFirstChildElement(parentElem, name);
        if (node != null) {
            return getTextContent(node);
        } else {
            return null;
        }
    }

    private static Element getFirstChildElement(Element parentElem, String name) {
        if (parentElem == null) {
            return null;
        }
        NodeList childs = parentElem.getChildNodes();
        for (int i = 0; i < childs.getLength(); i++) {
            Node node = childs.item(i);
            if (node instanceof Element && name.equals(node.getNodeName())) {
                return (Element) node;
            }
        }
        return null;
    }
    
    private static List/* <Element> */getAllChilds(Element parent) {
        List r = new LinkedList();
        if (parent != null) {
            NodeList childs = parent.getChildNodes();
            for (int i = 0; i < childs.getLength(); i++) {
                Node node = childs.item(i);
                if (node instanceof Element) {
                    r.add(node);
                }
            }
        }
        return r;
    }





}
