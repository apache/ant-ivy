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

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.ivy.core.cache.ArtifactOrigin;
import org.apache.ivy.core.module.descriptor.License;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ConfigurationResolveReport;
import org.apache.ivy.core.report.MetadataArtifactDownloadReport;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.core.resolve.IvyNodeCallers.Caller;
import org.apache.ivy.core.resolve.IvyNodeEviction.EvictionData;
import org.apache.ivy.util.DateUtil;
import org.apache.ivy.util.StringUtils;
import org.apache.ivy.util.XMLHelper;

/**
 * XmlReportWriter allows to write ResolveReport in an xml format.
 */
public class XmlReportWriter {

    static final String REPORT_ENCODING = "UTF-8";

    public void output(ConfigurationResolveReport report, OutputStream stream) {
        output(report, new String[] {report.getConfiguration()}, stream);
    }

    public void output(ConfigurationResolveReport report, String[] confs, OutputStream stream) {
        OutputStreamWriter encodedOutStream;
        try {
            encodedOutStream = new OutputStreamWriter(stream, REPORT_ENCODING);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(REPORT_ENCODING + " is not known on your jvm", e);
        }
        PrintWriter out = new PrintWriter(new BufferedWriter(encodedOutStream));
        ModuleRevisionId mrid = report.getModuleDescriptor().getModuleRevisionId();
        // out.println("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
        out.println("<?xml version=\"1.0\" encoding=\"" + REPORT_ENCODING + "\"?>");
        out.println("<?xml-stylesheet type=\"text/xsl\" href=\"ivy-report.xsl\"?>");
        out.println("<ivy-report version=\"1.0\">");
        out.println("\t<info");
        out.println("\t\torganisation=\"" + XMLHelper.escape(mrid.getOrganisation()) + "\"");
        out.println("\t\tmodule=\"" + XMLHelper.escape(mrid.getName()) + "\"");
        out.println("\t\trevision=\"" + XMLHelper.escape(mrid.getRevision()) + "\"");
        if (mrid.getBranch() != null) {
            out.println("\t\tbranch=\"" + XMLHelper.escape(mrid.getBranch()) + "\"");
        }
        Map<String, String> extraAttributes = mrid.getExtraAttributes();
        for (Map.Entry<String, String> entry : extraAttributes.entrySet()) {
            out.println("\t\textra-" + entry.getKey() + "=\""
                    + XMLHelper.escape(entry.getValue()) + "\"");
        }
        out.println("\t\tconf=\"" + XMLHelper.escape(report.getConfiguration()) + "\"");
        out.println("\t\tconfs=\"" + XMLHelper.escape(StringUtils.join(confs, ", ")) + "\"");
        out.println("\t\tdate=\"" + DateUtil.format(report.getDate()) + "\"/>");

        out.println("\t<dependencies>");

        // create a list of ModuleRevisionIds indicating the position for each dependency
        List<ModuleRevisionId> dependencies = new ArrayList<>(report.getModuleRevisionIds());

        for (ModuleId mid : report.getModuleIds()) {
            out.println("\t\t<module organisation=\"" + XMLHelper.escape(mid.getOrganisation())
                    + "\"" + " name=\"" + XMLHelper.escape(mid.getName()) + "\">");
            for (IvyNode dep : report.getNodes(mid)) {
                ouputRevision(report, out, dependencies, dep);
            }
            out.println("\t\t</module>");
        }
        out.println("\t</dependencies>");
        out.println("</ivy-report>");
        out.flush();
    }

