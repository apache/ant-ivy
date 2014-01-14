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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.util.Message;

public class JarJarDependencyAnalyser implements DependencyAnalyser {
    private File jarjarjarLocation;

    public JarJarDependencyAnalyser(File jarjarjarLocation) {
        this.jarjarjarLocation = jarjarjarLocation;
    }

    public ModuleDescriptor[] analyze(JarModule[] modules) {

        StringBuffer jarjarCmd = new StringBuffer("java -jar \"").append(
            jarjarjarLocation.getAbsolutePath()).append("\" --find --level=jar ");
        Map jarModulesMap = new HashMap();
        Map mds = new HashMap();

        for (int i = 0; i < modules.length; i++) {
            jarModulesMap.put(modules[i].getJar().getAbsolutePath(), modules[i]);
            DefaultModuleDescriptor md = DefaultModuleDescriptor.newBasicInstance(
                modules[i].getMrid(), new Date(modules[i].getJar().lastModified()));
            mds.put(modules[i].getMrid(), md);
            jarjarCmd.append("\"").append(modules[i].getJar().getAbsolutePath()).append("\"");
            if (i + 1 < modules.length) {
                jarjarCmd.append(File.pathSeparator);
            }
        }

        Message.verbose("jarjar command: " + jarjarCmd);

        try {
            Process p = Runtime.getRuntime().exec(jarjarCmd.toString());
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = r.readLine()) != null) {
                String[] deps = line.split(" -> ");
                JarModule module = (JarModule) jarModulesMap.get(deps[0]);
                JarModule dependency = (JarModule) jarModulesMap.get(deps[1]);

                if (module.getMrid().getModuleId().equals(dependency.getMrid().getModuleId())) {
                    continue;
                }
                Message.verbose(module.getMrid() + " depends on " + dependency.getMrid());

                DefaultModuleDescriptor md = (DefaultModuleDescriptor) mds.get(module.getMrid());

                DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(md,
                        dependency.getMrid(), false, false, true);
                dd.addDependencyConfiguration(ModuleDescriptor.DEFAULT_CONFIGURATION,
                    ModuleDescriptor.DEFAULT_CONFIGURATION);
                md.addDependency(dd);
            }
        } catch (IOException e) {
            Message.debug(e);
        }
        return (ModuleDescriptor[]) mds.values().toArray(new ModuleDescriptor[mds.values().size()]);
    }

    public static void main(String[] args) {
        JarJarDependencyAnalyser a = new JarJarDependencyAnalyser(new File(
                "D:/temp/test2/jarjar-0.7.jar"));
        a.analyze(new JarModuleFinder(
                "D:/temp/test2/ivyrep/[organisation]/[module]/[revision]/[artifact].[ext]")
                .findJarModules());
    }
}
