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

import java.util.Collection;

import org.apache.ivy.core.cache.RepositoryCacheManager;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.plugins.latest.LatestStrategy;
import org.apache.ivy.plugins.namespace.Namespace;
import org.apache.ivy.plugins.parser.ParserSettings;
import org.apache.ivy.plugins.signer.SignatureGenerator;
import org.apache.ivy.plugins.version.VersionMatcher;

public interface ResolverSettings extends ParserSettings {

    LatestStrategy getLatestStrategy(String latestStrategyName);

    LatestStrategy getDefaultLatestStrategy();

    RepositoryCacheManager getRepositoryCacheManager(String name);

    RepositoryCacheManager getDefaultRepositoryCacheManager();

    RepositoryCacheManager[] getRepositoryCacheManagers();

    Namespace getNamespace(String namespaceName);

    Namespace getSystemNamespace();

    String getVariable(String string);

    void configureRepositories(boolean b);

    VersionMatcher getVersionMatcher();

    String getResolveMode(ModuleId moduleId);

    void filterIgnore(Collection<String> names);

    SignatureGenerator getSignatureGenerator(String name);

}
