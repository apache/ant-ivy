/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.repository.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import fr.jayasoft.ivy.repository.Resource;

public class FileResource implements Resource {
    private File _file;
    private FileRepository _repository;

    public FileResource(FileRepository repository, File f) {
    	_repository = repository;
        _file = f;
    }

    public String getName() {
        return _file.getPath();
    }
    
    public Resource clone(String cloneName) {
    	return new FileResource(_repository, new File(cloneName));
    }

    public long getLastModified() {
        return _file.lastModified();
    }

    public long getContentLength() {
        return _file.length();
    }

    public boolean exists() {
        return _file.exists();
    }

    public String toString() {
        return getName();
    }

    public File getFile() {
        return _file;
    }
    
    public FileRepository getRepository() {
    	return _repository;
    }
    
    public boolean isLocal() {
        return _repository.isLocal();
    }

	public InputStream openStream() throws IOException {
		return new FileInputStream(_file);
	}
}
