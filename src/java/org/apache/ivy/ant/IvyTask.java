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
package org.apache.ivy.ant;

import java.util.Date;
import java.util.Locale;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.util.DateUtil;
import org.apache.ivy.util.Message;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Reference;

import static org.apache.ivy.util.StringUtils.joinArray;
import static org.apache.ivy.util.StringUtils.splitToArray;

/**
 * Base class for all ivy ant tasks, deal particularly with ivy instance storage in ant project.
 */
public abstract class IvyTask extends Task {
    public static final String ANT_PROJECT_CONTEXT_KEY = "ant-project";

    private Boolean validate = null;

    private Reference antIvyEngineRef = null;

    protected boolean doValidate(IvySettings ivy) {
        if (validate == null) {
            return ivy.doValidate();
        }
        return validate;
    }

    public boolean isValidate() {
        return validate == null || validate;
    }

    public void setValidate(boolean validate) {
        this.validate = validate;
    }

    public void setSettingsRef(Reference ref) {
        antIvyEngineRef = ref;
    }

    public Reference getSettingsRef() {
        return antIvyEngineRef;
    }

    protected IvySettings getSettings() {
        return getIvyInstance().getSettings();
    }

    protected Ivy getIvyInstance() {
        Object antIvyEngine;
        if (antIvyEngineRef == null) {
            antIvyEngine = IvyAntSettings.getDefaultInstance(this);
        } else {
            antIvyEngine = antIvyEngineRef.getReferencedObject(getProject());
            if (!antIvyEngine.getClass().getName().equals(IvyAntSettings.class.getName())) {
                throw new BuildException(antIvyEngineRef.getRefId()
                        + " doesn't reference an ivy:settings", getLocation());
            }
            if (!(antIvyEngine instanceof IvyAntSettings)) {
                throw new BuildException(antIvyEngineRef.getRefId()
                        + " has been defined in a different classloader.  "
                        + "Please use the same loader when defining your task, or "
                        + "redeclare your ivy:settings in this classloader", getLocation());
            }
        }
        Ivy ivy = ((IvyAntSettings) antIvyEngine).getConfiguredIvyInstance(this);
        AntMessageLogger.register(this, ivy);
        return ivy;
    }

    protected void setResolved(ResolveReport report, boolean keep) {
        ModuleDescriptor md = report.getModuleDescriptor();
        String[] confs = report.getConfigurations();
        if (keep) {
            getProject().addReference("ivy.resolved.report", report);
            getProject().addReference("ivy.resolved.configurations.ref", confs);
            getProject().addReference("ivy.resolved.descriptor", md);
        }
        String suffix = md.getModuleRevisionId().getModuleId().getOrganisation() + "."
                + md.getModuleRevisionId().getModuleId().getName();
        getProject().addReference("ivy.resolved.report." + suffix, report);
        getProject().addReference("ivy.resolved.descriptor." + suffix, md);
        getProject().addReference("ivy.resolved.configurations.ref." + suffix, confs);
    }

    protected void setResolved(ResolveReport report, String resolveId, boolean keep) {
        setResolved(report, keep);
        if (resolveId == null) {
            return;
        }
        ModuleDescriptor md = report.getModuleDescriptor();
        String[] confs = report.getConfigurations();
        getProject().addReference("ivy.resolved.report." + resolveId, report);
        getProject().addReference("ivy.resolved.descriptor." + resolveId, md);
        getProject().addReference("ivy.resolved.configurations.ref." + resolveId, confs);
    }

    protected String[] getResolvedConfigurations(String org, String module, boolean strict) {
        return (String[]) getReference("ivy.resolved.configurations.ref", org, module, strict);
    }

    protected <T> T getResolvedDescriptor(String resolveId) {
        return getResolvedDescriptor(resolveId, true);
    }

    protected <T> T getResolvedDescriptor(String resolveId, boolean strict) {
        T result = getProject().getReference("ivy.resolved.descriptor." + resolveId);
        if (strict && result == null) {
            throw new BuildException("ModuleDescriptor for resolve with id '" + resolveId
                    + "' not found.");
        }
        return result;
    }

