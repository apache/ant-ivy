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
import java.io.IOException;
import java.io.InputStream;

import org.apache.ivy.util.FileUtil;

public abstract class StreamPacking extends ArchivePacking {

    public abstract InputStream unpack(InputStream packed) throws IOException;

    @Override
    public void unpack(InputStream packed, File dest) throws IOException {
        FileUtil.copy(unpack(packed), dest, null);
    }

}
