/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.ant;

import java.io.File;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;

import junit.framework.TestCase;

public class IvyBuildListTest extends TestCase {

    public void testSimple() {
        Project p = new Project();
        
        IvyBuildList buildlist = new IvyBuildList();
        buildlist.setProject(p);
        
        FileSet fs = new FileSet();
        fs.setDir(new File("test/buildlist"));
        fs.setIncludes("**/build.xml");
        buildlist.addFileset(fs);
        
        buildlist.setReference("ordered.build.files");
        
        buildlist.execute();
        
        Object o = p.getReference("ordered.build.files");
        assertNotNull(o);
        assertTrue(o instanceof Path);
        
        Path path = (Path)o;
        String[] files = path.list();
        assertNotNull(files);
        assertEquals(3, files.length);
        
        assertEquals(new File("test/buildlist/B/build.xml").getAbsolutePath(), new File(files[0]).getAbsolutePath());
        assertEquals(new File("test/buildlist/C/build.xml").getAbsolutePath(), new File(files[1]).getAbsolutePath());
        assertEquals(new File("test/buildlist/A/build.xml").getAbsolutePath(), new File(files[2]).getAbsolutePath());
    }

    public void testReverse() {
        Project p = new Project();
        
        IvyBuildList buildlist = new IvyBuildList();
        buildlist.setProject(p);
		buildlist.setReverse(true);
        
        FileSet fs = new FileSet();
        fs.setDir(new File("test/buildlist"));
        fs.setIncludes("**/build.xml");
        buildlist.addFileset(fs);
        
        buildlist.setReference("reverse.ordered.build.files");
        
        buildlist.execute();
        
        Object o = p.getReference("reverse.ordered.build.files");
        assertNotNull(o);
        assertTrue(o instanceof Path);
        
        Path path = (Path)o;
        String[] files = path.list();
        assertNotNull(files);
        assertEquals(3, files.length);
        
        assertEquals(new File("test/buildlist/A/build.xml").getAbsolutePath(), new File(files[0]).getAbsolutePath());
        assertEquals(new File("test/buildlist/C/build.xml").getAbsolutePath(), new File(files[1]).getAbsolutePath());
        assertEquals(new File("test/buildlist/B/build.xml").getAbsolutePath(), new File(files[2]).getAbsolutePath());
    }

}
