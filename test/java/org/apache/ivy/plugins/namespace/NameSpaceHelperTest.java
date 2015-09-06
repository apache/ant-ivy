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
package org.apache.ivy.plugins.namespace;

import java.util.Collections;
import java.util.Date;

import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.id.ArtifactRevisionId;
import org.apache.ivy.core.module.id.ModuleRevisionId;

import junit.framework.TestCase;

public class NameSpaceHelperTest extends TestCase {
    public void testTransformArtifactWithExtraAttributes() throws Exception {
        Artifact artifact = new DefaultArtifact(ArtifactRevisionId.newInstance(
            ModuleRevisionId.parse("org.apache#test;1.0"), "test", "jar", "jar",
            Collections.singletonMap("m:qualifier", "sources")), new Date(), null, false);

        MRIDTransformationRule r = new MRIDTransformationRule();
        r.addSrc(new MRIDRule("org.apache", "test", null));
        r.addDest(new MRIDRule("apache", "test", null));

        Artifact transformed = NameSpaceHelper.transform(artifact, r);
        assertEquals("apache#test;1.0", transformed.getModuleRevisionId().toString());
        assertEquals(Collections.singletonMap("m:qualifier", "sources"),
            transformed.getQualifiedExtraAttributes());
    }
}
