/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.ivy.plugins.repository.vfs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.impl.StandardFileSystemManager;
import org.apache.ivy.core.settings.TimeoutConstraint;
import org.apache.ivy.plugins.repository.AbstractRepository;
import org.apache.ivy.plugins.repository.RepositoryCopyProgressListener;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.plugins.repository.TransferEvent;
import org.apache.ivy.util.CopyProgressListener;
import org.apache.ivy.util.FileUtil;
import org.apache.ivy.util.Message;

/**
 * Implementation of a VFS repository
 */
public class VfsRepository extends AbstractRepository {
    /**
     * Name of the resource defining the Ivy VFS Repo configuration.
     */
    private static final String IVY_VFS_CONFIG = "ivy_vfs.xml";

    private StandardFileSystemManager manager = null;

    private final CopyProgressListener progress = new RepositoryCopyProgressListener(this);

    /**
     * Create a new Ivy VFS Repository Instance
     */
    public VfsRepository() {
    }

    public VfsRepository(final TimeoutConstraint timeoutConstraint) {
        super(timeoutConstraint);
    }

    private FileSystemManager getVFSManager() throws IOException {
        synchronized (this) {
            if (manager == null) {
                manager = createVFSManager();
            }
        }
        return manager;
    }

    private StandardFileSystemManager createVFSManager() throws IOException {
        StandardFileSystemManager result = null;
        try {
            /*
             * The DefaultFileSystemManager gets its configuration from the jakarta-vfs-common
             * implementation which includes the res and tmp schemes which are of no use to use
             * here. Using StandardFileSystemManager lets us specify which schemes to support as
             * well as providing a mechanism to change this support without recompilation.
             */
            result = new StandardFileSystemManager() {
                protected void configurePlugins() throws FileSystemException {
                    // disable automatic loading potential unsupported extensions
                }
            };
            result.setConfiguration(getClass().getResource(IVY_VFS_CONFIG));
            result.init();

            // Generate and print a list of available schemes
            Message.verbose("Available VFS schemes...");
            String[] schemes = result.getSchemes();
            Arrays.sort(schemes);
            for (String scheme : schemes) {
                Message.verbose("VFS Supported Scheme: " + scheme);
            }
        } catch (FileSystemException e) {
            /*
             * If our attempt to initialize a VFS Repository fails we log the failure but continue
             * on. Given that an Ivy instance may involve numerous different repository types, it
             * seems overly cautious to throw a runtime exception on the initialization failure of
             * just one repository type.
             */
            Message.error("Unable to initialize VFS repository manager!");
            Message.error(e.getLocalizedMessage());
            throw new IOException(e.getLocalizedMessage(), e);
        }

        return result;
    }

    protected void finalize() {
        if (manager != null) {
            manager.close();
            manager = null;
        }
    }

    /**
     * Get a VfsResource
     *
     * @param vfsURI
     *            a <code>String</code> identifying a VFS Resource
     * @return Resource
     * @throws IOException on failure
     * @see "Supported File Systems in the jakarta-commons-vfs documentation"
     */
    public Resource getResource(String vfsURI) throws IOException {
        return new VfsResource(vfsURI, getVFSManager());
    }

    /**
     * Transfer a VFS Resource from the repository to the local file system.
     *
     * @param srcVfsURI
     *            a <code>String</code> identifying the VFS resource to be fetched
     * @param destination
     *            a <code>File</code> identifying the destination file
     * @throws IOException on failure
     * @see "Supported File Systems in the jakarta-commons-vfs documentation"
     */
    public void get(String srcVfsURI, File destination) throws IOException {
        VfsResource src = new VfsResource(srcVfsURI, getVFSManager());
        fireTransferInitiated(src, TransferEvent.REQUEST_GET);
        try {
            FileContent content = src.getContent();
            if (content == null) {
                throw new IllegalArgumentException("invalid vfs uri " + srcVfsURI
                        + ": no content found");
            }
            FileUtil.copy(content.getInputStream(), destination, progress);
        } catch (IOException | RuntimeException ex) {
            fireTransferError(ex);
            throw ex;
        }
    }

    /**
     * Return a listing of the contents of a parent directory. Listing is a set of strings
     * representing VFS URIs.
     *
     * @param vfsURI
     *            providing identifying a VFS provided resource
     * @return List
     * @throws IOException
     *             on failure.
     * @see "Supported File Systems in the jakarta-commons-vfs documentation"
     */
    public List<String> list(String vfsURI) throws IOException {
        List<String> list = new ArrayList<>();
        Message.debug("list called for URI" + vfsURI);
        FileObject resourceImpl = getVFSManager().resolveFile(vfsURI);
        Message.debug("resourceImpl=" + resourceImpl.toString());
        Message.debug("resourceImpl.exists()" + resourceImpl.exists());
        Message.debug("resourceImpl.getType()" + resourceImpl.getType());
        Message.debug("FileType.FOLDER" + FileType.FOLDER);
        if (resourceImpl.exists() && resourceImpl.getType() == FileType.FOLDER) {
            List<FileObject> children = Arrays.asList(resourceImpl.getChildren());
            for (FileObject child : children) {
                Message.debug("child " + children.indexOf(child) + child.getName().getURI());
                list.add(VfsResource.normalize(child.getName().getURI()));
            }
        }
        return list;
    }

    /**
     * Transfer an Ivy resource to a VFS repository
     *
     * @param source
     *            a <code>File</code> identifying the local file to transfer to the repository
     * @param vfsURI
     *            a <code>String</code> identifying the destination VFS Resource.
     * @param overwrite
     *            whether to overwrite an existing resource.
     * @throws IOException on failure.
     * @see "Supported File Systems in the jakarta-commons-vfs documentation"
     */
    public void put(File source, String vfsURI, boolean overwrite) throws IOException {
        VfsResource dest = new VfsResource(vfsURI, getVFSManager());
        fireTransferInitiated(dest, TransferEvent.REQUEST_PUT);
        if (dest.physicallyExists() && !overwrite) {
            throw new IOException("Cannot copy. Destination file: " + dest.getName()
                    + " exists and overwrite not set.");
        }
        if (dest.getContent() == null) {
            throw new IllegalArgumentException("invalid vfs uri " + vfsURI
                    + " to put data to: resource has no content");
        }

        FileUtil.copy(new FileInputStream(source), dest.getContent().getOutputStream(), progress);
    }

}
