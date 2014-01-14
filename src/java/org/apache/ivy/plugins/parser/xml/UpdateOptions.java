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
package org.apache.ivy.plugins.parser.xml;

import java.util.Collections;
import java.util.Date;
import java.util.Map;

import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.plugins.namespace.Namespace;
import org.apache.ivy.plugins.parser.ParserSettings;

public class UpdateOptions {
    /**
     * Settings to use for update, may be <code>null</code>.
     */
    private ParserSettings settings = null;

    /**
     * Namespace in which the module to update is, may be <code>null</code>.
     */
    private Namespace namespace = null;

    /**
     * Map from ModuleId of dependencies to new revision (as String)
     */
    private Map resolvedRevisions = Collections.EMPTY_MAP;

    /**
     * Map from ModuleId of dependencies to new branch (as String)
     */
    private Map resolvedBranches = Collections.EMPTY_MAP;

    /**
     * the new status, <code>null</code> to keep the old one
     */
    private String status = null;

    /**
     * the new revision, <code>null</code> to keep the old one
     */
    private String revision = null;

    /**
     * the new publication date, <code>null</code> to keep the old one
     */
    private Date pubdate = null;

    /**
     * Should included information be replaced
     */
    private boolean replaceInclude = true;

    /**
     * Should parent descriptor be merged inline
     */
    private boolean merge = true;

    private ModuleDescriptor mergedDescriptor = null;

    /**
     * Configurations to exclude during update, or <code>null</code> to keep all confs.
     */
    private String[] confsToExclude = null;

    /**
     * True to set branch information on dependencies to default branch when omitted, false to keep
     * it as is.
     */
    private boolean updateBranch = true;

    private String branch;

    /**
     * True to indicate that the revConstraint attribute should be generated if applicable, false to
     * never generate the revConstraint attribute.
     */
    private boolean generateRevConstraint = true;

    public ParserSettings getSettings() {
        return settings;
    }

    public UpdateOptions setSettings(ParserSettings settings) {
        this.settings = settings;
        return this;
    }

    public Namespace getNamespace() {
        return namespace;
    }

    public UpdateOptions setNamespace(Namespace ns) {
        this.namespace = ns;
        return this;
    }

    public Map getResolvedRevisions() {
        return resolvedRevisions;
    }

    public UpdateOptions setResolvedRevisions(Map resolvedRevisions) {
        this.resolvedRevisions = resolvedRevisions;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public UpdateOptions setStatus(String status) {
        this.status = status;
        return this;
    }

    public String getRevision() {
        return revision;
    }

    public UpdateOptions setRevision(String revision) {
        this.revision = revision;
        return this;
    }

    public Date getPubdate() {
        return pubdate;
    }

    public UpdateOptions setPubdate(Date pubdate) {
        this.pubdate = pubdate;
        return this;
    }

    public boolean isReplaceInclude() {
        return replaceInclude;
    }

    public UpdateOptions setReplaceInclude(boolean replaceInclude) {
        this.replaceInclude = replaceInclude;
        return this;
    }

    public boolean isMerge() {
        // only return true if merge is set to true and if there is something to merge!
        return merge && (mergedDescriptor != null)
                && (mergedDescriptor.getInheritedDescriptors().length > 0);
    }

    public UpdateOptions setMerge(boolean merge) {
        this.merge = merge;
        return this;
    }

    public ModuleDescriptor getMergedDescriptor() {
        return mergedDescriptor;
    }

    public UpdateOptions setMergedDescriptor(ModuleDescriptor mergedDescriptor) {
        this.mergedDescriptor = mergedDescriptor;
        return this;
    }

    public String[] getConfsToExclude() {
        return confsToExclude;
    }

    public UpdateOptions setConfsToExclude(String[] confsToExclude) {
        this.confsToExclude = confsToExclude;
        return this;
    }

    public boolean isUpdateBranch() {
        return updateBranch;
    }

    public UpdateOptions setUpdateBranch(boolean updateBranch) {
        this.updateBranch = updateBranch;
        return this;
    }

    public String getBranch() {
        return branch;
    }

    public UpdateOptions setBranch(String pubBranch) {
        this.branch = pubBranch;
        return this;
    }

    public boolean isGenerateRevConstraint() {
        return generateRevConstraint;
    }

    public UpdateOptions setGenerateRevConstraint(boolean generateRevConstraint) {
        this.generateRevConstraint = generateRevConstraint;
        return this;
    }

    public Map getResolvedBranches() {
        return resolvedBranches;
    }

    public UpdateOptions setResolvedBranches(Map resolvedBranches) {
        this.resolvedBranches = resolvedBranches;
        return this;
    }
}
