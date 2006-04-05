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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import fr.jayasoft.ivy.Artifact;
import fr.jayasoft.ivy.DefaultArtifact;
import fr.jayasoft.ivy.DefaultModuleDescriptor;
import fr.jayasoft.ivy.DefaultModuleRevision;
import fr.jayasoft.ivy.DependencyDescriptor;
import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.IvyNode;
import fr.jayasoft.ivy.LatestStrategy;
import fr.jayasoft.ivy.ModuleDescriptor;
import fr.jayasoft.ivy.ModuleRevisionId;
import fr.jayasoft.ivy.ResolveData;
import fr.jayasoft.ivy.ResolvedModuleRevision;
import fr.jayasoft.ivy.ResolvedURL;
import fr.jayasoft.ivy.Status;
import fr.jayasoft.ivy.parser.ModuleDescriptorParserRegistry;
import fr.jayasoft.ivy.report.ArtifactDownloadReport;
import fr.jayasoft.ivy.report.DownloadReport;
import fr.jayasoft.ivy.report.DownloadStatus;
import fr.jayasoft.ivy.repository.url.URLResource;
import fr.jayasoft.ivy.util.CopyProgressEvent;
import fr.jayasoft.ivy.util.CopyProgressListener;
import fr.jayasoft.ivy.util.FileUtil;
import fr.jayasoft.ivy.util.IvyPattern;
import fr.jayasoft.ivy.util.IvyPatternHelper;
import fr.jayasoft.ivy.util.Message;
import fr.jayasoft.ivy.xml.XmlModuleDescriptorUpdater;
import fr.jayasoft.ivy.xml.XmlModuleDescriptorWriter;

/**
 * @deprecated use RepositoryResolver instead
 * @author Xavier Hanin
 *
 */
public abstract class AbstractURLResolver extends AbstractResolver {
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");

    protected String _workspaceName;
    /**
     * True if the files resolved are dependent of the environment from which they have been resolved, false otherwise. In general, relative paths are dependent of the environment, and absolute paths including machine reference are not. 
     */
    private boolean _envDependent = true;

    /**
     * The latest strategy to use to find latest among several artifacts
     */
    private LatestStrategy _latestStrategy;

    private String _latestStrategyName;

