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
package org.apache.ivy.plugins.repository.vfs;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.vfs.FileName;
import org.apache.commons.vfs.FileSystem;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemOptions;
import org.apache.commons.vfs.provider.GenericFileName;
import org.apache.commons.vfs.provider.webdav.WebdavFileProvider;

/**
 * Modified version of the WebdavFileProvider from VFS which adds support for httpclient 3.x.
 * See http://issues.apache.org/jira/browse/VFS-74 for more info.
 * 
 * A provider for WebDAV.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @author Maarten Coene
 * @version $Revision: 417178 $ $Date: 2006-06-26 05:31:41 -0700 (Mon, 26 Jun 2006) $
 */
public class IvyWebdavFileProvider extends WebdavFileProvider {

	/***********************************************************************************************
	 * Creates a filesystem.
	 */
	protected FileSystem doCreateFileSystem(final FileName name,
			final FileSystemOptions fileSystemOptions) throws FileSystemException {
		// Create the file system
		final GenericFileName rootName = (GenericFileName) name;

		HttpClient httpClient = IvyWebdavClientFactory.createConnection(rootName.getHostName(),
				rootName.getPort(), rootName.getUserName(), rootName.getPassword(),
				fileSystemOptions);

		return new IvyWebdavFileSystem(rootName, httpClient, fileSystemOptions);
	}

}
