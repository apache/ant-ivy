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
package org.apache.ivy.osgi.updatesite.xml;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.ivy.osgi.util.DelegatingHandler;
import org.apache.ivy.osgi.util.Version;
import org.apache.ivy.util.XMLHelper;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class FeatureParser {

    public static EclipseFeature parse(InputStream in) throws IOException, SAXException {
        FeatureHandler handler = new FeatureHandler();
        try {
            XMLHelper.parse(in, null, handler, null);
        } catch (ParserConfigurationException e) {
            throw new SAXException(e);
        }
        return handler.feature;
    }

    static class FeatureHandler extends DelegatingHandler {

        private static final String FEATURE = "feature";

        private static final String COLOCATION_AFFINITY = "colocation-affinity";

        private static final String PRIMARY = "primary";

        private static final String EXCLUSIVE = "exclusive";

        private static final String PLUGIN = "plugin";

        private static final String APPLICATION = "application";

        private static final String ARCH = "arch";

        private static final String NL = "nl";

        private static final String WS = "ws";

        private static final String OS = "os";

        private static final String VERSION = "version";

        private static final String ID = "id";

        private static final String PROVIDER_NAME = "provider-name";

        private static final String LABEL = "label";

        private static final String IMAGE = "image";

        EclipseFeature feature;

        public FeatureHandler() {
            super(FEATURE);
            addChild(new DescriptionHandler(), new ChildElementHandler<DescriptionHandler>() {
                @Override
                public void childHanlded(DescriptionHandler child) {
                    feature.setDescription(child.getBufferedChars().trim());
                }
            });
            addChild(new LicenseHandler(), new ChildElementHandler<LicenseHandler>() {
                @Override
                public void childHanlded(LicenseHandler child) {
                    feature.setLicense(child.getBufferedChars().trim());
                }
            });
            addChild(new CopyrightHandler(), new ChildElementHandler<CopyrightHandler>() {
                @Override
                public void childHanlded(CopyrightHandler child) {
                    feature.setCopyright(child.getBufferedChars().trim());
                }
            });
            addChild(new PluginHandler(), new ChildElementHandler<PluginHandler>() {
                @Override
                public void childHanlded(PluginHandler child) {
                    feature.addPlugin(child.plugin);
                }
            });
            addChild(new RequiresHandler(), new ChildElementHandler<RequiresHandler>() {
                @Override
                public void childHanlded(RequiresHandler child) {
                    for (Require require : child.requires) {
                        feature.addRequire(require);
                    }
                }
            });
            // addChild(new UrlHandler(), new ChildElementHandler<UrlHandler>() {
            // public void childHanlded(UrlHandler child) {
            // }
            // });
        }

        @Override
        protected void handleAttributes(Attributes atts) throws SAXException {
            String id = atts.getValue(ID);
            String version = atts.getValue(VERSION);
            try {
                feature = new EclipseFeature(id, new Version(version));
            } catch (ParseException e) {
                throw new SAXException("Incorrect version on feature '" + id + "': " + version
                        + " (" + e.getMessage() + ")");
            }

            feature.setOS(atts.getValue(OS));
            feature.setWS(atts.getValue(WS));
            feature.setNL(atts.getValue(NL));
            feature.setArch(atts.getValue(ARCH));
            feature.setApplication(atts.getValue(APPLICATION));
            feature.setPlugin(atts.getValue(PLUGIN));
            feature.setExclusive(Boolean.valueOf(atts.getValue(EXCLUSIVE)).booleanValue());
            feature.setPrimary(Boolean.valueOf(atts.getValue(PRIMARY)).booleanValue());
            feature.setColocationAffinity(atts.getValue(COLOCATION_AFFINITY));
            feature.setProviderName(atts.getValue(PROVIDER_NAME));
            feature.setLabel(atts.getValue(LABEL));
            feature.setImage(atts.getValue(IMAGE));
        }

    }

    private static class PluginHandler extends DelegatingHandler {

        private static final String PLUGIN = "plugin";

        private static final String FILTER = "filter";

        private static final String FRAGMENT = "fragment";

        private static final String UNPACK = "unpack";

        private static final String VERSION = "version";

        private static final String ID = "id";

        private EclipsePlugin plugin;

        public PluginHandler() {
            super(PLUGIN);
        }

        @Override
        protected void handleAttributes(Attributes atts) throws SAXException {
            plugin = new EclipsePlugin();

            String id = atts.getValue(ID);
            String version = atts.getValue(VERSION);

            plugin.setId(id);
            try {
                plugin.setVersion(new Version(version));
            } catch (ParseException e) {
                throw new SAXException("Incorrect version on feature's plugin '" + id + "': "
                        + version + " (" + e.getMessage() + ")");
            }
            plugin.setUnpack(Boolean.valueOf(atts.getValue(UNPACK)).booleanValue());
            plugin.setFragment(atts.getValue(FRAGMENT));
            plugin.setFilter(atts.getValue(FILTER));
        }
    }

    private static class DescriptionHandler extends DelegatingHandler {

        private static final String DESCRIPTION = "description";

        // private static final String URL = "url";

        public DescriptionHandler() {
            super(DESCRIPTION);
            setBufferingChar(true);
        }

        @Override
        protected void handleAttributes(Attributes atts) throws SAXException {
            // String url = atts.getValue(URL);
        }
    }

    private static class LicenseHandler extends DelegatingHandler {

        private static final String LICENSE = "license";

        // private static final String URL = "url";

        public LicenseHandler() {
            super(LICENSE);
            setBufferingChar(true);
        }

        @Override
        protected void handleAttributes(Attributes atts) throws SAXException {
            // String url = atts.getValue(URL);
        }

    }

    private static class CopyrightHandler extends DelegatingHandler {

        private static final String COPYRIGHT = "copyright";

        // private static final String URL = "url";

        public CopyrightHandler() {
            super(COPYRIGHT);
            setBufferingChar(true);
        }

        @Override
        protected void handleAttributes(Attributes atts) throws SAXException {
            // String url = atts.getValue(URL);
        }
    }

    static class RequiresHandler extends DelegatingHandler {

        private static final String REQUIRES = "requires";

        List<Require> requires = new ArrayList<Require>();

        public RequiresHandler() {
            super(REQUIRES);
            addChild(new ImportHandler(), new ChildElementHandler<ImportHandler>() {
                @Override
                public void childHanlded(ImportHandler child) {
                    requires.add(child.require);
                }
            });
        }
    }

    private static class ImportHandler extends DelegatingHandler {

        Require require;

        private static final String IMPORT = "import";

        private static final String FILTER = "filter";

        private static final String MATCH = "match";

        private static final String VERSION = "version";

        private static final String PLUGIN = "plugin";

        private static final String FEATURE = "feature";

        public ImportHandler() {
            super(IMPORT);
        }

        @Override
        protected void handleAttributes(Attributes atts) throws SAXException {
            require = new Require();

            String version = atts.getValue(VERSION);

            require.setFeature(atts.getValue(FEATURE));
            require.setPlugin(atts.getValue(PLUGIN));
            try {
                require.setVersion(new Version(version));
            } catch (ParseException e) {
                throw new SAXException("Incorrect version on feature's import: " + version + " ("
                        + e.getMessage() + ")");
            }
            require.setMatch(atts.getValue(MATCH));
            require.setFilter(atts.getValue(FILTER));
        }
    }

    // private static class IncludesHandler extends DelegetingHandler {
    //
    // private static final String INCLUDES = "includes";
    //
    // private static final String FILTER = "filter";
    //
    // private static final String OPTIONAL = "optional";
    //
    // private static final String VERSION = "version";
    //
    // private static final String ID = "id";
    //
    // public IncludesHandler() {
    // super(INCLUDES);
    // }
    //
    // protected void handleAttributes(Attributes atts) throws SAXException {
    // String id = atts.getValue(ID);
    // String version = atts.getValue(VERSION);
    // String optional = atts.getValue(OPTIONAL);
    // String filter = atts.getValue(FILTER);
    // }
    //
    // }

    // private static class InstallHandlerHandler extends DelegetingHandler {
    //
    // private static final String INSTALL_HANDLER = "install-handler";
    //
    // private static final String URL = "url";
    //
    // private static final String LIBRARY = "library";
    //
    // private static final String HANDLER = "handler";
    //
    // public InstallHandlerHandler() {
    // super(INSTALL_HANDLER);
    // }
    //
    // protected void handleAttributes(Attributes atts) throws SAXException {
    // String handler = atts.getValue(HANDLER);
    // String library = atts.getValue(LIBRARY);
    // String url = atts.getValue(URL);
    // }
    //
    // }

    // private static class UrlHandler extends DelegetingHandler {
    //
    // private static final String URL = "url";
    //
    // public UrlHandler() {
    // super(URL);
    // addChild(new UpdateHandler(), new ChildElementHandler<UpdateHandler>() {
    // public void childHanlded(UpdateHandler child) {
    // }
    // });
    // addChild(new DiscoveryHandler(), new ChildElementHandler<DiscoveryHandler>() {
    // public void childHanlded(DiscoveryHandler child) {
    // }
    // });
    // }
    //
    // }

    // private static class UpdateHandler extends DelegetingHandler {
    //
    // private static final String UPDATE = "update";
    //
    // private static final String LABEL = "label";
    //
    // private static final String URL = "url";
    //
    // public UpdateHandler() {
    // super(UPDATE);
    // }
    //
    // protected void handleAttributes(Attributes atts) throws SAXException {
    // String label = atts.getValue(LABEL);
    // String url = atts.getValue(URL);
    // }
    //
    // }

    // private static class DiscoveryHandler extends DelegetingHandler {
    //
    // private static final String DISCOVERY = "discovery";
    //
    // private static final String URL = "url";
    //
    // private static final String LABEL = "label";
    //
    // private static final String TYPE = "type";
    //
    // public DiscoveryHandler() {
    // super(DISCOVERY);
    // }
    //
    // protected void handleAttributes(Attributes atts) throws SAXException {
    // String type = atts.getValue(TYPE);
    // String label = atts.getValue(LABEL);
    // String url = atts.getValue(URL);
    // }
    //
    // }

}
