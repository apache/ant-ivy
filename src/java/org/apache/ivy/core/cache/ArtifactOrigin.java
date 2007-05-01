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

/**
 * This class contains information about the origin of an artifact.
 * @see org.apache.ivy.plugins.resolver.BasicResolver
 * @see org.apache.ivy.plugins.resolver.util.ResolvedResource
 */
public class ArtifactOrigin {    
    private boolean _isLocal;
	private String _location;

    /**
     *  Create a new instance
     * 
     * @param isLocal <code>boolean</code> value indicating if the resource is local (on the filesystem).
     * @param location the location of the resource (normally a url)
     */
    public ArtifactOrigin(boolean isLocal, String location) {
		_isLocal = isLocal;
		_location = location;
	}

    /**
     * Is this resource local to this host, i.e. is it on the file system?
     *
     * @return <code>boolean</code> value indicating if the resource is local.
     */
    public boolean isLocal() {
		return _isLocal;
	}

    /**
     * Return the location of the resource (normally a url)
     *
     * @return the location of the resource
     */
    public String getLocation() {
		return _location;
	}

    public String toString() {
        return "ArtifactOrigin { _isLocal=" + _isLocal + ", _location=" + _location + "}";
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ArtifactOrigin that = (ArtifactOrigin) o;

        if (_isLocal != that._isLocal) return false;
        if (!_location.equals(that._location)) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (_isLocal ? 1 : 0);
        result = 31 * result + _location.hashCode();
        return result;
    }
}
