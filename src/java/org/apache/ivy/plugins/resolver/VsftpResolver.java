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

import org.apache.ivy.plugins.repository.vsftp.VsftpRepository;

/**
 * This resolver uses SecureCRT vsft to access an sftp server. It supports listing and publishing.
 * The server host should absolutely be set using setHost, so does the username.
 */
public class VsftpResolver extends RepositoryResolver {
    public VsftpResolver() {
        setRepository(new VsftpRepository());
    }

    @Override
    public String getTypeName() {
        return "vsftp";
    }

    public VsftpRepository getVsftpRepository() {
        return (VsftpRepository) getRepository();
    }

    public void disconnect() {
        getVsftpRepository().disconnect();
    }

    public String getAuthentication() {
        return getVsftpRepository().getAuthentication();
    }

    public String getHost() {
        return getVsftpRepository().getHost();
    }

    public String getUsername() {
        return getVsftpRepository().getUsername();
    }

    public void setAuthentication(String authentication) {
        getVsftpRepository().setAuthentication(authentication);
    }

    public void setHost(String host) {
        getVsftpRepository().setHost(host);
    }

    public void setUsername(String username) {
        getVsftpRepository().setUsername(username);
    }

    public void setReuseConnection(long time) {
        getVsftpRepository().setReuseConnection(time);
    }

    public void setReadTimeout(long readTimeout) {
        getVsftpRepository().setReadTimeout(readTimeout);
    }
}
