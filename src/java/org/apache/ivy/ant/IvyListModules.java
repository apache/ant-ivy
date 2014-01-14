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
package org.apache.ivy.ant;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.search.SearchEngine;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.tools.ant.BuildException;

/**
 * Look for modules in the repository matching the given criteria, and sets a set of properties
 * according to what was found.
 */
public class IvyListModules extends IvyTask {
    private String organisation;

    private String module;

    private String branch = PatternMatcher.ANY_EXPRESSION;

    private String revision;

    private String matcher = PatternMatcher.EXACT_OR_REGEXP;

    private String property;

    private String value;

    private String resolver;

    public String getMatcher() {
        return matcher;
    }

    public void setMatcher(String matcher) {
        this.matcher = matcher;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String name) {
        this.property = name;
    }

    public String getOrganisation() {
        return organisation;
    }

    public void setOrganisation(String organisation) {
        this.organisation = organisation;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public void setResolver(String resolver) {
        this.resolver = resolver;
    }

    public String getResolver() {
        return resolver;
    }

    public void doExecute() throws BuildException {
        if (organisation == null) {
            throw new BuildException("no organisation provided for ivy listmodules task");
        }
        if (module == null) {
            throw new BuildException("no module name provided for ivy listmodules task");
        }
        if (revision == null) {
            throw new BuildException("no revision provided for ivy listmodules task");
        }
        if (property == null) {
            throw new BuildException("no property provided for ivy listmodules task");
        }
        if (value == null) {
            throw new BuildException("no value provided for ivy listmodules task");
        }

        Ivy ivy = getIvyInstance();
        IvySettings settings = ivy.getSettings();

        SearchEngine searcher = new SearchEngine(settings);
        PatternMatcher patternMatcher = settings.getMatcher(matcher);

        ModuleRevisionId[] mrids;
        if (resolver == null) {
            mrids = searcher.listModules(
                ModuleRevisionId.newInstance(organisation, module, branch, revision),
                patternMatcher);
        } else {
            DependencyResolver depResolver = settings.getResolver(resolver);
            if (depResolver == null) {
                throw new BuildException("Unknown resolver: " + resolver);
            }
            mrids = searcher.listModules(depResolver,
                ModuleRevisionId.newInstance(organisation, module, branch, revision),
                patternMatcher);
        }

        for (int i = 0; i < mrids.length; i++) {
            String name = IvyPatternHelper.substitute(settings.substitute(property), mrids[i]);
            String value = IvyPatternHelper.substitute(settings.substitute(this.value), mrids[i]);
            getProject().setProperty(name, value);
        }
    }
}
