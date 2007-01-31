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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.util.FileUtil;

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
        resolvedRevisions.put(ModuleRevisionId.newInstance("yourorg", "yourmodule2", "2+"), "2.5");
        resolvedRevisions.put(ModuleRevisionId.newInstance("yourorg", "yourmodule6", "latest.integration"), "6.3");
        
        GregorianCalendar cal = new GregorianCalendar();
        cal.set(2005, 2, 22, 14, 32, 54);
        
        Ivy ivy = Ivy.newInstance();
        ivy.setVariable("myvar", "myconf1");
        XmlModuleDescriptorUpdater.update(ivy.getSettings(), 
                XmlModuleUpdaterTest.class.getResource("test-update.xml"), 
                dest, resolvedRevisions, "release", "mynewrev", cal.getTime(), null, true);
        
        assertTrue(dest.exists());
        String expected = FileUtil.readEntirely(new BufferedReader(new InputStreamReader(XmlModuleUpdaterTest.class.getResourceAsStream("updated.xml"))));
        String updated = FileUtil.readEntirely(new BufferedReader(new FileReader(dest)));
        assertEquals(expected, updated);
    }
    
    public void testUpdateWithImportedMappingOverride() throws Exception {
       ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        XmlModuleDescriptorUpdater.update(new IvySettings(), 
                XmlModuleUpdaterTest.class.getResourceAsStream("test-configurations-import4.xml"), 
                buffer, new HashMap(), "release", "mynewrev", new Date(), null, true);
       
        String updatedXml = buffer.toString();
        
        // just make sure that 'confmappingoverride="true"' is declared somewhere in the XML.
        assertTrue("Updated XML doesn't define the confmappingoverride attribute", updatedXml.indexOf("confmappingoverride=\"true\"") != -1);
    }
    
}
