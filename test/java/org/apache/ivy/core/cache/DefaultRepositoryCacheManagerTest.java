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
package org.apache.ivy.core.cache;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.Date;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorWriter;
import org.apache.ivy.plugins.repository.BasicResource;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.plugins.repository.ResourceDownloader;
import org.apache.ivy.plugins.resolver.MockResolver;
import org.apache.ivy.plugins.resolver.util.ResolvedResource;
import org.apache.ivy.util.DefaultMessageLogger;
import org.apache.ivy.util.Message;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;

import junit.framework.TestCase;

/**
 * @see DefaultResolutionCacheManager
 */
public class DefaultRepositoryCacheManagerTest extends TestCase {
    
    private DefaultRepositoryCacheManager cacheManager;
    private Artifact artifact;
    private ArtifactOrigin origin;
    private Ivy ivy;

    protected void setUp() throws Exception {
        File f = File.createTempFile("ivycache", ".dir");
        ivy = new Ivy();
        ivy.configureDefault();
        ivy.getLoggerEngine().setDefaultLogger(new DefaultMessageLogger(Message.MSG_DEBUG));
        IvyContext.pushNewContext().setIvy(ivy);
        
        IvySettings settings = ivy.getSettings();
        f.delete(); // we want to use the file as a directory, so we delete the file itself
        cacheManager = new DefaultRepositoryCacheManager();
        cacheManager.setSettings(settings);
        cacheManager.setBasedir(f);

        artifact = createArtifact("org", "module", "rev", "name", "type", "ext");

        Artifact originArtifact = createArtifact("org", "module", "rev", "name", "pom.original",
            "pom");
        origin = new ArtifactOrigin(originArtifact, true, "file:/some/where.pom");

        cacheManager.saveArtifactOrigin(originArtifact, origin);
        cacheManager.saveArtifactOrigin(artifact, origin);
    }

    protected void tearDown() throws Exception {
        IvyContext.popContext();
        Delete del = new Delete();
        del.setProject(new Project());
        del.setDir(cacheManager.getRepositoryCacheRoot());
        del.execute();
    }

    public void testArtifactOrigin() {
        ArtifactOrigin found = cacheManager.getSavedArtifactOrigin(artifact);
        assertEquals(origin, found);
        assertEquals("pom", found.getArtifact().getExt());

        artifact = createArtifact("org", "module", "rev", "name", "type2", "ext");
        found = cacheManager.getSavedArtifactOrigin(artifact);
        assertTrue(ArtifactOrigin.isUnknown(found));
    }

    public void testUniqueness() {
        cacheManager.saveArtifactOrigin(artifact, origin);

        artifact = createArtifact("org1", "module", "rev", "name", "type", "ext");
        ArtifactOrigin found = cacheManager.getSavedArtifactOrigin(artifact);
        assertTrue(ArtifactOrigin.isUnknown(found));

        artifact = createArtifact("org", "module1", "rev", "name", "type", "ext");
        found = cacheManager.getSavedArtifactOrigin(artifact);
        assertTrue(ArtifactOrigin.isUnknown(found));

        artifact = createArtifact("org", "module", "rev1", "name", "type", "ext");
        found = cacheManager.getSavedArtifactOrigin(artifact);
        assertTrue(ArtifactOrigin.isUnknown(found));

        artifact = createArtifact("org", "module", "rev", "name1", "type", "ext");
        found = cacheManager.getSavedArtifactOrigin(artifact);
        assertTrue(ArtifactOrigin.isUnknown(found));

        artifact = createArtifact("org", "module", "rev", "name", "type1", "ext");
        found = cacheManager.getSavedArtifactOrigin(artifact);
        assertTrue(ArtifactOrigin.isUnknown(found));

        artifact = createArtifact("org", "module", "rev", "name", "type", "ext1");
        found = cacheManager.getSavedArtifactOrigin(artifact);
        assertTrue(ArtifactOrigin.isUnknown(found));
    }

    public void testLatestIntegrationIsCachedPerResolver() throws Exception {
        // given a module org#module
        ModuleId mi = new ModuleId("org", "module");

        // and a latest.integration mrid/dd
        ModuleRevisionId mridLatest = new ModuleRevisionId(mi, "trunk", "latest.integration");
        DependencyDescriptor ddLatest = new DefaultDependencyDescriptor(mridLatest,  false);

        // and some random options
        CacheMetadataOptions options = new CacheMetadataOptions().setCheckTTL(false);

        // setup resolver1 to download the static content so we can call cacheModuleDescriptor
        MockResolver resolver1 = new MockResolver();
        resolver1.setName("resolver1");
        resolver1.setSettings(ivy.getSettings());
        ivy.getSettings().addResolver(resolver1);
        ResourceDownloader downloader = new ResourceDownloader() {
            public void download(Artifact artifact, Resource resource, File dest)
                    throws IOException {
                String content = "<ivy-module version=\"2.0\"><info organisation=\"org\" module=\"module\" status=\"integration\" revision=\"1.1\" branch=\"trunk\"/></ivy-module>";
                dest.getParentFile().mkdirs();
                FileOutputStream out = new FileOutputStream(dest);
                PrintWriter pw = new PrintWriter(out);
                pw.write(content);
                pw.flush();
                out.close();
            }
        };
        ModuleDescriptorWriter writer = new ModuleDescriptorWriter() {
            public void write(ResolvedResource originalMdResource, ModuleDescriptor md, File src, File dest) throws IOException, ParseException {
                XmlModuleDescriptorWriter.write(md, dest);
            }
        };

        // latest.integration will resolve to 1.1 in resolver1
        ModuleRevisionId mrid11 = new ModuleRevisionId(mi, "trunk", "1.1");
        DependencyDescriptor dd11 = new DefaultDependencyDescriptor(mrid11,  false);
        DefaultArtifact artifact11 = new DefaultArtifact(mrid11, new Date(), "module-1.1.ivy", "ivy", "ivy", true);
        BasicResource resource11 = new BasicResource("/module-1-1.ivy", true, 1, 0, true);
        ResolvedResource mdRef11 = new ResolvedResource(resource11, "1.1");

        // tell the cache about 1.1
        ResolvedModuleRevision rmr11 = cacheManager.cacheModuleDescriptor(resolver1, mdRef11, dd11, artifact11, downloader, options);
        cacheManager.originalToCachedModuleDescriptor(resolver1, mdRef11, artifact11, rmr11, writer);
        // and use the new overload that passes in resolver name
        cacheManager.saveResolvedRevision("resolver1", mridLatest, "1.1");

        ResolvedModuleRevision rmrFromCache = cacheManager.findModuleInCache(ddLatest, mridLatest, options, "resolver1");
        assertEquals(rmr11, rmrFromCache);
    }

    protected static DefaultArtifact createArtifact(String org, String module, String rev, String name,
            String type, String ext) {
        ModuleId mid = new ModuleId(org, module);
        ModuleRevisionId mrid = new ModuleRevisionId(mid, rev);
        return new DefaultArtifact(mrid, new Date(), name, type, ext);
    }

}
