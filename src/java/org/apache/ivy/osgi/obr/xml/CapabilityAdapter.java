/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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

import static org.apache.ivy.util.StringUtils.splitToArray;

public class CapabilityAdapter {

    public static void adapt(BundleInfo bundleInfo, Capability capability) throws ParseException {
        String name = capability.getName();
        switch (name) {
            case BundleInfo.PACKAGE_TYPE:
                bundleInfo.addCapability(getExportPackage(bundleInfo, capability));
                break;
            case BundleInfo.BUNDLE_TYPE:
                // nothing to do, already handled at the resource tag level
                break;
            case BundleInfo.SERVICE_TYPE:
                bundleInfo.addCapability(getOSGiService(bundleInfo, capability));
                break;
            default:
                Message.warn("Unsupported capability '" + name + "' on the bundle '"
                        + bundleInfo.getSymbolicName() + "'");
                break;
        }
    }

    private static ExportPackage getExportPackage(BundleInfo bundleInfo, Capability capability)
            throws ParseException {
        String pkgName = null;
        Version version = null;
        String uses = null;
        for (CapabilityProperty property : capability.getProperties()) {
            String propName = property.getName();
            switch (propName) {
                case "package":
                    pkgName = property.getValue();
                    break;
                case "uses":
                    uses = property.getValue();
                    break;
                case "version":
                    version = new Version(property.getValue());
                    break;
                default:
                    Message.warn("Unsupported property '" + propName
                            + "' on the 'package' capability of the bundle '"
                            + bundleInfo.getSymbolicName() + "'");
                    break;
            }
        }
        if (pkgName == null) {
            throw new ParseException("No package name for the capability", 0);
        }
        ExportPackage exportPackage = new ExportPackage(pkgName, version);
        if (uses != null) {
            for (String use : splitToArray(uses)) {
                exportPackage.addUse(use);
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
            switch (propName) {
                case "service":
                    name = property.getValue();
                    break;
                case "version":
                    version = new Version(property.getValue());
                    break;
                default:
                    Message.warn("Unsupported property '" + propName
                            + "' on the 'package' capability of the bundle '"
                            + bundleInfo.getSymbolicName() + "'");
                    break;
            }
        }

        if (name == null) {
            throw new ParseException("No service name for the capability", 0);
        }
        return new BundleCapability(BundleInfo.SERVICE_TYPE, name, version);
    }
}
