/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.resolver;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import fr.jayasoft.ivy.Artifact;
import fr.jayasoft.ivy.DefaultArtifact;
import fr.jayasoft.ivy.DefaultModuleDescriptor;
import fr.jayasoft.ivy.DefaultModuleRevision;
import fr.jayasoft.ivy.DependencyDescriptor;
import fr.jayasoft.ivy.DependencyResolver;
import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.IvyNode;
import fr.jayasoft.ivy.ModuleDescriptor;
import fr.jayasoft.ivy.ModuleRevisionId;
import fr.jayasoft.ivy.ResolveData;
import fr.jayasoft.ivy.ResolvedModuleRevision;
import fr.jayasoft.ivy.Status;
import fr.jayasoft.ivy.event.EndDownloadEvent;
import fr.jayasoft.ivy.event.StartDownloadEvent;
import fr.jayasoft.ivy.parser.ModuleDescriptorParser;
import fr.jayasoft.ivy.parser.ModuleDescriptorParserRegistry;
import fr.jayasoft.ivy.report.ArtifactDownloadReport;
import fr.jayasoft.ivy.report.DownloadReport;
import fr.jayasoft.ivy.report.DownloadStatus;
import fr.jayasoft.ivy.repository.Resource;
import fr.jayasoft.ivy.repository.ResourceHelper;
import fr.jayasoft.ivy.util.IvyPatternHelper;
import fr.jayasoft.ivy.util.Message;
import fr.jayasoft.ivy.xml.XmlModuleDescriptorParser;
import fr.jayasoft.ivy.xml.XmlModuleDescriptorWriter;

/**
 * @author Xavier Hanin
 *
 */
public abstract class BasicResolver extends AbstractResolver {
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");

    protected String _workspaceName;
    /**
     * True if the files resolved are dependent of the environment from which they have been resolved, false otherwise. In general, relative paths are dependent of the environment, and absolute paths including machine reference are not. 
     */
    private boolean _envDependent = true;

    private List _ivyattempts = new ArrayList();
    private Map _artattempts = new HashMap();

    private Boolean _checkmodified = null;
    
    public BasicResolver() {
        _workspaceName = Ivy.getLocalHostName();
    }

    public String getWorkspaceName() {
        return _workspaceName;
    }

    public void setWorkspaceName(String workspaceName) {
        _workspaceName = workspaceName;
    }

    public boolean isEnvDependent() {
        return _envDependent;
    }

    public void setEnvDependent(boolean envDependent) {
        _envDependent = envDependent;
    }

    /**
     * True if this resolver should check lastmodified date to know if ivy files are up to date.
     * @return
     */
    public boolean isCheckmodified() {
        if (_checkmodified == null) {
            if (getIvy() != null) {
                String check = getIvy().getVariable("ivy.resolver.default.check.modified");
                return check != null ? Boolean.valueOf(check).booleanValue() : false;
            } else {
                return false;
            }
        } else {
            return _checkmodified.booleanValue();
        }
    }
    

    public void setCheckmodified(boolean check) {
        _checkmodified = Boolean.valueOf(check);
    }
    
