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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.ivy.osgi.util.DelegatingHandler;
import org.apache.ivy.util.XMLHelper;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class P2CompositeParser implements XMLInputParser {

    private Set<String> childLocations = new LinkedHashSet<String>();

    public Set<String> getChildLocations() {
        return childLocations;
    }

    public void parse(InputStream in) throws ParseException, IOException, SAXException {
        RepositoryHandler handler = new RepositoryHandler();
        try {
            XMLHelper.parse(in, null, handler, null);
        } catch (ParserConfigurationException e) {
            throw new SAXException(e);
        }
        childLocations.addAll(handler.childLocations);
    }

    private static class RepositoryHandler extends DelegatingHandler {

        private static final String REPOSITORY = "repository";

        // private static final String NAME = "name";
        //
        // private static final String TYPE = "type";
        //
        // private static final String VERSION = "version";

        List<String> childLocations = Collections.emptyList();

        public RepositoryHandler() {
            super(REPOSITORY);
            addChild(new ChildrenHandler(), new ChildElementHandler<ChildrenHandler>() {
                @Override
                public void childHanlded(ChildrenHandler child) {
                    childLocations = child.childLocations;
                }
            });
        }
        // protected void handleAttributes(Attributes atts) {
        // String name = atts.getValue(NAME);
        // String type = atts.getValue(TYPE);
        // String version = atts.getValue(VERSION);
        // }
    }

    private static class ChildrenHandler extends DelegatingHandler {

        private static final String CHILDREN = "children";

        private static final String SIZE = "size";

        List<String> childLocations;

        public ChildrenHandler() {
            super(CHILDREN);
            addChild(new ChildHandler(), new ChildElementHandler<ChildHandler>() {
                @Override
                public void childHanlded(ChildHandler child) {
                    childLocations.add(child.location);
                }
            });
        }

        @Override
        protected void handleAttributes(Attributes atts) {
            int size = Integer.parseInt(atts.getValue(SIZE));
            childLocations = new ArrayList<String>(size);
        }

    }

    private static class ChildHandler extends DelegatingHandler {

        private static final String CHILD = "child";

        private static final String LOCATION = "location";

        String location;

        public ChildHandler() {
            super(CHILD);
        }

        @Override
        protected void handleAttributes(Attributes atts) {
            location = atts.getValue(LOCATION);
        }

    }

}
