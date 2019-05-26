/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
     *
     * @param ivyFile URL
     * @param resolvername String
     * @return boolean
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
                Set<Artifact> artifacts = new HashSet<>();
                for (String conf : md.getConfigurationsNames()) {
                    artifacts.addAll(Arrays.asList(md.getArtifacts(conf)));
                }
                for (Artifact artifact : artifacts) {
                    if (!resolver.exists(artifact)) {
                        Message.info("declared publication not found: " + artifact);
                        result = false;
                    }
                }
            }

            // check dependencies
            ResolveData data = new ResolveData(resolveEngine, new ResolveOptions());
            for (DependencyDescriptor dd : md.getDependencies()) {
                // check master confs
                for (String masterConf : dd.getModuleConfigurations()) {
                    if (!"*".equals(masterConf.trim()) && md.getConfiguration(masterConf) == null) {
                        Message.info("dependency required in non existing conf for " + ivyFile
                                + " \n\tin " + dd + ": " + masterConf);
                        result = false;
                    }
                }
                // resolve
                DependencyResolver resolver = settings.getResolver(dd.getDependencyRevisionId());
                ResolvedModuleRevision rmr = resolver.getDependency(dd, data);
                if (rmr == null) {
                    Message.info("dependency not found in " + ivyFile + ":\n\t" + dd);
                    result = false;
                } else {
                    for (String depConf : dd.getDependencyConfigurations(md.getConfigurationsNames())) {
                        if (!Arrays.asList(rmr.getDescriptor().getConfigurationsNames())
                                .contains(depConf)) {
                            Message.info("dependency configuration is missing for " + ivyFile
                                    + "\n\tin " + dd + ": " + depConf);
                            result = false;
                        }
                        for (Artifact art : rmr.getDescriptor().getArtifacts(depConf)) {
                            if (!resolver.exists(art)) {
                                Message.info("dependency artifact is missing for " + ivyFile
                                        + "\n\t in " + dd + ": " + art);
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
