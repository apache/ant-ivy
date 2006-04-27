/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.extendable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class UnmodifiableExtendableItem implements ExtendableItem {
    private Map _attributes = new HashMap();
    private Map _unmodifiableAttributesView = Collections.unmodifiableMap(_attributes);
    private Map _stdAttributes = new HashMap();
    private Map _unmodifiableStdAttributesView = Collections.unmodifiableMap(_stdAttributes);
    private Map _extraAttributes = new HashMap();
    private Map _unmodifiableExtraAttributesView = Collections.unmodifiableMap(_extraAttributes);
    
    public UnmodifiableExtendableItem(Map stdAttributes, Map extraAttributes) {
        if (stdAttributes != null) {
            _attributes.putAll(stdAttributes);
            _stdAttributes.putAll(stdAttributes);
        }
        if (extraAttributes != null) {
            _attributes.putAll(extraAttributes);
            _extraAttributes.putAll(extraAttributes);
        }
    }
    public String getAttribute(String attName) {
        return (String)_attributes.get(attName);
    }
    public String getExtraAttribute(String attName) {
        return (String)_extraAttributes.get(attName);
    }
    public String getStandardAttribute(String attName) {
        return (String)_stdAttributes.get(attName);
    }
    protected void setExtraAttribute(String attName, String attValue) {
        setAttribute(attName, attValue, true);
    }
    protected void setStandardAttribute(String attName, String attValue) {
        setAttribute(attName, attValue, false);
    }
    protected void setAttribute(String attName, String attValue, boolean extra) {
        if (extra) {
            _extraAttributes.put(attName, attValue);
        } else {
            _stdAttributes.put(attName, attValue);
        }
        _attributes.put(attName, attValue);
    }
    public Map getAttributes() {
        return _unmodifiableAttributesView;
    }
    public Map getStandardAttributes() {
        return _unmodifiableStdAttributesView;
    }
    public Map getExtraAttributes() {
        return _unmodifiableExtraAttributesView;
    }

}
