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
package org.apache.ivy.core.module.descriptor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExtraInfoHolder {

    private String name;

    private Map<String, String> attributes = new LinkedHashMap<String, String>();

    private String content;

    private List<ExtraInfoHolder> nestedExtraInfoHolder = new ArrayList<ExtraInfoHolder>();

    public ExtraInfoHolder() {

    }

    public ExtraInfoHolder(String name, String content) {
        this.name = name;
        this.content = content;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<ExtraInfoHolder> getNestedExtraInfoHolder() {
        return nestedExtraInfoHolder;
    }

    public void setNestedExtraInfoHolder(List<ExtraInfoHolder> nestedExtraInfoHolder) {
        this.nestedExtraInfoHolder = nestedExtraInfoHolder;
    }

}
