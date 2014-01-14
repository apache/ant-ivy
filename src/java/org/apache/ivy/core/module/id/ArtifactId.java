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
package org.apache.ivy.core.module.id;

/**
 * Identifies an artifact in a module, without revision information
 * 
 * @see <a href="package-summary.html">org.apache.ivy.core.module.id</a>
 */
public class ArtifactId {
    private ModuleId mid;

    private String name;

    private String type;

    private String ext;

    /**
     * @param mid
     *            The ModuleId, which is the base of this artifact.
     * @param name
     *            The name of the artifact.
     * @param type
     *            The type of the artifact.
     */
    public ArtifactId(ModuleId mid, String name, String type, String ext) {
        this.mid = mid;
        this.name = name;
        this.type = type;
        this.ext = ext;
    }

    /** {@inheritDoc} */
    public boolean equals(Object obj) {
        if (!(obj instanceof ArtifactId)) {
            return false;
        }
        ArtifactId aid = (ArtifactId) obj;
        return getModuleId().equals(aid.getModuleId()) && getName().equals(aid.getName())
                && getExt().equals(aid.getExt()) && getType().equals(aid.getType());
    }

    /** {@inheritDoc} */
    public int hashCode() {
        // CheckStyle:MagicNumber| OFF
        int hash = 17;
        hash += getModuleId().hashCode() * 37;
        hash += getName().hashCode() * 37;
        hash += getType().hashCode() * 37;
        // CheckStyle:MagicNumber| OFF
        return hash;
    }

    /** {@inheritDoc} */
    public String toString() {
        return getModuleId() + "!" + getShortDescription();
    }

    public String getShortDescription() {
        return getName() + "." + getExt()
                + (getType().equals(getExt()) ? "" : "(" + getType() + ")");
    }

    /**
     * @return Returns the module id.
     */
    public ModuleId getModuleId() {
        return mid;
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    /**
     * @return Returns the type.
     */
    public String getType() {
        return type;
    }

    /**
     * @return Returns the ext.
     */
    public String getExt() {
        return ext;
    }
}
