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
package org.apache.ivy.core.settings;

import java.net.URL;

import org.apache.ivy.plugins.resolver.ChainResolver;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.plugins.resolver.IvyRepResolver;
import org.apache.ivy.util.url.URLHandler;
import org.apache.ivy.util.url.URLHandlerDispatcher;
import org.apache.ivy.util.url.URLHandlerRegistry;

import junit.framework.TestCase;

/**
 * split from XmlIvyConfigurationParserTest due to dependency on network resource
 */
public class OnlineXmlSettingsParserTest extends TestCase {
    // remote.test

    public void testIncludeHttpUrl() throws Exception {
        configureURLHandler();
        IvySettings settings = new IvySettings();
        XmlSettingsParser parser = new XmlSettingsParser(settings);
        parser.parse(new URL("http://ant.apache.org/ivy/test/ivysettings-include-http-url.xml"));

        DependencyResolver resolver = settings.getResolver("ivyrep");
        assertNotNull(resolver);
        assertTrue(resolver instanceof IvyRepResolver);
    }

    public void testIncludeHttpRelativeUrl() throws Exception {
        // Use a settings file via http that use an include with relative url
        configureURLHandler();
        IvySettings settings = new IvySettings();
        XmlSettingsParser parser = new XmlSettingsParser(settings);
        parser.parse(new URL(
                "http://ant.apache.org/ivy/test/ivysettings-include-http-relative-url.xml"));

        DependencyResolver resolver = settings.getResolver("ivyrep");
        assertNotNull(resolver);
        assertTrue(resolver instanceof IvyRepResolver);
    }

    public void testIncludeHttpRelativeFile() throws Exception {
        // Use a settings file via http that use an include with relative file
        configureURLHandler();
        IvySettings settings = new IvySettings();
        XmlSettingsParser parser = new XmlSettingsParser(settings);
        parser.parse(new URL(
                "http://ant.apache.org/ivy/test/ivysettings-include-http-relative-file.xml"));

        DependencyResolver resolver = settings.getResolver("ivyrep");
        assertNotNull(resolver);
        assertTrue(resolver instanceof IvyRepResolver);
    }

    public void testIncludeHttpAbsoluteFile() throws Exception {
        // Use a settings file via http that use an include with absolute file
        // WARNING : this test will only work if the test are launched from the project root
        // directory
        configureURLHandler();
        IvySettings settings = new IvySettings();
        XmlSettingsParser parser = new XmlSettingsParser(settings);
        parser.parse(new URL(
                "http://ant.apache.org/ivy/test/ivysettings-include-http-absolute-file.xml"));

        DependencyResolver inc = settings.getResolver("includeworks");
        assertNotNull(inc);
        assertTrue(inc instanceof ChainResolver);
    }

    private void configureURLHandler() {
        URLHandlerDispatcher dispatcher = new URLHandlerDispatcher();
        URLHandler httpHandler = URLHandlerRegistry.getHttp();
        dispatcher.setDownloader("http", httpHandler);
        dispatcher.setDownloader("https", httpHandler);
        URLHandlerRegistry.setDefault(dispatcher);
    }

}