    private void ouputRevision(ConfigurationResolveReport report, PrintWriter out,
            List<ModuleRevisionId> dependencies, IvyNode dep) {
        Map<String, String> extraAttributes;
        ModuleDescriptor md = null;
        if (dep.getModuleRevision() != null) {
            md = dep.getModuleRevision().getDescriptor();
        }
        StringBuilder details = new StringBuilder();
        if (dep.isLoaded()) {
            details.append(" status=\"");
            details.append(XMLHelper.escape(dep.getDescriptor().getStatus()));
            details.append("\" pubdate=\"");
            details.append(DateUtil.format(new Date(dep.getPublication())));
            details.append("\" resolver=\"");
            details.append(XMLHelper.escape(dep.getModuleRevision().getResolver().getName()));
            details.append("\" artresolver=\"");
            details.append(XMLHelper
                    .escape(dep.getModuleRevision().getArtifactResolver().getName()));
            details.append("\"");
        }
        if (dep.isEvicted(report.getConfiguration())) {
            EvictionData ed = dep.getEvictedData(report.getConfiguration());
            if (ed.getConflictManager() != null) {
                details.append(" evicted=\"")
                        .append(XMLHelper.escape(ed.getConflictManager().toString())).append("\"");
            } else {
                details.append(" evicted=\"transitive\"");
            }
            details.append(" evicted-reason=\"")
                    .append(XMLHelper.escape(ed.getDetail() == null ? "" : ed.getDetail()))
                    .append("\"");
        }
        if (dep.hasProblem()) {
            details.append(" error=\"").append(XMLHelper.escape(dep.getProblem().getMessage()))
                    .append("\"");
        }
        if (md != null && md.getHomePage() != null) {
            details.append(" homepage=\"").append(XMLHelper.escape(md.getHomePage())).append("\"");
        }
        extraAttributes = (md != null) ? md.getExtraAttributes() : dep.getResolvedId()
                .getExtraAttributes();
        for (Map.Entry<String, String> entry : extraAttributes.entrySet()) {
            details.append(" extra-").append(entry.getKey()).append("=\"")
                    .append(XMLHelper.escape(entry.getValue())).append("\"");
        }
        out.println(String.format("\t\t\t<revision name=\"%s\"%s%s downloaded=\"%s\" searched=\"%s\"%s conf=\"%s\" position=\"%d\">",
                XMLHelper.escape(dep.getResolvedId().getRevision()),
                (dep.getResolvedId().getBranch() == null) ? "" : " branch=\""
                + XMLHelper.escape(dep.getResolvedId().getBranch()) + "\"",
                details, dep.isDownloaded(), dep.isSearched(),
                (dep.getDescriptor() == null) ? "" : " default=\""
                + dep.getDescriptor().isDefault() + "\"",
                toString(dep.getConfigurations(report.getConfiguration())),
                dependencies.indexOf(dep.getResolvedId())));
        if (md != null) {
            License[] licenses = md.getLicenses();
            for (License license : licenses) {
                String lurl;
                if (license.getUrl() != null) {
                    lurl = " url=\"" + XMLHelper.escape(license.getUrl()) + "\"";
                } else {
                    lurl = "";
                }
                out.println("\t\t\t\t<license name=\"" + XMLHelper.escape(license.getName()) + "\""
                        + lurl + "/>");
            }
        }
        outputMetadataArtifact(out, dep);
        outputEvictionInformation(report, out, dep);
        outputCallers(report, out, dep);
        outputArtifacts(report, out, dep);
        out.println("\t\t\t</revision>");
    }

    private void outputEvictionInformation(ConfigurationResolveReport report, PrintWriter out,
            IvyNode dep) {
        if (dep.isEvicted(report.getConfiguration())) {
            EvictionData ed = dep.getEvictedData(report.getConfiguration());
            Collection<IvyNode> selected = ed.getSelected();
            if (selected != null) {
                for (IvyNode sel : selected) {
                    out.println("\t\t\t\t<evicted-by rev=\""
                            + XMLHelper.escape(sel.getResolvedId().getRevision()) + "\"/>");
                }
            }
        }
    }

    private void outputMetadataArtifact(PrintWriter out, IvyNode dep) {
        if (dep.getModuleRevision() != null) {
            MetadataArtifactDownloadReport madr = dep.getModuleRevision().getReport();
            out.print("\t\t\t\t<metadata-artifact");
            out.print(" status=\"" + XMLHelper.escape(madr.getDownloadStatus().toString()) + "\"");
            out.print(" details=\"" + XMLHelper.escape(madr.getDownloadDetails()) + "\"");
            out.print(" size=\"" + madr.getSize() + "\"");
            out.print(" time=\"" + madr.getDownloadTimeMillis() + "\"");
            if (madr.getLocalFile() != null) {
                out.print(" location=\"" + XMLHelper.escape(madr.getLocalFile().getAbsolutePath())
                        + "\"");
            }

            out.print(" searched=\"" + madr.isSearched() + "\"");
            if (madr.getOriginalLocalFile() != null) {
                out.print(" original-local-location=\""
                        + XMLHelper.escape(madr.getOriginalLocalFile().getAbsolutePath()) + "\"");
            }

            ArtifactOrigin origin = madr.getArtifactOrigin();
            if (origin != null) {
                out.print(" origin-is-local=\"" + String.valueOf(origin.isLocal()) + "\"");
                out.print(" origin-location=\"" + XMLHelper.escape(origin.getLocation()) + "\"");
            }
            out.println("/>");

        }
    }

