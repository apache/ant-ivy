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
package org.apache.ivy.osgi.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ManifestHeaderElement {
    private List<String> values = new ArrayList<String>();

    private Map<String, String> attributes = new HashMap<String, String>();

    private Map<String, String> directives = new HashMap<String, String>();

    public List<String> getValues() {
        return values;
    }

    public void addValue(String value) {
        values.add(value);
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void addAttribute(String name, String value) {
        attributes.put(name, value);
    }

    public Map<String, String> getDirectives() {
        return directives;
    }

    public void addDirective(String name, String value) {
        directives.put(name, value);
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ManifestHeaderElement)) {
            return false;
        }
        ManifestHeaderElement other = (ManifestHeaderElement) obj;
        if (other.values.size() != values.size()) {
            return false;
        }
        for (String value : values) {
            if (!other.values.contains(value)) {
                return false;
            }
        }
        if (other.directives.size() != directives.size()) {
            return false;
        }
        for (Entry<String, String> directive : directives.entrySet()) {
            if (!directive.getValue().equals(other.directives.get(directive.getKey()))) {
                return false;
            }
        }
        if (other.attributes.size() != attributes.size()) {
            return false;
        }
        for (Entry<String, String> attribute : attributes.entrySet()) {
            if (!attribute.getValue().equals(other.attributes.get(attribute.getKey()))) {
                return false;
            }
        }
        return true;
    }

    public String toString() {
        String string = "";
        Iterator<String> itValues = values.iterator();
        while (itValues.hasNext()) {
            string = string.concat(itValues.next());
            if (itValues.hasNext()) {
                string = string.concat(";");
            }
        }
        for (Entry<String, String> directive : directives.entrySet()) {
            string = string.concat(";");
            string = string.concat(directive.getKey());
            string = string.concat(":=");
            string = string.concat(directive.getValue());
        }
        for (Entry<String, String> attribute : attributes.entrySet()) {
            string = string.concat(";");
            string = string.concat(attribute.getKey());
            string = string.concat("=");
            string = string.concat(attribute.getValue());
        }
        return string;
    }
}