    public ResolvedModuleRevision getDependency(DependencyDescriptor dd, ResolveData data) throws ParseException {
        clearIvyAttempts();
        boolean downloaded = false;
        boolean searched = false;
        Date cachedPublicationDate = null;
        ModuleRevisionId mrid = dd.getDependencyRevisionId();
    	// check revision
		int index = mrid.getRevision().indexOf("@");
		if (index != -1 && !mrid.getRevision().substring(index+1).equals(_workspaceName)) {
            Message.verbose("\t"+getName()+": unhandled revision => "+mrid.getRevision());
            return null;
		}
        
        if (!mrid.isExactRevision() && !acceptLatest()) {
            Message.error("latest revisions not handled by "+getClass().getName()+". impossible to resolve "+mrid);
            return null;
        }
    	
        // if we do not have to check modified and if the revision is exact and not changing,  
        // we first search for it in cache
        if (mrid.isExactRevision() && !isCheckmodified() && !dd.isChanging()) {
            ResolvedModuleRevision rmr = data.getIvy().findModuleInCache(mrid, data.getCache(), doValidate(data));
            if (rmr != null) {
                Message.verbose("\t"+getName()+": revision in cache: "+mrid);
                return rmr;
            }
        }
        URL cachedIvyURL = null;
        ResolvedResource ivyRef = findIvyFileRef(dd, data);
        searched = true;
        
        // get module descriptor
        ModuleDescriptorParser parser;
        ModuleDescriptor md;
        if (ivyRef == null) {
            parser = XmlModuleDescriptorParser.getInstance();
            md = DefaultModuleDescriptor.newDefaultInstance(mrid, dd.getAllDependencyArtifactsIncludes());
            ResolvedResource artifactRef = findFirstArtifactRef(md, dd, data);
            if (artifactRef == null) {
                Message.verbose("\t"+getName()+": no ivy file nor artifact found for "+mrid);
                logIvyNotFound(mrid);
                String[] conf = md.getConfigurationsNames();
                for (int i = 0; i < conf.length; i++) {
                    Artifact[] artifacts = md.getArtifacts(conf[i]);
                    for (int j = 0; j < artifacts.length; j++) {
                        logArtifactNotFound(artifacts[j]);
                    }
                }
                return null;
            } else {
                Message.verbose("\t"+getName()+": no ivy file found for "+mrid+": using default data");            
                logIvyNotFound(mrid);
    	        if (!mrid.isExactRevision()) {
    	            md.setResolvedModuleRevisionId(new ModuleRevisionId(mrid.getModuleId(), artifactRef.getRevision()));
    	        }
            }
        } else {
            parser = ModuleDescriptorParserRegistry.getInstance().getParser(ivyRef.getResource());
            if (parser == null) {
                Message.warn("no module descriptor parser available for "+ivyRef.getResource());
                return null;
            }
            Message.verbose("\t"+getName()+": found md file for "+mrid);
            Message.verbose("\t\t=> "+ivyRef);
            Message.debug("\tparser = "+parser);

            ModuleRevisionId resolvedMrid = mrid;
            // first check if this dependency has not yet been resolved
            if (!mrid.isExactRevision() && ModuleRevisionId.isExactRevision(ivyRef.getRevision())) {
                resolvedMrid = new ModuleRevisionId(mrid.getModuleId(), ivyRef.getRevision());
                IvyNode node = data.getNode(resolvedMrid);
                if (node != null && node.getModuleRevision() != null) {
                    // this revision has already be resolved : return it
                    Message.verbose("\t"+getName()+": revision already resolved: "+resolvedMrid);
                    return searchedRmr(node.getModuleRevision());
                }
            }
            
            // now let's see if we can find it in cache and if it is up to date
            ResolvedModuleRevision rmr = data.getIvy().findModuleInCache(resolvedMrid, data.getCache(), doValidate(data));
            if (rmr != null) {
                if (!isCheckmodified() && !dd.isChanging()) {
                    Message.verbose("\t"+getName()+": revision in cache: "+resolvedMrid);
                    return searchedRmr(rmr);
                }
                long repLastModified = ivyRef.getLastModified();
                long cacheLastModified = rmr.getDescriptor().getLastModified(); 
                if (!rmr.getDescriptor().isDefault() && repLastModified <= cacheLastModified) {
                    Message.verbose("\t"+getName()+": revision in cache (not updated): "+resolvedMrid);
                    return searchedRmr(rmr);
                } else {
                    Message.verbose("\t"+getName()+": revision in cache is not up to date: "+resolvedMrid);
                    if (dd.isChanging()) {
                        // ivy file has been updated, we should see if it has a new publication date
                        // to see if a new download is required (in case the dependency is a changing one)
                        cachedPublicationDate = rmr.getDescriptor().getResolvedPublicationDate();
                    }
                }
            }
            
            // now download ivy file and parse it
            try {
                // first check if source file is not cache file itself
                if (ResourceHelper.equals(ivyRef.getResource(), 
                        data.getIvy().getIvyFileInCache(data.getCache(), resolvedMrid))) {
                    Message.error("invalid configuration for resolver '"+getName()+"': pointing ivy files to ivy cache is forbidden !");
                    return null;
                }
                
                // temp file is used to prevent downloading twice
                File ivyTempFile = File.createTempFile("ivy", "xml"); 
                ivyTempFile.deleteOnExit();
                Message.debug("\t"+getName()+": downloading "+ivyRef.getResource());
                get(ivyRef.getResource(), ivyTempFile);
                downloaded = true;
                try {
                    cachedIvyURL = ivyTempFile.toURL();
                } catch (MalformedURLException ex) {
                    Message.warn("malformed url exception for temp file: "+ivyTempFile+": "+ex.getMessage());
                    return null;
                }
            } catch (IOException ex) {
                Message.warn("problem while downloading ivy file: "+ivyRef.getResource()+": "+ex.getMessage());
                return null;
            }
            try {
                md = parser.parseDescriptor(data.getIvy(), cachedIvyURL, ivyRef.getResource(), doValidate(data));
                Message.debug("\t"+getName()+": parsed downloaded ivy file for "+mrid+" parsed="+md.getModuleRevisionId());
                
                // check descriptor data is in sync with resource revision and names
                if (!mrid.getOrganisation().equals(md.getModuleRevisionId().getOrganisation())) {
                    throw new IllegalStateException("bad organisation found in "+ivyRef.getResource()+": expected="+mrid.getOrganisation()+" found="+md.getModuleRevisionId().getOrganisation());
                }
                if (!mrid.getName().equals(md.getModuleRevisionId().getName())) {
                    throw new IllegalStateException("bad module name found in "+ivyRef.getResource()+": expected="+mrid.getName()+" found="+md.getModuleRevisionId().getName());
                }
                if (ivyRef.getRevision() != null && md.getModuleRevisionId().getRevision() != null && 
                        !ModuleRevisionId.acceptRevision(ivyRef.getRevision(), md.getModuleRevisionId().getRevision())) {
                    throw new IllegalStateException("bad revision found in "+ivyRef.getResource()+": expected="+ivyRef.getRevision()+" found="+md.getModuleRevisionId().getRevision());
                }
                
                // check if we should delete old artifacts
                boolean deleteOldArtifacts = false;
                if (cachedPublicationDate != null && !cachedPublicationDate.equals(md.getResolvedPublicationDate())) {
                    // artifacts have changed, they should be downloaded again
                    Message.verbose(dd+" has changed: deleting old artifacts");
                    deleteOldArtifacts = true;
                }
                if (deleteOldArtifacts) {
                    String[] confs = rmr.getDescriptor().getConfigurationsNames();
                    for (int i = 0; i < confs.length; i++) {
                        Artifact[] arts = rmr.getDescriptor().getArtifacts(confs[i]);
                        for (int j = 0; j < arts.length; j++) {
                            File artFile = data.getIvy().getArchiveFileInCache(data.getCache(), arts[j]);
                            if (artFile.exists()) {
                                Message.debug("deleting "+artFile);
                                artFile.delete();
                            }
                        }
                    }
                } else if (dd.isChanging()){
                    Message.verbose(dd+" is changing, but has not changed: will trust cached artifacts if any");
                } 
            } catch (IOException ex) {
                Message.warn("io problem while parsing ivy file: "+ivyRef.getResource()+": "+ex.getMessage());
                return null;
            }
        }
        
        // check module descriptor revision
        if (mrid.getRevision().startsWith("latest.")) {
            String askedStatus = mrid.getRevision().substring("latest.".length());
            if (Status.getPriority(askedStatus) < Status.getPriority(md.getStatus())) {
                Message.info("\t"+getName()+": unacceptable status => was="+md.getStatus()+" required="+askedStatus);
                return null;
            }
        } else if (!mrid.acceptRevision(md.getModuleRevisionId().getRevision())) {
            Message.info("\t"+getName()+": unacceptable revision => was="+md.getModuleRevisionId().getRevision()+" required="+mrid.getRevision());
            return null;
        }
        
        // resolve revision
        ModuleRevisionId resolvedMrid = mrid;
        if (!resolvedMrid.isExactRevision()) {
            resolvedMrid = md.getResolvedModuleRevisionId();
            if (resolvedMrid.getRevision() == null || resolvedMrid.getRevision().length() == 0) {
                if (ivyRef.getRevision() == null || ivyRef.getRevision().length() == 0 || !ModuleRevisionId.isExactRevision(ivyRef.getRevision())) {
                    resolvedMrid = new ModuleRevisionId(resolvedMrid.getModuleId(), (_envDependent?"##":"")+DATE_FORMAT.format(data.getDate())+"@"+_workspaceName);
                } else {
                    resolvedMrid = new ModuleRevisionId(resolvedMrid.getModuleId(), ivyRef.getRevision());
                }
            }
            Message.verbose("\t\t["+resolvedMrid.getRevision()+"] "+mrid.getModuleId());
        }
        md.setResolvedModuleRevisionId(resolvedMrid);
        
        // resolve and check publication date
        if (data.getDate() != null) {
            long pubDate = getPublicationDate(md, dd, data);
            if (pubDate > data.getDate().getTime()) {
                Message.info("\t"+getName()+": unacceptable publication date => was="+new Date(pubDate)+" required="+data.getDate());
                return null;
            } else if (pubDate == -1) {
                Message.info("\t"+getName()+": impossible to guess publication date: artifact missing for "+mrid);
                return null;
            }
            md.setResolvedPublicationDate(new Date(pubDate));
        }
    
        try {
            File ivyFile = data.getIvy().getIvyFileInCache(data.getCache(), md.getResolvedModuleRevisionId());
	        if (ivyRef == null) {
                // a basic ivy file is written containing default data
	            XmlModuleDescriptorWriter.write(md, ivyFile);
	        } else {
	            // copy and update ivy file from source to cache
                parser.toIvyFile(cachedIvyURL, ivyRef.getResource(), ivyFile, md);
                long repLastModified = ivyRef.getLastModified();
                if (repLastModified > 0) {
                    ivyFile.setLastModified(repLastModified);
                }
	        }
        } catch (Exception e) {
            Message.warn("impossible to copy ivy file to cache : "+ivyRef.getResource());
        }
        
        data.getIvy().saveResolver(data.getCache(), md, getName());
        return new DefaultModuleRevision(this, md, searched, downloaded);
    }

