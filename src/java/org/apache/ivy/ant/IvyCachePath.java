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
package org.apache.ivy.ant;

import java.io.File;
import java.util.List;

import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.osgi.core.BundleInfo;
import org.apache.ivy.osgi.core.ManifestParser;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;

/**
 * Creates an ant path consisting in all artifacts found during a resolve.
 */
public class IvyCachePath extends IvyCacheTask {

    private String pathid;

    private String id;

    private boolean osgi = false;

    public String getPathid() {
        return pathid;
    }

    public void setPathid(String id) {
        pathid = id;
    }

    public void setOsgi(boolean osgi) {
        this.osgi = osgi;
    }

    /**
     * @deprecated use setPathid instead
     * @param id String
     */
    @Deprecated
    public void setId(String id) {
        this.id = id;
    }

    public void doExecute() throws BuildException {
        prepareAndCheck();
        if (pathid == null) {
            if (id == null) {
                throw new BuildException("pathid is required in ivy classpath");
            }
            pathid = id;
            log("ID IS DEPRECATED, PLEASE USE PATHID INSTEAD", Project.MSG_WARN);
        }
        try {
            Path path = new Path(getProject());
            getProject().addReference(pathid, path);
            for (ArtifactDownloadReport adr : getArtifactReports()) {
                File f = adr.getLocalFile();
                if (adr.getUnpackedLocalFile() != null) {
                    f = adr.getUnpackedLocalFile();
                }
                addToPath(path, f);
            }
        } catch (Exception ex) {
            throw new BuildException("impossible to build ivy path: " + ex, ex);
        }

    }

    protected void addToPath(Path path, File f) throws Exception {
        if (!osgi || !f.isDirectory()) {
            path.createPathElement().setLocation(f);
            return;
        }
        File manifest = new File(f, "META-INF/MANIFEST.MF");
        if (!manifest.exists()) {
            path.createPathElement().setLocation(f);
            return;
        }
        BundleInfo bundleInfo = ManifestParser.parseManifest(manifest);
        List<String> cp = bundleInfo.getClasspath();
        if (cp == null) {
            path.createPathElement().setLocation(f);
            return;
        }
        for (String p : cp) {
            if (p.equals(".")) {
                path.createPathElement().setLocation(f);
            } else {
                path.createPathElement().setLocation(new File(f, p));
            }
        }
    }

}
