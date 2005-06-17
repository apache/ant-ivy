/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.xml;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import fr.jayasoft.ivy.Artifact;
import fr.jayasoft.ivy.DefaultArtifact;
import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.ModuleId;
import fr.jayasoft.ivy.ModuleRevisionId;
import fr.jayasoft.ivy.report.XmlReportOutputter;

public class XmlReportParser {
    public Artifact[] getArtifacts(ModuleId moduleId, String conf, File cache) throws ParseException, IOException {
        File report = new File(cache, XmlReportOutputter.getReportFileName(moduleId, conf));
        if (!report.exists()) {
            throw new IllegalStateException("no report file found for "+moduleId+" "+conf+" in "+cache);
        }
        final List artifacts = new ArrayList();
        try {
            SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
            saxParser.parse(report, new DefaultHandler() {
                private String _organisation;
                private String _module;
                private String _revision;
                private Date _pubdate;
                private boolean _skip;
                public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                    if ("module".equals(qName)) {
                        _organisation = attributes.getValue("organisation");
                        _module = attributes.getValue("name");
                    } else if ("revision".equals(qName)) {
                        _revision = attributes.getValue("name");
                        if (attributes.getValue("error") != null || attributes.getValue("evicted") != null) {
                            _skip = true;
                        } else {
                            try {
                                _pubdate = Ivy.DATE_FORMAT.parse(attributes.getValue("pubdate"));
                                _skip = false;
                            } catch (ParseException e) {
                                throw new IllegalArgumentException("invalid publication date for "+_organisation+" "+_module+" "+_revision+": "+attributes.getValue("pubdate"));
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
                        Artifact artifact = new DefaultArtifact(ModuleRevisionId.newInstance(_organisation, _module, _revision), _pubdate, artifactName, type, ext);
                        artifacts.add(artifact);
                    }
                }
            });
            return (Artifact[])artifacts.toArray(new Artifact[artifacts.size()]);
        } catch (Exception ex) {
            ParseException pe = new ParseException("failed to parse report for "+moduleId+" "+conf+": "+ex.getMessage(), 0);
            pe.initCause(ex);
            throw pe;
        }
    }
        
    
}