    protected void clearIvyAttempts() {
        _ivyattempts.clear();
        clearArtifactAttempts();
    }

    protected ResolvedModuleRevision searchedRmr(final ResolvedModuleRevision rmr) {
        // delegate all to previously found except isSearched
        return new ResolvedModuleRevision() {                    
            public boolean isSearched() {
                return true;
            }
        
            public boolean isDownloaded() {
                return rmr.isDownloaded();
            }
        
            public ModuleDescriptor getDescriptor() {
                return rmr.getDescriptor();
            }
        
            public Date getPublicationDate() {
                return rmr.getPublicationDate();
            }
        
            public ModuleRevisionId getId() {
                return rmr.getId();
            }
        
            public DependencyResolver getResolver() {
                return rmr.getResolver();
            }                    
        };
    }
    
    protected void logIvyAttempt(String attempt) {
        _ivyattempts.add(attempt);
        Message.verbose("\t\ttried "+attempt);
    }
    
    protected void logArtifactAttempt(Artifact art, String attempt) {
        List attempts = (List)_artattempts.get(art);
        if (attempts == null) {
            attempts = new ArrayList();
            _artattempts.put(art, attempts);
        }
        attempts.add(attempt);
        Message.verbose("\t\ttried "+attempt);
    }
    
    public void reportFailure() {
        for (ListIterator iter = _ivyattempts.listIterator(); iter.hasNext();) {
            String m = (String)iter.next();
            Message.warn("\t\t"+getName()+": tried "+m);
        }
        for (Iterator iter = _artattempts.keySet().iterator(); iter.hasNext();) {
            Artifact art = (Artifact)iter.next();
            List attempts = (List)_artattempts.get(art);
            if (attempts != null) {
                Message.warn("\t\t"+getName()+": tried artifact "+art+":");
                for (ListIterator iterator = attempts.listIterator(); iterator.hasNext();) {
                    String m = (String)iterator.next();
                    Message.warn("\t\t\t"+m);
                }
            }
        }
    }

