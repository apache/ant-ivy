package fr.jayasoft.ivy.url;

import java.util.HashMap;
import java.util.Map;

import fr.jayasoft.ivy.util.Credentials;
import fr.jayasoft.ivy.util.Message;

/**
 * 
 * @author Christian Riege
 * @author Xavier Hanin
 */
public class CredentialsStore {
    /**
     * A Map of Credentials objects keyed by the 'key' of the Credentials.
     */
    private final static Map keyring = new HashMap();
    public final static CredentialsStore INSTANCE = new CredentialsStore();
    
    private CredentialsStore() {
    }

    public void addCredentials(String realm, String host, String userName, String passwd) {
    	if (userName == null) {
    		return;
    	}
        Credentials c = new Credentials(realm, host, userName, passwd);
        Message.debug("credentials added: "+c);
        keyring.put(c.getKey(), c);
        // add also with host only, to be able to find credential with host only 
        // (useful for httpclient especially)
        keyring.put(c.getHost(), c); 
    }

	public Credentials getCredentials(String realm, String host) {
		return (Credentials) keyring.get(Credentials.buildKey(realm, host));
	}

}
