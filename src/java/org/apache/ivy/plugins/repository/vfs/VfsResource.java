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
package org.apache.ivy.plugins.repository.vfs;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.vfs.FileContent;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.FileType;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.plugins.resolver.VfsResolver;
import org.apache.ivy.util.Message;

/**
 * VFS implementation of the Resource interface
 */
public class VfsResource implements Resource {
    private String vfsURI;

    private FileSystemManager fsManager;

    private transient boolean init = false;

    private transient boolean exists;

    private transient long lastModified;

    private transient long contentLength;

    private transient FileContent content = null;

    private transient FileObject resourceImpl;

    // Constructor
    public VfsResource(String vfsURI, FileSystemManager fsManager) {
        this.vfsURI = vfsURI;
        this.fsManager = fsManager;
        this.init = false;
    }

    private void init() {
        if (!init) {
            try {
                resourceImpl = fsManager.resolveFile(vfsURI);
                content = resourceImpl.getContent();

                exists = resourceImpl.exists();
                lastModified = content.getLastModifiedTime();
                contentLength = content.getSize();
            } catch (FileSystemException e) {
                Message.debug(e);
                Message.verbose(e.getLocalizedMessage());
                exists = false;
                lastModified = 0;
                contentLength = 0;
            }

            init = true;
        }
    }

    /**
     * Get a list of direct descendents of the given resource. Note that attempts to get a list of
     * children does <emphasize>not</emphasize> result in an error. Instead an error message is
     * logged and an empty ArrayList returned.
     * 
     * @return A <code>ArrayList</code> of VFSResources
     */
    public List getChildren() {
        init();
        ArrayList list = new ArrayList();
        try {
            if ((resourceImpl != null) && resourceImpl.exists()
                    && (resourceImpl.getType() == FileType.FOLDER)) {
                FileObject[] children = resourceImpl.getChildren();
                for (int i = 0; i < children.length; i++) {
                    FileObject child = children[i];
                    list.add(normalize(child.getName().getURI()));
                }
            }
        } catch (IOException e) {
            Message.debug(e);
            Message.verbose(e.getLocalizedMessage());
        }
        return list;
    }

    public FileContent getContent() {
        init();
        return content;
    }

    /**
     * Get the name of the resource.
     * 
     * @return a <code>String</code> representing the Resource URL.
     */
    public String getName() {
        return normalize(vfsURI);
    }

    public Resource clone(String cloneName) {
        return new VfsResource(cloneName, fsManager);
    }

    /**
     * The VFS FileName getURI method seems to have a bug in it where file: URIs will have 4 forward
     * slashes instead of 3.
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
        return lastModified;
    }

    /**
     * Get the size of the resource
     * 
     * @return a <code>long</code> representing the size of the resource (in bytes).
     */
    public long getContentLength() {
        init();
        return contentLength;
    }

    /**
     * Flag indicating whether a resource is available for querying
     * 
     * @return <code>true</code> if the resource is available for querying, <code>false</code>
     *         otherwise.
     */
    public boolean exists() {
        init();
        return exists;
    }

    /**
     * Return a flag indicating whether a provided VFS resource physically exists
     * 
     * @return <code>true</code> if the resource physically exists, <code>false</code> otherwise.
     */
    public boolean physicallyExists() {
        // TODO: there is no need for this method anymore, replace it by calling exists();
        init();

        try {
            return resourceImpl.exists();
            // originally I only checked for a FileSystemException. I expanded it to
            // include all exceptions when I found it would throw a NPE exception when the query was
            // run on non-wellformed VFS URI.
        } catch (Exception e) {
            Message.verbose("Fail to check the existance of the resource " + getName(), e);
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
