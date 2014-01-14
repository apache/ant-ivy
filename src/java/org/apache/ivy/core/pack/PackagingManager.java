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
package org.apache.ivy.core.pack;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.IvySettingsAware;

public class PackagingManager implements IvySettingsAware {

    private IvySettings settings;

    public void setSettings(IvySettings settings) {
        this.settings = settings;
    }

    public Artifact getUnpackedArtifact(Artifact artifact) {
        String packaging = artifact.getExtraAttribute("packaging");
        if (packaging == null) {
            // not declared as packed, nothing to do
            return null;
        }

        String ext = artifact.getExt();

        String[] packings = packaging.split(",");
        for (int i = packings.length - 1; i >= 1; i--) {
            ArchivePacking packing = settings.getPackingRegistry().get(packings[i]);
            if (packing == null) {
                throw new IllegalStateException("Unknown packing type '" + packings[i]
                        + "' in the packing chain: " + packaging);
            }
            if (!(packing instanceof StreamPacking)) {
                throw new IllegalStateException("Unsupported archive only packing type '"
                        + packings[i] + "' in the streamed chain: " + packaging);
            }
            ext = ((StreamPacking) packing).getUnpackedExtension(ext);
        }
        ArchivePacking packing = settings.getPackingRegistry().get(packings[0]);
        if (packing == null) {
            throw new IllegalStateException("Unknown packing type '" + packings[0]
                    + "' in the packing chain: " + packaging);
        }
        ext = packing.getUnpackedExtension(ext);

        DefaultArtifact unpacked = new DefaultArtifact(artifact.getModuleRevisionId(),
                artifact.getPublicationDate(), artifact.getName(),
                artifact.getType() + "_unpacked", ext);

        return unpacked;
    }

    public void unpackArtifact(Artifact artifact, File localFile, File archiveFile)
            throws IOException {
        String packaging = artifact.getExtraAttribute("packaging");
        if (packaging == null) {
            // not declared as packed, nothing to do
            return;
        }

        String[] packings = packaging.split(",");
        InputStream in = null;
        try {
            in = new FileInputStream(localFile);
            for (int i = packings.length - 1; i >= 1; i--) {
                ArchivePacking packing = settings.getPackingRegistry().get(packings[i]);
                if (packing == null) {
                    throw new IllegalStateException("Unknown packing type '" + packings[i]
                            + "' in the packing chain: " + packaging);
                }
                if (!(packing instanceof StreamPacking)) {
                    throw new IllegalStateException("Unsupported archive only packing type '"
                            + packings[i] + "' in the streamed chain: " + packaging);
                }
                in = ((StreamPacking) packing).unpack(in);
            }
            ArchivePacking packing = settings.getPackingRegistry().get(packings[0]);
            if (packing == null) {
                throw new IllegalStateException("Unknown packing type '" + packings[0]
                        + "' in the packing chain: " + packaging);
            }
            packing.unpack(in, archiveFile);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

}
