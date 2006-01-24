/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.xml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.ModuleId;
import fr.jayasoft.ivy.util.FileUtil;

public class XmlModuleUpdaterTest extends TestCase {
    
    public void testUpdate() throws Exception {
        /*
         * For updated file to be equals to updated.xml,
         * we have to fix the line separator to the one used
         * in updated.xml, in order for this test to works in
         * all platforms (default line separator used in 
         * updater being platform dependent 
         */
        XmlModuleDescriptorUpdater.LINE_SEPARATOR = "\n";
        File dest = new File("build/updated-test.xml");
        dest.deleteOnExit();
        Map resolvedRevisions = new HashMap();
        resolvedRevisions.put(new ModuleId("yourorg", "yourmodule2"), "2.5");
        resolvedRevisions.put(new ModuleId("yourorg", "yourmodule6"), "6.3");
        
        GregorianCalendar cal = new GregorianCalendar();
        cal.set(2005, 2, 22, 14, 32, 54);
        
        Ivy ivy = new Ivy();
        ivy.setVariable("myvar", "myconf1");
        XmlModuleDescriptorUpdater.update(ivy, 
                XmlModuleUpdaterTest.class.getResource("test-update.xml"), 
                dest, resolvedRevisions, "release", "mynewrev", cal.getTime(), null, true);
        
        assertTrue(dest.exists());
        String expected = FileUtil.readEntirely(new BufferedReader(new InputStreamReader(XmlModuleUpdaterTest.class.getResourceAsStream("updated.xml"))));
        String updated = FileUtil.readEntirely(new BufferedReader(new FileReader(dest)));
        assertEquals(expected, updated);
    }
}
