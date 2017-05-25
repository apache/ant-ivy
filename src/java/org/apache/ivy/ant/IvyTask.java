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

import java.util.Date;
import java.util.Locale;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.util.DateUtil;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.StringUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Reference;

/**
 * Base class for all ivy ant tasks, deal particularly with ivy instance storage in ant project.
 */
public abstract class IvyTask extends Task {
    public static final String ANT_PROJECT_CONTEXT_KEY = "ant-project";

    private Boolean validate = null;

    private Reference antIvyEngineRef = null;

    protected boolean doValidate(IvySettings ivy) {
        if (validate != null) {
            return validate.booleanValue();
        }
        return ivy.doValidate();
    }

    public boolean isValidate() {
        return validate == null ? true : validate.booleanValue();
    }

    public void setValidate(boolean validate) {
        this.validate = Boolean.valueOf(validate);
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
        if (antIvyEngineRef != null) {
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
        } else {
            antIvyEngine = IvyAntSettings.getDefaultInstance(this);
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
        if (resolveId != null) {
            ModuleDescriptor md = report.getModuleDescriptor();
            String[] confs = report.getConfigurations();
            getProject().addReference("ivy.resolved.report." + resolveId, report);
            getProject().addReference("ivy.resolved.descriptor." + resolveId, md);
            getProject().addReference("ivy.resolved.configurations.ref." + resolveId, confs);
        }
    }

    protected String[] getResolvedConfigurations(String org, String module, boolean strict) {
        return (String[]) getReference("ivy.resolved.configurations.ref", org, module, strict);
    }

    protected Object getResolvedDescriptor(String resolveId) {
        return getResolvedDescriptor(resolveId, true);
    }

    protected Object getResolvedDescriptor(String resolveId, boolean strict) {
        Object result = getProject().getReference("ivy.resolved.descriptor." + resolveId);
        if (strict && (result == null)) {
            throw new BuildException("ModuleDescriptor for resolve with id '" + resolveId
                    + "' not found.");
        }
        return result;
    }

    protected Object getResolvedDescriptor(String org, String module) {
        return getResolvedDescriptor(org, module, false);
    }

    protected Object getResolvedDescriptor(String org, String module, boolean strict) {
        return getReference("ivy.resolved.descriptor", org, module, strict);
    }

    private Object getReference(String prefix, String org, String module, boolean strict) {
        Object reference = null;
        if (org != null && module != null) {
            reference = getProject().getReference(prefix + "." + org + "." + module);
        }
        if (!strict && reference == null) {
            reference = getProject().getReference(prefix);
        }
        return reference;
    }

    protected ResolveReport getResolvedReport(String org, String module, String resolveId) {
        ResolveReport result = null;

        if (resolveId == null) {
            result = (ResolveReport) getReference("ivy.resolved.report", org, module, false);
        } else {
            result = (ResolveReport) getReference("ivy.resolved.report." + resolveId, null, null,
                false);
        }

        return result;
    }

    protected String[] splitConfs(String conf) {
        if (conf == null) {
            return null;
        }
        String[] confs = conf.split(",");
        for (int i = 0; i < confs.length; i++) {
            confs[i] = confs[i].trim();
        }
        return confs;
    }

    protected String mergeConfs(String[] conf) {
        return StringUtils.join(conf, ", ");
    }

    protected static Date getPubDate(String date, Date def) {
        if (date != null) {
            if ("now".equals(date.toLowerCase(Locale.US))) {
                return new Date();
            }
            try {
                return DateUtil.parse(date);
            } catch (Exception ex) {
                throw new BuildException("Publication date provided in bad format. Should be '"
                        + DateUtil.DATE_FORMAT_PATTERN + "' and not '" + date + "'!");
            }
        } else {
            return def;
        }
    }

    protected String getProperty(String value, IvySettings ivy, String name) {
        if (value == null) {
            return getProperty(ivy, name);
        } else {
            value = ivy.substitute(value);
            Message.debug("parameter found as attribute value: " + name + "=" + value);
            return value;
        }
    }

    protected String getProperty(String value, IvySettings ivy, String name, String resolveId) {
        if (resolveId == null) {
            return getProperty(value, ivy, name);
        } else {
            return getProperty(value, ivy, name + "." + resolveId);
        }
    }

    protected String getProperty(IvySettings ivy, String name, String resolveId) {
        if (resolveId == null) {
            return getProperty(ivy, name);
        } else {
            return getProperty(ivy, name + "." + resolveId);
        }
    }

    protected String getProperty(IvySettings ivy, String name) {
        String val = ivy.getVariable(name);
        if (val == null) {
            val = ivy.substitute(getProject().getProperty(name));
            if (val != null) {
                Message.debug("parameter found as ant project property: " + name + "=" + val);
            } else {
                Message.debug("parameter not found: " + name);
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
            Message.error("ANT project poped from stack not equals current !. Ignoring");
        }
        IvyContext.popContext();
    }

    /**
     * Ant task execute. Calls prepareTask, doExecute, finalzeTask
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
     * @throws BuildException
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
