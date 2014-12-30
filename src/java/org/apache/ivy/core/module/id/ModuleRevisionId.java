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

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.util.extendable.UnmodifiableExtendableItem;

/**
 * Identifies a module in a particular version
 * 
 * @see <a href="package-summary.html">org.apache.ivy.core.module.id</a>
 */
public class ModuleRevisionId extends UnmodifiableExtendableItem {

    private static final String ENCODE_SEPARATOR = ModuleId.ENCODE_SEPARATOR;

    private static final String ENCODE_PREFIX = "+";

    private static final String NULL_ENCODE = "@#:NULL:#@";

    static final String STRICT_CHARS_PATTERN = "[a-zA-Z0-9\\-/\\._+=]";

    private static final String REV_STRICT_CHARS_PATTERN = "[a-zA-Z0-9\\-/\\._+=,\\[\\]\\{\\}\\(\\):@]";

    private static final Map<ModuleRevisionId, WeakReference<ModuleRevisionId>> CACHE = new WeakHashMap<ModuleRevisionId, WeakReference<ModuleRevisionId>>();

    /**
     * Pattern to use to matched mrid text representation.
     * 
     * @see #parse(String)
     */
    public static final Pattern MRID_PATTERN = Pattern.compile("(" + STRICT_CHARS_PATTERN + "*)"
            + "#(" + STRICT_CHARS_PATTERN + "+)" + "(?:#(" + STRICT_CHARS_PATTERN + "+))?" + ";("
            + REV_STRICT_CHARS_PATTERN + "+)");

    /**
     * Same as MRID_PATTERN but using non capturing groups, useful to build larger regexp
     */
    public static final Pattern NON_CAPTURING_PATTERN = Pattern.compile("(?:"
            + STRICT_CHARS_PATTERN + "*)" + "#(?:" + STRICT_CHARS_PATTERN + "+)" + "(?:#(?:"
            + STRICT_CHARS_PATTERN + "+))?" + ";(?:" + REV_STRICT_CHARS_PATTERN + "+)");

    /**
     * Parses a module revision id text representation and returns a new {@link ModuleRevisionId}
     * instance corresponding to the parsed String.
     * <p>
     * The result is unspecified if the module doesn't respect strict name conventions.
     * </p>
     * 
     * @param mrid
     *            the text representation of the module (as returned by {@link #toString()}). Must
     *            not be <code>null</code>.
     * @return a {@link ModuleRevisionId} corresponding to the given text representation
     * @throws IllegalArgumentException
     *             if the given text representation does not match the {@link ModuleRevisionId} text
     *             representation rules.
     */
    public static ModuleRevisionId parse(String mrid) {
        Matcher m = MRID_PATTERN.matcher(mrid.trim());
        if (!m.matches()) {
            throw new IllegalArgumentException(
                    "module revision text representation do not match expected pattern."
                            + " given mrid='" + mrid + "' expected form=" + MRID_PATTERN.pattern());
        }

        // CheckStyle:MagicNumber| OFF
        return newInstance(m.group(1), m.group(2), m.group(3), m.group(4));
        // CheckStyle:MagicNumber| ON
    }

    public static ModuleRevisionId newInstance(String organisation, String name, String revision) {
        return intern(new ModuleRevisionId(ModuleId.newInstance(organisation, name), revision));
    }

    public static ModuleRevisionId newInstance(String organisation, String name, String revision,
            Map<String, String> extraAttributes) {
        return intern(new ModuleRevisionId(ModuleId.newInstance(organisation, name), revision,
                extraAttributes));
    }

    public static ModuleRevisionId newInstance(String organisation, String name, String branch,
            String revision) {
        return intern(new ModuleRevisionId(ModuleId.newInstance(organisation, name), branch,
                revision));
    }

    public static ModuleRevisionId newInstance(String organisation, String name, String branch,
            String revision, Map<String, String> extraAttributes) {
        return intern(new ModuleRevisionId(ModuleId.newInstance(organisation, name), branch,
                revision, extraAttributes));
    }

    public static ModuleRevisionId newInstance(String organisation, String name, String branch,
            String revision, Map<String, String> extraAttributes,
            boolean replaceNullBranchWithDefault) {
        return intern(new ModuleRevisionId(ModuleId.newInstance(organisation, name), branch,
                revision, extraAttributes, replaceNullBranchWithDefault));
    }

    public static ModuleRevisionId newInstance(ModuleRevisionId mrid, String rev) {
        return intern(new ModuleRevisionId(mrid.getModuleId(), mrid.getBranch(), rev,
                mrid.getQualifiedExtraAttributes()));
    }

    public static ModuleRevisionId newInstance(ModuleRevisionId mrid, String branch, String rev) {
        return intern(new ModuleRevisionId(mrid.getModuleId(), branch, rev,
                mrid.getQualifiedExtraAttributes()));
    }

