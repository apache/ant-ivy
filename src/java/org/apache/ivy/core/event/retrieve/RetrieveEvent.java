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

import org.apache.ivy.core.event.IvyEvent;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.retrieve.RetrieveOptions;

public class RetrieveEvent extends IvyEvent {
    private ModuleRevisionId mrid;

    private RetrieveOptions options;

    protected RetrieveEvent(String name, ModuleRevisionId mrid, String[] confs,
            RetrieveOptions options) {
        super(name);
        this.mrid = mrid;
        addMridAttributes(mrid);
        addConfsAttribute(confs);
        addAttribute("symlink", String.valueOf(options.isMakeSymlinks()));
        addAttribute("sync", String.valueOf(options.isSync()));
        this.options = options;
    }

    public ModuleRevisionId getModuleRevisionId() {
        return mrid;
    }

    public RetrieveOptions getOptions() {
        return options;
    }
}
