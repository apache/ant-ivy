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
package fr.jayasoft.ivy;

import java.net.URL;

import fr.jayasoft.ivy.matcher.PatternMatcher;

/**
 * This describes an artifact that is asked for a dependency.
 * 
 * It is used to resctrict the artifacts asked for a dependency, or describe them
 * when there is no ivy file.
 */
public interface DependencyArtifactDescriptor {
    /**
     * Returns the dependency descriptor in which this artifact is asked
     * @return
     */
    public DependencyDescriptor getDependency();

    /**
     * Returns the id of the described artifact, without revision information
     * @return
     */
    public ArtifactId getId();
    /**
     * Returns the name of the artifact asked
     * @return
     */
    public String getName();
    /**
     * Returns the type of the artifact asked
     * @return
     */
    public String getType();
    /**
     * Returns the ext of the artifact asked
     * @return
     */
    public String getExt();
    /**
     * Returns the url to look this artifact at
     * @return
     */
    public URL getUrl();
    /**
     * Returns the configurations of the module in which the artifact is asked
     * @return an array of configuration names in which the artifact is asked
     */
    public String[] getConfigurations();
    
    /**
     * Returns the matcher to use to know if an artifact match the current descriptor
     * @return
     */
    public PatternMatcher getMatcher();
}
