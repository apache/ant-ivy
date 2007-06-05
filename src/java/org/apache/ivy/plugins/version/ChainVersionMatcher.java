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
import org.apache.ivy.util.Checks;

/**
 * An implementation of {@link VersionMatcher} chaining several version matchers, and implemeting
 * the {@link VersionMatcher} interface by returning results from the first matcher in the chain
 * accepting the version.
 */
public class ChainVersionMatcher extends AbstractVersionMatcher {
    /**
     * The list of version matchers in the chain. This list will be queried in order, so the last
     * matcher will be used only if no other matcher accept the revision before.
     */
    private List/* <VersionMatcher> */matchers = new LinkedList();

    /**
     * Unique Constructor.
     */
    public ChainVersionMatcher() {
        super("chain");
    }

    /**
     * Adds a {@link VersionMatcher} to the chain.
     * 
     * @param matcher
     *            the version matcher to add. Must not be null
     */
    public void add(VersionMatcher matcher) {
        Checks.checkNotNull(matcher, "matcher");
        matchers.add(0, matcher);
        if (getSettings() != null && matcher instanceof IvySettingsAware) {
            ((IvySettingsAware) matcher).setSettings(getSettings());
        }
    }

    /**
     * Sets the settings this matcher will use, and set to the matcher in the chain which implements
     * {@link IvySettingsAware}.
     * 
     * @param settings
     *            the settings to use in the whole chain. Must not be null.
     */
    public void setSettings(IvySettings settings) {
        super.setSettings(settings);
        for (Iterator iter = matchers.iterator(); iter.hasNext();) {
            VersionMatcher matcher = (VersionMatcher) iter.next();
            if (matcher instanceof IvySettingsAware) {
                ((IvySettingsAware) matcher).setSettings(settings);
            }
        }
    }

    /**
     * Returns the list of matchers in the chain.
     * <p>
     * The list is returned as an unmodifiable view on the actual list of matchers, and will thus
     * reflect futher changes made in the chain.
     * 
     * @return the list of matchers in the chain. Is never null.
     */
    public List getMatchers() {
        return Collections.unmodifiableList(matchers);
    }

    public boolean isDynamic(ModuleRevisionId askedMrid) {
        Checks.checkNotNull(askedMrid, "askedMrid");
        for (Iterator iter = matchers.iterator(); iter.hasNext();) {
            VersionMatcher matcher = (VersionMatcher) iter.next();
            if (matcher.isDynamic(askedMrid)) {
                return true;
            }
        }
        return false;
    }

    public int compare(ModuleRevisionId askedMrid, ModuleRevisionId foundMrid,
            Comparator staticComparator) {
        Checks.checkNotNull(askedMrid, "askedMrid");
        Checks.checkNotNull(foundMrid, "foundMrid");
        Checks.checkNotNull(staticComparator, "staticComparator");
        for (Iterator iter = matchers.iterator(); iter.hasNext();) {
            VersionMatcher matcher = (VersionMatcher) iter.next();
            if (matcher.isDynamic(askedMrid)) {
                return matcher.compare(askedMrid, foundMrid, staticComparator);
            }
        }
        throw new IllegalArgumentException(
                "impossible to compare revisions: askedMrid is not dynamic: " + askedMrid);
    }

    public boolean accept(ModuleRevisionId askedMrid, ModuleRevisionId foundMrid) {
        Checks.checkNotNull(askedMrid, "askedMrid");
        Checks.checkNotNull(foundMrid, "foundMrid");
        for (Iterator iter = matchers.iterator(); iter.hasNext();) {
            VersionMatcher matcher = (VersionMatcher) iter.next();
            if (!iter.hasNext() || matcher.isDynamic(askedMrid)) {
                return matcher.accept(askedMrid, foundMrid);
            }
        }
        return false;
    }

    public boolean needModuleDescriptor(ModuleRevisionId askedMrid, ModuleRevisionId foundMrid) {
        Checks.checkNotNull(askedMrid, "askedMrid");
        Checks.checkNotNull(foundMrid, "foundMrid");
        for (Iterator iter = matchers.iterator(); iter.hasNext();) {
            VersionMatcher matcher = (VersionMatcher) iter.next();
            if (!iter.hasNext() || matcher.isDynamic(askedMrid)) {
                return matcher.needModuleDescriptor(askedMrid, foundMrid);
            }
        }
        return false;
    }

    public boolean accept(ModuleRevisionId askedMrid, ModuleDescriptor foundMD) {
        Checks.checkNotNull(askedMrid, "askedMrid");
        Checks.checkNotNull(foundMD, "foundMD");
        for (Iterator iter = matchers.iterator(); iter.hasNext();) {
            VersionMatcher matcher = (VersionMatcher) iter.next();
            if (!iter.hasNext() || matcher.isDynamic(askedMrid)) {
                return matcher.accept(askedMrid, foundMD);
            }
        }
        return false;
    }
}
