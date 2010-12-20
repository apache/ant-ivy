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
import java.text.ParseException;

import org.apache.ivy.osgi.core.BundleInfo;
import org.apache.ivy.osgi.obr.filter.RequirementFilterParser;
import org.apache.ivy.osgi.repo.BundleRepo;
import org.apache.ivy.osgi.util.DelegetingHandler;
import org.apache.ivy.osgi.util.Version;
import org.apache.ivy.util.Message;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class OBRXMLParser {

    static final String REPOSITORY = "repository";

    static final String REPOSITORY_LASTMODIFIED = "lastmodified";

    static final String REPOSITORY_NAME = "name";

    static final String RESOURCE = "resource";

    static final String RESOURCE_ID = "id";

    static final String RESOURCE_PRESENTATION_NAME = "presentationname";

    static final String RESOURCE_SYMBOLIC_NAME = "symbolicname";

    static final String RESOURCE_URI = "uri";

    static final String RESOURCE_VERSION = "version";

    static final String CAPABILITY = "capability";

    static final String CAPABILITY_NAME = "name";

    static final String CAPABILITY_PROPERTY = "p";

    static final String CAPABILITY_PROPERTY_NAME = "n";

    static final String CAPABILITY_PROPERTY_VALUE = "v";

    static final String CAPABILITY_PROPERTY_TYPE = "t";

    static final String REQUIRE = "require";

    static final String REQUIRE_NAME = "name";

    static final String REQUIRE_OPTIONAL = "optional";

    static final String REQUIRE_MULTIPLE = "multiple";

    static final String REQUIRE_EXTEND = "extend";

    static final String REQUIRE_FILTER = "filter";

    static final String TRUE = "true";

    static final String FALSE = "false";

    public static BundleRepo parse(InputStream in) throws ParseException, IOException, SAXException {
        XMLReader reader;
        try {
            reader = XMLReaderFactory.createXMLReader();
        } catch (SAXException e) {
            throw new ParseException(e.getMessage(), 0);
        }
        RepositoryHandler handler = new RepositoryHandler();
        reader.setContentHandler(handler);
        reader.parse(new InputSource(in));
        return handler.repo;
    }

    private static class RepositoryHandler extends DelegetingHandler/* <DelegetingHandler<?>> */{

        BundleRepo repo;

        public RepositoryHandler() {
            super(REPOSITORY, null);
            new ResourceHandler(this);
        }

        protected void handleAttributes(Attributes atts) {
            repo = new BundleRepo();

            repo.setName(atts.getValue(REPOSITORY_NAME));

            String lastModified = atts.getValue(REPOSITORY_LASTMODIFIED);
            if (lastModified != null) {
                try {
                    repo.setLastModified(Long.valueOf(lastModified));
                } catch (NumberFormatException e) {
                    printWarning(this, "Incorrect last modified timestamp : " + lastModified
                            + ". It will be ignored.");
                }
            }

        }
    }

    private static class ResourceHandler extends DelegetingHandler/* <RepositoryHandler> */{

        BundleInfo bundleInfo;

        public ResourceHandler(RepositoryHandler repositoryHandler) {
            super(RESOURCE, repositoryHandler);
            new ResourceDescriptionHandler(this);
            new ResourceDocumentationHandler(this);
            new ResourceLicenseHandler(this);
            new ResourceSizeHandler(this);
            new CapabilityHandler(this);
            new RequireHandler(this);
        }

        protected void handleAttributes(Attributes atts) throws SAXException {
            String symbolicname = atts.getValue(RESOURCE_SYMBOLIC_NAME);
            if (symbolicname == null) {
                printError(this, "Resource with no symobilc name, skipping it.");
                skip();
                return;
            }

            String v = atts.getValue(RESOURCE_VERSION);
            Version version;
            if (v == null) {
                version = new Version(1, 0, 0, null);
            } else {
                try {
                    version = new Version(v);
                } catch (NumberFormatException e) {
                    printError(this, "Incorrect resource version: " + v + ". The resource "
                            + symbolicname + " is then ignored.");
                    skip();
                    return;
                }
            }

            bundleInfo = new BundleInfo(symbolicname, version);
            bundleInfo.setPresentationName(atts.getValue(RESOURCE_PRESENTATION_NAME));
            bundleInfo.setUri(atts.getValue(RESOURCE_URI));
            bundleInfo.setId(atts.getValue(RESOURCE_ID));
        }

        protected void doEndElement(String uri, String localName, String name) throws SAXException {
            ((RepositoryHandler) getParent()).repo.addBundle(bundleInfo);
        }

    }

    private static class ResourceDescriptionHandler extends DelegetingHandler/* <ResourceHandler> */{

        public ResourceDescriptionHandler(ResourceHandler resourceHandler) {
            super("description", resourceHandler);
            setBufferingChar(true);
        }

        protected void doEndElement(String uri, String localName, String name) throws SAXException {
            ((ResourceHandler) getParent()).bundleInfo.setDescription(getBufferedChars().trim());
        }
    }

    private static class ResourceDocumentationHandler extends DelegetingHandler/* <ResourceHandler> */{

        public ResourceDocumentationHandler(ResourceHandler resourceHandler) {
            super("documentation", resourceHandler);
            setBufferingChar(true);
        }

        protected void doEndElement(String uri, String localName, String name) throws SAXException {
            ((ResourceHandler) getParent()).bundleInfo.setDocumentation(getBufferedChars().trim());
        }
    }

    private static class ResourceLicenseHandler extends DelegetingHandler/* <ResourceHandler> */{

        public ResourceLicenseHandler(ResourceHandler resourceHandler) {
            super("license", resourceHandler);
            setBufferingChar(true);
        }

        protected void doEndElement(String uri, String localName, String name) throws SAXException {
            ((ResourceHandler) getParent()).bundleInfo.setLicense(getBufferedChars().trim());
        }
    }

    private static class ResourceSizeHandler extends DelegetingHandler/* <ResourceHandler> */{

        public ResourceSizeHandler(ResourceHandler resourceHandler) {
            super("size", resourceHandler);
            setBufferingChar(true);
        }

        protected void doEndElement(String uri, String localName, String name) throws SAXException {
            String size = getBufferedChars().trim();
            try {
                ((ResourceHandler) getParent()).bundleInfo.setSize(Integer.valueOf(size));
            } catch (NumberFormatException e) {
                printWarning(this, "Invalid size for the bundle"
                        + ((ResourceHandler) getParent()).bundleInfo.getSymbolicName() + ": "
                        + size + ". This size is then ignored.");
            }
        }
    }

    private static class CapabilityHandler extends DelegetingHandler/* <ResourceHandler> */{

        Capability capability;

        public CapabilityHandler(ResourceHandler resourceHandler) {
            super(CAPABILITY, resourceHandler);
            new CapabilityPropertyHandler(this);
        }

        protected void handleAttributes(Attributes atts) throws SAXException {
            String name = atts.getValue(CAPABILITY_NAME);
            if (name == null) {
                skipResourceOnError(this, "Capability with no name");
                return;
            }

            capability = new Capability(name);
        }

        protected void doEndElement(String uri, String localName, String name) throws SAXException {
            try {
                CapabilityAdapter.adapt(((ResourceHandler) getParent()).bundleInfo, capability);
            } catch (ParseException e) {
                skipResourceOnError(this, "Invalid capability: " + e.getMessage());
            }
        }
    }

    private static class CapabilityPropertyHandler extends DelegetingHandler/* <CapabilityHandler> */{

        public CapabilityPropertyHandler(CapabilityHandler capabilityHandler) {
            super(CAPABILITY_PROPERTY, capabilityHandler);
        }

        protected void handleAttributes(Attributes atts) throws SAXException {
            String name = atts.getValue(CAPABILITY_PROPERTY_NAME);
            if (name == null) {
                skipResourceOnError(this, "Capability property with no name on a capability "
                        + ((CapabilityHandler) getParent()).capability.getName());
                return;
            }
            String value = atts.getValue(CAPABILITY_PROPERTY_VALUE);
            if (value == null) {
                skipResourceOnError(this, "Capability property with no value on a capability "
                        + ((CapabilityHandler) getParent()).capability.getName());
                return;
            }
            String type = atts.getValue(CAPABILITY_PROPERTY_TYPE);

            ((CapabilityHandler) getParent()).capability.addProperty(name, value, type);
        }
    }

    private static class RequireHandler extends DelegetingHandler/* <ResourceHandler> */{

        private Requirement requirement;

        private RequirementFilter filter;

        public RequireHandler(ResourceHandler resourceHandler) {
            super(REQUIRE, resourceHandler);
        }

        protected void handleAttributes(Attributes atts) throws SAXException {
            String name = atts.getValue(REQUIRE_NAME);
            if (name == null) {
                skipResourceOnError(this, "Requirement with no name");
                return;
            }

            String filterText = atts.getValue(REQUIRE_FILTER);
            filter = null;
            if (filterText != null) {
                try {
                    filter = RequirementFilterParser.parse(filterText);
                } catch (ParseException e) {
                    skipResourceOnError(this, "Requirement with illformed filter: " + filterText);
                    return;
                }
            }

            Boolean optional = null;
            try {
                optional = parseBoolean(atts, REQUIRE_OPTIONAL);
            } catch (ParseException e) {
                skipResourceOnError(this,
                    "Requirement with unrecognised optional: " + e.getMessage());
                return;
            }

            Boolean multiple = null;
            try {
                multiple = parseBoolean(atts, REQUIRE_MULTIPLE);
            } catch (ParseException e) {
                skipResourceOnError(this,
                    "Requirement with unrecognised multiple: " + e.getMessage());
                return;
            }

            Boolean extend = null;
            try {
                extend = parseBoolean(atts, REQUIRE_EXTEND);
            } catch (ParseException e) {
                skipResourceOnError(this, "Requirement with unrecognised extend: " + e.getMessage());
                return;
            }

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

        protected void doEndElement(String uri, String localName, String name) throws SAXException {
            try {
                RequirementAdapter.adapt(((ResourceHandler) getParent()).bundleInfo, requirement);
            } catch (UnsupportedFilterException e) {
                skipResourceOnError(this,
                    "Unsupported requirement filter: " + filter + " (" + e.getMessage() + ")");
            } catch (ParseException e) {
                skipResourceOnError(this,
                    "Error in the requirement filter on the bundle: " + e.getMessage());
            }
        }
    }

    private static Boolean parseBoolean(Attributes atts, String name) throws ParseException {
        String v = atts.getValue(name);
        if (v == null) {
            return null;
        }
        if (TRUE.equalsIgnoreCase(v)) {
            return Boolean.TRUE;
        } else if (FALSE.equalsIgnoreCase(v)) {
            return Boolean.FALSE;
        } else {
            throw new ParseException("Unparsable boolean value: " + v, 0);
        }
    }

    private static void skipResourceOnError(DelegetingHandler/* <?> */handler, String message) {
        DelegetingHandler/* <?> */resourceHandler = handler;
        while (!(resourceHandler instanceof ResourceHandler)) {
            resourceHandler = resourceHandler.getParent();
        }
        BundleInfo bundleInfo = ((ResourceHandler) resourceHandler).bundleInfo;
        printError(handler, message + ". The resource " + bundleInfo.getSymbolicName() + "/"
                + bundleInfo.getVersion() + " is then ignored.");
        resourceHandler.skip();
    }

    private static void printError(DelegetingHandler/* <?> */handler, String message) {
        Message.error(getLocation(handler.getLocator()) + message);
    }

    private static void printWarning(DelegetingHandler/* <?> */handler, String message) {
        Message.warn(getLocation(handler.getLocator()) + message);
    }

    private static String getLocation(Locator locator) {
        if (locator == null) {
            return "";
        }
        return "[line " + locator.getLineNumber() + " col. " + locator.getColumnNumber() + "] ";
    }
}
