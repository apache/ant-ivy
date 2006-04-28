/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.extendable;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.xml.sax.Attributes;


public class ExtendableItemHelper {

    public static Map getExtraAttributes(Attributes attributes, String prefix) {
        Map ret = new HashMap();
        for (int i=0; i<attributes.getLength(); i++) {
            if (attributes.getQName(i).startsWith(prefix)) {
                ret.put(attributes.getQName(i).substring(prefix.length()), attributes.getValue(i));
            }
        }
        return ret;
    }

    public static Map getExtraAttributes(Attributes attributes, String[] ignoredAttNames) {
        Map ret = new HashMap();
        Collection ignored = Arrays.asList(ignoredAttNames);
        for (int i=0; i<attributes.getLength(); i++) {
            if (!ignored.contains(attributes.getQName(i))) {
                ret.put(attributes.getQName(i), attributes.getValue(i));
            }
        }
        return ret;
    }

    public static void fillExtraAttributes(DefaultExtendableItem item, Attributes attributes, String[] ignoredAttNames) {
        Map att = getExtraAttributes(attributes, ignoredAttNames);
        for (Iterator iter = att.keySet().iterator(); iter.hasNext();) {
            String attName = (String)iter.next();
            String attValue = (String)att.get(attName);
            item.setExtraAttribute(attName, attValue);
        }
    }

}
