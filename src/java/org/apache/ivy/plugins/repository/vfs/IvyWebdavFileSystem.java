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
import org.apache.commons.vfs.FileSystemOptions;
import org.apache.commons.vfs.provider.GenericFileName;
import org.apache.commons.vfs.provider.webdav.WebDavFileSystem;


/**
 * This class extends from WebDavFileSystem because it doesn't provide an accessible constructor.
 * 
 * @author Maarten Coene
 */
class IvyWebdavFileSystem extends WebDavFileSystem {

	protected IvyWebdavFileSystem(GenericFileName arg0, HttpClient arg1, FileSystemOptions arg2) {
		super(arg0, arg1, arg2);
	}

}
