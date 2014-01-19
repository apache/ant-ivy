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
 * Interface for a pattern matcher.
 * <p/>
 * The pattern matcher is the main abstraction regarding the matching of an expression.
 * Implementation may vary depending on the expression syntax handling that is desired.
 */
public interface PatternMatcher {

    /**
     * 'exact' pattern matcher name
     */
    public static final String EXACT = "exact";

    /**
     * pattern matcher name 'regexp'
     */
    public static final String REGEXP = "regexp";

    /**
     * pattern matcher 'glob'
     */
    public static final String GLOB = "glob";

    /**
     * pattern matcher name 'exactOrRegexp'
     */
    public static final String EXACT_OR_REGEXP = "exactOrRegexp";

    /**
     * Any expression string: '*'
     */
    public static final String ANY_EXPRESSION = "*";

    /**
     * Return the matcher for the given expression.
     * 
     * @param expression
     *            the expression to be matched. Cannot be null ?
     * @return the matcher instance for the given expression. Never null.
     */
    public/* @NotNull */Matcher getMatcher(/* @NotNull */String expression);

    /**
     * return the name of this pattern matcher
     * 
     * @return the name of this pattern matcher. Never null.
     * @see #EXACT
     * @see #REGEXP
     * @see #GLOB
     * @see #EXACT_OR_REGEXP
     */
    public/* @NotNull */String getName();
}
