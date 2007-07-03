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
package org.apache.ivy.plugins.parser.xml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyArtifactDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ExcludeRule;
import org.apache.ivy.core.module.descriptor.IncludeRule;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;

/**
 *
 */
public class XmlModuleDescriptorWriter {
    public static void write(ModuleDescriptor md, File output) throws IOException {
        write(md, null, output);
    }

    public static void write(ModuleDescriptor md, String licenseHeader, File output)
            throws IOException {
        if (output.getParentFile() != null) {
            output.getParentFile().mkdirs();
        }
        PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(output),
                "UTF-8"));
        try {
            out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            if (licenseHeader != null) {
                out.print(licenseHeader);
            }
            out.println("<ivy-module version=\"1.0\">");
            out.println("\t<info organisation=\"" + md.getModuleRevisionId().getOrganisation()
                    + "\"");
            out.println("\t\tmodule=\"" + md.getModuleRevisionId().getName() + "\"");
            String branch = md.getResolvedModuleRevisionId().getBranch();
            if (branch != null) {
                out.println("\t\tbranch=\"" + branch + "\"");
            }
            String revision = md.getResolvedModuleRevisionId().getRevision();
            if (revision != null) {
                out.println("\t\trevision=\"" + revision + "\"");
            }
            out.println("\t\tstatus=\"" + md.getStatus() + "\"");
            out.println("\t\tpublication=\""
                    + Ivy.DATE_FORMAT.format(md.getResolvedPublicationDate()) + "\"");
            if (md.isDefault()) {
                out.println("\t\tdefault=\"true\"");
            }
            if (md instanceof DefaultModuleDescriptor) {
                DefaultModuleDescriptor dmd = (DefaultModuleDescriptor) md;
                if (dmd.getNamespace() != null && !dmd.getNamespace().getName().equals("system")) {
                    out.println("\t\tnamespace=\"" + dmd.getNamespace().getName() + "\"");
                }
            }
            out.println("\t/>");
            Configuration[] confs = md.getConfigurations();
            if (confs.length > 0) {
                out.println("\t<configurations>");
                for (int i = 0; i < confs.length; i++) {
                    out.print("\t\t<conf");
                    out.print(" name=\"" + confs[i].getName() + "\"");
                    out.print(" visibility=\"" + confs[i].getVisibility() + "\"");
                    if (confs[i].getDescription() != null) {
                        out.print(" description=\"" + confs[i].getDescription() + "\"");
                    }
                    String[] exts = confs[i].getExtends();
                    if (exts.length > 0) {
                        out.print(" extends=\"");
                        for (int j = 0; j < exts.length; j++) {
                            out.print(exts[j]);
                            if (j + 1 < exts.length) {
                                out.print(",");
                            }
                        }
                        out.print("\"");
                    }
                    out.println("/>");
                }
                out.println("\t</configurations>");
            }
            out.println("\t<publications>");
            Artifact[] artifacts = md.getAllArtifacts();
            for (int i = 0; i < artifacts.length; i++) {
                out.print("\t\t<artifact");
                out.print(" name=\"" + artifacts[i].getName() + "\"");
                out.print(" type=\"" + artifacts[i].getType() + "\"");
                out.print(" ext=\"" + artifacts[i].getExt() + "\"");
                out.print(" conf=\"" + getConfs(md, artifacts[i]) + "\"");
                out.println("/>");
            }
            out.println("\t</publications>");

            DependencyDescriptor[] dds = md.getDependencies();
            if (dds.length > 0) {
                out.println("\t<dependencies>");
                for (int i = 0; i < dds.length; i++) {
                    out.print("\t\t<dependency");
                    out
                            .print(" org=\"" + dds[i].getDependencyRevisionId().getOrganisation()
                                    + "\"");
                    out.print(" name=\"" + dds[i].getDependencyRevisionId().getName() + "\"");
                    out.print(" rev=\"" + dds[i].getDependencyRevisionId().getRevision() + "\"");
                    if (dds[i].isForce()) {
                        out.print(" force=\"" + dds[i].isForce() + "\"");
                    }
                    if (dds[i].isChanging()) {
                        out.print(" changing=\"" + dds[i].isChanging() + "\"");
                    }
                    if (!dds[i].isTransitive()) {
                        out.print(" transitive=\"" + dds[i].isTransitive() + "\"");
                    }
                    out.print(" conf=\"");
                    String[] modConfs = dds[i].getModuleConfigurations();
                    for (int j = 0; j < modConfs.length; j++) {
                        String[] depConfs = dds[i].getDependencyConfigurations(modConfs[j]);
                        out.print(modConfs[j] + "->");
                        for (int k = 0; k < depConfs.length; k++) {
                            out.print(depConfs[k]);
                            if (k + 1 < depConfs.length) {
                                out.print(",");
                            }
                        }
                        if (j + 1 < modConfs.length) {
                            out.print(";");
                        }
                    }
                    out.print("\"");
                    DependencyArtifactDescriptor[] depArtifacts = dds[i]
                            .getAllDependencyArtifacts();
                    if (depArtifacts.length > 0) {
                        out.println(">");
                        for (int j = 0; j < depArtifacts.length; j++) {
                            out.print("\t\t\t<artifact");
                            out.print(" name=\"" + depArtifacts[j].getName() + "\"");
                            out.print(" type=\"" + depArtifacts[j].getType() + "\"");
                            out.print(" ext=\"" + depArtifacts[j].getExt() + "\"");
                            String[] dadconfs = depArtifacts[j].getConfigurations();
                            if (!Arrays.asList(dadconfs).equals(
                                Arrays.asList(md.getConfigurationsNames()))) {
                                out.print(" conf=\"");
                                for (int k = 0; k < dadconfs.length; k++) {
                                    out.print(dadconfs[k]);
                                    if (k + 1 < dadconfs.length) {
                                        out.print(",");
                                    }
                                }
                                out.print("\"");
                            }
                            Map extra = depArtifacts[j].getExtraAttributes();
                            for (Iterator iter = extra.entrySet().iterator(); iter.hasNext();) {
                                Map.Entry entry = (Map.Entry) iter.next();
                                out.print(" " + entry.getKey() + "=\"" + entry.getValue() + "\"");
                            }
                            out.println("/>");
                        }
                    }
                    IncludeRule[] includes = dds[i].getAllIncludeRules();
                    if (includes.length > 0) {
                        if (depArtifacts.length == 0) {
                            out.println(">");
                        }
                        for (int j = 0; j < includes.length; j++) {
                            out.print("\t\t\t<include");
                            out.print(" name=\"" + includes[j].getId().getName() + "\"");
                            out.print(" type=\"" + includes[j].getId().getType() + "\"");
                            out.print(" ext=\"" + includes[j].getId().getExt() + "\"");
                            String[] ruleConfs = includes[j].getConfigurations();
                            if (!Arrays.asList(ruleConfs).equals(
                                Arrays.asList(md.getConfigurationsNames()))) {
                                out.print(" conf=\"");
                                for (int k = 0; k < ruleConfs.length; k++) {
                                    out.print(ruleConfs[k]);
                                    if (k + 1 < ruleConfs.length) {
                                        out.print(",");
                                    }
                                }
                                out.print("\"");
                            }
                            out.print(" matcher=\"" + includes[j].getMatcher().getName() + "\"");
                            out.println("/>");
                        }
                    }
                    ExcludeRule[] excludes = dds[i].getAllExcludeRules();
                    if (excludes.length > 0) {
                        if (includes.length == 0 && depArtifacts.length == 0) {
                            out.println(">");
                        }
                        for (int j = 0; j < excludes.length; j++) {
                            out.print("\t\t\t<exclude");
                            out.print(" org=\""
                                    + excludes[j].getId().getModuleId().getOrganisation() + "\"");
                            out.print(" module=\"" + excludes[j].getId().getModuleId().getName()
                                    + "\"");
                            out.print(" name=\"" + excludes[j].getId().getName() + "\"");
                            out.print(" type=\"" + excludes[j].getId().getType() + "\"");
                            out.print(" ext=\"" + excludes[j].getId().getExt() + "\"");
                            String[] ruleConfs = excludes[j].getConfigurations();
                            if (!Arrays.asList(ruleConfs).equals(
                                Arrays.asList(md.getConfigurationsNames()))) {
                                out.print(" conf=\"");
                                for (int k = 0; k < ruleConfs.length; k++) {
                                    out.print(ruleConfs[k]);
                                    if (k + 1 < ruleConfs.length) {
                                        out.print(",");
                                    }
                                }
                                out.print("\"");
                            }
                            out.print(" matcher=\"" + excludes[j].getMatcher().getName() + "\"");
                            out.println("/>");
                        }
                    }
                    if (includes.length + excludes.length + depArtifacts.length == 0) {
                        out.println("/>");
                    } else {
                        out.println("\t\t</dependency>");
                    }
                }
                ExcludeRule[] excludes = md.getAllExcludeRules();
                if (excludes.length > 0) {
                    for (int j = 0; j < excludes.length; j++) {
                        out.print("\t\t<exclude");
                        out.print(" org=\""
                                + excludes[j].getId().getModuleId().getOrganisation() + "\"");
                        out.print(" module=\"" + excludes[j].getId().getModuleId().getName()
                                + "\"");
                        out.print(" artifact=\"" + excludes[j].getId().getName() + "\"");
                        out.print(" type=\"" + excludes[j].getId().getType() + "\"");
                        out.print(" ext=\"" + excludes[j].getId().getExt() + "\"");
                        String[] ruleConfs = excludes[j].getConfigurations();
                        if (!Arrays.asList(ruleConfs).equals(
                            Arrays.asList(md.getConfigurationsNames()))) {
                            out.print(" conf=\"");
                            for (int k = 0; k < ruleConfs.length; k++) {
                                out.print(ruleConfs[k]);
                                if (k + 1 < ruleConfs.length) {
                                    out.print(",");
                                }
                            }
                            out.print("\"");
                        }
                        out.print(" matcher=\"" + excludes[j].getMatcher().getName() + "\"");
                        out.println("/>");
                    }
                }
                out.println("\t</dependencies>");
            }
            out.println("</ivy-module>");
        } finally {
            out.close();
        }
    }

    private static String getConfs(ModuleDescriptor md, Artifact artifact) {
        StringBuffer ret = new StringBuffer();

        String[] confs = md.getConfigurationsNames();
        for (int i = 0; i < confs.length; i++) {
            if (Arrays.asList(md.getArtifacts(confs[i])).contains(artifact)) {
                ret.append(confs[i]).append(",");
            }
        }
        if (ret.length() > 0) {
            ret.setLength(ret.length() - 1);
        }
        return ret.toString();
    }
}
