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
package org.apache.ivy.osgi.obr.xml;

import java.util.ArrayList;
import java.util.List;

public class Capability {

    private List<CapabilityProperty> properties = new ArrayList<CapabilityProperty>();

    private String name;

    public Capability(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void addProperty(String n, String value, String type) {
        properties.add(new CapabilityProperty(n, value, type));
    }

    public List<CapabilityProperty> getProperties() {
        return properties;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(name);
        for (CapabilityProperty p : properties) {
            buffer.append(" ");
            buffer.append(p);
        }
        return buffer.toString();
    }
}
