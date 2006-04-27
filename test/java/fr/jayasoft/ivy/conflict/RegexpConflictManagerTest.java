/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.conflict;

/**
 * @author Anders janmyr
 */

import java.util.Date;

import junit.framework.*;
import fr.jayasoft.ivy.Ivy;


public class RegexpConflictManagerTest extends TestCase
{
    private Ivy ivy;

    protected void setUp() throws Exception
    {
        ivy = new Ivy();
        ivy.configure( RegexpConflictManagerTest.class
                .getResource( "ivyconf-regexp-test.xml" ) );
    }

    public void testNoApiConflictResolve() throws Exception
    {
        try
        {
            ivy.resolve( RegexpConflictManagerTest.class
                    .getResource( "ivy-no-regexp-conflict.xml" ), null,
                    new String[] { "*" }, null, new Date(), false );
        }
        catch ( StrictConflictException e )
        {
            fail( "Unexpected conflict: " + e );
        }
    }

    public void testConflictResolve() throws Exception
    {
        try
        {
            ivy.resolve( RegexpConflictManagerTest.class
                    .getResource( "ivy-conflict.xml" ), null,
                    new String[] { "*" }, null, new Date(), false );

            fail( "Resolve should have failed with a conflict" );
        }
        catch ( StrictConflictException e )
        {
            // this is expected
            assertTrue(e.getMessage().indexOf("[ org1 | mod1.2 | 2.0.0 ]:2.0 (needed by [ jayasoft | resolve-noconflict | 1.0 ])")!=-1);
            assertTrue(e.getMessage().indexOf("conflicts with")!=-1);
            assertTrue(e.getMessage().indexOf("[ org1 | mod1.2 | 2.1.0 ]:2.1 (needed by [ jayasoft | resolve-noconflict | 1.0 ])")!=-1);
        }
    }
}