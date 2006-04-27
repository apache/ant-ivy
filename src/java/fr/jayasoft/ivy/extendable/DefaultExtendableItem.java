/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.extendable;

import java.util.Map;

/**
 * An item which is meant to be extended, i.e. defined using extra attributes
 */
public class DefaultExtendableItem extends UnmodifiableExtendableItem {
    public DefaultExtendableItem() {
        this(null, null);
    }
    public DefaultExtendableItem(Map stdAttributes, Map extraAttributes) {
        super(stdAttributes, extraAttributes);
    }
    public void setExtraAttribute(String attName, String attValue) {
        super.setExtraAttribute(attName, attValue);
    }
    public void setStandardAttribute(String attName, String attValue) {
        super.setStandardAttribute(attName, attValue);
    }
    public void setAttribute(String attName, String attValue, boolean extra) {
        super.setAttribute(attName, attValue, extra);
    }
}
