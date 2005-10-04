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
import java.util.Iterator;
import java.util.List;

import fr.jayasoft.ivy.Artifact;
import fr.jayasoft.ivy.DependencyDescriptor;
import fr.jayasoft.ivy.DependencyResolver;
import fr.jayasoft.ivy.Ivy;
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
    private boolean _returnFirst = false;
    private List _chain = new ArrayList();

    public void add(DependencyResolver resolver) {
        _chain.add(resolver);
    }
    
    public ResolvedModuleRevision getDependency(DependencyDescriptor dd, ResolveData data) throws ParseException {
        data = new ResolveData(data, doValidate(data));
        ResolvedModuleRevision ret = null;
        
        for (Iterator iter = _chain.iterator(); iter.hasNext();) {
            DependencyResolver resolver = (DependencyResolver) iter.next();
            ResolvedModuleRevision mr = resolver.getDependency(dd, data);
            if (mr != null) {
                // check if latest is asked and compare to return the most recent
                if (!_returnFirst && !dd.getDependencyRevisionId().isExactRevision()) {
                    if (ret == null || mr.getPublicationDate().after(ret.getPublicationDate())) {
                        Message.debug("\tmodule revision kept as younger: "+mr.getId());
                        ret = mr;
                    } else {
                        Message.debug("\tmodule revision discarded as older: "+mr.getId());
                    }
                } else {
                    return mr;
                }
            }
        }
        return ret;
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

    public DownloadReport download(Artifact[] artifacts, Ivy ivy, File cache) {
        List artifactsToDownload = new ArrayList(Arrays.asList(artifacts));
        DownloadReport report = new DownloadReport();
        for (Iterator iter = _chain.iterator(); iter.hasNext() && !artifactsToDownload.isEmpty();) {
            DependencyResolver resolver = (DependencyResolver) iter.next();
            DownloadReport r = resolver.download((Artifact[])artifactsToDownload.toArray(new Artifact[artifactsToDownload.size()]), ivy, cache);
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
}
