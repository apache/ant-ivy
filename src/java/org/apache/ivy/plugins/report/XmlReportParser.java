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
package org.apache.ivy.plugins.report;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.ivy.core.cache.ArtifactOrigin;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.DownloadStatus;
import org.apache.ivy.core.report.MetadataArtifactDownloadReport;
import org.apache.ivy.util.DateUtil;
import org.apache.ivy.util.extendable.ExtendableItemHelper;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class XmlReportParser {
    private static class SaxXmlReportParser {
        private final class XmlReportParserHandler extends DefaultHandler {
            private String organisation;

            private String module;

            private String branch;

            private String revision;

            private int position;

            private Date pubdate;

            private boolean skip;

            private ModuleRevisionId mrid;

            private boolean isDefault;

            // Use a TreeMap to order by
            private SortedMap<Integer, List<ArtifactDownloadReport>> revisionsMap = new TreeMap<>();

            private List<ArtifactDownloadReport> revisionArtifacts = null;

            public void startElement(String uri, String localName, String qName,
                    Attributes attributes) throws SAXException {
                switch (qName) {
                    case "module":
                        organisation = attributes.getValue("organisation");
                        module = attributes.getValue("name");
                        break;
                    case "revision":
                        revisionArtifacts = new ArrayList<>();
                        branch = attributes.getValue("branch");
                        revision = attributes.getValue("name");
                        isDefault = Boolean.valueOf(attributes.getValue("default"));
                        // retrieve position from file. If no position is found, it may be an old
                        // report generated with a previous version,
                        // in which case, we put it at the last position
                        String pos = attributes.getValue("position");
                        position = pos == null ? getMaxPos() + 1 : Integer.valueOf(pos);
                        if (attributes.getValue("error") != null) {
                            hasError = true;
                            skip = true;
                        } else if (attributes.getValue("evicted") != null) {
                            skip = true;
                        } else {
                            revisionsMap.put(position, revisionArtifacts);
                            mrid = ModuleRevisionId.newInstance(organisation, module, branch, revision,
                                    ExtendableItemHelper.getExtraAttributes(attributes, "extra-"));
                            mrids.add(mrid);
                            if (isDefault) {
                                defaultMrids.add(mrid);
                            } else {
                                Artifact metadataArtifact = DefaultArtifact.newIvyArtifact(mrid,
                                        pubdate);
                                MetadataArtifactDownloadReport madr = new MetadataArtifactDownloadReport(
                                        metadataArtifact);
                                metadataReports.put(mrid, madr);
                                realMrids.add(mrid);
                            }
                            try {
                                String pubDateAttr = attributes.getValue("pubdate");
                                if (pubDateAttr != null) {
                                    pubdate = DateUtil.parse(pubDateAttr);
                                }
                                skip = false;
                            } catch (ParseException e) {
                                throw new IllegalArgumentException("invalid publication date for "
                                        + organisation + " " + module + " " + revision + ": "
                                        + attributes.getValue("pubdate"));
                            }
                        }
                        break;
                    case "metadata-artifact":
                        if (skip) {
                            return;
                        }
                        MetadataArtifactDownloadReport madr = metadataReports.get(mrid);
                        if (madr != null) {
                            madr.setDownloadStatus(DownloadStatus.fromString(attributes
                                    .getValue("status")));
                            madr.setDownloadDetails(attributes.getValue("details"));
                            madr.setSize(Long.parseLong(attributes.getValue("size")));
                            madr.setDownloadTimeMillis(Long.parseLong(attributes.getValue("time")));
                            madr.setSearched(parseBoolean(attributes.getValue("searched")));
                            if (attributes.getValue("location") != null) {
                                madr.setLocalFile(new File(attributes.getValue("location")));
                            }
                            if (attributes.getValue("original-local-location") != null) {
                                madr.setOriginalLocalFile(new File(attributes
                                        .getValue("original-local-location")));
                            }
                            if (attributes.getValue("origin-location") != null) {
                                if (ArtifactOrigin.isUnknown(attributes.getValue("origin-location"))) {
                                    madr.setArtifactOrigin(ArtifactOrigin.unknown(madr.getArtifact()));
                                } else {
                                    madr.setArtifactOrigin(new ArtifactOrigin(madr.getArtifact(),
                                            parseBoolean(attributes.getValue("origin-is-local")),
                                            attributes.getValue("origin-location")));
                                }
                            }
                        }
                        break;
                    case "artifact":
                        if (skip) {
                            return;
                        }
                        String status = attributes.getValue("status");
                        String artifactName = attributes.getValue("name");
                        String type = attributes.getValue("type");
                        String ext = attributes.getValue("ext");
                        Artifact artifact = new DefaultArtifact(mrid, pubdate, artifactName, type, ext,
                                ExtendableItemHelper.getExtraAttributes(attributes, "extra-"));
                        ArtifactDownloadReport aReport = new ArtifactDownloadReport(artifact);
                        aReport.setDownloadStatus(DownloadStatus.fromString(status));
                        aReport.setDownloadDetails(attributes.getValue("details"));
                        aReport.setSize(Long.parseLong(attributes.getValue("size")));
                        aReport.setDownloadTimeMillis(Long.parseLong(attributes.getValue("time")));
                        if (attributes.getValue("location") != null) {
                            aReport.setLocalFile(new File(attributes.getValue("location")));
                        }
                        if (attributes.getValue("unpackedFile") != null) {
                            aReport.setUnpackedLocalFile(new File(attributes.getValue("unpackedFile")));
                        }
                        revisionArtifacts.add(aReport);
                        break;
                    case "origin-location":
                        if (skip) {
                            return;
                        }
                        ArtifactDownloadReport adr = revisionArtifacts
                                .get(revisionArtifacts.size() - 1);

                        if (ArtifactOrigin.isUnknown(attributes.getValue("location"))) {
                            adr.setArtifactOrigin(ArtifactOrigin.unknown(adr.getArtifact()));
                        } else {
                            adr.setArtifactOrigin(new ArtifactOrigin(adr.getArtifact(),
                                    parseBoolean(attributes.getValue("is-local")),
                                    attributes.getValue("location")));
                        }
                        break;
                    case "info":
                        String organisation = attributes.getValue("organisation");
                        String name = attributes.getValue("module");
                        String branch = attributes.getValue("branch");
                        String revision = attributes.getValue("revision");
                        mRevisionId = ModuleRevisionId.newInstance(organisation, name, branch, revision,
                            ExtendableItemHelper.getExtraAttributes(attributes, "extra-"));
                        break;
                }
            }

            public void endElement(String uri, String localName, String qname) throws SAXException {
                if ("dependencies".equals(qname)) {
                    // add the artifacts in the correct order
                    for (List<ArtifactDownloadReport> artifactReports : revisionsMap.values()) {
                        SaxXmlReportParser.this.artifactReports.addAll(artifactReports);
                        for (ArtifactDownloadReport artifactReport : artifactReports) {
                            if (artifactReport.getDownloadStatus() != DownloadStatus.FAILED) {
                                artifacts.add(artifactReport.getArtifact());
                            }
                        }

                    }
                }
            }

            private int getMaxPos() {
                return revisionsMap.isEmpty() ? -1
                        : (Integer) revisionsMap.keySet().toArray()[revisionsMap.size() - 1];
            }
        }

        private List<ModuleRevisionId> mrids = new ArrayList<>();

        private List<ModuleRevisionId> defaultMrids = new ArrayList<>();

        private List<ModuleRevisionId> realMrids = new ArrayList<>();

        private List<Artifact> artifacts = new ArrayList<>();

        private List<ArtifactDownloadReport> artifactReports = new ArrayList<>();

        private Map<ModuleRevisionId, MetadataArtifactDownloadReport> metadataReports = new HashMap<>();

        private ModuleRevisionId mRevisionId;

        private File report;

        private boolean hasError = false;

        SaxXmlReportParser(File report) {
            this.report = report;
        }

        public void parse() throws Exception {
            SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
            saxParser.parse(report, new XmlReportParserHandler());
        }

        private static boolean parseBoolean(String str) {
            return (str != null) && str.equalsIgnoreCase("true");
        }

        public List<Artifact> getArtifacts() {
            return artifacts;
        }

        public List<ArtifactDownloadReport> getArtifactReports() {
            return artifactReports;
        }

        public List<ModuleRevisionId> getModuleRevisionIds() {
            return mrids;
        }

        public List<ModuleRevisionId> getRealModuleRevisionIds() {
            return realMrids;
        }

        public ModuleRevisionId getResolvedModule() {
            return mRevisionId;
        }

        public MetadataArtifactDownloadReport getMetadataArtifactReport(ModuleRevisionId id) {
            return metadataReports.get(id);
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
        return parser.getArtifacts().toArray(new Artifact[parser.getArtifacts().size()]);
    }

    public ArtifactDownloadReport[] getArtifactReports() {
        return parser.getArtifactReports().toArray(
            new ArtifactDownloadReport[parser.getArtifactReports().size()]);
    }

    public ModuleRevisionId[] getDependencyRevisionIds() {
        return parser.getModuleRevisionIds().toArray(
            new ModuleRevisionId[parser.getModuleRevisionIds().size()]);
    }

    public ModuleRevisionId[] getRealDependencyRevisionIds() {
        return parser.getRealModuleRevisionIds().toArray(
            new ModuleRevisionId[parser.getRealModuleRevisionIds().size()]);
    }

    public MetadataArtifactDownloadReport getMetadataArtifactReport(ModuleRevisionId id) {
        return parser.getMetadataArtifactReport(id);
    }

    /**
     * Returns the <tt>ModuleRevisionId</tt> of the resolved module.
     *
     * @return ModuleRevisionId
     */
    public ModuleRevisionId getResolvedModule() {
        return parser.getResolvedModule();
    }

    public boolean hasError() {
        return parser.hasError;
    }
}
