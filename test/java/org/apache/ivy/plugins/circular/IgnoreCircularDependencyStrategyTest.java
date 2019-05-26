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
package org.apache.ivy.plugins.circular;

import static org.junit.Assert.assertEquals;

import org.apache.ivy.TestHelper;
import org.apache.ivy.core.IvyContext;
import org.apache.ivy.util.MessageLoggerEngine;
import org.apache.ivy.util.MockMessageLogger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class IgnoreCircularDependencyStrategyTest {
    private CircularDependencyStrategy strategy;

    private MockMessageLogger mockMessageImpl;

    private MessageLoggerEngine messageLoggerEngine;

    @Before
    public void setUp() {
        strategy = IgnoreCircularDependencyStrategy.getInstance();

        mockMessageImpl = new MockMessageLogger();
        messageLoggerEngine = setupMockLogger(mockMessageImpl);
    }

    @After
    public void tearDown() {
        resetMockLogger(messageLoggerEngine);
    }

    @Test
    public void testLog() {
        strategy.handleCircularDependency(TestHelper.parseMridsToArray("#A;1.0, #B;1.0"));

        mockMessageImpl.assertLogVerboseContains("circular dependency found: #A;1.0->#B;1.0");
    }

    @Test
    public void testRemoveDuplicates() {
        strategy.handleCircularDependency(TestHelper.parseMridsToArray("#A;1.1, #B;1.0"));
        strategy.handleCircularDependency(TestHelper.parseMridsToArray("#A;1.1, #B;1.0"));

        // should only log the circular dependency once
        assertEquals(1, mockMessageImpl.getLogs().size());
    }

    private MessageLoggerEngine setupMockLogger(final MockMessageLogger mockLogger) {
        if (mockLogger == null) {
            return null;
        }
        final MessageLoggerEngine loggerEngine = IvyContext.getContext().getIvy().getLoggerEngine();
        loggerEngine.pushLogger(mockLogger);
        return loggerEngine;
    }

    private void resetMockLogger(final MessageLoggerEngine loggerEngine) {
        if (loggerEngine == null) {
            return;
        }
        loggerEngine.popLogger();
    }
}
