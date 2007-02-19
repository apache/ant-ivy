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
package org.apache.ivy.ant;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.cache.ArtifactOrigin;
import org.apache.ivy.core.cache.CacheManager;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.retrieve.RetrieveOptions;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;


/**
 * Generates a report of all artifacts involved during the last resolve. 
 */
public class IvyArtifactReport extends IvyTask {
    private File _tofile;
    private String _conf;
    private String _pattern;
    private boolean _haltOnFailure = true;
    private File _cache;

    public File getTofile() {
        return _tofile;
    }
    public void setTofile(File tofile) {
        _tofile = tofile;
    }
    public String getConf() {
        return _conf;
    }
    public void setConf(String conf) {
        _conf = conf;
    }
    public String getPattern() {
        return _pattern;
    }
    public void setPattern(String pattern) {
        _pattern = pattern;
    }
    public boolean isHaltonfailure() {
        return _haltOnFailure;
    }
    public void setHaltonfailure(boolean haltOnFailure) {
        _haltOnFailure = haltOnFailure;
    }
    public File getCache() {
        return _cache;
    }
    public void setCache(File cache) {
        _cache = cache;
    }

    public void execute() throws BuildException {
        if (_tofile == null) {
            throw new BuildException("no destination file name: please provide it through parameter 'tofile'");
        }

        Ivy ivy = getIvyInstance();
        IvySettings settings = ivy.getSettings();

        ensureResolved(isHaltonfailure(), false, null, null);

        String organisation = getProperty(null, settings, "ivy.organisation");
        String module = getProperty(null, settings, "ivy.module");
        String revision = getProperty(Ivy.getWorkingRevision(), settings, "ivy.revision");

        if (_cache == null) {
            _cache = settings.getDefaultCache();
        }
        _pattern = getProperty(_pattern, settings, "ivy.retrieve.pattern");
        _conf = getProperty(_conf, settings, "ivy.resolved.configurations");
        if ("*".equals(_conf)) {
            _conf = getProperty(settings, "ivy.resolved.configurations");
            if (_conf == null) {
                throw new BuildException("bad provided for ivy artifact report task: * can only be used with a prior call to <resolve/>");
            }
        }

        if (organisation == null) {
            throw new BuildException("no organisation provided for ivy artifact report task: It can be set via a prior call to <resolve/>");
        }
        if (module == null) {
            throw new BuildException("no module name provided for ivy artifact report task: It can be set via a prior call to <resolve/>");
        }
        if (_conf == null) {
            throw new BuildException("no conf provided for ivy artifact report task: It can either be set explicitely via the attribute 'conf' or via 'ivy.resolved.configurations' property or a prior call to <resolve/>");
        }
        try {
            String[] confs = splitConfs(_conf);
            CacheManager cacheManager = CacheManager.getInstance(settings, _cache);
			IvyNode[] dependencies = ivy.getResolveEngine()
            	.getDependencies((ModuleDescriptor) getProject().getReference("ivy.resolved.descriptor"), 
            			new ResolveOptions()
            				.setConfs(confs)
            				.setCache(cacheManager)
            				.setValidate(doValidate(settings)),
            			null);

            Map artifactsToCopy = ivy.getRetrieveEngine().determineArtifactsToCopy(
            		ModuleRevisionId.newInstance(organisation, module, revision), 
            		_pattern, 
            		new RetrieveOptions()
            			.setConfs(confs)
            			.setCache(cacheManager));
            
            Map moduleRevToArtifactsMap = new HashMap();
            for (Iterator iter = artifactsToCopy.keySet().iterator(); iter.hasNext();) {
                Artifact artifact = (Artifact) iter.next();
                Set moduleRevArtifacts = (Set) moduleRevToArtifactsMap.get(artifact.getModuleRevisionId());
                if (moduleRevArtifacts == null) {
                    moduleRevArtifacts = new HashSet();
                    moduleRevToArtifactsMap.put(artifact.getModuleRevisionId(), moduleRevArtifacts);
                }
                moduleRevArtifacts.add(artifact);
            }

            CacheManager cache = getCacheManager();
            generateXml(cache, dependencies, moduleRevToArtifactsMap, artifactsToCopy);
        } catch (ParseException e) {
            log(e.getMessage(), Project.MSG_ERR);
            throw new BuildException("syntax errors in ivy file: "+e, e);
        } catch (IOException e) {
            throw new BuildException("impossible to generate report: "+e, e);
        }
    }
    private void generateXml(CacheManager cache, IvyNode[] dependencies, Map moduleRevToArtifactsMap, Map artifactsToCopy) {
        try {
            FileOutputStream fileOuputStream = new FileOutputStream(_tofile);
            try {
                TransformerHandler saxHandler = createTransformerHandler(fileOuputStream);
                
                saxHandler.startDocument();
                saxHandler.startElement(null, "modules", "modules", new AttributesImpl());

                for (int i = 0; i < dependencies.length; i++) {
                    IvyNode dependency = dependencies[i];
                    if (dependency.getModuleRevision() == null || dependency.isCompletelyEvicted()) {
                    	continue;
                    }

                    startModule(saxHandler, dependency);

                    Set artifactsOfModuleRev = (Set) moduleRevToArtifactsMap.get(dependency.getModuleRevision().getId());
                    if (artifactsOfModuleRev != null) {
                        for (Iterator iter = artifactsOfModuleRev.iterator(); iter.hasNext();) {
                            Artifact artifact = (Artifact) iter.next();

                            startArtifact(saxHandler, artifact);

                            writeOriginLocationIfPresent(cache, saxHandler, artifact);

                            writeCacheLocation(cache, saxHandler, artifact);

                            Set artifactDestPaths = (Set) artifactsToCopy.get(artifact);
                            for (Iterator iterator = artifactDestPaths.iterator(); iterator.hasNext();) {
                                String artifactDestPath = (String) iterator.next();
                                writeRetrieveLocation(saxHandler, artifactDestPath);
                            }
                            saxHandler.endElement(null, "artifact", "artifact");
                        }
                    }
                    saxHandler.endElement(null, "module", "module");
                }
                saxHandler.endElement(null, "modules", "modules");
                saxHandler.endDocument();
            } finally {
                fileOuputStream.close();
            }
        } catch (SAXException e) {
            throw new BuildException("impossible to generate report", e);
        } catch (TransformerConfigurationException e) {
            throw new BuildException("impossible to generate report", e);
        } catch (IOException e) {
            throw new BuildException("impossible to generate report", e);
        }
    }

