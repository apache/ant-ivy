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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.util.Message;

public class ArtifactReportManifestIterable implements Iterable<ManifestAndLocation> {

    private final List<ArtifactDownloadReport> artifactReports;

    public ArtifactReportManifestIterable(List<ArtifactDownloadReport> artifactReports) {
        this.artifactReports = artifactReports;
    }

    public Iterator<ManifestAndLocation> iterator() {
        return new ArtifactReportManifestIterator();
    }

    class ArtifactReportManifestIterator implements Iterator<ManifestAndLocation> {

        private ManifestAndLocation next = null;

        private Iterator<ArtifactDownloadReport> it;

        public ArtifactReportManifestIterator() {
            it = artifactReports.iterator();
        }

        public boolean hasNext() {
            while (next == null && it.hasNext()) {
                ArtifactDownloadReport report = (ArtifactDownloadReport) it.next();
                File artifact = report.getLocalFile();
                JarInputStream in = null;
                try {
                    in = new JarInputStream(new FileInputStream(artifact));
                    Manifest manifest = in.getManifest();
                    if (manifest != null) {
                        next = new ManifestAndLocation(manifest, artifact.toURI());
                        return true;
                    }
                    Message.debug("No manifest in jar: " + artifact);
                } catch (FileNotFoundException e) {
                    Message.debug("Jar file just removed: " + artifact, e);
                } catch (IOException e) {
                    Message.warn("Unreadable jar: " + artifact, e);
                } finally {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException e) {
                            // Don't care
                        }
                    }
                }
            }
            if (next == null) {
                return false;
            }
            return true;
        }

        public ManifestAndLocation next() {
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