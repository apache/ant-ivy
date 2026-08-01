/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivy.plugins.parser.m2;

import java.io.File;

import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.parser.ModuleDescriptorParserRegistry;
import org.apache.ivy.util.FileUtil;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class PomModuleDescriptorWriterTest {

    private ModuleDescriptor readDescriptor(String resourceName) {
        try {
            return ModuleDescriptorParserRegistry.getInstance().parseDescriptor(
                new IvySettings(), getClass().getResource(resourceName), false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String readResourceContents(String resourceName) {
        try {
            return FileUtil.readEntirely(getClass().getResourceAsStream(resourceName));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String writeDescriptor(ModuleDescriptor md,
            PomWriterOptions options) {
        File pom = new File("build/test/test-write.pom");
        assertTrue(FileUtil.forceDelete(pom));
        if (!pom.getParentFile().exists()) {
            pom.getParentFile().mkdirs();
        }
        try {
            String license = readResourceContents("license.xml");

            PomModuleDescriptorWriter.write(md, pom, options.setLicenseHeader(license).setPrintIvyInfo(false));

            String output = FileUtil.readEntirely(pom);
            output = output.replace("\r\n", "\n");
            output = output.replace('\r', '\n');
            return output;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            FileUtil.forceDelete(pom);
        }
    }

    //--------------------------------------------------------------------------

    @Test
    public void testSimple() {
        ModuleDescriptor md = readDescriptor("test-simple.pom");

        String actual = writeDescriptor(md, new PomWriterOptions());

        assertEquals(readResourceContents("test-write-simple.xml"), actual);
    }

    @Test
    public void testSimpleDependencies() {
        ModuleDescriptor md = readDescriptor("test-dependencies.pom");

        String actual = writeDescriptor(md, new PomWriterOptions());

        assertEquals(readResourceContents("test-write-simple-dependencies.xml"), actual);
    }

    /**
     * Test case for <a href="https://issues.apache.org/jira/browse/IVY-1655">IVY-1655</a>.
     */
    @Test
    public void testDependenciesWithRange() {
        ModuleDescriptor md = readDescriptor("test-dependencies-with-range.xml");

        String actual = writeDescriptor(md, new PomWriterOptions());

        assertEquals(readResourceContents("test-write-dependencies-with-range.xml"), actual);
    }

    @Test
    public void testDependenciesWithScope() {
        ModuleDescriptor md = readDescriptor("test-dependencies-with-scope.pom");

        String actual = writeDescriptor(md, new PomWriterOptions());

        assertEquals(readResourceContents("test-write-dependencies-with-scope.xml"), actual);
    }

    @Test
    public void testDependenciesWithType() {
        ModuleDescriptor md = readDescriptor("test-dependencies-with-type.pom");

        String actual = writeDescriptor(md, new PomWriterOptions());

        assertEquals(readResourceContents("test-write-dependencies-with-type.xml"), actual);
    }

    @Test
    public void testDependenciesWithClassifier() {
        ModuleDescriptor md = readDescriptor("test-dependencies-with-classifier.pom");

        String actual = writeDescriptor(md, new PomWriterOptions());

        assertEquals(readResourceContents("test-write-dependencies-with-classifier.xml"), actual);
    }

    @Test
    public void testOptional() {
        ModuleDescriptor md = readDescriptor("test-optional.pom");

        String actual = writeDescriptor(md, new PomWriterOptions());

        assertEquals(readResourceContents("test-write-dependencies-optional.xml"), actual);
    }

    @Test
    public void testTransitive() {
        ModuleDescriptor md = readDescriptor("test-transitive.xml");

        String actual = writeDescriptor(md, new PomWriterOptions());

        assertEquals(readResourceContents("test-transitive.pom"), actual);
    }

    @Test
    public void testPackaging() {
        ModuleDescriptor md = readDescriptor("test-packaging.pom");

        String actual = writeDescriptor(md, new PomWriterOptions());

        assertEquals(readResourceContents("test-write-packaging.xml"), actual);
    }

    @Test
    public void testWriteCompileConfigurationOnly() {
        ModuleDescriptor md = readDescriptor("test-dependencies-with-scope.pom");

        String actual = writeDescriptor(md, new PomWriterOptions().setConfs(new String[] {"compile"}));

        assertEquals(readResourceContents("test-write-compile-dependencies.xml"), actual);
    }

    @Test
    public void testWriteRuntimeConfigurationOnly() {
        ModuleDescriptor md = readDescriptor("test-dependencies-with-scope.pom");

        String actual = writeDescriptor(md, new PomWriterOptions().setConfs(new String[] {"runtime"}));

        assertEquals(readResourceContents("test-write-dependencies-with-scope.xml"), actual);
    }

    @Test
    public void testWriteAllConfiguration() {
        ModuleDescriptor md = readDescriptor("test-dependencies-with-scope.pom");

        String actual = writeDescriptor(md, new PomWriterOptions().setConfs(new String[] {"*"}));

        assertEquals(readResourceContents("test-write-dependencies-with-scope.xml"), actual);
    }

    @Test
    public void testWriteAllExceptRuntimeConfiguration() {
        ModuleDescriptor md = readDescriptor("test-dependencies-with-scope.pom");

        String actual = writeDescriptor(md, new PomWriterOptions().setConfs(new String[] {"*", "!runtime"}));

        assertEquals(readResourceContents("test-write-compile-dependencies.xml"), actual);
    }
}
