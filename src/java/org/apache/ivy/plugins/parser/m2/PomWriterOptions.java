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
package org.apache.ivy.plugins.parser.m2;

import org.apache.ivy.plugins.parser.m2.PomModuleDescriptorWriter.ConfigurationScopeMapping;

public class PomWriterOptions {
    private String[] confs;
    
    private String licenseHeader;
    
    private ConfigurationScopeMapping mapping;
    
    private boolean printIvyInfo = true;

    public String[] getConfs() {
        return confs;
    }

    public PomWriterOptions setConfs(String[] confs) {
        this.confs = confs;
        return this;
    }

    public String getLicenseHeader() {
        return licenseHeader;
    }

    public PomWriterOptions setLicenseHeader(String licenseHeader) {
        this.licenseHeader = licenseHeader;
        return this;
    }

    public ConfigurationScopeMapping getMapping() {
        if (mapping == null) {
            return PomModuleDescriptorWriter.DEFAULT_MAPPING;
        }
        return mapping;
    }

    public PomWriterOptions setMapping(ConfigurationScopeMapping mapping) {
        this.mapping = mapping;
        return this;
    }

    public boolean isPrintIvyInfo() {
        return printIvyInfo;
    }

    public PomWriterOptions setPrintIvyInfo(boolean printIvyInfo) {
        this.printIvyInfo = printIvyInfo;
        return this;
    }
    
    
    
}