    public void reportFailure(Artifact art) {
        List attempts = (List)_artattempts.get(art);
        if (attempts != null) {
            for (ListIterator iter = attempts.listIterator(); iter.hasNext();) {
                String m = (String)iter.next();
                Message.warn("\t\t"+getName()+": tried "+m);
            }
        }
    }

    protected boolean acceptLatest() {
        return true;
    }

    public DownloadReport download(Artifact[] artifacts, Ivy ivy, File cache) {
        clearArtifactAttempts();
        DownloadReport dr = new DownloadReport();
        for (int i = 0; i < artifacts.length; i++) {
        	final ArtifactDownloadReport adr = new ArtifactDownloadReport(artifacts[i]);
        	dr.addArtifactReport(adr);
            ivy.fireIvyEvent(new StartDownloadEvent(this, artifacts[i]));
        	File archiveFile = ivy.getArchiveFileInCache(cache, artifacts[i]);
        	if (archiveFile.exists()) {
        		Message.verbose("\t[NOT REQUIRED] "+artifacts[i]);
        		adr.setDownloadStatus(DownloadStatus.NO);  
                adr.setSize(archiveFile.length());
        	} else {
                ResolvedResource artifactRef = findArtifactRef(artifacts[i], null);
        		if (artifactRef != null) {
                    if (ResourceHelper.equals(artifactRef.getResource(), 
                            archiveFile)) {
                        Message.error("invalid configuration for resolver '"+getName()+"': pointing artifacts to ivy cache is forbidden !");
                        return null;
                    }
    			    long start = System.currentTimeMillis();
        			try {
        			    Message.info("downloading "+artifactRef.getResource()+" ...");
                        File tmp = ivy.getArchiveFileInCache(cache, 
                                new DefaultArtifact(
                                        artifacts[i].getModuleRevisionId(), 
                                        artifacts[i].getPublicationDate(), 
                                        artifacts[i].getName(), 
                                        artifacts[i].getType(), 
                                        artifacts[i].getExt()+".part"));
                        adr.setSize(get(artifactRef.getResource(), tmp));
                        tmp.renameTo(archiveFile);
                		Message.info("\t[SUCCESSFUL ] "+artifacts[i]+" ("+(System.currentTimeMillis()-start)+"ms)");
        				adr.setDownloadStatus(DownloadStatus.SUCCESSFUL);
        			} catch (Exception ex) {
                		Message.warn("\t[FAILED     ] "+artifacts[i]+" ("+(System.currentTimeMillis()-start)+"ms)");
        				adr.setDownloadStatus(DownloadStatus.FAILED);
        			}
        		} else {
        		    logArtifactNotFound(artifacts[i]);
        			adr.setDownloadStatus(DownloadStatus.FAILED);                
        		}
        	}
            ivy.fireIvyEvent(new EndDownloadEvent(this, artifacts[i], adr));
        }
    	return dr;
    }

