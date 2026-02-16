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
package org.apache.ivy.plugins.pack;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.IvySettingsAware;
import org.apache.ivy.util.FileUtil;

/**
 * Packaging which handle OSGi bundles with inner packed jar
 */
public class OsgiBundlePacking extends ZipPacking implements IvySettingsAware {

    private static final String[] NAMES = {"bundle"};

    private IvySettings settings;

    @Override
    public String[] getNames() {
        return NAMES;
    }

    @Override
    protected void writeFile(InputStream zip, File f) throws IOException {
        // XXX maybe we should only unpack file listed by the 'Bundle-ClassPath' MANIFEST header ?
        if (f.getName().endsWith(".jar.pack.gz")) {
            // unpack the pack200 file
            ArchivePacking pack200 = settings.getPackingRegistry().get("pack200");
            if (pack200 == null) {
                throw new IOException("Packing type 'pack200' not found!");
            }

            if (!(pack200 instanceof StreamPacking)) {
                throw new IOException("Packing type 'pack200' is not a stream packing!");
            }

            f = new File(f.getParentFile(), f.getName().substring(0, f.getName().length() - 8));
            zip = ((StreamPacking) pack200).unpack(zip);
        }
        super.writeFile(zip, f);
    }

    @Override
    public void setSettings(IvySettings settings) {
        this.settings = settings;
    }
}