    private List _ivyattempts = new ArrayList();
    
    
    public AbstractURLResolver() {
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

    public LatestStrategy getLatestStrategy() {        
        if (_latestStrategy == null) {
            if (_latestStrategyName != null) {
                _latestStrategy = getIvy().getLatestStrategy(_latestStrategyName);
                if (_latestStrategy == null) {
                    Message.error("unknown latest strategy: "+_latestStrategyName);
                    _latestStrategy = getIvy().getDefaultLatestStrategy();
                }
            } else {
                _latestStrategy = getIvy().getDefaultLatestStrategy();
                Message.debug(getName()+": no latest strategy defined: using default");
            }
        }
        return _latestStrategy;
    }
    

    public void setLatestStrategy(LatestStrategy latestStrategy) {
        _latestStrategy = latestStrategy;
    }    

    public void setLatest(String strategyName) {
        _latestStrategyName = strategyName;
    }    

    public ResolvedModuleRevision getDependency(DependencyDescriptor dd, ResolveData data) throws ParseException {
        _ivyattempts.clear();
        boolean downloaded = false;
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
    	
        // look for ivy file
    	ResolvedURL ivyURL = getIvyURL(mrid, data.getDate());
    	URL cachedIvyURL = null;
        // get module descriptor
        ModuleDescriptor md;
        if (ivyURL == null) {
            md = DefaultModuleDescriptor.newDefaultInstance(mrid, dd.getAllDependencyArtifactsIncludes());
            ResolvedURL artifactURL = findFirstArtifact(md, mrid, data.getDate());
            if (artifactURL == null) {
                Message.verbose("\t"+getName()+": no ivy file nor artifact found for "+mrid);
                logIvyNotFound(mrid);
                return null;
            } else {
                Message.verbose("\t"+getName()+": no ivy file found for "+mrid+": using default data");            
                logIvyNotFound(mrid);
    	        if (!mrid.isExactRevision()) {
    	            md.setResolvedModuleRevisionId(new ModuleRevisionId(mrid.getModuleId(), artifactURL.getRevision()));
    	        }
            }
        } else {
            Message.verbose("\t"+getName()+": found ivy file for "+mrid);
            Message.verbose("\t\t=> "+ivyURL);
            // first check if this dependency has not yet been resolved
            if (!mrid.isExactRevision()) {
                ModuleRevisionId resolvedMrid = new ModuleRevisionId(mrid.getModuleId(), ivyURL.getRevision());
                IvyNode node = data.getNode(resolvedMrid);
                if (node != null) {
                    // this revision has already be resolved : return it
                    Message.verbose("\t"+getName()+": revision already resolved: "+resolvedMrid);
                    return node.getModuleRevision();
                }
                
                // now let's see if we can find it in cache
                ResolvedModuleRevision rmr = data.getIvy().findModuleInCache(resolvedMrid, data.getCache(), doValidate(data));
                if (rmr != null) {
                    Message.verbose("\t"+getName()+": revision in cache: "+resolvedMrid);
                    return rmr;
                }

            }
            
            
            // now download ivy file and parse it  
            cachedIvyURL = ivyURL.getURL(); // default value: the source itself
            try {
                // temp file is used to prevent downloading twice
                File ivyTempFile = File.createTempFile("ivy", "xml"); 
                ivyTempFile.deleteOnExit();
                Message.debug("\t"+getName()+": downloading "+ivyURL.getURL());
                FileUtil.copy(ivyURL.getURL(), ivyTempFile, null);
                downloaded = true;
                try {
                    cachedIvyURL = ivyTempFile.toURL();
                } catch (MalformedURLException ex) {
                    // ignored, will not used temp file
                    Message.warn("malformed url exception for temp file: "+ivyTempFile+": "+ex.getMessage());
                }
            } catch (IOException ex) {
                // ignored, will not used temp file
                Message.warn("problem while downloading ivy file: "+ivyURL+": "+ex.getMessage());
            }
            try {
                md = ModuleDescriptorParserRegistry.getInstance().parseDescriptor(data.getIvy(), cachedIvyURL, new URLResource(ivyURL.getURL()), doValidate(data));
                Message.debug("\t"+getName()+": parsed downloaded ivy file for "+mrid+" parsed="+md.getModuleRevisionId());
            } catch (IOException ex) {
                Message.warn("io problem while parsing ivy file: "+ivyURL+": "+ex.getMessage());
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
                if (ivyURL.getRevision() == null || ivyURL.getRevision().length() == 0) {
                    resolvedMrid = new ModuleRevisionId(resolvedMrid.getModuleId(), (_envDependent?"##":"")+DATE_FORMAT.format(data.getDate())+"@"+_workspaceName);
                } else {
                    resolvedMrid = new ModuleRevisionId(resolvedMrid.getModuleId(), ivyURL.getRevision());
                }
            }
            Message.verbose("\t\t["+resolvedMrid.getRevision()+"] "+mrid.getModuleId());
        }
        md.setResolvedModuleRevisionId(resolvedMrid);
        
        // resolve and check publication date
        if (data.getDate() != null) {
            long pubDate = getPublicationDate(md, resolvedMrid, data.getDate());
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
	        if (ivyURL == null) {
                // a basic ivy file is written containing default data
	            XmlModuleDescriptorWriter.write(md, data.getIvy().getIvyFileInCache(data.getCache(), md.getResolvedModuleRevisionId()));
	        } else {
	            // copy and update ivy file from source to cache
                XmlModuleDescriptorUpdater.update(
                        cachedIvyURL != null?cachedIvyURL:ivyURL.getURL(), 
                        data.getIvy().getIvyFileInCache(data.getCache(), md.getResolvedModuleRevisionId()), 
                        Collections.EMPTY_MAP, 
                        md.getStatus(), 
                        md.getResolvedModuleRevisionId().getRevision(), 
                        md.getResolvedPublicationDate());
	        }
        } catch (Exception e) {
            Message.warn("impossible to copy ivy file to cache : "+ivyURL.getURL());
        }
        
        return new DefaultModuleRevision(this, this, md, true, downloaded);
    }
    
    public void reportFailure() {
        for (ListIterator iter = _ivyattempts.listIterator(); iter.hasNext();) {
            String m = (String)iter.next();
            Message.warn("\t\t"+getName()+": tried "+m);
        }
    }

    protected boolean acceptLatest() {
        return true;
    }

    public DownloadReport download(Artifact[] artifacts, Ivy ivy, File cache) {
        DownloadReport dr = new DownloadReport();
        for (int i = 0; i < artifacts.length; i++) {
        	final ArtifactDownloadReport adr = new ArtifactDownloadReport(artifacts[i]);
        	dr.addArtifactReport(adr);
        	File archiveFile = ivy.getArchiveFileInCache(cache, artifacts[i]);
        	if (archiveFile.exists()) {
        		Message.verbose("\t[NOT REQUIRED] "+artifacts[i]);
        		adr.setDownloadStatus(DownloadStatus.NO);  
                adr.setSize(archiveFile.length());
        	} else {
        		ResolvedURL artifactURL = getArtifactURL(artifacts[i]);
        		if (artifactURL != null) {
    			    long start = System.currentTimeMillis();
        			try {
        			    Message.info("downloading "+artifactURL+" ...");
        				FileUtil.copy(artifactURL.getURL(), archiveFile, new CopyProgressListener() {
                            public void start(CopyProgressEvent evt) {
                            }
                            public void progress(CopyProgressEvent evt) {
                                Message.progress();
                            }
                            public void end(CopyProgressEvent evt) {
                                Message.endProgress(" ("+(evt.getTotalReadBytes() / 1024)+"kB)");
                                adr.setSize(evt.getTotalReadBytes());
                            }
                        });
                		Message.info("\t[SUCCESSFUL ] "+artifacts[i]+" ("+(System.currentTimeMillis()-start)+"ms)");
        				adr.setDownloadStatus(DownloadStatus.SUCCESSFUL);
        			} catch (Exception ex) {
                		Message.warn("\t[FAILED     ] "+artifacts[i]+" ("+(System.currentTimeMillis()-start)+"ms)");
        				adr.setDownloadStatus(DownloadStatus.FAILED);
        			}
        		} else {
            		Message.warn("\t[NOT FOUND  ] "+artifacts[i]);
        		    logNotFound(artifacts[i]);
        			adr.setDownloadStatus(DownloadStatus.FAILED);                
        		}
        	}
        }
    	return dr;
    }

    protected long getPublicationDate(ModuleDescriptor md, ModuleRevisionId mrid, Date date) {
        if (md.getPublicationDate() != null) {
            return md.getPublicationDate().getTime();
        }
        ResolvedURL artifactURL = findFirstArtifact(md, mrid, date);
        if (artifactURL != null) {
            try {
                return artifactURL.getURL().openConnection().getLastModified();
            } catch (IOException e) {
                return 0;
            }
        }
        return -1;
    }

    public String toString() {
        return getName();
    }

	private List _ivyPatterns = new ArrayList(); // List (String pattern)
	private List _artifactPatterns = new ArrayList();  // List (String pattern)

	/**
	 * example of pattern : ~/Workspace/[module]/[module].ivy.xml
	 * @param pattern
	 */
	public void addIvyPattern(String pattern) {
		_ivyPatterns.add(pattern);
	}

	public void addArtifactPattern(String pattern) {
		_artifactPatterns.add(pattern);
	}
    
    public List getIvyPatterns() {
        return Collections.unmodifiableList(_ivyPatterns);
    }

    public List getArtifactPatterns() {
        return Collections.unmodifiableList(_artifactPatterns);
    }
    protected void setIvyPatterns(List ivyPatterns) {
        _ivyPatterns = ivyPatterns;
    }
    protected void setArtifactPatterns(List artifactPatterns) {
        _artifactPatterns = artifactPatterns;
    }

	/*
	 * Methods respecting ivy conf method specifications
	 */
	public void addConfiguredIvy(IvyPattern p) {
		_ivyPatterns.add(p.getPattern());
	}

	public void addConfiguredArtifact(IvyPattern p) {
		_artifactPatterns.add(p.getPattern());
	}

	protected ResolvedURL getIvyURL(ModuleRevisionId moduleRevision, Date date) {
	    return findURLUsingPatterns(moduleRevision, _ivyPatterns, "ivy", "ivy", "xml", date);
    }
	
	protected ResolvedURL getArtifactURL(Artifact artifact) {
        return findURLUsingPatterns(artifact.getModuleRevisionId(), _artifactPatterns, artifact.getName(), artifact.getType(), artifact.getExt(), null);
    }

    private ResolvedURL findURLUsingPatterns(ModuleRevisionId moduleRevision, List patternList, String artifact, String type, String ext, Date date) {
        ResolvedURL rurl = null;
	    for (Iterator iter = patternList.iterator(); iter.hasNext() && rurl == null;) {
            String pattern = (String)iter.next();
            rurl = findURLUsingPattern(moduleRevision, pattern, artifact, type, ext, date);
        }
        return rurl;
    }
    
    protected ResolvedURL findFirstArtifact(ModuleDescriptor md, ModuleRevisionId mrid, Date date) {
        ResolvedURL ret = null;
        String[] conf = md.getConfigurationsNames();
        for (int i = 0; i < conf.length; i++) {
            Artifact[] artifacts = md.getArtifacts(conf[i]);
            for (int j = 0; j < artifacts.length; j++) {
                ret = findURLUsingPatterns(mrid, _artifactPatterns, artifacts[j].getName(), artifacts[j].getType(), artifacts[j].getExt(), date);
                if (ret != null) {
                    return ret;
                }
            }
        }
        return null;
    }

    /**
     * Output message to log indicating what have been done to look for an artifact which
     * has finally not been found
     * 
     * @param artifact the artifact which has not been found
     */
    protected void logIvyNotFound(ModuleRevisionId mrid) {
        Artifact artifact = new DefaultArtifact(mrid, new Date(), "ivy", "ivy", "xml");
        String revisionToken = mrid.getRevision().startsWith("latest.")?"[any "+mrid.getRevision().substring("latest.".length())+"]":"["+mrid.getRevision()+"]";
        Artifact latestArtifact = new DefaultArtifact(new ModuleRevisionId(mrid.getModuleId(), revisionToken), new Date(), "ivy", "ivy", "xml");
        for (Iterator iter = _ivyPatterns.iterator(); iter.hasNext();) {
            String pattern = (String)iter.next();
            String resolvedFileName = IvyPatternHelper.substitute(pattern, artifact);
            Message.verbose("\t\ttried "+resolvedFileName);
            _ivyattempts.add(resolvedFileName);
            if (!mrid.isExactRevision()) {
                resolvedFileName = IvyPatternHelper.substitute(pattern, latestArtifact);
                Message.verbose("\t\ttried "+resolvedFileName);
                _ivyattempts.add(resolvedFileName);
            }
        }
    }

    /**
     * Output message to log indicating what have been done to look for an artifact which
     * has finally not been found
     * 
     * @param artifact the artifact which has not been found
     */
    protected void logNotFound(Artifact artifact) {
        for (Iterator iter = _artifactPatterns.iterator(); iter.hasNext();) {
            String pattern = (String)iter.next();
            String resolvedFileName = IvyPatternHelper.substitute(pattern, artifact);
            Message.warn("\t\ttried "+resolvedFileName);
        }
    }

    protected abstract ResolvedURL findURLUsingPattern(ModuleRevisionId moduleRevision, String pattern, String artifact, String type, String ext, Date date);


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

    protected Collection findNames(Map tokenValues, String token) {
        Collection names = new HashSet();
        names.addAll(findIvyNames(tokenValues, token));
        names.addAll(findArtifactNames(tokenValues, token));
        return names;
    }

    protected Collection findIvyNames(Map tokenValues, String token) {
        Collection names = new HashSet();
        tokenValues = new HashMap(tokenValues);
        tokenValues.put(IvyPatternHelper.ARTIFACT_KEY, "ivy");
        tokenValues.put(IvyPatternHelper.TYPE_KEY, "ivy");
        tokenValues.put(IvyPatternHelper.EXT_KEY, "xml");
        findTokenValues(names, getIvyPatterns(), tokenValues, token);
        getIvy().filterIgnore(names);
        return names;
    }
    
    protected Collection findArtifactNames(Map tokenValues, String token) {
        Collection names = new HashSet();
        tokenValues = new HashMap(tokenValues);
        tokenValues.put(IvyPatternHelper.ARTIFACT_KEY, tokenValues.get(IvyPatternHelper.MODULE_KEY));
        tokenValues.put(IvyPatternHelper.TYPE_KEY, "jar");
        tokenValues.put(IvyPatternHelper.EXT_KEY, "jar");
        findTokenValues(names, getArtifactPatterns(), tokenValues, token);
        getIvy().filterIgnore(names);
        return names;
    }

    // should be overwritten by subclasses wanting to have listing features
    protected void findTokenValues(Collection names, List patterns, Map tokenValues, String token) {
    }

    protected void findTokenValues(Collection names, List listers, String prefix, List patterns, Map tokenValues, String token) {
        for (Iterator iter = patterns.iterator(); iter.hasNext();) {
            String pattern = (String)iter.next();
            String partiallyResolvedPattern = IvyPatternHelper.substituteTokens(pattern, tokenValues);
            for (Iterator iterator = listers.iterator(); iterator.hasNext();) {
                URLLister lister = (URLLister)iterator.next();
                String[] values = ResolverHelper.listTokenValues(lister, prefix+partiallyResolvedPattern, token);
                if (values != null) {
                    names.addAll(Arrays.asList(values));
                }
            }
        }
    }
}
