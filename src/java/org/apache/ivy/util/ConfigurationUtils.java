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
package org.apache.ivy.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.Configuration.Visibility;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;

/**
 * Class containing several utility methods for working with configurations.
 */
public final class ConfigurationUtils {

    /**
     * Private constructor to avoid instantiation of this class.
     */
    private ConfigurationUtils() {
    }

    /**
     * Replace the wildcards in the given configuration array, by the name of the given
     * ModuleDescriptor
     * 
     * The supported wildcards are:
     * <ul>
     * <li><b><tt>*</tt> :</b> all configurations</li>
     * <li><b><tt>*(public)</tt> :</b> all public configurations</li>
     * <li><b><tt>*(private)</tt> :</b> all private configurations</li>
     * </ul>
     * If the given array of configurations is <code>null</code>, all configurations from the given
     * module descriptor are returned, including if this array is empty.
     * 
     * @param confs
     *            the configurations, can contain wildcards
     * @param md
     *            the configurations where the wildcards are replaced
     * @return
     */
    public static String[] replaceWildcards(String[] confs, ModuleDescriptor md) {
        if (confs == null) {
            return md.getConfigurationsNames();
        }

        Set result = new LinkedHashSet();
        Set excluded = new LinkedHashSet();
        for (int i = 0; i < confs.length; i++) {
            if ("*".equals(confs[i])) {
                result.addAll(Arrays.asList(md.getConfigurationsNames()));
            } else if ("*(public)".equals(confs[i])) {
                Configuration[] all = md.getConfigurations();
                for (int j = 0; j < all.length; j++) {
                    if (all[j].getVisibility().equals(Visibility.PUBLIC)) {
                        result.add(all[j].getName());
                    }
                }
            } else if ("*(private)".equals(confs[i])) {
                Configuration[] all = md.getConfigurations();
                for (int j = 0; j < all.length; j++) {
                    if (all[j].getVisibility().equals(Visibility.PRIVATE)) {
                        result.add(all[j].getName());
                    }
                }
            } else if (confs[i].startsWith("!")) {
                excluded.add(confs[i].substring(1));
            } else {
                result.add(confs[i]);
            }
        }
        for (Iterator iter = excluded.iterator(); iter.hasNext();) {
            result.remove(iter.next());
        }

        return (String[]) result.toArray(new String[result.size()]);
    }

}
