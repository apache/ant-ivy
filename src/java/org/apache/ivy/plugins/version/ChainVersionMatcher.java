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
package org.apache.ivy.plugins.version;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.IvySettingsAware;


public class ChainVersionMatcher extends AbstractVersionMatcher {
    private List _matchers = new LinkedList();
    
    public ChainVersionMatcher() {
    	super("chain");
    }
    
    public void add(VersionMatcher matcher) {
        _matchers.add(0, matcher);
		if (getSettings() != null && matcher instanceof IvySettingsAware) {
			((IvySettingsAware) matcher).setSettings(getSettings());
		}
    }
    
    public void setSettings(IvySettings settings) {
    	super.setSettings(settings);
    	for (Iterator iter = _matchers.iterator(); iter.hasNext();) {
			VersionMatcher matcher = (VersionMatcher) iter.next();
			if (matcher instanceof IvySettingsAware) {
				((IvySettingsAware) matcher).setSettings(settings);
			}
		}
    }
    
    public List getMatchers() {
    	return Collections.unmodifiableList(_matchers);
    }
    

    public boolean isDynamic(ModuleRevisionId askedMrid) {
        for (Iterator iter = _matchers.iterator(); iter.hasNext();) {
            VersionMatcher matcher = (VersionMatcher)iter.next();
            if (matcher.isDynamic(askedMrid)) {
                return true;
            }
        }
        return false;
    }

    public int compare(ModuleRevisionId askedMrid, ModuleRevisionId foundMrid, Comparator staticComparator) {
    	for (Iterator iter = _matchers.iterator(); iter.hasNext();) {
    		VersionMatcher matcher = (VersionMatcher)iter.next();
    		if (matcher.isDynamic(askedMrid)) {
    			return matcher.compare(askedMrid, foundMrid, staticComparator);
    		}
    	}
    	throw new IllegalArgumentException("impossible to compare revisions: askedMrid is not dynamic: "+askedMrid);
    }

    public boolean accept(ModuleRevisionId askedMrid, ModuleRevisionId foundMrid) {
        for (Iterator iter = _matchers.iterator(); iter.hasNext();) {
            VersionMatcher matcher = (VersionMatcher)iter.next();
            if (!iter.hasNext() || matcher.isDynamic(askedMrid)) {
                return matcher.accept(askedMrid, foundMrid);
            }
        }
        return false;
    }

    public boolean needModuleDescriptor(ModuleRevisionId askedMrid, ModuleRevisionId foundMrid) {
        for (Iterator iter = _matchers.iterator(); iter.hasNext();) {
            VersionMatcher matcher = (VersionMatcher)iter.next();
            if (!iter.hasNext() || matcher.isDynamic(askedMrid)) {
                return matcher.needModuleDescriptor(askedMrid, foundMrid);
            }
        }
        return false;
    }

    public boolean accept(ModuleRevisionId askedMrid, ModuleDescriptor foundMD) {
        for (Iterator iter = _matchers.iterator(); iter.hasNext();) {
            VersionMatcher matcher = (VersionMatcher)iter.next();
            if (!iter.hasNext() || matcher.isDynamic(askedMrid)) {
                return matcher.accept(askedMrid, foundMD);
            }
        }
        return false;
    }

}
