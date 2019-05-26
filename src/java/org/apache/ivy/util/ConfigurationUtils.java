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
package org.apache.ivy.util;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;

import static org.apache.ivy.core.module.descriptor.Configuration.Visibility.PRIVATE;
import static org.apache.ivy.core.module.descriptor.Configuration.Visibility.PUBLIC;

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
     * @return configurations
     */
    public static String[] replaceWildcards(String[] confs, ModuleDescriptor md) {
        if (confs == null) {
            return md.getConfigurationsNames();
        }

        Set<String> result = new LinkedHashSet<>();
        Set<String> excluded = new LinkedHashSet<>();
        for (String conf : confs) {
            if ("*".equals(conf)) {
                result.addAll(Arrays.asList(md.getConfigurationsNames()));
            } else if ("*(public)".equals(conf)) {
                for (Configuration cf : md.getConfigurations()) {
                    if (PUBLIC.equals(cf.getVisibility())) {
                        result.add(cf.getName());
                    }
                }
            } else if ("*(private)".equals(conf)) {
                for (Configuration cf : md.getConfigurations()) {
                    if (PRIVATE.equals(cf.getVisibility())) {
                        result.add(cf.getName());
                    }
                }
            } else if (conf.startsWith("!")) {
                excluded.add(conf.substring(1));
            } else {
                result.add(conf);
            }
        }
        for (String ex : excluded) {
            result.remove(ex);
        }

        return result.toArray(new String[result.size()]);
    }

}
