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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.BaseResourceCollectionWrapper;
import org.apache.tools.ant.types.resources.FileResource;

public class IvyResources extends IvyCacheTask implements ResourceCollection {

    /**
     * Delegate for the implementation of the resource collection
     */
    private class IvyBaseResourceCollectionWrapper extends BaseResourceCollectionWrapper {

        protected Collection<Resource> getCollection() {
            return resolveResources(null);
        }

    }

    private IvyBaseResourceCollectionWrapper wrapper = new IvyBaseResourceCollectionWrapper();

    // delegate the ProjectComponent API on the wrapper

    public void setLocation(Location location) {
        super.setLocation(location);
        wrapper.setLocation(location);
    }

    public void setProject(Project project) {
        super.setProject(project);
        wrapper.setProject(project);
    }

    public void setDescription(String desc) {
        super.setDescription(desc);
        wrapper.setDescription(desc);
    }

    // delegate the DataType API on the wrapper

    public void setRefid(Reference ref) {
        wrapper.setRefid(ref);
    }

    // delegate the AbstractResourceCollectionWrapper API on the wrapper

    public void setCache(boolean b) {
        wrapper.setCache(b);
    }

    // implementation of the Resource Collection API

    public boolean isFilesystemOnly() {
        return true;
    }

    public Iterator<Resource> iterator() {
        return wrapper.iterator();
    }

    public int size() {
        return wrapper.size();
    }

    // convert the ivy reports into an Ant Resource collection

    private Collection<Resource> resolveResources(String id) throws BuildException {
        prepareAndCheck();
        try {
            List<Resource> resources = new ArrayList<>();
            if (id != null) {
                getProject().addReference(id, this);
            }
            for (ArtifactDownloadReport adr : getArtifactReports()) {
                resources.add(new FileResource(adr.getLocalFile()));
            }
            return resources;
        } catch (Exception ex) {
            throw new BuildException("impossible to build ivy resources: " + ex, ex);
        }
    }

    // implementation of the IvyPostResolveTask API

    public void doExecute() throws BuildException {
        // TODO : maybe there is a way to implement it ?
        throw new BuildException("ivy:resources should not be used as a Ant Task");
    }

}
