/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.xml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.GregorianCalendar;

import junit.framework.TestCase;
import fr.jayasoft.ivy.DefaultModuleDescriptor;
import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.ModuleDescriptor;
import fr.jayasoft.ivy.util.FileUtil;

public class XmlModuleDescriptorWriterTest extends TestCase {
    private File _dest = new File("build/test/test-write.xml");

    public void testSimple() throws Exception {
        DefaultModuleDescriptor md = (DefaultModuleDescriptor)XmlModuleDescriptorParser.getInstance().parseDescriptor(new Ivy(), XmlModuleDescriptorWriterTest.class.getResource("test-simple.xml"), true);
        md.setResolvedPublicationDate(new GregorianCalendar(2005, 4, 1, 11, 0, 0).getTime());
        XmlModuleDescriptorWriter.write(md, _dest);
        
        assertTrue(_dest.exists());
        String wrote = FileUtil.readEntirely(new BufferedReader(new FileReader(_dest))).replaceAll("\r\n", "\n").replace('\r', '\n');
        String expected = readEntirely("test-write-simple.xml").replaceAll("\r\n", "\n").replace('\r', '\n');
        assertEquals(expected, wrote);
    }
    
    public void testDependencies() throws Exception {
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(new Ivy(), XmlModuleDescriptorWriterTest.class.getResource("test-dependencies.xml"), true);
        XmlModuleDescriptorWriter.write(md, _dest);
        
        assertTrue(_dest.exists());
        String wrote = FileUtil.readEntirely(new BufferedReader(new FileReader(_dest))).replaceAll("\r\n", "\n").replace('\r', '\n');
        String expected = readEntirely("test-write-dependencies.xml").replaceAll("\r\n", "\n").replace('\r', '\n');
        assertEquals(expected, wrote);
    }
    
    
    public void testFull() throws Exception {
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(new Ivy(), XmlModuleDescriptorWriterTest.class.getResource("test.xml"), true);
        XmlModuleDescriptorWriter.write(md, _dest);
        
        assertTrue(_dest.exists());
        String wrote = FileUtil.readEntirely(new BufferedReader(new FileReader(_dest))).replaceAll("\r\n", "\n").replace('\r', '\n');
        String expected = readEntirely("test-write-full.xml").replaceAll("\r\n", "\n").replace('\r', '\n');
        assertEquals(expected, wrote);
    }
    
    

    private String readEntirely(String resource) throws IOException {
        return FileUtil.readEntirely(new BufferedReader(new InputStreamReader(XmlModuleDescriptorWriterTest.class.getResource(resource).openStream())));
    }

    public void setUp() {
        if (_dest.exists()) {
            _dest.delete();
        }
        if (!_dest.getParentFile().exists()) {
            _dest.getParentFile().mkdirs();
        }
    }
    
    protected void tearDown() throws Exception {
        if (_dest.exists()) {
            _dest.delete();
        }
    }
}
