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
import org.apache.ivy.core.cache.ArtifactOrigin;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.DownloadStatus;
import org.apache.ivy.core.report.MetadataArtifactDownloadReport;
import org.apache.ivy.util.extendable.ExtendableItemHelper;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class XmlReportParser {
    private static class SaxXmlReportParser {
        private List/*<ModuleRevisionId>*/ mrids;

        private List/*<ModuleRevisionId>*/ defaultMrids;

        private List/*<ModuleRevisionId>*/ realMrids;

        private List/*<Artifact>*/ artifacts;

        private List/*<ArtifactDownloadReport>*/ artifactReports;
        
        private Map/*<ModuleRevisionId,MetadataArtifactDownloadReport>*/ metadataReports;

        private ModuleRevisionId mRevisionId;

        private File report;

        SaxXmlReportParser(File report) {
            artifacts = new ArrayList();
            artifactReports = new ArrayList();
            mrids = new ArrayList();
            defaultMrids = new ArrayList();
            realMrids = new ArrayList();
            metadataReports = new HashMap();
            this.report = report;
        }

        public void parse() throws Exception {
            SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
            saxParser.parse(report, new DefaultHandler() {
                private String organisation;

                private String module;

                private String branch;

                private String revision;

                private int position;

                private Date pubdate;

                private boolean skip;

                private ModuleRevisionId mrid;

                private boolean isDefault;

                private SortedMap revisionsMap = new TreeMap(); // Use a TreeMap to order by

                // position (position = key)

                private List revisionArtifacts = null;

                public void startElement(String uri, String localName, String qName,
                        Attributes attributes) throws SAXException {
                    if ("module".equals(qName)) {
                        organisation = attributes.getValue("organisation");
                        module = attributes.getValue("name");
                    } else if ("revision".equals(qName)) {
                        revisionArtifacts = new ArrayList();
                        branch = attributes.getValue("branch");
                        revision = attributes.getValue("name");
                        isDefault = Boolean.valueOf(attributes.getValue("default")).booleanValue();
                        // retrieve position from file. If no position is found, it may be an old
                        // report generated with a previous version,
                        // in which case, we put it at the last position
                        String pos = attributes.getValue("position");
                        position = pos == null ? getMaxPos() + 1 : Integer.valueOf(pos).intValue();
                        if (attributes.getValue("error") != null
                                || attributes.getValue("evicted") != null) {
                            skip = true;
                        } else {
                            revisionsMap.put(new Integer(position), revisionArtifacts);
                            mrid = ModuleRevisionId.newInstance(organisation, module, branch,
                                revision, ExtendableItemHelper.getExtraAttributes(attributes,
                                    "extra-"));
                            mrids.add(mrid);
                            if (isDefault) {
                                defaultMrids.add(mrid);
                            } else {
                                Artifact metadataArtifact = 
                                    DefaultArtifact.newIvyArtifact(mrid, pubdate);
                                MetadataArtifactDownloadReport madr = 
                                    new MetadataArtifactDownloadReport(metadataArtifact);
                                metadataReports.put(mrid, madr);
                                realMrids.add(mrid);
                            }
                            try {
                                pubdate = Ivy.DATE_FORMAT.parse(attributes.getValue("pubdate"));
                                skip = false;
                            } catch (ParseException e) {
                                throw new IllegalArgumentException("invalid publication date for "
                                        + organisation + " " + module + " " + revision + ": "
                                        + attributes.getValue("pubdate"));
                            }
                        }
                    } else if ("metadata-artifact".equals(qName)) {
                        if (skip) {
                            return;
                        }
                        MetadataArtifactDownloadReport madr = 
                            (MetadataArtifactDownloadReport) metadataReports.get(mrid);
                        if (madr != null) {
                            madr.setDownloadStatus(
                                DownloadStatus.fromString(attributes.getValue("status")));
                            madr.setDownloadDetails(attributes.getValue("details"));
                            madr.setSize(Long.parseLong(attributes.getValue("size")));
                            madr.setDownloadTimeMillis(Long.parseLong(attributes.getValue("time")));
                            madr.setSearched(parseBoolean(attributes.getValue("searched")));
                            if (attributes.getValue("location") != null) {
                                madr.setLocalFile(new File(attributes.getValue("location")));
                            }
                            if (attributes.getValue("original-local-location") != null) {
                                madr.setOriginalLocalFile(
                                    new File(attributes.getValue("original-local-location")));
                            }
                            if (attributes.getValue("origin-location") != null) {
                                if (ArtifactOrigin.UNKNOWN.getLocation().equals(
                                        attributes.getValue("origin-location"))) {
                                    madr.setArtifactOrigin(ArtifactOrigin.UNKNOWN);
                                } else {
                                    madr.setArtifactOrigin(
                                        new ArtifactOrigin(
                                            parseBoolean(attributes.getValue("origin-is-local")),
                                            attributes.getValue("origin-location")));
                                }
                            }
                        }
                    } else if ("artifact".equals(qName)) {
                        if (skip) {
                            return;
                        }
                        String status = attributes.getValue("status");
                        String artifactName = attributes.getValue("name");
                        String type = attributes.getValue("type");
                        String ext = attributes.getValue("ext");
                        Artifact artifact = new DefaultArtifact(mrid, pubdate, artifactName,
                                type, ext, ExtendableItemHelper.getExtraAttributes(attributes,
                                    "extra-"));
                        ArtifactDownloadReport aReport = new ArtifactDownloadReport(artifact);
                        aReport.setDownloadStatus(DownloadStatus.fromString(status));
                        aReport.setDownloadDetails(attributes.getValue("details"));
                        aReport.setSize(Long.parseLong(attributes.getValue("size")));
                        aReport.setDownloadTimeMillis(Long.parseLong(attributes.getValue("time")));
                        if (attributes.getValue("location") != null) {
                            aReport.setLocalFile(new File(attributes.getValue("location")));
                        }
                        revisionArtifacts.add(aReport);
                    } else if ("origin-location".equals(qName)) {
                        if (skip) {
                            return;
                        }
                        ArtifactDownloadReport aReport = (ArtifactDownloadReport) 
                            revisionArtifacts.get(revisionArtifacts.size() - 1);
                        
                        if (ArtifactOrigin.UNKNOWN.getLocation().equals(
                            attributes.getValue("location"))) {
                            aReport.setArtifactOrigin(ArtifactOrigin.UNKNOWN);
                        } else {
                            aReport.setArtifactOrigin(
                                new ArtifactOrigin(
                                    parseBoolean(attributes.getValue("is-local")),
                                    attributes.getValue("location")));
                        }
                    } else if ("info".equals(qName)) {
                        String organisation = attributes.getValue("organisation");
                        String name = attributes.getValue("module");
                        String branch = attributes.getValue("branch");
                        String revision = attributes.getValue("revision");
                        Map extraAttributes = new HashMap();
                        for (int i = 0; i < attributes.getLength(); i++) {
                            String attName = attributes.getQName(i);
                            if (attName.startsWith("extra-")) {
                                String extraAttrName = attName.substring("extra-".length());
                                String extraAttrValue = attributes.getValue(i);
                                extraAttributes.put(extraAttrName, extraAttrValue);
                            }
                        }
                        mRevisionId = ModuleRevisionId.newInstance(organisation, name, branch,
                            revision, extraAttributes);
                    }
                }

                public void endElement(String uri, String localName, String qname)
                        throws SAXException {
                    if ("dependencies".equals(qname)) {
                        // add the artifacts in the correct order
                        for (Iterator it = revisionsMap.values().iterator(); it.hasNext();) {
                            List artifactReports = (List) it.next();
                            SaxXmlReportParser.this.artifactReports.addAll(artifactReports);
                            for (Iterator iter = artifactReports.iterator(); iter.hasNext();) {
                                ArtifactDownloadReport artifactReport 
                                    = (ArtifactDownloadReport) iter.next();
                                if (artifactReport.getDownloadStatus() != DownloadStatus.FAILED) {
                                    artifacts.add(artifactReport.getArtifact());
                                }
                            }
                            
                        }
                    }
                }

                private int getMaxPos() {
                    return revisionsMap.isEmpty() ? -1 : ((Integer) revisionsMap.keySet()
                            .toArray()[revisionsMap.size() - 1]).intValue();
                }
            });
        }
        
        private static boolean parseBoolean(String str) {
            return (str != null) && str.equalsIgnoreCase("true");
        }

        public List getArtifacts() {
            return artifacts;
        }

        public List getArtifactReports() {
            return artifactReports;
        }

        public List getModuleRevisionIds() {
            return mrids;
        }

        public List getRealModuleRevisionIds() {
            return realMrids;
        }

        public ModuleRevisionId getResolvedModule() {
            return mRevisionId;
        }

        public MetadataArtifactDownloadReport getMetadataArtifactReport(ModuleRevisionId id) {
            return (MetadataArtifactDownloadReport) metadataReports.get(id);
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

    public ArtifactDownloadReport[] getArtifactReports() {
        return (ArtifactDownloadReport[]) parser.getArtifactReports().toArray(
            new ArtifactDownloadReport[parser.getArtifactReports().size()]);
    }

    public ModuleRevisionId[] getDependencyRevisionIds() {
        return (ModuleRevisionId[]) parser.getModuleRevisionIds().toArray(
            new ModuleRevisionId[parser.getModuleRevisionIds().size()]);
    }

    public ModuleRevisionId[] getRealDependencyRevisionIds() {
        return (ModuleRevisionId[]) parser.getRealModuleRevisionIds().toArray(
            new ModuleRevisionId[parser.getRealModuleRevisionIds().size()]);
    }

    public MetadataArtifactDownloadReport getMetadataArtifactReport(ModuleRevisionId id) {
        return parser.getMetadataArtifactReport(id);
    }

    /**
     * Returns the <tt>ModuleRevisionId</tt> of the resolved module.
     */
    public ModuleRevisionId getResolvedModule() {
        return parser.getResolvedModule();
    }
}
