/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy;

import java.util.HashMap;
import java.util.Map;

/**
 * An item which is meant to be extended, i.e. defined using extra attributes
 */
public class DefaultExtendableItem {
    private Map _attributes = new HashMap();
    public String getAttribute(String attName) {
        return (String)_attributes.get(attName);
    }
    public void setAttribute(String attName, String attValue) {
        _attributes.put(attName, attValue);
    }
}
