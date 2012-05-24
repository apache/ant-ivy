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
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.ivy.osgi.core.BundleCapability;
import org.apache.ivy.osgi.core.BundleInfo;
import org.apache.ivy.osgi.core.BundleRequirement;
import org.apache.ivy.osgi.core.ExportPackage;
import org.apache.ivy.osgi.p2.PropertiesParser.PropertiesHandler;
import org.apache.ivy.osgi.util.DelegetingHandler;
import org.apache.ivy.osgi.util.Version;
import org.apache.ivy.osgi.util.VersionRange;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.XMLHelper;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class P2MetadataParser implements XMLInputParser {

    private final P2Descriptor p2Descriptor;

    public P2MetadataParser(P2Descriptor p2Descriptor) {
        this.p2Descriptor = p2Descriptor;
    }

    public void parse(InputStream in) throws ParseException, IOException, SAXException {
        RepositoryHandler handler = new RepositoryHandler(p2Descriptor);
        try {
            XMLHelper.parse(in, null, handler, null);
        } catch (ParserConfigurationException e) {
            throw new SAXException(e);
        }
    }

    static class RepositoryHandler extends DelegetingHandler {

        private static final String REPOSITORY = "repository";

        // private static final String NAME = "name";
        //
        // private static final String TYPE = "type";
        //
        // private static final String VERSION = "version";
        //
        // private static final String DESCRIPTION = "description";
        //
        // private static final String PROVIDER = "provider";

        public RepositoryHandler(final P2Descriptor p2Descriptor) {
            super(REPOSITORY);
            addChild(new PropertiesHandler(), new ChildElementHandler() {
                public void childHanlded(DelegetingHandler child) {
                    Map properties = ((PropertiesHandler) child).properties;
                    String timestamp = (String) properties.get("p2.timestamp");
                    if (timestamp != null) {
                        p2Descriptor.setTimestamp(Long.parseLong(timestamp));
                    }
                }
            });
            addChild(new UnitsHandler(), new ChildElementHandler() {
                public void childHanlded(DelegetingHandler child) {
                    Iterator it = ((UnitsHandler) child).bundles.iterator();
                    while (it.hasNext()) {
                        p2Descriptor.addBundle((BundleInfo) it.next());
                    }
                }
            });
            addChild(new ReferencesHandler(), new ChildElementHandler() {
                public void childHanlded(DelegetingHandler child) {
                }
            });
        }

        // protected void handleAttributes(Attributes atts) {
        // String name = atts.getValue(NAME);
        // String type = atts.getValue(TYPE);
        // String version = atts.getValue(VERSION);
        // String description = atts.getValue(DESCRIPTION);
        // String provider = atts.getValue(PROVIDER);
        // }
    }

    static class ReferencesHandler extends DelegetingHandler {

        private static final String REFERENCES = "references";

        private static final String SIZE = "size";

        List/* <URI> */repositoryUris;

        public ReferencesHandler() {
            super(REFERENCES);
            addChild(new RepositoryReferenceHandler(), new ChildElementHandler() {
                public void childHanlded(DelegetingHandler child) {
                    repositoryUris.add(((RepositoryReferenceHandler) child).uri);
                }
            });
        }

        protected void handleAttributes(Attributes atts) throws SAXException {
            int size = Integer.parseInt(atts.getValue(SIZE));
            repositoryUris = new ArrayList(size);
        }
    }

    static class RepositoryReferenceHandler extends DelegetingHandler {

        private static final String REPOSITORY = "repository";

        private static final String TYPE = "type";

        private static final String OPTIONS = "options";

        private static final String NAME = "name";

        private static final String URI = "uri";

        private static final String URL = "url";

        public RepositoryReferenceHandler() {
            super(REPOSITORY);
        }

        int type;

        int options;

        String name;

        URI uri;

        protected void handleAttributes(Attributes atts) throws SAXException {
            type = Integer.parseInt(atts.getValue(TYPE));
            options = Integer.parseInt(atts.getValue(OPTIONS));
            name = atts.getValue(NAME);

            try {
                String uriAtt = atts.getValue(URI);
                String url = atts.getValue(URL);
                if (uri != null) {
                    uri = new URI(uriAtt);
                } else if (url != null) {
                    uri = new URI(url);
                }
            } catch (URISyntaxException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    static class UnitsHandler extends DelegetingHandler {

        private static final String UNITS = "units";

        private static final String SIZE = "size";

        List bundles;

        public UnitsHandler() {
            super(UNITS);
            addChild(new UnitHandler(), new ChildElementHandler() {
                public void childHanlded(DelegetingHandler child) {
                    BundleInfo bundleInfo = ((UnitHandler) child).bundleInfo;
                    if (!bundleInfo.getCapabilities().isEmpty()) {
                        bundles.add(((UnitHandler) child).bundleInfo);
                    }
                }
            });
        }

        protected void handleAttributes(Attributes atts) {
            int size = Integer.parseInt(atts.getValue(SIZE));
            bundles = new ArrayList(size);
        }

    }

    static class UnitHandler extends DelegetingHandler {

        private static final String UNIT = "unit";

        private static final String ID = "id";

        private static final String VERSION = "version";

        // private static final String SINGLETON = "singleton";

        BundleInfo bundleInfo;

        public UnitHandler() {
            super(UNIT);
            // addChild(new UpdateHandler(), new ChildElementHandler() {
            // public void childHanlded(DelegetingHandler child) {
            // }
            // });
            addChild(new PropertiesHandler(), new ChildElementHandler() {
                public void childHanlded(DelegetingHandler child) {
                    Map properties = ((PropertiesHandler) child).properties;
                    String category = (String) properties
                            .get("org.eclipse.equinox.p2.type.category");
                    if (category != null && Boolean.valueOf(category).booleanValue()) {
                        // this is a category definition, this is useless, skip this unit
                        child.getParent().skip();
                    }
                }
            });
            addChild(new ProvidesHandler(), new ChildElementHandler() {
                public void childHanlded(DelegetingHandler child) {
                    Iterator it = ((ProvidesHandler) child).capabilities.iterator();
                    while (it.hasNext()) {
                        bundleInfo.addCapability((BundleCapability) it.next());
                    }
                }
            });
            addChild(new FilterHandler(), new ChildElementHandler() {
                public void childHanlded(DelegetingHandler child) {
                }
            });
            addChild(new RequiresHandler(), new ChildElementHandler() {
                public void childHanlded(DelegetingHandler child) {
                    Iterator it = ((RequiresHandler) child).requirements.iterator();
                    while (it.hasNext()) {
                        bundleInfo.addRequirement((BundleRequirement) it.next());
                    }
                }
            });
            addChild(new HostRequirementsHandler(), new ChildElementHandler() {
                public void childHanlded(DelegetingHandler child) {
                }
            });
            addChild(new MetaRequirementsHandler(), new ChildElementHandler() {
                public void childHanlded(DelegetingHandler child) {
                }
            });
            addChild(new ArtifactsHandler(), new ChildElementHandler() {
                public void childHanlded(DelegetingHandler child) {
                }
            });
            // addChild(new TouchpointHandler(), new ChildElementHandler() {
            // public void childHanlded(DelegetingHandler child) {
            // }
            // });
            // addChild(new TouchpointDataHandler(), new ChildElementHandler() {
            // public void childHanlded(DelegetingHandler child) {
            // }
            // });
            // addChild(new LicensesHandler(), new ChildElementHandler() {
            // public void childHanlded(DelegetingHandler child) {
            // }
            // });
            // addChild(new CopyrightHandler(), new ChildElementHandler() {
            // public void childHanlded(DelegetingHandler child) {
            // }
            // });
            addChild(new ChangesHandler(), new ChildElementHandler() {
                public void childHanlded(DelegetingHandler child) {
                }
            });

        }

        protected void handleAttributes(Attributes atts) throws SAXException {
            String id = atts.getValue(ID);
            String version = atts.getValue(VERSION);
            // Boolean singleton = Boolean.valueOf(atts.getValue(SINGLETON));
            try {
                bundleInfo = new BundleInfo(id, new Version(version));
            } catch (ParseException e) {
                throw new SAXException("Incorrect version on bundle '" + id + "': " + version
                        + " (" + e.getMessage() + ")");
            }
        }

    }

    // static class UpdateHandler extends DelegetingHandler {
    //
    // private static final String UPDATE = "update";
    //
    // private static final String ID = "id";
    //
    // private static final String RANGE = "range";
    //
    // private static final String SEVERITY = "severity";
    //
    // public UpdateHandler() {
    // super(UPDATE);
    // }
    //
    // protected void handleAttributes(Attributes atts) {
    // String id = atts.getValue(ID);
    // String range = atts.getValue(RANGE);
    // String severity = atts.getValue(SEVERITY);
    // }
    //
    // }

    static class FilterHandler extends DelegetingHandler {

        private static final String FILTER = "filter";

        public FilterHandler() {
            super(FILTER);
            setBufferingChar(true);
        }

    }

    private static String namespace2Type(String namespace) {
        if (namespace.equals("java.package")) {
            return BundleInfo.PACKAGE_TYPE;
        }
        if (namespace.equals("osgi.bundle")) {
            return BundleInfo.BUNDLE_TYPE;
        }
        return null;
    }

    static class ProvidesHandler extends DelegetingHandler {

        private static final String PROVIDES = "provides";

        private static final String SIZE = "size";

        List capabilities;

        public ProvidesHandler() {
            super(PROVIDES);
            addChild(new ProvidedHandler(), new ChildElementHandler() {
                public void childHanlded(DelegetingHandler child) {
                    String name = ((ProvidedHandler) child).name;
                    Version version = ((ProvidedHandler) child).version;
                    String type = namespace2Type(((ProvidedHandler) child).namespace);
                    if (type == null) {
                        Message.debug("Unsupported provided capability "
                                + ((ProvidedHandler) child).namespace + " " + name + " " + version);
                        return;
                    }
                    BundleCapability capability;
                    if (type == BundleInfo.PACKAGE_TYPE) {
                        capability = new ExportPackage(name, version);
                    } else {
                        capability = new BundleCapability(type, name, version);
                    }
                    capabilities.add(capability);
                }
            });
        }

        protected void handleAttributes(Attributes atts) {
            int size = Integer.parseInt(atts.getValue(SIZE));
            capabilities = new ArrayList(size);
        }

    }

    static class ProvidedHandler extends DelegetingHandler {

        private static final String PROVIDED = "provided";

        private static final String NAMESPACE = "namespace";

        private static final String NAME = "name";

        private static final String VERSION = "version";

        String namespace;

        String name;

        Version version;

        public ProvidedHandler() {
            super(PROVIDED);
        }

        protected void handleAttributes(Attributes atts) throws SAXException {
            namespace = atts.getValue(NAMESPACE);
            name = atts.getValue(NAME);
            try {
                version = new Version(atts.getValue(VERSION));
            } catch (ParseException e) {
                throw new SAXException("Incorrect version on provided capability: " + version
                        + " (" + e.getMessage() + ")");
            }
        }

    }

    static abstract class AbstractRequirementHandler extends DelegetingHandler {

        private static final String SIZE = "size";

        List requirements;

        public AbstractRequirementHandler(String name) {
            super(name);
            addChild(new RequiredHandler(), new ChildElementHandler() {
                public void childHanlded(DelegetingHandler child) {
                    String name = ((RequiredHandler) child).name;
                    VersionRange range = ((RequiredHandler) child).range;
                    String type = namespace2Type(((RequiredHandler) child).namespace);
                    if (type == null) {
                        Message.debug("Unsupported required capability "
                                + ((RequiredHandler) child).namespace + " " + name + " " + range);
                    } else {
                        requirements.add(new BundleRequirement(type, name, range, null));
                    }
                }
            });
        }

        protected void handleAttributes(Attributes atts) {
            int size = Integer.parseInt(atts.getValue(SIZE));
            requirements = new ArrayList(size);
        }

    }

    static class RequiresHandler extends AbstractRequirementHandler {

        private static final String REQUIRES = "requires";

        public RequiresHandler() {
            super(REQUIRES);
        }

    }

    static class RequiredHandler extends DelegetingHandler {

        private static final String REQUIRED = "required";

        private static final String NAMESPACE = "namespace";

        private static final String NAME = "name";

        private static final String RANGE = "range";

        String namespace;

        String name;

        VersionRange range;

        String filter;

        public RequiredHandler() {
            super(REQUIRED);
            addChild(new FilterHandler(), new ChildElementHandler() {
                public void childHanlded(DelegetingHandler child) {
                    filter = child.getBufferedChars().trim();
                }
            });
        }

        protected void handleAttributes(Attributes atts) {
            namespace = atts.getValue(NAMESPACE);
            name = atts.getValue(NAME);
            try {
                range = new VersionRange(atts.getValue(RANGE));
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }

    }

    static class HostRequirementsHandler extends AbstractRequirementHandler {

        private static final String HOST_REQUIREMENTS = "hostRequirements";

        public HostRequirementsHandler() {
            super(HOST_REQUIREMENTS);
        }

    }

    static class MetaRequirementsHandler extends AbstractRequirementHandler {

        private static final String META_REQUIREMENTS = "metaRequirements";

        public MetaRequirementsHandler() {
            super(META_REQUIREMENTS);
        }

    }

    static class ArtifactsHandler extends DelegetingHandler {

        private static final String ARTIFACTS = "artifacts";

        private static final String SIZE = "size";

        List artifacts;

        public ArtifactsHandler() {
            super(ARTIFACTS);
            addChild(new ArtifactHandler(), new ChildElementHandler() {
                public void childHanlded(DelegetingHandler child) {
                    artifacts.add(((ArtifactHandler) child).artifact);
                }
            });
        }

        protected void handleAttributes(Attributes atts) {
            int size = Integer.parseInt(atts.getValue(SIZE));
            artifacts = new ArrayList(size);
        }

    }

    static class ArtifactHandler extends DelegetingHandler/* <ArtifactsHandler> */{

        private static final String ARTIFACT = "artifact";

        private static final String ID = "id";

        private static final String VERSION = "version";

        private static final String CLASSIFIER = "classifier";

        P2Artifact artifact;

        public ArtifactHandler() {
            super(ARTIFACT);
        }

        protected void handleAttributes(Attributes atts) throws SAXException {
            String id = atts.getValue(ID);
            String version = atts.getValue(VERSION);
            String classifier = atts.getValue(CLASSIFIER);
            try {
                artifact = new P2Artifact(id, new Version(version), classifier);
            } catch (ParseException e) {
                throw new SAXException("Incorrect version on artifact '" + id + "': " + version
                        + " (" + e.getMessage() + ")");
            }
        }

    }

    //
    // static class TouchpointHandler extends DelegetingHandler {
    //
    // private static final String TOUCHPOINT = "touchpoint";
    //
    // private static final String ID = "id";
    //
    // private static final String VERSION = "version";
    //
    // public TouchpointHandler() {
    // super(TOUCHPOINT);
    // }
    //
    // protected void handleAttributes(Attributes atts) {
    // String id = atts.getValue(ID);
    // String version = atts.getValue(VERSION);
    // }
    //
    // }
    //
    // static class TouchpointDataHandler extends DelegetingHandler {
    //
    // private static final String TOUCHPOINTDATA = "touchpointData";
    //
    // private static final String SIZE = "size";
    //
    // public TouchpointDataHandler() {
    // super(TOUCHPOINTDATA);
    // addChild(new InstructionsHandler(), new ChildElementHandler() {
    // public void childHanlded(DelegetingHandler child) {
    // }
    // });
    // }
    //
    // protected void handleAttributes(Attributes atts) {
    // String size = atts.getValue(SIZE);
    // }
    //
    // }
    //
    // static class InstructionsHandler extends DelegetingHandler {
    //
    // private static final String INSTRUCTIONS = "instructions";
    //
    // private static final String SIZE = "size";
    //
    // public InstructionsHandler() {
    // super(INSTRUCTIONS);
    // addChild(new InstructionHandler(), new ChildElementHandler() {
    // public void childHanlded(DelegetingHandler child) {
    // }
    // });
    // }
    //
    // protected void handleAttributes(Attributes atts) {
    // String size = atts.getValue(SIZE);
    // }
    //
    // }
    //
    // static class InstructionHandler extends DelegetingHandler {
    //
    // private static final String INSTRUCTION = "instruction";
    //
    // private static final String KEY = "key";
    //
    // public InstructionHandler() {
    // super(INSTRUCTION);
    // setBufferingChar(true);
    // }
    //
    // protected void handleAttributes(Attributes atts) {
    // String size = atts.getValue(KEY);
    // }
    //
    // }
    //
    // static class LicensesHandler extends DelegetingHandler {
    //
    // private static final String LICENSES = "licenses";
    //
    // private static final String SIZE = "size";
    //
    // public LicensesHandler() {
    // super(LICENSES);
    // addChild(new LicenseHandler(), new ChildElementHandler() {
    // public void childHanlded(DelegetingHandler child) {
    // }
    // });
    // }
    //
    // protected void handleAttributes(Attributes atts) {
    // String size = atts.getValue(SIZE);
    // }
    //
    // }
    //
    // static class LicenseHandler extends DelegetingHandler {
    //
    // private static final String LICENSE = "license";
    //
    // private static final String URI = "uri";
    //
    // private static final String URL = "url";
    //
    // public LicenseHandler() {
    // super(LICENSE);
    // setBufferingChar(true);
    // }
    //
    // protected void handleAttributes(Attributes atts) {
    // String uri = atts.getValue(URI);
    // String url = atts.getValue(URL);
    // }
    //
    // }
    //
    // static class CopyrightHandler extends DelegetingHandler {
    //
    // private static final String COPYRIGHT = "copyright";
    //
    // private static final String URI = "uri";
    //
    // private static final String URL = "url";
    //
    // public CopyrightHandler() {
    // super(COPYRIGHT);
    // }
    //
    // protected void handleAttributes(Attributes atts) {
    // String uri = atts.getValue(URI);
    // String url = atts.getValue(URL);
    // }
    //
    // }

    static class ChangesHandler extends DelegetingHandler {

        private static final String CHANGES = "changes";

        // private static final String SIZE = "size";

        public ChangesHandler() {
            super(CHANGES);
            addChild(new ChangeHandler(), new ChildElementHandler() {
                public void childHanlded(DelegetingHandler child) {
                }
            });
        }

        protected void handleAttributes(Attributes atts) {
            // int size = Integer.parseInt(atts.getValue(SIZE));
        }
    }

    static class ChangeHandler extends DelegetingHandler {

        private static final String CHANGE = "change";

        public ChangeHandler() {
            super(CHANGE);
        }
    }

    static class FromHandler extends AbstractRequirementHandler {

        private static final String FROM = "from";

        public FromHandler() {
            super(FROM);
        }

    }

    static class ToHandler extends AbstractRequirementHandler {

        private static final String TO = "to";

        public ToHandler() {
            super(TO);
        }

    }

    static class PatchScopeHandler extends DelegetingHandler {

        private static final String PATCH_SCOPE = "patchScope";

        // private static final String SIZE = "size";

        public PatchScopeHandler() {
            super(PATCH_SCOPE);
            addChild(new PatchScopeHandler(), new ChildElementHandler() {
                public void childHanlded(DelegetingHandler child) {
                }
            });
        }

        protected void handleAttributes(Attributes atts) {
            // int size = Integer.parseInt(atts.getValue(SIZE));
        }
    }

    static class ScopeHandler extends DelegetingHandler {

        private static final String SCOPE = "scope";

        public ScopeHandler() {
            super(SCOPE);
            addChild(new RequiresHandler(), new ChildElementHandler() {
                public void childHanlded(DelegetingHandler child) {
                }
            });
        }
    }

    static class LifeCycleHandler extends AbstractRequirementHandler {

        private static final String LIFE_CYCLE = "lifeCycle";

        public LifeCycleHandler() {
            super(LIFE_CYCLE);
        }
    }

}
