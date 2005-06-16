/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
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
     * @param realm
     * @param host
     * @param userName
     * @param passwd
     * 
     * @return most accurate http downloader
     */
    public static URLHandler getHttp(String realm, String host, String userName, String passwd) {
        try {
            Class.forName("org.apache.commons.httpclient.HttpClient");
            Message.verbose("jakarta commons httpclient detected: using it for http downloading");
            return new HttpClientHandler(realm, host, userName, passwd); 
        } catch (ClassNotFoundException e) {
            Message.verbose("jakarta commons httpclient not found: no authentication will be done");
            return new BasicURLHandler();
        }
    }

    /**
     * This method is used to get appropriate http downloader
     * dependening on Jakarta Commons HttpClient
     * availability in classpath, or simply use jdk url
     * handling in other cases.
     * 
     * @param realm
     * @param host
     * @param userName
     * @param passwd
     * 
     * @return most accurate http downloader
     */
    public static URLHandler getHttp() {
        return getHttp(null, null, null, null);
    }

}
