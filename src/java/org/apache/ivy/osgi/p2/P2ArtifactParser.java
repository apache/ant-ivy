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
package org.apache.ivy.osgi.p2;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.ivy.osgi.filter.OSGiFilter;
import org.apache.ivy.osgi.filter.OSGiFilterParser;
import org.apache.ivy.osgi.p2.PropertiesParser.PropertiesHandler;
import org.apache.ivy.osgi.util.DelegatingHandler;
import org.apache.ivy.osgi.util.Version;
import org.apache.ivy.util.XMLHelper;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class P2ArtifactParser implements XMLInputParser {

    private final P2Descriptor p2Descriptor;

    private final String repoUrl;

    public P2ArtifactParser(P2Descriptor p2Descriptor, String repoUrl) {
        this.p2Descriptor = p2Descriptor;
        this.repoUrl = repoUrl;
    }

    public void parse(InputStream in) throws ParseException, IOException, SAXException {
        RepositoryHandler handler = new RepositoryHandler(p2Descriptor, repoUrl);
        try {
            XMLHelper.parse(in, null, handler, null);
        } catch (ParserConfigurationException e) {
            throw new SAXException(e);
        }
    }

    private static class RepositoryHandler extends DelegatingHandler {

        private static final String REPOSITORY = "repository";

        // private static final String NAME = "name";
        //
        // private static final String TYPE = "type";
        //
        // private static final String VERSION = "version";

        private Map<OSGiFilter, String> artifactPatterns = new LinkedHashMap<OSGiFilter, String>();

        public RepositoryHandler(final P2Descriptor p2Descriptor, String repoUrl) {
            super(REPOSITORY);
            // addChild(new PropertiesHandler(), new ChildElementHandler<PropertiesHandler>() {
            // public void childHanlded(PropertiesHandler child) {
            // }
            // });
            addChild(new MappingsHandler(), new ChildElementHandler<MappingsHandler>() {
                @Override
                public void childHanlded(MappingsHandler child) {
                    for (Entry<String, String> entry : child.outputByFilter.entrySet()) {
                        OSGiFilter filter;
                        try {
                            filter = OSGiFilterParser.parse(entry.getKey());
                        } catch (ParseException e) {
                            throw new IllegalStateException();
                        }
                        artifactPatterns.put(filter, entry.getValue());

                    }
                }
            });
            addChild(new ArtifactsHandler(p2Descriptor, artifactPatterns, repoUrl),
                new ChildElementHandler<ArtifactsHandler>() {
                    @Override
                    public void childHanlded(ArtifactsHandler child) {
                        // nothing to do
                    }
                });
        }
        // protected void handleAttributes(Attributes atts) {
        // String name = atts.getValue(NAME);
        // String type = atts.getValue(TYPE);
        // String version = atts.getValue(VERSION);
        // }
    }

    private static class MappingsHandler extends DelegatingHandler {

        private static final String MAPPINGS = "mappings";

        private static final String SIZE = "size";

        Map<String, String> outputByFilter;

        public MappingsHandler() {
            super(MAPPINGS);
            addChild(new RuleHandler(), new ChildElementHandler<RuleHandler>() {
                @Override
                public void childHanlded(RuleHandler child) {
                    outputByFilter.put(child.filter, child.output);
                }
            });
        }

        @Override
        protected void handleAttributes(Attributes atts) {
            int size = Integer.parseInt(atts.getValue(SIZE));
            outputByFilter = new LinkedHashMap<String, String>(size);
        }

    }

    private static class RuleHandler extends DelegatingHandler {

        private static final String RULE = "rule";

        private static final String FILTER = "filter";

        private static final String OUTPUT = "output";

        private String filter;

        private String output;

        public RuleHandler() {
            super(RULE);
        }

        @Override
        protected void handleAttributes(Attributes atts) {
            filter = atts.getValue(FILTER);
            output = atts.getValue(OUTPUT);
        }

    }

    private static class ArtifactsHandler extends DelegatingHandler {

        private static final String ARTIFACTS = "artifacts";

        // private static final String SIZE = "size";

        public ArtifactsHandler(final P2Descriptor p2Descriptor,
                final Map<OSGiFilter, String> artifactPatterns, final String repoUrl) {
            super(ARTIFACTS);
            addChild(new ArtifactHandler(), new ChildElementHandler<ArtifactHandler>() {
                @Override
                public void childHanlded(ArtifactHandler child) throws SAXParseException {
                    String url = getPattern(child.p2Artifact, child.properties);
                    if (url != null) {
                        url = url.replaceAll("\\$\\{repoUrl\\}", repoUrl);
                        url = url.replaceAll("\\$\\{id\\}", child.p2Artifact.getId());
                        url = url.replaceAll("\\$\\{version\\}", child.p2Artifact.getVersion()
                                .toString());
                        URI uri;
                        try {
                            uri = new URL(url).toURI();
                        } catch (MalformedURLException e) {
                            throw new SAXParseException("Incorrect artifact url '" + url + "' ("
                                    + e.getMessage() + ")", getLocator(), e);
                        } catch (URISyntaxException e) {
                            throw new SAXParseException("Incorrect artifact url '" + url + "' ("
                                    + e.getMessage() + ")", getLocator(), e);
                        }
                        p2Descriptor.addArtifactUrl(child.p2Artifact.getClassifier(),
                            child.p2Artifact.getId(), child.p2Artifact.getVersion(), uri,
                            child.properties.get("format"));
                    }
                }

                private String getPattern(P2Artifact p2Artifact, Map<String, String> properties) {
                    Map<String, String> props = new HashMap<String, String>(properties);
                    props.put("classifier", p2Artifact.getClassifier());
                    for (Entry<OSGiFilter, String> pattern : artifactPatterns.entrySet()) {
                        if (pattern.getKey().eval(props)) {
                            return pattern.getValue();
                        }
                    }
                    return null;
                }
            });
        }

        // protected void handleAttributes(Attributes atts) {
        // int size = Integer.parseInt(atts.getValue(SIZE));
        // artifacts = new ArrayList(size);
        // }

    }

    private static class ArtifactHandler extends DelegatingHandler {

        private static final String ARTIFACT = "artifact";

        private static final String CLASSIFIER = "classifier";

        private static final String ID = "id";

        private static final String VERSION = "version";

        private P2Artifact p2Artifact;

        private Map<String, String> properties;

        public ArtifactHandler() {
            super(ARTIFACT);
            addChild(new PropertiesHandler(), new ChildElementHandler<PropertiesHandler>() {
                @Override
                public void childHanlded(PropertiesHandler child) {
                    properties = child.properties;
                }
            });
        }

        @Override
        protected void handleAttributes(Attributes atts) throws SAXException {
            String id = atts.getValue(ID);
            Version version;
            try {
                version = new Version(atts.getValue(VERSION));
            } catch (ParseException e) {
                throw new SAXException("Incorrect version attribute on artifact '" + id + "': "
                        + atts.getValue(VERSION) + " (" + e.getMessage() + ")");
            }
            String classifier = atts.getValue(CLASSIFIER);

            p2Artifact = new P2Artifact(id, version, classifier);
        }
    }

}
