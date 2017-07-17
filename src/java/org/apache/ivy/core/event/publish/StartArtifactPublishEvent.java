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
package org.apache.ivy.core.event.publish;

import java.io.File;

import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.plugins.resolver.DependencyResolver;

/**
 * Event fired just before an artifact is published into a resolver. Triggers registered on
 * {@link #NAME} will be notified of these events.
 *
 * @see DependencyResolver#publish(Artifact, File, boolean)
 */
public class StartArtifactPublishEvent extends PublishEvent {

    public static final String NAME = "pre-publish-artifact";

    public StartArtifactPublishEvent(DependencyResolver resolver, Artifact artifact, File data,
            boolean overwrite) {
        super(NAME, resolver, artifact, data, overwrite);
    }

}
