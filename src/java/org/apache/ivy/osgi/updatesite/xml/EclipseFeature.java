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

import java.util.ArrayList;
import java.util.List;

import org.apache.ivy.osgi.util.Version;

public class EclipseFeature {

    private String id;

    private Version version;

    private List<EclipsePlugin> plugins = new ArrayList<EclipsePlugin>();

    private List<Require> requires = new ArrayList<Require>();

    private String url;

    private String description;

    private String license;

    public EclipseFeature(String id, Version version) {
        this.id = id;
        this.version = version;
        this.url = "features/" + id + '_' + version + ".jar";
    }

    public void setURL(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setType(String type) {
        // TODO Auto-generated method stub
    }

    public String getId() {
        return id;
    }

    public Version getVersion() {
        return version;
    }

    public void setLabel(String label) {
        // TODO Auto-generated method stub

    }

    public void setOS(String os) {
        // TODO Auto-generated method stub

    }

    public void setWS(String ws) {
        // TODO Auto-generated method stub

    }

    public void setNL(String nl) {
        // TODO Auto-generated method stub

    }

    public void setArch(String arch) {
        // TODO Auto-generated method stub

    }

    public void setPatch(String patch) {
        // TODO Auto-generated method stub

    }

    public void addCategory(String name) {
        // TODO Auto-generated method stub

    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setCopyright(String trim) {
        // not useful
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public String getLicense() {
        return license;
    }

    public void addPlugin(EclipsePlugin plugin) {
        plugins.add(plugin);
    }

    public List<EclipsePlugin> getPlugins() {
        return plugins;
    }

    public void addRequire(Require require) {
        requires.add(require);
    }

    public List<Require> getRequires() {
        return requires;
    }

    public void setApplication(String value) {
        // TODO Auto-generated method stub

    }

    public void setPlugin(String value) {
        // TODO Auto-generated method stub

    }

    public void setExclusive(boolean booleanValue) {
        // TODO Auto-generated method stub

    }

    public void setPrimary(boolean booleanValue) {
        // TODO Auto-generated method stub

    }

    public void setColocationAffinity(String value) {
        // TODO Auto-generated method stub

    }

    public void setProviderName(String value) {
        // TODO Auto-generated method stub

    }

    public void setImage(String value) {
        // TODO Auto-generated method stub

    }

    public String toString() {
        return id + "#" + version;
    }
}
