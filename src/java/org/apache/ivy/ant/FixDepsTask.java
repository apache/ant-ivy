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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorWriter;
import org.apache.tools.ant.BuildException;

public class FixDepsTask extends IvyPostResolveTask {

    private File dest;

    private List<Keep> keeps = new ArrayList<Keep>();

    public void setToFile(File dest) {
        this.dest = dest;
    }

    public static class Keep {

        private String org;

        private String module;

        public void setOrg(String org) {
            this.org = org;
        }

        public void setModule(String module) {
            this.module = module;
        }
    }

    public Keep createKeep() {
        Keep k = new Keep();
        keeps.add(k);
        return k;
    }

    @Override
    public void doExecute() throws BuildException {
        prepareAndCheck();

        if (dest == null) {
            throw new BuildException("Missing required parameter 'tofile'");
        }
        if (dest.exists() && dest.isDirectory()) {
            throw new BuildException("The destination file '" + dest.getAbsolutePath()
                    + "' already exist and is a folder");
        }

        ResolveReport report = getResolvedReport();

        List<ModuleId> midToKeep = new ArrayList<ModuleId>();
        for (Keep keep : keeps) {
            midToKeep.add(ModuleId.newInstance(keep.org, keep.module));
        }

        ModuleDescriptor md = report.toFixedModuleDescriptor(getSettings(), midToKeep);
        try {
            XmlModuleDescriptorWriter.write(md, dest);
        } catch (IOException e) {
            throw new BuildException("Failed to write into the file " + dest.getAbsolutePath()
                    + " (" + e.getMessage() + ")", e);
        }
    }

}
