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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Authenticator;
import java.net.PasswordAuthentication;

import org.apache.ivy.util.Credentials;
import org.apache.ivy.util.Message;

/**
 * 
 */
public final class IvyAuthenticator extends Authenticator {

    private Authenticator original;

    private static boolean securityWarningLogged = false;

    /**
     * Private c'tor to prevent instantiation.
     */
    private IvyAuthenticator(Authenticator original) {
        this.original = original;
    }

    /**
     * Installs an <tt>IvyAuthenticator</tt> as default <tt>Authenticator</tt>. Call this method
     * before opening HTTP(S) connections to enable Ivy authentication.
     */
    public static void install() {
        // We will try to use the original authenticator as backup authenticator.
        // Since there is no getter available, so try to use some reflection to
        // obtain it. If that doesn't work, assume there is no original authenticator
        Authenticator original = null;

        try {
            Field f = Authenticator.class.getDeclaredField("theAuthenticator");
            f.setAccessible(true);
            original = (Authenticator) f.get(null);
        } catch (Throwable t) {
            Message.debug("Error occurred while getting the original authenticator: "
                    + t.getMessage());
        }

        if (!(original instanceof IvyAuthenticator)) {
            try {
                Authenticator.setDefault(new IvyAuthenticator(original));
            } catch (SecurityException e) {
                if (!securityWarningLogged) {
                    securityWarningLogged = true;
                    Message.warn("Not enough permissions to set the IvyAuthenticator. "
                            + "HTTP(S) authentication will be disabled!");
                }
            }
        }
    }

    // API ******************************************************************

    // Overriding Authenticator *********************************************

    protected PasswordAuthentication getPasswordAuthentication() {
        PasswordAuthentication result = null;

        if (isProxyAuthentication()) {
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
                    + Credentials.buildKey(getRequestingPrompt(), getRequestingHost()) + "' c='"
                    + c + "'");
            if (c != null) {
                final String password = c.getPasswd() == null ? "" : c.getPasswd();
                result = new PasswordAuthentication(c.getUserName(), password.toCharArray());
            }
        }

        if ((result == null) && (original != null)) {
            Authenticator.setDefault(original);
            try {
                result = Authenticator.requestPasswordAuthentication(getRequestingHost(),
                    getRequestingSite(), getRequestingPort(), getRequestingProtocol(),
                    getRequestingPrompt(), getRequestingScheme());
            } finally {
                Authenticator.setDefault(this);
            }
        }

        return result;
    }

    /**
     * Checks if the current authentication request is for the proxy server. This functionality is
     * not available in JDK1.4, so we check this in a very dirty way which is probably not very
     * portable, but will work for the SUN 1.4 JDKs.
     * 
     * @return
     */
    private boolean isProxyAuthentication() {
        try {
            // we first try to invoke the getRequestorType() method which is a JDK1.5+ method
            Method m = Authenticator.class.getDeclaredMethod("getRequestorType", null);
            Object result = m.invoke(this, null);
            return "PROXY".equals(String.valueOf(result));
        } catch (NoSuchMethodException e) {
            // do nothing, this is a JDK1.5+ method
        } catch (Throwable t) {
            Message.debug("Error occurred while checking if the authentication request is for the proxy server: "
                    + t.getMessage());
        }

        // now we will do something very dirty and analyse the stack trace to see
        // if this method is called from within the 'getHttpProxyAuthentication' method
        // or the 'getServerAuthentication' method which are both part of the
        // sun.net.www.protocol.http.HttpURLConnection class.
        // This might not work on other 1.4 JVM's!
        // This code should be removed when Ivy requires JDK1.5+
        StackTraceElement[] stackTrace = (new Exception()).getStackTrace();
        for (int i = 0; i < stackTrace.length; i++) {
            if ("getHttpProxyAuthentication".equals(stackTrace[i].getMethodName())) {
                return true;
            }
            if ("getServerAuthentication".equals(stackTrace[i].getMethodName())) {
                return false;
            }
        }

        // fallback to the Ivy 2.2.0 behavior
        String proxyHost = System.getProperty("http.proxyHost");
        return getRequestingHost().equals(proxyHost);
    }

}
