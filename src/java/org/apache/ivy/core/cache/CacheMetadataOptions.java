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
package org.apache.ivy.core.cache;

import org.apache.ivy.plugins.namespace.Namespace;

public class CacheMetadataOptions extends CacheDownloadOptions {
    private boolean isChanging = false; 
    private boolean isCheckmodified = false;
    private boolean validate = false; 
    private Namespace namespace = Namespace.SYSTEM_NAMESPACE;
    
    public boolean isChanging() {
        return isChanging;
    }
    public CacheMetadataOptions setChanging(boolean isChanging) {
        this.isChanging = isChanging;
        return this;
    }
    public Namespace getNamespace() {
        return namespace;
    }
    public CacheMetadataOptions setNamespace(Namespace namespace) {
        this.namespace = namespace;
        return this;
    }
    public boolean isValidate() {
        return validate;
    }
    public CacheMetadataOptions setValidate(boolean validate) {
        this.validate = validate;
        return this;
    }
    public boolean isCheckmodified() {
        return isCheckmodified;
    }
    public CacheMetadataOptions setCheckmodified(boolean isCheckmodified) {
        this.isCheckmodified = isCheckmodified;
        return this;
    }
}
