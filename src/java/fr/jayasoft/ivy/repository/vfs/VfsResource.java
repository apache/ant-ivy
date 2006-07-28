/**
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 *
 * VFS implementation of the Resource interface
 * 
 * @author glen
 * @author Matt Inger
 * @author Stephen Nesbitt
 *
 */
package fr.jayasoft.ivy.repository.vfs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.vfs.FileContent;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.FileType;

import fr.jayasoft.ivy.repository.Resource;
import fr.jayasoft.ivy.util.Message;


public class VfsResource implements Resource {
	private FileContent _content = null;
    private FileObject _resourceImpl;
    private boolean _isAvailable;
    
    // Constructor
    public VfsResource(String vfsURI, FileSystemManager fsManager) throws IOException {
    	_isAvailable = false;
    	_resourceImpl = null;
    	
 		try {
			_resourceImpl = fsManager.resolveFile(vfsURI);
 		} catch (FileSystemException e) {
 			Message.verbose(e.getLocalizedMessage());
 			throw new IOException(e.getLocalizedMessage());
		}

 		try {
			_isAvailable = _resourceImpl.exists();
 		} catch (FileSystemException e) {
 			Message.verbose(e.getLocalizedMessage());
		}
    }
    
    /**
     * Get a list of direct descendents of the given resource.
     * Note that attempts to get a list of children does <emphasize>not</emphasize>
     * result in an error. Instead an error message is logged and an empty ArrayList returned.
     * 
     * @return A <code>ArrayList</code> of VFSResources
     *
     */
    public List getChildren() {
    	ArrayList list = new ArrayList();
    	try {
			if (_resourceImpl.exists() && _resourceImpl.getType() == FileType.FOLDER) {
				FileObject[] children = _resourceImpl.getChildren();
				for (int i = 0; i < children.length; i++) {
					FileObject child = children[i];
					list.add(normalize(child.getName().getURI()));
				}
			}
		} catch (IOException e) {
			Message.verbose(e.getLocalizedMessage());
		}    	
    	return list;
    }
    
    public FileContent getContent() throws IOException {
    	if ((_content == null) && _isAvailable) {
 			try {
				_content = _resourceImpl.getContent();
			} catch (FileSystemException e) {
				IOException error = new IOException(e.getLocalizedMessage());
				error.initCause(e);
	 			throw error;
			}
    	}
    	return _content;
    }
    
     /**
     * Get the name of the resource.
     * 
     * @return a <code>String</code> representing the Resource URL.
     */
    public String getName() {
    	if (exists()) {
    		return normalize(_resourceImpl.getName().getURI());
    	} else {
    		return "";
    	}
    }
    
    public Resource clone(String cloneName) {
    	try {
    		return new VfsResource(cloneName, _resourceImpl.getFileSystem().getFileSystemManager());
    	} catch (IOException e) {
    		throw new RuntimeException(e);
    	}
    }
    
    /**
     * The VFS FileName getURI method seems to have a bug in it where
     * file: URIs will have 4 forward slashes instead of 3.
     * 
     * @param vfsURI
     * @return a normalized <class>String</class> representing the VFS URI
     */
    private String normalize(String vfsURI) {
		if (vfsURI.startsWith("file:////")) {
			vfsURI = vfsURI.replaceFirst("////", "///");
		}
		return vfsURI;
    }

    /**
     * Get the last modification time of the resource.
     * 
     * @return a <code>long</code> indicating last modified time.
     */
     public long getLastModified() {
    	 long time = 0;
    	 if (exists()) {
    		try {
				time = _resourceImpl.getContent().getLastModifiedTime();
			} catch (FileSystemException e) {
				Message.verbose(e.getLocalizedMessage());
			}
    	 }
    	 return time;
    }

     /**
      * Get the size of the resource
      * 
      * @return a <code>long</code> representing the size of the resource (in bytes).
      */
    public long getContentLength() {
    	long size = 0;
    	if (exists()) {
    		try {
				size = _resourceImpl.getContent().getSize();
			} catch (FileSystemException e) {
				Message.verbose(e.getLocalizedMessage());
			}
    	}
    	return size;   	
    }

    /**
     * Flag indicating whether a resource is available for querying
     * 
     * @return <code>true</code> if the resource is available for querying,
     *         <code>false</code> otherwise.
     */
    public boolean exists() {
    	return _isAvailable;
    }
    
    /**
     * Return a flag indicating whether a provided VFS resource physically exists
     * 
     * @return <code>true</code> if the resource physically exists, <code>false</code>
     *         otherwise.
     */
     public boolean physicallyExists() {
    	
    	try {
			return _resourceImpl.exists();
			// originally I only checked for a FileSystemException. I expanded it to
			// include all exceptions when I found it would throw a NPE exception when the query was 
			// run on non-wellformed VFS URI.
		} catch (Exception e) {
			Message.verbose(e.getLocalizedMessage());
			return false;
		}
    }

    public String toString() {
        return getName();
    }

    public boolean isLocal() {
        return false;
    }
}
