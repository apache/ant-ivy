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
package org.apache.ivy.osgi.ivy.internal;


import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.osgi.ivy.OsgiManifestParser;
import org.apache.ivy.osgi.util.Version;
import org.apache.ivy.osgi.util.VersionComparator;
import org.apache.ivy.osgi.util.VersionRange;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.util.Message;


import static org.apache.ivy.osgi.ivy.OsgiManifestParser.PACKAGE;

public class PackageRegistry {

    private final static PackageRegistry instance = new PackageRegistry();

    public static PackageRegistry getInstance() {
        return instance;
    }

    private static final String PKGREF = ".pkgref";

    private final OsgiManifestParser osgiManifestParser = new OsgiManifestParser();

    private final Set<String> processedEntries = new HashSet<String>();

    private File cacheDir;

    private PackageRegistry() {
        // nothing to initialize
    }

    public void processExports(File cacheDirectory, Resource res) throws IOException {
        this.cacheDir = cacheDirectory;

        if (processedEntries.contains(res.getName()) || !osgiManifestParser.accept(res)) {
            return;
        }

        ModuleDescriptor md;
        try {
            md = osgiManifestParser.parseExports(res);
        } catch (Exception e) {
            Message.error("\t\tFailed to parse package resource descriptor: " + res);
            e.printStackTrace();
            return;
        }
        
        if(md == null) {
            return;
        }

        final ModuleRevisionId mrid = md.getResolvedModuleRevisionId();

        final File pkgRootDir = new File(cacheDir, PACKAGE);
        pkgRootDir.mkdirs();

        for (DependencyDescriptor dep : md.getDependencies()) {
            final ModuleRevisionId depMrid = dep.getDependencyRevisionId();
            if (depMrid.getOrganisation().equalsIgnoreCase(PACKAGE)) {
                final File pkgDir = new File(pkgRootDir, (depMrid.getName().replace('.', '/') + "/"));
                pkgDir.mkdirs();
                final File file = new File(pkgDir, mrid + PKGREF);
                if(!file.exists()) {
                    Message.debug("\t\tWriting pkg ref: " + file);
                    file.createNewFile();
                }
            }
        }

        processedEntries.add(res.getName());
    }

    public ModuleRevisionId processImports(final String pkgName, final VersionRange importRange) {
        final File pkgRootDir = new File(cacheDir, PACKAGE);
        if (!pkgRootDir.canRead() || !pkgRootDir.isDirectory()) {
            return null;
        }

        final File pkgDir = new File(pkgRootDir, pkgName.replace('.', '/') + "/");
        
        final TreeMap<Version, ModuleRevisionId> pkgMrids = new TreeMap<Version, ModuleRevisionId>(VersionComparator.DESCENDING);
        pkgDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                if (name.endsWith(PKGREF)) {
                    final String baseName = name.substring(0, (name.length() - PKGREF.length()));
                    final int hashIdx = baseName.indexOf('#');
                    final int semicolIdx = baseName.indexOf(';');
                    final String mridOrg = baseName.substring(0, hashIdx);
                    final String[] tokens = baseName.substring(hashIdx + 1, semicolIdx).split("[#]");
                    final String mridName = tokens[0];
                    final String mridBranch = (tokens.length > 1 ? tokens[1] : null);
                    final String mridRev = baseName.substring(semicolIdx + 1);
                    pkgMrids.put(new Version(mridRev), new ModuleRevisionId(new ModuleId(mridOrg, mridName), mridBranch, mridRev));
                }
                return false;
            }
        });

        ModuleRevisionId matchingMrid = null;
        for (Entry<Version, ModuleRevisionId> entry : pkgMrids.entrySet()) {
            if (importRange == null || importRange.contains(entry.getKey())) {
                matchingMrid = entry.getValue();
                break;
            }
        }

        return matchingMrid;
    }
}
