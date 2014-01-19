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
package org.apache.ivy.core.settings;

import java.util.HashMap;
import java.util.Map;

import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.util.Message;

public class IvyVariableContainerImpl implements IvyVariableContainer {

    private Map variables;

    private String envPrefix;

    public IvyVariableContainerImpl() {
        this.variables = new HashMap();
    }

    public IvyVariableContainerImpl(Map variables) {
        this.variables = variables;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.ivy.core.settings.IvyVariableContainer#setVariable(java.lang.String,
     * java.lang.String, boolean)
     */
    public void setVariable(String varName, String value, boolean overwrite) {
        if (overwrite || !variables.containsKey(varName)) {
            Message.debug("setting '" + varName + "' to '" + value + "'");
            variables.put(varName, substitute(value));
        } else {
            Message.debug("'" + varName + "' already set: discarding '" + value + "'");
        }
    }

    public void setEnvironmentPrefix(String prefix) {
        if ((prefix != null) && !prefix.endsWith(".")) {
            this.envPrefix = prefix + ".";
        } else {
            this.envPrefix = prefix;
        }
    }

    protected String substitute(String value) {
        return IvyPatternHelper.substituteVariables(value, this);
    }

    protected Map getVariables() {
        return variables;
    }

    protected String getEnvironmentPrefix() {
        return envPrefix;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.ivy.core.settings.IvyVariableContainer#getVariable(java.lang.String)
     */
    public String getVariable(String name) {
        String val = null;
        if ((envPrefix != null) && name.startsWith(envPrefix)) {
            val = System.getenv(name.substring(envPrefix.length()));
        } else {
            val = (String) variables.get(name);
        }

        return val;
    }

    public Object clone() {
        IvyVariableContainerImpl clone;
        try {
            clone = (IvyVariableContainerImpl) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("unable to clone a " + this.getClass());
        }
        clone.variables = new HashMap(this.variables);
        return clone;
    }
}
