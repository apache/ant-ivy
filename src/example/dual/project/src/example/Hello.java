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
package example;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.lang.WordUtils;

/**
 * Simple hello world example to show how easy it is to retrieve libs with ivy, 
 * including transitive dependencies 
 */
public final class Hello {
    public static void main(String[] args) throws Exception {
        String  message = "hello ivy !";
        System.out.println("standard message : " + message);
        System.out.println("capitalized by " + WordUtils.class.getName() 
            + " : " + WordUtils.capitalizeFully(message));
        
        HttpClient client = new HttpClient();
        HeadMethod head = new HeadMethod("http://www.ibiblio.org/");
        client.executeMethod(head);
        
        int status = head.getStatusCode();
        System.out.println("head status code with httpclient: " + status);
        head.releaseConnection();
        
        System.out.println(
            "now check if httpclient dependency on commons-logging has been realized");
        Class clss = Class.forName("org.apache.commons.logging.Log");
        System.out.println("found logging class in classpath: " + clss);
    }
    
    private Hello() {
    }
}