    /**
     * Returns an intern instance of the given ModuleRevisionId if any, or put the given
     * ModuleRevisionId in a cache of intern instances and returns it.
     * <p>
     * This method should be called on ModuleRevisionId created with one of the constructor to
     * decrease memory footprint.
     * </p>
     * <p>
     * When using static newInstances methods, this method is already called.
     * </p>
     * 
     * @param moduleRevisionId
     *            the module revision id to intern
     * @return an interned ModuleRevisionId
     */
    public static ModuleRevisionId intern(ModuleRevisionId moduleRevisionId) {
        ModuleRevisionId r = null;

        synchronized (CACHE) {
            WeakReference<ModuleRevisionId> ref = CACHE.get(moduleRevisionId);
            if (ref != null) {
                r = ref.get();
            }
            if (r == null) {
                r = moduleRevisionId;
                CACHE.put(r, new WeakReference<ModuleRevisionId>(r));
            }
        }

        return r;
    }

    private final ModuleId moduleId;

    private final String branch;

    private final String revision;

    private int hash;

    // TODO: make these constructors private and use only static factory methods

    public ModuleRevisionId(ModuleId moduleId, String revision) {
        this(moduleId, null, revision, null);
    }

    public ModuleRevisionId(ModuleId moduleId, String branch, String revision) {
        this(moduleId, branch, revision, null);
    }

    private ModuleRevisionId(ModuleId moduleId, String revision, Map<String, String> extraAttributes) {
        this(moduleId, null, revision, extraAttributes);
    }

    private ModuleRevisionId(ModuleId moduleId, String branch, String revision,
            Map<String, String> extraAttributes) {
        this(moduleId, branch, revision, extraAttributes, true);
    }

    private ModuleRevisionId(ModuleId moduleId, String branch, String revision,
            Map<String, String> extraAttributes, boolean replaceNullBranchWithDefault) {
        super(null, extraAttributes);
        this.moduleId = moduleId;
        IvyContext context = IvyContext.getContext();
        this.branch = (replaceNullBranchWithDefault && branch == null)
        // we test if there's already an Ivy instance loaded, to avoid loading a default one
        // just to get the default branch
        ? (context.peekIvy() == null ? null : context.getSettings().getDefaultBranch(moduleId))
                : branch;
        this.revision = revision == null ? Ivy.getWorkingRevision() : normalizeRevision(revision);
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

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ModuleRevisionId)) {
            return false;
        }
        ModuleRevisionId other = (ModuleRevisionId) obj;

        if (!other.getRevision().equals(getRevision())) {
            return false;
        } else if (other.getBranch() == null && getBranch() != null) {
            return false;
        } else if (other.getBranch() != null && !other.getBranch().equals(getBranch())) {
            return false;
        } else if (!other.getModuleId().equals(getModuleId())) {
            return false;
        } else {
            return other.getQualifiedExtraAttributes().equals(getQualifiedExtraAttributes());
        }
    }

    @Override
    public int hashCode() {
        if (hash == 0) {
            // CheckStyle:MagicNumber| OFF
            hash = 31;
            hash = hash * 13 + (getBranch() == null ? 0 : getBranch().hashCode());
            hash = hash * 13 + getRevision().hashCode();
            hash = hash * 13 + getModuleId().hashCode();
            hash = hash * 13 + getQualifiedExtraAttributes().hashCode();
            // CheckStyle:MagicNumber| ON
        }
        return hash;
    }

    @Override
    public String toString() {
        return moduleId + (branch == null || branch.length() == 0 ? "" : "#" + branch) + ";"
                + (revision == null ? "NONE" : revision);
    }

    public String encodeToString() {
        StringBuffer buf = new StringBuffer();
        Map<String, String> attributes = new HashMap<String, String>(getAttributes());
        attributes.keySet().removeAll(getExtraAttributes().keySet());
        attributes.putAll(getQualifiedExtraAttributes());

        for (Entry<String, String> att : attributes.entrySet()) {
            String attName = att.getKey();
            String value = att.getValue();
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
        Map<String, String> attributes = new HashMap<String, String>();
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
        String org = attributes.remove(IvyPatternHelper.ORGANISATION_KEY);
        String mod = attributes.remove(IvyPatternHelper.MODULE_KEY);
        String rev = attributes.remove(IvyPatternHelper.REVISION_KEY);
        String branch = attributes.remove(IvyPatternHelper.BRANCH_KEY);
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

    /**
     * [revision] is a valid revision in maven. This method strips the '[' and ']' characters. Cfr.
     * http://docs.codehaus.org/x/IGU
     */
    private static String normalizeRevision(String revision) {
        if (revision.startsWith("[") && revision.endsWith("]") && revision.indexOf(',') == -1) {
            if (IvyPatternHelper.getTokenString(IvyPatternHelper.REVISION_KEY).equals(revision)) {
                // this is the case when listing dynamic revions
                return revision;
            }

            return revision.substring(1, revision.length() - 1);
        } else {
            return revision;
        }
    }
}
