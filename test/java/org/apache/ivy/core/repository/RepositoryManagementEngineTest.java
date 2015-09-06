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
package org.apache.ivy.core.repository;

import org.apache.ivy.TestFixture;
import org.apache.ivy.TestHelper;
import org.apache.ivy.core.event.EventManager;
import org.apache.ivy.core.resolve.ResolveEngine;
import org.apache.ivy.core.search.SearchEngine;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.core.sort.SortEngine;

import junit.framework.TestCase;

public class RepositoryManagementEngineTest extends TestCase {
    private RepositoryManagementEngine repository;

    private TestFixture fixture;

    protected void setUp() throws Exception {
        fixture = new TestFixture();
        IvySettings settings = fixture.getSettings();
        repository = new RepositoryManagementEngine(settings, new SearchEngine(settings),
                new ResolveEngine(settings, new EventManager(), new SortEngine(settings)));
    }

    protected void tearDown() throws Exception {
        fixture.clean();
    }

    public void testLoad() throws Exception {
        fixture.addMD("o1#A;1").addMD("o1#A;2").addMD("o1#A;3").addMD("o1#B;1")
                .addMD("o1#B;2->o1#A;2").addMD("o2#C;1->{o1#B;1 o1#A;1}").init();

        repository.load();
        assertEquals(3, repository.getModuleIdsNumber());
        assertEquals(6, repository.getRevisionsNumber());
    }

    public void testOrphans() throws Exception {
        fixture.addMD("o1#A;1").addMD("o1#A;2").addMD("o1#A;3").addMD("o1#B;1")
                .addMD("o1#B;2->o1#A;2").addMD("o2#C;1->{o1#B;1 o1#A;1}").init();

        repository.load();
        repository.analyze();
        TestHelper.assertModuleRevisionIds("o1#A;3 o1#B;2 o2#C;1", repository.getOrphans());
    }
}
