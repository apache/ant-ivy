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
package org.apache.ivy.plugins.repository;

import java.io.File;
import java.io.IOException;

import org.apache.ivy.core.module.descriptor.Artifact;

/**
 * A {@link ResourceDownloader} is able to download a Resource to a File.
 * <p>
 * Depending on the implementation, the downloader may also choose to download checksums
 * automatically and check the consistency of the downloaded resource.
 * </p>
 * <p>
 * The implementation is also responsible for using a .part file during download, to ensure the
 * destination file will exist only if the download is completed successfully.
 * </p>
 */
public interface ResourceDownloader {
    public void download(Artifact artifact, Resource resource, File dest) throws IOException;
}
