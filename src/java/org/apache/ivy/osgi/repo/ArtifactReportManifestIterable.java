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
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.util.Message;

public class ArtifactReportManifestIterable implements Iterable<ManifestAndLocation> {

    private final Map<ModuleRevisionId, List<ArtifactDownloadReport>> artifactReports = new HashMap<ModuleRevisionId, List<ArtifactDownloadReport>>();

    private List<String> sourceTypes;

    public ArtifactReportManifestIterable(List<ArtifactDownloadReport> reports,
            List<String> sourceTypes) {
        this.sourceTypes = sourceTypes;
        for (ArtifactDownloadReport report : reports) {
            ModuleRevisionId mrid = report.getArtifact().getModuleRevisionId();
            List<ArtifactDownloadReport> moduleReports = artifactReports.get(mrid);
            if (moduleReports == null) {
                moduleReports = new ArrayList<ArtifactDownloadReport>();
                artifactReports.put(mrid, moduleReports);
            }
            moduleReports.add(report);
        }
    }

    public Iterator<ManifestAndLocation> iterator() {
        return new ArtifactReportManifestIterator();
    }

    class ArtifactReportManifestIterator implements Iterator<ManifestAndLocation> {

        private ManifestAndLocation next = null;

        private Iterator<ModuleRevisionId> it;

        public ArtifactReportManifestIterator() {
            it = artifactReports.keySet().iterator();
        }

        public boolean hasNext() {
            while (next == null && it.hasNext()) {
                ModuleRevisionId mrid = it.next();
                List<ArtifactDownloadReport> reports = artifactReports.get(mrid);
                ArtifactDownloadReport jar = null;
                ArtifactDownloadReport source = null;
                for (ArtifactDownloadReport report : reports) {
                    if (sourceTypes != null && sourceTypes.contains(report.getArtifact().getType())) {
                        source = report;
                    } else {
                        jar = report;
                    }
                }
                if (jar == null) {
                    // didn't found any suitable jar
                    continue;
                }
                URI sourceURI = null;
                if (source != null) {
                    if (source.getUnpackedLocalFile() != null) {
                        sourceURI = source.getUnpackedLocalFile().toURI();
                    } else {
                        sourceURI = source.getLocalFile().toURI();
                    }
                }
                if (jar.getUnpackedLocalFile() != null && jar.getUnpackedLocalFile().isDirectory()) {
                    FileInputStream in = null;
                    try {
                        in = new FileInputStream(new File(jar.getUnpackedLocalFile(),
                                "META-INF/MANIFEST.MF"));
                        next = new ManifestAndLocation(new Manifest(in), jar.getUnpackedLocalFile()
                                .toURI(), sourceURI);
                        return true;
                    } catch (FileNotFoundException e) {
                        Message.debug(
                            "Bundle directory file just removed: " + jar.getUnpackedLocalFile(), e);
                    } catch (IOException e) {
                        Message.debug("The Manifest in the bundle directory could not be read: "
                                + jar.getUnpackedLocalFile(), e);
                    } finally {
                        if (in != null) {
                            try {
                                in.close();
                            } catch (IOException e) {
                                // ignore
                            }
                        }
                    }
                } else {
                    File artifact;
                    if (jar.getUnpackedLocalFile() != null) {
                        artifact = jar.getUnpackedLocalFile();
                    } else {
                        artifact = jar.getLocalFile();
                    }
                    JarInputStream in = null;
                    try {
                        in = new JarInputStream(new FileInputStream(artifact));
                        Manifest manifest = in.getManifest();
                        if (manifest != null) {
                            next = new ManifestAndLocation(manifest, artifact.toURI(), sourceURI);
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