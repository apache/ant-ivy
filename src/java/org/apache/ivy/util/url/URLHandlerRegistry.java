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
package org.apache.ivy.util.url;

import org.apache.ivy.util.Message;

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
