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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class MapMatcher {
    private Map<String, Matcher> matchers = new HashMap<String, Matcher>();

    private PatternMatcher pm;

    private Map<String, String> attributes;

    public MapMatcher(Map<String, String> attributes, PatternMatcher pm) {
        this.attributes = attributes;
        this.pm = pm;
        for (Entry<String, String> entry : attributes.entrySet()) {
            String value = entry.getValue();
            if (value != null) {
                matchers.put(entry.getKey(), pm.getMatcher(value));
            }
        }
    }

    public boolean matches(Map<String, String> m) {
        for (Entry<String, Matcher> entry : matchers.entrySet()) {
            Matcher matcher = entry.getValue();
            String value = m.get(entry.getKey());
            if ((value == null) || !matcher.matches(value)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        return attributes + " (" + pm.getName() + ")";
    }

    public Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public PatternMatcher getPatternMatcher() {
        return pm;
    }
}
