/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ivy.plugins.parser.ParserSettings;
import org.xml.sax.Attributes;

public final class ExtendableItemHelper {
    private ExtendableItemHelper() {
    }

    private static final char separator = '.';

    /**
     * Decode qualified attribute name from blob.
     *
     * @param blob Encoded attribute name
     * @param prefix Prefix used during encoding
     * @return String
     * @see #encodeAttribute(String, String)
     */
    public static String decodeAttribute(String blob, String prefix) {
        // Decoding <qualifier>:<attribute> from 
        //   <pre><qlen><sep><qualifier><sep><attribute>
        // where qualifier (with following separator) is optional.
        StringBuilder builder = new StringBuilder(blob);

        // Skipping prefix
        int cur = prefix.length();

        // Resolving length of qualifier
        int sepi = blob.indexOf(separator, cur);
        int qlen = Integer.parseInt(blob.substring(cur, sepi));

        // Skipping to attribute and reclaiming ':'
        cur = sepi + 1;
        if (qlen > 0)
            builder.setCharAt(cur + qlen, ':');

        return builder.substring(cur);
    }

    /**
     * Encode qualified attribute name into blob
     * to be used in XML report.
     *
     * @param attribute Qualified (or unqualified) attribute name
     * @param prefix Prefix
     * @return String
     * @see #decodeAttribute(String, String)
     */
    public static String encodeAttribute(String attribute, String prefix) {
        StringBuilder builder = new StringBuilder(
            attribute.length() + prefix.length() + 5
        );

        // Resolving length of qualifier
        int coloni = attribute.indexOf(':');
        int qlen = coloni == -1
            ? 0
            : coloni;

        // Encoding <qualifier>:<attribute> as
        //   <pre><qlen><sep><qualifier><sep><attribute>
        // where qualifier (with following separator) is optional;
        // e.g. `extra-3.foo.bar` for `foo:bar`, or `extra-0.foo` for `foo`
        builder.append(prefix);
        builder.append(qlen);
        builder.append(separator);
        builder.append(attribute);

        // Replacing ':' with '.' in order for report XML to not
        // deal with all those pesky namespaces (c)
        if (qlen > 0) {
            int cur = builder.length() - attribute.length();
            builder.setCharAt(cur + qlen, separator);
        }

        return builder.toString();
    }

    public static Map<String, String> getExtraAttributes(Attributes attributes, String prefix) {
        Map<String, String> ret = new HashMap<>();
        for (int i = 0; i < attributes.getLength(); i++) {
            if (attributes.getQName(i).startsWith(prefix)) {
                String name = decodeAttribute(attributes.getQName(i), prefix);
                String value = attributes.getValue(i);
                ret.put(name, value);
            }
        }
        return ret;
    }

    @Deprecated
    public static Map<String, String> getExtraAttributes(ParserSettings settings,
                                                         Attributes attributes, String[] ignoredAttNames) {
        return getExtraAttributes(settings, attributes, Arrays.asList(ignoredAttNames));
    }

    /**
     * Extract from the XML attribute the extra Ivy ones
     *
     * @param settings ParserSettings
     * @param attributes Attributes
     * @param ignoredAttNames
     *            the XML attributes names which are not extra but Ivy core ones
     * @return Map&lt;String,String&gt;
     */
    public static Map<String, String> getExtraAttributes(ParserSettings settings,
            Attributes attributes, List<String> ignoredAttNames) {
        Map<String, String> ret = new HashMap<>();
        for (int i = 0; i < attributes.getLength(); i++) {
            if (!ignoredAttNames.contains(attributes.getQName(i))) {
                ret.put(attributes.getQName(i), settings.substitute(attributes.getValue(i)));
            }
        }
        return ret;
    }

    @Deprecated
    public static void fillExtraAttributes(ParserSettings settings, DefaultExtendableItem item,
                                           Attributes attributes, String[] ignoredAttNames) {
        fillExtraAttributes(settings, item, attributes, Arrays.asList(ignoredAttNames));
    }

    public static void fillExtraAttributes(ParserSettings settings, DefaultExtendableItem item,
            Attributes attributes, List<String> ignoredAttNames) {
        Map<String, String> att = getExtraAttributes(settings, attributes, ignoredAttNames);
        for (Map.Entry<String, String> entry : att.entrySet()) {
            item.setExtraAttribute(entry.getKey(), entry.getValue());
        }
    }

}
