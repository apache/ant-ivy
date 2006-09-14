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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.vfs.FileContent;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.FileType;

import fr.jayasoft.ivy.repository.Resource;
import fr.jayasoft.ivy.util.Message;
import fr.jayasoft.ivy.resolver.VfsResolver;

public class VfsResource implements Resource {	
	private String _vfsURI;
	private FileSystemManager _fsManager;
	
    private transient boolean _init = false;
    private transient boolean _exists;
    private transient long _lastModified;
    private transient long _contentLength;

	private transient FileContent _content = null;
    private transient FileObject _resourceImpl;
    
    // Constructor
    public VfsResource(String vfsURI, FileSystemManager fsManager) {
    	this._vfsURI = vfsURI;
    	this._fsManager = fsManager;
    	this._init = false;
    }
    
    private void init() {
    	if (!_init) {
	    	try {
	    		_resourceImpl = _fsManager.resolveFile(_vfsURI);
	    		_content = _resourceImpl.getContent();
	    		
	    		_exists = _resourceImpl.exists();
	    		_lastModified = _content.getLastModifiedTime();
	    		_contentLength = _content.getSize();
	   		} catch (FileSystemException e) {
	 			Message.verbose(e.getLocalizedMessage());
	 			_exists = false;
	 			_lastModified = 0;
	 			_contentLength = 0;
			}
	   		
	   		_init = true;
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
    	init();
    	ArrayList list = new ArrayList();
    	try {  		
			if ((_resourceImpl != null) && _resourceImpl.exists() && (_resourceImpl.getType() == FileType.FOLDER)) {
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
    
    public FileContent getContent() {
    	init();
    	return _content;
    }
    
     /**
     * Get the name of the resource.
     * 
     * @return a <code>String</code> representing the Resource URL.
     */
    public String getName() {
   		return normalize(_vfsURI);
    }
    
    public Resource clone(String cloneName) {
   		return new VfsResource(cloneName, _fsManager);
    }
    
    /**
     * The VFS FileName getURI method seems to have a bug in it where
     * file: URIs will have 4 forward slashes instead of 3.
     * 
     * @param vfsURI
     * @return a normalized <class>String</class> representing the VFS URI
     */
    public static String normalize(String vfsURI) {
    	if (vfsURI == null) {
    		return "";
    	}
    	
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
    	 init();
    	 return _lastModified;
    }

     /**
      * Get the size of the resource
      * 
      * @return a <code>long</code> representing the size of the resource (in bytes).
      */
    public long getContentLength() {
    	init();
    	return _contentLength;
    }

    /**
     * Flag indicating whether a resource is available for querying
     * 
     * @return <code>true</code> if the resource is available for querying,
     *         <code>false</code> otherwise.
     */
    public boolean exists() {
    	init();
    	return _exists;
    }
    
    /**
     * Return a flag indicating whether a provided VFS resource physically exists
     * 
     * @return <code>true</code> if the resource physically exists, <code>false</code>
     *         otherwise.
     */
     public boolean physicallyExists() {
    	 // TODO: there is no need for this method anymore, replace it by calling exists();
    	 init();
    	
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
        return VfsResolver.prepareForDisplay(getName());
    }

    public boolean isLocal() {
        return getName().startsWith("file:");
    }

	public InputStream openStream() throws IOException {
		return getContent().getInputStream();
	}
}
