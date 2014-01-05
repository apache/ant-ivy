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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;
import java.util.jar.Pack200.Unpacker;
import java.util.zip.GZIPInputStream;

public class Pack200Packing extends StreamPacking {

    private static final String[] NAMES = {"pack200"};

    @Override
    public String[] getNames() {
        return NAMES;
    }

    @Override
    public String getUnpackedExtension(String ext) {
        if (ext.endsWith("pack.gz")) {
            ext = ext.substring(0, ext.length() - 7);
            if (ext.endsWith(".")) {
                ext = ext.substring(0, ext.length() - 1);
            }
        } else if (ext.endsWith("pack")) {
            ext = ext.substring(0, ext.length() - 4);
            if (ext.endsWith(".")) {
                ext = ext.substring(0, ext.length() - 1);
            }
        }
        return ext;
    }

    @Override
    public InputStream unpack(InputStream packed) throws IOException {
        BufferedInputStream buffered = new BufferedInputStream(packed);
        buffered.mark(4);
        byte[] magic = new byte[4];
        buffered.read(magic, 0, 4);
        buffered.reset();

        InputStream in = buffered;

        if (magic[0] == (byte) 0x1F && magic[1] == (byte) 0x8B && magic[2] == (byte) 0x08) {
            // this is a gziped pack200
            in = new GZIPInputStream(in);
        }

        Unpacker unpacker = Pack200.newUnpacker();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JarOutputStream jar = new JarOutputStream(baos);
        unpacker.unpack(in, jar);
        jar.close();
        return new ByteArrayInputStream(baos.toByteArray());
    }

}
