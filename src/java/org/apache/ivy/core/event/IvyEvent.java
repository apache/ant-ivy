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
package org.apache.ivy.core.event;

import java.util.HashMap;
import java.util.Map;

import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.util.StringUtils;

/**
 * The root of all ivy events Any ivy event knows which ivy instance triggered the event (the
 * source) and also has a name and a map of attributes. The name of the event represents the event
 * type, usually there is a one - one mapping between event names and IvyEvent subclass, even if
 * this is not mandatory. Example: pre-resolve pre-resolve-dependency post-download The map of
 * attributes is a Map from String keys to String values. It is especially useful to filter events,
 * and to get some of their essential data in some context where access to Java types is not easy
 * (in an ant build file, for example), Example: pre-resolve (organisation=foo, module=bar,
 * revision=1.0, conf=default) post-download (organisation=foo, module=bar, revision=1.0,
 * artifact=foo-test, type=jar, ext=jar)
 */
public class IvyEvent {
    private EventManager source;

    private String name;

    private Map attributes = new HashMap();

    protected IvyEvent(String name) {
        this.source = IvyContext.getContext().getEventManager();
        this.name = name;
    }

    /**
     * Should only be called during event object construction, since events should be immutable
     * 
     * @param key
     * @param value
     */
    protected void addAttribute(String key, String value) {
        attributes.put(key, value);
    }

    protected void addMDAttributes(ModuleDescriptor md) {
        addMridAttributes(md.getResolvedModuleRevisionId());
    }

    protected void addMridAttributes(ModuleRevisionId mrid) {
        addModuleIdAttributes(mrid.getModuleId());
        addAttribute("revision", mrid.getRevision());
        addAttribute("branch", mrid.getBranch());
        addAttributes(mrid.getQualifiedExtraAttributes());
        addAttributes(mrid.getExtraAttributes());
    }

    protected void addModuleIdAttributes(ModuleId moduleId) {
        addAttribute("organisation", moduleId.getOrganisation());
        addAttribute("module", moduleId.getName());
    }

    protected void addConfsAttribute(String[] confs) {
        addAttribute("conf", StringUtils.join(confs, ", "));
    }

    protected void addAttributes(Map attributes) {
        this.attributes.putAll(attributes);
    }

    public EventManager getSource() {
        return source;
    }

    public String getName() {
        return name;
    }

    /**
     * Returns the attributes of this event, as a Map(String->String)
     * 
     * @return the attributes of this event, as a Map(String->String)
     */
    public Map getAttributes() {
        return new HashMap(attributes);
    }

    public String toString() {
        return getName() + " " + getAttributes();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof IvyEvent)) {
            return false;
        }
        IvyEvent e = (IvyEvent) obj;

        return getSource().equals(e.getSource()) && getName().equals(e.getName())
                && attributes.equals(e.attributes);
    }

    public int hashCode() {
        // CheckStyle:MagicNumber| OFF
        int hash = 37;
        hash = 13 * hash + getSource().hashCode();
        hash = 13 * hash + getName().hashCode();
        hash = 13 * hash + attributes.hashCode();
        // CheckStyle:MagicNumber| ON
        return hash;
    }
}
