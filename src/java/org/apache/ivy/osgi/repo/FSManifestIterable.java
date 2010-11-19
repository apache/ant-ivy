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
package org.apache.ivy.osgi.repo;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FSManifestIterable extends AbstractFSManifestIterable {

    /**
     * List of directory name that usually contains jars but are not bundles
     */
    public static final Set<String> NON_BUNDLE_DIRS = new HashSet<String>(Arrays.asList("source", "sources", "javadoc",
            "javadocs", "doc", "docs"));

    /**
     * Default directory filter that doesn't select .svn directories, neither the directories that match
     * {@link #NON_BUNDLE_DIRS}.
     */
    public static final FilenameFilter DEFAULT_DIR_FILTER = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            return !name.equals(".svn") && !NON_BUNDLE_DIRS.contains(name);
        }
    };

    /**
     * Default bundle filter that select only .jar files
     */
    public static final FilenameFilter DEFAULT_BUNLDE_FILTER = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            return name.endsWith(".jar");
        }
    };

    private FilenameFilter dirFilter = DEFAULT_DIR_FILTER;

    private FilenameFilter bundleFilter = DEFAULT_BUNLDE_FILTER;

    private String root;

    private final String basePath;

    /**
     * Default constructor
     * 
     * @param root
     *            the root directory of the file system to lookup
     * @param basePath
     *            path the found locations should be append to
     */
    public FSManifestIterable(File root, String basePath) {
        this.basePath = basePath;
        this.root = root.getAbsolutePath();
    }

    public FilenameFilter getDirFilter() {
        return dirFilter;
    }

    public void setDirFilter(FilenameFilter dirFilter) {
        this.dirFilter = dirFilter;
    }

    public FilenameFilter getBundleFilter() {
        return bundleFilter;
    }

    public void setBundleFilter(FilenameFilter bundleFilter) {
        this.bundleFilter = bundleFilter;
    }

    @Override
    protected String createBundleLocation(String location) {
        return basePath + location;
    }

    @Override
    protected InputStream getInputStream(String f) throws FileNotFoundException {
        return new FileInputStream(new File(root, f));
    }

    @Override
    protected List<String> listBundleFiles(String dir) {
        return fileArray2pathList(new File(root, dir).listFiles(new FileFilter() {
            public boolean accept(File f) {
                if (!f.isFile()) {
                    return false;
                }
                return bundleFilter.accept(f.getParentFile(), f.getName());
            }
        }));
    }

    private List<String> fileArray2pathList(File[] files) {
        ArrayList<String> list = new ArrayList<String>(files.length);
        for (int i = 0; i < files.length; i++) {
            list.add(files[i].getAbsolutePath().substring(root.length() + 1));
        }
        return list;
    }

    @Override
    protected List<String> listDirs(String dir) {
        return fileArray2pathList(new File(root, dir).listFiles(new FileFilter() {
            public boolean accept(File f) {
                if (!f.isDirectory()) {
                    return false;
                }
                return dirFilter == null || dirFilter.accept(f.getParentFile(), f.getName());
            }
        }));
    }
}