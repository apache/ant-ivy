/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.conflict;

import java.util.Date;

import fr.jayasoft.ivy.ConflictManager;
import fr.jayasoft.ivy.Ivy;
import junit.framework.TestCase;

public class StrictConflictManagerTest extends TestCase {

    public void testInitFromConf() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(StrictConflictManagerTest.class.getResource("ivyconf-strict-test.xml"));
        ConflictManager cm = ivy.getDefaultConflictManager();
        assertTrue(cm instanceof StrictConflictManager);
    }

    public void testNoConflictResolve() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(StrictConflictManagerTest.class.getResource("ivyconf-strict-test.xml"));

        ivy.resolve(StrictConflictManagerTest.class.getResource("ivy-noconflict.xml"), null, new String[] { "*" }, null, new Date(), false);
    }

    public void testConflictResolve() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(StrictConflictManagerTest.class.getResource("ivyconf-strict-test.xml"));

        try {
            ivy.resolve(StrictConflictManagerTest.class.getResource("ivy-conflict.xml"), null, new String[] { "*" }, null, new Date(), false);

            fail("Resolve should have failed with a conflict");
        } catch (StrictConflictException e) {
            // this is expected
        }
    }

}
