package fr.jayasoft.ivy.util;

/**
 * 
 * @author Christian Riege
 * @author Xavier Hanin
 */
public class Credentials {
	private String _realm;
	private String _host;
	private String _userName;
	private String _passwd;
	
	public Credentials(String realm, String host, String userName, String passwd) {
		_realm = realm;
		_host = host;
		_userName = userName;
		_passwd = passwd;
	}
	
	public String getHost() {
		return _host;
	}
	public String getPasswd() {
		return _passwd;
	}
	public String getRealm() {
		return _realm;
	}
	public String getUserName() {
		return _userName;
	}

	public static String buildKey(String realm, String host) {
        if (realm == null || "".equals(realm.trim())) {
            return host;
        } else {
            return realm + "@" + host;
        }
    }

	public String toString() {
		return getKey() + " " + getUserName() + "/" + getPasswd();
	}

	public boolean equals(Object o) {
		if(o == null) {
			return false;
		}

		if(o instanceof Credentials) {
			Credentials c = (Credentials) o;
			return getKey().equals(c.getKey());
		}

		return false;
	}
	
	public int hashCode() {
		return getKey().hashCode();
	}
	
	public String getKey() {
		return buildKey(_realm, _host);
	}
}
