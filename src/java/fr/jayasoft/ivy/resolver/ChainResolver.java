/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.resolver;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import fr.jayasoft.ivy.Artifact;
import fr.jayasoft.ivy.ArtifactInfo;
import fr.jayasoft.ivy.DependencyDescriptor;
import fr.jayasoft.ivy.DependencyResolver;
import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.LatestStrategy;
import fr.jayasoft.ivy.ResolveData;
import fr.jayasoft.ivy.ResolvedModuleRevision;
import fr.jayasoft.ivy.report.ArtifactDownloadReport;
import fr.jayasoft.ivy.report.DownloadReport;
import fr.jayasoft.ivy.report.DownloadStatus;
import fr.jayasoft.ivy.util.Message;

/**
 * @author Hanin
 *
 */
public class ChainResolver extends AbstractResolver {
    public static class ResolvedModuleRevisionArtifactInfo implements ArtifactInfo {
        private ResolvedModuleRevision _rmr;

        public ResolvedModuleRevisionArtifactInfo(ResolvedModuleRevision rmr) {
            _rmr = rmr;
        }

        public String getRevision() {
            return _rmr.getId().getRevision();
        }

        public long getLastModified() {
            return _rmr.getPublicationDate().getTime();
        }

    }

    private boolean _returnFirst = false;
    private List _chain = new ArrayList();
    private boolean _dual;

    public void add(DependencyResolver resolver) {
        _chain.add(resolver);
    }
    
    public ResolvedModuleRevision getDependency(DependencyDescriptor dd, ResolveData data) throws ParseException {
        data = new ResolveData(data, doValidate(data));
        ResolvedModuleRevision ret = null;
        
        List errors = new ArrayList();
        
        for (Iterator iter = _chain.iterator(); iter.hasNext();) {
            DependencyResolver resolver = (DependencyResolver) iter.next();
            LatestStrategy oldLatest = setLatestIfRequired(resolver, getLatestStrategy());
            ResolvedModuleRevision mr = null;
            try {
                mr = resolver.getDependency(dd, data);
            } catch (Exception ex) {
            	Message.verbose("problem occured while resolving "+dd+" with "+resolver+": "+ex);
            	errors.add(ex);
            } finally {
                if (oldLatest != null) {
                    setLatest(resolver, oldLatest);
                }
            }
            checkInterrupted();
            if (mr != null) {
                boolean shouldReturn = _returnFirst;
                shouldReturn |= !getIvy().getVersionMatcher().isDynamic(dd.getDependencyRevisionId()) && ret != null && !ret.getDescriptor().isDefault();
                if (!shouldReturn) {
                    // check if latest is asked and compare to return the most recent
                    String mrDesc = mr.getId()+(mr.getDescriptor().isDefault()?"[default]":"")+" from "+mr.getResolver().getName();
                    Message.debug("\tchecking "+mrDesc+" against "+ret);
                    if (ret == null) {
                        Message.debug("\tmodule revision kept as first found: "+mrDesc);
                        ret = mr;
                    } else if (isAfter(mr, ret, data.getDate())) {
                        Message.debug("\tmodule revision kept as younger: "+mrDesc);
                        ret = mr;
                    } else if (!mr.getDescriptor().isDefault() && ret.getDescriptor().isDefault()) {
                            Message.debug("\tmodule revision kept as better (not default): "+mrDesc);
                            ret = mr;
                    } else {
                        Message.debug("\tmodule revision discarded as older: "+mrDesc);
                    }
                    if (!getIvy().getVersionMatcher().isDynamic(dd.getDependencyRevisionId()) && !ret.getDescriptor().isDefault()) {
                        Message.debug("\tmodule revision found and is not default: returning "+mrDesc);
                        return resolvedRevision(mr);
                    }
                } else {
                    return resolvedRevision(mr);
                }
            }
        }
        if (ret == null && !errors.isEmpty()) {
        	if (errors.size() == 1) {
        		Exception ex = (Exception) errors.get(0);
        		if (ex instanceof RuntimeException) {
        			throw (RuntimeException)ex;
        		} else if (ex instanceof ParseException) {
        			throw (ParseException)ex;
        		} else {
        			throw new RuntimeException(ex.toString(), ex);
        		}
        	} else {
        		StringBuffer err = new StringBuffer();
        		for (Iterator iter = errors.iterator(); iter.hasNext();) {
					Exception ex = (Exception) iter.next();
					err.append(ex).append("\n");
				}
        		err.setLength(err.length() - 1);
        		throw new RuntimeException("several problems occured while resolving "+dd+":\n"+err);
        	}
        }
        return resolvedRevision(ret);
    }
    
    private ResolvedModuleRevision resolvedRevision(ResolvedModuleRevision mr) {
        if (isDual() && mr != null) {
            return new ResolvedModuleRevisionProxy(mr, this);
        } else {
            return mr;
        }
    }
    

