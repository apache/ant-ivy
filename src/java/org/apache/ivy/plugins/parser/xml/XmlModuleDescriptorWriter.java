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
package org.apache.ivy.plugins.parser.xml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyArtifactDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptorMediator;
import org.apache.ivy.core.module.descriptor.ExcludeRule;
import org.apache.ivy.core.module.descriptor.ExtendsDescriptor;
import org.apache.ivy.core.module.descriptor.ExtraInfoHolder;
import org.apache.ivy.core.module.descriptor.IncludeRule;
import org.apache.ivy.core.module.descriptor.License;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.descriptor.OverrideDependencyDescriptorMediator;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.matcher.MapMatcher;
import org.apache.ivy.util.DateUtil;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.XMLHelper;
import org.apache.ivy.util.extendable.ExtendableItem;

import static org.apache.ivy.util.StringUtils.isNullOrEmpty;
import static org.apache.ivy.util.StringUtils.joinArray;

/**
 *
 */
public final class XmlModuleDescriptorWriter {

    private XmlModuleDescriptorWriter() {
        // Utility class
    }

    public static void write(ModuleDescriptor md, File output) throws IOException {
        write(md, null, output);
    }

    public static void write(ModuleDescriptor md, String licenseHeader, File output)
            throws IOException {
        if (output.getParentFile() != null) {
            output.getParentFile().mkdirs();
        }
        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(output),
                StandardCharsets.UTF_8))) {
            out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            if (licenseHeader != null) {
                out.print(licenseHeader);
            }
            StringBuilder xmlNamespace = new StringBuilder();
            Map<String, String> namespaces = md.getExtraAttributesNamespaces();
            for (Map.Entry<String, String> ns : namespaces.entrySet()) {
                xmlNamespace.append(" xmlns:").append(ns.getKey()).append("=\"")
                        .append(ns.getValue()).append("\"");
            }

            String version = "2.0";
            if (md.getInheritedDescriptors().length > 0) {
                version = "2.2";
            }

            out.println("<ivy-module version=\"" + version + "\"" + xmlNamespace + ">");
            printInfoTag(md, out);
            printConfigurations(md, out);
            printPublications(md, out);
            printDependencies(md, out);
            out.println("</ivy-module>");
        }
    }

    private static void printDependencies(ModuleDescriptor md, PrintWriter out) {
        DependencyDescriptor[] dds = md.getDependencies();
        if (dds.length > 0) {
            out.println("\t<dependencies>");
            for (DependencyDescriptor dep : dds) {
                out.print("\t\t");
                printDependency(md, dep, out);
            }
            printAllExcludes(md, out);
            printAllMediators(md, out);
            out.println("\t</dependencies>");
        }
    }

    protected static void printDependency(ModuleDescriptor md, DependencyDescriptor dep,
            PrintWriter out) {
        final ModuleRevisionId dependencyRevisionId = dep.getDependencyRevisionId();
        out.print(String.format("<dependency org=\"%s\" name=\"%s\"",
                XMLHelper.escape(dependencyRevisionId.getOrganisation()),
                XMLHelper.escape(dependencyRevisionId.getName())));
        if (dependencyRevisionId.getBranch() != null) {
            out.print(" branch=\"" + XMLHelper.escape(dependencyRevisionId.getBranch()) + "\"");
        }
        out.print(" rev=\"" + XMLHelper.escape(dependencyRevisionId.getRevision()) + "\"");
        final ModuleRevisionId dynamicConstraintDependencyRevisionId =
                dep.getDynamicConstraintDependencyRevisionId();
        if (!dynamicConstraintDependencyRevisionId.equals(dependencyRevisionId)) {
            if (dynamicConstraintDependencyRevisionId.getBranch() != null) {
                out.print(" branchConstraint=\""
                        + XMLHelper.escape(dynamicConstraintDependencyRevisionId.getBranch())
                        + "\"");
            }
            out.print(" revConstraint=\""
                    + XMLHelper.escape(dynamicConstraintDependencyRevisionId.getRevision())
                    + "\"");
        }
        if (dep.isForce()) {
            out.print(" force=\"" + dep.isForce() + "\"");
        }
        if (dep.isChanging()) {
            out.print(" changing=\"" + dep.isChanging() + "\"");
        }
        if (!dep.isTransitive()) {
            out.print(" transitive=\"" + dep.isTransitive() + "\"");
        }
        StringBuilder sb = new StringBuilder();
        for (String modConf : dep.getModuleConfigurations()) {
            if (sb.length() > 0) {
                sb.append(";");
            }
            sb.append(XMLHelper.escape(modConf)).append(
                    listToPrefixedString(dep.getDependencyConfigurations(modConf), "->"));
        }
        out.print(" conf=\"" + sb + "\"");

        printExtraAttributes(dep, out, " ");

        DependencyArtifactDescriptor[] depArtifacts = dep.getAllDependencyArtifacts();
        if (depArtifacts.length > 0) {
            out.println(">");
        }
        printDependencyArtefacts(md, out, depArtifacts);

        IncludeRule[] includes = dep.getAllIncludeRules();
        if (includes.length > 0 && depArtifacts.length == 0) {
            out.println(">");
        }
        printDependencyIncludeRules(md, out, includes);

        ExcludeRule[] excludes = dep.getAllExcludeRules();
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

    private static void printAllMediators(ModuleDescriptor md, PrintWriter out) {
        Map<MapMatcher, DependencyDescriptorMediator> mediators = md
                .getAllDependencyDescriptorMediators().getAllRules();

        for (Map.Entry<MapMatcher, DependencyDescriptorMediator> mediatorRule : mediators.entrySet()) {
            MapMatcher matcher = mediatorRule.getKey();
            DependencyDescriptorMediator mediator = mediatorRule.getValue();

            if (mediator instanceof OverrideDependencyDescriptorMediator) {
                OverrideDependencyDescriptorMediator oddm = (OverrideDependencyDescriptorMediator) mediator;

                out.print(String.format("\t\t<override org=\"%s\" module=\"%s\" matcher=\"%s\"",
                        XMLHelper.escape(matcher.getAttributes().get(IvyPatternHelper.ORGANISATION_KEY)),
                        XMLHelper.escape(matcher.getAttributes().get(IvyPatternHelper.MODULE_KEY)),
                        XMLHelper.escape(matcher.getPatternMatcher().getName())
                ));
                if (oddm.getBranch() != null) {
                    out.print(" branch=\"" + XMLHelper.escape(oddm.getBranch()) + "\"");
                }
                if (oddm.getVersion() != null) {
                    out.print(" rev=\"" + XMLHelper.escape(oddm.getVersion()) + "\"");
                }
                out.println("/>");
            } else {
                Message.verbose("ignoring unhandled DependencyDescriptorMediator: "
                        + mediator.getClass());
            }
        }
    }

    private static void printAllExcludes(ModuleDescriptor md, PrintWriter out) {
        ExcludeRule[] excludes = md.getAllExcludeRules();
        if (excludes.length > 0) {
            for (ExcludeRule exclude : excludes) {
                out.print(String.format("\t\t<exclude org=\"%s\" module=\"%s\" artifact=\"%s\" type=\"%s\" ext=\"%s\"",
                        XMLHelper.escape(exclude.getId().getModuleId().getOrganisation()),
                        XMLHelper.escape(exclude.getId().getModuleId().getName()),
                        XMLHelper.escape(exclude.getId().getName()),
                        XMLHelper.escape(exclude.getId().getType()),
                        XMLHelper.escape(exclude.getId().getExt())));
                String[] ruleConfs = exclude.getConfigurations();
                if (!Arrays.asList(ruleConfs).equals(Arrays.asList(md.getConfigurationsNames()))) {
                    out.print(listToPrefixedString(ruleConfs, " conf=\""));
                }
                out.print(" matcher=\"" + XMLHelper.escape(exclude.getMatcher().getName()) + "\"");
                out.println("/>");
            }
        }
    }

    private static void printDependencyExcludeRules(ModuleDescriptor md, PrintWriter out,
            ExcludeRule[] excludes) {
        if (excludes.length > 0) {
            for (ExcludeRule exclude : excludes) {
                out.print(String.format("\t\t\t<exclude org=\"%s\" module=\"%s\" name=\"%s\" type=\"%s\" ext=\"%s\"",
                        XMLHelper.escape(exclude.getId().getModuleId().getOrganisation()),
                        XMLHelper.escape(exclude.getId().getModuleId().getName()),
                        XMLHelper.escape(exclude.getId().getName()),
                        XMLHelper.escape(exclude.getId().getType()),
                        XMLHelper.escape(exclude.getId().getExt())));
                String[] ruleConfs = exclude.getConfigurations();
                if (!Arrays.asList(ruleConfs).equals(Arrays.asList(md.getConfigurationsNames()))) {
                    out.print(listToPrefixedString(ruleConfs, " conf=\""));
                }
                out.print(" matcher=\"" + XMLHelper.escape(exclude.getMatcher().getName()) + "\"");
                out.println("/>");
            }
        }
    }

    private static void printDependencyIncludeRules(ModuleDescriptor md, PrintWriter out,
            IncludeRule[] includes) {
        if (includes.length > 0) {
            for (IncludeRule include : includes) {
                out.print(String.format("\t\t\t<include name=\"%s\" type=\"%s\" ext=\"%s\"",
                        XMLHelper.escape(include.getId().getName()),
                        XMLHelper.escape(include.getId().getType()),
                        XMLHelper.escape(include.getId().getExt())));
                String[] ruleConfs = include.getConfigurations();
                if (!Arrays.asList(ruleConfs).equals(Arrays.asList(md.getConfigurationsNames()))) {
                    out.print(listToPrefixedString(ruleConfs, " conf=\""));
                }
                out.print(" matcher=\"" + XMLHelper.escape(include.getMatcher().getName()) + "\"");
                out.println("/>");
            }
        }
    }

    private static void printDependencyArtefacts(ModuleDescriptor md, PrintWriter out,
            DependencyArtifactDescriptor[] depArtifacts) {
        if (depArtifacts.length > 0) {
            for (DependencyArtifactDescriptor depArtifact : depArtifacts) {
                out.print(String.format("\t\t\t<artifact name=\"%s\" type=\"%s\" ext=\"%s\"",
                        XMLHelper.escape(depArtifact.getName()),
                        XMLHelper.escape(depArtifact.getType()),
                        XMLHelper.escape(depArtifact.getExt())));
                final String[] dadConfs = depArtifact.getConfigurations();
                if (dadConfs != null && dadConfs.length > 0) {
                    if (!Arrays.asList(dadConfs).equals(Arrays.asList(md.getConfigurationsNames()))) {
                        out.print(listToPrefixedString(dadConfs, " conf=\""));
                    }
                }
                printExtraAttributes(depArtifact, out, " ");
                out.println("/>");
            }
        }
    }

    private static String listToPrefixedString(String[] confs, String prefix) {
        StringBuilder sb = new StringBuilder(prefix);
        for (String conf : confs) {
            if (sb.length() > prefix.length()) {
                sb.append(",");
            }
            sb.append(XMLHelper.escape(conf));
        }
        if (prefix.endsWith("\"")) {
            sb.append("\"");
        }
        return sb.toString();
    }

    /**
     * Writes the extra attributes of the given {@link ExtendableItem} to the given
     * <tt>PrintWriter</tt>.
     *
     * @param item
     *            the {@link ExtendableItem}, cannot be <tt>null</tt>
     * @param out
     *            the writer to use
     * @param prefix
     *            the string to write before writing the attributes (if any)
     */
    private static void printExtraAttributes(ExtendableItem item, PrintWriter out, String prefix) {
        printExtraAttributes(item.getQualifiedExtraAttributes(), out, prefix);
    }

    /**
     * Writes the specified <tt>Map</tt> containing the extra attributes to the given
     * <tt>PrintWriter</tt>.
     *
     * @param extra
     *            the extra attributes, can be <tt>null</tt>
     * @param out
     *            the writer to use
     * @param prefix
     *            the string to write before writing the attributes (if any)
     */
    private static void printExtraAttributes(Map<String, String> extra, PrintWriter out, String prefix) {
        if (extra == null) {
            return;
        }

        String delim = prefix;
        for (Map.Entry<String, String> entry : extra.entrySet()) {
            out.print(String.format("%s%s=\"%s\"", delim, entry.getKey(),
                    XMLHelper.escape(entry.getValue())));
            delim = " ";
        }
    }

    private static void printPublications(ModuleDescriptor md, PrintWriter out) {
        out.println("\t<publications>");
        for (Artifact artifact : md.getAllArtifacts()) {
            out.print(String.format("\t\t<artifact name=\"%s\" type=\"%s\" ext=\"%s\" conf=\"%s\"",
                    XMLHelper.escape(artifact.getName()),
                    XMLHelper.escape(artifact.getType()),
                    XMLHelper.escape(artifact.getExt()),
                    XMLHelper.escape(getConfs(md, artifact))));
            printExtraAttributes(artifact, out, " ");
            out.println("/>");
        }
        out.println("\t</publications>");
    }

    private static void printConfigurations(ModuleDescriptor md, PrintWriter out) {
        Configuration[] confs = md.getConfigurations();
        if (confs.length > 0) {
            out.println("\t<configurations>");
            for (Configuration conf : confs) {
                out.print("\t\t");
                printConfiguration(conf, out);
            }
            out.println("\t</configurations>");
        }
    }

    protected static void printConfiguration(Configuration conf, PrintWriter out) {
        out.print("<conf");
        out.print(" name=\"" + XMLHelper.escape(conf.getName()) + "\"");
        out.print(" visibility=\"" + XMLHelper.escape(conf.getVisibility().toString()) + "\"");
        if (conf.getDescription() != null) {
            out.print(" description=\"" + XMLHelper.escape(conf.getDescription()) + "\"");
        }
        String[] exts = conf.getExtends();
        if (exts.length > 0) {
            out.print(listToPrefixedString(exts, " extends=\""));
        }
        if (!conf.isTransitive()) {
            out.print(" transitive=\"false\"");
        }
        if (conf.getDeprecated() != null) {
            out.print(" deprecated=\"" + XMLHelper.escape(conf.getDeprecated()) + "\"");
        }
        printExtraAttributes(conf, out, " ");
        out.println("/>");
    }

    private static void printInfoTag(ModuleDescriptor md, PrintWriter out) {
        out.println("\t<info organisation=\""
                + XMLHelper.escape(md.getModuleRevisionId().getOrganisation()) + "\"");
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
        out.println("\t\tpublication=\"" + DateUtil.format(md.getResolvedPublicationDate()) + "\"");
        if (md.isDefault()) {
            out.println("\t\tdefault=\"true\"");
        }
        if (md instanceof DefaultModuleDescriptor) {
            DefaultModuleDescriptor dmd = (DefaultModuleDescriptor) md;
            if (dmd.getNamespace() != null && !dmd.getNamespace().getName().equals("system")) {
                out.println("\t\tnamespace=\"" + XMLHelper.escape(dmd.getNamespace().getName())
                        + "\"");
            }
        }
        if (!md.getExtraAttributes().isEmpty()) {
            printExtraAttributes(md, out, "\t\t");
            out.println();
        }
        if (requireInnerInfoElement(md)) {
            out.println("\t>");
            for (ExtendsDescriptor parent : md.getInheritedDescriptors()) {
                ModuleRevisionId mrid = parent.getParentRevisionId();
                out.print(String.format("\t\t<extends organisation=\"%s\" module=\"%s\" revision=\"%s\"",
                        XMLHelper.escape(mrid.getOrganisation()),
                        XMLHelper.escape(mrid.getName()),
                        XMLHelper.escape(mrid.getRevision())));

                String location = parent.getLocation();
                if (location != null) {
                    out.print(" location=\"" + XMLHelper.escape(location) + "\"");
                }
                out.print(" extendType=\"" + joinArray(parent.getExtendsTypes(), ",") + "\"");
                out.println("/>");
            }
            License[] licenses = md.getLicenses();
            for (License license : licenses) {
                out.print("\t\t<license ");
                if (license.getName() != null) {
                    out.print("name=\"" + XMLHelper.escape(license.getName()) + "\" ");
                }
                if (license.getUrl() != null) {
                    out.print("url=\"" + XMLHelper.escape(license.getUrl()) + "\" ");
                }
                out.println("/>");
            }
            if (md.getHomePage() != null || md.getDescription() != null) {
                out.print("\t\t<description");
                if (md.getHomePage() != null) {
                    out.print(" homepage=\"" + XMLHelper.escape(md.getHomePage()) + "\"");
                }
                if (isNullOrEmpty(md.getDescription())) {
                    out.println(" />");
                } else {
                    out.println(">");
                    out.println("\t\t" + XMLHelper.escape(md.getDescription()));
                    out.println("\t\t</description>");
                }
            }
            for (ExtraInfoHolder extraInfo : md.getExtraInfos()) {
                printExtraInfoElement(out, extraInfo, 2);
            }
            out.println("\t</info>");
        } else {
            out.println("\t/>");
        }

    }

    private static void printExtraInfoElement(PrintWriter out, ExtraInfoHolder extraInfo, int indent) {
        for (int i = 1; i <= indent; i++) {
            out.print("\t");
        }
        out.print("<");
        out.print(extraInfo.getName());
        for (Map.Entry<String, String> entry : extraInfo.getAttributes().entrySet()) {
            out.print(String.format(" %s=\"%s\"", entry.getKey(), entry.getValue()));
        }
        boolean requireClosingTag = false;
        if (!isNullOrEmpty(extraInfo.getContent())) {
            out.print(">");
            out.print(XMLHelper.escape(extraInfo.getContent()));
            requireClosingTag = true;
        }
        if (!extraInfo.getNestedExtraInfoHolder().isEmpty()) {
            out.println(">");
            for (ExtraInfoHolder nestedElement : extraInfo.getNestedExtraInfoHolder()) {
                printExtraInfoElement(out, nestedElement, indent + 1);
            }
            requireClosingTag = true;
            // prepare indentation for closing tag
            for (int i = 1; i <= indent; i++) {
                out.print("\t");
            }
        }
        if (requireClosingTag) {
            out.print("</");
            out.print(extraInfo.getName());
            out.println(">");
        } else {
            out.println("/>");
        }
    }

    private static boolean requireInnerInfoElement(ModuleDescriptor md) {
        return md.getExtraInfos().size() > 0 || md.getHomePage() != null
                || !isNullOrEmpty(md.getDescription())
                || md.getLicenses().length > 0 || md.getInheritedDescriptors().length > 0;
    }

    private static String getConfs(ModuleDescriptor md, Artifact artifact) {
        StringBuilder ret = new StringBuilder();
        for (String conf : md.getConfigurationsNames()) {
            if (Arrays.asList(md.getArtifacts(conf)).contains(artifact)) {
                ret.append(conf).append(",");
            }
        }
        if (ret.length() > 0) {
            ret.setLength(ret.length() - 1);
        }
        return ret.toString();
    }
}
