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
import java.util.Map;

import org.apache.ivy.core.settings.IvyVariableContainer;
import org.apache.ivy.core.settings.IvyVariableContainerImpl;
import org.apache.ivy.util.Message;
import org.apache.tools.ant.Project;

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

    public Map getVariables() {
        Map r = new HashMap(super.getVariables());
        r.putAll(project.getProperties());
        r.putAll(overwrittenProperties);
        return r;
    }

    public void setVariable(String varName, String value, boolean overwrite) {
        if (overwrite) {
            Message.debug("setting '" + varName + "' to '" + value + "'");
            overwrittenProperties.put(varName, value);
        } else {
            super.setVariable(varName, value, overwrite);
        }
    }
}
