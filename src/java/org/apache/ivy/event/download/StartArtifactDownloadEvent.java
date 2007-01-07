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
package org.apache.ivy.event.download;

import org.apache.ivy.Artifact;
import org.apache.ivy.ArtifactOrigin;
import org.apache.ivy.DependencyResolver;
import org.apache.ivy.Ivy;

public class StartArtifactDownloadEvent extends DownloadEvent {
    public static final String NAME = "pre-download-artifact";
    
	private DependencyResolver _resolver;
	private ArtifactOrigin _origin;

    public StartArtifactDownloadEvent(Ivy source, DependencyResolver resolver, Artifact artifact, ArtifactOrigin origin) {
        super(source, NAME, artifact);
        _resolver = resolver;
        _origin = origin;
        addAttribute("resolver", _resolver.getName());
        addAttribute("origin", origin.getLocation());
        addAttribute("local", String.valueOf(origin.isLocal()));
    }

    public DependencyResolver getResolver() {
        return _resolver;
    }

	public ArtifactOrigin getOrigin() {
		return _origin;
	}

}
