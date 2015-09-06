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

import org.apache.ivy.core.module.id.ModuleRevisionId;

import junit.framework.TestCase;

public class MRIDTransformationRuleTest extends TestCase {

    public void testTransformation() {
        MRIDTransformationRule r = new MRIDTransformationRule();
        r.addSrc(new MRIDRule("apache", "commons.+", null));
        r.addDest(new MRIDRule("$m0", "$m0", null));

        assertEquals(ModuleRevisionId.newInstance("commons-client", "commons-client", "1.0"),
            r.transform(ModuleRevisionId.newInstance("apache", "commons-client", "1.0")));
        assertEquals(ModuleRevisionId.newInstance("apache", "module", "1.0"),
            r.transform(ModuleRevisionId.newInstance("apache", "module", "1.0")));

        r = new MRIDTransformationRule();
        r.addSrc(new MRIDRule(null, "commons\\-(.+)", null));
        r.addDest(new MRIDRule("$o0.commons", "$m1", null));

        assertEquals(ModuleRevisionId.newInstance("apache.commons", "client", "1.0"),
            r.transform(ModuleRevisionId.newInstance("apache", "commons-client", "1.0")));
        assertEquals(ModuleRevisionId.newInstance("apache", "module", "1.0"),
            r.transform(ModuleRevisionId.newInstance("apache", "module", "1.0")));

        r = new MRIDTransformationRule();
        r.addSrc(new MRIDRule("(.+)\\.(.+)", ".+", null));
        r.addDest(new MRIDRule("$o1", "$o2-$m0", null));

        assertEquals(ModuleRevisionId.newInstance("apache", "commons-client", "1.0"),
            r.transform(ModuleRevisionId.newInstance("apache.commons", "client", "1.0")));
        assertEquals(ModuleRevisionId.newInstance("apache", "module", "1.0"),
            r.transform(ModuleRevisionId.newInstance("apache", "module", "1.0")));
    }
}
