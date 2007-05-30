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
package org.apache.ivy.core.sort;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.circular.CircularDependencyHelper;
import org.apache.ivy.plugins.circular.CircularDependencyStrategy;
import org.apache.ivy.util.Message;

public class SortTest extends TestCase {
    
	private DefaultModuleDescriptor md1;
    private DefaultModuleDescriptor md2;
    private DefaultModuleDescriptor md3;
    private DefaultModuleDescriptor md4;
	private static Ivy ivy;
	
    /* (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        md1 = createModuleDescriptorToSort("md1", null); //The revison is often not set in the ivy.xml file that are ordered
        md2 = createModuleDescriptorToSort("md2", "rev2");//But somtimes they are set
        md3 = createModuleDescriptorToSort("md3", "rev3");
        md4 = createModuleDescriptorToSort("md4", "rev4");
        
        ivy = new Ivy();
		ivy.configureDefault();
		Message.setImpl(null);
    }

    
    protected void tearDown() throws Exception {
    	Message.sumupProblems();//purge the warning and error messages
    	Message.setImpl(null);
    }

	public void testSort() throws Exception {        
        addDependency(md2,"md1" , "rev1");
        addDependency(md3,"md2" , "rev2");
        addDependency(md4,"md3" , "rev3");

        DefaultModuleDescriptor[][] expectedOrder = new DefaultModuleDescriptor[][] {{ md1, md2, md3, md4 }};
        
        Collection permutations = getAllLists(md1, md3, md2, md4);
        for (Iterator it = permutations.iterator(); it.hasNext();) {
			List toSort = (List) it.next();
	        assertSorted(expectedOrder, ivy.sortModuleDescriptors(toSort));			
		}
    }

    
	
    /**
     * Sorter does not throw circular dependency, circular dependencies are handled at resolve time only.
     * However the sort respect the transitive order when it is unambiguous. (if A depends transitively of B,
     * but B doesn't depends transitively on A then B always comes before A).
     */
    public void testCircularDependency() throws Exception {
        addDependency(md1,"md4" , "rev4");
        addDependency(md2,"md1" , "rev1");
        addDependency(md3,"md2" , "rev2");
        addDependency(md4,"md3" , "rev3");
        
        DefaultModuleDescriptor[][] possibleOrder = new DefaultModuleDescriptor[][] {
                {md2, md3, md4, md1},
                {md3, md4, md1, md2},
                {md4, md1, md2, md3},
                {md1, md2, md3, md4}
            };

        Collection permutations = getAllLists(md1, md3, md2, md4);
        for (Iterator it = permutations.iterator(); it.hasNext();) {
			List toSort = (List) it.next();
	        assertSorted(possibleOrder, ivy.sortModuleDescriptors(toSort));
		}        
    }
    
    public void testCircularDependency2() throws Exception {
        addDependency(md2,"md3" , "rev3");
        addDependency(md2,"md1" , "rev1");
        addDependency(md3,"md2" , "rev2");
        addDependency(md4,"md3" , "rev3");
        DefaultModuleDescriptor[][] possibleOrder = new DefaultModuleDescriptor[][] {
                {md1, md3, md2, md4},
                {md1, md2, md3, md4}//,
                //{md3, md1, md2, md4}	//It should be better to not have this solution.  The loop should apear has one contigous element.
            };
        Collection permutations = getAllLists(md1, md3, md2, md4);
        for (Iterator it = permutations.iterator(); it.hasNext();) {
			List toSort = (List) it.next();
	        assertSorted(possibleOrder, ivy.sortModuleDescriptors(toSort));
		}
    }

    
    /**
     *  In case of Circular dependency a warning is generated.
     */
    public void testCircularDependencyReport() {
        addDependency(md2,"md3" , "rev3");
        addDependency(md2,"md1" , "rev1");
        addDependency(md3,"md2" , "rev2");
        addDependency(md4,"md3" , "rev3");

        //MockMessageImpl mockMessageImpl = new MockMessageImpl();
		//Message.setImpl(mockMessageImpl);

		//Would be much easier with a tool like jmock 
		class CircularDependencyReporterMock implements CircularDependencyStrategy {
			private int nbOfCall = 0;
			public String getName() {
				return "CircularDependencyReporterMock";
			}
			public void handleCircularDependency(ModuleRevisionId[] mrids) {
				assertEquals("handleCircularDependency is expected to be called only once" , 0, nbOfCall);
				assertTrue("incorrect cicular dependency invocation" + CircularDependencyHelper.formatMessage(mrids), 
						mrids.length == 3 && ( 
						(mrids[0].equals(md2.getModuleRevisionId()) && mrids[1].equals(md3.getModuleRevisionId()) && mrids[2].equals(md2.getModuleRevisionId())) || 
						(mrids[0].equals(md3.getModuleRevisionId()) && mrids[1].equals(md2.getModuleRevisionId()) && mrids[2].equals(md3.getModuleRevisionId()))
						));
				nbOfCall++;				
			}
			public void validate() {
				Assert.assertEquals("handleCircularDependency has nor been called" , 1, nbOfCall);
			}
		}
		CircularDependencyReporterMock circularDepReportMock = new CircularDependencyReporterMock();
		ivy.getSettings().setCircularDependencyStrategy(circularDepReportMock);
        
		List toSort = Arrays.asList(new ModuleDescriptor[] {md4 , md3 , md2 , md1});
		ivy.sortModuleDescriptors(toSort);
		
		circularDepReportMock.validate();
        //mockMessageImpl.assertLogWarningContains("circular dependency detected during sort: [ org | md3 | rev3 ]->[ org | md2 | rev2 ]->[ org | md3 | rev3 ]");
    }
        
