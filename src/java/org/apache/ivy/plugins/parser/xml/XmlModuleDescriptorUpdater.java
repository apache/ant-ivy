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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ExtendsDescriptor;
import org.apache.ivy.core.module.descriptor.InheritableItem;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.namespace.NameSpaceHelper;
import org.apache.ivy.plugins.namespace.Namespace;
import org.apache.ivy.plugins.parser.ParserSettings;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.plugins.repository.file.FileResource;
import org.apache.ivy.plugins.repository.url.URLResource;
import org.apache.ivy.util.Checks;
import org.apache.ivy.util.DateUtil;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.XMLHelper;
import org.apache.ivy.util.extendable.ExtendableItemHelper;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

import static org.apache.ivy.util.StringUtils.isNullOrEmpty;
import static org.apache.ivy.util.StringUtils.joinArray;
import static org.apache.ivy.util.StringUtils.splitToArray;

/**
 * Used to update ivy files. Uses ivy file as source and not ModuleDescriptor to preserve as much as
 * possible the original syntax
 */
public final class XmlModuleDescriptorUpdater {
    // CheckStyle:StaticVariableName| OFF
    // LINE_SEPARATOR is actually a constant, but we have to modify it for the tests
    public static String LINE_SEPARATOR = System.lineSeparator();

    // CheckStyle:StaticVariableName| ON

    private XmlModuleDescriptorUpdater() {
    }

    /**
     * used to copy a module descriptor xml file (also known as ivy file) and update the revisions
     * of its dependencies, its status and revision
     *
     * @param srcURL
     *            the url of the source module descriptor file
     * @param destFile
     *            The file to which the updated module descriptor should be output
     * @param options
     *            UpdateOptions
     * @throws IOException if something goes wrong
     * @throws SAXException if something goes wrong
     */
    public static void update(URL srcURL, File destFile, UpdateOptions options) throws IOException,
            SAXException {
        if (destFile.getParentFile() != null) {
            destFile.getParentFile().mkdirs();
        }
        OutputStream destStream = new FileOutputStream(destFile);
        try {
            update(srcURL, destStream, options);
        } finally {
            try {
                destStream.close();
            } catch (IOException e) {
                Message.warn("failed to close a stream : " + e.toString());
            }
        }
    }

