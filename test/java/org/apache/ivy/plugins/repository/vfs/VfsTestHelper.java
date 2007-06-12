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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.impl.StandardFileSystemManager;
import org.apache.ivy.Ivy;
import org.apache.ivy.util.FileUtil;

public class VfsTestHelper {
    private Ivy ivy = null;

    public final StandardFileSystemManager fsManager;

    public static final String VFS_CONF = "ivy_vfs.xml";

    // Ivy Variables
    public static final String PROP_VFS_HOST = "vfs.host";

    public static final String PROP_VFS_SAMBA_REPO = "vfs.samba.share";

    public static final String PROP_VFS_USER_ID = "vfs.user";

    public static final String PROP_VFS_USER_PASSWD = "vfs.passwd";

    // Resources
    public static final String CWD = System.getProperty("user.dir");

    public static final String TEST_REPO_DIR = "test/repositories";

    public static final String IVY_CONFIG_FILE = FileUtil.concat(TEST_REPO_DIR, "ivysettings.xml");

    public static final String TEST_IVY_XML = "2/mod5.1/ivy-4.2.xml";

    public static final String SCRATCH_DIR = "_vfsScratchArea";

    public VfsTestHelper() throws Exception {
        // setup and initialize VFS
        fsManager = new StandardFileSystemManager() {
            protected void configurePlugins() throws FileSystemException {
                // disable automatic loading potential unsupported extensions
            }
        };
        fsManager.setConfiguration(getClass().getResource(VFS_CONF).toString());
        fsManager.init();

        // setup and initialize ivy
        ivy = new Ivy();
        ivy.configure(new File(IVY_CONFIG_FILE));
    }

    /**
     * Generate a set of well-formed VFS resource identifiers
     * 
     * @param resource
     *            name of the resource
     * @return <class>List</class> of well-formed VFS reosurce identifiers
     */
    public List createVFSUriSet(String resource) {
        List set = new ArrayList();
        for (int i = 0; i < VfsURI.SUPPORTED_SCHEMES.length; i++) {
            set.add(VfsURI.vfsURIFactory(VfsURI.SUPPORTED_SCHEMES[i], resource, ivy));
        }
        return set;
    }

    public Ivy getIvy() {
        return ivy;
    }

}
