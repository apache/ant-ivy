/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.resolver;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collections;

import fr.jayasoft.ivy.Artifact;
import fr.jayasoft.ivy.DependencyDescriptor;
import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.IvyNode;
import fr.jayasoft.ivy.ModuleRevisionId;
import fr.jayasoft.ivy.ResolveData;
import fr.jayasoft.ivy.ResolvedModuleRevision;
import fr.jayasoft.ivy.report.ArtifactDownloadReport;
import fr.jayasoft.ivy.report.DownloadReport;
import fr.jayasoft.ivy.report.DownloadStatus;
import fr.jayasoft.ivy.util.Message;


public class CacheResolver extends FileSystemResolver {
    private File _configured = null;
    
    public CacheResolver() {
    }
    
    public CacheResolver(Ivy ivy) {
        setIvy(ivy);
        setName("cache");
    }

    public ResolvedModuleRevision getDependency(DependencyDescriptor dd, ResolveData data) throws ParseException {
        clearIvyAttempts();

        ModuleRevisionId mrid = dd.getDependencyRevisionId();
        // check revision
        
        // if we do not have to check modified and if the revision is exact and not changing,  
        // we first search for it in cache
        if (!getIvy().getVersionMatcher().isDynamic(mrid)) {
            ResolvedModuleRevision rmr = data.getIvy().findModuleInCache(mrid, data.getCache(), doValidate(data));
            if (rmr != null) {
                Message.verbose("\t"+getName()+": revision in cache: "+mrid);
                return rmr;
            } else {
                Message.verbose("\t"+getName()+": no ivy file in cache found for "+mrid);
                return null;
            }
        } else {
            ensureConfigured(data.getIvy(), data.getCache());
            ResolvedResource ivyRef = findIvyFileRef(dd, data);
            if (ivyRef != null) {
                Message.verbose("\t"+getName()+": found ivy file in cache for "+mrid);
                Message.verbose("\t\t=> "+ivyRef);

                ModuleRevisionId resolvedMrid = ModuleRevisionId.newInstance(mrid, ivyRef.getRevision());
                IvyNode node = data.getNode(resolvedMrid);
                if (node != null && node.getModuleRevision() != null) {
                    // this revision has already be resolved : return it
                    Message.verbose("\t"+getName()+": revision already resolved: "+resolvedMrid);
                    return searchedRmr(node.getModuleRevision());
                }
                ResolvedModuleRevision rmr = data.getIvy().findModuleInCache(resolvedMrid, data.getCache(), doValidate(data));
                if (rmr != null) {
                    Message.verbose("\t"+getName()+": revision in cache: "+resolvedMrid);
                    return searchedRmr(rmr);
                } else {
                    Message.error("\t"+getName()+": inconsistent cache: clean it and resolve again");
                    return null;
                }
            } else {
                Message.verbose("\t"+getName()+": no ivy file in cache found for "+mrid);
                return null;
            }
        }
    }
    
    public DownloadReport download(Artifact[] artifacts, Ivy ivy, File cache) {
        clearArtifactAttempts();
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
                    logArtifactNotFound(artifacts[i]);
                    adr.setDownloadStatus(DownloadStatus.FAILED);                
            }
        }
        return dr;
    }
    public boolean exists(Artifact artifact) {
        ensureConfigured();
        return super.exists(artifact);
    }
    public void publish(Artifact artifact, File src, boolean overwrite) throws IOException {
        ensureConfigured();
        super.publish(artifact, src, overwrite);
    }
    
    
    public OrganisationEntry[] listOrganisations() {
        ensureConfigured();
        return super.listOrganisations();
    }
    public ModuleEntry[] listModules(OrganisationEntry org) {
        ensureConfigured();
        return super.listModules(org);        
    }
    public RevisionEntry[] listRevisions(ModuleEntry module) {
        ensureConfigured();
        return super.listRevisions(module);        
    }
    public void dumpConfig() {
        Message.verbose("\t"+getName()+" [cache]");
    }

    private void ensureConfigured() {
        if (getIvy() != null) {
            ensureConfigured(getIvy(), getIvy().getDefaultCache());
        }
    }
    private void ensureConfigured(Ivy ivy, File cache) {
        if (ivy == null || cache == null || (_configured != null && _configured.equals(cache))) {
            return;
        }
        setIvyPatterns(Collections.singletonList(cache.getAbsolutePath()+"/"+ivy.getCacheIvyPattern()));
        setArtifactPatterns(Collections.singletonList(cache.getAbsolutePath()+"/"+ivy.getCacheArtifactPattern()));
    }
    public String getTypeName() {
        return "cache";
    }
}
