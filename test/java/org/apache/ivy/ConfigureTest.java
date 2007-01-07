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
package org.apache.ivy;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

import org.apache.ivy.DependencyResolver;
import org.apache.ivy.Ivy;

import junit.framework.TestCase;

public class ConfigureTest extends TestCase {
    public void testDefault() throws ParseException, IOException {
        Ivy ivy = new Ivy();
        ivy.configureDefault();
        
        assertNotNull(ivy.getDefaultResolver());
    }
    
    public void testTypedefWithCustomClasspath() throws Exception {
        Ivy ivy = new Ivy();
        ivy.setVariable("ivy.custom.test.dir", new File("test/java/org/apache/ivy").toURL().toString());
        ivy.configure(ConfigureTest.class.getResource("ivyconf-custom-typedef.xml"));
        
        DependencyResolver custom = ivy.getResolver("custom");
        assertNotNull(custom);
        assertEquals("org.apache.ivy.resolver.CustomResolver", custom.getClass().getName());
    }

    public void testTypedefWithCustomClasspathWithFile() throws Exception {
        Ivy ivy = new Ivy();
        ivy.setVariable("ivy.custom.test.dir", new File("test/java/org/apache/ivy").toString());
        ivy.configure(ConfigureTest.class.getResource("ivyconf-custom-typedef2.xml"));
        
        DependencyResolver custom = ivy.getResolver("custom");
        assertNotNull(custom);
        assertEquals("org.apache.ivy.resolver.CustomResolver", custom.getClass().getName());
    }

}
