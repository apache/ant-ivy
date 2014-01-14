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

import junit.framework.TestCase;

public class PatternVersionMatcherTest extends TestCase {
    public void testSingleMatch() {
        PatternVersionMatcher pvm = new PatternVersionMatcher();
        pvm.addMatch(generateRegexpMatch1());

        assertAccept(pvm, "foo(1,3)", "1.4.1", false);
        assertAccept(pvm, "foo(1,3)", "1.3", false);
        assertAccept(pvm, "foo(1,3)", "2.3.1", false);

        assertAccept(pvm, "foo(1,3)", "1.3.1", true);
    }

    public void testMultipleMatchEqualRevisions() {
        PatternVersionMatcher pvm = new PatternVersionMatcher();
        pvm.addMatch(generateRegexpMatch1());
        pvm.addMatch(generateRegexpMatch2());

        assertAccept(pvm, "foo(1,3)", "1.3", true);
        assertAccept(pvm, "foo(1,3)", "1.3.1", true);
    }

    public void testMultipleMatchNonEqualRevisions() {
        PatternVersionMatcher pvm = new PatternVersionMatcher();
        pvm.addMatch(generateRegexpMatch1());
        pvm.addMatch(generateRegexpMatch3());

        assertAccept(pvm, "foo(1,3)", "1.3", false);
        assertAccept(pvm, "bar(1,3)", "1.3", true);
        assertAccept(pvm, "foo(1,3)", "1.3.1", true);
    }

    /**
     * Generates a Match instance that has the following xml representation: <match revision="foo"
     * pattern="${major}\.${minor}\.\d+" args="major, minor" matcher="regexp" />
     * 
     * @return
     */
    private Match generateRegexpMatch1() {
        Match match = new Match();
        match.setRevision("foo");
        match.setPattern("${major}\\.${minor}\\.\\d+");
        match.setArgs("major, minor");
        match.setMatcher("regexp");

        return match;
    }

    /**
     * Generates a Match instance that has the following xml representation: <match revision="foo"
     * pattern="${major}\.${minor}" args="major, minor" matcher="regexp" />
     * 
     * @return
     */
    private Match generateRegexpMatch2() {
        Match match = new Match();
        match.setRevision("foo");
        match.setPattern("${major}\\.${minor}");
        match.setArgs("major, minor");
        match.setMatcher("regexp");

        return match;
    }

    private Match generateRegexpMatch3() {
        Match match = new Match();
        match.setRevision("bar");
        match.setPattern("${major}\\.${minor}");
        match.setArgs("major, minor");
        match.setMatcher("regexp");

        return match;
    }

    private void assertAccept(PatternVersionMatcher matcher, String askedVersion,
            String depVersion, boolean b) {
        assertEquals(b, matcher.accept(ModuleRevisionId.newInstance("org", "name", askedVersion),
            ModuleRevisionId.newInstance("org", "name", depVersion)));
    }
}