    protected <T> T getResolvedDescriptor(String org, String module) {
        return getResolvedDescriptor(org, module, false);
    }

    protected <T> T getResolvedDescriptor(String org, String module, boolean strict) {
        return getReference("ivy.resolved.descriptor", org, module, strict);
    }

    private <T> T getReference(String prefix, String org, String module, boolean strict) {
        T reference = null;
        if (org != null && module != null) {
            reference = getProject().getReference(prefix + "." + org + "." + module);
        }
        if (!strict && reference == null) {
            reference = getProject().getReference(prefix);
        }
        return reference;
    }

    protected ResolveReport getResolvedReport(String org, String module, String resolveId) {
        if (resolveId == null) {
            return getReference("ivy.resolved.report", org, module, false);
        }
        return getReference("ivy.resolved.report." + resolveId, null, null,
            false);
    }

    protected String[] splitConfs(String conf) {
        return splitToArray(conf);
    }

    protected String mergeConfs(String[] conf) {
        return joinArray(conf, ", ");
    }

    protected static Date getPubDate(String date, Date def) {
        if (date == null) {
            return def;
        }
        if ("now".equals(date.toLowerCase(Locale.US))) {
            return new Date();
        }
        try {
            return DateUtil.parse(date);
        } catch (Exception ex) {
            throw new BuildException("Publication date provided in bad format. Should be '"
                    + DateUtil.DATE_FORMAT_PATTERN + "' and not '" + date + "'!");
        }
    }

    protected String getProperty(String value, IvySettings ivy, String name) {
        if (value == null) {
            return getProperty(ivy, name);
        }
        value = ivy.substitute(value);
        Message.debug("parameter found as attribute value: " + name + "=" + value);
        return value;
    }

    protected String getProperty(String value, IvySettings ivy, String name, String resolveId) {
        if (resolveId == null) {
            return getProperty(value, ivy, name);
        }
        return getProperty(value, ivy, name + "." + resolveId);
    }

    protected String getProperty(IvySettings ivy, String name, String resolveId) {
        if (resolveId == null) {
            return getProperty(ivy, name);
        }
        return getProperty(ivy, name + "." + resolveId);
    }

    protected String getProperty(IvySettings ivy, String name) {
        String val = ivy.getVariable(name);
        if (val == null) {
            val = ivy.substitute(getProject().getProperty(name));
            if (val == null) {
                Message.debug("parameter not found: " + name);
            } else {
                Message.debug("parameter found as ant project property: " + name + "=" + val);
            }
        } else {
            val = ivy.substitute(val);
            Message.debug("parameter found as ivy variable: " + name + "=" + val);
        }
        return val;
    }

    /**
     * Called when task starts its execution.
     */
    protected void prepareTask() {
        getProject().setProperty("ivy.version", Ivy.getIvyVersion());

        // push current project and Ivy on the stack in context
        IvyContext.pushNewCopyContext();
        IvyContext.getContext().setIvy(getIvyInstance());
        IvyContext.getContext().push(ANT_PROJECT_CONTEXT_KEY, getProject());
    }

    /**
     * Called when task is about to finish Should clean up all state related information (stacks for
     * example)
     */
    protected void finalizeTask() {
        if (!IvyContext.getContext().pop(ANT_PROJECT_CONTEXT_KEY, getProject())) {
            Message.error("ANT project popped from stack not equals current! Ignoring");
        }
        IvyContext.popContext();
    }

    /**
     * Ant task execute. Calls prepareTask, doExecute, finalizeTask
     */
    @Override
    public final void execute() throws BuildException {
        try {
            prepareTask();
            doExecute();
        } finally {
            finalizeTask();
        }
    }

    /**
     * The real logic of task execution after project has been set in the context. MUST be
     * implemented by subclasses
     *
     * @throws BuildException if something goes wrong
     */
    public abstract void doExecute() throws BuildException;

    @Override
    public String toString() {
        return getClass().getName() + ":" + getTaskName();
    }

    /**
     * Informs the user that the cache attribute is not supported any more.
     */
    protected void cacheAttributeNotSupported() {
        throw new BuildException(
                "cache attribute is not supported any more. See IVY-685 for details.");
    }

}
