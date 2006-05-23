/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.resolver;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import fr.jayasoft.ivy.Artifact;
import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.LatestStrategy;
import fr.jayasoft.ivy.ModuleRevisionId;
import fr.jayasoft.ivy.report.DownloadReport;
import fr.jayasoft.ivy.repository.AbstractRepository;
import fr.jayasoft.ivy.repository.Repository;
import fr.jayasoft.ivy.repository.Resource;
import fr.jayasoft.ivy.util.IvyPatternHelper;
import fr.jayasoft.ivy.util.Message;
import fr.jayasoft.ivy.version.VersionMatcher;

/**
 * @author Xavier Hanin
 *
 */
public class RepositoryResolver extends AbstractResourceResolver {
    
    private Repository _repository;
    private Boolean _alwaysCheckExactRevision = null;

    
    public RepositoryResolver() {
    }
    
    public Repository getRepository() {
        return _repository;
    }    

    public void setRepository(Repository repository) {
        _repository = repository;
    }

    public void setName(String name) {
        super.setName(name);
        if (_repository instanceof AbstractRepository) {
            ((AbstractRepository)_repository).setName(name);
        }
    }


    protected ResolvedResource findResourceUsingPattern(ModuleRevisionId mrid, String pattern, Artifact artifact, ResourceMDParser rmdparser, Date date) {
        return findResourceUsingPattern(getName(), getRepository(), getLatestStrategy(), getIvy().getVersionMatcher(), rmdparser, mrid, pattern, artifact, date, isAlwaysCheckExactRevision());
    }
    
    public static ResolvedResource findResourceUsingPattern(String name, Repository repository, LatestStrategy strategy, VersionMatcher versionMatcher, ResourceMDParser rmdparser, ModuleRevisionId mrid, String pattern, Artifact artifact, Date date, boolean alwaysCheckExactRevision) {
        try {
            if (!versionMatcher.isDynamic(mrid) || alwaysCheckExactRevision) {
                String resourceName = IvyPatternHelper.substitute(pattern, mrid, artifact);
                Message.debug("\t trying "+resourceName);
                Resource res = repository.getResource(resourceName);
                boolean reachable = res.exists();
                if (reachable) {
                    return new ResolvedResource(res, mrid.getRevision());
                } else if (versionMatcher.isDynamic(mrid)) {
                    return findDynamicResourceUsingPattern(name, repository, strategy, versionMatcher, rmdparser, mrid, pattern, artifact, date);
                } else {
                    Message.debug("\t"+name+": resource not reachable for "+mrid+": res="+res);
                    return null;
                }
            } else {
                return findDynamicResourceUsingPattern(name, repository, strategy, versionMatcher, rmdparser, mrid, pattern, artifact, date);
            }
        } catch (Exception ex) {
            Message.debug("\t"+name+": unable to get resource for "+mrid+": res="+IvyPatternHelper.substitute(pattern, mrid, artifact)+": "+ex.getMessage());
            return null;
        }
    }
    
