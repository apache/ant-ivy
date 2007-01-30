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

import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.ivy.util.extendable.DefaultExtendableItem;



/**
 * Represents a module configuration
 */
public class Configuration extends DefaultExtendableItem {
    public static class Visibility {
        public static Visibility PUBLIC = new Visibility("public");
        public static Visibility PRIVATE = new Visibility("private");
        public static Visibility getVisibility(String name) {
            if ("private".equals(name)) {
                return PRIVATE;
            } else if ("public".equals(name)) {
                return PUBLIC;
            } else {
                throw new IllegalArgumentException("unknwon visibility "+name);
            }
        }
        private String _name;
        private Visibility(String name) {
            _name = name;
        }
        public String toString() {
            return _name;
        }
    }
    
    private String _name;
    private String _description;
    private String[] _extends;
    private Visibility _visibility;
    private boolean _transitive = true;
    
    /**
     * @param name
     * @param visibility
     * @param description
     * @param ext
     */
    public Configuration(String name, Visibility visibility,
            String description, String[] ext) {
        this(name, visibility, description, ext, true);
    }
    
    /**
     * @param name
     * @param visibility
     * @param description
     * @param ext
     * @param transitive
     */
    public Configuration(String name, Visibility visibility,
            String description, String[] ext, boolean transitive) {
        if (name == null) {
            throw new NullPointerException("null configuration name not allowed");
        }
        if (visibility == null) {
            throw new NullPointerException("null visibility not allowed");
        }
        _name = name;
        _visibility = visibility;
        _description = description;
        if (ext == null) {
            _extends = new String[0];
        } else {
            _extends = new String[ext.length];
            for (int i = 0; i < ext.length; i++) {
                _extends[i] = ext[i].trim();
            }
        }
        _transitive=transitive;
    }
    
    /**
     * @param name
     */
    public Configuration(String name) {
        this(name, Visibility.PUBLIC, null, null);
    }
    
    /**
     * @return Returns the description. It may be null.
     */
    public String getDescription() {
        return _description;
    }
    /**
     * @return Returns the extends. May be empty, but never null.
     */
    public String[] getExtends() {
        return _extends;
    }
    /**
     * @return Returns the name. Never null;
     */
    public String getName() {
        return _name;
    }
    /**
     * @return Returns the visibility. Never null.
     */
    public Visibility getVisibility() {
        return _visibility;
    }
    
    /**
     * @return Returns the transitive.
     */
    public final boolean isTransitive() {
        return _transitive;
    }
   
    public String toString() {
        return _name;
    }
    
    public boolean equals(Object obj) {
        if (! (obj instanceof Configuration)) {
            return false;
        }
        return ((Configuration)obj).getName().equals(getName());
    }
    
    public int hashCode() {
        return getName().hashCode();
    }
    
    public void replaceWildcards(ModuleDescriptor md) {
        if (this != md.getConfiguration(_name)) {
            throw new IllegalArgumentException(
            "The given ModuleDescriptor doesn't own this configuration!");
        }
        
        Configuration[] configs = md.getConfigurations();
        
        Set newExtends = new LinkedHashSet();
        for (int j = 0; j < _extends.length; j++) {
            if ("*".equals(_extends[j])) {
                addOther(configs, null, newExtends);
            } else if ("*(public)".equals(_extends[j])) {
                addOther(configs, Visibility.PUBLIC, newExtends);
            } else if ("*(private)".equals(_extends[j])) {
                addOther(configs, Visibility.PRIVATE, newExtends);
            } else {
                newExtends.add(_extends[j]);
            }
        }
        
        this._extends = (String[]) newExtends.toArray(new String[newExtends.size()]);
    }
    
    private void addOther(Configuration[] allConfigs, Visibility visibility, Set configs) {
        for (int i = 0; i < allConfigs.length; i++) {
            String currentName = allConfigs[i].getName();
            if (!_name.equals(currentName)
                    && ((visibility == null) || visibility.equals(allConfigs[i].getVisibility()))) {
                configs.add(currentName);
            }
        }
    }
    
}
