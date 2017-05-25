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
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.ivy.osgi.core.BundleCapability;
import org.apache.ivy.osgi.core.BundleInfo;
import org.apache.ivy.osgi.core.BundleRequirement;
import org.apache.ivy.osgi.core.ExportPackage;
import org.apache.ivy.osgi.core.ManifestParser;
import org.apache.ivy.osgi.p2.PropertiesParser.PropertiesHandler;
import org.apache.ivy.osgi.util.DelegatingHandler;
import org.apache.ivy.osgi.util.Version;
import org.apache.ivy.osgi.util.VersionRange;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.XMLHelper;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class P2MetadataParser implements XMLInputParser {

    private final P2Descriptor p2Descriptor;

    private int logLevel = Message.MSG_INFO;

    public P2MetadataParser(P2Descriptor p2Descriptor) {
        this.p2Descriptor = p2Descriptor;
    }

    public void setLogLevel(int logLevel) {
        this.logLevel = logLevel;
    }

    public void parse(InputStream in) throws ParseException, IOException, SAXException {
        RepositoryHandler handler = new RepositoryHandler(p2Descriptor);
        try {
            XMLHelper.parse(in, null, handler, null);
        } catch (ParserConfigurationException e) {
            throw new SAXException(e);
        }
    }

    private class RepositoryHandler extends DelegatingHandler {

        // private static final String P2_TIMESTAMP = "p2.timestamp";

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
            // addChild(new PropertiesHandler(P2_TIMESTAMP),
            // new ChildElementHandler<PropertiesHandler>() {
            // public void childHanlded(PropertiesHandler child) {
            // String timestamp = child.properties.get(P2_TIMESTAMP);
            // if (timestamp != null) {
            // p2Descriptor.setTimestamp(Long.parseLong(timestamp));
            // }
            // }
            // });
            addChild(new UnitsHandler(), new ChildElementHandler<UnitsHandler>() {
                @Override
                public void childHanlded(UnitsHandler child) {
                    for (BundleInfo bundle : child.bundles) {
                        p2Descriptor.addBundle(bundle);
                    }
                }
            });
            addChild(new ReferencesHandler(), new ChildElementHandler<ReferencesHandler>() {
                @Override
                public void childHanlded(ReferencesHandler child) {
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

    private class ReferencesHandler extends DelegatingHandler {

        private static final String REFERENCES = "references";

        private static final String SIZE = "size";

        List<URI> repositoryUris;

        public ReferencesHandler() {
            super(REFERENCES);
            addChild(new RepositoryReferenceHandler(),
                new ChildElementHandler<RepositoryReferenceHandler>() {
                    @Override
                    public void childHanlded(RepositoryReferenceHandler child) {
                        repositoryUris.add(child.uri);
                    }
                });
        }

        @Override
        protected void handleAttributes(Attributes atts) throws SAXException {
            int size = Integer.parseInt(atts.getValue(SIZE));
            repositoryUris = new ArrayList<URI>(size);
        }
    }

    private class RepositoryReferenceHandler extends DelegatingHandler {

        private static final String REPOSITORY = "repository";

        // private static final String TYPE = "type";
        //
        // private static final String OPTIONS = "options";
        //
        // private static final String NAME = "name";

        private static final String URI = "uri";

        private static final String URL = "url";

        public RepositoryReferenceHandler() {
            super(REPOSITORY);
        }

        // int type;
        //
        // int options;
        //
        // String name;

        URI uri;

        @Override
        protected void handleAttributes(Attributes atts) throws SAXException {
            // type = Integer.parseInt(atts.getValue(TYPE));
            // options = Integer.parseInt(atts.getValue(OPTIONS));
            // name = atts.getValue(NAME);

            String uriAtt = atts.getValue(URI);
            String urlAtt = atts.getValue(URL);

            if (uriAtt != null) {
                try {
                    uri = new URI(uriAtt);
                } catch (URISyntaxException e) {
                    throw new SAXParseException("Invalid uri attribute " + uriAtt + "("
                            + e.getMessage() + ")", getLocator());
                }
            }
            if (uri != null && urlAtt != null) {
                try {
                    uri = new URI(urlAtt);
                } catch (URISyntaxException e) {
                    throw new SAXParseException("Invalid url attribute " + urlAtt + "("
                            + e.getMessage() + ")", getLocator());
                }
            }
        }
    }

    private class UnitsHandler extends DelegatingHandler {

        private static final String UNITS = "units";

        private static final String SIZE = "size";

        List<BundleInfo> bundles;

        public UnitsHandler() {
            super(UNITS);
            addChild(new UnitHandler(), new ChildElementHandler<UnitHandler>() {
                @Override
                public void childHanlded(UnitHandler child) {
                    if (child.bundleInfo != null && !child.bundleInfo.getCapabilities().isEmpty()) {
                        bundles.add(child.bundleInfo);
                    }
                }
            });
        }

        @Override
        protected void handleAttributes(Attributes atts) {
            int size = Integer.parseInt(atts.getValue(SIZE));
            bundles = new ArrayList<BundleInfo>(size);
        }

    }

    class UnitHandler extends DelegatingHandler {

        private static final String CATEGORY_PROPERTY = "org.eclipse.equinox.p2.type.category";

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
            addChild(new PropertiesHandler(CATEGORY_PROPERTY),
                new ChildElementHandler<PropertiesHandler>() {
                    @Override
                    public void childHanlded(PropertiesHandler child) {
                        String category = child.properties.get(CATEGORY_PROPERTY);
                        if (category != null && Boolean.valueOf(category).booleanValue()) {
                            // this is a category definition, this is useless, skip this unit
                            child.getParent().skip();
                            bundleInfo = null;
                        }
                    }
                });
            addChild(new ProvidesHandler(), new ChildElementHandler<ProvidesHandler>() {
                @Override
                public void childHanlded(ProvidesHandler child) {
                    if ("source".equals(child.eclipseType)) {
                        // this is some source of some bundle
                        bundleInfo.setSource(true);
                        // we need to parse the manifest in the toupointData to figure out the
                        // targeted bundle
                        // in case we won't have the proper data in the manifest, prepare the source
                        // data from the convention
                        String symbolicName = bundleInfo.getSymbolicName();
                        if (symbolicName.endsWith(".source")) {
                            bundleInfo.setSymbolicNameTarget(symbolicName.substring(0,
                                symbolicName.length() - 7));
                            bundleInfo.setVersionTarget(bundleInfo.getVersion());
                        }
                    }
                    for (BundleCapability capability : child.capabilities) {
                        bundleInfo.addCapability(capability);
                    }
                }
            });
            addChild(new FilterHandler(), new ChildElementHandler<FilterHandler>() {
                @Override
                public void childHanlded(FilterHandler child) {
                }
            });
            addChild(new RequiresHandler(), new ChildElementHandler<RequiresHandler>() {
                @Override
                public void childHanlded(RequiresHandler child) {
                    for (BundleRequirement requirement : child.requirements) {
                        bundleInfo.addRequirement(requirement);
                    }
                }
            });
            addChild(new HostRequirementsHandler(),
                new ChildElementHandler<HostRequirementsHandler>() {
                    @Override
                    public void childHanlded(HostRequirementsHandler child) {
                    }
                });
            addChild(new MetaRequirementsHandler(),
                new ChildElementHandler<MetaRequirementsHandler>() {
                    @Override
                    public void childHanlded(MetaRequirementsHandler child) {
                    }
                });
            addChild(new ArtifactsHandler(), new ChildElementHandler<ArtifactsHandler>() {
                @Override
                public void childHanlded(ArtifactsHandler child) {
                }
            });
            // addChild(new TouchpointHandler(), new ChildElementHandler() {
            // public void childHanlded(DelegetingHandler child) {
            // }
            // });
            addChild(new TouchpointDataHandler(), new ChildElementHandler<TouchpointDataHandler>() {
                @Override
                public void childHanlded(TouchpointDataHandler child) throws SAXParseException {
                    if (child.zipped != null) {
                        bundleInfo.setHasInnerClasspath(child.zipped.booleanValue());
                    }
                    if (!bundleInfo.isSource()) {
                        // we only care about parsing the manifest if it is a source
                        return;
                    }
                    if (child.manifest != null) {
                        // Eclipse may have serialized a little bit weirdly
                        String manifest = ManifestParser.formatLines(child.manifest.trim());
                        BundleInfo embeddedInfo;
                        try {
                            embeddedInfo = ManifestParser.parseManifest(manifest);
                        } catch (IOException e) {
                            if (logLevel >= Message.MSG_VERBOSE) {
                                Message.verbose(
                                    "The Manifest of the source bundle "
                                            + bundleInfo.getSymbolicName() + " could not be parsed",
                                    e);
                            }
                            return;
                        } catch (ParseException e) {
                            if (logLevel >= Message.MSG_VERBOSE) {
                                Message.verbose(
                                    "The Manifest of the source bundle "
                                            + bundleInfo.getSymbolicName() + " is ill formed", e);
                            }
                            return;
                        }
                        if (!embeddedInfo.isSource()) {
                            if (logLevel >= Message.MSG_VERBOSE) {
                                Message.verbose("The Manifest of the source bundle "
                                        + bundleInfo.getSymbolicName()
                                        + " is not declaring being a source.");
                            }
                            return;
                        }
                        String symbolicNameTarget = embeddedInfo.getSymbolicNameTarget();
                        if (symbolicNameTarget == null) {
                            if (logLevel >= Message.MSG_VERBOSE) {
                                Message.verbose("The Manifest of the source bundle "
                                        + bundleInfo.getSymbolicName()
                                        + " is not declaring a target symbolic name.");
                            }
                            return;
                        }
                        Version versionTarget = embeddedInfo.getVersionTarget();
                        if (versionTarget == null) {
                            if (logLevel >= Message.MSG_VERBOSE) {
                                Message.verbose("The Manifest of the source bundle "
                                        + bundleInfo.getSymbolicName()
                                        + " is not declaring a target version.");
                            }
                            return;
                        }
                        bundleInfo.setSymbolicNameTarget(symbolicNameTarget);
                        bundleInfo.setVersionTarget(versionTarget);
                    }
                }
            });
            // addChild(new LicensesHandler(), new ChildElementHandler() {
            // public void childHanlded(DelegetingHandler child) {
            // }
            // });
            // addChild(new CopyrightHandler(), new ChildElementHandler() {
            // public void childHanlded(DelegetingHandler child) {
            // }
            // });
            // addChild(new ChangesHandler(), new ChildElementHandler() {
            // public void childHanlded(DelegetingHandler child) {
            // }
            // });

        }

        @Override
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

    private static class FilterHandler extends DelegatingHandler {

        private static final String FILTER = "filter";

        public FilterHandler() {
            super(FILTER);
            setBufferingChar(true);
        }

    }

    private static String namespace2Type(String namespace) {
        if (namespace == null) {
            return null;
        }
        if (namespace.equals("java.package")) {
            return BundleInfo.PACKAGE_TYPE;
        }
        if (namespace.equals("osgi.bundle")) {
            return BundleInfo.BUNDLE_TYPE;
        }
        return null;
    }

    private class ProvidesHandler extends DelegatingHandler {

        private static final String PROVIDES = "provides";

        private static final String SIZE = "size";

        List<BundleCapability> capabilities;

        String eclipseType;

        public ProvidesHandler() {
            super(PROVIDES);
            addChild(new ProvidedHandler(), new ChildElementHandler<ProvidedHandler>() {
                @Override
                public void childHanlded(ProvidedHandler child) {
                    if (child.namespace.equals("org.eclipse.equinox.p2.eclipse.type")) {
                        eclipseType = child.name;
                    } else {
                        String type = namespace2Type(child.namespace);
                        if (type == null) {
                            if (logLevel >= Message.MSG_DEBUG) {
                                Message.debug("Unsupported provided capability " + child.namespace
                                        + " " + child.name + " " + child.version);
                            }
                            return;
                        }
                        BundleCapability capability;
                        if (type == BundleInfo.PACKAGE_TYPE) {
                            capability = new ExportPackage(child.name, child.version);
                        } else {
                            capability = new BundleCapability(type, child.name, child.version);
                        }
                        capabilities.add(capability);
                    }
                }
            });
        }

        @Override
        protected void handleAttributes(Attributes atts) {
            int size = Integer.parseInt(atts.getValue(SIZE));
            capabilities = new ArrayList<BundleCapability>(size);
        }

    }

    private static class ProvidedHandler extends DelegatingHandler {

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

        @Override
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

    abstract class AbstractRequirementHandler extends DelegatingHandler {

        private static final String SIZE = "size";

        List<BundleRequirement> requirements;

        public AbstractRequirementHandler(String name) {
            super(name);
            addChild(new RequiredHandler(), new ChildElementHandler<RequiredHandler>() {
                @Override
                public void childHanlded(RequiredHandler child) {
                    String name = child.name;
                    VersionRange range = child.range;
                    String type = namespace2Type(child.namespace);
                    if (type == null) {
                        if (logLevel >= Message.MSG_DEBUG) {
                            Message.debug("Unsupported required capability " + child.namespace
                                    + " " + name + " " + range);
                        }
                    } else {
                        String resolution = child.optional ? "optional" : null;
                        requirements.add(new BundleRequirement(type, name, range, resolution));
                    }
                }
            });
        }

        @Override
        protected void handleAttributes(Attributes atts) {
            int size = Integer.parseInt(atts.getValue(SIZE));
            requirements = new ArrayList<BundleRequirement>(size);
        }

    }

    private class RequiresHandler extends AbstractRequirementHandler {

        private static final String REQUIRES = "requires";

        public RequiresHandler() {
            super(REQUIRES);
        }

    }

    private class RequiredHandler extends DelegatingHandler {

        private static final String REQUIRED = "required";

        private static final String NAMESPACE = "namespace";

        private static final String NAME = "name";

        private static final String RANGE = "range";

        private static final String OPTIONAL = "optional";

        // private static final String GREEDY = "greedy";

        String namespace;

        String name;

        VersionRange range;

        // String filter;
        //
        // boolean greedy;

        boolean optional;

        public RequiredHandler() {
            super(REQUIRED);
            // addChild(new FilterHandler(), new ChildElementHandler<FilterHandler>() {
            // public void childHanlded(FilterHandler child) {
            // filter = child.getBufferedChars().trim();
            // }
            // });
        }

        @Override
        protected void handleAttributes(Attributes atts) throws SAXParseException {
            namespace = atts.getValue(NAMESPACE);
            name = atts.getValue(NAME);
            try {
                range = new VersionRange(atts.getValue(RANGE));
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
            // greedy = getOptionalBooleanAttribute(atts, GREEDY, Boolean.TRUE).booleanValue();
            optional = getOptionalBooleanAttribute(atts, OPTIONAL, Boolean.FALSE).booleanValue();
        }

    }

    private class HostRequirementsHandler extends AbstractRequirementHandler {

        private static final String HOST_REQUIREMENTS = "hostRequirements";

        public HostRequirementsHandler() {
            super(HOST_REQUIREMENTS);
        }

    }

    private class MetaRequirementsHandler extends AbstractRequirementHandler {

        private static final String META_REQUIREMENTS = "metaRequirements";

        public MetaRequirementsHandler() {
            super(META_REQUIREMENTS);
        }

    }

    private class ArtifactsHandler extends DelegatingHandler {

        private static final String ARTIFACTS = "artifacts";

        private static final String SIZE = "size";

        List<P2Artifact> artifacts;

        public ArtifactsHandler() {
            super(ARTIFACTS);
            addChild(new ArtifactHandler(), new ChildElementHandler<ArtifactHandler>() {
                @Override
                public void childHanlded(ArtifactHandler child) {
                    artifacts.add(child.artifact);
                }
            });
        }

        @Override
        protected void handleAttributes(Attributes atts) {
            int size = Integer.parseInt(atts.getValue(SIZE));
            artifacts = new ArrayList<P2Artifact>(size);
        }

    }

    private class ArtifactHandler extends DelegatingHandler {

        private static final String ARTIFACT = "artifact";

        private static final String ID = "id";

        private static final String VERSION = "version";

        private static final String CLASSIFIER = "classifier";

        P2Artifact artifact;

        public ArtifactHandler() {
            super(ARTIFACT);
        }

        @Override
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

    // private static class TouchpointHandler extends DelegetingHandler {
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

    private class TouchpointDataHandler extends DelegatingHandler {

        private static final String TOUCHPOINTDATA = "touchpointData";

        // private static final String SIZE = "size";

        String manifest;

        Boolean zipped;

        public TouchpointDataHandler() {
            super(TOUCHPOINTDATA);
            addChild(new InstructionsHandler(), new ChildElementHandler<InstructionsHandler>() {
                @Override
                public void childHanlded(InstructionsHandler child) {
                    manifest = child.manifest;
                    zipped = child.zipped;
                }
            });
        }

        @Override
        protected void handleAttributes(Attributes atts) {
            // String size = atts.getValue(SIZE);
        }

    }

    private class InstructionsHandler extends DelegatingHandler {

        private static final String INSTRUCTIONS = "instructions";

        // private static final String SIZE = "size";

        String manifest;

        Boolean zipped;

        public InstructionsHandler() {
            super(INSTRUCTIONS);
            addChild(new InstructionHandler(), new ChildElementHandler<InstructionHandler>() {
                @Override
                public void childHanlded(InstructionHandler child) {
                    manifest = null;
                    zipped = null;
                    String buffer = child.getBufferedChars().trim();
                    if ("manifest".equals(child.key)) {
                        manifest = buffer;
                    } else if ("zipped".equals(child.key) && buffer.length() != 0) {
                        zipped = Boolean.valueOf(buffer);
                    }
                }
            });
        }

        @Override
        protected void handleAttributes(Attributes atts) {
            // String size = atts.getValue(SIZE);
        }

    }

    private class InstructionHandler extends DelegatingHandler {

        private static final String INSTRUCTION = "instruction";

        private static final String KEY = "key";

        String key;

        public InstructionHandler() {
            super(INSTRUCTION);
            setBufferingChar(true);
        }

        @Override
        protected void handleAttributes(Attributes atts) {
            key = atts.getValue(KEY);
        }

    }

    // private static class LicensesHandler extends DelegetingHandler {
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

    // private static class LicenseHandler extends DelegetingHandler {
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

    // private static class CopyrightHandler extends DelegetingHandler {
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

    // private class ChangesHandler extends DelegetingHandler {
    //
    // private static final String CHANGES = "changes";
    //
    // private static final String SIZE = "size";
    //
    // public ChangesHandler() {
    // super(CHANGES);
    // addChild(new ChangeHandler(), new ChildElementHandler<ChangeHandler>() {
    // public void childHanlded(ChangeHandler child) {
    // }
    // });
    // }
    //
    // protected void handleAttributes(Attributes atts) {
    // int size = Integer.parseInt(atts.getValue(SIZE));
    // }
    // }

    // private class ChangeHandler extends DelegetingHandler {
    //
    // private static final String CHANGE = "change";
    //
    // public ChangeHandler() {
    // super(CHANGE);
    // }
    // }

    // private class FromHandler extends AbstractRequirementHandler {
    //
    // private static final String FROM = "from";
    //
    // public FromHandler() {
    // super(FROM);
    // }
    //
    // }

    // private class ToHandler extends AbstractRequirementHandler {
    //
    // private static final String TO = "to";
    //
    // public ToHandler() {
    // super(TO);
    // }
    //
    // }

    // private class PatchScopeHandler extends DelegetingHandler {
    //
    // private static final String PATCH_SCOPE = "patchScope";
    //
    // private static final String SIZE = "size";
    //
    // public PatchScopeHandler() {
    // super(PATCH_SCOPE);
    // addChild(new PatchScopeHandler(), new ChildElementHandler<PatchScopeHandler>() {
    // public void childHanlded(PatchScopeHandler child) {
    // }
    // });
    // }
    //
    // protected void handleAttributes(Attributes atts) {
    // int size = Integer.parseInt(atts.getValue(SIZE));
    // }
    // }

    // private class ScopeHandler extends DelegetingHandler {
    //
    // private static final String SCOPE = "scope";
    //
    // public ScopeHandler() {
    // super(SCOPE);
    // addChild(new RequiresHandler(), new ChildElementHandler<RequiresHandler>() {
    // public void childHanlded(RequiresHandler child) {
    // }
    // });
    // }
    // }

    // private class LifeCycleHandler extends AbstractRequirementHandler {
    //
    // private static final String LIFE_CYCLE = "lifeCycle";
    //
    // public LifeCycleHandler() {
    // super(LIFE_CYCLE);
    // }
    // }

}
