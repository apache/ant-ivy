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
package org.apache.ivy.core.pack;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.ivy.util.FileUtil;
import org.apache.ivy.util.Message;

public class ZipPacking extends ArchivePacking {

    private static final String[] NAMES = {"zip", "jar", "war"};

    @Override
    public String[] getNames() {
        return NAMES;
    }

    @Override
    public String getUnpackedExtension(String ext) {
        if (ext.endsWith("zip") || ext.endsWith("jar") || ext.endsWith("war")) {
            ext = ext.substring(0, ext.length() - 3);
            if (ext.endsWith(".")) {
                ext = ext.substring(0, ext.length() - 1);
            }
        }
        return ext;
    }

    @Override
    public void unpack(InputStream packed, File dest) throws IOException {
        ZipInputStream zip = null;
        try {
            zip = new ZipInputStream(packed);
            ZipEntry entry = null;
            while (((entry = zip.getNextEntry()) != null)) {
                File f = new File(dest, entry.getName());
                Message.verbose("\t\texpanding " + entry.getName() + " to " + f);

                // create intermediary directories - sometimes zip don't add them
                File dirF = f.getParentFile();
                if (dirF != null) {
                    dirF.mkdirs();
                }

                if (entry.isDirectory()) {
                    f.mkdirs();
                } else {
                    writeFile(zip, f);
                }

                f.setLastModified(entry.getTime());
            }
        } finally {
            if (zip != null) {
                try {
                    zip.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    protected void writeFile(InputStream zip, File f) throws FileNotFoundException, IOException {
        FileOutputStream out = new FileOutputStream(f);
        try {
            FileUtil.copy(zip, out, null, false);
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

}
