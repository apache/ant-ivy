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
import java.util.Map;
import java.util.Map.Entry;

import org.apache.ivy.plugins.parser.ParserSettings;
import org.xml.sax.Attributes;

public final class ExtendableItemHelper {
    private ExtendableItemHelper() {
    }

    public static Map<String, String> getExtraAttributes(Attributes attributes, String prefix) {
        Map<String, String> ret = new HashMap<String, String>();
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
    public static Map<String, String> getExtraAttributes(ParserSettings settings,
            Attributes attributes, String[] ignoredAttNames) {
        Map<String, String> ret = new HashMap<String, String>();
        Collection<String> ignored = Arrays.asList(ignoredAttNames);
        for (int i = 0; i < attributes.getLength(); i++) {
            if (!ignored.contains(attributes.getQName(i))) {
                ret.put(attributes.getQName(i), settings.substitute(attributes.getValue(i)));
            }
        }
        return ret;
    }

    public static void fillExtraAttributes(ParserSettings settings, DefaultExtendableItem item,
            Attributes attributes, String[] ignoredAttNames) {
        Map<String, String> att = getExtraAttributes(settings, attributes, ignoredAttNames);
        for (Entry<String, String> entry : att.entrySet()) {
            item.setExtraAttribute(entry.getKey(), entry.getValue());
        }
    }

}
