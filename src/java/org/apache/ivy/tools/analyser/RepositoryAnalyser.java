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
package org.apache.ivy.tools.analyser;

import java.io.File;
import java.io.IOException;

import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorWriter;
import org.apache.ivy.util.Message;

public class RepositoryAnalyser {
    public void analyse(String pattern, DependencyAnalyser depAnalyser) {
        JarModuleFinder finder = new JarModuleFinder(pattern);
        ModuleDescriptor[] mds = depAnalyser.analyze(finder.findJarModules());
        Message.info("found " + mds.length + " modules");
        for (int i = 0; i < mds.length; i++) {
            File ivyFile = new File(IvyPatternHelper.substitute(
                pattern,
                DefaultArtifact.newIvyArtifact(mds[i].getModuleRevisionId(),
                    mds[i].getPublicationDate())));
            try {
                Message.info("generating " + ivyFile);
                XmlModuleDescriptorWriter.write(mds[i], ivyFile);
            } catch (IOException e) {
                Message.debug(e);
            }
        }
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out
                    .println("usage: ivyanalyser path/to/jarjar.jar absolute-ivy-repository-pattern");
            return;
        }
        String jarjarLocation = args[0];
        String pattern = args[1];

        JarJarDependencyAnalyser a = new JarJarDependencyAnalyser(new File(jarjarLocation));
        new RepositoryAnalyser().analyse(pattern, a);
    }
}
