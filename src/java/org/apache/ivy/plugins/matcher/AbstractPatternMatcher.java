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

package org.apache.ivy.plugins.matcher;

/**
 * An abstract implementation of the pattern matcher providing base template methods
 */
public abstract class AbstractPatternMatcher implements PatternMatcher {
    private final String name;

    /**
     * Create a new instance of a pattern matcher
     * 
     * @param name
     *            the name of the pattern matcher. Never null.
     */
    public AbstractPatternMatcher(/* @NotNull */String name) {
        this.name = name;
    }

    public/* @NotNull */Matcher getMatcher(/* @NotNull */String expression) {
        if (expression == null) {
            throw new NullPointerException();
        }
        if (ANY_EXPRESSION.equals(expression)) {
            return AnyMatcher.INSTANCE;
        }
        return newMatcher(expression);
    }

    public/* @NotNull */String getName() {
        return name;
    }

    /**
     * Returns an instance of the implementation specific matcher.
     * 
     * @param expression
     *            the string to be matched.
     * @return the instance of the related matcher. Never null.
     */
    protected abstract/* @NotNull */Matcher newMatcher(/* @NotNull */String expression);

    @Override
    public String toString() {
        return getName();
    }
}
