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
package org.apache.ivy.plugins.repository.file;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.ivy.plugins.repository.AbstractRepository;
import org.apache.ivy.plugins.repository.RepositoryCopyProgressListener;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.plugins.repository.TransferEvent;
import org.apache.ivy.util.FileUtil;

public class FileRepository extends AbstractRepository {
    private RepositoryCopyProgressListener _progress = new RepositoryCopyProgressListener(this);

    private File _baseDir;

    private boolean _local = true;

    public FileRepository() {
        _baseDir = null;
    }

    public FileRepository(File basedir) {
        _baseDir = basedir;
    }

    public Resource getResource(String source) throws IOException {
        return new FileResource(this, getFile(source));
    }

    public void get(String source, File destination) throws IOException {
        fireTransferInitiated(getResource(source), TransferEvent.REQUEST_GET);
        copy(getFile(source), destination, true);
    }

    public void put(File source, String destination, boolean overwrite) throws IOException {
        fireTransferInitiated(getResource(destination), TransferEvent.REQUEST_PUT);
        copy(source, getFile(destination), overwrite);
    }

    private void copy(File src, File destination, boolean overwrite) throws IOException {
        try {
            _progress.setTotalLength(new Long(src.length()));
            if (!FileUtil.copy(src, destination, _progress, overwrite)) {
                if (!overwrite) {
                    throw new IOException("file copy not done from " + src + " to " + destination
                            + ": destination probably already exists and overwrite is false");
                } else {
                    throw new IOException("file copy not done from " + src + " to " + destination);
                }
            }
        } catch (IOException ex) {
            fireTransferError(ex);
            throw ex;
        } catch (RuntimeException ex) {
            fireTransferError(ex);
            throw ex;
        } finally {
            _progress.setTotalLength(null);
        }
    }

    public List list(String parent) throws IOException {
        File dir = getFile(parent);
        if (dir.exists() && dir.isDirectory()) {
            String[] names = dir.list();
            if (names != null) {
                List ret = new ArrayList(names.length);
                for (int i = 0; i < names.length; i++) {
                    ret.add(parent + getFileSeparator() + names[i]);
                }
                return ret;
            }
        }
        return null;
    }

    private File getFile(String source) {
        if (_baseDir != null) {
            return new File(_baseDir, source);
        } else {
            return new File(source);
        }
    }

    public boolean isLocal() {
        return _local;
    }

    public void setLocal(boolean local) {
        _local = local;
    }

}
