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
 *
 */
public class ModuleId implements Comparable {
    static final String ENCODE_SEPARATOR = ":#@#:";

    private String organisation;

    private String name;

    private int hash;

    /**
     * Constructor.
     * @param  organisation  The organisation which creates the module.
     * @param  name  The name of the module.
     */
    public ModuleId(String organisation, String name) {
        if (name == null) {
            throw new IllegalArgumentException("null name not allowed");
        }
        this.organisation = organisation;
        this.name = name;
    }

    /**
     * Returns the name of the module.
     * @return  The name of the module.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the name of the organisation.
     * @return  The name of the organisation.
     */
    public String getOrganisation() {
        return organisation;
    }

    /** {@inheritDoc} */    
    public boolean equals(Object obj) {
        if (!(obj instanceof ModuleId)) {
            return false;
        }
        ModuleId other = (ModuleId) obj;
        if (other.organisation == null) {
            return organisation == null && other.name.equals(name);
        } else {
            return other.organisation.equals(organisation) && other.name.equals(name);
        }
    }

    /** {@inheritDoc} */    
    public int hashCode() {
        if (hash == 0) {
            //CheckStyle:MagicNumber| OFF
            hash = 31;
            hash = hash * 13 + (organisation == null ? 0 : organisation.hashCode());
            hash = hash * 13 + name.hashCode();
            //CheckStyle:MagicNumber| ON
        }
        return hash;
    }

    /** {@inheritDoc} */    
    public String toString() {
        return "[ " + organisation + " | " + name + " ]";
    }

    /** {@inheritDoc} */    
    public int compareTo(Object obj) {
        ModuleId that = (ModuleId) obj;
        int result = organisation.compareTo(that.organisation);
        if (result == 0) {
            result = name.compareTo(that.name);
        }
        return result;
    }

    /**
     * Returns the encoded String representing this ModuleId.
     * @return  The ModuleId encoded as String.
     */
    public String encodeToString() {
        return getOrganisation() + ENCODE_SEPARATOR + getName();
    }

    /**
     * Returns a ModuleId  
     * @param  encoded  
     * @return  The new ModuleId.
     * @throws  IllegalArgumentException  If the given String could not be decoded.
     */
    public static ModuleId decode(String encoded) {
        String[] parts = encoded.split(ENCODE_SEPARATOR);
        if (parts.length != 2) {
            throw new IllegalArgumentException("badly encoded module id: '" + encoded + "'");
        }
        return new ModuleId(parts[0], parts[1]);
    }
}
