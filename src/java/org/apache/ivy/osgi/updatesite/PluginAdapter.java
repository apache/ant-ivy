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
package org.apache.ivy.osgi.updatesite;

import java.net.URI;
import java.util.Iterator;

import org.apache.ivy.osgi.core.BundleInfo;
import org.apache.ivy.osgi.core.BundleRequirement;
import org.apache.ivy.osgi.updatesite.xml.EclipseFeature;
import org.apache.ivy.osgi.updatesite.xml.EclipsePlugin;
import org.apache.ivy.osgi.updatesite.xml.Require;
import org.apache.ivy.osgi.util.VersionRange;

public class PluginAdapter {

    public static BundleInfo featureAsBundle(URI baseUri, EclipseFeature feature) {
        BundleInfo b = new BundleInfo(feature.getId(), feature.getVersion());

        if (feature.getUrl() == null) {
            b.setUri(baseUri.resolve("features/" + feature.getId() + '_' + feature.getVersion()
                    + ".jar"));
        } else {
            b.setUri(baseUri.resolve(feature.getUrl()));
        }

        b.setDescription(feature.getDescription());
        b.setLicense(feature.getLicense());

        Iterator itPlugins = feature.getPlugins().iterator();
        while (itPlugins.hasNext()) {
            EclipsePlugin plugin = (EclipsePlugin) itPlugins.next();
            BundleRequirement r = new BundleRequirement(BundleInfo.BUNDLE_TYPE, plugin.getId(),
                    new VersionRange(plugin.getVersion()), null);
            b.addRequirement(r);
        }

        Iterator itRequires = feature.getRequires().iterator();
        while (itRequires.hasNext()) {
            Require require = (Require) itRequires.next();
            String id;
            if (require.getPlugin() != null) {
                id = require.getPlugin();
            } else {
                id = require.getFeature();
            }
            VersionRange range;
            if (require.getMatch().equals("greaterOrEqual")) {
                range = new VersionRange(require.getVersion());
            } else {
                throw new IllegalStateException("unsupported match " + require.getMatch());
            }
            BundleRequirement r = new BundleRequirement(BundleInfo.BUNDLE_TYPE, id, range, null);
            b.addRequirement(r);
        }

        return b;
    }

    public static BundleInfo pluginAsBundle(URI baseUri, EclipsePlugin plugin) {
        BundleInfo b = new BundleInfo(plugin.getId(), plugin.getVersion());

        b.setUri(baseUri.resolve("plugins/" + plugin.getId() + '_' + plugin.getVersion() + ".jar"));

        return b;
    }
}
