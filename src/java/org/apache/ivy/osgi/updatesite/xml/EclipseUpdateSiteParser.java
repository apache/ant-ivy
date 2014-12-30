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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.ivy.osgi.util.DelegatingHandler;
import org.apache.ivy.osgi.util.Version;
import org.apache.ivy.util.XMLHelper;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class EclipseUpdateSiteParser {

    public static UpdateSite parse(InputStream in) throws IOException, SAXException {
        SiteHandler handler = new SiteHandler();
        try {
            XMLHelper.parse(in, null, handler, null);
        } catch (ParserConfigurationException e) {
            throw new SAXException(e);
        }
        return handler.updatesite;
    }

    private static class SiteHandler extends DelegatingHandler {

        private static final String SITE = "site";

        private static final String URL = "url";

        private static final String PACK200 = "pack200";

        private static final String MIRRORS_URL = "mirrorsURL";

        private static final String ASSOCIATE_SITES_URL = "associateSitesURL";

        private static final String DIGEST_URL = "digestURL";

        UpdateSite updatesite;

        public SiteHandler() {
            super(SITE);
            // addChild(new DescriptionHandler(), new ChildElementHandler() {
            // public void childHanlded(DelegetingHandler child) {
            // updateSite.setDescription(child.getBufferedChars().trim());
            // }
            // });
            addChild(new FeatureHandler(), new ChildElementHandler<FeatureHandler>() {
                @Override
                public void childHanlded(FeatureHandler child) {
                    updatesite.addFeature(child.feature);
                }
            });
            // addChild(new ArchiveHandler(), new ChildElementHandler() {
            // public void childHanlded(DelegetingHandler child) {
            // updateSite.addArchive(((ArchiveHandler) child).archive);
            // }
            // });
            // addChild(new CategoryDefHandler(), new ChildElementHandler() {
            // public void childHanlded(DelegetingHandler child) {
            // updateSite.addCategoryDef(((CategoryDefHandler) child).categoryDef);
            // }
            // });
        }

        @Override
        protected void handleAttributes(Attributes atts) {
            updatesite = new UpdateSite();

            String url = atts.getValue(URL);
            if (url != null && !("".equals(url.trim()))) {
                if (!url.endsWith("/") && !url.endsWith(File.separator)) {
                    url += "/";
                }
                try {
                    updatesite.setUri(new URI(url));
                } catch (URISyntaxException e) {
                    throw new RuntimeException("illegal url", e);
                }
            }

            String mirrorsURL = atts.getValue(MIRRORS_URL);
            if (mirrorsURL != null && mirrorsURL.trim().length() > 0) {
                updatesite.setMirrorsURL(mirrorsURL);
            }

            String pack200 = atts.getValue(PACK200);
            if (pack200 != null && new Boolean(pack200).booleanValue()) {
                updatesite.setPack200(true);
            }

            String digestURL = atts.getValue(DIGEST_URL);
            if (digestURL != null) {
                try {
                    updatesite.setDigestUri(new URI(digestURL));
                } catch (URISyntaxException e) {
                    throw new RuntimeException("illegal url", e);
                }
            }

            String associateSitesURL = atts.getValue(ASSOCIATE_SITES_URL);
            if (associateSitesURL != null) {
                updatesite.setAssociateSitesURL(associateSitesURL);
            }
        }
    }

    // private static class DescriptionHandler extends DelegetingHandler {
    //
    // private static final String DESCRIPTION = "description";
    //
    // private static final String URL = "url";
    //
    // public DescriptionHandler() {
    // super(DESCRIPTION);
    // setBufferingChar(true);
    // }
    //
    // protected void handleAttributes(Attributes atts) {
    // String url = atts.getValue(URL);
    // }
    // }

    private static class FeatureHandler extends DelegatingHandler {

        private static final String FEATURE = "feature";

        private static final String VERSION = "version";

        private static final String ID = "id";

        private static final String URL = "url";

        private static final String PATCH = "patch";

        private static final String ARCH = "arch";

        private static final String NL = "nl";

        private static final String WS = "ws";

        private static final String OS = "os";

        private static final String LABEL = "label";

        private static final String TYPE = "type";

        private EclipseFeature feature;

        public FeatureHandler() {
            super(FEATURE);
            addChild(new CategoryHandler(), new ChildElementHandler<CategoryHandler>() {
                @Override
                public void childHanlded(CategoryHandler child) {
                    feature.addCategory(child.name);
                }
            });
        }

        @Override
        protected void handleAttributes(Attributes atts) throws SAXException {
            String id = atts.getValue(ID);
            String version = atts.getValue(VERSION);
            try {
                feature = new EclipseFeature(id, new Version(version));
            } catch (ParseException e) {
                throw new SAXException("Incorrect version on the feature '" + id + "': " + version
                        + " (" + e.getMessage() + ")");
            }

            String url = atts.getValue(URL);
            if (url != null) {
                feature.setURL(url);
            }
            feature.setType(atts.getValue(TYPE));
            feature.setLabel(atts.getValue(LABEL));
            feature.setOS(atts.getValue(OS));
            feature.setWS(atts.getValue(WS));
            feature.setNL(atts.getValue(NL));
            feature.setArch(atts.getValue(ARCH));
            feature.setPatch(atts.getValue(PATCH));
        }

    }

    private static class CategoryHandler extends DelegatingHandler {

        private static final String CATEGORY = "category";

        private static final String NAME = "name";

        String name;

        public CategoryHandler() {
            super(CATEGORY);
        }

        @Override
        protected void handleAttributes(Attributes atts) throws SAXException {
            name = atts.getValue(NAME);
        }
    }

    // private static class ArchiveHandler extends DelegetingHandler {
    //
    // private static final String ARCHIVE = "archive";
    //
    // private static final String URL = "url";
    //
    // private static final String PATH = "path";
    //
    // private Archive archive;
    //
    // public ArchiveHandler() {
    // super(ARCHIVE);
    // }
    //
    // protected void handleAttributes(Attributes atts) throws SAXException {
    // archive = new Archive();
    //
    // String path = atts.getValue(PATH);
    // archive.setPath(path);
    //
    // String url = atts.getValue(URL);
    // archive.setURL(url);
    //
    // }
    // }

    // private static class CategoryDefHandler extends DelegetingHandler {
    //
    // private static final String CATEGORY_DEF = "category-def";
    //
    // private static final String NAME = "name";
    //
    // private static final String LABEL = "label";
    //
    // private CategoryDef categoryDef;
    //
    // public CategoryDefHandler() {
    // super(CATEGORY_DEF);
    // addChild(new DescriptionHandler(), new ChildElementHandler<DescriptionHandler>() {
    // public void childHanlded(DescriptionHandler child) {
    // categoryDef.setDescription(child.getBufferedChars().trim());
    // }
    // });
    // }
    //
    // protected void handleAttributes(Attributes atts) throws SAXException {
    // categoryDef = new CategoryDef();
    //
    // String name = atts.getValue(NAME);
    // categoryDef.setName(name);
    //
    // String label = atts.getValue(LABEL);
    // categoryDef.setLabel(label);
    // }
    // }

}
