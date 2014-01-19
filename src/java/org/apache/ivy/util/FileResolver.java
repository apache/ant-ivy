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
package org.apache.ivy.util;

import java.io.File;

public interface FileResolver {
    public static final FileResolver DEFAULT = new FileResolver() {
        public File resolveFile(String path, String filename) {
            return new File(path);
        }
    };

    /**
     * Return the canonical form of a path, or raise an {@link IllegalArgumentException} if the path
     * is not valid for this {@link FileResolver}.
     * <p>
     * 
     * @param path
     *            The path of the file to resolve. Must not be <code>null</code>.
     * @param fileName
     *            The name of the file to resolve. This is used only for exception message if any.
     *            Must not be <code>null</code>.
     * 
     * @return the resolved File.
     * 
     */
    File resolveFile(String path, String filename);
}
