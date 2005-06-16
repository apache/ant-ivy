/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package example;

import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.lang.WordUtils;

/**
 * Simple hello world example to show how easy it is to retrieve libs with ivy, 
 * including transitive dependencies 
 */
public class Hello {
    public static void main(String[] args) throws Exception {
        String  message = "hello ivy !";
        System.out.println("standard message : " + message);
        System.out.println("capitalized by " + WordUtils.class.getName() + " : " + WordUtils.capitalizeFully(message));
        
        HttpClient client = new HttpClient();
        HeadMethod head = new HeadMethod("http://www.ibiblio.org/");
        client.executeMethod(head);
        
        int status = head.getStatusCode();
        System.out.println("head status code with httpclient: "+status);
        head.releaseConnection();
        
        System.out.println("now check if httpclient dependency on commons-logging has been realized");
        Class clss = Class.forName("org.apache.commons.logging.Log");
        System.out.println("found logging class in classpath: "+clss);
    }
}
