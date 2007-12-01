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

import java.io.File;

import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.plugins.lock.LockStrategy;
import org.apache.ivy.plugins.parser.ParserSettings;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.plugins.version.VersionMatcher;

public interface CacheSettings extends ParserSettings {
    File getDefaultCache();
    
    File getResolutionCacheRoot(File cache);
    
    File getRepositoryCacheRoot(File cache);

    String getCacheResolvedIvyPattern();

    String getCacheResolvedIvyPropertiesPattern();

    String getCacheIvyPattern();

    String getCacheArtifactPattern();

    String getCacheDataFilePattern();

    VersionMatcher getVersionMatcher();

    DependencyResolver getResolver(ModuleId moduleId);

    DependencyResolver getResolver(String artResolverName);
    
    LockStrategy getLockStrategy(String name);
    
    LockStrategy getDefaultLockStrategy();
    
    boolean debugLocking();
}
