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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.StringTokenizer;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.namespace.Namespace;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.plugins.repository.file.FileResource;
import org.apache.ivy.plugins.repository.url.URLResource;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.XMLHelper;
import org.apache.ivy.util.extendable.ExtendableItemHelper;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Used to update ivy files. Uses ivy file as source and not ModuleDescriptor to preserve as much as
 * possible the original syntax
 */
public class XmlModuleDescriptorUpdater {
    private static final int MAX_HEADER_LENGTH = 10000;
    public static String LINE_SEPARATOR = System.getProperty("line.separator");

    /**
     * used to copy a module descriptor xml file (also known as ivy file) and update the revisions
     * of its dependencies, its status and revision
     * 
     * @param srcURL
     *            the url of the source module descriptor file
     * @param destFile
     *            The file to which the updated module descriptor should be output
     * @param resolvedRevisions
     *            Map from ModuleId of dependencies to new revision (as String)
     * @param status
     *            the new status, null to keep the old one
     * @param revision
     *            the new revision, null to keep the old one
     */
    public static void update(URL srcURL, File destFile, final Map resolvedRevisions,
            final String status, final String revision, final Date pubdate, String[] confsToExclude)
            throws IOException, SAXException {
        update(null, srcURL, destFile, resolvedRevisions, status, revision, pubdate, null, false,
            confsToExclude);
    }

    public static void update(final IvySettings settings, URL srcURL, File destFile,
            final Map resolvedRevisions, final String status, final String revision,
            final Date pubdate, final Namespace ns, final boolean replaceInclude,
            String[] confsToExclude) throws IOException, SAXException {
        if (destFile.getParentFile() != null) {
            destFile.getParentFile().mkdirs();
        }
        OutputStream destStream = new FileOutputStream(destFile);
        try {
            update(settings, srcURL, destStream, resolvedRevisions, status, revision,
                pubdate, ns, replaceInclude, confsToExclude);
        } finally {
            try {
                destStream.close();
            } catch (IOException e) {
                Message.warn("failed to close a stream : " + e.toString());
            }
        }
    }

