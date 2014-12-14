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
package org.apache.ivy.core.retrieve;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.ivy.core.report.ArtifactDownloadReport;

public class RetrieveReport {

    private Collection<File> upToDateFiles = new HashSet<File>();

    private Collection<File> copiedFiles = new HashSet<File>();

    private Map<File, ArtifactDownloadReport> downloadReport = new HashMap<File, ArtifactDownloadReport>();

    private File retrieveRoot;

    /**
     * Returns the root directory to where the artifacts are retrieved.
     */
    public File getRetrieveRoot() {
        return retrieveRoot;
    }

    public void setRetrieveRoot(File retrieveRoot) {
        this.retrieveRoot = retrieveRoot;
    }

    public int getNbrArtifactsCopied() {
        return copiedFiles.size();
    }

    public int getNbrArtifactsUpToDate() {
        return upToDateFiles.size();
    }

    public void addCopiedFile(File file, ArtifactDownloadReport report) {
        copiedFiles.add(file);
        downloadReport.put(file, report);
    }

    public void addUpToDateFile(File file, ArtifactDownloadReport report) {
        upToDateFiles.add(file);
        downloadReport.put(file, report);
    }

    /**
     * Returns a collection of <tt>File</tt> objects who were actually copied during the retrieve
     * process.
     */
    public Collection<File> getCopiedFiles() {
        return new ArrayList<File>(copiedFiles);
    }

    /**
     * Returns a collection of <tt>File</tt> objects who were actually copied during the retrieve
     * process.
     */
    public Collection<File> getUpToDateFiles() {
        return new ArrayList<File>(upToDateFiles);
    }

    /**
     * Returns a collection of <tt>File</tt> objects who were retrieved during the retrieve process.
     * This is the union of the files being copied and the files that were up-to-date.
     */
    public Collection<File> getRetrievedFiles() {
        Collection<File> result = new ArrayList<File>(upToDateFiles.size() + copiedFiles.size());
        result.addAll(upToDateFiles);
        result.addAll(copiedFiles);
        return result;
    }

    /**
     * Get the mapping between the copied files and their corresponding download report
     */
    public Map<File, ArtifactDownloadReport> getDownloadReport() {
        return downloadReport;
    }
}
