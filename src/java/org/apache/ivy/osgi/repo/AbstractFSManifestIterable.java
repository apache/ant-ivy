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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.apache.ivy.util.Message;

public abstract class AbstractFSManifestIterable { //implements Iterable/* <ManifestAndLocation> */{

    public Iterator/* <ManifestAndLocation> */iterator() {
        return new FSManifestIterator();
    }

    abstract protected List/* <String> */listBundleFiles(String dir) throws IOException;

    abstract protected List/* <String> */listDirs(String dir) throws IOException;

    abstract protected InputStream getInputStream(String f) throws IOException;

    protected String createBundleLocation(String location) {
        return location;
    }

    class FSManifestIterator implements Iterator/* <ManifestAndLocation> */{

        private ManifestAndLocation next = null;

        /**
         * Stack of list of directories. An iterator in the stack represents the current directory
         * being lookup. The first element in the stack is the root directory. The next element in
         * the stack is an iterator on the children on the root. The last iterator in the stack
         * points to {@link #currentDir}.
         */
        private Stack/* <Iterator<String>> */dirs = new Stack/* <Iterator<String>> */();

        /**
         * The bundles files being lookup.
         */
        private Iterator/* <String> */bundleCandidates = null;

        private String currentDir = null;

        FSManifestIterator() {
            dirs.add(Collections.singleton("").iterator());
        }

        /**
         * Deep first tree lookup for the directories and the bundles are searched on each found
         * directory.
         */
        public boolean hasNext() {
            while (next == null) {
                // no current directory
                if (currentDir == null) {
                    // so get the next one
                    if (((Iterator) dirs.peek()).hasNext()) {
                        currentDir = (String) ((Iterator) dirs.peek()).next();
                        try {
                            bundleCandidates = listBundleFiles(currentDir).iterator();
                        } catch (IOException e) {
                            Message.warn("Unlistable dir: " + currentDir + " (" + e + ")");
                            currentDir = null;
                        }
                    } else if (dirs.size() <= 1) {
                        // no next directory, but we are at the root: finished
                        return false;
                    } else {
                        // remove the top of the stack and continue with a sibling.
                        dirs.pop();
                    }
                } else if (bundleCandidates.hasNext()) {
                    String bundleCandidate = (String) bundleCandidates.next();
                    try {
                        JarInputStream in = new JarInputStream(getInputStream(bundleCandidate));
                        Manifest manifest = in.getManifest();
                        if (manifest != null) {
                            next = new ManifestAndLocation(manifest,
                                    createBundleLocation(bundleCandidate));
                        } else {
                            Message.debug("No manifest in jar: " + bundleCandidate);
                        }
                    } catch (FileNotFoundException e) {
                        Message.debug("Jar file just removed: " + bundleCandidate + " (" + e + ")");
                    } catch (IOException e) {
                        Message.warn("Unreadable jar: " + bundleCandidate + " (" + e + ")");
                    }
                } else {
                    // no more candidate on the current directory
                    // so lookup in the children directories
                    try {
                        dirs.add(listDirs(currentDir).iterator());
                    } catch (IOException e) {
                        Message.warn("Unlistable dir: " + currentDir + " (" + e + ")");
                        dirs.add(Collections.EMPTY_LIST.iterator());
                    }
                    currentDir = null;
                }
            }
            return true;
        }

        public Object next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            ManifestAndLocation manifest = next;
            next = null;
            return manifest;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}