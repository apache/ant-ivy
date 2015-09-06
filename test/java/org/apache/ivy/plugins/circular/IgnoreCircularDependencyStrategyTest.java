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
package org.apache.ivy.plugins.circular;

import org.apache.ivy.TestHelper;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.MockMessageLogger;

import junit.framework.TestCase;

public class IgnoreCircularDependencyStrategyTest extends TestCase {
    private CircularDependencyStrategy strategy;

    private MockMessageLogger mockMessageImpl;

    protected void setUp() throws Exception {
        strategy = IgnoreCircularDependencyStrategy.getInstance();

        mockMessageImpl = new MockMessageLogger();
        Message.setDefaultLogger(mockMessageImpl);
    }

    public void testLog() throws Exception {
        strategy.handleCircularDependency(TestHelper.parseMridsToArray("#A;1.0, #B;1.0"));

        mockMessageImpl.assertLogVerboseContains("circular dependency found: #A;1.0->#B;1.0");
    }

    public void testRemoveDuplicates() throws Exception {
        strategy.handleCircularDependency(TestHelper.parseMridsToArray("#A;1.1, #B;1.0"));
        strategy.handleCircularDependency(TestHelper.parseMridsToArray("#A;1.1, #B;1.0"));

        // should only log the circular dependency once
        assertEquals(1, mockMessageImpl.getLogs().size());
    }
}
