/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

public class ModuleRevisionIdTest extends TestCase {

    public void testEncodeDecodeToString() {
        testEncodeDecodeToString(ModuleRevisionId.newInstance("org", "name", "revision"));
        testEncodeDecodeToString(ModuleRevisionId.newInstance("org", "name", ""));
        testEncodeDecodeToString(ModuleRevisionId.newInstance("org.jayasoft", "name-post", "1.0"));
        testEncodeDecodeToString(ModuleRevisionId.newInstance("org/jayasoft", "pre/name", "1.0-dev8/2"));
        Map extraAttributes = new HashMap();
        extraAttributes.put("extra", "extravalue");
        extraAttributes.put("att/name", "att/value");
        extraAttributes.put("att.name", "att.value");
        extraAttributes.put("att<name", "att<value");
        testEncodeDecodeToString(ModuleRevisionId.newInstance("org/jayasoft", "pre/name", "1.0-dev8/2", extraAttributes));
        extraAttributes.put("nullatt", null);
        testEncodeDecodeToString(ModuleRevisionId.newInstance("org/jayasoft", "pre/name", "1.0-dev8/2", extraAttributes));

    }

    private void testEncodeDecodeToString(ModuleRevisionId mrid) {
        assertEquals(mrid, ModuleRevisionId.decode(mrid.encodeToString()));
    }
}