    private LatestStrategy setLatestIfRequired(DependencyResolver resolver, LatestStrategy latestStrategy) {
        String latestName = getLatestStrategyName(resolver);
        if (latestName != null && !"default".equals(latestName)) {
            LatestStrategy oldLatest = getLatest(resolver);
            setLatest(resolver, latestStrategy);
            return oldLatest;
        } else {
            return null;
        }
    }

    /**
     * Returns true if rmr1 is after rmr2, using the latest strategy to determine
     * which is the latest
     * @param rmr1
     * @param rmr2
     * @return
     */
    private boolean isAfter(ResolvedModuleRevision rmr1, ResolvedModuleRevision rmr2, Date date) {
        ArtifactInfo[] ais = new ArtifactInfo[] {
                new ResolvedModuleRevisionArtifactInfo(rmr2),
                new ResolvedModuleRevisionArtifactInfo(rmr1)
        };
        return getLatestStrategy().findLatest(ais, date) != ais[0];
    }

    public void reportFailure() {
        for (Iterator iter = _chain.iterator(); iter.hasNext();) {
            DependencyResolver resolver = (DependencyResolver) iter.next();
            resolver.reportFailure();
        }
    }

    public void reportFailure(Artifact art) {
        for (Iterator iter = _chain.iterator(); iter.hasNext();) {
            DependencyResolver resolver = (DependencyResolver) iter.next();
            resolver.reportFailure(art);
        }
    }

    public DownloadReport download(Artifact[] artifacts, Ivy ivy, File cache, boolean useOrigin) {
        List artifactsToDownload = new ArrayList(Arrays.asList(artifacts));
        DownloadReport report = new DownloadReport();
        for (Iterator iter = _chain.iterator(); iter.hasNext() && !artifactsToDownload.isEmpty();) {
            DependencyResolver resolver = (DependencyResolver) iter.next();
            DownloadReport r = resolver.download((Artifact[])artifactsToDownload.toArray(new Artifact[artifactsToDownload.size()]), ivy, cache, useOrigin);
            ArtifactDownloadReport[] adr = r.getArtifactsReports();
            for (int i = 0; i < adr.length; i++) {
                if (adr[i].getDownloadStatus() != DownloadStatus.FAILED) {
                    artifactsToDownload.remove(adr[i].getArtifact());
                    report.addArtifactReport(adr[i]);
                }
            }
        }
        for (Iterator iter = artifactsToDownload.iterator(); iter.hasNext();) {
            Artifact art = (Artifact)iter.next();
            ArtifactDownloadReport adr = new ArtifactDownloadReport(art);
            adr.setDownloadStatus(DownloadStatus.FAILED);
            report.addArtifactReport(adr);
        }
        return report;
    }
    public List getResolvers() {
        return _chain;
    }
    public void publish(Artifact artifact, File src, boolean overwrite) throws IOException {
        if (_chain.isEmpty()) {
            throw new IllegalStateException("invalid chain resolver with no sub resolver");
        }
        ((DependencyResolver)_chain.get(0)).publish(artifact, src, overwrite);
    }
    public boolean isReturnFirst() {
        return _returnFirst;
    }
    
    public void setReturnFirst(boolean returnFirst) {
        _returnFirst = returnFirst;
    }
    
    public void dumpConfig() {
        Message.verbose("\t"+getName()+" [chain] "+_chain);
        Message.debug("\t\treturn first: "+isReturnFirst());
        Message.debug("\t\tdual: "+isDual());
        for (Iterator iter = _chain.iterator(); iter.hasNext();) {
            DependencyResolver r = (DependencyResolver)iter.next();
            Message.debug("\t\t-> "+r.getName());
        }
    }
    
    public boolean exists(Artifact artifact) {
        for (Iterator iter = _chain.iterator(); iter.hasNext();) {
            DependencyResolver resolver = (DependencyResolver)iter.next();
            if (resolver.exists(artifact)) {
                return true;
            }
        }
        return false;
    }


    private static void setLatest(DependencyResolver resolver, LatestStrategy latest) {
        if (resolver instanceof HasLatestStrategy) {
            HasLatestStrategy r = (HasLatestStrategy)resolver;
            r.setLatestStrategy(latest);
        }
    }

    private static LatestStrategy getLatest(DependencyResolver resolver) {
        if (resolver instanceof HasLatestStrategy) {
            HasLatestStrategy r = (HasLatestStrategy)resolver;
            return r.getLatestStrategy();
        }
        return null;
    }

    private static String getLatestStrategyName(DependencyResolver resolver) {
        if (resolver instanceof HasLatestStrategy) {
            HasLatestStrategy r = (HasLatestStrategy)resolver;
            return r.getLatest();
        }
        return null;
    }

    public void setDual(boolean b) {
        _dual = b;
    }

    public boolean isDual() {
        return _dual;
    }

}
