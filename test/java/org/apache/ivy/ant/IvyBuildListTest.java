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
package org.apache.ivy.ant;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.ivy.TestHelper;
import org.apache.ivy.util.FileUtil;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;

import junit.framework.TestCase;

// CheckStyle:MagicNumber| OFF
// The test very often use MagicNumber. Using a constant is less expressive.

public class IvyBuildListTest extends TestCase {

    private File cache;

    private Project project;

    private IvyBuildList buildlist;

    protected void setUp() throws Exception {
        createCache();

        project = TestHelper.newProject();
        project.init();

        buildlist = new IvyBuildList();
        buildlist.setProject(project);

        System.setProperty("ivy.cache.dir", cache.getAbsolutePath());
    }

    protected void tearDown() throws Exception {
        cleanCache();
    }

    private void cleanCache() {
        FileUtil.forceDelete(cache);
    }

    private void createCache() {
        cache = new File("build/cache");
        cache.mkdirs();
    }

    private String[] getFiles(IvyBuildList buildlist) {
        buildlist.setReference("ordered.build.files");
        buildlist.execute();

        Object o = buildlist.getProject().getReference("ordered.build.files");
        assertNotNull(o);
        assertTrue(o instanceof Path);

        Path path = (Path) o;
        String[] files = path.list();
        assertNotNull(files);
        return files;
    }

    private void assertListOfFiles(String prefix, String[] expected, String[] actual) {
        for (int i = 0; i < expected.length; i++) {
            assertEquals(new File(prefix + expected[i] + "/build.xml").getAbsolutePath(), new File(
                    actual[i]).getAbsolutePath());
        }
    }

    /*
     * Those tests use the ivy files A , B , C , D , E in test/buildlist The dependencies are : A ->
     * C B has no dependency C -> B D -> A , B E has no dependency F -> G G -> F
     */

    public void testSimple() {
        FileSet fs = new FileSet();
        fs.setDir(new File("test/buildlist"));
        fs.setIncludes("**/build.xml");
        fs.setExcludes("E2/build.xml,F/build.xml,G/build.xml");

        buildlist.addFileset(fs);
        buildlist.setOnMissingDescriptor("skip");

        String[] files = getFiles(buildlist);

        assertEquals(5, files.length);

        assertListOfFiles("test/buildlist/", new String[] {"B", "C", "A", "D", "E"}, files);
    }

    public void testReverse() {
        FileSet fs = new FileSet();
        fs.setDir(new File("test/buildlist"));
        fs.setIncludes("**/build.xml");
        fs.setExcludes("E2/build.xml,F/build.xml,G/build.xml");

        buildlist.addFileset(fs);
        buildlist.setReverse(true);
        buildlist.setOnMissingDescriptor("skip");

        String[] files = getFiles(buildlist);

        assertEquals(5, files.length);

        assertListOfFiles("test/buildlist/", new String[] {"E", "D", "A", "C", "B"}, files);
    }

    public void testWithRoot() {
        FileSet fs = new FileSet();
        fs.setDir(new File("test/buildlist"));
        fs.setIncludes("**/build.xml");
        fs.setExcludes("E2/**");

        buildlist.addFileset(fs);
        buildlist.setRoot("C");
        buildlist.setOnMissingDescriptor("skip");

        String[] files = getFiles(buildlist);

        assertEquals(2, files.length); // A and D should be filtered out

        assertListOfFiles("test/buildlist/", new String[] {"B", "C"}, files);
    }

    public void testWithRootCircular() {
        FileSet fs = new FileSet();
        fs.setDir(new File("test/buildlist"));
        fs.setIncludes("**/build.xml");

        buildlist.addFileset(fs);
        buildlist.setRoot("F");
        buildlist.setOnMissingDescriptor("skip");

        String[] files = getFiles(buildlist);

        assertEquals(2, files.length); // F and G should be in the list
    }

    public void testWithTwoRoots() {
        FileSet fs = new FileSet();
        fs.setDir(new File("test/buildlist"));
        fs.setIncludes("**/build.xml");
        fs.setExcludes("E2/**");

        buildlist.addFileset(fs);
        buildlist.setRoot("C,E");
        buildlist.setOnMissingDescriptor("skip");

        String[] files = getFiles(buildlist);

        assertEquals(3, files.length); // A and D should be filtered out

        assertListOfFiles("test/buildlist/", new String[] {"B", "C", "E"}, files);
    }

    public void testWithRootExclude() {
        FileSet fs = new FileSet();
        fs.setDir(new File("test/buildlist"));
        fs.setIncludes("**/build.xml");
        fs.setExcludes("E2/**");

        buildlist.addFileset(fs);
        buildlist.setRoot("C");
        buildlist.setExcludeRoot(true);
        buildlist.setOnMissingDescriptor("skip");

        String[] files = getFiles(buildlist);

        assertEquals(1, files.length); // A, D and C should be filtered out

        assertListOfFiles("test/buildlist/", new String[] {"B"}, files);
    }

