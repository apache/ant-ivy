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
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.ivy.core.module.id.ArtifactRevisionId;
import org.apache.ivy.core.module.id.ModuleRevisionId;

/**
 * @author x.hanin
 *
 */
public class MDArtifact extends AbstractArtifact {

    public static Artifact newIvyArtifact(ModuleDescriptor md) {
        return new MDArtifact(md, "ivy", "ivy", "xml");
    }
    
    private ModuleDescriptor _md;
    private String _name;
    private String _type;
    private String _ext;
    private List  _confs = new ArrayList();
    private ArtifactRevisionId _arid;
    private Map _extraAttributes = null;
	private URL _url;

    public MDArtifact(ModuleDescriptor md, String name, String type, String ext) {
        this(md, name, type, ext, null, null);
    }
    public MDArtifact(ModuleDescriptor md, String name, String type, String ext, URL url, Map extraAttributes) {
        if (md == null) {
            throw new NullPointerException("null module descriptor not allowed");
        }
        if (name == null) {
            throw new NullPointerException("null name not allowed");
        }
        if (type == null) {
            throw new NullPointerException("null type not allowed");
        }
        if (ext == null) {
            throw new NullPointerException("null ext not allowed");
        }
        _md = md;
        _name = name;
        _type = type;
        _ext = ext;
        _url = url;
        _extraAttributes = extraAttributes;
    }
    
    public ModuleRevisionId getModuleRevisionId() {
        return _md.getResolvedModuleRevisionId();
    }
    
    public Date getPublicationDate() {
        return _md.getResolvedPublicationDate();
    }
    public ArtifactRevisionId getId() {
        if (_arid == null) {
            _arid = ArtifactRevisionId.newInstance(_md.getResolvedModuleRevisionId(), _name, _type, _ext, _extraAttributes);
        }
        return _arid;
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

    public void addConfiguration(String conf) {
        _confs.add(conf);
    }
    
	public URL getUrl() {
		return _url;
	}
    
}
