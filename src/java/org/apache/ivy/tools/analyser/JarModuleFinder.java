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
import java.util.ArrayList;
import java.util.List;

import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.resolver.util.FileURLLister;
import org.apache.ivy.plugins.resolver.util.ResolverHelper;
import org.apache.ivy.plugins.resolver.util.URLLister;
import org.apache.ivy.util.Message;

public class JarModuleFinder {
    private String pattern;

    private String filePattern;

    public JarModuleFinder(String pattern) {
        this.pattern = "file:///" + pattern;
        this.filePattern = pattern;
    }

    public JarModule[] findJarModules() {
        List<JarModule> ret = new ArrayList<>();
        URLLister lister = new FileURLLister();
        try {
            for (String org : ResolverHelper.listTokenValues(lister, pattern, "organisation")) {
                String orgPattern = IvyPatternHelper.substituteToken(pattern,
                        IvyPatternHelper.ORGANISATION_KEY, org);
                for (String module : ResolverHelper.listTokenValues(lister, orgPattern, "module")) {
                    String modPattern = IvyPatternHelper.substituteToken(orgPattern,
                            IvyPatternHelper.MODULE_KEY, module);
                    for (String rev : ResolverHelper.listTokenValues(lister, modPattern, "revision")) {
                        File jar = new File(IvyPatternHelper.substitute(filePattern, org,
                                module, rev, module, "jar", "jar"));
                        if (jar.exists()) {
                            ret.add(new JarModule(ModuleRevisionId.newInstance(org, module, rev), jar));
                        }
                    }
                }
            }

        } catch (Exception e) {
            Message.debug(e);
            // TODO: handle exception
        }
        return ret.toArray(new JarModule[ret.size()]);
    }

    public static void main(String[] args) {
        JarModule[] mods = new JarModuleFinder(
                "D:/temp/test2/ivyrep/[organisation]/[module]/[revision]/[artifact].[ext]")
                .findJarModules();
        for (JarModule mod : mods) {
            System.out.println(mod);
        }
    }
}
