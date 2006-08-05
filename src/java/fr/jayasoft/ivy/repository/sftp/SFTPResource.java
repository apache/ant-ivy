/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.repository.sftp;

import java.io.IOException;
import java.io.InputStream;

import fr.jayasoft.ivy.repository.Resource;

public class SFTPResource implements Resource {
	private SFTPRepository _repository;
    private String _path;
    
    private transient boolean _init = false;
    private transient boolean _exists;
    private transient long _lastModified;
    private transient long _contentLength;

    public SFTPResource(SFTPRepository repository, String path) {
		_repository = repository;
		_path = path;
	}

	public String getName() {
        return _path;
    }
    
    public Resource clone(String cloneName) {
    	return new SFTPResource(_repository, cloneName);
    }

    public long getLastModified() {
    	init();
        return _lastModified;
    }

    public long getContentLength() {
    	init();
        return _contentLength;
    }

    public boolean exists() {
    	init();
        return _exists;
    }

    private void init() {
		if (!_init) {
			Resource r = _repository.resolveResource(_path);
			_contentLength = r.getContentLength();
			_lastModified = r.getLastModified();
			_exists = r.exists();
			_init = true;
		}
	}

	public String toString() {
        return getName();
    }

    public boolean isLocal() {
        return false;
    }

	public InputStream openStream() throws IOException {
		return _repository.openStream(this);
	}
}
