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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.filters.LineContainsRegExp;
import org.apache.tools.ant.filters.TokenFilter;
import org.apache.tools.ant.taskdefs.Concat;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.FilterChain;
import org.apache.tools.ant.types.RegularExpression;

/**
 * Extracts imports from a set of java sources and generate corresponding ivy file
 */
public class IvyExtractFromSources extends Task {
    public static class Ignore {
        private String packageName;

        public String getPackage() {
            return packageName;
        }

        public void setPackage(String package1) {
            packageName = package1;
        }
    }

    private String organisation;

    private String module;

    private String revision;

    private String status;

    private List ignoredPackaged = new ArrayList(); // List (String package)

    private Map mapping = new HashMap(); // Map (String package -> ModuleRevisionId)

    private Concat concat = new Concat();

    private File to;

    public void addConfiguredIgnore(Ignore ignore) {
        ignoredPackaged.add(ignore.getPackage());
    }

    public File getTo() {
        return to;
    }

    public void setTo(File to) {
        this.to = to;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getOrganisation() {
        return organisation;
    }

    public void setOrganisation(String organisation) {
        this.organisation = organisation;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void addConfiguredMapping(PackageMapping mapping) {
        this.mapping.put(mapping.getPackage(), mapping.getModuleRevisionId());
    }

    public void addFileSet(FileSet fileSet) {
        concat.addFileset(fileSet);
    }

    public void execute() throws BuildException {
        configureConcat();
        Writer out = new StringWriter();
        concat.setWriter(out);
        concat.execute();
        Set importsSet = new HashSet(Arrays.asList(out.toString().split("\n")));
        Set dependencies = new HashSet();
        for (Iterator iter = importsSet.iterator(); iter.hasNext();) {
            String pack = ((String) iter.next()).trim();
            ModuleRevisionId mrid = getMapping(pack);
            if (mrid != null) {
                dependencies.add(mrid);
            }
        }
        try {
            PrintWriter writer = new PrintWriter(new FileOutputStream(to));
            writer.println("<ivy-module version=\"1.0\">");
            writer.println("\t<info organisation=\"" + organisation + "\"");
            writer.println("\t       module=\"" + module + "\"");
            if (revision != null) {
                writer.println("\t       revision=\"" + revision + "\"");
            }
            if (status != null) {
                writer.println("\t       status=\"" + status + "\"");
            } else {
                writer.println("\t       status=\"integration\"");
            }
            writer.println("\t/>");
            if (!dependencies.isEmpty()) {
                writer.println("\t<dependencies>");
                for (Iterator iter = dependencies.iterator(); iter.hasNext();) {
                    ModuleRevisionId mrid = (ModuleRevisionId) iter.next();
                    writer.println("\t\t<dependency org=\"" + mrid.getOrganisation() + "\" name=\""
                            + mrid.getName() + "\" rev=\"" + mrid.getRevision() + "\"/>");
                }
                writer.println("\t</dependencies>");
            }
            writer.println("</ivy-module>");
            writer.close();
            log(dependencies.size() + " dependencies put in " + to);
        } catch (FileNotFoundException e) {
            throw new BuildException("impossible to create file " + to + ": " + e, e);
        }
    }

    /**
     * @param pack
     * @return
     */
    private ModuleRevisionId getMapping(String pack) {
        String askedPack = pack;
        ModuleRevisionId ret = null;
        while (ret == null && pack.length() > 0) {
            if (ignoredPackaged.contains(pack)) {
                return null;
            }
            ret = (ModuleRevisionId) mapping.get(pack);
            int lastDotIndex = pack.lastIndexOf('.');
            if (lastDotIndex != -1) {
                pack = pack.substring(0, lastDotIndex);
            } else {
                break;
            }
        }
        if (ret == null) {
            log("no mapping found for " + askedPack, Project.MSG_VERBOSE);
        }
        return ret;
    }

    private void configureConcat() {
        concat.setProject(getProject());
        concat.setTaskName(getTaskName());
        FilterChain filterChain = new FilterChain();
        LineContainsRegExp lcre = new LineContainsRegExp();
        RegularExpression regexp = new RegularExpression();
        regexp.setPattern("^import .+;");
        lcre.addConfiguredRegexp(regexp);
        filterChain.add(lcre);
        TokenFilter tf = new TokenFilter();
        TokenFilter.ReplaceRegex rre = new TokenFilter.ReplaceRegex();
        rre.setPattern("import (.+);.*");
        rre.setReplace("\\1");
        tf.add(rre);
        filterChain.add(tf);
        concat.addFilterChain(filterChain);
    }
}
