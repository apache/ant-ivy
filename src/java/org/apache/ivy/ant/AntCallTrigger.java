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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.event.IvyEvent;
import org.apache.ivy.plugins.trigger.AbstractTrigger;
import org.apache.ivy.plugins.trigger.Trigger;
import org.apache.ivy.util.Message;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.CallTarget;
import org.apache.tools.ant.taskdefs.Property;

/**
 * Triggers an call to an ant target on an event occurence.
 * <p>
 * This trigger only works when ivy is called from an ant build file, otherwise the trigger only log
 * a failure.
 * <p>
 * Example of use in an ivysettings file:
 * 
 * <pre>
 * &lt;ant-call-trigger event=&quot;post-download-artifact&quot; filter=&quot;type=zip&quot;
 * target=&quot;unzip&quot;/&gt;
 * </pre>
 * 
 * Triggers a call to the target "unzip" for any downloaded artifact of type zip
 * 
 * @see AntBuildTrigger
 * @since 1.4
 */
public class AntCallTrigger extends AbstractTrigger implements Trigger {
    private boolean onlyonce = true;

    private String target = null;

    private Collection calls = new ArrayList();

    private String prefix;

    public void progress(IvyEvent event) {
        Project project = (Project) IvyContext.peekInContextStack(IvyTask.ANT_PROJECT_CONTEXT_KEY);
        if (project == null) {
            Message.info("ant call trigger can only be used from an ant build. Ignoring.");
            return;
        }
        if (onlyonce && isTriggered(event)) {
            Message.verbose("call already triggered for this event, skipping: " + event);
        } else {
            CallTarget call = new CallTarget();

            call.setProject(project);
            call.setTaskName("antcall");

            Map attributes = event.getAttributes();
            String target = IvyPatternHelper.substituteTokens(getTarget(), attributes);
            call.setTarget(target);

            for (Iterator iter = attributes.keySet().iterator(); iter.hasNext();) {
                String key = (String) iter.next();
                String value = (String) attributes.get(key);
                Property p = call.createParam();
                p.setName(prefix == null ? key : prefix + key);
                p.setValue(value == null ? "" : value);
            }

            Message.verbose("triggering ant call: target=" + target + " for " + event);
            call.execute();
            markTriggered(event);

            Message.debug("triggered ant call finished: target=" + target + " for " + event);
        }
    }

    private void markTriggered(IvyEvent event) {
        calls.add(event);
    }

    private boolean isTriggered(IvyEvent event) {
        return calls.contains(event);
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public boolean isOnlyonce() {
        return onlyonce;
    }

    public void setOnlyonce(boolean onlyonce) {
        this.onlyonce = onlyonce;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
        if (!prefix.endsWith(".")) {
            this.prefix += ".";
        }
    }
}
