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
package org.apache.ivy.core.sort;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.circular.CircularDependencyHelper;
import org.apache.ivy.plugins.circular.CircularDependencyStrategy;
import org.apache.ivy.plugins.circular.WarnCircularDependencyStrategy;
import org.apache.ivy.plugins.version.ExactVersionMatcher;
import org.apache.ivy.plugins.version.LatestVersionMatcher;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class SortTest {

    private DefaultModuleDescriptor md1;

    private DefaultModuleDescriptor md2;

    private DefaultModuleDescriptor md3;

    private DefaultModuleDescriptor md4;

    private SortEngine sortEngine;

    private SimpleSortEngineSettings settings;

    private SilentNonMatchingVersionReporter nonMatchReporter;

    @Before
    public void setUp() {
        md1 = createModuleDescriptorToSort("md1", null); // The revision is often not set in the
        // ivy.xml file that are ordered
        md2 = createModuleDescriptorToSort("md2", "rev2"); // But sometimes they are set
        md3 = createModuleDescriptorToSort("md3", "rev3");
        md4 = createModuleDescriptorToSort("md4", "rev4");

        settings = new SimpleSortEngineSettings();
        settings.setCircularDependencyStrategy(WarnCircularDependencyStrategy.getInstance());
        settings.setVersionMatcher(new ExactVersionMatcher());

        sortEngine = new SortEngine(settings);

        nonMatchReporter = new SilentNonMatchingVersionReporter();
    }

    @Test
    public void testSort() {
        addDependency(md2, "md1", "rev1");
        addDependency(md3, "md2", "rev2");
        addDependency(md4, "md3", "rev3");

        DefaultModuleDescriptor[][] expectedOrder = new DefaultModuleDescriptor[][] {
                {md1, md2, md3, md4}};

        for (List<ModuleDescriptor> toSort : getAllLists(md1, md3, md2, md4)) {
            assertSorted(expectedOrder, sortModuleDescriptors(toSort, nonMatchReporter));
        }
    }

    /**
     * Sorter does not throw circular dependency, circular dependencies are handled at resolve time
     * only. However the sort respect the transitive order when it is unambiguous. (If A depends
     * transitively of B, but B doesn't depends transitively on A, then B always comes before A.)
     */
    @Test
    public void testCircularDependency() {
        addDependency(md1, "md4", "rev4");
        addDependency(md2, "md1", "rev1");
        addDependency(md3, "md2", "rev2");
        addDependency(md4, "md3", "rev3");

        DefaultModuleDescriptor[][] possibleOrder = new DefaultModuleDescriptor[][] {
                {md2, md3, md4, md1}, {md3, md4, md1, md2}, {md4, md1, md2, md3},
                {md1, md2, md3, md4}};

        for (List<ModuleDescriptor> toSort : getAllLists(md1, md3, md2, md4)) {
            assertSorted(possibleOrder, sortModuleDescriptors(toSort, nonMatchReporter));
        }
    }

    @Test
    public void testCircularDependency2() {
        addDependency(md2, "md3", "rev3");
        addDependency(md2, "md1", "rev1");
        addDependency(md3, "md2", "rev2");
        addDependency(md4, "md3", "rev3");
        DefaultModuleDescriptor[][] possibleOrder = new DefaultModuleDescriptor[][] {
                {md1, md3, md2, md4}, {md1, md2, md3, md4}
                // {md3, md1, md2, md4}
                // we don't have this solution. The loops appear has one contiguous element.
        };
        for (List<ModuleDescriptor> toSort : getAllLists(md1, md3, md2, md4)) {
            assertSorted(possibleOrder, sortModuleDescriptors(toSort, nonMatchReporter));
        }
    }

    /**
     * Test case for IVY-624
     *
     * @see <a href="https://issues.apache.org/jira/browse/IVY-624">IVY-624</a>
     */
    @Test
    public void testCircularDependencyInfiniteLoop() {
        addDependency(md1, "md2", "rev2");
        addDependency(md1, "md3", "rev3");
        addDependency(md2, "md3", "rev3");
        addDependency(md3, "md4", "rev4");
        addDependency(md4, "md1", "rev1");
        addDependency(md4, "md2", "rev2");
        sortModuleDescriptors(Arrays.<ModuleDescriptor> asList(md1, md2, md3, md4), nonMatchReporter);
        // If it ends, it's ok.
    }

    /**
     * In case of Circular dependency a warning is generated.
     */
    @Test
    public void testCircularDependencyReport() {
        addDependency(md2, "md3", "rev3");
        addDependency(md2, "md1", "rev1");
        addDependency(md3, "md2", "rev2");
        addDependency(md4, "md3", "rev3");

        // Would be much easier with a tool like jmock
        class CircularDependencyReporterMock implements CircularDependencyStrategy {
            private int nbOfCall = 0;

            public String getName() {
                return "CircularDependencyReporterMock";
            }

            public void handleCircularDependency(ModuleRevisionId[] mrids) {
                assertEquals("handleCircularDependency is expected to be called only once", 0,
                    nbOfCall);
                String assertMsg = "incorrect circular dependency invocation"
                        + CircularDependencyHelper.formatMessage(mrids);
                final int expectedLength = 3;
                assertEquals(assertMsg, expectedLength, mrids.length);
                if (mrids[0].equals(md2.getModuleRevisionId())) {
                    assertEquals(assertMsg, md3.getModuleRevisionId(), mrids[1]);
                    assertEquals(assertMsg, md2.getModuleRevisionId(), mrids[2]);
                } else {
                    assertEquals(assertMsg, md3.getModuleRevisionId(), mrids[0]);
                    assertEquals(assertMsg, md2.getModuleRevisionId(), mrids[1]);
                    assertEquals(assertMsg, md3.getModuleRevisionId(), mrids[2]);
                }
                nbOfCall++;
            }

            public void validate() {
                assertEquals("handleCircularDependency has not been called", 1, nbOfCall);
            }
        }
        CircularDependencyReporterMock circularDepReportMock = new CircularDependencyReporterMock();
        settings.setCircularDependencyStrategy(circularDepReportMock);

        sortModuleDescriptors(Arrays.<ModuleDescriptor> asList(md4, md3, md2, md1), nonMatchReporter);

        circularDepReportMock.validate();
    }

    /**
     * The dependency can ask for the latest integration. It should match whatever the version
     * declared in the modules to order.
     */
    @Test
    public void testLatestIntegration() {
        addDependency(md2, "md1", "latest.integration");
        addDependency(md3, "md2", "latest.integration");
        addDependency(md4, "md3", "latest.integration");

        settings.setVersionMatcher(new LatestVersionMatcher());

        DefaultModuleDescriptor[][] expectedOrder = new DefaultModuleDescriptor[][] {
                {md1, md2, md3, md4}};

        for (List<ModuleDescriptor> toSort : getAllLists(md1, md3, md2, md4)) {
            assertSorted(expectedOrder, sortModuleDescriptors(toSort, nonMatchReporter));
        }
    }

    /**
     * When the version asked by a dependency is not compatible with the version declared in the
     * module to order, the two modules should be considered as independent.
     * NB: I'm sure of what 'compatible' means !
     */
    @Test
    public void testDifferentVersionNotConsidered() {
        // To test it, I use a 'broken' loop (in one step, I change the revision) in such a way that
        // I get only one solution. If the loop was
        // complete more solutions where possible.

        addDependency(md1, "md4", "rev4-other");
        addDependency(md2, "md1", "rev1");
        addDependency(md3, "md2", "rev2");
        addDependency(md4, "md3", "rev3");

        DefaultModuleDescriptor[][] possibleOrder = new DefaultModuleDescriptor[][] {
                {md1, md2, md3, md4}};

        for (List<ModuleDescriptor> toSort : getAllLists(md1, md3, md2, md4)) {
            assertSorted(possibleOrder, sortModuleDescriptors(toSort, nonMatchReporter));
        }
    }

    /**
     * In case of Different version a warning is generated.
     */
    @Test
    public void testDifferentVersionWarning() {
        final DependencyDescriptor md4OtherDep = addDependency(md1, "md4", "rev4-other");
        addDependency(md2, "md1", "rev1");
        addDependency(md3, "md2", "rev2");
        addDependency(md4, "md3", "rev3");

        // Would be much easier with a tool like jmock
        class NonMatchingVersionReporterMock implements NonMatchingVersionReporter {
            private int nbOfCall = 0;

            public void reportNonMatchingVersion(DependencyDescriptor descriptor,
                    ModuleDescriptor md) {
                assertEquals("reportNonMatchingVersion should be invoked only once", 0,
                    nbOfCall);
                assertEquals(md4OtherDep, descriptor);
                assertEquals(md4, md);
                nbOfCall++;
            }

            public void validate() {
                assertEquals("reportNonMatchingVersion has not been called", 1, nbOfCall);
            }
        }
        NonMatchingVersionReporterMock nonMatchingVersionReporterMock = new NonMatchingVersionReporterMock();
        List<ModuleDescriptor> toSort = Arrays.asList(new ModuleDescriptor[] {md4, md3, md2, md1});
        sortModuleDescriptors(toSort, nonMatchingVersionReporterMock);
        nonMatchingVersionReporterMock.validate();
    }

    private List<ModuleDescriptor> sortModuleDescriptors(List<ModuleDescriptor> toSort,
            NonMatchingVersionReporter nonMatchingVersionReporter) {
        return sortEngine.sortModuleDescriptors(toSort,
            new SortOptions().setNonMatchingVersionReporter(nonMatchingVersionReporter));
    }

    private DefaultModuleDescriptor createModuleDescriptorToSort(String moduleName, String revision) {
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org", moduleName, revision);
        return new DefaultModuleDescriptor(mrid, "integration", new Date());
    }

    private DependencyDescriptor addDependency(DefaultModuleDescriptor parent, String moduleName,
            String revision) {
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org", moduleName, revision);
        DependencyDescriptor depDescr = new DefaultDependencyDescriptor(parent, mrid, false, false,
                true);
        parent.addDependency(depDescr);
        return depDescr;
    }

    /**
     * Verifies that sorted is one of the lists of listOfPossibleSort.
     *
     * @param listOfPossibleSort
     *            array of possible sort result
     * @param sorted
     *            actual sortedList to compare
     */
    private void assertSorted(DefaultModuleDescriptor[][] listOfPossibleSort,
            List<ModuleDescriptor> sorted) {
        for (DefaultModuleDescriptor[] expectedList : listOfPossibleSort) {
            assertEquals(expectedList.length, sorted.size());
            boolean isExpected = true;
            for (int j = 0; j < expectedList.length; j++) {
                if (!expectedList[j].equals(sorted.get(j))) {
                    isExpected = false;
                    break;
                }
            }
            if (isExpected) {
                return;
            }
        }
        // failed, build a nice message
        StringBuilder errorMessage = new StringBuilder();
        errorMessage.append("Unexpected order : \n{ ");
        for (int i = 0; i < sorted.size(); i++) {
            if (i > 0) {
                errorMessage.append(" , ");
            }
            errorMessage.append(sorted.get(i).getModuleRevisionId());
        }
        errorMessage.append("}\nExpected : \n");
        for (int i = 0; i < listOfPossibleSort.length; i++) {
            DefaultModuleDescriptor[] expectedList = listOfPossibleSort[i];
            if (i > 0) {
                errorMessage.append(" or\n");
            }
            errorMessage.append("{ ");
            for (int j = 0; j < expectedList.length; j++) {
                if (j > 0) {
                    errorMessage.append(" , ");
                }
                errorMessage.append(expectedList[j].getModuleRevisionId());
            }
            errorMessage.append(" } ");
        }
        fail(errorMessage.toString());
    }

    /**
     * Returns a collection of lists that contains the elements a, b, c and d
     */
    private Collection<List<ModuleDescriptor>> getAllLists(ModuleDescriptor a, ModuleDescriptor b,
            ModuleDescriptor c, ModuleDescriptor d) {
        final int nbOfList = 24;
        Collection<List<ModuleDescriptor>> r = new ArrayList<>(nbOfList);
        r.add(Arrays.asList(a, b, c, d));
        r.add(Arrays.asList(a, b, d, c));
        r.add(Arrays.asList(a, c, b, d));
        r.add(Arrays.asList(a, c, d, b));
        r.add(Arrays.asList(a, d, b, c));
        r.add(Arrays.asList(a, d, c, b));
        r.add(Arrays.asList(b, a, c, d));
        r.add(Arrays.asList(b, a, d, c));
        r.add(Arrays.asList(b, c, a, d));
        r.add(Arrays.asList(b, c, d, a));
        r.add(Arrays.asList(b, d, a, c));
        r.add(Arrays.asList(b, d, c, a));
        r.add(Arrays.asList(c, b, a, d));
        r.add(Arrays.asList(c, b, d, a));
        r.add(Arrays.asList(c, a, b, d));
        r.add(Arrays.asList(c, a, d, b));
        r.add(Arrays.asList(c, d, b, a));
        r.add(Arrays.asList(c, d, a, b));
        r.add(Arrays.asList(d, b, c, a));
        r.add(Arrays.asList(d, b, a, c));
        r.add(Arrays.asList(d, c, b, a));
        r.add(Arrays.asList(d, c, a, b));
        r.add(Arrays.asList(d, a, b, c));
        r.add(Arrays.asList(d, a, c, b));
        return r;
    }

}
