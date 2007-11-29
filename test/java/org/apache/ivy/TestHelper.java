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
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

import junit.framework.Assert;

import org.apache.ivy.core.cache.CacheManager;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.id.ModuleRevisionId;

public class TestHelper {

    public static File getArchiveFileInCache(Ivy ivy, File cache, String organisation,
            String module, String revision, String artifact, String type, String ext) {
        return getArchiveFileInCache(ivy.getCacheManager(cache), organisation, module, revision,
            artifact, type, ext);
    }

    public static File getArchiveFileInCache(CacheManager cacheManager, String organisation,
            String module, String revision, String artifact, String type, String ext) {
        return cacheManager.getArchiveFileInCache(new DefaultArtifact(ModuleRevisionId.newInstance(
            organisation, module, revision), new Date(), artifact, type, ext));
    }

    /**
     * Assertion utility methods to test if a collection of {@link ModuleRevisionId} matches a given
     * expected set of mrids.
     * <p>
     * Expected mrids is given as a String of comma separated string representations of
     * {@link ModuleRevisionId}.
     * 
     * @param expectedMrids
     *            the expected set of mrids
     * @param mrids
     *            the3 mrids to test
     */
    public static void assertModuleRevisionIds(String expectedMrids,
            Collection/* <ModuleRevisionId> */mrids) {
        Collection expected = parseMrids(expectedMrids);
        Assert.assertEquals(expected, mrids);
    }

    /**
     * Returns a Set of {@link ModuleRevisionId} corresponding to the given comma separated list of
     * their text representation.
     * 
     * @param mrids
     *            the text representation of the {@link ModuleRevisionId}
     * @return a collection of {@link ModuleRevisionId}
     */
    public static Collection parseMrids(String mrids) {
        String[] m = mrids.split(", ");
        Collection c = new HashSet();
        for (int i = 0; i < m.length; i++) {
            c.add(ModuleRevisionId.parse(m[i]));
        }
        return c;
    }
    
    /**
     * Returns an array of {@link ModuleRevisionId} corresponding to the given comma separated list of
     * their text representation.
     * 
     * @param mrids
     *            the text representation of the {@link ModuleRevisionId}
     * @return an array of {@link ModuleRevisionId}
     */
    public static ModuleRevisionId[] parseMridsToArray(String mrids) {
        Collection parsedMrids = parseMrids(mrids);
        return (ModuleRevisionId[]) parsedMrids.toArray(new ModuleRevisionId[parsedMrids.size()]);
    }
}
