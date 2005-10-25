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

/**
 * @author Xavier Hanin
 *
 */
public class RepositoryResolver extends AbstractResourceResolver {
    
    private Repository _repository;

    
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


    protected ResolvedResource findResourceUsingPattern(ModuleRevisionId mrid, String pattern, String artifact, String type, String ext, Date date) {
        return findResourceUsingPattern(getName(), getRepository(), getLatestStrategy(), mrid, pattern, artifact, type, ext, date);
    }
    public static ResolvedResource findResourceUsingPattern(String name, Repository repository, LatestStrategy strategy, ModuleRevisionId mrid, String pattern, String artifact, String type, String ext, Date date) {
        String resourceName = IvyPatternHelper.substitute(pattern, mrid, artifact, type, ext);
        Message.debug("\t trying "+resourceName);
        try {
            Resource res = repository.getResource(resourceName);
            long start = System.currentTimeMillis();
            boolean reachable = res.exists();
            if (reachable) {
                return new ResolvedResource(res, mrid.getRevision());
            } else if (!mrid.isExactRevision()) {
                ResolvedResource[] rress = ResolverHelper.findAll(repository, mrid, pattern, artifact, type, ext);
                if (rress == null) {
                    Message.debug("\t"+name+": unable to list resources for "+mrid+": pattern="+pattern);
                    return null;
                } else {
                    for (int i = 0; i < rress.length; i++) {
                        Message.debug("\t"+name+": found "+rress[i]);
                    }
                }
                ResolvedResource found;
                if (rress.length == 1) {
                    Message.debug("only one resource found: no need to use latest strategy");
                     found = rress[0];
                } else {
                    found = (ResolvedResource)strategy.findLatest(rress, date);
                }
                if (found == null) {
                    Message.debug("\t"+name+": no resource found for "+mrid+": pattern="+pattern);                    
                } else if (!found.getResource().exists()) {
                    Message.debug("\t"+name+": resource not reachable for "+mrid+": res="+res);
                    return null; 
                }
                return found;
            } else {
                Message.debug("\t"+name+": resource not reachable for "+mrid+": res="+res);
                return null;
            }
        } catch (Exception ex) {
            Message.debug("\t"+name+": unable to get resource for "+mrid+": res="+resourceName+": "+ex.getMessage());
            return null;
        }
    }

    /**
     * Returns all resolved res matching the given pattern and matching given mrid, 
     * or null if no lister is able to handle the given pattern
     * 
     * @param mrid
     * @param pattern
     * @param artifact
     * @param type
     * @param ext
     * @return
     */
    protected ResolvedResource[] findAll(ModuleRevisionId mrid, String pattern, String artifact, String type, String ext) {
        return ResolverHelper.findAll(_repository, mrid, pattern, artifact, type, ext);
    }

    protected long get(Resource resource, File ivyTempFile) throws IOException {
        _repository.get(resource.getName(), ivyTempFile);
        return ivyTempFile.length();
    }

    public void publish(Artifact artifact, File src, boolean overwrite) throws IOException {
        String destPattern;
        if ("ivy".equals(artifact.getType()) && !getIvyPatterns().isEmpty()) {
            destPattern =  (String)getIvyPatterns().get(0);
        } else {
            destPattern =  (String)getArtifactPatterns().get(0);
        }
        String dest = IvyPatternHelper.substitute(destPattern, artifact);
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
}