    public void testWithRootAndOnlyDirectDep() {
        FileSet fs = new FileSet();
        fs.setDir(new File("test/buildlist"));
        fs.setIncludes("**/build.xml");
        fs.setExcludes("E2/**");

        buildlist.addFileset(fs);
        buildlist.setRoot("A");
        buildlist.setOnlydirectdep(true);
        buildlist.setOnMissingDescriptor("skip");

        String[] files = getFiles(buildlist);

        assertEquals(2, files.length); // We should have only A and C

        assertListOfFiles("test/buildlist/", new String[] {"C", "A"}, files);
    }

    public void testWithLeaf() {
        FileSet fs = new FileSet();
        fs.setDir(new File("test/buildlist"));
        fs.setIncludes("**/build.xml");
        fs.setExcludes("E2/**");

        buildlist.addFileset(fs);
        buildlist.setLeaf("C");
        buildlist.setOnMissingDescriptor("skip");

        String[] files = getFiles(buildlist);

        assertEquals(3, files.length); // B should be filtered out

        assertListOfFiles("test/buildlist/", new String[] {"C", "A", "D"}, files);
    }

    public void testWithLeafCircular() {
        FileSet fs = new FileSet();
        fs.setDir(new File("test/buildlist"));
        fs.setIncludes("**/build.xml");

        buildlist.addFileset(fs);
        buildlist.setLeaf("F");
        buildlist.setOnMissingDescriptor("skip");

        String[] files = getFiles(buildlist);

        assertEquals(2, files.length);
    }

    public void testWithTwoLeafs() {
        FileSet fs = new FileSet();
        fs.setDir(new File("test/buildlist"));
        fs.setIncludes("**/build.xml");
        fs.setExcludes("E2/**");

        buildlist.addFileset(fs);
        buildlist.setLeaf("C,E");
        buildlist.setOnMissingDescriptor("skip");

        String[] files = getFiles(buildlist);

        assertEquals(4, files.length); // B should be filtered out

        assertListOfFiles("test/buildlist/", new String[] {"C", "A", "D", "E"}, files);
    }

    public void testWithLeafExclude() {
        FileSet fs = new FileSet();
        fs.setDir(new File("test/buildlist"));
        fs.setIncludes("**/build.xml");
        fs.setExcludes("E2/**");

        buildlist.addFileset(fs);
        buildlist.setLeaf("C");
        buildlist.setExcludeLeaf(true);
        buildlist.setOnMissingDescriptor("skip");

        String[] files = getFiles(buildlist);

        assertEquals(2, files.length); // B and C should be filtered out

        assertListOfFiles("test/buildlist/", new String[] {"A", "D"}, files);
    }

    public void testWithLeafAndOnlyDirectDep() {
        FileSet fs = new FileSet();
        fs.setDir(new File("test/buildlist"));
        fs.setIncludes("**/build.xml");
        fs.setExcludes("E2/**");

        buildlist.addFileset(fs);
        buildlist.setOnMissingDescriptor("skip");
        buildlist.setLeaf("C");
        buildlist.setOnlydirectdep(true);

        String[] files = getFiles(buildlist);

        assertEquals(2, files.length); // We must have only A and C

        assertListOfFiles("test/buildlist/", new String[] {"C", "A"}, files);
    }

    public void testRestartFrom() {
        FileSet fs = new FileSet();
        fs.setDir(new File("test/buildlist"));
        fs.setIncludes("**/build.xml");
        fs.setExcludes("E2/build.xml,F/build.xml,G/build.xml");

        buildlist.addFileset(fs);
        buildlist.setOnMissingDescriptor("skip");
        buildlist.setRestartFrom("C");

        String[] files = getFiles(buildlist);

        assertEquals(4, files.length);

        assertListOfFiles("test/buildlist/", new String[] {"C", "A", "D", "E"}, files);
    }

    public void testOnMissingDescriptor() {
        FileSet fs = new FileSet();
        fs.setDir(new File("test/buildlist"));
        fs.setIncludes("**/build.xml");
        fs.setExcludes("E2/build.xml,F/build.xml,G/build.xml");

        buildlist.addFileset(fs);
        buildlist.setOnMissingDescriptor(new String("tail")); // IVY-805: new String instance

        String[] files = getFiles(buildlist);

        assertEquals(6, files.length);

        assertListOfFiles("test/buildlist/", new String[] {"B", "C", "A", "D", "E", "H"}, files);
    }

