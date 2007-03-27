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
package org.apache.ivy.plugins.parser.xml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.GregorianCalendar;

import junit.framework.TestCase;

import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.util.FileUtil;

public class XmlModuleDescriptorWriterTest extends TestCase {
    private static String LICENSE;
    static {
    	try {
			LICENSE = FileUtil.readEntirely(new BufferedReader(new InputStreamReader(XmlModuleDescriptorWriterTest.class.getResourceAsStream("license.xml"))));
		} catch (IOException e) {
			e.printStackTrace();
		} 
    }
    private File _dest = new File("build/test/test-write.xml");

    public void testSimple() throws Exception {
        DefaultModuleDescriptor md = (DefaultModuleDescriptor)XmlModuleDescriptorParser.getInstance().parseDescriptor(new IvySettings(), XmlModuleDescriptorWriterTest.class.getResource("test-simple.xml"), true);
        md.setResolvedPublicationDate(new GregorianCalendar(2005, 4, 1, 11, 0, 0).getTime());
        md.setResolvedModuleRevisionId(new ModuleRevisionId(md.getModuleRevisionId().getModuleId(), "NONE"));
        XmlModuleDescriptorWriter.write(md, LICENSE, _dest);
        
        assertTrue(_dest.exists());
        String wrote = FileUtil.readEntirely(new BufferedReader(new FileReader(_dest))).replaceAll("\r\n", "\n").replace('\r', '\n');
        String expected = readEntirely("test-write-simple.xml").replaceAll("\r\n", "\n").replace('\r', '\n');
        assertEquals(expected, wrote);
    }
    
    public void testDependencies() throws Exception {
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(new IvySettings(), XmlModuleDescriptorWriterTest.class.getResource("test-dependencies.xml"), true);
        XmlModuleDescriptorWriter.write(md, LICENSE, _dest);
        
        assertTrue(_dest.exists());
        String wrote = FileUtil.readEntirely(new BufferedReader(new FileReader(_dest))).replaceAll("\r\n", "\n").replace('\r', '\n');
        String expected = readEntirely("test-write-dependencies.xml").replaceAll("\r\n", "\n").replace('\r', '\n');
        assertEquals(expected, wrote);
    }
    
    
    public void testFull() throws Exception {
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(new IvySettings(), XmlModuleDescriptorWriterTest.class.getResource("test.xml"), true);
        XmlModuleDescriptorWriter.write(md, LICENSE, _dest);
        
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
