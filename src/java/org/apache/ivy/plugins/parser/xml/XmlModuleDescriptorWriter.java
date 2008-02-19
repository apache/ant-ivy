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
import java.util.Map.Entry;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyArtifactDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ExcludeRule;
import org.apache.ivy.core.module.descriptor.IncludeRule;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.util.XMLHelper;
import org.apache.ivy.util.extendable.ExtendableItem;

/**
 *
 */
public final class XmlModuleDescriptorWriter {
    
    private XmlModuleDescriptorWriter() {
        //Utility class
    }
    
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
            StringBuffer xmlNamespace = new StringBuffer();
            Map namespaces = md.getExtraAttributesNamespaces();
            for (Iterator iter = namespaces.entrySet().iterator(); iter.hasNext();) {
                Entry ns = (Entry) iter.next();
                xmlNamespace.append(" xmlns:").append(ns.getKey()).append("=\"")
                            .append(ns.getValue()).append("\"");
            }
            out.println("<ivy-module version=\"1.0\"" + xmlNamespace + ">");
            printInfoTag(md, out);
            printConfigurations(md, out);
            printPublications(md, out);
            printDependencies(md, out);
            out.println("</ivy-module>");
        } finally {
            out.close();
        }
    }

    private static void printDependencies(ModuleDescriptor md, PrintWriter out) {
        DependencyDescriptor[] dds = md.getDependencies();
        if (dds.length > 0) {
            out.println("\t<dependencies>");
            for (int i = 0; i < dds.length; i++) {
                out.print("\t\t<dependency");
                out.print(" org=\"" 
                    + XMLHelper.escape(dds[i].getDependencyRevisionId().getOrganisation()) + "\"");
                out.print(" name=\"" 
                    + XMLHelper.escape(dds[i].getDependencyRevisionId().getName()) + "\"");
                out.print(" rev=\"" 
                    + XMLHelper.escape(dds[i].getDependencyRevisionId().getRevision()) + "\"");
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
                    out.print(XMLHelper.escape(modConfs[j]) + "->");
                    for (int k = 0; k < depConfs.length; k++) {
                        out.print(XMLHelper.escape(depConfs[k]));
                        if (k + 1 < depConfs.length) {
                            out.print(",");
                        }
                    }
                    if (j + 1 < modConfs.length) {
                        out.print(";");
                    }
                }
                out.print("\"");
                
                printExtraAttributes(dds[i], out, " ");
                
                DependencyArtifactDescriptor[] depArtifacts = dds[i].getAllDependencyArtifacts();
                if (depArtifacts.length > 0) {
                    out.println(">");
                }
                printDependencyArtefacts(md, out, depArtifacts);
                
                IncludeRule[] includes = dds[i].getAllIncludeRules();
                if (includes.length > 0 && depArtifacts.length == 0) {
                        out.println(">");
                    }
                printDependencyIncludeRules(md, out, includes);
                
                ExcludeRule[] excludes = dds[i].getAllExcludeRules();
                if (excludes.length > 0 && includes.length == 0 && depArtifacts.length == 0) {
                     out.println(">");
                }
                printDependencyExcludeRules(md, out, excludes);
                if (includes.length + excludes.length + depArtifacts.length == 0) {
                    out.println("/>");
                } else {
                    out.println("\t\t</dependency>");
                }
            }
            printAllExcludes(md, out);
        }
    }

    private static void printAllExcludes(ModuleDescriptor md, PrintWriter out) {
        ExcludeRule[] excludes = md.getAllExcludeRules();
        if (excludes.length > 0) {
            for (int j = 0; j < excludes.length; j++) {
                out.print("\t\t<exclude");
                out.print(" org=\""
                        + XMLHelper.escape(excludes[j].getId().getModuleId().getOrganisation()) 
                        + "\"");
                out.print(" module=\"" 
                    + XMLHelper.escape(excludes[j].getId().getModuleId().getName())
                        + "\"");
                out.print(" artifact=\"" + XMLHelper.escape(excludes[j].getId().getName()) + "\"");
                out.print(" type=\"" + XMLHelper.escape(excludes[j].getId().getType()) + "\"");
                out.print(" ext=\"" + XMLHelper.escape(excludes[j].getId().getExt()) + "\"");
                String[] ruleConfs = excludes[j].getConfigurations();
                if (!Arrays.asList(ruleConfs).equals(
                    Arrays.asList(md.getConfigurationsNames()))) {
                    out.print(" conf=\"");
                    for (int k = 0; k < ruleConfs.length; k++) {
                        out.print(XMLHelper.escape(ruleConfs[k]));
                        if (k + 1 < ruleConfs.length) {
                            out.print(",");
                        }
                    }
                    out.print("\"");
                }
                out.print(" matcher=\"" 
                    + XMLHelper.escape(excludes[j].getMatcher().getName()) + "\"");
                out.println("/>");
            }
        }
        out.println("\t</dependencies>");
    }

    private static void printDependencyExcludeRules(ModuleDescriptor md, PrintWriter out,
            ExcludeRule[] excludes) {
        if (excludes.length > 0) {
            for (int j = 0; j < excludes.length; j++) {
                out.print("\t\t\t<exclude");
                out.print(" org=\""
                        + XMLHelper.escape(excludes[j].getId().getModuleId().getOrganisation()) 
                        + "\"");
                out.print(" module=\"" 
                    + XMLHelper.escape(excludes[j].getId().getModuleId().getName())
                        + "\"");
                out.print(" name=\"" + XMLHelper.escape(excludes[j].getId().getName()) + "\"");
                out.print(" type=\"" + XMLHelper.escape(excludes[j].getId().getType()) + "\"");
                out.print(" ext=\"" + XMLHelper.escape(excludes[j].getId().getExt()) + "\"");
                String[] ruleConfs = excludes[j].getConfigurations();
                if (!Arrays.asList(ruleConfs).equals(
                    Arrays.asList(md.getConfigurationsNames()))) {
                    out.print(" conf=\"");
                    for (int k = 0; k < ruleConfs.length; k++) {
                        out.print(XMLHelper.escape(ruleConfs[k]));
                        if (k + 1 < ruleConfs.length) {
                            out.print(",");
                        }
                    }
                    out.print("\"");
                }
                out.print(" matcher=\"" 
                    + XMLHelper.escape(excludes[j].getMatcher().getName()) + "\"");
                out.println("/>");
            }
        }
    }

    private static void printDependencyIncludeRules(ModuleDescriptor md, PrintWriter out,
            IncludeRule[] includes) {
        if (includes.length > 0) {
            for (int j = 0; j < includes.length; j++) {
                out.print("\t\t\t<include");
                out.print(" name=\"" + XMLHelper.escape(includes[j].getId().getName()) + "\"");
                out.print(" type=\"" + XMLHelper.escape(includes[j].getId().getType()) + "\"");
                out.print(" ext=\"" + XMLHelper.escape(includes[j].getId().getExt()) + "\"");
                String[] ruleConfs = includes[j].getConfigurations();
                if (!Arrays.asList(ruleConfs).equals(
                    Arrays.asList(md.getConfigurationsNames()))) {
                    out.print(" conf=\"");
                    for (int k = 0; k < ruleConfs.length; k++) {
                        out.print(XMLHelper.escape(ruleConfs[k]));
                        if (k + 1 < ruleConfs.length) {
                            out.print(",");
                        }
                    }
                    out.print("\"");
                }
                out.print(" matcher=\"" 
                    + XMLHelper.escape(includes[j].getMatcher().getName()) + "\"");
                out.println("/>");
            }
        }
    }

    private static void printDependencyArtefacts(ModuleDescriptor md, PrintWriter out, 
            DependencyArtifactDescriptor[] depArtifacts) {
        if (depArtifacts.length > 0) {
            for (int j = 0; j < depArtifacts.length; j++) {
                out.print("\t\t\t<artifact");
                out.print(" name=\"" + XMLHelper.escape(depArtifacts[j].getName()) + "\"");
                out.print(" type=\"" + XMLHelper.escape(depArtifacts[j].getType()) + "\"");
                out.print(" ext=\"" + XMLHelper.escape(depArtifacts[j].getExt()) + "\"");
                String[] dadconfs = depArtifacts[j].getConfigurations();
                if (!Arrays.asList(dadconfs).equals(
                    Arrays.asList(md.getConfigurationsNames()))) {
                    out.print(" conf=\"");
                    for (int k = 0; k < dadconfs.length; k++) {
                        out.print(XMLHelper.escape(dadconfs[k]));
                        if (k + 1 < dadconfs.length) {
                            out.print(",");
                        }
                    }
                    out.print("\"");
                }
                printExtraAttributes(depArtifacts[j], out, " ");
                out.println("/>");
            }
        }
    }

    /**
     * Writes the extra attributes of the given {@link ExtendableItem} to the
     * given <tt>PrintWriter</tt>.
     * 
     * @param item the {@link ExtendableItem}, cannot be <tt>null</tt>
     * @param out the writer to use
     * @param prefix the string to write before writing the attributes (if any)
     */
    private static void printExtraAttributes(ExtendableItem item, PrintWriter out, String prefix) {
        printExtraAttributes(item.getQualifiedExtraAttributes(), out, prefix);
    }

    /**
     * Writes the specified <tt>Map</tt> containing the extra attributes to the
     * given <tt>PrintWriter</tt>.
     * 
     * @param extra the extra attributes, can be <tt>null</tt>
     * @param out the writer to use
     * @param prefix the string to write before writing the attributes (if any)
     */
    private static void printExtraAttributes(Map extra, PrintWriter out, String prefix) {
        if (extra == null) {
            return;
        }
        
        String delim = prefix;
        for (Iterator iter = extra.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry) iter.next();
            out.print(delim + entry.getKey() + "=\"" 
                + XMLHelper.escape(entry.getValue().toString()) + "\"");
            delim = " ";
        }
    }

    private static void printPublications(ModuleDescriptor md, PrintWriter out) {
        out.println("\t<publications>");
        Artifact[] artifacts = md.getAllArtifacts();
        for (int i = 0; i < artifacts.length; i++) {
            out.print("\t\t<artifact");
            out.print(" name=\"" + XMLHelper.escape(artifacts[i].getName()) + "\"");
            out.print(" type=\"" + XMLHelper.escape(artifacts[i].getType()) + "\"");
            out.print(" ext=\"" + XMLHelper.escape(artifacts[i].getExt()) + "\"");
            out.print(" conf=\"" + XMLHelper.escape(getConfs(md, artifacts[i])) + "\"");
            printExtraAttributes(artifacts[i], out, " ");
            out.println("/>");
        }
        out.println("\t</publications>");
    }

    private static void printConfigurations(ModuleDescriptor md, PrintWriter out) {
        Configuration[] confs = md.getConfigurations();
        if (confs.length > 0) {
            out.println("\t<configurations>");
            for (int i = 0; i < confs.length; i++) {
                out.print("\t\t<conf");
                out.print(" name=\"" + XMLHelper.escape(confs[i].getName()) + "\"");
                out.print(" visibility=\"" 
                    + XMLHelper.escape(confs[i].getVisibility().toString()) + "\"");
                if (confs[i].getDescription() != null) {
                    out.print(" description=\"" 
                        + XMLHelper.escape(confs[i].getDescription()) + "\"");
                }
                String[] exts = confs[i].getExtends();
                if (exts.length > 0) {
                    out.print(" extends=\"");
                    for (int j = 0; j < exts.length; j++) {
                        out.print(XMLHelper.escape(exts[j]));
                        if (j + 1 < exts.length) {
                            out.print(",");
                        }
                    }
                    out.print("\"");
                }
                if (confs[i].getDeprecated() != null) {
                    out.print(" deprecated=\"" + XMLHelper.escape(confs[i].getDeprecated()) + "\"");
                }
                printExtraAttributes(confs[i], out, " ");
                out.println("/>");
            }
            out.println("\t</configurations>");
        }
    }

    private static void printInfoTag(ModuleDescriptor md, PrintWriter out) {
        out.println("\t<info organisation=\"" 
            + XMLHelper.escape(md.getModuleRevisionId().getOrganisation())
                + "\"");
        out.println("\t\tmodule=\"" + XMLHelper.escape(md.getModuleRevisionId().getName()) + "\"");
        String branch = md.getResolvedModuleRevisionId().getBranch();
        if (branch != null) {
            out.println("\t\tbranch=\"" + XMLHelper.escape(branch) + "\"");
        }
        String revision = md.getResolvedModuleRevisionId().getRevision();
        if (revision != null) {
            out.println("\t\trevision=\"" + XMLHelper.escape(revision) + "\"");
        }
        out.println("\t\tstatus=\"" + XMLHelper.escape(md.getStatus()) + "\"");
        out.println("\t\tpublication=\""
                + Ivy.DATE_FORMAT.format(md.getResolvedPublicationDate()) + "\"");
        if (md.isDefault()) {
            out.println("\t\tdefault=\"true\"");
        }
        if (md instanceof DefaultModuleDescriptor) {
            DefaultModuleDescriptor dmd = (DefaultModuleDescriptor) md;
            if (dmd.getNamespace() != null && !dmd.getNamespace().getName().equals("system")) {
                out.println("\t\tnamespace=\"" 
                    + XMLHelper.escape(dmd.getNamespace().getName()) + "\"");
            }
        }
        if (!md.getExtraAttributes().isEmpty()) {
            printExtraAttributes(md, out, "\t\t");
            out.println();
        }
        if (md.getExtraInfo().size()>0) {
            out.println("\t>");
            for (Iterator it = md.getExtraInfo().entrySet().iterator(); it.hasNext();) {
                Map.Entry extraDescr = (Map.Entry) it.next();
                if (extraDescr.getValue()==null || ((String)extraDescr.getValue()).length()==0) {
                    continue;
                }
                out.print("\t\t<");
                out.print(extraDescr.getKey());
                out.print(">");
                out.print(XMLHelper.escape((String) extraDescr.getValue()));
                out.print("</");
                out.print(extraDescr.getKey());
                out.println(">");
            }
            out.println("\t</info>");
        } else {
            out.println("\t/>");            
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