    private void outputCallers(ConfigurationResolveReport report, PrintWriter out, IvyNode dep) {
        Caller[] callers = dep.getCallers(report.getConfiguration());
        for (Caller caller : callers) {
            StringBuilder callerDetails = new StringBuilder();
            Map<String, String> callerExtraAttributes = caller.getDependencyDescriptor()
                    .getExtraAttributes();
            for (Map.Entry<String, String> entry : callerExtraAttributes.entrySet()) {
                callerDetails.append(" extra-").append(entry.getKey()).append("=\"")
                        .append(XMLHelper.escape(entry.getValue())).append("\"");
            }

            out.println(String.format("\t\t\t\t<caller organisation=\"%s\" name=\"%s\" conf=\"%s\" rev=\"%s\" rev-constraint-default=\"%s\" rev-constraint-dynamic=\"%s\" callerrev=\"%s\"%s/>",
                    XMLHelper.escape(caller.getModuleRevisionId().getOrganisation()),
                    XMLHelper.escape(caller.getModuleRevisionId().getName()),
                    XMLHelper.escape(toString(caller.getCallerConfigurations())),
                    XMLHelper.escape(caller.getAskedDependencyId(dep.getData()).getRevision()),
                    XMLHelper.escape(caller.getDependencyDescriptor().getDependencyRevisionId().getRevision()),
                    XMLHelper.escape(caller.getDependencyDescriptor().getDynamicConstraintDependencyRevisionId().getRevision()),
                    XMLHelper.escape(caller.getModuleRevisionId().getRevision()), callerDetails));
        }
    }

    private void outputArtifacts(ConfigurationResolveReport report, PrintWriter out, IvyNode dep) {
        out.println("\t\t\t\t<artifacts>");
        for (ArtifactDownloadReport adr : report.getDownloadReports(dep.getResolvedId())) {
            out.print("\t\t\t\t\t<artifact name=\"" + XMLHelper.escape(adr.getName())
                    + "\" type=\"" + XMLHelper.escape(adr.getType()) + "\" ext=\""
                    + XMLHelper.escape(adr.getExt()) + "\"");
            for (Map.Entry<String, String> entry : adr.getArtifact().getExtraAttributes().entrySet()) {
                out.print(" extra-" + entry.getKey() + "=\""
                        + XMLHelper.escape(entry.getValue()) + "\"");
            }
            out.print(" status=\"" + XMLHelper.escape(adr.getDownloadStatus().toString()) + "\"");
            out.print(" details=\"" + XMLHelper.escape(adr.getDownloadDetails()) + "\"");
            out.print(" size=\"" + adr.getSize() + "\"");
            out.print(" time=\"" + adr.getDownloadTimeMillis() + "\"");
            if (adr.getLocalFile() != null) {
                out.print(" location=\""
                        + XMLHelper.escape(adr.getLocalFile().getAbsolutePath()) + "\"");
            }
            if (adr.getUnpackedLocalFile() != null) {
                out.print(" unpackedFile=\""
                        + XMLHelper.escape(adr.getUnpackedLocalFile().getAbsolutePath()) + "\"");
            }

            ArtifactOrigin origin = adr.getArtifactOrigin();
            if (origin != null) {
                out.println(">");
                out.println("\t\t\t\t\t\t<origin-location is-local=\""
                        + String.valueOf(origin.isLocal()) + "\"" + " location=\""
                        + XMLHelper.escape(origin.getLocation()) + "\"/>");
                out.println("\t\t\t\t\t</artifact>");
            } else {
                out.println("/>");
            }
        }
        out.println("\t\t\t\t</artifacts>");
    }

    private String toString(String[] strs) {
        StringBuilder buf = new StringBuilder();
        for (String str : strs) {
            if (buf.length() > 0) {
                buf.append(", ");
            }
            buf.append(str);
        }
        return XMLHelper.escape(buf.toString());
    }
}
