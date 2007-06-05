/**
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

    protected Map _overwrittenProperties = new HashMap();

    protected Project _project;

    public IvyAntVariableContainer(Project project) {
        this._project = project;
    }

    public String getVariable(String name) {
        String r = (String) _overwrittenProperties.get(name);
        if (r == null) {
            r = _project.getProperty(name);
        }
        if (r == null) {
            r = super.getVariable(name);
        }
        return r;
    }

    public Map getVariables() {
        Map r = new HashMap(super.getVariables());
        r.putAll(_project.getProperties());
        r.putAll(_overwrittenProperties);
        return r;
    }

    public void setVariable(String varName, String value, boolean overwrite) {
        if (overwrite) {
            Message.debug("setting '" + varName + "' to '" + value + "'");
            _overwrittenProperties.put(varName, value);
        } else {
            super.setVariable(varName, value, overwrite);
        }
    }
}
