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
package org.apache.ivy.osgi.obr.xml;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.ivy.osgi.core.BundleArtifact;
import org.apache.ivy.osgi.core.BundleInfo;
import org.apache.ivy.osgi.core.ExecutionEnvironmentProfileProvider;
import org.apache.ivy.osgi.filter.OSGiFilter;
import org.apache.ivy.osgi.filter.OSGiFilterParser;
import org.apache.ivy.osgi.repo.BundleRepoDescriptor;
import org.apache.ivy.osgi.util.DelegatingHandler;
import org.apache.ivy.osgi.util.Version;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.XMLHelper;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class OBRXMLParser {

    public static BundleRepoDescriptor parse(URI baseUri, InputStream in) throws IOException,
            SAXException {
        RepositoryHandler handler = new RepositoryHandler(baseUri);
        try {
            XMLHelper.parse(in, null, handler, null);
        } catch (ParserConfigurationException e) {
            throw new SAXException(e);
        }
        return handler.repo;
    }

    static class RepositoryHandler extends DelegatingHandler {

        static final String REPOSITORY = "repository";

        static final String LASTMODIFIED = "lastmodified";

        static final String NAME = "name";

        BundleRepoDescriptor repo;

        private final URI baseUri;

        public RepositoryHandler(URI baseUri) {
            super(REPOSITORY);
            this.baseUri = baseUri;
            addChild(new ResourceHandler(), new ChildElementHandler<ResourceHandler>() {
                @Override
                public void childHanlded(ResourceHandler child) {
                    repo.addBundle(child.bundleInfo);
                }
            });
        }

        @Override
        protected void handleAttributes(Attributes atts) {
            repo = new BundleRepoDescriptor(baseUri,
                    ExecutionEnvironmentProfileProvider.getInstance());

            repo.setName(atts.getValue(NAME));

            repo.setLastModified(atts.getValue(LASTMODIFIED));
        }
    }

    static class ResourceHandler extends DelegatingHandler {

        private static final String DEFAULT_VERSION = "1.0.0";

        static final String RESOURCE = "resource";

        static final String ID = "id";

        static final String PRESENTATION_NAME = "presentationname";

        static final String SYMBOLIC_NAME = "symbolicname";

        static final String URI = "uri";

        static final String VERSION = "version";

        BundleInfo bundleInfo;

        public ResourceHandler() {
            super(RESOURCE);

            setSkipOnError(true); // if anything bad happen in any children, just ignore the
                                  // resource

            addChild(new ResourceSourceHandler(), new ChildElementHandler<ResourceSourceHandler>() {
                @Override
                public void childHanlded(ResourceSourceHandler child) {
                    String uri = child.getBufferedChars().trim();
                    if (!uri.endsWith(".jar")) {
                        // the maven plugin is putting some useless source url sometimes...
                        log(Message.MSG_WARN,
                            "A source uri is suspect, it is not ending with .jar, it is probably"
                                    + " a pointer to a download page. Ignoring it.");
                        return;
                    }
                    try {
                        bundleInfo.addArtifact(new BundleArtifact(true, new URI(uri), null));
                    } catch (URISyntaxException e) {
                        log(Message.MSG_WARN, "Incorrect uri " + uri + ". The source of "
                                + bundleInfo.getSymbolicName() + " is then ignored.");
                        return;
                    }
                }
            });
            addChild(new ResourceDescriptionHandler(),
                new ChildElementHandler<ResourceDescriptionHandler>() {
                    @Override
                    public void childHanlded(ResourceDescriptionHandler child) {
                        bundleInfo.setDescription(child.getBufferedChars().trim());
                    }
                });
            addChild(new ResourceDocumentationHandler(),
                new ChildElementHandler<ResourceDocumentationHandler>() {
                    @Override
                    public void childHanlded(ResourceDocumentationHandler child) {
                        bundleInfo.setDocumentation(child.getBufferedChars().trim());
                    }
                });
            addChild(new ResourceLicenseHandler(),
                new ChildElementHandler<ResourceLicenseHandler>() {
                    @Override
                    public void childHanlded(ResourceLicenseHandler child) {
                        bundleInfo.setLicense(child.getBufferedChars().trim());
                    }
                });
            addChild(new ResourceSizeHandler(), new ChildElementHandler<ResourceSizeHandler>() {
                @Override
                public void childHanlded(ResourceSizeHandler child) {
                    String size = child.getBufferedChars().trim();
                    try {
                        bundleInfo.setSize(Integer.valueOf(size));
                    } catch (NumberFormatException e) {
                        log(Message.MSG_WARN,
                            "Invalid size for the bundle " + bundleInfo.getSymbolicName() + ": "
                                    + size + ". This size is then ignored.");
                    }
                }
            });
            addChild(new CapabilityHandler(), new ChildElementHandler<CapabilityHandler>() {
                @Override
                public void childHanlded(CapabilityHandler child) throws SAXParseException {

                    try {
                        CapabilityAdapter.adapt(bundleInfo, child.capability);
                    } catch (ParseException e) {
                        throw new SAXParseException("Invalid capability: " + e.getMessage(), child
                                .getLocator());
                    }
                }
            });
            addChild(new RequireHandler(), new ChildElementHandler<RequireHandler>() {
                @Override
                public void childHanlded(RequireHandler child) throws SAXParseException {
                    try {
                        RequirementAdapter.adapt(bundleInfo, child.requirement);
                    } catch (UnsupportedFilterException e) {
                        throw new SAXParseException("Unsupported requirement filter: "
                                + child.filter + " (" + e.getMessage() + ")", getLocator());
                    } catch (ParseException e) {
                        throw new SAXParseException(
                                "Error in the requirement filter on the bundle: " + e.getMessage(),
                                getLocator());
                    }
                }
            });
            addChild(new ExtendHandler(), new ChildElementHandler<ExtendHandler>() {
                @Override
                public void childHanlded(ExtendHandler child) throws SAXParseException {
                    // TODO handle fragment host
                }
            });
        }

        @Override
        protected void handleAttributes(Attributes atts) throws SAXException {
            String symbolicname = atts.getValue(SYMBOLIC_NAME);
            if (symbolicname == null) {
                log(Message.MSG_ERR, "Resource with no symobilc name, skipping it.");
                skip();
                return;
            }

            String v = getOptionalAttribute(atts, VERSION, DEFAULT_VERSION);
            Version version;
            try {
                version = new Version(v);
            } catch (ParseException e) {
                log(Message.MSG_ERR, "Incorrect resource version: " + v + ". The resource "
                        + symbolicname + " is then ignored.");
                skip();
                return;
            }

            bundleInfo = new BundleInfo(symbolicname, version);
            bundleInfo.setPresentationName(atts.getValue(PRESENTATION_NAME));
            String uri = atts.getValue(URI);
            if (uri != null) {
                try {
                    bundleInfo.addArtifact(new BundleArtifact(false, new URI(uri), null));
                } catch (URISyntaxException e) {
                    log(Message.MSG_ERR, "Incorrect uri " + uri + ". The resource " + symbolicname
                            + " is then ignored.");
                    skip();
                    return;
                }
            }
            bundleInfo.setId(atts.getValue(ID));
        }

        @Override
        protected String getCurrentElementIdentifier() {
            return bundleInfo.getSymbolicName() + "/" + bundleInfo.getVersion();
        }

    }

    static class ResourceSourceHandler extends DelegatingHandler {

        static final String SOURCE = "source";

        public ResourceSourceHandler() {
            super(SOURCE);
            setBufferingChar(true);
        }

    }

    static class ResourceDescriptionHandler extends DelegatingHandler {

        static final String DESCRIPTION = "description";

        public ResourceDescriptionHandler() {
            super(DESCRIPTION);
            setBufferingChar(true);
        }

    }

    static class ResourceDocumentationHandler extends DelegatingHandler {

        static final String DOCUMENTATION = "documentation";

        public ResourceDocumentationHandler() {
            super(DOCUMENTATION);
            setBufferingChar(true);
        }
    }

    static class ResourceLicenseHandler extends DelegatingHandler {

        static final String LICENSE = "license";

        public ResourceLicenseHandler() {
            super(LICENSE);
            setBufferingChar(true);
        }

    }

    static class ResourceSizeHandler extends DelegatingHandler {

        static final String SIZE = "size";

        public ResourceSizeHandler() {
            super(SIZE);
            setBufferingChar(true);
        }
    }

    static class CapabilityHandler extends DelegatingHandler {

        static final String CAPABILITY = "capability";

        static final String NAME = "name";

        Capability capability;

        public CapabilityHandler() {
            super(CAPABILITY);
            addChild(new CapabilityPropertyHandler(),
                new ChildElementHandler<CapabilityPropertyHandler>() {
                    @Override
                    public void childHanlded(CapabilityPropertyHandler child) {
                        String name = child.name;
                        String value = child.value;
                        String type = child.type;

                        capability.addProperty(name, value, type);
                    }
                });
        }

        @Override
        protected void handleAttributes(Attributes atts) throws SAXException {
            String name = getRequiredAttribute(atts, NAME);
            capability = new Capability(name);
        }

    }

    static class CapabilityPropertyHandler extends DelegatingHandler {

        static final String CAPABILITY_PROPERTY = "p";

        static final String NAME = "n";

        static final String VALUE = "v";

        static final String TYPE = "t";

        String name;

        String value;

        String type;

        public CapabilityPropertyHandler() {
            super(CAPABILITY_PROPERTY);
        }

        @Override
        protected void handleAttributes(Attributes atts) throws SAXException {
            name = getRequiredAttribute(atts, NAME);
            value = getRequiredAttribute(atts, VALUE);
            type = atts.getValue(TYPE);
        }
    }

    static class AbstractRequirementHandler extends DelegatingHandler {

        static final String NAME = "name";

        static final String OPTIONAL = "optional";

        static final String MULTIPLE = "multiple";

        static final String FILTER = "filter";

        Requirement requirement;

        OSGiFilter filter;

        public AbstractRequirementHandler(String name) {
            super(name);
        }

        @Override
        protected void handleAttributes(Attributes atts) throws SAXException {
            String name = getRequiredAttribute(atts, NAME);

            String filterText = atts.getValue(FILTER);
            filter = null;
            if (filterText != null) {
                try {
                    filter = OSGiFilterParser.parse(filterText);
                } catch (ParseException e) {
                    throw new SAXParseException("Requirement with illformed filter: " + filterText,
                            getLocator());
                }
            }

            Boolean optional = getOptionalBooleanAttribute(atts, OPTIONAL, null);
            Boolean multiple = getOptionalBooleanAttribute(atts, MULTIPLE, null);

            requirement = new Requirement(name, filter);
            if (optional != null) {
                requirement.setOptional(optional.booleanValue());
            }
            if (multiple != null) {
                requirement.setMultiple(multiple.booleanValue());
            }
        }

    }

    static class RequireHandler extends AbstractRequirementHandler {

        static final String REQUIRE = "require";

        public RequireHandler() {
            super(REQUIRE);
        }

    }

    static class ExtendHandler extends AbstractRequirementHandler {

        static final String EXTEND = "extend";

        public ExtendHandler() {
            super(EXTEND);
        }

    }
}
