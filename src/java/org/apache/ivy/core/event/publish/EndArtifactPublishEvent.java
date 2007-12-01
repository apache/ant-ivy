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
 * Event fired after artifact publication has finished (possibly in error). Triggers registered on
 * {@link #NAME} will be notified of these events.
 * 
 * @see DependencyResolver#publish(Artifact, File, boolean)
 */
public class EndArtifactPublishEvent extends PublishEvent {

    private static final long serialVersionUID = -65690169431499422L;

    public static final String NAME = "post-publish-artifact";

    public static final String STATUS_SUCCESSFUL = "successful";

    public static final String STATUS_FAILED = "failed";

    private final boolean successful;

    public EndArtifactPublishEvent(DependencyResolver resolver, Artifact artifact, File data,
            boolean overwrite, boolean successful) {
        super(NAME, resolver, artifact, data, overwrite);
        this.successful = successful;
        addAttribute("status", isSuccessful() ? STATUS_SUCCESSFUL : STATUS_FAILED);
    }

    /**
     * @return true iff no errors were encountered during the publication
     */
    public boolean isSuccessful() {
        return successful;
    }
}
