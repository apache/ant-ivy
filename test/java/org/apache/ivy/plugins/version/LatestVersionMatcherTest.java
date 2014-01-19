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
package org.apache.ivy.plugins.version;

import junit.framework.TestCase;

import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.module.status.Status;
import org.apache.ivy.core.module.status.StatusManager;

public class LatestVersionMatcherTest extends TestCase {
    private LatestVersionMatcher vm = new LatestVersionMatcher();

    protected void setUp() {
        IvyContext.pushNewContext();
    }

    protected void tearDown() {
        IvyContext.popContext();
    }

    public void testNeedModuleDescriptorStandardStatus() throws Exception {
        assertNeed("latest.release", true);
        assertNeed("latest.milestone", true);
        assertNeed("latest.integration", false);
    }

    public void testNeedModuleDescriptorCustomStatus() throws Exception {
        StatusManager.getCurrent().addStatus(new Status("release", false));
        StatusManager.getCurrent().addStatus(new Status("snapshot", true));

        assertNeed("latest.release", true);
        assertNeed("latest.snapshot", false);
    }

    // assertion helper methods
    private void assertNeed(String askedVersion, boolean b) {
        assertEquals(b, vm.needModuleDescriptor(
            ModuleRevisionId.newInstance("org", "name", askedVersion), null));
    }
}