    private TransformerHandler createTransformerHandler(FileOutputStream fileOuputStream) throws TransformerFactoryConfigurationError, TransformerConfigurationException, SAXException {
        SAXTransformerFactory transformerFact = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
        TransformerHandler saxHandler = transformerFact.newTransformerHandler();
        saxHandler.getTransformer().setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        saxHandler.getTransformer().setOutputProperty(OutputKeys.INDENT, "yes");
        saxHandler.setResult(new StreamResult(fileOuputStream));
        return saxHandler;
    }

    private void startModule(TransformerHandler saxHandler, IvyNode dependency) throws SAXException {
        AttributesImpl moduleAttrs = new AttributesImpl();
        moduleAttrs.addAttribute(null, "organisation", "organisation", "CDATA", dependency.getModuleId().getOrganisation());
        moduleAttrs.addAttribute(null, "name", "name", "CDATA", dependency.getModuleId().getName());
        moduleAttrs.addAttribute(null, "rev", "rev", "CDATA", dependency.getModuleRevision().getId().getRevision());
        moduleAttrs.addAttribute(null, "status", "status", "CDATA", dependency.getModuleRevision().getDescriptor().getStatus());
        saxHandler.startElement(null, "module", "module", moduleAttrs);
    }

    private void startArtifact(TransformerHandler saxHandler, Artifact artifact) throws SAXException {
        AttributesImpl artifactAttrs = new AttributesImpl();
        artifactAttrs.addAttribute(null, "name", "name", "CDATA", artifact.getName());
        artifactAttrs.addAttribute(null, "ext", "ext", "CDATA", artifact.getExt());
        artifactAttrs.addAttribute(null, "type", "type", "CDATA", artifact.getType());
        saxHandler.startElement(null, "artifact", "artifact", artifactAttrs);
    }

