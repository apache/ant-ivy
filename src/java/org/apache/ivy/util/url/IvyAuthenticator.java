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

import java.net.Authenticator;
import java.net.PasswordAuthentication;

import org.apache.ivy.util.Credentials;
import org.apache.ivy.util.Message;

/**
 * 
 */
public final class IvyAuthenticator extends Authenticator {

    /**
     * The sole instance.
     */
    public static final IvyAuthenticator INSTANCE = new IvyAuthenticator();

    /**
     * Private c'tor to prevent instantiation. Also installs this as the default Authenticator to
     * use by the JVM.
     */
    private IvyAuthenticator() {
        // Install this as the default Authenticator object.
        Authenticator.setDefault(this);
    }

    // API ******************************************************************

    // Overriding Authenticator *********************************************

    protected PasswordAuthentication getPasswordAuthentication() {
        PasswordAuthentication result = null;
        
        String proxyHost = System.getProperty("http.proxyHost");
        if (getRequestingHost().equals(proxyHost)) {
            String proxyUser = System.getProperty("http.proxyUser");
            if ((proxyUser != null) && (proxyUser.trim().length() > 0)) {
                String proxyPass = System.getProperty("http.proxyPassword", "");
                Message.debug("authenicating to proxy server with username [" + proxyUser + "]");
                result = new PasswordAuthentication(proxyUser, proxyPass.toCharArray());
            }
        } else {
            Credentials c = CredentialsStore.INSTANCE.getCredentials(getRequestingPrompt(),
                getRequestingHost());
            Message.debug("authentication: k='"
                    + Credentials.buildKey(getRequestingPrompt(), getRequestingHost()) + "' c='" + c
                    + "'");
            if (c != null) {
                result = new PasswordAuthentication(c.getUserName(), c.getPasswd().toCharArray());
            }
        }
        
        return result;
    }

}
