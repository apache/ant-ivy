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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.compressors.pack200.Pack200CompressorInputStream;

public class Pack200Packing extends StreamPacking {

    private static final String[] NAMES = {"pack200"};

    public Pack200Packing() {
    }

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

        return new Pack200CompressorInputStream(in);
    }

}
