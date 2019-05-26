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

    public void parse(InputStream in) throws IOException, ParseException, SAXException {
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

        private Map<OSGiFilter, String> artifactPatterns = new LinkedHashMap<>();

        public RepositoryHandler(final P2Descriptor p2Descriptor, String repoUrl) {
            super(REPOSITORY);
            // addChild(new PropertiesHandler(), new ChildElementHandler<PropertiesHandler>() {
            // public void childHandled(PropertiesHandler child) {
            // }
            // });
            addChild(new MappingsHandler(), new ChildElementHandler<MappingsHandler>() {
                @Override
                public void childHandled(MappingsHandler child) {
                    for (Map.Entry<String, String> entry : child.outputByFilter.entrySet()) {
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
                    public void childHandled(ArtifactsHandler child) {
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
                public void childHandled(RuleHandler child) {
                    outputByFilter.put(child.filter, child.output);
                }
            });
        }

        @Override
        protected void handleAttributes(Attributes atts) {
            int size = Integer.parseInt(atts.getValue(SIZE));
            outputByFilter = new LinkedHashMap<>(size);
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
                public void childHandled(ArtifactHandler child) throws SAXParseException {
                    String url = getPattern(child.p2Artifact, child.properties);
                    if (url != null) {
                        url = url.replaceAll("\\$\\{repoUrl\\}", repoUrl);
                        url = url.replaceAll("\\$\\{id\\}", child.p2Artifact.getId());
                        url = url.replaceAll("\\$\\{version\\}", child.p2Artifact.getVersion()
                                .toString());
                        URI uri;
                        try {
                            uri = new URL(url).toURI();
                        } catch (MalformedURLException | URISyntaxException e) {
                            throw new SAXParseException("Incorrect artifact url '" + url + "' ("
                                    + e.getMessage() + ")", getLocator(), e);
                        }
                        p2Descriptor.addArtifactUrl(child.p2Artifact.getClassifier(),
                            child.p2Artifact.getId(), child.p2Artifact.getVersion(), uri,
                            child.properties.get("format"));
                    }
                }

                private String getPattern(P2Artifact p2Artifact, Map<String, String> properties) {
                    Map<String, String> props = new HashMap<>(properties);
                    props.put("classifier", p2Artifact.getClassifier());
                    for (Map.Entry<OSGiFilter, String> pattern : artifactPatterns.entrySet()) {
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
                public void childHandled(PropertiesHandler child) {
                    properties = child.properties;
                }
            });
        }

        @Override
        protected void handleAttributes(Attributes atts) throws SAXException {
            String id = atts.getValue(ID);
            Version version = new Version(atts.getValue(VERSION));
            String classifier = atts.getValue(CLASSIFIER);

            p2Artifact = new P2Artifact(id, version, classifier);
        }
    }

}
