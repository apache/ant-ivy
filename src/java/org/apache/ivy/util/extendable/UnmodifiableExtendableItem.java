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

public class UnmodifiableExtendableItem implements ExtendableItem {
    private Map attributes = new HashMap();

    private Map unmodifiableAttributesView = Collections.unmodifiableMap(attributes);

    private Map stdAttributes = new HashMap();

    private Map unmodifiableStdAttributesView = Collections.unmodifiableMap(stdAttributes);

    private Map extraAttributes = new HashMap();

    private Map unmodifiableExtraAttributesView = Collections.unmodifiableMap(extraAttributes);

    public UnmodifiableExtendableItem(Map stdAttributes, Map extraAttributes) {
        if (stdAttributes != null) {
            this.attributes.putAll(stdAttributes);
            this.stdAttributes.putAll(stdAttributes);
        }
        if (extraAttributes != null) {
            this.attributes.putAll(extraAttributes);
            this.extraAttributes.putAll(extraAttributes);
        }
    }

    public String getAttribute(String attName) {
        return (String) attributes.get(attName);
    }

    public String getExtraAttribute(String attName) {
        return (String) extraAttributes.get(attName);
    }

    public String getStandardAttribute(String attName) {
        return (String) stdAttributes.get(attName);
    }

    protected void setExtraAttribute(String attName, String attValue) {
        setAttribute(attName, attValue, true);
    }

    protected void setStandardAttribute(String attName, String attValue) {
        setAttribute(attName, attValue, false);
    }

    protected void setAttribute(String attName, String attValue, boolean extra) {
        if (extra) {
            extraAttributes.put(attName, attValue);
        } else {
            stdAttributes.put(attName, attValue);
        }
        attributes.put(attName, attValue);
    }

    public Map getAttributes() {
        return unmodifiableAttributesView;
    }

    public Map getStandardAttributes() {
        return unmodifiableStdAttributesView;
    }

    public Map getExtraAttributes() {
        return unmodifiableExtraAttributesView;
    }

}
