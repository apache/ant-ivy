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
package org.apache.ivy.core.module.descriptor;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.util.extendable.UnmodifiableExtendableItem;


public class DefaultDependencyArtifactDescriptor extends UnmodifiableExtendableItem
	implements DependencyArtifactDescriptor, ConfigurationAware {

    private Collection _confs = new ArrayList();
	private URL _url;
	private String _name;
	private String _type;
	private String _ext;
 
	/**
     * @param dd
     * @param name
     * @param type
     * @param url 
     */
    public DefaultDependencyArtifactDescriptor(
    		String name, String type, String ext, URL url, Map extraAttributes) {
    	super(null, extraAttributes);
        if (name == null) {
            throw new NullPointerException("name must not be null");
        }
        if (type == null) {
            throw new NullPointerException("type must not be null");
        }
        if (ext == null) {
            throw new NullPointerException("ext must not be null");
        }
        _name = name;
        _type = type;
        _ext = ext;
        _url = url;
        initStandardAttributes();
    }

	private void initStandardAttributes() {
        setStandardAttribute(IvyPatternHelper.ARTIFACT_KEY, getName());
        setStandardAttribute(IvyPatternHelper.TYPE_KEY, getType());
        setStandardAttribute(IvyPatternHelper.EXT_KEY, getExt());
        setStandardAttribute("url", _url != null ? String.valueOf(_url) : "");
	}
    
	public boolean equals(Object obj) {
        if (!(obj instanceof DependencyArtifactDescriptor)) {
            return false;
        }
        DependencyArtifactDescriptor dad = (DependencyArtifactDescriptor)obj;
        return getAttributes().equals(dad.getAttributes());
    }
    
    public int hashCode() {
        return getAttributes().hashCode();
    }
    
    /**
     * Add a configuration for this artifact.
     * @param conf
     */
    public void addConfiguration(String conf) {
        _confs.add(conf);
    }
        
    public String getName() {
        return _name;
    }

    public String getType() {
        return _type;
    }
    public String getExt() {
        return _ext;
    }

    public String[] getConfigurations() {
        return (String[])_confs.toArray(new String[_confs.size()]);
    }


	public URL getUrl() {
		return _url;
	}

	public String toString() {
		return "DA:"+_name+"."+_ext+"("+_type+") "+"("+_confs+")"+(_url==null?"":_url.toString());
	}
}
