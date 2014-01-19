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
package org.apache.ivy.core.module.descriptor;

import java.util.Iterator;
import java.util.Map;

/**
 * A configuration which is actually the intersection of several configurations.
 */
public class ConfigurationIntersection extends Configuration {

    private final Map/* <String, Configuration> */intersectedConfs;

    public ConfigurationIntersection(String confName,
            Map /* <String, Configuration> */intersectedConfs) {
        super(confName);
        this.intersectedConfs = intersectedConfs;
    }

    /**
     * Returns the list of configurations' names this object is an intersection of.
     * <p>
     * This list is built from the configuration name, if some of these configuration names have
     * actually not been recognized in the module, they will be <code>null</code> when accessed from
     * {@link #getIntersectedConfiguration(String)}.
     * </p>
     * 
     * @return the list of configurations' names this object is an intersection of.
     */
    public String[] getIntersectedConfigurationNames() {
        return (String[]) intersectedConfs.keySet().toArray(new String[intersectedConfs.size()]);
    }

    /**
     * Returns the intersected {@link Configuration} object for the given conf name, or
     * <code>null</code> if the given conf name is not part of this intersection or if this conf
     * name isn't defined in the module in which this intersection has been built.
     * 
     * @param confName
     *            the name of the configuration to return.
     * @return the intersected {@link Configuration} object for the given conf name
     */
    public Configuration getIntersectedConfiguration(String confName) {
        return (Configuration) intersectedConfs.get(confName);
    }

    public Visibility getVisibility() {
        for (Iterator it = intersectedConfs.values().iterator(); it.hasNext();) {
            Configuration c = (Configuration) it.next();
            if (c != null && Visibility.PRIVATE.equals(c.getVisibility())) {
                return Visibility.PRIVATE;
            }
        }
        return Visibility.PUBLIC;
    }
}
