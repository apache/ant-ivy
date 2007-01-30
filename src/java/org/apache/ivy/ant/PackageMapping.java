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
package org.apache.ivy.ant;

import org.apache.ivy.core.module.id.ModuleRevisionId;


/**
 * Describes a mapping between a package name and an org name revision uple
 * 
 * @author Xavier Hanin
 *
 */
public class PackageMapping {
    private String _package;
    private String _organisation;
    private String _module;
    private String _revision;
    
    public String getModule() {
        return _module;
    }
    public void setModule(String module) {
        _module = module;
    }
    public String getOrganisation() {
        return _organisation;
    }
    public void setOrganisation(String organisation) {
        _organisation = organisation;
    }
    public String getRevision() {
        return _revision;
    }
    public void setRevision(String revision) {
        _revision = revision;
    }
    public String getPackage() {
        return _package;
    }
    public void setPackage(String package1) {
        _package = package1;
    }
    public ModuleRevisionId getModuleRevisionId() {
        return ModuleRevisionId.newInstance(_organisation, _module, _revision);
    }
}
