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
package org.apache.ivy.plugins.repository.jar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.apache.ivy.plugins.repository.AbstractRepository;
import org.apache.ivy.plugins.repository.RepositoryCopyProgressListener;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.plugins.repository.TransferEvent;
import org.apache.ivy.util.FileUtil;

public class JarRepository extends AbstractRepository {

    private RepositoryCopyProgressListener progress = new RepositoryCopyProgressListener(this);

    private JarFile jarFile;

    public void setJarFile(JarFile jarFile) {
        this.jarFile = jarFile;
    }

    public Resource getResource(String source) throws IOException {
        return new JarResource(jarFile, source);
    }

    protected RepositoryCopyProgressListener getProgressListener() {
        return progress;
    }

    public void get(String source, File destination) throws IOException {
        fireTransferInitiated(getResource(source), TransferEvent.REQUEST_GET);
        try {
            ZipEntry entry = jarFile.getEntry(source);
            if (entry == null) {
                throw new FileNotFoundException();
            }
            getProgressListener().setTotalLength(new Long(entry.getSize()));
            FileUtil.copy(jarFile.getInputStream(entry), destination, getProgressListener());
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

    public List/* <String> */list(String parent) throws IOException {
        ZipEntry parentEntry = jarFile.getEntry(parent);
        if (parentEntry == null || !parentEntry.isDirectory()) {
            return null;
        }
        List/* <String> */children = new ArrayList();
        Enumeration entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            if (entry.getName().startsWith(parent) && entry.getName().equals(parentEntry.getName())) {
                children.add(entry.getName());
            }
        }
        return children;
    }

}
