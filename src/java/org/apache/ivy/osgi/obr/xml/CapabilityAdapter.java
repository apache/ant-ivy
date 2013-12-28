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

import java.text.ParseException;

import org.apache.ivy.osgi.core.BundleCapability;
import org.apache.ivy.osgi.core.BundleInfo;
import org.apache.ivy.osgi.core.ExportPackage;
import org.apache.ivy.osgi.util.Version;
import org.apache.ivy.util.Message;

public class CapabilityAdapter {

    public static void adapt(BundleInfo bundleInfo, Capability capability) throws ParseException {
        String name = capability.getName();
        if (BundleInfo.PACKAGE_TYPE.equals(name)) {
            ExportPackage exportPackage = getExportPackage(bundleInfo, capability);
            bundleInfo.addCapability(exportPackage);
        } else if (BundleInfo.BUNDLE_TYPE.equals(name)) {
            // nothing to do, already handled at the resource tag level
        } else if (BundleInfo.SERVICE_TYPE.equals(name)) {
            BundleCapability service = getOSGiService(bundleInfo, capability);
            bundleInfo.addCapability(service);
        } else {
            Message.warn("Unsupported capability '" + name + "' on the bundle '"
                    + bundleInfo.getSymbolicName() + "'");
        }
    }

    private static ExportPackage getExportPackage(BundleInfo bundleInfo, Capability capability)
            throws ParseException {
        String pkgName = null;
        Version version = null;
        String uses = null;
        for (CapabilityProperty property : capability.getProperties()) {
            String propName = property.getName();
            if ("package".equals(propName)) {
                pkgName = property.getValue();
            } else if ("version".equals(propName)) {
                version = new Version(property.getValue());
            } else if ("uses".equals(propName)) {
                uses = property.getValue();
            } else {
                Message.warn("Unsupported property '" + propName
                        + "' on the 'package' capability of the bundle '"
                        + bundleInfo.getSymbolicName() + "'");
            }
        }
        if (pkgName == null) {
            throw new ParseException("No package name for the capability", 0);
        }
        ExportPackage exportPackage = new ExportPackage(pkgName, version);
        if (uses != null) {
            String[] split = uses.trim().split(",");
            for (int i = 0; i < split.length; i++) {
                String u = split[i];
                exportPackage.addUse(u.trim());
            }
        }
        return exportPackage;
    }

    private static BundleCapability getOSGiService(BundleInfo bundleInfo, Capability capability)
            throws ParseException {
        String name = null;
        Version version = null;

        for (CapabilityProperty property : capability.getProperties()) {
            String propName = property.getName();
            if ("service".equals(propName)) {
                name = property.getValue();
            } else if ("version".equals(propName)) {
                version = new Version(property.getValue());
            } else {
                Message.warn("Unsupported property '" + propName
                        + "' on the 'package' capability of the bundle '"
                        + bundleInfo.getSymbolicName() + "'");
            }
        }

        if (name == null) {
            throw new ParseException("No service name for the capability", 0);
        }
        BundleCapability service = new BundleCapability(BundleInfo.SERVICE_TYPE, name, version);
        return service;
    }
}
