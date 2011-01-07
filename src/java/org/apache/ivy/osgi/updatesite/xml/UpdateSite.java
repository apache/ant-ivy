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

public class UpdateSite {

    private String url;

    private String mirrorsURL;

    private boolean pack200;

    private String digestUrl;

    private List/* <EclipseFeature> */features = new ArrayList();

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setMirrorsURL(String mirrorsURL) {
        this.mirrorsURL = mirrorsURL;
    }

    public void setPack200(boolean pack200) {
        this.pack200 = pack200;
    }

    public void setDigestURL(String digestURL) {
        this.digestUrl = digestURL;
    }

    public String getDigestURL() {
        return digestUrl;
    }

    public void addFeature(EclipseFeature feature) {
        features.add(feature);
    }

    public List getFeatures() {
        return features;
    }

    public void setAssociateSitesURL(String associateSitesURL) {
        // TODO what's that ?
    }

}