    private void writeOriginLocationIfPresent(CacheManager cache, TransformerHandler saxHandler, Artifact artifact) throws IOException, SAXException {
    	ArtifactOrigin origin = cache.getSavedArtifactOrigin(artifact);
    	if (origin != null) {
    		String originName = origin.getLocation();
    		boolean isOriginLocal = origin.isLocal();

    		String originLocation;
            AttributesImpl originLocationAttrs = new AttributesImpl();
            if (isOriginLocal) {
                originLocationAttrs.addAttribute(null, "is-local", "is-local", "CDATA", "true");
                File originNameFile = new File(originName);
                StringBuffer originNameWithSlashes = new StringBuffer(1000);
                replaceFileSeparatorWithSlash(originNameFile, originNameWithSlashes);
                originLocation = originNameWithSlashes.toString();
            } else {
                originLocationAttrs.addAttribute(null, "is-local", "is-local", "CDATA", "false");
                originLocation = originName;
            }
            saxHandler.startElement(null, "origin-location", "origin-location", originLocationAttrs);
            char[] originLocationAsChars = originLocation.toCharArray();
            saxHandler.characters(originLocationAsChars, 0, originLocationAsChars.length);
            saxHandler.endElement(null, "origin-location", "origin-location");
        }
    }

    private void writeCacheLocation(CacheManager cache, TransformerHandler saxHandler, Artifact artifact) throws SAXException {
    	ArtifactOrigin origin = cache.getSavedArtifactOrigin(artifact);
        File archiveInCacheFile = cache.getArchiveFileInCache(artifact, origin, false);
        StringBuffer archiveInCachePathWithSlashes = new StringBuffer(1000);
        replaceFileSeparatorWithSlash(archiveInCacheFile, archiveInCachePathWithSlashes);

        saxHandler.startElement(null, "cache-location", "cache-location", new AttributesImpl());
        char[] archiveInCachePathAsChars = archiveInCachePathWithSlashes.toString().toCharArray();
        saxHandler.characters(archiveInCachePathAsChars, 0, archiveInCachePathAsChars.length);
        saxHandler.endElement(null, "cache-location", "cache-location");
    }

    private void writeRetrieveLocation(TransformerHandler saxHandler, String artifactDestPath) throws SAXException {
        artifactDestPath = removeLeadingPath(getProject().getBaseDir(), new File(artifactDestPath));
        StringBuffer artifactDestPathWithSlashes = new StringBuffer(1000);
        replaceFileSeparatorWithSlash(new File(artifactDestPath), artifactDestPathWithSlashes);

        saxHandler.startElement(null, "retrieve-location", "retrieve-location", new AttributesImpl());
        char[] artifactDestPathAsChars = artifactDestPathWithSlashes.toString().toCharArray();
        saxHandler.characters(artifactDestPathAsChars, 0, artifactDestPathAsChars.length);
        saxHandler.endElement(null, "retrieve-location", "retrieve-location");
    }
    
    private void replaceFileSeparatorWithSlash(File file, StringBuffer resultPath) {
        if (file.getParentFile() != null) {
            replaceFileSeparatorWithSlash(file.getParentFile(), resultPath);
            resultPath.append('/');
        }

        if (file.getName().equals("")) {
            String fileSeparator = System.getProperty("file.separator");
            String path = file.getPath();
            while (path.endsWith(fileSeparator)) {
                path = path.substring(0, path.length() - fileSeparator.length());
            }
            resultPath.append(path);
        } else {
            resultPath.append(file.getName());
        }
    }
    
    // method largely inspired by ant 1.6.5 FileUtils method
    public String removeLeadingPath(File leading, File path) {
        String l = leading.getAbsolutePath();
        String p = path.getAbsolutePath();
        if (l.equals(p)) {
            return "";
        }

        // ensure that l ends with a /
        // so we never think /foo was a parent directory of /foobar
        if (!l.endsWith(File.separator)) {
            l += File.separator;
        }
        return (p.startsWith(l)) ? p.substring(l.length()) : p;
    }

	protected CacheManager getCacheManager() {
		CacheManager cache = new CacheManager(getSettings(), _cache);
		return cache;
	}

}
