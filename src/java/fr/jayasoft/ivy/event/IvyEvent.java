/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.event;

import java.util.HashMap;
import java.util.Map;

import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.ModuleDescriptor;
import fr.jayasoft.ivy.ModuleId;
import fr.jayasoft.ivy.ModuleRevisionId;
import fr.jayasoft.ivy.util.StringUtils;

/**
 * The root of all ivy events
 * 
 * Any ivy event knows which ivy instance triggered the event (the source)
 * and also has a name and a map of attributes.
 * 
 * The name of the event represents the event type, usually there is a one - one
 * mapping between event names and IvyEvent subclass, even if this is not mandatory.
 * Example: 
 * pre-resolve
 * pre-resolve-dependency
 * post-download
 * 
 * The map of attributes is a Map from String keys to String values.
 * It is especially useful to filter events, and to get some of their essential data in 
 * some context where access to Java types is not easy (in an ant build file, for example),
 * Example: 
 * pre-resolve (organisation=foo, module=bar, revision=1.0, conf=default)
 * post-download (organisation=foo, module=bar, revision=1.0, artifact=foo-test, type=jar, ext=jar)
 * 
 * @author Xavier Hanin
 *
 */
public class IvyEvent {
    private Ivy _source;
    private String _name;
    private Map _attributes = new HashMap(); 

	protected IvyEvent(Ivy source, String name) {
		_source = source;
		_name = name;
	}
	
	/**
	 * Should only be called during event object construction, since events should be immutable
	 * @param key
	 * @param value
	 */
	protected void addAttribute(String key, String value) {
		_attributes.put(key, value);
	}
	protected void addMDAttributes(ModuleDescriptor md) {
		addMridAttributes(md.getResolvedModuleRevisionId());
	}

	protected void addMridAttributes(ModuleRevisionId mrid) {
		addModuleIdAttributes(mrid.getModuleId());
		addAttribute("revision", mrid.getRevision());
		addAttributes(mrid.getExtraAttributes());
	}

	protected void addModuleIdAttributes(ModuleId moduleId) {
		addAttribute("organisation", moduleId.getOrganisation());
		addAttribute("module", moduleId.getName());
	}

	protected void addConfsAttribute(String[] confs) {
		addAttribute("conf", StringUtils.join(confs, ", "));
	}

	protected void addAttributes(Map attributes) {
		_attributes.putAll(attributes);
	}
	

	public Ivy getSource() {
		return _source;
	}

	public String getName() {
		return _name;
	}

	/**
	 * Returns the attributes of this event, as a Map(String->String)
	 * @return the attributes of this event, as a Map(String->String)
	 */
	public Map getAttributes() {
		return new HashMap(_attributes);
	}
    
    public String toString() {
    	return getName()+" "+getAttributes();
    }
    
    public boolean equals(Object obj) {
    	if (! (obj instanceof IvyEvent)) {
    		return false;
    	}
    	IvyEvent e = (IvyEvent) obj;
    	
    	return getSource().equals(e.getSource()) 
    		&& getName().equals(e.getName()) 
    		&& _attributes.equals(e._attributes);
    }
    
    public int hashCode() {
    	int hash = 37;
    	hash = 13 * hash + getSource().hashCode();
    	hash = 13 * hash + getName().hashCode();
    	hash = 13 * hash + _attributes.hashCode();
    	return hash;
    }
}
