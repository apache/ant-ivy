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

public class SortOptions {
    public static final SortOptions DEFAULT = new SortOptions();

    public static final SortOptions SILENT = new SortOptions().setNonMatchingVersionReporter(
        new SilentNonMatchingVersionReporter()).setUseCircularDependencyStrategy(false);

    /**
     * Used to report some non matching version (when a modules depends on a specific revision of an
     * other modules present in the of modules to sort with a different revision.
     */
    private NonMatchingVersionReporter nonMatchingVersionReporter = new WarningNonMatchingVersionReporter();

    /**
     * Should the default circular dependency strategy be used when a circular dependency is found,
     * or should circular dependencies be ignored?
     */
    private boolean useCircularDependencyStrategy = true;

    public NonMatchingVersionReporter getNonMatchingVersionReporter() {
        return nonMatchingVersionReporter;
    }

    public SortOptions setNonMatchingVersionReporter(
            NonMatchingVersionReporter nonMatchingVersionReporter) {
        this.nonMatchingVersionReporter = nonMatchingVersionReporter;
        return this;
    }

    public boolean isUseCircularDependencyStrategy() {
        return useCircularDependencyStrategy;
    }

    public SortOptions setUseCircularDependencyStrategy(boolean useCircularDependencyStrategy) {
        this.useCircularDependencyStrategy = useCircularDependencyStrategy;
        return this;
    }
}
