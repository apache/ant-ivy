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
package org.apache.ivy.core.retrieve;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.SystemUtils;
import org.apache.ivy.Ivy;
import org.apache.ivy.TestHelper;
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.event.IvyEvent;
import org.apache.ivy.core.event.IvyListener;
import org.apache.ivy.core.event.retrieve.EndRetrieveArtifactEvent;
import org.apache.ivy.core.event.retrieve.EndRetrieveEvent;
import org.apache.ivy.core.event.retrieve.StartRetrieveArtifactEvent;
import org.apache.ivy.core.event.retrieve.StartRetrieveEvent;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.util.DefaultMessageLogger;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.MockMessageLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;

import junit.framework.TestCase;

public class RetrieveTest extends TestCase {

    private Ivy ivy;

    protected void setUp() throws Exception {
        ivy = Ivy.newInstance();
        ivy.configure(new File("test/repositories/ivysettings.xml"));
        TestHelper.createCache();
        Message.setDefaultLogger(new DefaultMessageLogger(Message.MSG_INFO));
    }

    protected void tearDown() throws Exception {
        TestHelper.cleanCache();
        Delete del = new Delete();
        del.setProject(new Project());
        del.setDir(new File("build/test/retrieve"));
        del.execute();
    }

    public void testRetrieveSimple() throws Exception {
        // mod1.1 depends on mod1.2
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml").toURL(),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);

        String pattern = "build/test/retrieve/[module]/[conf]/[artifact]-[revision].[ext]";
        ivy.retrieve(md.getModuleRevisionId(), pattern, getRetrieveOptions());
        assertTrue(new File(IvyPatternHelper.substitute(pattern, "org1", "mod1.2", "2.0", "mod1.2",
            "jar", "jar", "default")).exists());

        pattern = "build/test/retrieve/[module]/[conf]/[type]s/[artifact]-[revision].[ext]";
        ivy.retrieve(md.getModuleRevisionId(), pattern, getRetrieveOptions());
        assertTrue(new File(IvyPatternHelper.substitute(pattern, "org1", "mod1.2", "2.0", "mod1.2",
            "jar", "jar", "default")).exists());
    }

    public void testRetrieveSameFileConflict() throws Exception {
        // mod1.1 depends on mod1.2
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org1/mod1.4/ivys/ivy-1.0.1.xml").toURL(),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);

        String pattern = "build/test/retrieve/[module]/[artifact]-[revision].[ext]";
        MockMessageLogger mockLogger = new MockMessageLogger();
        Message.setDefaultLogger(mockLogger);
        ivy.retrieve(md.getModuleRevisionId(), pattern, getRetrieveOptions());
        assertTrue(new File(IvyPatternHelper.substitute(pattern, "org1", "mod1.2", "2.2", "mod1.2",
            "jar", "jar", "default")).exists());
        mockLogger.assertLogDoesntContain("conflict on");
    }

    public void testRetrieveDifferentArtifactsOfSameModuleToSameFile() throws Exception {
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org2/mod2.2/ivys/ivy-0.5.xml").toURL(),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);

        String pattern = "build/test/retrieve/[module]/[module].[ext]";
        MockMessageLogger mockLogger = new MockMessageLogger();
        Message.setDefaultLogger(mockLogger);
        try {
            ivy.retrieve(md.getModuleRevisionId(), pattern, getRetrieveOptions());
            fail("Exeption should have been thrown!");
        } catch (RuntimeException e) {
            // expected!
        }
        mockLogger.assertLogDoesntContain("multiple artifacts");
    }

    public void testEvent() throws Exception {
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml").toURL(),
            getResolveOptions(new String[] {"*"}));

        final List events = new ArrayList();
        ivy.getEventManager().addIvyListener(new IvyListener() {
            public void progress(IvyEvent event) {
                events.add(event);
            }
        });
        ModuleDescriptor md = report.getModuleDescriptor();
        String pattern = "build/test/retrieve/[module]/[conf]/[artifact]-[revision].[ext]";
        ivy.retrieve(md.getModuleRevisionId(), pattern, getRetrieveOptions());
        assertEquals(4, events.size());
        assertTrue(events.get(0) instanceof StartRetrieveEvent);
        assertTrue(events.get(1) instanceof StartRetrieveArtifactEvent);
        assertTrue(events.get(2) instanceof EndRetrieveArtifactEvent);
        assertTrue(events.get(3) instanceof EndRetrieveEvent);
        EndRetrieveEvent ev = (EndRetrieveEvent) events.get(3);
        assertEquals(1, ev.getNbCopied());
        assertEquals(0, ev.getNbUpToDate());

