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

import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.module.status.Status;
import org.apache.ivy.core.module.status.StatusManager;

import junit.framework.TestCase;

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

    public void testAcceptForStandardStatus() throws Exception {
        assertAccept("latest.release", "release", true);
        assertAccept("latest.release", "milestone", false);
        assertAccept("latest.release", "integration", false);
    }

    public void testAcceptForSameBranches() throws Exception {
        assertAccept("latest.release", "trunk", "release", "trunk", true);
        assertAccept("latest.release", "trunk", "milestone", "trunk", false);
        assertAccept("latest.release", "trunk", "integration", "trunk", false);
    }

    public void testAcceptForDifferentBranches() throws Exception {
        assertAccept("latest.release", "trunk", "release", "feature", false);
        assertAccept("latest.release", "trunk", "milestone", "feature", false);
        assertAccept("latest.release", "trunk", "integration", "feature", false);
    }

    // assertion helper methods
    private void assertNeed(String askedVersion, boolean b) {
        assertEquals(b, vm.needModuleDescriptor(
            ModuleRevisionId.newInstance("org", "name", askedVersion), null));
    }

    private void assertNeed(String askedVersion, String askedBranch, boolean b) {
        assertEquals(b, vm.needModuleDescriptor(
            ModuleRevisionId.newInstance("org", "name", askedBranch, askedVersion), null));
    }

    private void assertAccept(String askedVersion, String foundStatus, boolean b) {
        ModuleRevisionId askedMrid = ModuleRevisionId.newInstance("org", "name", askedVersion);
        DefaultModuleDescriptor foundMD = DefaultModuleDescriptor
                .newDefaultInstance(ModuleRevisionId.newInstance("org", "name", null));
        foundMD.setStatus(foundStatus);
        assertEquals(b, vm.accept(askedMrid, foundMD));
    }

    private void assertAccept(String askedVersion, String askedBranch, String foundStatus,
            String foundBranch, boolean b) {
        ModuleRevisionId askedMrid = ModuleRevisionId.newInstance("org", "name", askedBranch,
            askedVersion);
        DefaultModuleDescriptor foundMD = DefaultModuleDescriptor.newDefaultInstance(
            ModuleRevisionId.newInstance("org", "name", foundBranch, (String) null));
        foundMD.setStatus(foundStatus);
        assertEquals(b, vm.accept(askedMrid, foundMD));
    }
}
