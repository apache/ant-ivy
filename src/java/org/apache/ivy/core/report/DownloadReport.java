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
package org.apache.ivy.core.report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ivy.core.module.descriptor.Artifact;

/**
 *
 */
public class DownloadReport {
    private final Map<Artifact, ArtifactDownloadReport> artifacts = new HashMap<>();

    public void addArtifactReport(ArtifactDownloadReport adr) {
        artifacts.put(adr.getArtifact(), adr);
    }

    public ArtifactDownloadReport[] getArtifactsReports() {
        return artifacts.values().toArray(new ArtifactDownloadReport[artifacts.size()]);
    }

    public ArtifactDownloadReport[] getArtifactsReports(DownloadStatus status) {
        List<ArtifactDownloadReport> ret = new ArrayList<>(artifacts.size());
        for (ArtifactDownloadReport adr : artifacts.values()) {
            if (adr.getDownloadStatus() == status) {
                ret.add(adr);
            }
        }
        return ret.toArray(new ArtifactDownloadReport[ret.size()]);
    }

    public ArtifactDownloadReport getArtifactReport(Artifact artifact) {
        return artifacts.get(artifact);
    }
}
