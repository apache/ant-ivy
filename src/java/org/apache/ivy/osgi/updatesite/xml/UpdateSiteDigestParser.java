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

import javax.xml.parsers.ParserConfigurationException;

import org.apache.ivy.osgi.core.ExecutionEnvironmentProfileProvider;
import org.apache.ivy.osgi.updatesite.UpdateSiteDescriptor;
import org.apache.ivy.osgi.updatesite.xml.FeatureParser.FeatureHandler;
import org.apache.ivy.osgi.util.DelegatingHandler;
import org.apache.ivy.util.XMLHelper;
import org.xml.sax.SAXException;

public class UpdateSiteDigestParser {

    public static UpdateSiteDescriptor parse(InputStream in, UpdateSite site) throws IOException,
            SAXException {
        DigestHandler handler = new DigestHandler(site);
        try {
            XMLHelper.parse(in, null, handler, null);
        } catch (ParserConfigurationException e) {
            throw new SAXException(e);
        }
        return handler.repoDescriptor;
    }

    static class DigestHandler extends DelegatingHandler {

        private static final String DIGEST = "digest";

        UpdateSiteDescriptor repoDescriptor;

        public DigestHandler(final UpdateSite site) {
            super(DIGEST);
            repoDescriptor = new UpdateSiteDescriptor(site.getUri(),
                    ExecutionEnvironmentProfileProvider.getInstance());
            addChild(new FeatureHandler(), new ChildElementHandler<FeatureHandler>() {
                @Override
                public void childHanlded(FeatureHandler child) {
                    repoDescriptor.addFeature(child.feature);
                }
            });
        }

    }

}
