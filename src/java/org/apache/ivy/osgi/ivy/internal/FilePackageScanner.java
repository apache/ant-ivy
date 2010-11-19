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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.ivy.core.cache.DefaultRepositoryCacheManager;
import org.apache.ivy.plugins.repository.file.FileResource;
import org.apache.ivy.plugins.resolver.ResolverSettings;
import org.apache.ivy.util.Message;

public class FilePackageScanner {

    private final Map/* <String, Collection<File>> */patternFiles = new HashMap/*
                                                                                * <String,
                                                                                * Collection<File>>
                                                                                */();

    private final boolean useFileCache = false;

    public void scanAllPackageExportHeaders(List/* <String> */ivyPatterns, ResolverSettings settings) {
        final DefaultRepositoryCacheManager cacheManager = (DefaultRepositoryCacheManager) settings
                .getDefaultRepositoryCacheManager();

        Iterator itPatterns = ivyPatterns.iterator();
        while (itPatterns.hasNext()) {
            String ivyPattern = (String) itPatterns.next();
            Collection/* <File> */fileList = null;
            fileList = (Collection) patternFiles.get(ivyPattern);
            if (fileList == null || !useFileCache) {
                fileList = new ArrayList/* <File> */();
                patternFiles.put(ivyPattern, fileList);
                final File rootDir = new File(ivyPattern.split("\\[[\\w]+\\]")[0]);
                scanDir(rootDir, fileList);
            }

            Iterator itFile = fileList.iterator();
            while (itFile.hasNext()) {
                File currFile = (File) itFile.next();
                try {
                    PackageRegistry.getInstance().processExports(cacheManager.getBasedir(),
                        new FileResource(null, currFile));
                } catch (IOException e) {
                    Message.error("Failed to process exports for file: " + currFile);
                }
            }
        }
    }

    protected void scanDir(File currFile, Collection/* <File> */fileList) {
        if (!currFile.canRead()) {
            return;
        }

        if (currFile.isDirectory()) {
            List/* <File> */files = new ArrayList/* <File> */();
            File[] listFiles = currFile.listFiles();
            for (int i = 0; i < listFiles.length; i++) {
                File file = listFiles[i];
                files.add(file);
                // pre-process to check if we have recursed into an exploded bundle
                if (file.getPath().endsWith("META-INF")) {
                    fileList.add(new File(file, "MANIFEST.MF"));
                    return;
                }
            }
            // continue scanning...
            Iterator itFile = files.iterator();
            while (itFile.hasNext()) {
                File file = (File) itFile.next();
                scanDir(file, fileList);
            }
        } else if (currFile.isFile()) {
            final String path = currFile.getPath();
            if (path.toUpperCase().endsWith("META-INF/MANIFEST.MF")
                    || path.toUpperCase().endsWith(".JAR")) {
                fileList.add(currFile);
            }
        }
    }
}
