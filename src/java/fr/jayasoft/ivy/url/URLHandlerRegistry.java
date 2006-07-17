/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * version 1.3.1
 */
package fr.jayasoft.ivy.url;

import fr.jayasoft.ivy.util.Message;

/**
 * @author Xavier Hanin
 *
 */
public class URLHandlerRegistry {
    private static URLHandler _default = new BasicURLHandler();

    public static URLHandler getDefault() {
        return _default;
    }
    public static void setDefault(URLHandler def) {
        _default = def;
    }
    
    /**
     * This method is used to get appropriate http downloader
     * dependening on Jakarta Commons HttpClient
     * availability in classpath, or simply use jdk url
     * handling in other cases.
     * 
     * @return most accurate http downloader
     */
    public static URLHandler getHttp() {
        try {
            Class.forName("org.apache.commons.httpclient.HttpClient");
            Message.verbose("jakarta commons httpclient detected: using it for http downloading");
            return new HttpClientHandler(); 
        } catch (ClassNotFoundException e) {
             Message.verbose("jakarta commons httpclient not found: using jdk url handling");
            return new BasicURLHandler();
        }
    }

}
