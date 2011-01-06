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
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

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

    public static BundleRepoDescriptor parse(InputStream in) throws ParseException, IOException,
            SAXException {
        RepositoryHandler handler = new RepositoryHandler();
        try {
            XMLHelper.parse(in, null, handler, null);
        } catch (ParserConfigurationException e) {
            ParseException exc = new ParseException(e.getMessage(), 0);
            exc.initCause(e);
            throw exc;
        }
        return handler.repo;
    }

    private static class RepositoryHandler extends DelegetingHandler {

        BundleRepoDescriptor repo;

        public RepositoryHandler() {
            super(REPOSITORY);
            addChild(new ResourceHandler(), new ChildElementHandler() {
                public void childHanlded(DelegetingHandler child) {
                    repo.addBundle(((ResourceHandler) child).bundleInfo);
                }
            });
        }

        protected void handleAttributes(Attributes atts) {
            repo = new BundleRepoDescriptor(ExecutionEnvironmentProfileProvider.getInstance());

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

    private static class ResourceHandler extends DelegetingHandler {

        BundleInfo bundleInfo;

        public ResourceHandler() {
            super(RESOURCE);
            addChild(new ResourceDescriptionHandler(), new ChildElementHandler() {
                public void childHanlded(DelegetingHandler child) {
                    bundleInfo.setDescription(((ResourceDescriptionHandler) child)
                            .getBufferedChars().trim());
                }
            });
            addChild(new ResourceDocumentationHandler(), new ChildElementHandler() {
                public void childHanlded(DelegetingHandler child) {
                    bundleInfo.setDocumentation(((ResourceDocumentationHandler) child)
                            .getBufferedChars().trim());
                }
            });
            addChild(new ResourceLicenseHandler(), new ChildElementHandler() {
                public void childHanlded(DelegetingHandler child) {
                    bundleInfo.setLicense(((ResourceLicenseHandler) child).getBufferedChars()
                            .trim());
                }
            });
            addChild(new ResourceSizeHandler(), new ChildElementHandler() {
                public void childHanlded(DelegetingHandler child) {
                    String size = ((ResourceSizeHandler) child).getBufferedChars().trim();
                    try {
                        bundleInfo.setSize(Integer.valueOf(size));
                    } catch (NumberFormatException e) {
                        printWarning(child,
                            "Invalid size for the bundle" + bundleInfo.getSymbolicName() + ": "
                                    + size + ". This size is then ignored.");
                    }
                }
            });
            addChild(new CapabilityHandler(), new ChildElementHandler() {
                public void childHanlded(DelegetingHandler child) {

                    try {
                        CapabilityAdapter.adapt(bundleInfo, ((CapabilityHandler) child).capability);
                    } catch (ParseException e) {
                        skipResourceOnError(child, "Invalid capability: " + e.getMessage());
                    }
                }
            });
            addChild(new RequireHandler(), new ChildElementHandler() {
                public void childHanlded(DelegetingHandler child) {
                    try {
                        RequirementAdapter.adapt(bundleInfo, ((RequireHandler) child).requirement);
                    } catch (UnsupportedFilterException e) {
                        skipResourceOnError(child, "Unsupported requirement filter: "
                                + ((RequireHandler) child).filter + " (" + e.getMessage() + ")");
                    } catch (ParseException e) {
                        skipResourceOnError(child,
                            "Error in the requirement filter on the bundle: " + e.getMessage());
                    }
                }
            });
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

    }

    private static class ResourceDescriptionHandler extends DelegetingHandler {

        public ResourceDescriptionHandler() {
            super("description");
            setBufferingChar(true);
        }

    }

    private static class ResourceDocumentationHandler extends DelegetingHandler {

        public ResourceDocumentationHandler() {
            super("documentation");
            setBufferingChar(true);
        }
    }

    private static class ResourceLicenseHandler extends DelegetingHandler {

        public ResourceLicenseHandler() {
            super("license");
            setBufferingChar(true);
        }

    }

    private static class ResourceSizeHandler extends DelegetingHandler {

        public ResourceSizeHandler() {
            super("size");
            setBufferingChar(true);
        }
    }

    private static class CapabilityHandler extends DelegetingHandler {

        Capability capability;

        public CapabilityHandler() {
            super(CAPABILITY);
            addChild(new CapabilityPropertyHandler(), new ChildElementHandler() {
                public void childHanlded(DelegetingHandler child) {
                    String name = ((CapabilityPropertyHandler) child).name;
                    if (name == null) {
                        skipResourceOnError(
                            child,
                            "Capability property with no name on a capability "
                                    + capability.getName());
                        return;
                    }
                    String value = ((CapabilityPropertyHandler) child).value;
                    if (value == null) {
                        skipResourceOnError(
                            child,
                            "Capability property with no value on a capability "
                                    + capability.getName());
                        return;
                    }
                    String type = ((CapabilityPropertyHandler) child).type;

                    capability.addProperty(name, value, type);
                }
            });
        }

        protected void handleAttributes(Attributes atts) throws SAXException {
            String name = atts.getValue(CAPABILITY_NAME);
            if (name == null) {
                skipResourceOnError(this, "Capability with no name");
                return;
            }

            capability = new Capability(name);
        }

    }

    private static class CapabilityPropertyHandler extends DelegetingHandler {

        String name;

        String value;

        String type;

        public CapabilityPropertyHandler() {
            super(CAPABILITY_PROPERTY);
        }

        protected void handleAttributes(Attributes atts) throws SAXException {
            name = atts.getValue(CAPABILITY_PROPERTY_NAME);
            value = atts.getValue(CAPABILITY_PROPERTY_VALUE);
            type = atts.getValue(CAPABILITY_PROPERTY_TYPE);
        }
    }

    private static class RequireHandler extends DelegetingHandler {

        private Requirement requirement;

        private RequirementFilter filter;

        public RequireHandler() {
            super(REQUIRE);
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
