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
import java.util.Collection;
import java.util.Map;

import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.module.id.ArtifactId;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.ivy.util.extendable.UnmodifiableExtendableItem;

/**
 * Abstract class used as implementation for both {@link IncludeRule} and {@link ExcludeRule},
 * since their contract is almost identical
 * 
 * @author Xavier Hanin
 *
 */
public abstract class AbstractIncludeExcludeRule extends UnmodifiableExtendableItem 
		implements ConfigurationAware {

    private ArtifactId _id;
    private Collection _confs = new ArrayList();
    private PatternMatcher _patternMatcher;
    
    public AbstractIncludeExcludeRule(ArtifactId aid, PatternMatcher matcher, Map extraAttributes) {
    	super(null, extraAttributes);
        _id = aid;
        _patternMatcher = matcher;
        initStandardAttributes();
    }

	private void initStandardAttributes() {
		setStandardAttribute(IvyPatternHelper.ORGANISATION_KEY, _id.getModuleId().getOrganisation());
        setStandardAttribute(IvyPatternHelper.MODULE_KEY, _id.getModuleId().getName());
        setStandardAttribute(IvyPatternHelper.ARTIFACT_KEY, _id.getName());
        setStandardAttribute(IvyPatternHelper.TYPE_KEY, _id.getType());
        setStandardAttribute(IvyPatternHelper.EXT_KEY, _id.getExt());
        setStandardAttribute("matcher", _patternMatcher.getName());
	}
    
	public boolean equals(Object obj) {
        if (!(obj instanceof AbstractIncludeExcludeRule)) {
            return false;
        }
        AbstractIncludeExcludeRule rule = (AbstractIncludeExcludeRule)obj;
        return getId().equals(rule.getId());
    }
    
    public int hashCode() {
        return getId().hashCode();
    }
    
    /**
     * Add a configuration for this rule
     * @param conf
     */
    public void addConfiguration(String conf) {
        _confs.add(conf);
    }
        
    public ArtifactId getId() {
        return _id;
    }

    public String[] getConfigurations() {
        return (String[])_confs.toArray(new String[_confs.size()]);
    }

    public PatternMatcher getMatcher() {
        return _patternMatcher;
    }

	public String toString() {
		return _id+"("+_confs+")";
	}
}
