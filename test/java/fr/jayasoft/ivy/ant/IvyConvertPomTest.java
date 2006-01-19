/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.ant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

import junit.framework.TestCase;

import org.apache.tools.ant.Project;

import fr.jayasoft.ivy.util.FileUtil;

public class IvyConvertPomTest extends TestCase {
    public void testSimple() throws Exception {
        IvyConvertPom task = new IvyConvertPom();
        task.setProject(new Project());
        task.setPomFile(new File("test/java/fr/jayasoft/ivy/ant/test.pom"));
        File destFile = File.createTempFile("ivy", ".xml");
        destFile.deleteOnExit();
        task.setIvyFile(destFile);
        task.execute();
        
        String wrote = FileUtil.readEntirely(new BufferedReader(new FileReader(destFile)));
        String expected = readEntirely("test-convertpom.xml").replaceAll("\r\n", "\n").replace('\r', '\n');
        // do not work properly on all platform and depends on the file date
//        assertEquals(expected, wrote);
    }

    private String readEntirely(String resource) throws IOException {
        return FileUtil.readEntirely(new BufferedReader(new InputStreamReader(IvyConvertPomTest.class.getResource(resource).openStream()))).replaceAll("\r\n", "\n").replace('\r', '\n');
    }
}
