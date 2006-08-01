package fr.jayasoft.ivy.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

/**
 * An implementation of Properties which stores the values encrypted.
 * 
 * The use is transparent from the user point of view (use as any Properties instance),
 * except that get, put and putAll do not handle encryption/decryption.
 * 
 * This means that get returns the encrypted value, while put and putAll
 * puts given values without encrypting them.
 * 
 * It this thus recommended to void using them, use setProperty and getProperty instead.
 * 
 * @author Xavier Hanin
 *
 */
public class EncrytedProperties extends Properties {
	
	public EncrytedProperties() {
		super();
	}
	
	public synchronized Object setProperty(String key, String value) {
		return StringUtils.decrypt((String)super.setProperty(key, StringUtils.encrypt(value)));
	}
	public String getProperty(String key) {
		return StringUtils.decrypt(super.getProperty(key));
	}
	public String getProperty(String key, String defaultValue) {
		return StringUtils.decrypt(super.getProperty(key, StringUtils.encrypt(defaultValue)));
	}
	public boolean containsValue(Object value) {
		return super.containsValue(StringUtils.encrypt((String)value));
	}
	public synchronized boolean contains(Object value) {
		return super.contains(StringUtils.encrypt((String)value));
	}
	public Collection values() {
		List ret = new ArrayList(super.values());
		for (int i=0; i<ret.size(); i++) {
			ret.set(i, StringUtils.decrypt((String)ret.get(i)));
		}
		return ret;
	}
}
