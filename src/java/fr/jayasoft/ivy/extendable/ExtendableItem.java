/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.extendable;

import java.util.Map;

public interface ExtendableItem {
    /**
     * Gets the value of an attribute
     * Can be used to access the value of a standard attribute (like organisation, revision) or of an extra attribute.
     * @param attName the name of the attribute to get
     * @return the value of the attribute, null if the attribute doesn't exist
     */
    String getAttribute(String attName);
    /**
     * Gets the value of a standard attribute
     * Can be used only to access the value of a standard attribute (like organisation, revision), not an extra one
     * @param attName the name of the standard attribute to get
     * @return the value of the attribute, null if the attribute doesn't exist
     */
    String getStandardAttribute(String attName);
    /**
     * Gets the value of an extra attribute
     * Can be used only to access the value of an extra attribute, not a standard one (like organisation, revision)
     * @param attName the name of the extra attribute to get
     * @return the value of the attribute, null if the attribute doesn't exist
     */
    String getExtraAttribute(String attName);
    
    /**
     * Returns a Map of all attributes of this extendable item, including standard and extra ones.
     * The Map keys are attribute names as Strings, and values are corresponding attribute values (as String too)
     * @return
     */
    Map getAttributes();
    /**
     * Returns a Map of all standard attributes of this extendable item.
     * The Map keys are attribute names as Strings, and values are corresponding attribute values (as String too)
     * @return
     */
    Map getStandardAttributes();
    /**
     * Returns a Map of all extra attributes of this extendable item.
     * The Map keys are attribute names as Strings, and values are corresponding attribute values (as String too)
     * @return
     */
    Map getExtraAttributes();
}