    private static ResolvedResource findDynamicResourceUsingPattern(
    		String name, 
    		Repository repository, 
    		LatestStrategy strategy, 
    		VersionMatcher versionMatcher, 
    		ResourceMDParser rmdparser,
    		ModuleRevisionId mrid, 
    		String pattern, 
    		Artifact artifact, 
    		Date date) {
        ResolvedResource[] rress = ResolverHelper.findAll(repository, mrid, pattern, artifact);
        if (rress == null) {
            Message.debug("\t"+name+": unable to list resources for "+mrid+": pattern="+pattern);
            return null;
        } else {
        	ResolvedResource found = null;
        	List sorted = strategy.sort(rress);
        	for (Iterator iter = sorted.iterator(); iter.hasNext();) {
				ResolvedResource rres = (ResolvedResource) iter.next();
				if ((date != null && rres.getLastModified() > date.getTime())) {
	                Message.debug("\t"+name+": too young: "+rres);
					continue;
				}
				ModuleRevisionId foundMrid = ModuleRevisionId.newInstance(mrid, rres.getRevision());
				if (!versionMatcher.accept(mrid, foundMrid)) {
	                Message.debug("\t"+name+": rejected by version matcher: "+rres);
					continue;
				}
				if (versionMatcher.needModuleDescriptor(mrid, foundMrid)) {
            		ResolvedResource r = rmdparser.parse(rres.getResource(), rres.getRevision());
            		if (!versionMatcher.accept(mrid, ((MDResolvedResource)r).getResolvedModuleRevision().getDescriptor())) {
    	                Message.debug("\t"+name+": md rejected by version matcher: "+rres);
            			continue;
            		} else {
            			found = r;
            		}
					
				} else {
					found = rres;
				}
			}
        	if (found == null) {
        		Message.debug("\t"+name+": no resource found for "+mrid+": pattern="+pattern);                    
        	} else if (!found.getResource().exists()) {
        		Message.debug("\t"+name+": resource not reachable for "+mrid+": res="+found.getResource());
        		return null; 
        	}
        	return found;
        }
    }

    protected long get(Resource resource, File ivyTempFile) throws IOException {
        _repository.get(resource.getName(), ivyTempFile);
        return ivyTempFile.length();
    }

    public void publish(Artifact artifact, File src, boolean overwrite) throws IOException {
        String destPattern;
        if ("ivy".equals(artifact.getType()) && !getIvyPatterns().isEmpty()) {
            destPattern =  (String)getIvyPatterns().get(0);
        } else if (!getArtifactPatterns().isEmpty()) {
            destPattern =  (String)getArtifactPatterns().get(0);
        } else {
            throw new IllegalStateException("impossible to publish "+artifact+" using "+this+": no artifact pattern defined");
        }
        // Check for m2 compatibility
        ModuleRevisionId mrid = artifact.getModuleRevisionId();
        if (isM2compatible()) {
            mrid = convertM2IdForResourceSearch(mrid);
        }
        
        String dest = IvyPatternHelper.substitute(destPattern,
                mrid,
                artifact); 
        
        _repository.put(src, dest, overwrite);
        Message.info("\tpublished "+artifact.getName()+" to "+dest);
    }
    
    public DownloadReport download(Artifact[] artifacts, Ivy ivy, File cache) {
        try {
            _repository.addTransferListener(ivy);
            return super.download(artifacts, ivy, cache);
        } finally {
            if (ivy != null) {
                _repository.removeTransferListener(ivy);
            }
        }
    }    

    protected void findTokenValues(Collection names, List patterns, Map tokenValues, String token) {
        for (Iterator iter = patterns.iterator(); iter.hasNext();) {
            String pattern = (String)iter.next();
            String partiallyResolvedPattern = IvyPatternHelper.substituteTokens(pattern, tokenValues);
            String[] values = ResolverHelper.listTokenValues(_repository, partiallyResolvedPattern, token);
            if (values != null) {
                names.addAll(Arrays.asList(values));
            }
        }
    }
    
    public String getTypeName() {
        return "repository";
    }
    public void dumpConfig() {
        super.dumpConfig();
        Message.debug("\t\trepository: "+getRepository());
    }
    
    public void setIvy(Ivy ivy) {
        super.setIvy(ivy);
        if (ivy != null) {
            if (_alwaysCheckExactRevision == null) {
                _alwaysCheckExactRevision = Boolean.valueOf(ivy.getVariable("ivy.default.always.check.exact.revision"));
            }
        }
    }

    public boolean isAlwaysCheckExactRevision() {
        return _alwaysCheckExactRevision == null ? true : _alwaysCheckExactRevision.booleanValue();
    }

    public void setAlwaysCheckExactRevision(boolean alwaysCheckExactRevision) {
        _alwaysCheckExactRevision = Boolean.valueOf(alwaysCheckExactRevision);
    }
}
