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
package org.apache.ivy.core.event.resolve;

import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.report.ResolveReport;

public class EndResolveEvent extends ResolveEvent {
    public static final String NAME = "post-resolve";

    private ResolveReport report;

    public EndResolveEvent(ModuleDescriptor md, String[] confs, ResolveReport report) {
        super(NAME, md, confs);
        this.report = report;
        addAttribute("resolve-id", String.valueOf(report.getResolveId()));
        addAttribute("nb-dependencies", String.valueOf(report.getDependencies().size()));
        addAttribute("nb-artifacts", String.valueOf(report.getArtifacts().size()));
        addAttribute("resolve-duration", String.valueOf(report.getResolveTime()));
        addAttribute("download-duration", String.valueOf(report.getDownloadTime()));
        addAttribute("download-size", String.valueOf(report.getDownloadSize()));
    }

    public ResolveReport getReport() {
        return report;
    }

}