    /**
     * The dependency can ask for the latest integration.  It should match whatever the version declared in the modules to order.
     */
    public void testLatestIntegration() {
    	
    	addDependency(md2,"md1" , "latest.integration");
        addDependency(md3,"md2" , "latest.integration");
        addDependency(md4,"md3" , "latest.integration");

        DefaultModuleDescriptor[][] expectedOrder = new DefaultModuleDescriptor[][] {{ md1, md2, md3, md4 }};
        
        Collection permutations = getAllLists(md1, md3, md2, md4);
        for (Iterator it = permutations.iterator(); it.hasNext();) {
			List toSort = (List) it.next();
	        assertSorted(expectedOrder, ivy.sortModuleDescriptors(toSort));			
		}

    }
    
    /**
     * When the version asked by a dependency is not compatible with the version declared in the module to order, 
     * the two modules should be considered as independant
     * NB:  I'm sure of what 'compatible' means !
     */
    public void testDifferentVersionNotConsidered() {
    	//To test it, I use a 'broken' loop (in one step, I change the revision) in such a way that I get only one solution.  If the loop was 
    	//complete more solutions where possible.

        addDependency(md1,"md4" , "rev4-other");
        addDependency(md2,"md1" , "rev1");
        addDependency(md3,"md2" , "rev2");
        addDependency(md4,"md3" , "rev3");
        
        DefaultModuleDescriptor[][] possibleOrder = new DefaultModuleDescriptor[][] {
                {md1, md2, md3, md4}
            };

        Collection permutations = getAllLists(md1, md3, md2, md4);
        for (Iterator it = permutations.iterator(); it.hasNext();) {
			List toSort = (List) it.next();
	        assertSorted(possibleOrder, ivy.sortModuleDescriptors(toSort));
		}        

    }
    

    /**
     *  In case of Different version a warning is generated.
     */
    public void testDifferentVersionWarning() {
    	final DependencyDescriptor md4OtherDep = addDependency(md1,"md4" , "rev4-other");
        addDependency(md2,"md1" , "rev1");
        addDependency(md3,"md2" , "rev2");
        addDependency(md4,"md3" , "rev3");

		//Would be much easier with a tool like jmock 
		class NonMatchingVersionReporterMock implements NonMatchingVersionReporter {
			private int nbOfCall = 0;
			public void reportNonMatchingVersion(DependencyDescriptor descriptor, ModuleDescriptor md) {
				Assert.assertEquals("reportNonMatchingVersion should be invokded only once" , 0, nbOfCall);
				Assert.assertEquals(md4OtherDep, descriptor);
				Assert.assertEquals(md4, md);
				nbOfCall++;
			}
			public void validate() {
				Assert.assertEquals("reportNonMatchingVersion has not be called" , 1, nbOfCall);
			}
		}
		NonMatchingVersionReporterMock nonMatchingVersionReporterMock = new NonMatchingVersionReporterMock();
		List toSort = Arrays.asList(new ModuleDescriptor[] {md4 , md3 , md2 , md1});
		ivy.sortModuleDescriptors(toSort , nonMatchingVersionReporterMock);
		nonMatchingVersionReporterMock.validate();
    }

    
    
    
    
    
    private DefaultModuleDescriptor createModuleDescriptorToSort(String moduleName, String revision) {
    	ModuleRevisionId mrid = ModuleRevisionId.newInstance("org", moduleName, revision);
    	return new DefaultModuleDescriptor(mrid, "integration", new Date());
    }
    
