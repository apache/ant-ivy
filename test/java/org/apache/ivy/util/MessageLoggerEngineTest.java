/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ivy.util;

import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class MessageLoggerEngineTest {

    /**
     * Tests the issue reported in IVY-1628 and verifies that the {@link MessageLogger#sumupProblems()}
     * doesn't run into {@link java.util.ConcurrentModificationException} when multiple threads are
     * using the logger
     */
    @Test
    public void testConcurrentSumupProblems() throws Exception {
        final MessageLoggerEngine engine = new MessageLoggerEngine();
        final LoggingTask loggingTask = new LoggingTask(engine);
        final ExecutorService executor = Executors.newFixedThreadPool(1);
        final Future<Void> result;
        try {
            result = executor.submit(loggingTask);
            // keep invoking sumupProblems in this thread while the other thread is logging
            // error and warn messages
            for (int i = 0; i < 1000; i++) {
                engine.sumupProblems();
            }
            loggingTask.stop = true;
        } finally {
            loggingTask.stop = true;
            executor.shutdownNow();
        }
        result.get(2, TimeUnit.SECONDS);
    }

    private final class LoggingTask implements Callable<Void> {

        private final MessageLoggerEngine engine;
        private volatile boolean stop;

        private LoggingTask(final MessageLoggerEngine engine) {
            this.engine = engine;
        }

        @Override
        public Void call() throws Exception {
            do {
                for (int i = 0; i < 1000; i++) {
                    if (stop) {
                        return null;
                    }
                    if (i % 2 == 0) {
                        engine.error("Dummy error");
                    } else {
                        engine.warn("Dummy warn");
                    }
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    return null;
                }
            } while (!stop);
            return null;
        }
    }
}
