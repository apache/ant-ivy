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
package org.apache.ivy.core.module.id;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.plugins.matcher.ExactPatternMatcher;
import org.apache.ivy.plugins.matcher.MapMatcher;
import org.apache.ivy.plugins.matcher.PatternMatcher;

/**
 * This class targets to speed up lookup for exact pattern matcher by keys, which are created with
 * (organization, module) information. When exact pattern matcher is added, the key is created from
 * matcher's attributes. When matcher is looked up against specific module, the key is recreated
 * from module's attributes.
 * <p>
 * </p>
 * The lookup doesn't target to speed up lookup for non exact pattern matcher. All non exact
 * matchers are placed in non-keyed collection.
 * <p>
 * </p>
 * At lookup for matchers against specific module, all non exact pattern matchers are iterated to
 * match with module attributes, and exact pattern matchers binding to the same key will also
 * iterated to match with module attributes.
 * <p>
 * </p>
 * If there are much more exact pattern matchers than non exact pattern matchers, the matcher lookup
 * speed can benefit from this class significantly. A quick example could be user declares lots of
 * dependencyOverrides which are typically exact pattern matchers.
 * <p>
 * </p>
 * If there are balanced exact and non exact pattern matchers, the matcher lookup speed doesn't hurt
 * by this class.
 * <p>
 * </p>
 */
public class MatcherLookup {

    // private static final String FORMAT = "{org:%s, module:%s}";

    private static final String DEFAULT = "{org:" + "default" + ", module:" + "default" + "}";

    private Map<String, List<MapMatcher>> lookup = new HashMap<String, List<MapMatcher>>();

    private List<MapMatcher> non_exact_matchers = new ArrayList<MapMatcher>();

    /**
     * Add matcher.
     * 
     * If matcher is exact pattern matcher, it will be associated with a key and placed in keyed
     * collection.
     * 
     * If matcher is not exact pattern matcher, it will be placed into non-keyed collection
     * 
     * @param matcher
     */
    public void add(MapMatcher matcher) {
        if (!(matcher.getPatternMatcher() instanceof ExactPatternMatcher)) {
            non_exact_matchers.add(matcher);
            return;
        }
        String key = key(matcher.getAttributes());
        List<MapMatcher> exact_matchers = lookup.get(key);
        if (exact_matchers == null) {
            exact_matchers = new ArrayList<MapMatcher>();
            lookup.put(key, exact_matchers);
        }
        exact_matchers.add(matcher);
    }

    /**
     * Get a list of matchers which can apply to module with specified attributes
     * 
     * @param attrs
     *            A map of attributes that matcher should match.
     * 
     * @return list A list of candidate matchers that matches specified attributes
     */
    public List<MapMatcher> get(Map<String, String> attrs) {
        List<MapMatcher> matchers = new ArrayList<MapMatcher>();
        // Step 1: find matchers from non_exact_matchers list
        if (!non_exact_matchers.isEmpty()) {
            for (MapMatcher matcher : non_exact_matchers) {
                if (matcher.matches(attrs)) {
                    matchers.add(matcher);
                }
            }
        }
        // Step 2: find matchers from exact_matchers list of key
        String key = key(attrs);
        List<MapMatcher> exact_matchers = lookup.get(key);
        if (exact_matchers != null) {
            for (MapMatcher matcher : exact_matchers) {
                if (matcher.matches(attrs)) {
                    matchers.add(matcher);
                }
            }
        }
        // Step 3: (iff key != DEFAULT) find matchers from exact_matchers of DEFAULT
        if (key != DEFAULT) {
            List<MapMatcher> default_exact_matchers = lookup.get(DEFAULT);
            if (default_exact_matchers != null) {
                for (MapMatcher matcher : default_exact_matchers) {
                    if (matcher.matches(attrs)) {
                        matchers.add(matcher);
                    }
                }
            }
        }
        return matchers;
    }

    /**
     * Create a key from specified attributes
     * 
     * @param attrs
     *            A map of attributes
     * @return key object
     */
    private String key(Map<String, String> attrs) {
        String org = attrs.get(IvyPatternHelper.ORGANISATION_KEY);
        String module = attrs.get(IvyPatternHelper.MODULE_KEY);
        if (org == null || PatternMatcher.ANY_EXPRESSION.equals(org) || module == null
                || PatternMatcher.ANY_EXPRESSION.equals(module)) {
            return DEFAULT;
        }
        return "{org:" + org + ", module:" + module + "}";
    }

}
