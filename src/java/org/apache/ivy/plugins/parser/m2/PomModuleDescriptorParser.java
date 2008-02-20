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
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolveEngine;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.plugins.parser.ModuleDescriptorParser;
import org.apache.ivy.plugins.parser.ParserSettings;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorWriter;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.plugins.repository.url.URLResource;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.util.Message;
import org.xml.sax.SAXException;

/**
 * A parser for Maven 2 POM.
 * <p>
 * The configurations used in the generated module descriptor mimics the behavior defined by maven 2
 * scopes, as documented here:<br/>
 * http://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html
 * The PomModuleDescriptorParser use a PomDomReader to read the pom, and the 
 * PomModuleDescriptorBuilder to write the ivy module descriptor using the info read by the 
 * PomDomReader.  
 */
public final class PomModuleDescriptorParser implements ModuleDescriptorParser {


    private static final PomModuleDescriptorParser INSTANCE = new PomModuleDescriptorParser();

    public static PomModuleDescriptorParser getInstance() {
        return INSTANCE;
    }

    private PomModuleDescriptorParser() {
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

    public ModuleDescriptor parseDescriptor(ParserSettings ivySettings, URL descriptorURL, 
            boolean validate) throws ParseException, IOException {        
        URLResource resource = new URLResource(descriptorURL);
        return parseDescriptor(ivySettings, descriptorURL, resource, validate);
    }

    public ModuleDescriptor parseDescriptor(ParserSettings ivySettings, URL descriptorURL, 
            Resource res, boolean validate) throws ParseException, IOException {
        
        PomModuleDescriptorBuilder mdBuilder = new PomModuleDescriptorBuilder(this, res);
        
        try {           
            PomReader domReader = new PomReader(descriptorURL, res);
            
            domReader.setProperty("parent.version", domReader.getParentVersion());
            
            String groupId = domReader.getGroupId();
            String artifactId = domReader.getArtifactId();
            String version = domReader.getVersion();
            mdBuilder.setModuleRevId(groupId , artifactId , version);

            ModuleRevisionId relocation = domReader.getRelocation();
            
            if (relocation != null) {
                if (groupId != null && artifactId != null
                        && artifactId.equals(relocation.getName())
                        && groupId.equals(relocation.getOrganisation())) {
                    Message.error("Relocation to an other version number not supported in ivy : "
                            + mdBuilder.getModuleDescriptor().getModuleRevisionId()
                            + " relocated to " + relocation
                            + ". Please update your dependency to directly use the right version.");
                    Message.warn("Resolution will only pick dependencies of the relocated element."
                            + "  Artefact and other metadata will be ignored.");
                    ResolvedModuleRevision relocatedModule = parseOtherPom(ivySettings, relocation);
                    DependencyDescriptor[] dds = relocatedModule.getDescriptor().getDependencies();
                    for (int i = 0; i < dds.length; i++) {
                        mdBuilder.addDependency(dds[i]);
                    }
                } else {
                    Message.info(mdBuilder.getModuleDescriptor().getModuleRevisionId()
                            + " is relocated to " + relocation
                            + ". Please update your dependencies.");
                    Message.verbose("Relocated module will be considered as a dependency");
                    DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(mdBuilder
                            .getModuleDescriptor(), relocation, true, false, true);
                    /* Map all public dependencies */
                    dd.addDependencyConfiguration("compile", "compile");
                    dd.addDependencyConfiguration("runtime", "runtime");
                    dd.addDependencyConfiguration("default", "default");
                    dd.addDependencyConfiguration("master", "master");
                    dd.addDependencyConfiguration("provided", "provided");
                    dd.addDependencyConfiguration("system", "system");
                    mdBuilder.addDependency(dd);
                }
            } else {                            
                domReader.setProperty("project.groupId", groupId);
                domReader.setProperty("pom.groupId", groupId);
                domReader.setProperty("project.artifactId", artifactId);
                domReader.setProperty("pom.artifactId", artifactId);
                domReader.setProperty("project.version", version);
                domReader.setProperty("pom.version", version);
                domReader.setProperty("version", version);

                ModuleDescriptor parentDescr = null;
                if (domReader.hasParent()) {
                    domReader.setProperty("parent.version", domReader.getParentVersion());
                    //Is there any other parent properties?
                    
                    ModuleRevisionId parentModRevID = ModuleRevisionId.newInstance(
                        domReader.getParentGroupId(), 
                        domReader.getParentArtifactId(), 
                        domReader.getParentVersion());
                    ResolvedModuleRevision parentModule = parseOtherPom(ivySettings, 
                        parentModRevID);
                    parentDescr = parentModule.getDescriptor();
                }
                                
                Map pomProperties = domReader.getPomProperties();
                for (Iterator iter = pomProperties.entrySet().iterator(); iter.hasNext();) {
                    Map.Entry prop = (Map.Entry) iter.next();
                    domReader.setProperty((String) prop.getKey(), (String) prop.getValue());
                    mdBuilder.addProperty((String) prop.getKey(), (String) prop.getValue());
                }
                
                if (parentDescr != null) {
                    Map parentPomProps = mdBuilder.extractPomProperties(parentDescr.getExtraInfo());
                    for (Iterator iter = parentPomProps.entrySet().iterator(); iter.hasNext();) {
                        Map.Entry prop = (Map.Entry) iter.next();
                        domReader.setProperty((String) prop.getKey(), (String) prop.getValue());
                    }                    
                }
                
                for (Iterator it = domReader.getDependencyMgt().iterator(); it.hasNext();) {
                    PomReader.PomDependencyMgt dep = (PomReader.PomDependencyMgt) it.next();
                    mdBuilder.addDependencyMgt(dep);
                }
                
                if (parentDescr != null) {
                    mdBuilder.addExtraInfos(parentDescr.getExtraInfo());
                }

                for (Iterator it = domReader.getDependencies().iterator(); it.hasNext();) {
                    PomReader.PomDependencyData dep = (PomReader.PomDependencyData) it.next();
                    mdBuilder.addDependency(res, dep);
                }

                if (parentDescr != null) {
                    for (int i = 0; i < parentDescr.getDependencies().length; i++) {
                        mdBuilder.addDependency(parentDescr.getDependencies()[i]);
                    }
                }
                
                mdBuilder.addArtifact(artifactId , domReader.getPackaging());
            }            
        } catch (SAXException e) {
            throw newParserException(e);
        }
        
        return mdBuilder.getModuleDescriptor();
    }

    private ResolvedModuleRevision parseOtherPom(ParserSettings ivySettings,
            ModuleRevisionId parentModRevID) throws ParseException {
        DependencyDescriptor dd = new DefaultDependencyDescriptor(parentModRevID, true);
        ResolveData data = IvyContext.getContext().getResolveData();
        if (data == null) {
            ResolveEngine engine = IvyContext.getContext().getIvy().getResolveEngine();
            ResolveOptions options = new ResolveOptions();
            options.setDownload(false);
            data = new ResolveData(engine, options);
        }
        
        DependencyResolver resolver = ivySettings.getResolver(parentModRevID);
        if (resolver == null) {
            // TODO: Throw exception here?
            return null;
        } else {
            ResolvedModuleRevision otherModule = resolver.getDependency(dd, data);
            return otherModule;
        }
    }

    private ParseException newParserException(Exception e) {
        Message.error(e.getMessage());
        ParseException pe = new ParseException(e.getMessage() , 0);
        pe.initCause(e);
        return pe;
    }

}
