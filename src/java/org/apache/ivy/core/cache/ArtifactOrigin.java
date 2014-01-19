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
package org.apache.ivy.core.cache;

import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.util.Checks;

/**
 * This class contains information about the origin of an artifact.
 * 
 * @see org.apache.ivy.plugins.resolver.BasicResolver
 * @see org.apache.ivy.plugins.resolver.util.ResolvedResource
 */
public class ArtifactOrigin {
    private static final String UNKNOWN = "UNKNOWN";

    /**
     * ArtifactOrigin instance used when the origin is unknown.
     */
    public static final ArtifactOrigin unkwnown(Artifact artifact) {
        return new ArtifactOrigin(artifact, false, UNKNOWN);
    }

    public static final boolean isUnknown(ArtifactOrigin artifact) {
        return artifact == null || UNKNOWN.equals(artifact.getLocation());
    }

    public static final boolean isUnknown(String location) {
        return location == null || UNKNOWN.equals(location);
    }

    private static final int MAGIC_HASH_VALUE = 31;

    private boolean isLocal;

    private String location;

    private Artifact artifact;

    private Long lastChecked;

    private boolean exists = true;

    /**
     * Create a new instance
     * 
     * @param artifact
     *            the artifact pointed by this location. Must not be <code>null</code>.
     * @param isLocal
     *            <code>boolean</code> value indicating if the resource is local (on the
     *            filesystem).
     * @param location
     *            the location of the resource (normally a url). Must not be <code>null</code>.
     */
    public ArtifactOrigin(Artifact artifact, boolean isLocal, String location) {
        Checks.checkNotNull(artifact, "artifact");
        Checks.checkNotNull(location, "location");
        this.artifact = artifact;
        this.isLocal = isLocal;
        this.location = location;
    }

    /**
     * Is this resource local to this host, i.e. is it on the file system?
     * 
     * @return <code>boolean</code> value indicating if the resource is local.
     */
    public boolean isLocal() {
        return isLocal;
    }

    /**
     * Return the location of the resource (normally a url)
     * 
     * @return the location of the resource
     */
    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Return the artifact that this location is pointing at.
     * 
     * @return the artifact that this location is pointing at.
     */
    public Artifact getArtifact() {
        return artifact;
    }

    /**
     * The last time the resource was checked to be up to date. Maybe <code>null</code> if this
     * information is not actually used by in some case.
     * 
     * @return
     */
    public Long getLastChecked() {
        return lastChecked;
    }

    public void setLastChecked(Long lastChecked) {
        this.lastChecked = lastChecked;
    }

    public boolean isExists() {
        return exists;
    }

    public void setExist(boolean exists) {
        this.exists = exists;
    }

    public String toString() {
        return "ArtifactOrigin { isLocal=" + isLocal + ", location=" + location + ", lastChecked="
                + lastChecked + ", exists=" + exists + "}";
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ArtifactOrigin that = (ArtifactOrigin) o;

        if (isLocal != that.isLocal) {
            return false;
        }
        if (!location.equals(that.location)) {
            return false;
        }
        if (lastChecked == null) {
            if (that.lastChecked != null) {
                return false;
            }
        } else if (!lastChecked.equals(that.lastChecked)) {
            return false;
        }
        if (exists != that.exists) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result;
        result = (isLocal ? 1 : 0);
        result = MAGIC_HASH_VALUE * result + location.hashCode();
        result = MAGIC_HASH_VALUE * result + ((lastChecked == null) ? 0 : lastChecked.hashCode());
        result = MAGIC_HASH_VALUE * result + (exists ? 1 : 0);
        return result;
    }
}
