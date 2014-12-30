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
package org.apache.ivy.util.extendable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class UnmodifiableExtendableItem implements ExtendableItem {
    private final Map<String, String> attributes = new HashMap<String, String>();

    private final Map<String, String> unmodifiableAttributesView = Collections
            .unmodifiableMap(attributes);

    private final Map<String, String> extraAttributes = new HashMap<String, String>();

    private final Map<String, String> unmodifiableExtraAttributesView = Collections
            .unmodifiableMap(extraAttributes);

    /*
     * this is the only place where extra attributes are stored in qualified form. In all other maps
     * they are stored unqualified.
     */
    private final Map<String, String> qualifiedExtraAttributes = new HashMap<String, String>();

    private final Map<String, String> unmodifiableQualifiedExtraAttributesView = Collections
            .unmodifiableMap(qualifiedExtraAttributes);

    public UnmodifiableExtendableItem(Map<String, String> stdAttributes,
            Map<String, String> extraAttributes) {
        if (stdAttributes != null) {
            this.attributes.putAll(stdAttributes);
        }
        if (extraAttributes != null) {
            for (Entry<String, String> extraAtt : extraAttributes.entrySet()) {
                setExtraAttribute(extraAtt.getKey(), extraAtt.getValue());
            }
        }
    }

    public String getAttribute(String attName) {
        return attributes.get(attName);
    }

    public String getExtraAttribute(String attName) {
        String v = qualifiedExtraAttributes.get(attName);
        if (v == null) {
            v = extraAttributes.get(attName);
        }
        return v;
    }

    protected void setExtraAttribute(String attName, String attValue) {
        qualifiedExtraAttributes.put(attName, attValue);

        // unqualify att name if required
        int index = attName.indexOf(':');
        if (index != -1) {
            attName = attName.substring(index + 1);
        }
        extraAttributes.put(attName, attValue);
        attributes.put(attName, attValue);
    }

    protected void setStandardAttribute(String attName, String attValue) {
        attributes.put(attName, attValue);
    }

    public Map<String, String> getAttributes() {
        return unmodifiableAttributesView;
    }

    public Map<String, String> getExtraAttributes() {
        return unmodifiableExtraAttributesView;
    }

    public Map<String, String> getQualifiedExtraAttributes() {
        return unmodifiableQualifiedExtraAttributesView;
    }

}
