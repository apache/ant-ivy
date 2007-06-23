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
package org.apache.ivy.core.module.id;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.util.extendable.UnmodifiableExtendableItem;

/**
 *
 */
public class ModuleRevisionId extends UnmodifiableExtendableItem {
    private static final String ENCODE_SEPARATOR = ModuleId.ENCODE_SEPARATOR;

    private static final String ENCODE_PREFIX = "+";

    private static final String NULL_ENCODE = "@#:NULL:#@";

    public static ModuleRevisionId newInstance(String organisation, String name, String revision) {
        return new ModuleRevisionId(new ModuleId(organisation, name), revision);
    }

    public static ModuleRevisionId newInstance(String organisation, String name, String revision,
            Map extraAttributes) {
        return new ModuleRevisionId(new ModuleId(organisation, name), revision, extraAttributes);
    }

    public static ModuleRevisionId newInstance(String organisation, String name, String branch,
            String revision) {
        return new ModuleRevisionId(new ModuleId(organisation, name), branch, revision);
    }

    public static ModuleRevisionId newInstance(String organisation, String name, String branch,
            String revision, Map extraAttributes) {
        return new ModuleRevisionId(new ModuleId(organisation, name), branch, revision,
                extraAttributes);
    }

    public static ModuleRevisionId newInstance(ModuleRevisionId mrid, String rev) {
        return new ModuleRevisionId(mrid.getModuleId(), mrid.getBranch(), rev, mrid
                .getExtraAttributes());
    }

    private ModuleId moduleId;

    private String branch;

    private String revision;

    private int hash;

    public ModuleRevisionId(ModuleId moduleId, String revision) {
        this(moduleId, null, revision, null);
    }

    public ModuleRevisionId(ModuleId moduleId, String branch, String revision) {
        this(moduleId, branch, revision, null);
    }

    public ModuleRevisionId(ModuleId moduleId, String revision, Map extraAttributes) {
        this(moduleId, null, revision, extraAttributes);
    }

    public ModuleRevisionId(ModuleId moduleId, String branch, String revision, 
            Map extraAttributes) {
        super(null, extraAttributes);
        this.moduleId = moduleId;
        this.branch = branch == null ? IvyContext.getContext().getSettings().getDefaultBranch(
            moduleId) : branch;
        this.revision = revision == null ? Ivy.getWorkingRevision() : revision;
        setStandardAttribute(IvyPatternHelper.ORGANISATION_KEY, this.moduleId.getOrganisation());
        setStandardAttribute(IvyPatternHelper.MODULE_KEY, this.moduleId.getName());
        setStandardAttribute(IvyPatternHelper.BRANCH_KEY, this.branch);
        setStandardAttribute(IvyPatternHelper.REVISION_KEY, this.revision);
    }

    public ModuleId getModuleId() {
        return moduleId;
    }

    public String getName() {
        return getModuleId().getName();
    }

    public String getOrganisation() {
        return getModuleId().getOrganisation();
    }

    public String getRevision() {
        return revision;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ModuleRevisionId)) {
            return false;
        }
        ModuleRevisionId other = (ModuleRevisionId) obj;
        return other.getRevision().equals(getRevision())
                && (other.getBranch() == null ? getBranch() == null : other.getBranch().equals(
                    getBranch())) && other.getModuleId().equals(getModuleId())
                && other.getExtraAttributes().equals(getExtraAttributes());
    }

    public int hashCode() {
        if (hash == 0) {
            //CheckStyle:MagicNumber| OFF
            hash = 31;
            hash = hash * 13 + (getBranch() == null ? 0 : getBranch().hashCode());
            hash = hash * 13 + getRevision().hashCode();
            hash = hash * 13 + getModuleId().hashCode();
            hash = hash * 13 + getAttributes().hashCode();
            //CheckStyle:MagicNumber| ON
        }
        return hash;
    }


    public String toString() {
        return "[ " + moduleId.getOrganisation() + " | " + moduleId.getName()
                + (branch == null || branch.length() == 0 ? "" : " | " + branch) + " | "
                + (revision == null ? "NONE" : revision) + " ]";
    }

    public String encodeToString() {
        StringBuffer buf = new StringBuffer();
        Map attributes = getAttributes();
        for (Iterator iter = attributes.keySet().iterator(); iter.hasNext();) {
            String attName = (String) iter.next();
            String value = (String) attributes.get(attName);
            value = value == null ? NULL_ENCODE : value;
            buf.append(ENCODE_PREFIX).append(attName).append(ENCODE_SEPARATOR)
                    .append(ENCODE_PREFIX).append(value).append(ENCODE_SEPARATOR);
        }
        return buf.toString();
    }

    public static ModuleRevisionId decode(String encoded) {
        String[] parts = encoded.split(ENCODE_SEPARATOR);
        if (parts.length % 2 != 0) {
            throw new IllegalArgumentException("badly encoded module revision id: '" + encoded
                    + "'");
        }
        Map attributes = new HashMap();
        for (int i = 0; i < parts.length; i += 2) {
            String attName = parts[i];
            if (!attName.startsWith(ENCODE_PREFIX)) {
                throw new IllegalArgumentException("badly encoded module revision id: '" + encoded
                        + "': " + attName + " doesn't start with " + ENCODE_PREFIX);
            } else {
                attName = attName.substring(1);
            }
            String attValue = parts[i + 1];
            if (!attValue.startsWith(ENCODE_PREFIX)) {
                throw new IllegalArgumentException("badly encoded module revision id: '" + encoded
                        + "': " + attValue + " doesn't start with " + ENCODE_PREFIX);
            } else {
                attValue = attValue.substring(1);
            }
            if (NULL_ENCODE.equals(attValue)) {
                attValue = null;
            }
            attributes.put(attName, attValue);
        }
        String org = (String) attributes.remove(IvyPatternHelper.ORGANISATION_KEY);
        String mod = (String) attributes.remove(IvyPatternHelper.MODULE_KEY);
        String rev = (String) attributes.remove(IvyPatternHelper.REVISION_KEY);
        String branch = (String) attributes.remove(IvyPatternHelper.BRANCH_KEY);
        if (org == null) {
            throw new IllegalArgumentException("badly encoded module revision id: '" + encoded
                    + "': no organisation");
        }
        if (mod == null) {
            throw new IllegalArgumentException("badly encoded module revision id: '" + encoded
                    + "': no module name");
        }
        if (rev == null) {
            throw new IllegalArgumentException("badly encoded module revision id: '" + encoded
                    + "': no revision");
        }
        return newInstance(org, mod, branch, rev, attributes);
    }

    public String getBranch() {
        return branch;
    }
}
