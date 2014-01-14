/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivy.util.extendable;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.ivy.plugins.parser.ParserSettings;
import org.xml.sax.Attributes;

public final class ExtendableItemHelper {
    private ExtendableItemHelper() {
    }

    public static Map getExtraAttributes(Attributes attributes, String prefix) {
        Map ret = new HashMap();
        for (int i = 0; i < attributes.getLength(); i++) {
            if (attributes.getQName(i).startsWith(prefix)) {
                ret.put(attributes.getQName(i).substring(prefix.length()), attributes.getValue(i));
            }
        }
        return ret;
    }

    /**
     * Extract from the XML attribute the extra Ivy ones
     * 
     * @param settings
     * @param attributes
     * @param ignoredAttNames
     *            the XML attributes names which are not extra but Ivy core ones
     * @return
     */
    public static Map getExtraAttributes(ParserSettings settings, Attributes attributes,
            String[] ignoredAttNames) {
        Map ret = new HashMap();
        Collection ignored = Arrays.asList(ignoredAttNames);
        for (int i = 0; i < attributes.getLength(); i++) {
            if (!ignored.contains(attributes.getQName(i))) {
                ret.put(attributes.getQName(i), settings.substitute(attributes.getValue(i)));
            }
        }
        return ret;
    }

    public static void fillExtraAttributes(ParserSettings settings, DefaultExtendableItem item,
            Attributes attributes, String[] ignoredAttNames) {
        Map att = getExtraAttributes(settings, attributes, ignoredAttNames);
        for (Iterator iter = att.keySet().iterator(); iter.hasNext();) {
            String attName = (String) iter.next();
            String attValue = (String) att.get(attName);
            item.setExtraAttribute(attName, attValue);
        }
    }

}