    public static void update(URL srcURL, OutputStream destFile, UpdateOptions options)
            throws IOException, SAXException {
        InputStream in = srcURL.openStream();
        try {
            update(srcURL, in, destFile, options);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                Message.warn("failed to close a stream : " + e.toString());
            }
            try {
                destFile.close();
            } catch (IOException e) {
                Message.warn("failed to close a stream : " + e.toString());
            }
        }

    }

    public static void update(InputStream in, Resource res, File destFile, UpdateOptions options)
            throws IOException, SAXException {
        if (destFile.getParentFile() != null) {
            destFile.getParentFile().mkdirs();
        }
        OutputStream fos = new FileOutputStream(destFile);
        try {
            // TODO: use resource as input stream context?
            URL inputStreamContext = null;
            if (res instanceof URLResource) {
                inputStreamContext = ((URLResource) res).getURL();
            } else if (res instanceof FileResource) {
                inputStreamContext = ((FileResource) res).getFile().toURI().toURL();
            }
            update(inputStreamContext, in, fos, options);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                Message.warn("failed to close a stream : " + e.toString());
            }
            try {
                fos.close();
            } catch (IOException e) {
                Message.warn("failed to close a stream : " + e.toString());
            }
        }
    }

    private static class UpdaterHandler extends DefaultHandler implements LexicalHandler {

        /** standard attributes of ivy-module/info */
        private static final Collection<String> STD_ATTS = Arrays.asList("organisation",
                "module", "branch", "revision", "status", "publication", "namespace");

        /** elements that may appear inside ivy-module, in expected order */
        private static final List<String> MODULE_ELEMENTS = Arrays.asList("info",
                "configurations", "publications", "dependencies", "conflicts");

        /** element position of "configurations" inside "ivy-module" */
        private static final int CONFIGURATIONS_POSITION = MODULE_ELEMENTS
                .indexOf("configurations");

        /** element position of "dependencies" inside "ivy-module" */
        private static final int DEPENDENCIES_POSITION = MODULE_ELEMENTS.indexOf("dependencies");

        /** elements that may appear inside of ivy-module/info */
        private static final Collection<String> INFO_ELEMENTS = Arrays.asList("extends",
                "ivyauthor", "license", "repository", "description");

        private final ParserSettings settings;

        private final PrintWriter out;

        private final Map<ModuleRevisionId, String> resolvedRevisions;

        private final Map<ModuleRevisionId, String> resolvedBranches;

        private final String status;

        private final String revision;

        private final Date pubdate;

        private final Namespace ns;

        private final boolean replaceInclude;

        private final boolean generateRevConstraint;

        private boolean inHeader = true;

        private final List<String> confs;

        private final URL relativePathCtx;

        private final UpdateOptions options;

        public UpdaterHandler(URL relativePathCtx, PrintWriter out, final UpdateOptions options) {
            this.options = options;
            this.settings = options.getSettings();
            this.out = out;
            this.resolvedRevisions = options.getResolvedRevisions();
            this.resolvedBranches = options.getResolvedBranches();
            this.status = options.getStatus();
            this.revision = options.getRevision();
            this.pubdate = options.getPubdate();
            this.ns = options.getNamespace();
            this.replaceInclude = options.isReplaceInclude();
            this.generateRevConstraint = options.isGenerateRevConstraint();
            this.relativePathCtx = relativePathCtx;
            if (options.getConfsToExclude() != null) {
                this.confs = Arrays.asList(options.getConfsToExclude());
            } else {
                this.confs = Collections.emptyList();
            }
        }

        // never print *ln* cause \n is found in copied characters stream
        // nor do we need do handle indentation, original one is maintained except for attributes

        private String organisation = null;

        // defaultConf of imported configurations, if any
        private String defaultConf = null;

        // defaultConfMapping of imported configurations, if any
        private String defaultConfMapping = null;

        // confMappingOverride of imported configurations, if any
        private Boolean confMappingOverride = null;

        // used to know if the last open tag was empty, to adjust termination
        // with /> instead of ></qName>
        private String justOpen = null;

        // track the size of the left indent, so that inserted elements are formatted
        // like nearby elements.

        // true when we're reading indent whitespace
        private boolean indenting;

        private StringBuilder currentIndent = new StringBuilder();

        private List<String> indentLevels = new ArrayList<>(); // ArrayList<String>

        // true if an ivy-module/info/description element has been found in the published descriptor
        private boolean hasDescription = false;

        // true if merged configurations have been written
        private boolean mergedConfigurations = false;

        // true if merged deps have been written
        private boolean mergedDependencies = false;

        // the new value of the defaultconf attribute on the publications tag
        private String newDefaultConf = null;

        private Stack<String> context = new Stack<>();

        private Stack<ExtendedBuffer> buffers = new Stack<>();

        private Stack<ExtendedBuffer> confAttributeBuffers = new Stack<>();

        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            inHeader = false;
            endIndent();
            if (justOpen != null) {
                write(">");
            }

            flushMergedElementsBefore(qName);

            // according to ivy.xsd, all <dependency> elements must occur before
            // the <exclude>, <override> or <conflict> elements
            if (options.isMerge()
                    && ("exclude".equals(localName) || "override".equals(localName) || "conflict"
                            .equals(localName)) && "ivy-module/dependencies".equals(getContext())) {
                ModuleDescriptor merged = options.getMergedDescriptor();
                writeInheritedDependencies(merged);
                out.println();
                out.print(getIndent());
            }

            context.push(qName);

            String path = getContext();
            if ("info".equals(qName)) {
                infoStarted(attributes);
            } else if (replaceInclude && "include".equals(qName)
                    && context.contains("configurations")) {
                // TODO, in the case of !replaceInclude, we should still replace the relative path
                // by an absolute path.
                includeStarted(attributes);
            } else if ("ivy-module/info/extends".equals(path)) {
                if (options.isMerge()) {
                    ModuleDescriptor mergedDescriptor = options.getMergedDescriptor();
                    for (ExtendsDescriptor inheritedDescriptor : mergedDescriptor.getInheritedDescriptors()) {
                        ModuleDescriptor rprid = inheritedDescriptor.getParentMd();
                        if (rprid instanceof DefaultModuleDescriptor) {
                            DefaultModuleDescriptor defaultModuleDescriptor = (DefaultModuleDescriptor) rprid;
                            if (defaultModuleDescriptor.getDefaultConf() != null) {
                                defaultConf = defaultModuleDescriptor.getDefaultConf();
                            }
                            if (defaultModuleDescriptor.getDefaultConfMapping() != null) {
                                defaultConfMapping = defaultModuleDescriptor.getDefaultConfMapping();
                            }
                            if (defaultModuleDescriptor.isMappingOverride()) {
                                confMappingOverride = Boolean.TRUE;
                            }
                        }
                    }
                }
                startExtends(attributes);
            } else if ("ivy-module/dependencies/dependency".equals(path)) {
                startElementInDependency(attributes);
            } else if ("ivy-module/configurations/conf".equals(path)) {
                startElementInConfigurationsConf(qName, attributes);
            } else if ("dependencies".equals(qName) || "configurations".equals(qName)) {
                startElementWithConfAttributes(qName, attributes);
            } else if ("ivy-module/publications/artifact/conf".equals(path)
                    || "ivy-module/dependencies/dependency/conf".equals(path)
                    || "ivy-module/dependencies/dependency/artifact/conf".equals(path)) {
                buffers.push(new ExtendedBuffer(getContext()));
                confAttributeBuffers.peek().setDefaultPrint(false);
                String confName = substitute(settings, attributes.getValue("name"));
                if (!confs.contains(confName)) {
                    confAttributeBuffers.peek().setPrint(true);
                    buffers.peek().setPrint(true);
                    write("<" + qName);
                    for (int i = 0; i < attributes.getLength(); i++) {
                        write(" " + attributes.getQName(i) + "=\""
                                + substitute(settings, attributes.getValue(i)) + "\"");
                    }
                }
            } else if ("ivy-module/publications/artifact".equals(path)) {
                ExtendedBuffer buffer = new ExtendedBuffer(getContext());
                buffers.push(buffer);
                confAttributeBuffers.push(buffer);
                write("<" + qName);
                buffer.setDefaultPrint(attributes.getValue("conf") == null
                        && (newDefaultConf == null || !newDefaultConf.isEmpty()));
                for (int i = 0; i < attributes.getLength(); i++) {
                    String attName = attributes.getQName(i);
                    if ("conf".equals(attName)) {
                        String confName = substitute(settings, attributes.getValue("conf"));
                        String newConf = removeConfigurationsFromList(confName);
                        if (!newConf.isEmpty()) {
                            write(" " + attName + "=\"" + newConf + "\"");
                            buffers.peek().setPrint(true);
                        }
                    } else {
                        write(" " + attName + "=\""
                                + substitute(settings, attributes.getValue(i)) + "\"");
                    }
                }
            } else if ("ivy-module/dependencies/dependency/artifact".equals(path)) {
                ExtendedBuffer buffer = new ExtendedBuffer(getContext());
                buffers.push(buffer);
                confAttributeBuffers.push(buffer);
                write("<" + qName);
                buffer.setDefaultPrint(attributes.getValue("conf") == null);
                for (int i = 0; i < attributes.getLength(); i++) {
                    String attName = attributes.getQName(i);
                    if ("conf".equals(attName)) {
                        String confName = substitute(settings, attributes.getValue("conf"));
                        String newConf = removeConfigurationsFromList(confName);
                        if (!newConf.isEmpty()) {
                            write(" " + attName + "=\"" + newConf + "\"");
                            buffers.peek().setPrint(true);
                        }
                    } else {
                        write(" " + attName + "=\""
                                + substitute(settings, attributes.getValue(i)) + "\"");
                    }
                }
            } else if ("ivy-module/publications".equals(path)) {
                startPublications(attributes);
            } else {
                if (options.isMerge() && path.startsWith("ivy-module/info")) {
                    ModuleDescriptor merged = options.getMergedDescriptor();
                    if (path.equals("ivy-module/info/description")) {
                        // if the descriptor already contains a description, don't bother printing
                        // the merged version.
                        hasDescription = true;
                    } else if (!INFO_ELEMENTS.contains(qName)) {
                        // according to the XSD, we should write description after all of the other
                        // standard <info> elements but before any extended elements.
                        writeInheritedDescription(merged);
                    }
                }

                // copy
                write("<" + qName);
                for (int i = 0; i < attributes.getLength(); i++) {
                    write(" " + attributes.getQName(i) + "=\""
                            + substitute(settings, attributes.getValue(i)) + "\"");
                }
            }
            justOpen = qName;
            // indent.append("\t");
        }

        private void startExtends(Attributes attributes) {
            // in merge mode, comment out extends element
            if (options.isMerge()) {
                write("<!-- ");
            }
            write("<extends");

            String org = substitute(settings, attributes.getValue("organisation"));
            String module = substitute(settings, attributes.getValue("module"));
            ModuleId parentId = new ModuleId(org, module);

            for (int i = 0; i < attributes.getLength(); i++) {
                String name = attributes.getQName(i);
                String value = null;

                switch (name) {
                    case "organisation":
                        value = org;
                        break;
                    case "module":
                        value = module;
                        break;
                    case "revision":
                        // replace inline revision with resolved parent revision
                        ModuleDescriptor merged = options.getMergedDescriptor();
                        if (merged != null) {
                            for (ExtendsDescriptor parent : merged.getInheritedDescriptors()) {
                                ModuleRevisionId resolvedId = parent.getResolvedParentRevisionId();
                                if (parentId.equals(resolvedId.getModuleId())) {
                                    value = resolvedId.getRevision();
                                    if (value != null) {
                                        break;
                                    }
                                }
                            }
                        }
                        if (value == null) {
                            value = substitute(settings, attributes.getValue(i));
                        }
                        break;
                    default:
                        value = substitute(settings, attributes.getValue(i));
                        break;
                }
                write(" " + name + "=\"" + value + "\"");
            }
        }

        private void startElementInConfigurationsConf(String qName, Attributes attributes) {
            buffers.push(new ExtendedBuffer(getContext()));
            String confName = substitute(settings, attributes.getValue("name"));
            if (!confs.contains(confName)) {
                buffers.peek().setPrint(true);
                String extend = substitute(settings, attributes.getValue("extends"));
                if (extend != null) {
                    for (String tok : splitToArray(extend)) {
                        if (confs.contains(tok)) {
                            throw new IllegalArgumentException(
                                    "Cannot exclude a configuration which is extended.");
                        }
                    }
                }

                write("<" + qName);
                for (int i = 0; i < attributes.getLength(); i++) {
                    write(" " + attributes.getQName(i) + "=\""
                            + substitute(settings, attributes.getValue(i)) + "\"");
                }
            }
        }

        private void startElementWithConfAttributes(String qName, Attributes attributes) {
            // copy
            write("<" + qName);
            for (int i = 0; i < attributes.getLength(); i++) {
                String attName = attributes.getQName(i);
                if ("defaultconf".equals(attName) || "defaultconfmapping".equals(attName)) {
                    String newMapping = removeConfigurationsFromMapping(
                        substitute(settings, attributes.getValue(attName)));
                    if (!newMapping.isEmpty()) {
                        write(" " + attName + "=\"" + newMapping + "\"");
                    }
                } else {
                    write(" " + attName + "=\""
                            + substitute(settings, attributes.getValue(i)) + "\"");
                }
            }
            // add default conf if needed
            if (defaultConf != null && attributes.getValue("defaultconf") == null) {
                String newConf = removeConfigurationsFromMapping(defaultConf);
                if (!newConf.isEmpty()) {
                    write(" defaultconf=\"" + newConf + "\"");
                }
            }
            // add default conf mapping if needed
            if (defaultConfMapping != null && attributes.getValue("defaultconfmapping") == null) {
                String newMapping = removeConfigurationsFromMapping(defaultConfMapping);
                if (!newMapping.isEmpty()) {
                    write(" defaultconfmapping=\"" + newMapping + "\"");
                }
            }
            // add confmappingoverride if needed
            if (confMappingOverride != null && attributes.getValue("confmappingoverride") == null) {
                write(" confmappingoverride=\"" + confMappingOverride.toString() + "\"");
            }
        }

        private void startPublications(Attributes attributes) {
            write("<publications");
            for (int i = 0; i < attributes.getLength(); i++) {
                String attName = attributes.getQName(i);
                if ("defaultconf".equals(attName)) {
                    newDefaultConf = removeConfigurationsFromList(
                        substitute(settings, attributes.getValue("defaultconf")));
                    if (!newDefaultConf.isEmpty()) {
                        write(" " + attName + "=\"" + newDefaultConf + "\"");
                    }
                } else {
                    write(" " + attName + "=\""
                            + substitute(settings, attributes.getValue(i)) + "\"");
                }
            }
        }

        private void startElementInDependency(Attributes attributes) {
            ExtendedBuffer buffer = new ExtendedBuffer(getContext());
            buffers.push(buffer);
            confAttributeBuffers.push(buffer);
            buffer.setDefaultPrint(isNullOrEmpty(attributes.getValue("conf")));
            write("<dependency");
            String org = substitute(settings, attributes.getValue("org"));
            if (org == null) {
                org = organisation;
            }
            String module = substitute(settings, attributes.getValue("name"));
            String branch = substitute(settings, attributes.getValue("branch"));
            String branchConstraint = substitute(settings, attributes.getValue("branchConstraint"));
            if (branchConstraint == null) {
                branchConstraint = branch;
            }

            // look for the branch used in resolved revisions
            if (branch == null) {
                ModuleId mid = ModuleId.newInstance(org, module);
                if (ns != null) {
                    mid = NameSpaceHelper.transform(mid, ns.getToSystemTransformer());
                }
                for (ModuleRevisionId mrid : resolvedRevisions.keySet()) {
                    if (mrid.getModuleId().equals(mid)) {
                        branch = mrid.getBranch();
                        break;
                    }
                }
            }

            String revision = substitute(settings, attributes.getValue("rev"));
            String revisionConstraint = substitute(settings, attributes.getValue("revConstraint"));
            Map<String, String> extraAttributes = ExtendableItemHelper.getExtraAttributes(settings, attributes,
                XmlModuleDescriptorParser.DEPENDENCY_REGULAR_ATTRIBUTES);
            ModuleRevisionId localMrid = ModuleRevisionId.newInstance(org, module, branch,
                revision, extraAttributes);
            ModuleRevisionId systemMrid = (ns == null) ? localMrid : ns.getToSystemTransformer()
                    .transform(localMrid);

            String newBranch = resolvedBranches.get(systemMrid);

            for (int i = 0; i < attributes.getLength(); i++) {
                String attName = attributes.getQName(i);
                switch (attName) {
                    case "org":
                        write(" org=\"" + systemMrid.getOrganisation() + "\"");
                        break;
                    case "name":
                        write(" name=\"" + systemMrid.getName() + "\"");
                        break;
                    case "rev":
                        String rev = resolvedRevisions.get(systemMrid);
                        if (rev == null) {
                            write(" rev=\"" + systemMrid.getRevision() + "\"");
                        } else {
                            write(" rev=\"" + rev + "\"");
                            if (attributes.getIndex("branchConstraint") == -1
                                    && branchConstraint != null) {
                                write(" branchConstraint=\"" + branchConstraint + "\"");
                            }
                            if (generateRevConstraint && attributes.getIndex("revConstraint") == -1
                                    && !rev.equals(systemMrid.getRevision())) {
                                write(" revConstraint=\"" + systemMrid.getRevision() + "\"");
                            }
                        }
                        break;
                    case "revConstraint":
                        write(" revConstraint=\"" + revisionConstraint + "\"");
                        break;
                    case "branch":
                        if (newBranch != null) {
                            write(" branch=\"" + newBranch + "\"");
                        } else if (!resolvedBranches.containsKey(systemMrid)) {
                            write(" branch=\"" + systemMrid.getBranch() + "\"");
                        } else {
                            // if resolvedBranches contains the systemMrid, but the new branch is null,
                            // the branch attribute will be removed altogether
                        }
                        break;
                    case "branchConstraint":
                        write(" branchConstraint=\"" + branchConstraint + "\"");
                        break;
                    case "conf":
                        String oldMapping = substitute(settings, attributes.getValue("conf"));
                        if (!oldMapping.isEmpty()) {
                            String newMapping = removeConfigurationsFromMapping(oldMapping);
                            if (!newMapping.isEmpty()) {
                                write(" conf=\"" + newMapping + "\"");
                                buffers.peek().setPrint(true);
                            }
                        }
                        break;
                    default:
                        write(" " + attName + "=\""
                                + substitute(settings, attributes.getValue(attName)) + "\"");
                        break;
                }
            }

            if (attributes.getIndex("branch") == -1) {
                // erase an existing branch attribute if its new value is blank
                if (!isNullOrEmpty(newBranch)) {
                    write(" branch=\"" + newBranch + "\"");
                } else if (options.isUpdateBranch() && systemMrid.getBranch() != null) {
                    // this dependency is on a specific branch, we set it explicitly in the updated
                    // file
                    write(" branch=\"" + systemMrid.getBranch() + "\"");
                }
            }
        }

        private void includeStarted(Attributes attributes) throws SAXException {
            final ExtendedBuffer buffer = new ExtendedBuffer(getContext());
            buffers.push(buffer);
            try {
                URL url;
                if (settings == null) {
                    // TODO : settings can be null, but I don't why.
                    // Check if the following code is correct in that case
                    String fileName = attributes.getValue("file");
                    if (fileName == null) {
                        String urlStr = attributes.getValue("url");
                        url = new URL(urlStr);
                    } else {
                        url = Checks.checkAbsolute(fileName, "settings.include").toURI().toURL();
                    }
                } else {
                    url = settings.getRelativeUrlResolver().getURL(relativePathCtx,
                        settings.substitute(attributes.getValue("file")),
                        settings.substitute(attributes.getValue("url")));
                }
                XMLHelper.parse(url, null, new DefaultHandler() {
                    private boolean insideConfigurations = false;

                    private boolean doIndent = false;

                    public void startElement(String uri, String localName, String qName,
                            Attributes attributes) throws SAXException {
                        if ("configurations".equals(qName)) {
                            insideConfigurations = true;
                            String defaultconf = substitute(settings,
                                attributes.getValue("defaultconf"));
                            if (defaultconf != null) {
                                defaultConf = defaultconf;
                            }
                            String defaultMapping = substitute(settings,
                                attributes.getValue("defaultconfmapping"));
                            if (defaultMapping != null) {
                                defaultConfMapping = defaultMapping;
                            }
                            String mappingOverride = substitute(settings,
                                attributes.getValue("confmappingoverride"));
                            if (mappingOverride != null) {
                                confMappingOverride = Boolean.valueOf(mappingOverride);
                            }
                        } else if ("conf".equals(qName) && insideConfigurations) {
                            String confName = substitute(settings, attributes.getValue("name"));
                            if (!confs.contains(confName)) {
                                buffer.setPrint(true);
                                if (doIndent) {
                                    write("/>\n\t\t");
                                }
                                String extend = substitute(settings, attributes.getValue("extends"));
                                if (extend != null) {
                                    for (String tok : splitToArray(extend)) {
                                        if (confs.contains(tok)) {
                                            throw new IllegalArgumentException(
                                                    "Cannot exclude a configuration which is extended.");
                                        }
                                    }
                                }

                                write("<" + qName);
                                for (int i = 0; i < attributes.getLength(); i++) {
                                    write(" " + attributes.getQName(i) + "=\""
                                            + substitute(settings, attributes.getValue(i)) + "\"");
                                }
                                doIndent = true;
                            }
                        }
                    }

                    public void endElement(String uri, String localName, String name)
                            throws SAXException {
                        if ("configurations".equals(name)) {
                            insideConfigurations = false;
                        }
                    }
                });
            } catch (Exception e) {
                Message.warn("exception occurred while importing configurations: " + e.getMessage());
                throw new SAXException(e);
            }
        }

        private void infoStarted(Attributes attributes) {

            String module = substitute(settings, attributes.getValue("module"));
            String rev = null;
            String branch = null;
            String status = null;
            String namespace = null;
            Map<String, String> extraAttributes = null;

            if (options.isMerge()) {
                // get attributes from merged descriptor, ignoring raw XML
                ModuleDescriptor merged = options.getMergedDescriptor();
                ModuleRevisionId mergedMrid = merged.getModuleRevisionId();
                organisation = mergedMrid.getOrganisation();
                branch = mergedMrid.getBranch();
                rev = mergedMrid.getRevision();
                status = merged.getStatus();

                // TODO: should namespace be added to ModuleDescriptor interface, so we don't
                // have to do this kind of check?
                if (merged instanceof DefaultModuleDescriptor) {
                    Namespace ns = ((DefaultModuleDescriptor) merged).getNamespace();
                    if (ns != null) {
                        namespace = ns.getName();
                    }
                }
                if (namespace == null) {
                    namespace = attributes.getValue("namespace");
                }

                extraAttributes = merged.getQualifiedExtraAttributes();
            } else {
                // get attributes from raw XML, performing property substitution
                organisation = substitute(settings, attributes.getValue("organisation"));
                rev = substitute(settings, attributes.getValue("revision"));
                branch = substitute(settings, attributes.getValue("branch"));
                status = substitute(settings, attributes.getValue("status"));
                namespace = substitute(settings, attributes.getValue("namespace"));
                extraAttributes = new LinkedHashMap<>(attributes.getLength());
                for (int i = 0; i < attributes.getLength(); i++) {
                    String qname = attributes.getQName(i);
                    if (!STD_ATTS.contains(qname)) {
                        extraAttributes.put(qname, substitute(settings, attributes.getValue(i)));
                    }
                }
            }

            // apply override values provided in options
            if (revision != null) {
                rev = revision;
            }
            if (options.getBranch() != null) {
                branch = options.getBranch();
            }
            if (this.status != null) {
                status = this.status;
            }

            // if necessary translate mrid using optional namespace argument
            ModuleRevisionId localMid = ModuleRevisionId.newInstance(organisation, module, branch,
                rev, ExtendableItemHelper.getExtraAttributes(settings, attributes,
                            Arrays.asList("organisation", "module", "revision", "status",
                                    "publication", "namespace")));
            ModuleRevisionId systemMid = (ns == null) ? localMid : ns.getToSystemTransformer()
                    .transform(localMid);

            write("<info");
            if (organisation != null) {
                write(" organisation=\"" + XMLHelper.escape(systemMid.getOrganisation()) + "\"");
            }
            write(" module=\"" + XMLHelper.escape(systemMid.getName()) + "\"");
            if (branch != null) {
                write(" branch=\"" + XMLHelper.escape(systemMid.getBranch()) + "\"");
            }
            if (systemMid.getRevision() != null) {
                write(" revision=\"" + XMLHelper.escape(systemMid.getRevision()) + "\"");
            }
            write(" status=\"" + XMLHelper.escape(status) + "\"");
            if (pubdate != null) {
                write(" publication=\"" + DateUtil.format(pubdate) + "\"");
            } else if (attributes.getValue("publication") != null) {
                write(" publication=\"" + substitute(settings, attributes.getValue("publication"))
                        + "\"");
            }
            if (namespace != null) {
                write(" namespace=\"" + namespace + "\"");
            }

            for (Map.Entry<String, String> extra : extraAttributes.entrySet()) {
                write(" " + extra.getKey() + "=\"" + extra.getValue() + "\"");
            }
        }

        private void write(String content) {
            getWriter().print(content);
        }

        private PrintWriter getWriter() {
            return buffers.isEmpty() ? out : buffers.peek().getWriter();
        }

        private String getContext() {
            return joinArray(context.toArray(new String[context.size()]), "/");
        }

        private String substitute(ParserSettings ivy, String value) {
            String result = (ivy == null) ? value : ivy.substitute(value);
            return XMLHelper.escape(result);
        }

        private String removeConfigurationsFromMapping(String mapping) {
            StringBuilder newMapping = new StringBuilder();
            String mappingSep = "";
            for (String groups : mapping.trim().split("\\s*;\\s*")) {
                String[] ops = groups.split("->");
                List<String> confsToWrite = new ArrayList<>();
                for (String lh : splitToArray(ops[0])) {
                    if (!confs.contains(lh)) {
                        confsToWrite.add(lh);
                    }
                }
                if (!confsToWrite.isEmpty()) {
                    newMapping.append(mappingSep);
                    String sep = "";
                    String listSep = groups.contains(", ") ? ", " : ",";
                    for (String confToWrite : confsToWrite) {
                        newMapping.append(sep).append(confToWrite);
                        sep = listSep;
                    }
                    if (ops.length == 2) {
                        newMapping.append("->").append(joinArray(splitToArray(ops[1]), sep));
                    }
                    mappingSep = ";";
                }
            }
            return newMapping.toString();
        }

        private String removeConfigurationsFromList(String list) {
            StringBuilder newList = new StringBuilder();
            String sep = "";
            String listSep = list.contains(", ") ? ", " : ",";
            for (String current : splitToArray(list)) {
                if (!confs.contains(current)) {
                    newList.append(sep).append(current);
                    sep = listSep;
                }
            }
            return newList.toString();
        }

        public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
            characters(ch, start, length);
        }

        public void characters(char[] ch, int start, int length) throws SAXException {
            if (justOpen != null) {
                write(">");
                justOpen = null;
            }
            write(XMLHelper.escape(String.valueOf(ch, start, length)));

            // examine characters for current indent level, keeping in mind
            // that our indent might be split across multiple calls to characters()
            for (int i = start, end = start + length; i < end; ++i) {
                char c = ch[i];
                if (c == '\r' || c == '\n') {
                    // newline resets the indent level
                    currentIndent.setLength(0);
                    indenting = true;
                } else if (indenting) {
                    // indent continues until first non-whitespace character
                    if (Character.isWhitespace(c)) {
                        currentIndent.append(c);
                    } else {
                        endIndent();
                    }
                }
            }
        }

        /** record the current indent level for future elements that appear at the same depth */
        private void endIndent() {
            if (indenting) {
                // record the indent at this level. if we insert any elements at
                // this level, we'll use the same indent.
                setIndent(context.size() - 1, currentIndent.toString());
                indenting = false;
            }
        }

        /**
         * Set the indent for the given depth. Indents less than the provided depth will be
         * calculated automatically, if they have not already been defined.
         */
        private void setIndent(int level, String indent) {
            fillIndents(level);
            indentLevels.set(level, indent);
        }

        /**
         * Guarantee that indent levels have been calculated up to and including the given depth
         * (starting at 0).
         */
        private void fillIndents(int level) {
            if (indentLevels.isEmpty()) {
                // add a default single-level indent until we see indents in the document
                indentLevels.add("    ");
            }
            String oneLevel = indentLevels.get(0);
            for (int fill = indentLevels.size(); fill <= level; ++fill) {
                indentLevels.add(indentLevels.get(fill - 1) + oneLevel);
            }
        }

        /**
         * get the whitespace that should precede new elements at the current depth in the document
         */
        private String getIndent() {
            int level = context.size() - 1;
            fillIndents(level);
            return indentLevels.get(level);
        }

        /**
         * Write XML elements that do not appear in the source descriptor, but have been copied in
         * from a parent module descriptor via &lt;extends&gt; declaration.
         *
         * @param merged
         *            child descriptor containing the merged data
         * @param items
         *            the list of inherited items to print
         * @param printer
         *            a printer that knows how to write the given type of item
         * @param itemName
         *            the name of the container element, e.g. "configurations"
         * @param includeContainer
         *            if true, include an enclosing element named <code>itemName</code>. Otherwise
         *            just write the inherited items inline, with a comment indicating where they
         *            came from.
         */
        private void writeInheritedItems(ModuleDescriptor merged, InheritableItem[] items,
                ItemPrinter printer, String itemName, boolean includeContainer) {
            // first categorize inherited items by their source module, so that
            // we can add some useful comments
            PrintWriter out = getWriter();

            Map<ModuleRevisionId, List<InheritableItem>> inheritedItems = collateInheritedItems(
                merged, items);
            boolean hasItems = !inheritedItems.isEmpty();

            if (hasItems && includeContainer) {
                if (currentIndent.length() == 0) {
                    out.print(getIndent());
                }
                String newConf = (defaultConf == null) ? ""
                        : removeConfigurationsFromMapping(defaultConf);
                String newMapping = (defaultConfMapping == null) ? ""
                        : removeConfigurationsFromMapping(defaultConfMapping);
                out.print(String.format("<%s%s%s%s>", itemName,
                        newConf.isEmpty() ? "" : " defaultconf=\"" + newConf + "\"",
                        newMapping.isEmpty() ? "" : " defaultconfmapping=\"" + newMapping + "\"",
                        (confMappingOverride != null) ? " confmappingoverride=\"" + confMappingOverride + "\"" : ""));
                context.push(itemName);
                justOpen = null;
            }

            for (Map.Entry<ModuleRevisionId, List<InheritableItem>> entry : inheritedItems
                    .entrySet()) {
                if (justOpen != null) {
                    out.println(">");
                    justOpen = null; // helps endElement() decide how to write close tags
                }
                writeInheritanceComment(itemName, entry.getKey());
                for (InheritableItem item : entry.getValue()) {
                    out.print(getIndent());
                    printer.print(merged, item, out);
                }
            }

            if (hasItems) {
                if (includeContainer) {
                    context.pop();
                    out.println(getIndent() + "</" + itemName + ">");
                    out.println();
                }
                // restore the prior indent
                out.print(currentIndent);
            }
        }

        private void writeInheritanceComment(String itemDescription, Object parentInfo) {
            PrintWriter out = getWriter();
            out.println();
            out.println(getIndent() + "<!-- " + itemDescription + " inherited from " + parentInfo
                    + " -->");
        }

        /**
         * Collect the given list of inherited descriptor items into lists keyed by parent Id. Thus
         * all of the items inherited from parent A can be written together, then all of the items
         * from parent B, and so on.
         *
         * @param merged
         *            the merged child descriptor
         * @param items
         *            the inherited items to collate
         * @return maps parent ModuleRevisionId to a List of InheritedItems imported from that
         *         parent
         */
        private Map<ModuleRevisionId, List<InheritableItem>> collateInheritedItems(
                ModuleDescriptor merged, InheritableItem[] items) {
            Map<ModuleRevisionId, List<InheritableItem>> inheritedItems = new LinkedHashMap<>();
            for (InheritableItem item : items) {
                ModuleRevisionId source = item.getSourceModule();
                // ignore items that are defined directly in the child descriptor
                if (source != null
                        && !source.getModuleId().equals(merged.getModuleRevisionId().getModuleId())) {
                    List<InheritableItem> accum = inheritedItems.get(source);
                    if (accum == null) {
                        accum = new ArrayList<>();
                        inheritedItems.put(source, accum);
                    }
                    accum.add(item);
                }
            }
            return inheritedItems;
        }

        /**
         * If no info/description element has yet been written, write the description inherited from
         * the parent descriptor, if any. Calling this method more than once has no affect.
         */
        private void writeInheritedDescription(ModuleDescriptor merged) {
            if (!hasDescription) {
                hasDescription = true;
                String description = merged.getDescription();
                if (!isNullOrEmpty(description)) {
                    PrintWriter writer = getWriter();
                    if (justOpen != null) {
                        writer.println(">");
                    }
                    writeInheritanceComment("description", "parent");
                    writer.println(getIndent() + "<description>" + XMLHelper.escape(description)
                            + "</description>");
                    // restore the indent that existed before we wrote the extra elements
                    writer.print(currentIndent);
                    justOpen = null;
                }
            }
        }

        private void writeInheritedConfigurations(ModuleDescriptor merged) {
            if (!mergedConfigurations) {
                mergedConfigurations = true;
                writeInheritedItems(merged, merged.getConfigurations(),
                    ConfigurationPrinter.INSTANCE, "configurations", false);
            }
        }

        private void writeInheritedDependencies(ModuleDescriptor merged) {
            if (!mergedDependencies) {
                mergedDependencies = true;
                writeInheritedItems(merged, merged.getDependencies(), DependencyPrinter.INSTANCE,
                    "dependencies", false);
            }
        }

        /**
         * <p>
         * If publishing in merge mode, guarantee that any merged elements appearing before
         * <code>moduleElement</code> have been written. This method should be called <i>before</i>
         * we write the start tag of <code>moduleElement</code>. This covers cases where merged
         * elements like "configurations" and "dependencies" appear in the parent descriptor, but
         * are completely missing in the child descriptor.
         * </p>
         *
         * <p>
         * For example, if "moduleElement" is "dependencies", guarantees that "configurations" has
         * been written. If <code>moduleElement</code> is <code>null</code>, then all missing merged
         * elements will be flushed.
         * </p>
         *
         * @param moduleElement
         *            a descriptor element name, for example "configurations" or "info"
         */
        private void flushMergedElementsBefore(String moduleElement) {
            if (options.isMerge() && context.size() == 1 && "ivy-module".equals(context.peek())
                    && !(mergedConfigurations && mergedDependencies)) {

                // calculate the position of the element in ivy-module
                int position = (moduleElement == null) ? MODULE_ELEMENTS.size()
                        : MODULE_ELEMENTS.indexOf(moduleElement);

                ModuleDescriptor merged = options.getMergedDescriptor();

                // see if we should write <configurations>
                if (!mergedConfigurations && position > CONFIGURATIONS_POSITION
                        && merged.getConfigurations().length > 0) {

                    mergedConfigurations = true;
                    writeInheritedItems(merged, merged.getConfigurations(),
                        ConfigurationPrinter.INSTANCE, "configurations", true);

                }
                // see if we should write <dependencies>
                if (!mergedDependencies && position > DEPENDENCIES_POSITION
                        && merged.getDependencies().length > 0) {

                    mergedDependencies = true;
                    writeInheritedItems(merged, merged.getDependencies(),
                        DependencyPrinter.INSTANCE, "dependencies", true);

                }
            }
        }

        private void flushAllMergedElements() {
            flushMergedElementsBefore(null);
        }

        public void endElement(String uri, String localName, String qName) throws SAXException {

            String path = getContext();
            if (options.isMerge()) {
                ModuleDescriptor merged = options.getMergedDescriptor();
                switch (path) {
                    case "ivy-module/info":
                        // guarantee that inherited description has been written before
                        // info element closes.
                        writeInheritedDescription(merged);
                        break;
                    case "ivy-module/configurations":
                        // write inherited configurations after all child configurations
                        writeInheritedConfigurations(merged);
                        break;
                    case "ivy-module/dependencies":
                        // write inherited dependencies after all child dependencies
                        writeInheritedDependencies(merged);
                        break;
                    case "ivy-module":
                        // write any remaining inherited data before we close the
                        // descriptor.
                        flushAllMergedElements();
                        break;
                }
            }

            if (qName.equals(justOpen)) {
                write("/>");
            } else {
                write("</" + qName + ">");
            }

            if (!buffers.isEmpty()) {
                ExtendedBuffer buffer = buffers.peek();
                if (buffer.getContext().equals(path)) {
                    buffers.pop();
                    if (buffer.isPrint()) {
                        write(buffer.toString());
                    }
                }
            }

            if (!confAttributeBuffers.isEmpty()) {
                ExtendedBuffer buffer = confAttributeBuffers.peek();
                if (buffer.getContext().equals(path)) {
                    confAttributeBuffers.pop();
                }
            }

            // <extends> element is commented out when in merge mode.
            if (options.isMerge() && "ivy-module/info/extends".equals(path)) {
                write(" -->");
            }

            justOpen = null;
            context.pop();
        }

        public void endDocument() throws SAXException {
            out.print(LINE_SEPARATOR);
            out.flush();
            out.close();
        }

        public void processingInstruction(String target, String data) throws SAXException {
            write("<?");
            write(target);
            write(" ");
            write(data);
            write("?>");
            write(LINE_SEPARATOR);
        }

        public void warning(SAXParseException e) throws SAXException {
            throw e;
        }

        public void error(SAXParseException e) throws SAXException {
            throw e;
        }

        public void fatalError(SAXParseException e) throws SAXException {
            throw e;
        }

        public void endCDATA() throws SAXException {
        }

        public void endDTD() throws SAXException {
        }

        public void startCDATA() throws SAXException {
        }

        public void comment(char[] ch, int start, int length) throws SAXException {
            if (justOpen != null) {
                write(">");
                justOpen = null;
            }

            write("<!--");
            write(String.valueOf(ch, start, length));
            write("-->");

            if (inHeader) {
                write(LINE_SEPARATOR);
            }
        }

        public void endEntity(String name) throws SAXException {
        }

        public void startEntity(String name) throws SAXException {
        }

        public void startDTD(String name, String publicId, String systemId) throws SAXException {
        }

    }

    public static void update(URL inStreamCtx, InputStream inStream, OutputStream outStream,
            final UpdateOptions options) throws IOException, SAXException {
        final PrintWriter out = new PrintWriter(new OutputStreamWriter(outStream, StandardCharsets.UTF_8));
        out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        out.write(LINE_SEPARATOR);

        try {
            UpdaterHandler updaterHandler = new UpdaterHandler(inStreamCtx, out, options);
            InputSource inSrc = new InputSource(new BufferedInputStream(inStream));
            if (inStreamCtx != null) {
                inSrc.setSystemId(inStreamCtx.toExternalForm());
            }
            XMLHelper.parse(inSrc, null, updaterHandler, updaterHandler);
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("impossible to update Ivy files: parser problem", e);
        }
    }

    private static class ExtendedBuffer {
        private String context = null;

        private Boolean print = null;

        private boolean defaultPrint = false;

        private StringWriter buffer = new StringWriter();

        private PrintWriter writer = new PrintWriter(buffer);

        ExtendedBuffer(String context) {
            this.context = context;
        }

        boolean isPrint() {
            if (print == null) {
                return defaultPrint;
            }
            return print;
        }

        void setPrint(boolean print) {
            this.print = print;
        }

        void setDefaultPrint(boolean print) {
            this.defaultPrint = print;
        }

        PrintWriter getWriter() {
            return writer;
        }

        String getContext() {
            return context;
        }

        public String toString() {
            writer.flush();
            return buffer.toString();
        }
    }

    /**
     * Prints a descriptor item's XML representation
     */
    protected interface ItemPrinter {
        /**
         * Print an XML representation of <code>item</code> to <code>out</code>.
         *
         * @param parent
         *            the module descriptor containing <code>item</code>
         * @param item
         *            subcomponent of the descriptor, for example a {@link DependencyDescriptor} or
         *            {@link Configuration}
         * @param out PrintWriter
         */
        void print(ModuleDescriptor parent, Object item, PrintWriter out);
    }

    protected static class DependencyPrinter implements ItemPrinter {

        public static final DependencyPrinter INSTANCE = new DependencyPrinter();

        public void print(ModuleDescriptor parent, Object item, PrintWriter out) {
            XmlModuleDescriptorWriter.printDependency(parent, (DependencyDescriptor) item, out);
        }
    }

    protected static class ConfigurationPrinter implements ItemPrinter {

        public static final ConfigurationPrinter INSTANCE = new ConfigurationPrinter();

        public void print(ModuleDescriptor parent, Object item, PrintWriter out) {
            XmlModuleDescriptorWriter.printConfiguration((Configuration) item, out);
        }
    }
}
