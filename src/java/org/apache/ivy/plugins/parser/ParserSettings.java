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
package org.apache.ivy.plugins.parser;

import java.io.File;
import java.util.Map;

import org.apache.ivy.core.RelativeUrlResolver;
import org.apache.ivy.core.cache.ResolutionCacheManager;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.module.status.StatusManager;
import org.apache.ivy.plugins.conflict.ConflictManager;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.ivy.plugins.namespace.Namespace;
import org.apache.ivy.plugins.resolver.DependencyResolver;

public interface ParserSettings {

    String substitute(String value);

    Map<String, String> substitute(Map<String, String> strings);

    ResolutionCacheManager getResolutionCacheManager();

    ConflictManager getConflictManager(String name);

    PatternMatcher getMatcher(String matcherName);

    Namespace getNamespace(String namespace);

    StatusManager getStatusManager();

    RelativeUrlResolver getRelativeUrlResolver();

    DependencyResolver getResolver(ModuleRevisionId mRevId);

    File resolveFile(String filename);

    String getDefaultBranch(ModuleId moduleId);

    /**
     * Returns the namespace context in which the current descriptor is parsed.
     */
    Namespace getContextNamespace();

    String getVariable(String string);
}
