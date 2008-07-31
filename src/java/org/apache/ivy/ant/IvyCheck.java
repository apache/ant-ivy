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
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.ivy.Ivy;
import org.apache.ivy.util.Message;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;

/**
 * Checks the given ivy file using current configuration to see if all dependencies are available,
 * with good confs. If a resolver name is given, it also checks that the declared publications are
 * available in the corresponding resolver. Note that the check is not performed recursively, i.e.
 * if a dependency has itself dependencies badly described or not available, this check will not
 * discover it.
 */
public class IvyCheck extends IvyTask {
    private File file = null;

    private List filesets = new ArrayList();

    private String resolvername;

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    /**
     * Adds a set of files to check.
     * 
     * @param set
     *            a set of files to check
     */
    public void addFileset(FileSet set) {
        filesets.add(set);
    }

    public String getResolvername() {
        return resolvername;
    }

    public void setResolvername(String resolverName) {
        resolvername = resolverName;
    }

    public void doExecute() throws BuildException {
        try {
            Ivy ivy = getIvyInstance();
            if (file != null) {
                if (ivy.check(file.toURI().toURL(), resolvername)) {
                    Message.verbose("checked " + file + ": OK");
                }
            }
            for (int i = 0; i < filesets.size(); i++) {
                FileSet fs = (FileSet) filesets.get(i);
                DirectoryScanner ds = fs.getDirectoryScanner(getProject());

                File fromDir = fs.getDir(getProject());

                String[] srcFiles = ds.getIncludedFiles();
                for (int j = 0; j < srcFiles.length; j++) {
                    File file = new File(fromDir, srcFiles[j]);
                    if (ivy.check(file.toURI().toURL(), resolvername)) {
                        Message.verbose("checked " + file + ": OK");
                    }
                }
            }
        } catch (MalformedURLException e) {
            throw new BuildException("impossible to convert a file to an url! " + e, e);
        }
    }

}
