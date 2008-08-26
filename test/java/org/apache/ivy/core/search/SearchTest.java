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
package org.apache.ivy.core.search;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.plugins.resolver.IBiblioResolver;

public class SearchTest extends TestCase {
    public void testListInMavenRepo() throws Exception {
        Ivy ivy = Ivy.newInstance();
        ivy.configure(new File("test/repositories/m2/ivysettings.xml").toURL());
        
        Map otherTokenValues = new HashMap();
        otherTokenValues.put(IvyPatternHelper.ORGANISATION_KEY, "org.apache");
        otherTokenValues.put(IvyPatternHelper.MODULE_KEY, "test-metadata");
        String[] revs = ivy.listTokenValues(IvyPatternHelper.REVISION_KEY, otherTokenValues);
        
        assertEquals(
            new HashSet(Arrays.asList(new String[] {"1.0","1.1"})), 
            new HashSet(Arrays.asList(revs)));
    }

    public void testListInMavenRepo2() throws Exception {
        Ivy ivy = Ivy.newInstance();
        ivy.configure(new File("test/repositories/m2/ivysettings.xml").toURL());
        ((IBiblioResolver) ivy.getSettings().getResolver("m2")).setUseMavenMetadata(false);
        
        Map otherTokenValues = new HashMap();
        otherTokenValues.put(IvyPatternHelper.ORGANISATION_KEY, "org.apache");
        otherTokenValues.put(IvyPatternHelper.MODULE_KEY, "test-metadata");
        String[] revs = ivy.listTokenValues(IvyPatternHelper.REVISION_KEY, otherTokenValues);
        
        assertEquals(
            new HashSet(Arrays.asList(new String[] {"1.0","1.1","1.2"})), 
            new HashSet(Arrays.asList(revs)));
    }
}
