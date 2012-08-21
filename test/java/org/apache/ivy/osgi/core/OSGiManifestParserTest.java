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
package org.apache.ivy.osgi.core;

import java.io.File;
import java.util.Arrays;

import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.parser.AbstractModuleDescriptorParserTester;
import org.apache.ivy.util.DefaultMessageLogger;
import org.apache.ivy.util.Message;

public class OSGiManifestParserTest extends AbstractModuleDescriptorParserTester {

    private IvySettings settings;

    protected void setUp() throws Exception {
        Message.setDefaultLogger(new DefaultMessageLogger(Message.MSG_WARN));

        settings = new IvySettings();
        // prevent test from polluting local cache
        settings.setDefaultCache(new File("build/cache"));
    }

    public void testSimple() throws Exception {
        ModuleDescriptor md = OSGiManifestParser.getInstance().parseDescriptor(settings,
            getClass().getResource("MANIFEST_classpath.MF"), true);
        assertNotNull(md);
        assertEquals("bundle", md.getModuleRevisionId().getOrganisation());
        assertEquals("org.apache.ivy.test", md.getModuleRevisionId().getName());
        assertEquals("1.0.0", md.getModuleRevisionId().getRevision());

        assertNotNull(md.getConfigurations());
        assertEquals(
            Arrays.asList(new Configuration[] {new Configuration("default"),
                    new Configuration("optional"), new Configuration("transitive-optional")}),
            Arrays.asList(md.getConfigurations()));

        assertEquals(0, md.getAllArtifacts().length);

        assertNotNull(md.getDependencies());
        assertEquals(0, md.getDependencies().length);
    }
}
