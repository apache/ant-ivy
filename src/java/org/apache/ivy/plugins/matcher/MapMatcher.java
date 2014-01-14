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
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class MapMatcher {
    private Map/* <String, Matcher> */matchers = new HashMap();

    private PatternMatcher pm;

    private Map attributes;

    public MapMatcher(Map attributes, PatternMatcher pm) {
        this.attributes = attributes;
        this.pm = pm;
        for (Iterator iter = attributes.entrySet().iterator(); iter.hasNext();) {
            Entry entry = (Entry) iter.next();
            String value = (String) entry.getValue();
            if (value != null) {
                matchers.put(entry.getKey(), pm.getMatcher(value));
            }
        }
    }

    public boolean matches(Map/* <String,String> */m) {
        for (Iterator iter = matchers.entrySet().iterator(); iter.hasNext();) {
            Entry entry = (Entry) iter.next();

            Matcher matcher = (Matcher) entry.getValue();
            String value = (String) m.get(entry.getKey());
            if ((value == null) || !matcher.matches(value)) {
                return false;
            }
        }

        return true;
    }

    public String toString() {
        return attributes + " (" + pm.getName() + ")";
    }

    public Map getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public PatternMatcher getPatternMatcher() {
        return pm;
    }
}
