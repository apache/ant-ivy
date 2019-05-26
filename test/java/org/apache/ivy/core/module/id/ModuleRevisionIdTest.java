/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivy.core.module.id;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ModuleRevisionIdTest {

    @Rule
    public ExpectedException expExc = ExpectedException.none();

    @Test
    public void testParse() {
        testParse("#A;1.0");
        testParse("org#module;2.0");
        testParse("org#module#branch;myversion");
        testParse("org#module#branch;[1.2,1.3]");
        testParse("org#module#branch;working@test");
        testParse(" org#module#branch;[1.2,1.3] ");
        testParse(" org#module#branch;[1.2,1.3) ");
    }

    private void testParse(String mrid) {
        assertEquals(mrid.trim(), ModuleRevisionId.parse(mrid).toString());
    }

    @Test
    public void testParseFailure1() {
        testParseFailure("bad");
    }

    @Test
    public void testParseFailure2() {
        testParseFailure("org#mod");
    }

    @Test
    public void testParseFailure3() {
        testParseFailure("#;1");
    }

    @Test
    public void testParseFailure4() {
        testParseFailure("#A%;1.0");
    }

    private void testParseFailure(String mrid) {
        expExc.expect(IllegalArgumentException.class);
        expExc.expectMessage(mrid);

        ModuleRevisionId.parse(mrid);
    }

    @Test
    public void testEncodeDecodeToString() {
        testEncodeDecodeToString(ModuleRevisionId.newInstance("org", "name", "revision"));
        testEncodeDecodeToString(ModuleRevisionId.newInstance("org", "name", ""));
        testEncodeDecodeToString(ModuleRevisionId.newInstance("org.apache", "name-post", "1.0"));
        testEncodeDecodeToString(
            ModuleRevisionId.newInstance("org/apache", "pre/name", "1.0-dev8/2"));
        Map<String, String> extraAttributes = new HashMap<>();
        extraAttributes.put("extra", "extravalue");
        extraAttributes.put("att/name", "att/value");
        extraAttributes.put("att.name", "att.value");
        extraAttributes.put("att<name", "att<value");
        testEncodeDecodeToString(
            ModuleRevisionId.newInstance("org/apache", "pre/name", "1.0-dev8/2", extraAttributes));
        extraAttributes.put("nullatt", null);
        testEncodeDecodeToString(
            ModuleRevisionId.newInstance("org/apache", "pre/name", "1.0-dev8/2", extraAttributes));

    }

    private void testEncodeDecodeToString(ModuleRevisionId mrid) {
        assertEquals(mrid, ModuleRevisionId.decode(mrid.encodeToString()));
    }

}
