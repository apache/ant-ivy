/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.ant;

import org.apache.tools.ant.Project;

import fr.jayasoft.ivy.Ivy;

import junit.framework.TestCase;

public class IvyVarTest extends TestCase {
    public void testSimple() {
        IvyVar task = new IvyVar();
        task.setProject(new Project());
        task.setName("mytest");
        task.setValue("myvalue");
        task.execute();
        Ivy ivy  = task.getIvyInstance();
        assertNotNull(ivy);
        assertEquals("myvalue", ivy.getVariable("mytest"));
    }
    
    public void testPrefix() {
        IvyVar task = new IvyVar();
        task.setProject(new Project());
        task.setName("mytest");
        task.setValue("myvalue");
        task.setPrefix("myprefix");
        task.execute();
        Ivy ivy  = task.getIvyInstance();
        assertNotNull(ivy);
        assertEquals("myvalue", ivy.getVariable("myprefix.mytest"));
    }
    
    public void testURL() {
        IvyVar task = new IvyVar();
        task.setProject(new Project());
        task.setUrl(IvyVarTest.class.getResource("vartest.properties").toExternalForm());
        task.execute();
        Ivy ivy  = task.getIvyInstance();
        assertNotNull(ivy);
        assertEquals("myvalue1", ivy.getVariable("mytest1"));
        assertEquals("myvalue2", ivy.getVariable("mytest2"));
    }

    public void testURLPrefix() {
        IvyVar task = new IvyVar();
        task.setProject(new Project());
        task.setUrl(IvyVarTest.class.getResource("vartest.properties").toExternalForm());
        task.setPrefix("myprefix.");
        task.execute();
        Ivy ivy  = task.getIvyInstance();
        assertNotNull(ivy);
        assertEquals("myvalue1", ivy.getVariable("myprefix.mytest1"));
        assertEquals("myvalue2", ivy.getVariable("myprefix.mytest2"));
    }
}
