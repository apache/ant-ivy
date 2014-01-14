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
package org.apache.ivy.core.event.retrieve;

import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.retrieve.RetrieveOptions;

public class EndRetrieveEvent extends RetrieveEvent {
    public static final String NAME = "post-retrieve";

    private long duration;

    private int nbCopied;

    private int nbUpToDate;

    private long totalCopiedSize;

    public EndRetrieveEvent(ModuleRevisionId mrid, String[] confs, long elapsedTime,
            int targetsCopied, int targetsUpToDate, long totalCopiedSize, RetrieveOptions options) {
        super(NAME, mrid, confs, options);

        this.duration = elapsedTime;
        this.nbCopied = targetsCopied;
        this.nbUpToDate = targetsUpToDate;
        this.totalCopiedSize = totalCopiedSize;
        addAttribute("duration", String.valueOf(elapsedTime));
        addAttribute("size", String.valueOf(totalCopiedSize));
        addAttribute("nbCopied", String.valueOf(targetsCopied));
        addAttribute("nbUptodate", String.valueOf(targetsUpToDate));
    }

    /**
     * Duration of the retrieve operation, in ms.
     * 
     * @return Duration of the retrieve operation, in ms.
     */
    public long getDuration() {
        return duration;
    }

    /**
     * Number of artifacts which were copied (or symlinked) during the retrieve
     * 
     * @return Number of artifacts which were copied during the retrieve.
     */
    public int getNbCopied() {
        return nbCopied;
    }

    /**
     * Number of artifacts which were not copied since they were already present and up to date.
     * 
     * @return Number of artifacts which were not copied since they were already present and up to
     *         date.
     */
    public int getNbUpToDate() {
        return nbUpToDate;
    }

    /**
     * Total size of all copied (or symlinked) artifacts, in bytes.
     * 
     * @return Total size of all copied (or symlinked) artifacts, in bytes.
     */
    public long getTotalCopiedSize() {
        return totalCopiedSize;
    }
}
