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
package org.apache.ivy.core.module.descriptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
            switch (name) {
                case "private":
                    return PRIVATE;
                case "public":
                    return PUBLIC;
                default:
                    throw new IllegalArgumentException("unknown visibility " + name);
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
        Collection<Configuration> extendingConfs = new ArrayList<>();
        for (Configuration cf : confs) {
            if (cf != null && Arrays.asList(cf.getExtends()).contains(conf)) {
                extendingConfs.add(cf);
                extendingConfs.addAll(findConfigurationExtending(cf.getName(), confs));
            }
        }
        return extendingConfs;
    }

    private String name;

    private String description;

    private Set<String> extendsFrom;

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
        this(source.getAttributes(), source.getQualifiedExtraAttributes(), source.getName(),
                source.getVisibility(), source.getDescription(), source.getExtends(),
                source.isTransitive(), source.getDeprecated(), sourceModule);
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
            String name, Visibility visibility, String description, String[] exts,
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
        if (exts == null) {
            extendsFrom = Collections.emptySet();
        } else {
            extendsFrom = new LinkedHashSet<>();
            for (String ext : exts) {
                extendsFrom.add(ext.trim());
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
        return extendsFrom.toArray(new String[extendsFrom.size()]);
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
        return obj instanceof Configuration && ((Configuration) obj).getName().equals(getName());
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

        Set<String> newExtends = new LinkedHashSet<>();
        for (String extend : extendsFrom) {
            switch (extend) {
                case "*":
                    addOther(configs, null, newExtends);
                    break;
                case "*(public)":
                    addOther(configs, Visibility.PUBLIC, newExtends);
                    break;
                case "*(private)":
                    addOther(configs, Visibility.PRIVATE, newExtends);
                    break;
                default:
                    newExtends.add(extend);
                    break;
            }
        }

        this.extendsFrom = newExtends;
    }

    private void addOther(Configuration[] allConfigs, Visibility visibility, Set<String> configs) {
        for (Configuration allConfig : allConfigs) {
            String currentName = allConfig.getName();
            if (!name.equals(currentName)
                    && (visibility == null || visibility.equals(allConfig.getVisibility()))) {
                configs.add(currentName);
            }
        }
    }

}