    private DependencyDescriptor addDependency(DefaultModuleDescriptor parent, String moduleName, String revision) {
    	ModuleRevisionId mrid = ModuleRevisionId.newInstance("org", moduleName, revision);
    	DependencyDescriptor depDescr =  new DefaultDependencyDescriptor(parent , mrid , false, false, true);
    	parent.addDependency(depDescr);
    	return depDescr;
	}

    /** 
     * Verifies that sorted in one of the list of listOfPossibleSort.
     * @param An array of possible sort result
     * @param The actual sortedList to compare 
     */
    private void assertSorted(DefaultModuleDescriptor[][] listOfPossibleSort, List sorted) {
    	for (int i = 0; i < listOfPossibleSort.length; i++) {
			DefaultModuleDescriptor[] expectedList = listOfPossibleSort[i];
			assertEquals(expectedList.length, sorted.size());
			boolean isExpected = true;
	        for (int j = 0; j < expectedList.length; j++) {
	            if(!expectedList[j].equals(sorted.get(j))) {
	            	isExpected = false;
	            	break;
	            }
	        }
	        if (isExpected) {
	        	return;
	        }
		}
    	//failed, build a nice message
        StringBuffer errorMessage = new StringBuffer();
        errorMessage.append("Unexpected order : \n{ ");
        for (int i = 0; i < sorted.size(); i++) {
			if (i>0) errorMessage.append(" , ");
			errorMessage.append(((DefaultModuleDescriptor)sorted.get(i)).getModuleRevisionId());
		}
        errorMessage.append("}\nEpected : \n");
    	for (int i = 0; i < listOfPossibleSort.length; i++) {
			DefaultModuleDescriptor[] expectedList = listOfPossibleSort[i];
			if (i>0) errorMessage.append(" or\n");
			errorMessage.append("{ ");
			for (int j = 0; j < expectedList.length; j++) {
				if (j>0) errorMessage.append(" , ");
				errorMessage.append(expectedList[j].getModuleRevisionId());
			}
			errorMessage.append(" } ");
		}
    	fail(errorMessage.toString());
    }
    
	/** Returns a collection of lists that contains the elements a,b,c and d */
    private Collection getAllLists(Object a, Object b, Object c, Object d) {
		ArrayList r = new ArrayList(24);
		r.add(Arrays.asList(new Object[] {a, b, c, d}));
		r.add(Arrays.asList(new Object[] {a, b, d, c}));
		r.add(Arrays.asList(new Object[] {a, c, b, d}));
		r.add(Arrays.asList(new Object[] {a, c, d, b}));
		r.add(Arrays.asList(new Object[] {a, d, b, c}));
		r.add(Arrays.asList(new Object[] {a, d, c, b}));
		r.add(Arrays.asList(new Object[] {b, a, c, d}));
		r.add(Arrays.asList(new Object[] {b, a, d, c}));
		r.add(Arrays.asList(new Object[] {b, c, a, d}));
		r.add(Arrays.asList(new Object[] {b, c, d, a}));
		r.add(Arrays.asList(new Object[] {b, d, a, c}));
		r.add(Arrays.asList(new Object[] {b, d, c, a}));
		r.add(Arrays.asList(new Object[] {c, b, a, d}));
		r.add(Arrays.asList(new Object[] {c, b, d, a}));
		r.add(Arrays.asList(new Object[] {c, a, b, d}));
		r.add(Arrays.asList(new Object[] {c, a, d, b}));
		r.add(Arrays.asList(new Object[] {c, d, b, a}));
		r.add(Arrays.asList(new Object[] {c, d, a, b}));
		r.add(Arrays.asList(new Object[] {d, b, c, a}));
		r.add(Arrays.asList(new Object[] {d, b, a, c}));
		r.add(Arrays.asList(new Object[] {d, c, b, a}));
		r.add(Arrays.asList(new Object[] {d, c, a, b}));
		r.add(Arrays.asList(new Object[] {d, a, b, c}));
		r.add(Arrays.asList(new Object[] {d, a, c, b}));
		return r;
	}


}