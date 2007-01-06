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
package fr.jayasoft.ivy.xml;

import java.net.URL;

import junit.framework.TestCase;
import fr.jayasoft.ivy.DependencyResolver;
import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.resolver.IvyRepResolver;
import fr.jayasoft.ivy.url.URLHandler;
import fr.jayasoft.ivy.url.URLHandlerDispatcher;
import fr.jayasoft.ivy.url.URLHandlerRegistry;

/**
 * split from XmlIvyConfigurationParserTest due to dependency on network resource
 */
public class OnlineXmlIvyConfigurationParserTest extends TestCase {
	// remote.test
    
    public void testIncludeHttpUrl() throws Exception {
        configureURLHandler();
        Ivy ivy = new Ivy();
        XmlIvyConfigurationParser parser = new XmlIvyConfigurationParser(ivy);
        parser.parse(new URL("http://www.jayasoft.org/misc/ivy/test/ivyconf-include-http-url.xml"));
        
        DependencyResolver resolver = ivy.getResolver("ivyrep");
        assertNotNull(resolver);
        assertTrue(resolver instanceof IvyRepResolver);
    }
    
    private void configureURLHandler() {
        URLHandlerDispatcher dispatcher = new URLHandlerDispatcher();
        URLHandler httpHandler = URLHandlerRegistry.getHttp();
        dispatcher.setDownloader("http", httpHandler);
        dispatcher.setDownloader("https", httpHandler);
        URLHandlerRegistry.setDefault(dispatcher);
    }
    
}
