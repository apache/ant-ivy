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
package org.apache.ivy.core.check;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolveEngine;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.plugins.parser.ModuleDescriptorParserRegistry;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.util.Message;

public class CheckEngine {
    private CheckEngineSettings settings;

    private ResolveEngine resolveEngine;

    public CheckEngine(CheckEngineSettings settings, ResolveEngine resolveEngine) {
        this.settings = settings;
        this.resolveEngine = resolveEngine;
    }

    /**
     * Checks the given ivy file using current settings to see if all dependencies are available,
     * with good confs. If a resolver name is given, it also checks that the declared publications
     * are available in the corresponding resolver. Note that the check is not performed
     * recursively, i.e. if a dependency has itself dependencies badly described or not available,
     * this check will not discover it.
     */
    public boolean check(URL ivyFile, String resolvername) {
        try {
            boolean result = true;
            // parse ivy file
            ModuleDescriptor md = ModuleDescriptorParserRegistry.getInstance().parseDescriptor(
                settings, ivyFile, settings.doValidate());

            // check publications if possible
            if (resolvername != null) {
                DependencyResolver resolver = settings.getResolver(resolvername);
                String[] confs = md.getConfigurationsNames();
                Set artifacts = new HashSet();
                for (int i = 0; i < confs.length; i++) {
                    artifacts.addAll(Arrays.asList(md.getArtifacts(confs[i])));
                }
                for (Iterator iter = artifacts.iterator(); iter.hasNext();) {
                    Artifact art = (Artifact) iter.next();
                    if (!resolver.exists(art)) {
                        Message.info("declared publication not found: " + art);
                        result = false;
                    }
                }
            }

            // check dependencies
            DependencyDescriptor[] dds = md.getDependencies();
            ResolveData data = new ResolveData(resolveEngine, new ResolveOptions());
            for (int i = 0; i < dds.length; i++) {
                // check master confs
                String[] masterConfs = dds[i].getModuleConfigurations();
                for (int j = 0; j < masterConfs.length; j++) {
                    if (!"*".equals(masterConfs[j].trim())
                            && md.getConfiguration(masterConfs[j]) == null) {
                        Message.info("dependency required in non existing conf for " + ivyFile
                                + " \n\tin " + dds[i] + ": " + masterConfs[j]);
                        result = false;
                    }
                }
                // resolve
                DependencyResolver resolver = settings
                        .getResolver(dds[i].getDependencyRevisionId());
                ResolvedModuleRevision rmr = resolver.getDependency(dds[i], data);
                if (rmr == null) {
                    Message.info("dependency not found in " + ivyFile + ":\n\t" + dds[i]);
                    result = false;
                } else {
                    String[] depConfs = dds[i].getDependencyConfigurations(md
                            .getConfigurationsNames());
                    for (int j = 0; j < depConfs.length; j++) {
                        if (!Arrays.asList(rmr.getDescriptor().getConfigurationsNames()).contains(
                            depConfs[j])) {
                            Message.info("dependency configuration is missing for " + ivyFile
                                    + "\n\tin " + dds[i] + ": " + depConfs[j]);
                            result = false;
                        }
                        Artifact[] arts = rmr.getDescriptor().getArtifacts(depConfs[j]);
                        for (int k = 0; k < arts.length; k++) {
                            if (!resolver.exists(arts[k])) {
                                Message.info("dependency artifact is missing for " + ivyFile
                                        + "\n\t in " + dds[i] + ": " + arts[k]);
                                result = false;
                            }
                        }
                    }
                }
            }
            return result;
        } catch (ParseException e) {
            Message.info("parse problem on " + ivyFile, e);
            return false;
        } catch (IOException e) {
            Message.info("io problem on " + ivyFile, e);
            return false;
        } catch (Exception e) {
            Message.info("problem on " + ivyFile, e);
            return false;
        }
    }

}
