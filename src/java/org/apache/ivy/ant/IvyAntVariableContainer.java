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
package org.apache.ivy.ant;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.ivy.core.settings.IvyVariableContainer;
import org.apache.ivy.core.settings.IvyVariableContainerImpl;
import org.apache.ivy.util.Message;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Property;

class IvyAntVariableContainer extends IvyVariableContainerImpl implements IvyVariableContainer {

    private Map overwrittenProperties = new HashMap();

    private Project project;

    public IvyAntVariableContainer(Project project) {
        this.project = project;
    }

    public String getVariable(String name) {
        String r = (String) overwrittenProperties.get(name);
        if (r == null) {
            r = project.getProperty(name);
        }
        if (r == null) {
            r = super.getVariable(name);
        }
        return r;
    }

    public void setVariable(String varName, String value, boolean overwrite) {
        if (overwrite) {
            Message.debug("setting '" + varName + "' to '" + value + "'");
            overwrittenProperties.put(varName, substitute(value));
        } else {
            super.setVariable(varName, value, overwrite);
        }
    }

    /**
     * Updates the Ant Project used in this container with variables set in Ivy.
     * 
     * All variables defined in Ivy will be set in the Ant project under two names:
     * <ul>
     * <li>the name of the variable</li>
     * <li>the name of the variable suffxied with a dot + the given id, if the given id is not null</li>
     * </ul>
     * 
     * @param id
     *            The identifier of the settings in which the variables have been set, which should
     *            be used as property names suffix
     */
    public void updateProject(String id) {
        Map r = new HashMap(super.getVariables());
        r.putAll(overwrittenProperties);
        for (Iterator it = r.entrySet().iterator(); it.hasNext();) {
            Entry entry = (Entry) it.next();

            setPropertyIfNotSet((String) entry.getKey(), (String) entry.getValue());
            if (id != null) {
                setPropertyIfNotSet((String) entry.getKey() + "." + id, (String) entry.getValue());
            }
        }

        if (getEnvironmentPrefix() != null) {
            Property propTask = new Property();
            propTask.setProject(project);
            propTask.setEnvironment(getEnvironmentPrefix());
            propTask.init();
            propTask.execute();
        }
    }

    private void setPropertyIfNotSet(String property, String value) {
        if (project.getProperty(property) == null) {
            project.setProperty(property, value);
        }
    }

    public Object clone() {
        IvyAntVariableContainer result = (IvyAntVariableContainer) super.clone();
        result.overwrittenProperties = (HashMap) ((HashMap) this.overwrittenProperties).clone();
        return result;
    }
}
