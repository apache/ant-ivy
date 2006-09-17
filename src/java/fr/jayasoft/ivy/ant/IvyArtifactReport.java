/*
 * This file is subject to the license found in LICENCE.TXT in the root
 * directory of the project. #SNAPSHOT#
 */
package fr.jayasoft.ivy.ant;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import fr.jayasoft.ivy.Artifact;
import fr.jayasoft.ivy.ArtifactOrigin;
import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.IvyNode;
import fr.jayasoft.ivy.ModuleDescriptor;
import fr.jayasoft.ivy.ModuleId;

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

        ensureResolved(isHaltonfailure(), false, null, null);

        String _organisation = getProperty(null, ivy, "ivy.organisation");
        String _module = getProperty(null, ivy, "ivy.module");

        if (_cache == null) {
            _cache = ivy.getDefaultCache();
        }
        _pattern = getProperty(_pattern, ivy, "ivy.retrieve.pattern");
        _conf = getProperty(_conf, ivy, "ivy.resolved.configurations");
        if ("*".equals(_conf)) {
            _conf = getProperty(ivy, "ivy.resolved.configurations");
            if (_conf == null) {
                throw new BuildException("bad provided for ivy artifact report task: * can only be used with a prior call to <resolve/>");
            }
        }

        if (_organisation == null) {
            throw new BuildException("no organisation provided for ivy artifact report task: It can be set via a prior call to <resolve/>");
        }
        if (_module == null) {
            throw new BuildException("no module name provided for ivy artifact report task: It can be set via a prior call to <resolve/>");
        }
        if (_conf == null) {
            throw new BuildException("no conf provided for ivy artifact report task: It can either be set explicitely via the attribute 'conf' or via 'ivy.resolved.configurations' property or a prior call to <resolve/>");
        }
        try {
            String[] confs = splitConfs(_conf);
            IvyNode[] dependencies = ivy.getDependencies((ModuleDescriptor) getProject().getReference("ivy.resolved.descriptor"), confs, _cache, new Date(), null, doValidate(ivy));

            Map artifactsToCopy = ivy.determineArtifactsToCopy(new ModuleId(_organisation, _module), confs, _cache, _pattern, null);
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

            generateXml(ivy, dependencies, moduleRevToArtifactsMap, artifactsToCopy);
        } catch (ParseException e) {
            log(e.getMessage(), Project.MSG_ERR);
            throw new BuildException("syntax errors in ivy file: "+e, e);
        } catch (IOException e) {
            throw new BuildException("impossible to generate report: "+e, e);
        }
    }

    private void generateXml(Ivy ivy, IvyNode[] dependencies, Map moduleRevToArtifactsMap, Map artifactsToCopy) {
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

                            writeOriginLocationIfPresent(ivy, saxHandler, artifact);

                            writeCacheLocation(ivy, saxHandler, artifact);

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

    private void writeOriginLocationIfPresent(Ivy ivy, TransformerHandler saxHandler, Artifact artifact) throws IOException, SAXException {
    	ArtifactOrigin origin = ivy.getSavedArtifactOrigin(_cache, artifact);
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

    private void writeCacheLocation(Ivy ivy, TransformerHandler saxHandler, Artifact artifact) throws SAXException {
    	ArtifactOrigin origin = ivy.getSavedArtifactOrigin(_cache, artifact);
        File archiveInCacheFile = ivy.getArchiveFileInCache(_cache, artifact, origin, false);
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

}
