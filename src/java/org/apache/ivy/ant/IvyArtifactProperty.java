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
package org.apache.ivy.ant;

import java.io.File;

import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.cache.ResolutionCacheManager;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.plugins.report.XmlReportParser;
import org.apache.tools.ant.BuildException;

/**
 * Set a set of ant properties according to the last artifact resolved
 */
public class IvyArtifactProperty extends IvyPostResolveTask {
    private String name;

    private String value;

    private boolean overwrite = false;

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;

    }

    public void doExecute() throws BuildException {
        prepareAndCheck();

        try {
            ResolutionCacheManager cacheMgr = getIvyInstance().getResolutionCacheManager();
            String[] confs = splitConfs(getConf());
            String resolveId = getResolveId();
            if (resolveId == null) {
                resolveId = ResolveOptions.getDefaultResolveId(getResolvedModuleId());
            }
            XmlReportParser parser = new XmlReportParser();
            for (int i = 0; i < confs.length; i++) {
                File report = cacheMgr.getConfigurationResolveReportInCache(resolveId, confs[i]);
                parser.parse(report);

                Artifact[] artifacts = parser.getArtifacts();
                for (int j = 0; j < artifacts.length; j++) {
                    Artifact artifact = artifacts[j];
                    String name = IvyPatternHelper.substitute(getSettings().substitute(getName()),
                        artifact, confs[i]);
                    String value = IvyPatternHelper.substitute(
                        getSettings().substitute(getValue()), artifact, confs[i]);
                    setProperty(name, value);
                }
            }
        } catch (Exception ex) {
            throw new BuildException("impossible to add artifact properties: " + ex, ex);
        }
    }

    private void setProperty(String name, String value) {
        if (overwrite) {
            getProject().setProperty(name, value);
        } else {
            getProject().setNewProperty(name, value);
        }
    }
}
