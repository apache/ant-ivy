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
 *
 */
public class SortTest extends TestCase {

	public void testSort() {
	    ModuleRevisionId mrid1 = ModuleRevisionId.newInstance("org", "md1", "rev1");
	    ModuleRevisionId mrid2 = ModuleRevisionId.newInstance("org", "md2", "rev2");
	    ModuleRevisionId mrid3 = ModuleRevisionId.newInstance("org", "md3", "rev3");
	    DefaultModuleDescriptor[] md = new DefaultModuleDescriptor[] {
	        new DefaultModuleDescriptor(mrid1, "integration", new Date()),
	        new DefaultModuleDescriptor(mrid2, "integration", new Date()),
	        new DefaultModuleDescriptor(mrid3, "integration", new Date()),
	    };
	    md[1].addDependency(new DefaultDependencyDescriptor(mrid1, false));
	    md[2].addDependency(new DefaultDependencyDescriptor(mrid2, false));
	    
	    List toSort;
	    
	    toSort = new ArrayList(Arrays.asList(new Object[] {md[0], md[2], md[1]}));
        assertSorted(md, Ivy.sortModuleDescriptors(toSort));
	    toSort = new ArrayList(Arrays.asList(new Object[] {md[0], md[1], md[2]}));
        assertSorted(md, Ivy.sortModuleDescriptors(toSort));
	    toSort = new ArrayList(Arrays.asList(new Object[] {md[1], md[0], md[2]}));
        assertSorted(md, Ivy.sortModuleDescriptors(toSort));
	    toSort = new ArrayList(Arrays.asList(new Object[] {md[1], md[2], md[0]}));
        assertSorted(md, Ivy.sortModuleDescriptors(toSort));
	    toSort = new ArrayList(Arrays.asList(new Object[] {md[2], md[1], md[0]}));
        assertSorted(md, Ivy.sortModuleDescriptors(toSort));
	    toSort = new ArrayList(Arrays.asList(new Object[] {md[2], md[0], md[1]}));
        assertSorted(md, Ivy.sortModuleDescriptors(toSort));
	}

    private void assertSorted(DefaultModuleDescriptor[] md, List sorted) {
	    assertEquals(md.length, sorted.size());
	    for (int i = 0; i < md.length; i++) {
		    assertEquals(md[i], sorted.get(i));
        }
    }
}
