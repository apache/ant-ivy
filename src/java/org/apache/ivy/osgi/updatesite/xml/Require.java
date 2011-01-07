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
package org.apache.ivy.osgi.updatesite.xml;

import org.apache.ivy.osgi.util.Version;

public class Require {

    private String plugin;

    private String feature;

    private Version version;

    private String match;

    private String filter;

    public void setFeature(String feature) {
        this.feature = feature;
    }

    public String getFeature() {
        return feature;
    }

    public void setPlugin(String plugin) {
        this.plugin = plugin;
    }

    public String getPlugin() {
        return plugin;
    }

    public void setVersion(Version version) {
        this.version = version;
    }

    public Version getVersion() {
        return version;
    }

    public void setMatch(String match) {
        this.match = match;
    }

    public String getMatch() {
        return match;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public String getFilter() {
        return filter;
    }
}
