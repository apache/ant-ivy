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
package org.apache.ivy.plugins.report;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.util.extendable.ExtendableItemHelper;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class XmlReportParser {
    private static class SaxXmlReportParser {
        private List _mrids;

        private List _defaultMrids;

        private List _realMrids;

        private List _artifacts;

        private ModuleRevisionId _mRevisionId;

        private File _report;

        SaxXmlReportParser(File report) {
            _artifacts = new ArrayList();
            _mrids = new ArrayList();
            _defaultMrids = new ArrayList();
            _realMrids = new ArrayList();
            _report = report;
        }

        public void parse() throws Exception {
            SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
            saxParser.parse(_report, new DefaultHandler() {
                private String _organisation;

                private String _module;

                private String _branch;

                private String _revision;

                private int _position;

                private Date _pubdate;

                private boolean _skip;

                private ModuleRevisionId _mrid;

                private boolean _default;

                private SortedMap _revisionsMap = new TreeMap(); // Use a TreeMap to order by

                // position (position = key)

                private List _revisionArtifacts = null;

                public void startElement(String uri, String localName, String qName,
                        Attributes attributes) throws SAXException {
                    if ("module".equals(qName)) {
                        _organisation = attributes.getValue("organisation");
                        _module = attributes.getValue("name");
                    } else if ("revision".equals(qName)) {
                        _revisionArtifacts = new ArrayList();
                        _branch = attributes.getValue("branch");
                        _revision = attributes.getValue("name");
                        _default = Boolean.valueOf(attributes.getValue("default")).booleanValue();
                        // retrieve position from file. If no position is found, it may be an old
                        // report generated with a previous version,
                        // in which case, we put it at the last position
                        String pos = attributes.getValue("position");
                        _position = pos == null ? getMaxPos() + 1 : Integer.valueOf(pos).intValue();
                        if (attributes.getValue("error") != null
                                || attributes.getValue("evicted") != null) {
                            _skip = true;
                        } else {
                            _revisionsMap.put(new Integer(_position), _revisionArtifacts);
                            _mrid = ModuleRevisionId.newInstance(_organisation, _module, _branch,
                                _revision, ExtendableItemHelper.getExtraAttributes(attributes,
                                    "extra-"));
                            _mrids.add(_mrid);
                            if (_default) {
                                _defaultMrids.add(_mrid);
                            } else {
                                _realMrids.add(_mrid);
                            }
                            try {
                                _pubdate = Ivy.DATE_FORMAT.parse(attributes.getValue("pubdate"));
                                _skip = false;
                            } catch (ParseException e) {
                                throw new IllegalArgumentException("invalid publication date for "
                                        + _organisation + " " + _module + " " + _revision + ": "
                                        + attributes.getValue("pubdate"));
                            }
                        }
                    } else if ("artifact".equals(qName)) {
                        if (_skip) {
                            return;
                        }
                        String status = attributes.getValue("status");
                        if (status != null && "failed".equals(status)) {
                            return;
                        }
                        String artifactName = attributes.getValue("name");
                        String type = attributes.getValue("type");
                        String ext = attributes.getValue("ext");
                        Artifact artifact = new DefaultArtifact(_mrid, _pubdate, artifactName,
                                type, ext, ExtendableItemHelper.getExtraAttributes(attributes,
                                    "extra-"));
                        _revisionArtifacts.add(artifact);
                    } else if ("info".equals(qName)) {
                        String organisation = attributes.getValue("organisation");
                        String name = attributes.getValue("module");
                        String branch = attributes.getValue("branch");
                        String revision = attributes.getValue("revision");
                        Map extraAttributes = new HashMap();
                        for (int i = 0; i < attributes.getLength(); i++) {
                            String attName = attributes.getQName(i);
                            if (attName.startsWith("extra-")) {
                                String extraAttrName = attName.substring(6);
                                String extraAttrValue = attributes.getValue(i);
                                extraAttributes.put(extraAttrName, extraAttrValue);
                            }
                        }
                        _mRevisionId = ModuleRevisionId.newInstance(organisation, name, branch,
                            revision, extraAttributes);
                    }
                }

                public void endElement(String uri, String localName, String qname)
                        throws SAXException {
                    if ("dependencies".equals(qname)) {
                        // add the artifacts in the correct order
                        for (Iterator it = _revisionsMap.values().iterator(); it.hasNext();) {
                            List artifacts = (List) it.next();
                            _artifacts.addAll(artifacts);
                        }
                    }
                }

                private int getMaxPos() {
                    return _revisionsMap.isEmpty() ? -1 : ((Integer) _revisionsMap.keySet()
                            .toArray()[_revisionsMap.size() - 1]).intValue();
                }
            });
        }

        public List getArtifacts() {
            return _artifacts;
        }

        public List getModuleRevisionIds() {
            return _mrids;
        }

        public List getRealModuleRevisionIds() {
            return _realMrids;
        }

        public ModuleRevisionId getResolvedModule() {
            return _mRevisionId;
        }
    }

    private SaxXmlReportParser parser = null;

    public void parse(File report) throws ParseException {
        if (!report.exists()) {
            throw new IllegalStateException("Report file '" + report.getAbsolutePath()
                    + "' does not exist.");
        }

        parser = new SaxXmlReportParser(report);
        try {
            parser.parse();
        } catch (Exception e) {
            ParseException pe = new ParseException("failed to parse report: " + report + ": "
                    + e.getMessage(), 0);
            pe.initCause(e);
            throw pe;
        }
    }

    public Artifact[] getArtifacts() {
        return (Artifact[]) parser.getArtifacts().toArray(
            new Artifact[parser.getArtifacts().size()]);
    }

    public ModuleRevisionId[] getDependencyRevisionIds() {
        return (ModuleRevisionId[]) parser.getModuleRevisionIds().toArray(
            new ModuleRevisionId[parser.getModuleRevisionIds().size()]);
    }

    public ModuleRevisionId[] getRealDependencyRevisionIds() {
        return (ModuleRevisionId[]) parser.getRealModuleRevisionIds().toArray(
            new ModuleRevisionId[parser.getRealModuleRevisionIds().size()]);
    }

    /**
     * Returns the <tt>ModuleRevisionId</tt> of the resolved module.
     */
    public ModuleRevisionId getResolvedModule() {
        return parser.getResolvedModule();
    }
}
