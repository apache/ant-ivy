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
import org.apache.ivy.util.Checks;
import org.apache.ivy.util.FileUtil;

public class FileRepository extends AbstractRepository {
    private RepositoryCopyProgressListener progress = new RepositoryCopyProgressListener(this);

    private File baseDir;

    private boolean local = true;

    public FileRepository() {
        baseDir = null;
    }

    public FileRepository(File basedir) {
        setBaseDir(basedir);
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

    public void move(File src, File dest) throws IOException {
        if (!src.renameTo(dest)) {
            throw new IOException("impossible to move '" + src + "' to '" + dest + "'");
        }
    }

    public void delete(File f) throws IOException {
        if (!FileUtil.forceDelete(f)) {
            throw new IOException("impossible to delete '" + f + "'");
        }
    }

    private void copy(File src, File destination, boolean overwrite) throws IOException {
        try {
            getProgressListener().setTotalLength(new Long(src.length()));
            if (!FileUtil.copy(src, destination, getProgressListener(), overwrite)) {
                if (!overwrite && destination.exists()) {
                    throw new IOException("file copy not done from " + src + " to " + destination
                            + ": destination already exists and overwrite is false");
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
            getProgressListener().setTotalLength(null);
        }
    }

    protected RepositoryCopyProgressListener getProgressListener() {
        return progress;
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

    File getFile(String source) {
        if (baseDir != null) {
            return FileUtil.resolveFile(baseDir, source);
        } else {
            return Checks.checkAbsolute(source, "source");
        }
    }

    public boolean isLocal() {
        return local;
    }

    public void setLocal(boolean local) {
        this.local = local;
    }

    public File getBaseDir() {
        return baseDir;
    }

    public final void setBaseDir(File baseDir) {
        Checks.checkAbsolute(baseDir, "basedir");
        this.baseDir = baseDir;
    }

    public String standardize(String source) {
        if (baseDir != null) {
            return FileUtil.resolveFile(baseDir, source).getPath();
        } else {
            return FileUtil.normalize(source).getPath();
        }
    }

    public String getFileSeparator() {
        return File.separator;
    }
}
