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

import org.apache.ivy.osgi.core.BundleInfo;
import org.apache.ivy.osgi.core.ExecutionEnvironmentProfileProvider;
import org.apache.ivy.osgi.obr.filter.RequirementFilterParser;
import org.apache.ivy.osgi.repo.BundleRepoDescriptor;
import org.apache.ivy.osgi.util.DelegetingHandler;
import org.apache.ivy.osgi.util.Version;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.XMLHelper;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class OBRXMLParser {

    public static BundleRepoDescriptor parse(URI baseUri, InputStream in) throws ParseException,
            IOException, SAXException {
        RepositoryHandler handler = new RepositoryHandler(baseUri);
        try {
            XMLHelper.parse(in, null, handler, null);
        } catch (ParserConfigurationException e) {
            throw new SAXException(e);
        }
        return handler.repo;
    }

    static class RepositoryHandler extends DelegetingHandler {

        static final String REPOSITORY = "repository";

        static final String LASTMODIFIED = "lastmodified";

        static final String NAME = "name";

        BundleRepoDescriptor repo;

        private final URI baseUri;

        public RepositoryHandler(URI baseUri) {
            super(REPOSITORY);
            this.baseUri = baseUri;
            addChild(new ResourceHandler(), new ChildElementHandler() {
                public void childHanlded(DelegetingHandler child) {
                    repo.addBundle(((ResourceHandler) child).bundleInfo);
                }
            });
        }

        protected void handleAttributes(Attributes atts) {
            repo = new BundleRepoDescriptor(baseUri,
                    ExecutionEnvironmentProfileProvider.getInstance());

            repo.setName(atts.getValue(NAME));

            try {
                Long lastModified = getOptionalLongAttribute(atts, LASTMODIFIED, null);
                repo.setLastModified(lastModified);
            } catch (SAXParseException e) {
                log(Message.MSG_WARN, e.getMessage() + ". It will be ignored.");
            }

        }
    }

    static class ResourceHandler extends DelegetingHandler {

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

            addChild(new ResourceDescriptionHandler(), new ChildElementHandler() {
                public void childHanlded(DelegetingHandler child) {
                    bundleInfo.setDescription(child.getBufferedChars().trim());
                }
            });
            addChild(new ResourceDocumentationHandler(), new ChildElementHandler() {
                public void childHanlded(DelegetingHandler child) {
                    bundleInfo.setDocumentation(child.getBufferedChars().trim());
                }
            });
            addChild(new ResourceLicenseHandler(), new ChildElementHandler() {
                public void childHanlded(DelegetingHandler child) {
                    bundleInfo.setLicense(child.getBufferedChars().trim());
                }
            });
            addChild(new ResourceSizeHandler(), new ChildElementHandler() {
                public void childHanlded(DelegetingHandler child) {
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
            addChild(new CapabilityHandler(), new ChildElementHandler() {
                public void childHanlded(DelegetingHandler child) throws SAXParseException {

                    try {
                        CapabilityAdapter.adapt(bundleInfo, ((CapabilityHandler) child).capability);
                    } catch (ParseException e) {
                        throw new SAXParseException("Invalid capability: " + e.getMessage(), child
                                .getLocator());
                    }
                }
            });
            addChild(new RequireHandler(), new ChildElementHandler() {
                public void childHanlded(DelegetingHandler child) throws SAXParseException {
                    try {
                        RequirementAdapter.adapt(bundleInfo, ((RequireHandler) child).requirement);
                    } catch (UnsupportedFilterException e) {
                        throw new SAXParseException("Unsupported requirement filter: "
                                + ((RequireHandler) child).filter + " (" + e.getMessage() + ")",
                                getLocator());
                    } catch (ParseException e) {
                        throw new SAXParseException(
                                "Error in the requirement filter on the bundle: " + e.getMessage(),
                                getLocator());
                    }
                }
            });
        }

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
                    bundleInfo.setUri(new URI(uri));
                } catch (URISyntaxException e) {
                    log(Message.MSG_ERR, "Incorrect uri " + uri + ". The resource " + symbolicname
                            + " is then ignored.");
                    skip();
                    return;
                }
            }
            bundleInfo.setId(atts.getValue(ID));
        }

        protected String getCurrentElementIdentifier() {
            return bundleInfo.getSymbolicName() + "/" + bundleInfo.getVersion();
        }

    }

    static class ResourceDescriptionHandler extends DelegetingHandler {

        static final String DESCRIPTION = "description";

        public ResourceDescriptionHandler() {
            super(DESCRIPTION);
            setBufferingChar(true);
        }

    }

    static class ResourceDocumentationHandler extends DelegetingHandler {

        static final String DOCUMENTATION = "documentation";

        public ResourceDocumentationHandler() {
            super(DOCUMENTATION);
            setBufferingChar(true);
        }
    }

    static class ResourceLicenseHandler extends DelegetingHandler {

        static final String LICENSE = "license";

        public ResourceLicenseHandler() {
            super(LICENSE);
            setBufferingChar(true);
        }

    }

    static class ResourceSizeHandler extends DelegetingHandler {

        static final String SIZE = "size";

        public ResourceSizeHandler() {
            super(SIZE);
            setBufferingChar(true);
        }
    }

    static class CapabilityHandler extends DelegetingHandler {

        static final String CAPABILITY = "capability";

        static final String NAME = "name";

        Capability capability;

        public CapabilityHandler() {
            super(CAPABILITY);
            addChild(new CapabilityPropertyHandler(), new ChildElementHandler() {
                public void childHanlded(DelegetingHandler child) {
                    String name = ((CapabilityPropertyHandler) child).name;
                    String value = ((CapabilityPropertyHandler) child).value;
                    String type = ((CapabilityPropertyHandler) child).type;

                    capability.addProperty(name, value, type);
                }
            });
        }

        protected void handleAttributes(Attributes atts) throws SAXException {
            String name = getRequiredAttribute(atts, NAME);
            capability = new Capability(name);
        }

    }

    static class CapabilityPropertyHandler extends DelegetingHandler {

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

        protected void handleAttributes(Attributes atts) throws SAXException {
            name = getRequiredAttribute(atts, NAME);
            value = getRequiredAttribute(atts, VALUE);
            type = atts.getValue(TYPE);
        }
    }

    static class RequireHandler extends DelegetingHandler {

        static final String REQUIRE = "require";

        static final String NAME = "name";

        static final String OPTIONAL = "optional";

        static final String MULTIPLE = "multiple";

        static final String EXTEND = "extend";

        static final String FILTER = "filter";

        private Requirement requirement;

        private RequirementFilter filter;

        public RequireHandler() {
            super(REQUIRE);
        }

        protected void handleAttributes(Attributes atts) throws SAXException {
            String name = getRequiredAttribute(atts, NAME);

            String filterText = atts.getValue(FILTER);
            filter = null;
            if (filterText != null) {
                try {
                    filter = RequirementFilterParser.parse(filterText);
                } catch (ParseException e) {
                    throw new SAXParseException("Requirement with illformed filter: " + filterText,
                            getLocator());
                }
            }

            Boolean optional = getOptionalBooleanAttribute(atts, OPTIONAL, null);
            Boolean multiple = getOptionalBooleanAttribute(atts, MULTIPLE, null);
            Boolean extend = getOptionalBooleanAttribute(atts, EXTEND, null);

            requirement = new Requirement(name, filter);
            if (optional != null) {
                requirement.setOptional(optional.booleanValue());
            }
            if (multiple != null) {
                requirement.setMultiple(multiple.booleanValue());
            }
            if (extend != null) {
                requirement.setExtend(extend.booleanValue());
            }
        }

    }

}