    public void testOnMissingDescriptor2() {
        FileSet fs = new FileSet();
        fs.setDir(new File("test/buildlist"));
        fs.setIncludes("**/build.xml");
        fs.setExcludes("E2/build.xml,F/build.xml,G/build.xml");

        buildlist.addFileset(fs);
        buildlist.setOnMissingDescriptor(new String("skip")); // IVY-805: new String instance

        String[] files = getFiles(buildlist);

        assertEquals(5, files.length);

        assertListOfFiles("test/buildlist/", new String[] {"B", "C", "A", "D", "E"}, files);
    }

    public void testWithModuleWithSameNameAndDifferentOrg() {
        FileSet fs = new FileSet();
        fs.setDir(new File("test/buildlist"));
        fs.setIncludes("**/build.xml");
        fs.setExcludes("F/build.xml,G/build.xml");

        buildlist.addFileset(fs);
        buildlist.setOnMissingDescriptor("skip");

        String[] files = getFiles(buildlist);

        assertEquals(6, files.length);

        assertListOfFiles("test/buildlist/", new String[] {"B", "C", "A", "D"}, files);

        // the order of E and E2 is undefined
        List other = new ArrayList();
        other.add(new File(files[4]).getAbsoluteFile().toURI());
        other.add(new File(files[5]).getAbsoluteFile().toURI());
        Collections.sort(other);

        assertEquals(new File("test/buildlist/E/build.xml").getAbsoluteFile().toURI(), other.get(0));
        assertEquals(new File("test/buildlist/E2/build.xml").getAbsoluteFile().toURI(),
            other.get(1));
    }

    public void testNoParents() {
        FileSet fs = new FileSet();
        fs.setDir(new File("test/buildlists/testNoParents"));
        fs.setIncludes("**/build.xml");

        buildlist.addFileset(fs);
        buildlist.setOnMissingDescriptor("skip");
        buildlist.setHaltonerror(false);

        String[] files = getFiles(buildlist);

        assertEquals(5, files.length);

        assertListOfFiles("test/buildlists/testNoParents/", new String[] {"bootstrap-parent",
                "ireland", "germany", "master-parent", "croatia"}, files);
    }

    public void testOneParent() {
        FileSet fs = new FileSet();
        fs.setDir(new File("test/buildlists/testOneParent"));
        fs.setIncludes("**/build.xml");

        buildlist.addFileset(fs);
        buildlist.setOnMissingDescriptor("skip");
        buildlist.setHaltonerror(false);

        String[] files = getFiles(buildlist);

        assertEquals(5, files.length);

        assertListOfFiles("test/buildlists/testOneParent/", new String[] {"bootstrap-parent",
                "master-parent", "croatia", "ireland", "germany"}, files);
    }

    public void testTwoParents() {
        FileSet fs = new FileSet();
        fs.setDir(new File("test/buildlists/testTwoParents"));
        fs.setIncludes("**/build.xml");

        buildlist.addFileset(fs);
        buildlist.setOnMissingDescriptor("skip");
        buildlist.setHaltonerror(false);

        String[] files = getFiles(buildlist);

        assertEquals(5, files.length);

        assertListOfFiles("test/buildlists/testTwoParents/", new String[] {"bootstrap-parent",
                "master-parent", "croatia", "ireland", "germany"}, files);
    }

    public void testRelativePathToParent() {
        FileSet fs = new FileSet();
        fs.setDir(new File("test/buildlists/testRelativePathToParent"));
        fs.setIncludes("**/build.xml");

        buildlist.addFileset(fs);
        buildlist.setOnMissingDescriptor("skip");
        buildlist.setHaltonerror(false);

        String[] files = getFiles(buildlist);

        assertEquals(5, files.length);

        assertListOfFiles("test/buildlists/testRelativePathToParent/", new String[] {
                "bootstrap-parent", "master-parent", "croatia", "ireland", "germany"}, files);
    }

    public void testAbsolutePathToParent() {
        project.setProperty("master-parent.dir", new File(
                "test/buildlists/testAbsolutePathToParent/master-parent").getAbsolutePath());

        FileSet fs = new FileSet();
        fs.setDir(new File("test/buildlists/testAbsolutePathToParent"));
        fs.setIncludes("**/build.xml");

        buildlist.addFileset(fs);
        buildlist.setOnMissingDescriptor("skip");
        buildlist.setHaltonerror(false);

        String[] files = getFiles(buildlist);

        assertEquals(5, files.length);

        assertListOfFiles("test/buildlists/testAbsolutePathToParent/", new String[] {
                "bootstrap-parent", "master-parent", "croatia", "ireland", "germany"}, files);
    }

}
// CheckStyle:MagicNumber| ON