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
package org.apache.ivy.core.module.descriptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.util.extendable.DefaultExtendableItem;

/**
 * Represents a module configuration
 */
public class Configuration extends DefaultExtendableItem implements InheritableItem {
    public static final class Visibility {
        public static final Visibility PUBLIC = new Visibility("public");

        public static final Visibility PRIVATE = new Visibility("private");

        public static Visibility getVisibility(String name) {
            if ("private".equals(name)) {
                return PRIVATE;
            } else if ("public".equals(name)) {
                return PUBLIC;
            } else {
                throw new IllegalArgumentException("unknwon visibility " + name);
            }
        }

        private String name;

        private Visibility(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static Collection<Configuration> findConfigurationExtending(String conf,
            Configuration[] confs) {
        Collection<Configuration> extendingConfs = new ArrayList<Configuration>();
        for (int i = 0; i < confs.length; i++) {
            if (confs[i] != null && Arrays.asList(confs[i].getExtends()).contains(conf)) {
                extendingConfs.add(confs[i]);
                extendingConfs.addAll(findConfigurationExtending(confs[i].getName(), confs));
            }
        }
        return extendingConfs;
    }

    private String name;

    private String description;

    private String[] extendsFrom;

    private Visibility visibility;

    private boolean transitive = true;

    private String deprecated;

    private ModuleRevisionId sourceModule;

    /**
     * Creates a new configuration.
     * 
     * @param name
     *            the name of the configuration
     */
    public Configuration(String name) {
        this(name, Visibility.PUBLIC, null, null, true, null);
    }

    public Configuration(Configuration source, ModuleRevisionId sourceModule) {
        this(source.getAttributes(), source.getQualifiedExtraAttributes(), source.getName(), source
                .getVisibility(), source.getDescription(), source.getExtends(), source
                .isTransitive(), source.getDeprecated(), sourceModule);
    }

    /**
     * Creates a new configuration.
     * 
     * @param name
     *            the name of the configuration
     * @param visibility
     *            the visibility of the configuration
     * @param description
     *            a description
     * @param ext
     *            the configurations to extend from
     * @param transitive
     *            indicates if the configuration is transitive
     * @param deprecated
     *            the deprecation message
     */
    public Configuration(String name, Visibility visibility, String description, String[] ext,
            boolean transitive, String deprecated) {
        this(null, null, name, visibility, description, ext, transitive, deprecated, null);
    }

    private Configuration(Map<String, String> attributes, Map<String, String> extraAttributes,
            String name, Visibility visibility, String description, String[] ext,
            boolean transitive, String deprecated, ModuleRevisionId sourceModule) {
        super(attributes, extraAttributes);

        if (name == null) {
            throw new NullPointerException("null configuration name not allowed");
        }
        if (visibility == null) {
            throw new NullPointerException("null visibility not allowed");
        }
        this.name = name;
        this.visibility = visibility;
        this.description = description;
        if (ext == null) {
            extendsFrom = new String[0];
        } else {
            extendsFrom = new String[ext.length];
            for (int i = 0; i < ext.length; i++) {
                extendsFrom[i] = ext[i].trim();
            }
        }
        this.transitive = transitive;
        this.deprecated = deprecated;
        this.sourceModule = sourceModule;
    }

    /**
     * Returns the deprecation message, or <tt>null</tt> if not specified.
     * 
     * @return Returns the deprecation message.
     */
    public String getDeprecated() {
        return deprecated;
    }

    /**
     * @return Returns the description. It may be null.
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return Returns the extends. May be empty, but never null.
     */
    public String[] getExtends() {
        return extendsFrom;
    }

    /**
     * @return Returns the name. Never null;
     */
    public String getName() {
        return name;
    }

    /**
     * @return Returns the visibility. Never null.
     */
    public Visibility getVisibility() {
        return visibility;
    }

    /**
     * @return Returns the transitive.
     */
    public final boolean isTransitive() {
        return transitive;
    }

    public ModuleRevisionId getSourceModule() {
        return sourceModule;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Configuration)) {
            return false;
        }
        return ((Configuration) obj).getName().equals(getName());
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    public void replaceWildcards(ModuleDescriptor md) {
        if (this != md.getConfiguration(name)) {
            throw new IllegalArgumentException(
                    "The given ModuleDescriptor doesn't own this configuration!");
        }

        Configuration[] configs = md.getConfigurations();

        Set<String> newExtends = new LinkedHashSet<String>();
        for (int j = 0; j < extendsFrom.length; j++) {
            if ("*".equals(extendsFrom[j])) {
                addOther(configs, null, newExtends);
            } else if ("*(public)".equals(extendsFrom[j])) {
                addOther(configs, Visibility.PUBLIC, newExtends);
            } else if ("*(private)".equals(extendsFrom[j])) {
                addOther(configs, Visibility.PRIVATE, newExtends);
            } else {
                newExtends.add(extendsFrom[j]);
            }
        }

        this.extendsFrom = newExtends.toArray(new String[newExtends.size()]);
    }

    private void addOther(Configuration[] allConfigs, Visibility visibility, Set<String> configs) {
        for (int i = 0; i < allConfigs.length; i++) {
            String currentName = allConfigs[i].getName();
            if (!name.equals(currentName)
                    && ((visibility == null) || visibility.equals(allConfigs[i].getVisibility()))) {
                configs.add(currentName);
            }
        }
    }

}
