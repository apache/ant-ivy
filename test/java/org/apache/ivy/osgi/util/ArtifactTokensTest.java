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
package org.apache.ivy.osgi.util;

import junit.framework.TestCase;

public class ArtifactTokensTest extends TestCase {

    public void testGoodMatching() {
        final String repoResource = "java/test-ivy/osgi/eclipse/plugins/org.eclipse.datatools.connectivity.ui_1.0.1.v200808121010";
        final ArtifactTokens tokens = new ArtifactTokens(repoResource);
        assertEquals("java/test-ivy/osgi/eclipse/plugins/", tokens.prefix);
        assertEquals("org.eclipse.datatools.connectivity.ui", tokens.module);
        assertEquals("1.0.1", tokens.version.numbersAsString());
        assertEquals("v200808121010", tokens.version.qualifier());
        assertFalse(tokens.isJar);
    }

    public void testGoodMatching2() {
        final String repoResource = "java/test-ivy/osgi/eclipse/plugins/org.eclipse.datatools.connectivity.ui_1.0.1";
        final ArtifactTokens tokens = new ArtifactTokens(repoResource);
        assertEquals("java/test-ivy/osgi/eclipse/plugins/", tokens.prefix);
        assertEquals("org.eclipse.datatools.connectivity.ui", tokens.module);
        assertEquals("1.0.1", tokens.version.numbersAsString());
        assertEquals("", tokens.version.qualifier());
        assertFalse(tokens.isJar);
    }

    public void testGoodMatching3() {
        final String repoResource = "java/test-ivy/osgi/eclipse/plugins/org.myorg.module.one_3.21.100.v20070530";
        final ArtifactTokens tokens = new ArtifactTokens(repoResource);
        assertEquals("java/test-ivy/osgi/eclipse/plugins/", tokens.prefix);
        assertEquals("org.myorg.module.one", tokens.module);
        assertEquals("3.21.100", tokens.version.numbersAsString());
        assertEquals("v20070530", tokens.version.qualifier());
        assertFalse(tokens.isJar);
    }

    public void testGoodMatching4() {
        final String repoResource = "java/test-ivy/osgi/eclipse/plugins/org.eclipse.mylyn.tasks.ui_3.0.1.v20080721-2100-e33.jar";
        // String repoResource =
        // "java/test-ivy/osgi/eclipse/plugins/org.eclipse.mylyn.tasks.ui_3.0.1.v20080721.jar";
        final ArtifactTokens tokens = new ArtifactTokens(repoResource);
        assertEquals("java/test-ivy/osgi/eclipse/plugins/", tokens.prefix);
        assertEquals("org.eclipse.mylyn.tasks.ui", tokens.module);
        assertEquals("3.0.1", tokens.version.numbersAsString());
        assertEquals("v20080721-2100-e33", tokens.version.qualifier());
        assertTrue(tokens.isJar);
    }

    public void testBadMatching() {
        final String repoResource = "java/test-ivy/osgi/eclipse/plugins/fake";
        final ArtifactTokens tokens = new ArtifactTokens(repoResource);
        assertNull(tokens.prefix);
        assertNull(tokens.module);
        assertNull(tokens.version);
    }

}
