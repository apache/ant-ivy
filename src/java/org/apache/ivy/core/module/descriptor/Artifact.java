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
package org.apache.ivy.core.module.descriptor;

import java.net.URL;
import java.util.Date;

import org.apache.ivy.core.module.id.ArtifactRevisionId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.util.extendable.ExtendableItem;



/**
 * @author x.hanin
 *
 */
public interface Artifact extends ExtendableItem {
    /**
     * Returns the resolved module revision id for this artifact
     * @return
     */
    ModuleRevisionId getModuleRevisionId();
    /**
     * Returns the resolved publication date for this artifact
     * @return the resolved publication date
     */
    Date getPublicationDate();
    String getName();
    String getType();
    String getExt();
    /**
     * Returns the url at which this artifact can be found independently of ivy configuration.
     * This can be null (and is usually for standard artifacts)
     * @return url at which this artifact can be found independently of ivy configuration
     */
    URL getUrl();
    String[] getConfigurations();

    /**
     * @return the id of the artifact
     */
    ArtifactRevisionId getId();
}
