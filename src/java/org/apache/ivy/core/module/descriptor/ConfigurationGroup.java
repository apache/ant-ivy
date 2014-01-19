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
 * A configuration which is actually a group of several configurations.
 */
public class ConfigurationGroup extends Configuration {

    private final Map/* <String, Configuration> */members;

    public ConfigurationGroup(String confName, Map /* <String, Configuration> */members) {
        super(confName);
        this.members = members;
    }

    /**
     * Returns the list of configurations' names this object is a group of.
     * <p>
     * This list is built from the configuration name, if some of these configuration names have
     * actually not been recognized in the module, they will be <code>null</code> when accessed from
     * {@link #getIntersectedConfiguration(String)}.
     * </p>
     * 
     * @return the list of configurations' names this object is an intersection of.
     */
    public String[] getMembersConfigurationNames() {
        return (String[]) members.keySet().toArray(new String[members.size()]);
    }

    /**
     * Returns the {@link Configuration} object for the given conf name, or <code>null</code> if the
     * given conf name is not part of this group or if this conf name isn't defined in the module in
     * which this group has been built.
     * 
     * @param confName
     *            the name of the configuration to return.
     * @return the member {@link Configuration} object for the given conf name
     */
    public Configuration getMemberConfiguration(String confName) {
        return (Configuration) members.get(confName);
    }

    public Visibility getVisibility() {
        for (Iterator it = members.values().iterator(); it.hasNext();) {
            Configuration c = (Configuration) it.next();
            if (c != null && Visibility.PRIVATE.equals(c.getVisibility())) {
                return Visibility.PRIVATE;
            }
        }
        return Visibility.PUBLIC;
    }
}
