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

import java.io.File;
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
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Ant;
import org.apache.tools.ant.taskdefs.Property;

/**
 * Triggers an ant build on an event occurence.
 * <p>
 * Example of use:
 * 
 * <pre>
 * &lt;ant-build-trigger event=&quot;pre-resolve-dependency&quot; 
 *                    filter=&quot;revision=latest.integration&quot;
 *                    antfile=&quot;/path/to/[module]/build.xml&quot; 
 *                    target=&quot;compile&quot;/&gt;
 * </pre>
 * 
 * Triggers an ant build for any dependency in asked in latest.integration, just before resolving
 * the dependency.
 * </p>
 * <p>
 * The onlyonce property is used to tell if the ant build should be triggered only once, or several
 * times in the same build.
 * </p>
 * 
 * @see AntCallTrigger
 * @since 1.4
 */
public class AntBuildTrigger extends AbstractTrigger implements Trigger {
    private boolean onlyOnce = true;

    private String target = null;

    private Collection builds = new ArrayList();

    private String buildFilePattern;

    private String prefix;

    public void progress(IvyEvent event) {
        File f = getBuildFile(event);
        if (f.exists()) {
            if (onlyOnce && isBuilt(f)) {
                Message.verbose("target build file already built, skipping: " + f);
            } else {
                Ant ant = new Ant();
                Project project = (Project) IvyContext
                        .peekInContextStack(IvyTask.ANT_PROJECT_CONTEXT_KEY);
                if (project == null) {
                    project = new Project();
                    project.init();
                }

                ant.setProject(project);
                ant.setTaskName("ant");

                ant.setAntfile(f.getAbsolutePath());
                ant.setInheritAll(false);
                String target = getTarget();
                if (target != null) {
                    ant.setTarget(target);
                }
                Map atts = event.getAttributes();
                for (Iterator iter = atts.keySet().iterator(); iter.hasNext();) {
                    String key = (String) iter.next();
                    String value = (String) atts.get(key);
                    if (value != null) {
                        Property p = ant.createProperty();
                        p.setName(prefix == null ? key : prefix + key);
                        p.setValue(value);
                    }
                }

                Message.verbose("triggering build: " + f + " target=" + target + " for " + event);
                try {
                    ant.execute();
                } catch (BuildException e) {
                    Message.verbose("Exception occurred while executing target " + target);
                    throw e;
                }
                markBuilt(f);

                Message.debug("triggered build finished: " + f + " target=" + target + " for "
                        + event);
            }
        } else {
            Message.verbose("no build file found for dependency, skipping: " + f);
        }
    }

    private void markBuilt(File f) {
        builds.add(f.getAbsolutePath());
    }

    private boolean isBuilt(File f) {
        return builds.contains(f.getAbsolutePath());
    }

    private File getBuildFile(IvyEvent event) {
        return IvyContext
                .getContext()
                .getSettings()
                .resolveFile(
                    IvyPatternHelper.substituteTokens(getBuildFilePattern(), event.getAttributes()));
    }

    public String getBuildFilePattern() {
        return buildFilePattern;
    }

    public void setAntfile(String pattern) {
        buildFilePattern = pattern;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public boolean isOnlyonce() {
        return onlyOnce;
    }

    public void setOnlyonce(boolean onlyonce) {
        onlyOnce = onlyonce;
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
