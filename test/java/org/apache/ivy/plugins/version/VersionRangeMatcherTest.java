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

import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.latest.LatestRevisionStrategy;

import junit.framework.TestCase;

public class VersionRangeMatcherTest extends TestCase {
    VersionMatcher vm = new VersionRangeMatcher("range", new LatestRevisionStrategy());

    public void testMavenExcludeParenthesis() throws Exception {
        assertAccept("[3.8,4.0)", "3.7", false);
        assertAccept("[3.8,4.0)", "3.8", true);
        assertAccept("[3.8,4.0)", "3.9", true);
        assertAccept("[3.8,4.0)", "4.0", false);
        assertAccept("[3.8,4.0)", "4.1", false);

        assertAccept("(3.8,4.0]", "3.7", false);
        assertAccept("(3.8,4.0]", "3.8", false);
        assertAccept("(3.8,4.0]", "3.9", true);
        assertAccept("(3.8,4.0]", "4.0", true);
        assertAccept("(3.8,4.0]", "4.1", false);

        assertAccept("(3.8,4.0)", "3.7", false);
        assertAccept("(3.8,4.0)", "3.8", false);
        assertAccept("(3.8,4.0)", "3.9", true);
        assertAccept("(3.8,4.0)", "4.0", false);
        assertAccept("(3.8,4.0)", "4.1", false);
    }

    public void testDynamic() {
        assertDynamic("lastest.integration", false);
        assertDynamic("[1.0]", false);
        assertDynamic("(1.0)", false);
        assertDynamic("[1.0;2.0]", false);

        assertDynamic("[1.0,2.0]", true);
        assertDynamic("[1.0,2.0[", true);
        assertDynamic("]1.0,2.0[", true);
        assertDynamic("]1.0,2.0]", true);
        assertDynamic("[1.0,)", true);
        assertDynamic("(,1.0]", true);

        assertDynamic("(1.0, 2.0)", true);
        assertDynamic("(1.0, 2.0]", true);
        assertDynamic("[1.0, 2.0)", true);
        assertDynamic("[1.0, 2.0]", true);
        assertDynamic("[ 1.0, 2.0]", true);
        assertDynamic("[1.0, 2.0 ]", true);
        assertDynamic("[ 1.0, 2.0 ]", true);
        assertDynamic("[1.0, 2.0[", true);
        assertDynamic("[ 1.0, 2.0[", true);
        assertDynamic("[1.0, 2.0 [", true);
        assertDynamic("[ 1.0, 2.0 [", true);
        assertDynamic("]1.0, 2.0[", true);
        assertDynamic("] 1.0, 2.0[", true);
        assertDynamic("]1.0, 2.0 [", true);
        assertDynamic("] 1.0, 2.0 [", true);
        assertDynamic("]1.0, 2.0]", true);
        assertDynamic("] 1.0, 2.0]", true);
        assertDynamic("]1.0, 2.0 ]", true);
        assertDynamic("] 1.0, 2.0 ]", true);
        assertDynamic("[1.0, )", true);
        assertDynamic("[ 1.0,)", true);
        assertDynamic("[ 1.0, )", true);
        assertDynamic("( ,1.0]", true);
        assertDynamic("(, 1.0]", true);
        assertDynamic("( , 1.0]", true);
        assertDynamic("( , 1.0 ]", true);
    }

    public void testIncludingFinite() {
        assertAccept("[1.0,2.0]", "1.1", true);
        assertAccept("[1.0,2.0]", "0.9", false);
        assertAccept("[1.0,2.0]", "2.1", false);
        assertAccept("[1.0,2.0]", "1.0", true);
        assertAccept("[1.0,2.0]", "2.0", true);

        assertAccept("[1.0, 2.0]", "1.1", true);
        assertAccept("[1.0, 2.0 ]", "0.9", false);
        assertAccept("[1.0, 2.0]", "2.1", false);
        assertAccept("[ 1.0,2.0]", "1.0", true);
        assertAccept("[ 1.0 , 2.0 ]", "2.0", true);
    }

    public void testExcludingFinite() {
        assertAccept("]1.0,2.0[", "1.1", true);
        assertAccept("]1.0,2.0[", "0.9", false);
        assertAccept("]1.0,2.0[", "2.1", false);

        assertAccept("]1.0,2.0]", "1.0", false);
        assertAccept("]1.0,2.0[", "1.0", false);
        assertAccept("[1.0,2.0[", "1.0", true);

        assertAccept("[1.0,2.0[", "2.0", false);
        assertAccept("]1.0,2.0[", "2.0", false);
        assertAccept("]1.0,2.0]", "2.0", true);
    }

    public void testIncludingInfinite() {
        assertAccept("[1.0,)", "1.1", true);
        assertAccept("[1.0,)", "2.0", true);
        assertAccept("[1.0,)", "3.5.6", true);
        assertAccept("[1.0,)", "1.0", true);

        assertAccept("[1.0,)", "0.9", false);

        assertAccept("(,2.0]", "1.1", true);
        assertAccept("(,2.0]", "0.1", true);
        assertAccept("(,2.0]", "0.2.4", true);
        assertAccept("(,2.0]", "2.0", true);

        assertAccept("(,2.0]", "2.3", false);

        assertAccept("[1.0, )", "1.1", true);
        assertAccept("[1.0 ,)", "2.0", true);
        assertAccept("[1.0 , )", "3.5.6", true);
        assertAccept("[ 1.0, )", "1.0", true);
    }

    public void testExcludingInfinite() {
        assertAccept("]1.0,)", "1.1", true);
        assertAccept("]1.0,)", "2.0", true);
        assertAccept("]1.0,)", "3.5.6", true);

        assertAccept("]1.0,)", "1.0", false);
        assertAccept("]1.0,)", "0.9", false);

        assertAccept("(,2.0[", "1.1", true);
        assertAccept("(,2.0[", "0.1", true);
        assertAccept("(,2.0[", "0.2.4", true);

        assertAccept("(,2.0[", "2.0", false);
        assertAccept("(,2.0[", "2.3", false);
    }

    // assertion helper methods

    private void assertDynamic(String askedVersion, boolean b) {
        assertEquals(b, vm.isDynamic(ModuleRevisionId.newInstance("org", "name", askedVersion)));
    }

    private void assertAccept(String askedVersion, String depVersion, boolean b) {
        assertEquals(
            b,
            vm.accept(ModuleRevisionId.newInstance("org", "name", askedVersion),
                ModuleRevisionId.newInstance("org", "name", depVersion)));
    }
}
