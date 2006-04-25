/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;

/**
 * @author Xavier Hanin
 * @author baumkar
 */
public class SortTest extends TestCase {
    
    private ModuleRevisionId mrid1;
    private ModuleRevisionId mrid2;
    private ModuleRevisionId mrid3;
    private ModuleRevisionId mrid4;
    private DefaultModuleDescriptor[] md;
    List toSort;
    
    
    
    /* (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        mrid1 = ModuleRevisionId.newInstance("org", "md1", "rev1");
        mrid2 = ModuleRevisionId.newInstance("org", "md2", "rev2");
        mrid3 = ModuleRevisionId.newInstance("org", "md3", "rev3");
        mrid4 = ModuleRevisionId.newInstance("org", "md4", "rev4");
        md = new DefaultModuleDescriptor[] {
            new DefaultModuleDescriptor(mrid1, "integration", new Date()),
            new DefaultModuleDescriptor(mrid2, "integration", new Date()),
            new DefaultModuleDescriptor(mrid3, "integration", new Date()),
            new DefaultModuleDescriptor(mrid4, "integration", new Date())
        };
        md[1].addDependency(new DefaultDependencyDescriptor(mrid1, false));
        md[2].addDependency(new DefaultDependencyDescriptor(mrid2, false));
        md[3].addDependency(new DefaultDependencyDescriptor(mrid3, false));
        
    }

    public void testSort() {
        toSort = new ArrayList(Arrays.asList(new Object[] {md[0], md[2], md[1], md[3]}));
        assertSorted(md, Ivy.sortModuleDescriptors(toSort));
        toSort = new ArrayList(Arrays.asList(new Object[] {md[0], md[1], md[2], md[3]}));
        assertSorted(md, Ivy.sortModuleDescriptors(toSort));
        toSort = new ArrayList(Arrays.asList(new Object[] {md[1], md[0], md[2], md[3]}));
        assertSorted(md, Ivy.sortModuleDescriptors(toSort));
        toSort = new ArrayList(Arrays.asList(new Object[] {md[1], md[2], md[0], md[3]}));
        assertSorted(md, Ivy.sortModuleDescriptors(toSort));
        toSort = new ArrayList(Arrays.asList(new Object[] {md[2], md[1], md[0], md[3]}));
        assertSorted(md, Ivy.sortModuleDescriptors(toSort));
        toSort = new ArrayList(Arrays.asList(new Object[] {md[2], md[0], md[1], md[3]}));
        assertSorted(md, Ivy.sortModuleDescriptors(toSort));
        toSort = new ArrayList(Arrays.asList(new Object[] {md[1], md[3], md[2], md[0]}));
        assertSorted(md, Ivy.sortModuleDescriptors(toSort));
    }

    private void assertSorted(DefaultModuleDescriptor[] md, List sorted) {
        assertEquals(md.length, sorted.size());
        for (int i = 0; i < md.length; i++) {
            assertEquals(md[i], sorted.get(i));
        }
    }
    
    // sorter does not throw circular dependency anymore for the moment,
    // because circular dependencies are more complicated to evaluate than just a callstack comparison 
    // (could be ok with appropriate configurations) - see http://jira.jayasoft.org/browse/IVY-230
//    public void testCircularDependency() {
//        md[0].addDependency(new DefaultDependencyDescriptor(mrid4, false));
//        toSort = new ArrayList(Arrays.asList(new Object[] {md[0], md[2], md[1], md[3]}));
//        try {
//            Ivy.sortModuleDescriptors(toSort);
//        } catch (CircularDependencyException e) {
//            //successfull
//            assertEquals("Wrong dependency graph message", "[ org | md1 | rev1 ]->[ org | md4 | rev4 ]->[ org | md3 | rev3 ]->[ org | md2 | rev2 ]->[ org | md1 | rev1 ]", e.getMessage());
//            return;
//        }
//        assertTrue("Should have thrown circular dependency exception", false);
//    }
//    
//    public void testCircularDependency2() {
//        md[1].addDependency(new DefaultDependencyDescriptor(mrid3, false));
//        toSort = new ArrayList(Arrays.asList(new Object[] {md[0], md[2], md[1], md[3]}));
//        try {
//            Ivy.sortModuleDescriptors(toSort);
//        } catch (CircularDependencyException e) {
//            //successfull
//            assertEquals("Wrong dependency graph message", "[ org | md3 | rev3 ]->[ org | md2 | rev2 ]->[ org | md3 | rev3 ]", e.getMessage());
//            return;
//        }
//        assertTrue("Should have thrown circular dependency exception", false);
//    }
}