        events.clear();
        ivy.retrieve(md.getModuleRevisionId(), pattern, getRetrieveOptions());
        assertEquals(2, events.size());
        assertTrue(events.get(0) instanceof StartRetrieveEvent);
        assertTrue(events.get(1) instanceof EndRetrieveEvent);
        ev = (EndRetrieveEvent) events.get(1);
        assertEquals(0, ev.getNbCopied());
        assertEquals(1, ev.getNbUpToDate());
    }

    public void testRetrieveOverwrite() throws Exception {
        // mod1.1 depends on mod1.2
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml").toURL(),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);

        String pattern = "build/test/retrieve/[module]/[conf]/[artifact]-[revision].[ext]";

        // we create a fake old file to see if it is overwritten
        File file = new File(IvyPatternHelper.substitute(pattern, "org1", "mod1.2", "2.0",
            "mod1.2", "jar", "jar", "default")).getCanonicalFile();
        file.getParentFile().mkdirs();
        file.createNewFile();
        ivy.retrieve(md.getModuleRevisionId(), pattern,
            getRetrieveOptions().setOverwriteMode("always"));
        assertEquals(
            new File("test/repositories/1/org1/mod1.2/jars/mod1.2-2.0.jar").lastModified(),
            file.lastModified());
    }

    public void testRetrieveWithSymlinks() throws Exception {
        // mod1.1 depends on mod1.2
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml").toURL(),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);

        RetrieveOptions options = getRetrieveOptions().setMakeSymlinks(true);

        String pattern = "build/test/retrieve/[module]/[conf]/[artifact]-[revision].[ext]";
        ivy.retrieve(md.getModuleRevisionId(), pattern, options);
        assertLink(IvyPatternHelper.substitute(pattern, "org1", "mod1.2", "2.0", "mod1.2", "jar",
            "jar", "default"));

        pattern = "build/test/retrieve/[module]/[conf]/[type]s/[artifact]-[revision].[ext]";
        ivy.retrieve(md.getModuleRevisionId(), pattern, options);
        assertLink(IvyPatternHelper.substitute(pattern, "org1", "mod1.2", "2.0", "mod1.2", "jar",
            "jar", "default"));
    }

    public void testRetrieveWithSymlinksMass() throws Exception {
        if (SystemUtils.IS_OS_WINDOWS) {
            return;
        }

        // mod1.1 depends on mod1.2
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml").toURI().toURL(),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);

        RetrieveOptions options = getRetrieveOptions().setMakeSymlinksInMass(true);

        String pattern = "build/test/retrieve/[module]/[conf]/[artifact]-[revision].[ext]";
        ivy.retrieve(md.getModuleRevisionId(), pattern, options);
        assertLink(IvyPatternHelper.substitute(pattern, "org1", "mod1.2", "2.0", "mod1.2", "jar",
            "jar", "default"));

        pattern = "build/test/retrieve/[module]/[conf]/[type]s/[artifact]-[revision].[ext]";
        ivy.retrieve(md.getModuleRevisionId(), pattern, options);
        assertLink(IvyPatternHelper.substitute(pattern, "org1", "mod1.2", "2.0", "mod1.2", "jar",
            "jar", "default"));
    }

    private void assertLink(String filename) throws IOException {
        // if the OS is known to support symlink, check that the file is a symlink,
        // otherwise just check the file exist.

        File file = new File(filename);
        assertTrue("The file " + filename + " doesn't exist", file.exists());

        String os = System.getProperty("os.name");
        if (os.equals("Linux") || os.equals("Solaris") || os.equals("FreeBSD")
                || os.equals("Mac OS X")) {
            // these OS should support symnlink, so check that the file is actually a symlink.
            // this is done be checking that the canonical path is different from the absolute
            // path.
            File absFile = file.getAbsoluteFile();
            File canFile = file.getCanonicalFile();
            assertFalse("The file " + filename + " isn't a symlink", absFile.equals(canFile));
        }
    }

    public void testRetrieveWithVariable() throws Exception {
        // mod1.1 depends on mod1.2
        ivy.setVariable("retrieve.dir", "retrieve");
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml").toURL(),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);

        String pattern = "build/test/${retrieve.dir}/[module]/[conf]/[artifact]-[revision].[ext]";
        ivy.retrieve(md.getModuleRevisionId(), pattern, getRetrieveOptions());
        pattern = IvyPatternHelper.substituteVariable(pattern, "retrieve.dir", "retrieve");
        assertTrue(new File(IvyPatternHelper.substitute(pattern, "org1", "mod1.2", "2.0", "mod1.2",
            "jar", "jar", "default")).exists());

        pattern = "build/test/${retrieve.dir}/[module]/[conf]/[type]s/[artifact]-[revision].[ext]";
        ivy.retrieve(md.getModuleRevisionId(), pattern, getRetrieveOptions());
        pattern = IvyPatternHelper.substituteVariable(pattern, "retrieve.dir", "retrieve");
        assertTrue(new File(IvyPatternHelper.substitute(pattern, "org1", "mod1.2", "2.0", "mod1.2",
            "jar", "jar", "default")).exists());
    }

    public void testRetrieveReport() throws Exception {
        // mod1.1 depends on mod1.2
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org20/mod20.1/ivys/ivy-1.2.xml").toURL(),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);

        ModuleRevisionId mrid = md.getModuleRevisionId();
        RetrieveOptions options = getRetrieveOptions();
        options.setConfs(new String[] {"A"});
        Map artifactsToCopy = ivy.getRetrieveEngine().determineArtifactsToCopy(mrid,
            "build/test/retrieve/[module]/[conf]/[artifact]-[revision].[ext]", options);
        assertEquals(2, artifactsToCopy.size());

        options.setConfs(new String[] {"B"});
        artifactsToCopy = ivy.getRetrieveEngine().determineArtifactsToCopy(mrid,
            "build/test/retrieve/[module]/[conf]/[artifact]-[revision].[ext]", options);
        assertEquals(2, artifactsToCopy.size());

        options.setConfs(new String[] {"A", "B"});
        artifactsToCopy = ivy.getRetrieveEngine().determineArtifactsToCopy(mrid,
            "build/test/retrieve/[module]/[conf]/[artifact]-[revision].[ext]", options);
        assertEquals(3, artifactsToCopy.size());
    }

    public void testUnpack() throws Exception {
        ResolveOptions roptions = getResolveOptions(new String[] {"*"});

        URL url = new File("test/repositories/1/packaging/module1/ivys/ivy-1.0.xml").toURI()
                .toURL();

        // normal resolve, the file goes in the cache
        ResolveReport report = ivy.resolve(url, roptions);
        assertFalse(report.hasError());
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);

        String pattern = "build/test/retrieve/[organization]/[module]/[conf]/[type]s/[artifact]-[revision](.[ext])";

        RetrieveOptions options = getRetrieveOptions();
        ivy.retrieve(md.getModuleRevisionId(), pattern, options);

        File dest = new File("build/test/retrieve/packaging/module2/default/jars/module2-1.0");
        assertTrue(dest.exists());
        assertTrue(dest.isDirectory());
        File[] jarContents = dest.listFiles();
        Arrays.sort(jarContents);
        assertEquals(new File(dest, "META-INF"), jarContents[0]);
        assertEquals(new File(dest, "test.txt"), jarContents[1]);
        assertEquals(new File(dest, "META-INF/MANIFEST.MF"), jarContents[0].listFiles()[0]);
    }

    public void testUnpackSync() throws Exception {
        ResolveOptions roptions = getResolveOptions(new String[] {"*"});

        URL url = new File("test/repositories/1/packaging/module1/ivys/ivy-1.0.xml").toURI()
                .toURL();

        // normal resolve, the file goes in the cache
        ResolveReport report = ivy.resolve(url, roptions);
        assertFalse(report.hasError());
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);

        String pattern = "build/test/retrieve/[organization]/[module]/[conf]/[type]s/[artifact]-[revision](.[ext])";

        RetrieveOptions options = getRetrieveOptions();
        options.setSync(true);
        ivy.retrieve(md.getModuleRevisionId(), pattern, options);

        File dest = new File("build/test/retrieve/packaging/module2/default/jars/module2-1.0");
        assertTrue(dest.exists());
        assertTrue(dest.isDirectory());
        File[] jarContents = dest.listFiles();
        Arrays.sort(jarContents);
        assertEquals(new File(dest, "META-INF"), jarContents[0]);
        assertEquals(new File(dest, "test.txt"), jarContents[1]);
        assertEquals(new File(dest, "META-INF/MANIFEST.MF"), jarContents[0].listFiles()[0]);
    }

    private RetrieveOptions getRetrieveOptions() {
        return new RetrieveOptions();
    }

    private ResolveOptions getResolveOptions(String[] confs) {
        return new ResolveOptions().setConfs(confs);
    }

}
