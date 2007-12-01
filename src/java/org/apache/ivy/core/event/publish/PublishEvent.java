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

import org.apache.ivy.core.event.IvyEvent;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.plugins.resolver.DependencyResolver;

/**
 * Base class for events fired during {@link DependencyResolver#publish(Artifact, File, boolean)}.
 * 
 * @see StartArtifactPublishEvent
 * @see EndArtifactPublishEvent
 */
public abstract class PublishEvent extends IvyEvent {

    private final DependencyResolver resolver;

    private final Artifact artifact;

    private final File data;

    private final boolean overwrite;

    protected PublishEvent(String name, DependencyResolver resolver, Artifact artifact, File data,
            boolean overwrite) {
        super(name);
        this.resolver = resolver;
        this.artifact = artifact;
        this.data = data;
        this.overwrite = overwrite;

        addMridAttributes(artifact.getModuleRevisionId());
        addAttributes(artifact.getAttributes());
        addAttribute("resolver", resolver.getName());
        addAttribute("file", data.getAbsolutePath());
        addAttribute("overwrite", String.valueOf(overwrite));
    }

    /** @return the resolver into which the artifact is being published */
    public DependencyResolver getResolver() {
        return resolver;
    }

    /** @return a local file containing the artifact data */
    public File getData() {
        return data;
    }

    /** @return metadata about the artifact being published */
    public Artifact getArtifact() {
        return artifact;
    }

    /** @return true iff this event overwrites existing resolver data for this artifact */
    public boolean isOverwrite() {
        return overwrite;
    }

}
