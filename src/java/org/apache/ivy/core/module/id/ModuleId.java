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
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ivy.core.IvyPatternHelper;

/**
 * Identifies a module, without revision information
 * 
 * @see <a href="package-summary.html">org.apache.ivy.core.module.id</a>
 */
public class ModuleId implements Comparable<ModuleId> {

    static final String ENCODE_SEPARATOR = ":#@#:";

    private static final Map<ModuleId, WeakReference<ModuleId>> CACHE = new WeakHashMap<ModuleId, WeakReference<ModuleId>>();

    /**
     * Returns a ModuleId for the given organization and module name.
     * 
     * @param org
     *            the module's organization, can be <code>null</code>
     * @param name
     *            the module's name, must not be <code>null</code>
     * @return a ModuleId instance
     */
    public static ModuleId newInstance(String org, String name) {
        return intern(new ModuleId(org, name));
    }

    /**
     * Returns an intern instance of a ModuleId equals to the given ModuleId if any, or the given
     * ModuleId.
     * <p>
     * This is useful to reduce the number of instances of ModuleId kept in memory, and thus reduce
     * memory footprint.
     * </p>
     * 
     * @param moduleId
     *            the module id to return
     * @return a unit instance of the given module id.
     */
    public static ModuleId intern(ModuleId moduleId) {
        ModuleId r = null;

        synchronized (CACHE) {
            WeakReference<ModuleId> ref = CACHE.get(moduleId);
            if (ref != null) {
                r = ref.get();
            }
            if (r == null) {
                r = moduleId;
                CACHE.put(r, new WeakReference<ModuleId>(r));
            }
        }

        return r;
    }

    private String organisation;

    private String name;

    private int hash;

    private Map<String, String> attributes = new HashMap<String, String>();

    /**
     * Constructor.
     * 
     * @param organisation
     *            The organisation which creates the module.
     * @param name
     *            The name of the module.
     */
    public ModuleId(String organisation, String name) {
        if (name == null) {
            throw new IllegalArgumentException("null name not allowed");
        }
        this.organisation = organisation;
        this.name = name;
        attributes.put(IvyPatternHelper.ORGANISATION_KEY, organisation);
        attributes.put(IvyPatternHelper.MODULE_KEY, name);
    }

    /**
     * Returns the name of the module.
     * 
     * @return The name of the module.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the name of the organisation.
     * 
     * @return The name of the organisation.
     */
    public String getOrganisation() {
        return organisation;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ModuleId)) {
            return false;
        }
        ModuleId other = (ModuleId) obj;
        if (other.organisation == null) {
            return organisation == null && other.name.equals(name);
        } else {
            return other.organisation.equals(organisation) && other.name.equals(name);
        }
    }

    @Override
    public int hashCode() {
        if (hash == 0) {
            // CheckStyle:MagicNumber| OFF
            hash = 31;
            hash = hash * 13 + (organisation == null ? 0 : organisation.hashCode());
            hash = hash * 13 + name.hashCode();
            // CheckStyle:MagicNumber| ON
        }
        return hash;
    }

    @Override
    public String toString() {
        return organisation + "#" + name;
    }

    public int compareTo(ModuleId that) {
        int result = organisation.compareTo(that.organisation);
        if (result == 0) {
            result = name.compareTo(that.name);
        }
        return result;
    }

    /**
     * Returns the encoded String representing this ModuleId.
     * 
     * @return The ModuleId encoded as String.
     */
    public String encodeToString() {
        return getOrganisation() + ENCODE_SEPARATOR + getName();
    }

    /**
     * Returns a Map of all attributes of this module id. The Map keys are attribute names as
     * Strings, and values are corresponding attribute values (as String too).
     * 
     * @return A Map instance containing all the attributes and their values.
     */
    public Map<String, String> getAttributes() {
        return attributes;
    }

    /**
     * Returns a ModuleId
     * 
     * @param encoded
     * @return The new ModuleId.
     * @throws IllegalArgumentException
     *             If the given String could not be decoded.
     */
    public static ModuleId decode(String encoded) {
        String[] parts = encoded.split(ENCODE_SEPARATOR);
        if (parts.length != 2) {
            throw new IllegalArgumentException("badly encoded module id: '" + encoded + "'");
        }
        return new ModuleId(parts[0], parts[1]);
    }

    /**
     * Pattern to use to matched mid text representation.
     * 
     * @see #parse(String)
     */
    public static final Pattern MID_PATTERN = Pattern.compile("("
            + ModuleRevisionId.STRICT_CHARS_PATTERN + "*)" + "#("
            + ModuleRevisionId.STRICT_CHARS_PATTERN + "+)");

    /**
     * Parses the module id text representation and returns it as a {@link ModuleId} instance.
     * 
     * @param mid
     *            the module id text representation to parse
     * @return the ModuleId instance corresponding to the representation
     * @throws IllegalArgumentException
     *             if the given text representation cannot be parsed
     */
    public static ModuleId parse(String mid) {
        Matcher m = MID_PATTERN.matcher(mid);
        if (!m.matches()) {
            throw new IllegalArgumentException(
                    "module text representation do not match expected pattern." + " given mid='"
                            + mid + "' expected form=" + MID_PATTERN.pattern());
        }

        // CheckStyle:MagicNumber| OFF
        return newInstance(m.group(1), m.group(2));
        // CheckStyle:MagicNumber| ON
    }
}
