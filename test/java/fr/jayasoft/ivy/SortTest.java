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

    public void testSort() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configureDefault();
        toSort = new ArrayList(Arrays.asList(new Object[] {md[0], md[2], md[1], md[3]}));
        assertSorted(md, ivy.sortModuleDescriptors(toSort));
        toSort = new ArrayList(Arrays.asList(new Object[] {md[0], md[1], md[2], md[3]}));
        assertSorted(md, ivy.sortModuleDescriptors(toSort));
        toSort = new ArrayList(Arrays.asList(new Object[] {md[1], md[0], md[2], md[3]}));
        assertSorted(md, ivy.sortModuleDescriptors(toSort));
        toSort = new ArrayList(Arrays.asList(new Object[] {md[1], md[2], md[0], md[3]}));
        assertSorted(md, ivy.sortModuleDescriptors(toSort));
        toSort = new ArrayList(Arrays.asList(new Object[] {md[2], md[1], md[0], md[3]}));
        assertSorted(md, ivy.sortModuleDescriptors(toSort));
        toSort = new ArrayList(Arrays.asList(new Object[] {md[2], md[0], md[1], md[3]}));
        assertSorted(md, ivy.sortModuleDescriptors(toSort));
        toSort = new ArrayList(Arrays.asList(new Object[] {md[1], md[3], md[2], md[0]}));
        assertSorted(md, ivy.sortModuleDescriptors(toSort));
    }

    private void assertSorted(DefaultModuleDescriptor[] md, List sorted) {
        assertEquals(md.length, sorted.size());
        for (int i = 0; i < md.length; i++) {
            assertEquals(md[i], sorted.get(i));
        }
    }
    
    // sorter does not throw circular dependency, circular dependencies are handled at resolve time only
    // because circular dependencies are more complicated to evaluate than just a callstack comparison
    
    public void testCircularDependency() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configureDefault();
        md[0].addDependency(new DefaultDependencyDescriptor(mrid4, false));
        toSort = new ArrayList(Arrays.asList(new Object[] {md[0], md[2], md[1], md[3]}));
        // the sorted array may begin by any of the modules since there is a circular dependency
        // in this case, the result is the following
        DefaultModuleDescriptor[] sorted = new DefaultModuleDescriptor[] {
                md[1], md[2], md[3], md[0]
            };

        assertSorted(sorted, ivy.sortModuleDescriptors(toSort));
    }
    
    public void testCircularDependency2() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configureDefault();
        md[1].addDependency(new DefaultDependencyDescriptor(mrid3, false));
        toSort = new ArrayList(Arrays.asList(new Object[] {md[0], md[2], md[1], md[3]}));
        assertSorted(md, ivy.sortModuleDescriptors(toSort));
    }
}