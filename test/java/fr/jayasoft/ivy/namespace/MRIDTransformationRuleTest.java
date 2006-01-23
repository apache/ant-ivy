/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.namespace;

import fr.jayasoft.ivy.ModuleRevisionId;
import junit.framework.TestCase;

public class MRIDTransformationRuleTest extends TestCase {
    
    public void testTransformation() {
        MRIDTransformationRule r = new MRIDTransformationRule();
        r.addSrc(new MRIDRule("apache", "commons.+", null));
        r.addDest(new MRIDRule("$m0", "$m0", null));
        
        assertEquals(ModuleRevisionId.newInstance("commons-client", "commons-client", "1.0"),
                r.transform(ModuleRevisionId.newInstance("apache", "commons-client", "1.0")));
    }
}