    protected void clearArtifactAttempts() {
        _artattempts.clear();
    }
    
    public boolean exists(Artifact artifact) {
        ResolvedResource artifactRef = findArtifactRef(artifact, null);
        if (artifactRef != null) {
            return artifactRef.getResource().exists();
        }
        return false;
    }

    protected long getPublicationDate(ModuleDescriptor md, DependencyDescriptor dd, ResolveData data) {
        if (md.getPublicationDate() != null) {
            return md.getPublicationDate().getTime();
        }
        ResolvedResource artifactRef = findFirstArtifactRef(md, dd, data);
        if (artifactRef != null) {
            return artifactRef.getLastModified();
        }
        return -1;
    }

    public String toString() {
        return getName();
    }

    public OrganisationEntry[] listOrganisations() {
        Collection names = findNames(Collections.EMPTY_MAP, IvyPatternHelper.ORGANISATION_KEY);
        OrganisationEntry[] ret = new OrganisationEntry[names.size()];
        int i =0;
        for (Iterator iter = names.iterator(); iter.hasNext(); i++) {
            String org = (String)iter.next();
            ret[i] = new OrganisationEntry(this, org);
        }
        return ret;
    }

    public ModuleEntry[] listModules(OrganisationEntry org) {
        Map tokenValues = new HashMap();
        tokenValues.put(IvyPatternHelper.ORGANISATION_KEY, org.getOrganisation());
        Collection names = findNames(tokenValues, IvyPatternHelper.MODULE_KEY);
        ModuleEntry[] ret = new ModuleEntry[names.size()];
        int i =0;
        for (Iterator iter = names.iterator(); iter.hasNext(); i++) {
            String name = (String)iter.next();
            ret[i] = new ModuleEntry(org, name);
        }
        return ret;
    }

    public RevisionEntry[] listRevisions(ModuleEntry mod) {
        Map tokenValues = new HashMap();
        tokenValues.put(IvyPatternHelper.ORGANISATION_KEY, mod.getOrganisation());
        tokenValues.put(IvyPatternHelper.MODULE_KEY, mod.getModule());
        Collection names = findNames(tokenValues, IvyPatternHelper.REVISION_KEY);
        RevisionEntry[] ret = new RevisionEntry[names.size()];
        int i =0;
        for (Iterator iter = names.iterator(); iter.hasNext(); i++) {
            String name = (String)iter.next();
            ret[i] = new RevisionEntry(mod, name);
        }
        return ret;
    }

    protected abstract Collection findNames(Map tokenValues, String token);

    protected abstract ResolvedResource findIvyFileRef(DependencyDescriptor dd, ResolveData data);

    protected ResolvedResource findFirstArtifactRef(ModuleDescriptor md, DependencyDescriptor dd, ResolveData data) {
        ResolvedResource ret = null;
        String[] conf = md.getConfigurationsNames();
        for (int i = 0; i < conf.length; i++) {
            Artifact[] artifacts = md.getArtifacts(conf[i]);
            for (int j = 0; j < artifacts.length; j++) {
                ret = findArtifactRef(artifacts[j], data.getDate());
                if (ret != null) {
                    return ret;
                }
            }
        }
        return null;
    }


    protected abstract ResolvedResource findArtifactRef(Artifact artifact, Date date);

    protected abstract long get(Resource resource, File ivyTempFile) throws IOException;

    protected abstract void logIvyNotFound(ModuleRevisionId mrid);    

    protected abstract void logArtifactNotFound(Artifact artifact);


}
