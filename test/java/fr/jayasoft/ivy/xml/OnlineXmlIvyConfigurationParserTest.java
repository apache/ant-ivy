/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
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
