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
import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.event.EventManager;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolveEngine;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.core.sort.SortEngine;
import org.apache.ivy.util.MessageLoggerEngine;
import org.apache.ivy.util.MockMessageLogger;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WarnCircularDependencyStrategyTest {
    private CircularDependencyStrategy strategy;

    private MessageLoggerEngine loggerEngine;
    private MockMessageLogger mockMessageLogger;

    @Before
    public void setUp() {
        // setup a new IvyContext for each test
        IvyContext.pushNewContext();
        strategy = WarnCircularDependencyStrategy.getInstance();
        mockMessageLogger = new MockMessageLogger();
        loggerEngine = setupMockLogger(mockMessageLogger);
    }

    protected void tearDown() throws Exception {
        resetMockLogger(loggerEngine);
        // pop the context we setup before
        IvyContext.popContext();
    }

    @Test
    public void testLog() throws Exception {
        strategy.handleCircularDependency(TestHelper.parseMridsToArray("#A;1.0, #B;1.0"));
        mockMessageLogger.assertLogWarningContains("circular dependency found: #A;1.0->#B;1.0");
    }

    @Test
    public void testRemoveDuplicates() throws Exception {
        strategy.handleCircularDependency(TestHelper.parseMridsToArray("#A;1.1, #B;1.0"));
        strategy.handleCircularDependency(TestHelper.parseMridsToArray("#A;1.1, #B;1.0"));

        // should only log the circular dependency once
        assertEquals(1, mockMessageLogger.getWarns().size());
    }

    @Test
    public void testRemoveDuplicates2() throws Exception {
        setResolveContext("1");
        strategy.handleCircularDependency(TestHelper.parseMridsToArray("#A;1.1, #B;1.0"));
        strategy.handleCircularDependency(TestHelper.parseMridsToArray("#A;1.1, #B;1.0"));

        // should only log the circular dependency once
        assertEquals(1, mockMessageLogger.getWarns().size());

        setResolveContext("2");
        // clear previous logs
        mockMessageLogger.clear();

        strategy.handleCircularDependency(TestHelper.parseMridsToArray("#A;1.1, #B;1.0"));
        // should log the message
        assertEquals(1, mockMessageLogger.getWarns().size());

        strategy.handleCircularDependency(TestHelper.parseMridsToArray("#A;1.1, #B;1.0"));

        // should not log the message again
        assertEquals(1, mockMessageLogger.getWarns().size());
    }

    private void setResolveContext(String resolveId) {
        IvySettings settings = new IvySettings();
        IvyContext.getContext().setResolveData(
            new ResolveData(new ResolveEngine(settings, new EventManager(),
                    new SortEngine(settings)), new ResolveOptions().setResolveId(resolveId)));
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
