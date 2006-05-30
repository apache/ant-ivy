/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * version 1.3.1
 */
package fr.jayasoft.ivy.url;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.HashMap;
import java.util.Map;

import fr.jayasoft.ivy.util.Message;

/**
 * 
 * @author Christian Riege
 */
public final class IvyAuthenticator extends Authenticator {

    /**
     * A Map of Credentials objects keyed by the 'key' of the Credentials.
     */
    private final static Map keyring = new HashMap();

    /**
     * The sole instance.
     */
    public final static IvyAuthenticator INSTANCE = new IvyAuthenticator();

    /**
     * Private c'tor to prevent instantiation. Also installs this as the default
     * Authenticator to use by the JVM.
     */
    private IvyAuthenticator() {
        // Install this as the default Authenticator object.
        Authenticator.setDefault(this);
    }

    // API ******************************************************************

    void addCredentials(String realm, String host, String userName, String passwd) {
        Credentials c = new Credentials(realm, host, userName, passwd);
        keyring.put(c.getKey(), c);
    }

    // Overriding Authenticator *********************************************

    protected PasswordAuthentication getPasswordAuthentication() {
        final String key = buildKey(getRequestingPrompt(), getRequestingHost());
        Credentials c = (Credentials) keyring.get(key);
        Message.debug("authentication: k: " + key + " c: '" + c + "'");
        return c != null ? c.getAuthentication() : null;
    }

    // Private helper code

    private String buildKey(String realm, String host) {
        final String credentialKey;
        if (realm == null || "".equals(realm)) {
            credentialKey = host;
        } else {
            credentialKey = realm + "@" + host;
        }
        return credentialKey;
    }

    // Private helper class storing credentials *****************************

    private class Credentials {
        private final String key;
        private final PasswordAuthentication auth;

        public Credentials(String realm, String host, String userName, String passwd) {
            auth = new PasswordAuthentication(userName, passwd.toCharArray());
            key = buildKey(realm, host);
        }

        public String getKey() {
            return key;
        }

        public PasswordAuthentication getAuthentication() {
            return auth;
        }

        public boolean equals(Object o) {
            if(o == null) {
                return false;
            }

            if(o instanceof Credentials) {
                Credentials c = (Credentials) o;
                return key.equals(c.key);
            }

            return false;
        }
        
        public int hashCode() {
            return key.hashCode();
        }

        public String toString() {
            return key + " " + auth.getUserName() + "/" + auth.getPassword().toString();
        }

    }

}
