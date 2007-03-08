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
 * @author x.hanin
 *
 */
public class ModuleId implements Comparable {
    static final String ENCODE_SEPARATOR = ":#@#:";
    private String _organisation;
    private String _name;
    private int _hash;

    public ModuleId(String organisation, String name) {
        if (name == null) {
            throw new IllegalArgumentException("null name not allowed");
        }
        _organisation = organisation;
        _name = name;
        _hash = _hashCode(); //stored for performance reasons, hashCode is very used in many maps
    }

    public String getName() {
        return _name;
    }
    public String getOrganisation() {
        return _organisation;
    }
    
    public boolean equals(Object obj) {
        if (! (obj instanceof ModuleId)) {
            return false;
        }
        ModuleId other = (ModuleId)obj;
        if (other._organisation == null) {
        	return _organisation == null && other._name.equals(_name);
        } else {
        	return other._organisation.equals(_organisation) && other._name.equals(_name);
        }
    }
    public int hashCode() {
        return _hash;
    }
    public int _hashCode() {
        int hash = 31;
        hash = hash * 13 + (_organisation == null ? 0 : _organisation.hashCode());
        hash = hash * 13 + _name.hashCode();
        return hash;
    }
    public String toString() {
        return "[ "+_organisation+" | "+_name+" ]";
    }

    public int compareTo(Object obj) {
        ModuleId that = (ModuleId)obj;
        int result = _organisation.compareTo(that._organisation);
        if (result == 0) {
            result = _name.compareTo(that._name);
        }
        return result;
    }

    public String encodeToString() {
        return getOrganisation() + ENCODE_SEPARATOR + getName();
    }
    public static ModuleId decode(String encoded) {
        String[] parts = encoded.split(ENCODE_SEPARATOR);
        if (parts.length != 2) {
            throw new IllegalArgumentException("badly encoded module id: '"+encoded+"'");
        }
        return new ModuleId(parts[0], parts[1]);
    }
}
