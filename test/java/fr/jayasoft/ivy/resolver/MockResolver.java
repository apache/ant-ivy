/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.resolver;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import fr.jayasoft.ivy.Artifact;
import fr.jayasoft.ivy.DefaultModuleDescriptor;
import fr.jayasoft.ivy.DependencyDescriptor;
import fr.jayasoft.ivy.DependencyResolver;
import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.ModuleDescriptor;
import fr.jayasoft.ivy.ModuleRevisionId;
import fr.jayasoft.ivy.ResolveData;
import fr.jayasoft.ivy.ResolvedModuleRevision;
import fr.jayasoft.ivy.report.DownloadReport;


public class MockResolver extends AbstractResolver {
    static MockResolver buildMockResolver(String name, boolean findRevision, final Date publicationDate) {
        return buildMockResolver(name, findRevision, ModuleRevisionId.newInstance("test", "test", "test"), publicationDate);
    }

    static MockResolver buildMockResolver(String name, boolean findRevision, final ModuleRevisionId mrid, final Date publicationDate) {
        return buildMockResolver(name, findRevision, mrid, publicationDate, false);
    }
    static MockResolver buildMockResolver(String name, boolean findRevision, final ModuleRevisionId mrid, final Date publicationDate, final boolean isdefault) {
        final MockResolver r = new MockResolver();
        r.setName(name);
        if (findRevision) {
            r.rmr = new ResolvedModuleRevision() {
                public DependencyResolver getResolver() {
                    return r;
                }

                public ModuleRevisionId getId() {
                    return mrid;
                }

                public Date getPublicationDate() {
                    return publicationDate;
                }

                public ModuleDescriptor getDescriptor() {
                    return new DefaultModuleDescriptor(mrid, "integration", new Date(), isdefault);
                }
                public boolean isDownloaded() {
                    return true;
                }
                public boolean isSearched() {
                    return true;
                }

                public DependencyResolver getArtifactResolver() {
                    return r;
                }
                public URL getLocalMDUrl() {
                	return null;
                }
            };
        }
        return r;
    }

    List askedDeps = new ArrayList();
    ResolvedModuleRevision rmr;
    
    public ResolvedModuleRevision getDependency(DependencyDescriptor dd, ResolveData data) throws ParseException {
        askedDeps.add(dd);
        return rmr;
    }

    public DownloadReport download(Artifact[] artifacts, Ivy ivy, File cache, boolean useOrigin) {
        return null;
    }
    public void publish(Artifact artifact, File src, boolean overwrite) throws IOException {
    }

}
