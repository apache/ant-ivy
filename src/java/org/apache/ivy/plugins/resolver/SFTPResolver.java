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
package org.apache.ivy.plugins.resolver;

import org.apache.ivy.plugins.repository.sftp.SFTPRepository;

/**
 * This resolver is able to work with any sftp server. It supports listing and publishing. The
 * server host should absolutely be set using setHost. basedir defaults to . port default to 22
 * username and password will be prompted using a dialog box if not set. So if you are in an
 * headless environment, provide username and password.
 */
public class SFTPResolver extends AbstractSshBasedResolver {

    public SFTPResolver() {
        setRepository(new SFTPRepository());
    }

    @Override
    public String getTypeName() {
        return "sftp";
    }

    public SFTPRepository getSFTPRepository() {
        return (SFTPRepository) getRepository();
    }
}