    public static void update(final IvySettings settings, URL srcURL, OutputStream destFile,
            final Map resolvedRevisions, final String status, final String revision,
            final Date pubdate, final Namespace ns, final boolean replaceInclude,
            String[] confsToExclude) throws IOException, SAXException {
        InputStream in = srcURL.openStream();
        try {
            update(settings, srcURL, in, destFile, resolvedRevisions, status, revision,
                pubdate, ns, replaceInclude, confsToExclude);
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

    
    public static void update(
            final IvySettings settings, InputStream in, Resource res, 
            File destFile, final Map resolvedRevisions, final String status, final String revision,
            final Date pubdate, final Namespace ns, final boolean replaceInclude,
            String[] confsToExclude) throws IOException, SAXException {
        if (destFile.getParentFile() != null) {
            destFile.getParentFile().mkdirs();
        }
        OutputStream fos = new FileOutputStream(destFile);
        try {
            //TODO: use resource as input stream context?
            URL inputStreamContext = null;
            if (res instanceof URLResource) {
                inputStreamContext = ((URLResource) res).getURL();
            } else if (res instanceof FileResource) {
                inputStreamContext = ((FileResource) res).getFile().toURL();
            }
            update(settings, inputStreamContext, in, fos, resolvedRevisions, status, revision, 
                pubdate, ns, replaceInclude, confsToExclude);
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

        private final IvySettings settings;

        private final PrintWriter out;

        private final Map resolvedRevisions;

        private final String status;

        private final String revision;

        private final Date pubdate;

        private final Namespace ns;

        private final boolean replaceInclude;

        private boolean inHeader = true;

        private final List confs;

        private final URL relativePathCtx;

        public UpdaterHandler(final IvySettings settings, final PrintWriter out,
                final Map resolvedRevisions, final String status, final String revision,
                final Date pubdate, final Namespace ns, final boolean replaceInclude,
                final String[] confs, final URL relativePathCtx) {
            this.settings = settings;
            this.out = out;
            this.resolvedRevisions = resolvedRevisions;
            this.status = status;
            this.revision = revision;
            this.pubdate = pubdate;
            this.ns = ns;
            this.replaceInclude = replaceInclude;
            this.relativePathCtx = relativePathCtx;
            if (confs != null) {
                this.confs = Arrays.asList(confs);
            } else {
                this.confs = Collections.EMPTY_LIST;
            }
        }

        // never print *ln* cause \n is found in copied characters stream
        // nor do we need do handle indentation, original one is maintained except for attributes

        private String _organisation = null;

        private String _defaultConfMapping = null; // defaultConfMapping of imported

        // configurations, if any

        private Boolean _confMappingOverride = null; // confMappingOverride of imported

        // configurations, if any

        private String _justOpen = null; // used to know if the last open tag was empty, to

        // adjust termination with /> instead of ></qName>

        private Stack _context = new Stack();

        private Stack _buffers = new Stack();

        private Stack _confAttributeBuffers = new Stack();

        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            inHeader = false;
            if (_justOpen != null) {
                write(">");
            }
            _context.push(qName);
            if ("info".equals(qName)) {
                _organisation = substitute(settings, attributes.getValue("organisation"));
                String module = substitute(settings, attributes.getValue("module"));
                String rev = revision;
                if (rev == null) {
                    rev = substitute(settings, attributes.getValue("revision"));
                }
                ModuleRevisionId localMid = ModuleRevisionId.newInstance(_organisation, module, null,
                    rev, ExtendableItemHelper.getExtraAttributes(attributes,
                        new String[] {"organisation", "module", "revision", "status", "publication", "namespace"}));
                ModuleRevisionId systemMid = ns == null ? localMid : ns.getToSystemTransformer()
                        .transform(localMid);

                write("<info organisation=\"" + systemMid.getOrganisation() + "\" module=\""
                        + systemMid.getName() + "\"");
                if (systemMid.getRevision() != null) {
                    write(" revision=\"" + systemMid.getRevision() + "\"");
                }
                if (status != null) {
                    write(" status=\"" + status + "\"");
                } else {
                    write(" status=\"" + substitute(settings, attributes.getValue("status")) + "\"");
                }
                if (pubdate != null) {
                    write(" publication=\"" + Ivy.DATE_FORMAT.format(pubdate) + "\"");
                } else if (attributes.getValue("publication") != null) {
                    write(" publication=\""
                            + substitute(settings, attributes.getValue("publication")) + "\"");
                }
                Collection stdAtts = Arrays.asList(new String[] {"organisation", "module",
                        "revision", "status", "publication", "namespace"});
                if (attributes.getValue("namespace") != null) {
                    write(" namespace=\"" + substitute(settings, attributes.getValue("namespace"))
                            + "\"");
                }
                for (int i = 0; i < attributes.getLength(); i++) {
                    if (!stdAtts.contains(attributes.getQName(i))) {
                        write(" " + attributes.getQName(i) + "=\""
                                + substitute(settings, attributes.getValue(i)) + "\"");
                    }
                }
            } else if (replaceInclude && "include".equals(qName)
                    && _context.contains("configurations")) {
                //TODO, in the case of !replaceInclude, we should still replace the relative path
                //by an absolute path. 
                final ExtendedBuffer buffer = new ExtendedBuffer(getContext());
                _buffers.push(buffer);
                try {
                    URL url;
                    if (settings != null) {
                        url = settings.getRelativeUrlResolver().getURL(relativePathCtx,
                            settings.substitute(attributes.getValue("file")),
                            settings.substitute(attributes.getValue("url")));
                    } else {
                        //TODO : settings can be null, but I don't why.  
                        //Check if the next code is correct in that case
                        String fileName = attributes.getValue("file");
                        if (fileName == null) {
                            String urlStr = attributes.getValue("url");
                            url = new URL(urlStr);
                        } else {
                            url = new File(fileName).toURL();
                        }
                    }
                    XMLHelper.parse(url, null, new DefaultHandler() {
                        boolean _insideConfigurations = false;

                        boolean _doIndent = false;

                        public void startElement(String uri, String localName, String qName,
                                Attributes attributes) throws SAXException {
                            if ("configurations".equals(qName)) {
                                _insideConfigurations = true;
                                String defaultconf = substitute(settings, attributes
                                        .getValue("defaultconfmapping"));
                                if (defaultconf != null) {
                                    _defaultConfMapping = defaultconf;
                                }
                                String mappingOverride = substitute(settings, attributes
                                        .getValue("confmappingoverride"));
                                if (mappingOverride != null) {
                                    _confMappingOverride = Boolean.valueOf(mappingOverride);
                                }
                            } else if ("conf".equals(qName) && _insideConfigurations) {
                                String confName = substitute(settings, attributes.getValue("name"));
                                if (!confs.contains(confName)) {
                                    buffer.setPrint(true);
                                    if (_doIndent) {
                                        write("/>\n\t\t");
                                    }
                                    String extend = substitute(settings, attributes.getValue("extends"));
                                    if (extend != null) {
                                        for (StringTokenizer tok = new StringTokenizer(extend, ", "); tok
                                                .hasMoreTokens();) {
                                            String current = tok.nextToken();
                                            if (confs.contains(current)) {
                                                throw new IllegalArgumentException(
                                                        "Cannot exclude a configuration which is extended.");
                                            }
                                        }

                                    }

                                    write("<" + qName);
                                    for (int i = 0; i < attributes.getLength(); i++) {
                                        write(" " + attributes.getQName(i) + "=\""
                                                + substitute(settings, attributes.getValue(i))
                                                + "\"");
                                    }
                                    _doIndent = true;
                                }
                            }
                        }

                        public void endElement(String uri, String localName, String name)
                                throws SAXException {
                            if ("configurations".equals(name)) {
                                _insideConfigurations = false;
                            }
                        }
                    });
                } catch (Exception e) {
                    Message.warn("exception occured while importing configurations: "
                            + e.getMessage());
                    throw new SAXException(e);
                }
            } else if ("ivy-module/dependencies/dependency".equals(getContext())) {
                ExtendedBuffer buffer = new ExtendedBuffer(getContext());
                _buffers.push(buffer);
                _confAttributeBuffers.push(buffer);
                buffer.setDefaultPrint(attributes.getValue("conf") == null);
                write("<dependency");
                String org = substitute(settings, attributes.getValue("org"));
                org = org == null ? _organisation : org;
                String module = substitute(settings, attributes.getValue("name"));
                String branch = substitute(settings, attributes.getValue("branch"));
                String revision = substitute(settings, attributes.getValue("rev"));
                ModuleRevisionId localMid = ModuleRevisionId.newInstance(org, module, branch,
                    revision, ExtendableItemHelper.getExtraAttributes(attributes,
                        XmlModuleDescriptorParser.DEPENDENCY_REGULAR_ATTRIBUTES));
                ModuleRevisionId systemMid = ns == null ? localMid : ns.getToSystemTransformer()
                        .transform(localMid);

                for (int i = 0; i < attributes.getLength(); i++) {
                    String attName = attributes.getQName(i);
                    if ("rev".equals(attName)) {
                        String rev = (String) resolvedRevisions.get(systemMid);
                        if (rev != null) {
                            write(" rev=\"" + rev + "\"");
                        } else {
                            write(" rev=\"" + systemMid.getRevision() + "\"");
                        }
                    } else if ("org".equals(attName)) {
                        write(" org=\"" + systemMid.getOrganisation() + "\"");
                    } else if ("name".equals(attName)) {
                        write(" name=\"" + systemMid.getName() + "\"");
                    } else if ("branch".equals(attName)) {
                        write(" branch=\"" + systemMid.getBranch() + "\"");
                    } else if ("conf".equals(attName)) {
                        String oldMapping = substitute(settings, attributes.getValue("conf"));
                        if (oldMapping.length() > 0) {
                            String newMapping = removeConfigurationsFromMapping(oldMapping, confs);
                            if (newMapping.length() > 0) {
                                write(" conf=\"" + newMapping + "\"");
                                ((ExtendedBuffer) _buffers.peek()).setPrint(true);
                            }
                        }
                    } else {
                        write(" " + attName + "=\""
                                + substitute(settings, attributes.getValue(attName)) + "\"");
                    }
                }
            } else if ("dependencies".equals(qName)) {
                // copy
                write("<" + qName);
                for (int i = 0; i < attributes.getLength(); i++) {
                    String attName = attributes.getQName(i);
                    if ("defaultconfmapping".equals(attName)) {
                        String newMapping = removeConfigurationsFromMapping(substitute(settings,
                            attributes.getValue("defaultconfmapping")), confs);
                        if (newMapping.length() > 0) {
                            write(" " + attributes.getQName(i) + "=\"" + newMapping + "\"");
                        }
                    } else {
                        write(" " + attributes.getQName(i) + "=\""
                                + substitute(settings, attributes.getValue(i)) + "\"");
                    }
                }
                // add default conf mapping if needed
                if (_defaultConfMapping != null
                        && attributes.getValue("defaultconfmapping") == null) {
                    String newMapping = removeConfigurationsFromMapping(_defaultConfMapping, confs);
                    if (newMapping.length() > 0) {
                        write(" defaultconfmapping=\"" + newMapping + "\"");
                    }
                }
                // add confmappingoverride if needed
                if (_confMappingOverride != null
                        && attributes.getValue("confmappingoverride") == null) {
                    write(" confmappingoverride=\"" + _confMappingOverride.toString() + "\"");
                }
            } else if ("ivy-module/configurations/conf".equals(getContext())) {
                _buffers.push(new ExtendedBuffer(getContext()));
                String confName = substitute(settings, attributes.getValue("name"));
                if (!confs.contains(confName)) {
                    ((ExtendedBuffer) _buffers.peek()).setPrint(true);
                    String extend = substitute(settings, attributes.getValue("extends"));
                    if (extend != null) {
                        for (StringTokenizer tok = new StringTokenizer(extend, ", "); tok
                                .hasMoreTokens();) {
                            String current = tok.nextToken();
                            if (confs.contains(current)) {
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
            } else if ("ivy-module/publications/artifact/conf".equals(getContext())
                    || "ivy-module/dependencies/dependency/conf".equals(getContext())
                    || "ivy-module/dependencies/dependency/artifact/conf".equals(getContext())) {
                _buffers.push(new ExtendedBuffer(getContext()));
                ((ExtendedBuffer) _confAttributeBuffers.peek()).setDefaultPrint(false);
                String confName = substitute(settings, attributes.getValue("name"));
                if (!confs.contains(confName)) {
                    ((ExtendedBuffer) _confAttributeBuffers.peek()).setPrint(true);
                    ((ExtendedBuffer) _buffers.peek()).setPrint(true);
                    write("<" + qName);
                    for (int i = 0; i < attributes.getLength(); i++) {
                        write(" " + attributes.getQName(i) + "=\""
                                + substitute(settings, attributes.getValue(i)) + "\"");
                    }
                }
            } else if ("ivy-module/publications/artifact".equals(getContext())
                    || "ivy-module/dependencies/dependency/artifact".equals(getContext())) {
                ExtendedBuffer buffer = new ExtendedBuffer(getContext());
                _buffers.push(buffer);
                _confAttributeBuffers.push(buffer);
                write("<" + qName);
                buffer.setDefaultPrint(attributes.getValue("conf") == null);
                for (int i = 0; i < attributes.getLength(); i++) {
                    String attName = attributes.getQName(i);
                    if ("conf".equals(attName)) {
                        String confName = substitute(settings, attributes.getValue("conf"));
                        String newConf = removeConfigurationsFromList(confName, confs);
                        if (newConf.length() > 0) {
                            write(" " + attributes.getQName(i) + "=\"" + newConf + "\"");
                            ((ExtendedBuffer) _buffers.peek()).setPrint(true);
                        }
                    } else {
                        write(" " + attributes.getQName(i) + "=\""
                                + substitute(settings, attributes.getValue(i)) + "\"");
                    }
                }
            } else {
                // copy
                write("<" + qName);
                for (int i = 0; i < attributes.getLength(); i++) {
                    write(" " + attributes.getQName(i) + "=\""
                            + substitute(settings, attributes.getValue(i)) + "\"");
                }
            }
            _justOpen = qName;
            // indent.append("\t");
        }

        private void write(String content) {
            if (_buffers.isEmpty()) {
                out.print(content);
            } else {
                ExtendedBuffer buffer = (ExtendedBuffer) _buffers.peek();
                buffer.getBuffer().append(content);
            }
        }

        private String getContext() {
            StringBuffer buf = new StringBuffer();
            for (Iterator iter = _context.iterator(); iter.hasNext();) {
                String ctx = (String) iter.next();
                buf.append(ctx).append("/");
            }
            if (buf.length() > 0) {
                buf.setLength(buf.length() - 1);
            }
            return buf.toString();
        }

        private String substitute(IvySettings ivy, String value) {
            return ivy == null ? value : ivy.substitute(value);
        }

        private String removeConfigurationsFromMapping(String mapping, List confsToRemove) {
            StringBuffer newMapping = new StringBuffer();
            String mappingSep = "";
            for (StringTokenizer tokenizer = new StringTokenizer(mapping, ";"); tokenizer
                    .hasMoreTokens();) {
                String current = tokenizer.nextToken();
                String[] ops = current.split("->");
                String[] lhs = ops[0].split(",");
                List confsToWrite = new ArrayList();
                for (int j = 0; j < lhs.length; j++) {
                    if (!confs.contains(lhs[j].trim())) {
                        confsToWrite.add(lhs[j]);
                    }
                }
                if (!confsToWrite.isEmpty()) {
                    newMapping.append(mappingSep);

                    String sep = "";
                    for (Iterator it = confsToWrite.iterator(); it.hasNext();) {
                        newMapping.append(sep);
                        newMapping.append(it.next());
                        sep = ",";
                    }
                    if (ops.length == 2) {
                        newMapping.append("->");
                        newMapping.append(ops[1]);
                    }
                    mappingSep = ";";
                }
            }

            return newMapping.toString();
        }

        private String removeConfigurationsFromList(String list, List confsToRemove) {
            StringBuffer newList = new StringBuffer();
            String listSep = "";
            for (StringTokenizer tokenizer = new StringTokenizer(list, ","); tokenizer
                    .hasMoreTokens();) {
                String current = tokenizer.nextToken();
                if (!confsToRemove.contains(current.trim())) {
                    newList.append(listSep);
                    newList.append(current);
                    listSep = ",";
                }
            }

            return newList.toString();
        }

        public void characters(char[] ch, int start, int length) throws SAXException {
            if (_justOpen != null) {
                write(">");
                _justOpen = null;
            }
            write(String.valueOf(ch, start, length));
        }

        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (qName.equals(_justOpen)) {
                write("/>");
            } else {
                write("</" + qName + ">");
            }

            if (!_buffers.isEmpty()) {
                ExtendedBuffer buffer = (ExtendedBuffer) _buffers.peek();
                if (buffer.getContext().equals(getContext())) {
                    _buffers.pop();
                    if (buffer.isPrint()) {
                        write(buffer.getBuffer().toString());
                    }
                }
            }

            if (!_confAttributeBuffers.isEmpty()) {
                ExtendedBuffer buffer = (ExtendedBuffer) _confAttributeBuffers.peek();
                if (buffer.getContext().equals(getContext())) {
                    _confAttributeBuffers.pop();
                }
            }

            _justOpen = null;
            _context.pop();
        }

        public void endDocument() throws SAXException {
            out.print(LINE_SEPARATOR);
            out.flush();
            out.close();
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
            if (!inHeader) {
                StringBuffer comment = new StringBuffer();
                comment.append(ch, start, length);
                write("<!--");
                write(comment.toString());
                write("-->");
            }
        }

        public void endEntity(String name) throws SAXException {
        }

        public void startEntity(String name) throws SAXException {
        }

        public void startDTD(String name, String publicId, String systemId) throws SAXException {
        }

    }

    public static void update(final IvySettings settings, URL inStreamCtx, InputStream inStream,
            OutputStream outStream, final Map resolvedRevisions, final String status,
            final String revision, final Date pubdate, final Namespace ns,
            final boolean replaceInclude, String[] confsToExclude) 
            throws IOException, SAXException {
        final PrintWriter out = new PrintWriter(new OutputStreamWriter(outStream, "UTF-8"));
        final BufferedInputStream in = new BufferedInputStream(inStream);

        in.mark(MAX_HEADER_LENGTH); // assume the header is never larger than 10000 bytes.
        copyHeader(in, out);
        in.reset(); // reposition the stream at the beginning

        try {
            UpdaterHandler updaterHandler = new UpdaterHandler(settings, out, resolvedRevisions,
                    status, revision, pubdate, ns, replaceInclude, confsToExclude, inStreamCtx);
            XMLHelper.parse(in, null, updaterHandler, updaterHandler);
        } catch (ParserConfigurationException e) {
            IllegalStateException ise = new IllegalStateException(
                    "impossible to update Ivy files: parser problem");
            ise.initCause(e);
            throw ise;
        }
    }

    /**
     * Copy xml header from src url ivy file to given printwriter In fact, copies everything before
     * <ivy-module to out, except if <ivy-module is not found, in which case nothing is copied. The
     * prolog <?xml version="..." encoding="...."?> is also replaced by <?xml version="1.0"
     * encoding="UTF-8"?> if it was present.
     * 
     * @param in
     * @param out
     * @throws IOException
     */
    private static void copyHeader(InputStream in, PrintWriter out) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(in));
        String line = r.readLine();
        if (line != null && line.startsWith("<?xml ")) {
            out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            line = line.substring(line.indexOf(">") + 1, line.length());
        }
        for (; line != null; line = r.readLine()) {
            int index = line.indexOf("<ivy-module");
            if (index == -1) {
                out.write(line);
                out.write(LINE_SEPARATOR);
            } else {
                out.write(line.substring(0, index));
                break;
            }
        }
        // r.close();
    }

    private static class ExtendedBuffer {
        private String _context = null;

        private Boolean _print = null;

        private boolean _defaultPrint = false;

        private StringBuffer _buffer = new StringBuffer();

        ExtendedBuffer(String context) {
            this._context = context;
        }

        boolean isPrint() {
            if (_print == null) {
                return _defaultPrint;
            }
            return _print.booleanValue();
        }

        void setPrint(boolean print) {
            this._print = Boolean.valueOf(print);
        }

        void setDefaultPrint(boolean print) {
            this._defaultPrint = print;
        }

        StringBuffer getBuffer() {
            return _buffer;
        }

        String getContext() {
            return _context;
        }
    }
}
