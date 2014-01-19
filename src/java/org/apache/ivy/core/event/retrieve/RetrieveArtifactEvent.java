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
package org.apache.ivy.core.event.retrieve;

import java.io.File;

import org.apache.ivy.core.event.IvyEvent;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.report.ArtifactDownloadReport;

public class RetrieveArtifactEvent extends IvyEvent {
    private ArtifactDownloadReport report;

    private File destFile;

    public RetrieveArtifactEvent(String name, ArtifactDownloadReport report, File destFile) {
        super(name);
        addArtifactAttributes(report.getArtifact());

        this.report = report;
        this.destFile = destFile;
        addAttribute("from", report.getLocalFile().getAbsolutePath());
        addAttribute("to", destFile.getAbsolutePath());
        addAttribute("size", String.valueOf(destFile.length()));
    }

    protected void addArtifactAttributes(Artifact artifact) {
        addMridAttributes(artifact.getModuleRevisionId());
        addAttributes(artifact.getAttributes());
        addAttribute("metadata", String.valueOf(artifact.isMetadata()));
    }

    public File getDestFile() {
        return destFile;
    }

    public ArtifactDownloadReport getReport() {
        return report;
    }
}
