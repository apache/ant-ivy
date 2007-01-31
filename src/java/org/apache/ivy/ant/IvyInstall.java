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

import java.io.File;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.ivy.util.filter.FilterHelper;
import org.apache.tools.ant.BuildException;


/**
 * Allow to install a module or a set of module from repository to another one.
 * 
 * 
 * @author Xavier Hanin
 *
 */
public class IvyInstall extends IvyTask {
    private String  _organisation;
    private String  _module;
    private String  _revision;
    private File 	_cache; 
    private boolean _overwrite = false;
    private String _from;
    private String _to;
    private boolean _transitive;
    private String _type;
    private String _matcher = PatternMatcher.EXACT;
    
    public void execute() throws BuildException {
        Ivy ivy = getIvyInstance();
        IvySettings settings = ivy.getSettings();
        if (_cache == null) {
            _cache = settings.getDefaultCache();
        }
        if (_organisation == null) {
            throw new BuildException("no organisation provided for ivy publish task: It can either be set explicitely via the attribute 'organisation' or via 'ivy.organisation' property or a prior call to <resolve/>");
        }
        if (_module == null && PatternMatcher.EXACT.equals(_matcher)) {
            throw new BuildException("no module name provided for ivy publish task: It can either be set explicitely via the attribute 'module' or via 'ivy.module' property or a prior call to <resolve/>");
        } else if (_module == null && !PatternMatcher.EXACT.equals(_matcher)) {
        	_module = PatternMatcher.ANY_EXPRESSION;
        }
        if (_revision == null && PatternMatcher.EXACT.equals(_matcher)) {
            throw new BuildException("no module revision provided for ivy publish task: It can either be set explicitely via the attribute 'revision' or via 'ivy.revision' property or a prior call to <resolve/>");
        } else if (_revision == null && !PatternMatcher.EXACT.equals(_matcher)) {
        	_revision = PatternMatcher.ANY_EXPRESSION;
        }
        if (_from == null) {
            throw new BuildException("no from resolver name: please provide it through parameter 'from'");
        }
        if (_to == null) {
            throw new BuildException("no to resolver name: please provide it through parameter 'to'");
        }
        ModuleRevisionId mrid = ModuleRevisionId.newInstance(_organisation, _module, _revision);
        try {
            ivy.install(mrid, _from, _to, _transitive, doValidate(settings), _overwrite, FilterHelper.getArtifactTypeFilter(_type), _cache, _matcher);
        } catch (Exception e) {
            throw new BuildException("impossible to install "+ mrid +": "+e, e);
        }
    }

    public File getCache() {
        return _cache;
    }
    public void setCache(File cache) {
        _cache = cache;
    }
    public String getModule() {
        return _module;
    }
    public void setModule(String module) {
        _module = module;
    }
    public String getOrganisation() {
        return _organisation;
    }
    public void setOrganisation(String organisation) {
        _organisation = organisation;
    }
    public String getRevision() {
        return _revision;
    }
    public void setRevision(String revision) {
        _revision = revision;
    }
    
    public boolean isOverwrite() {
        return _overwrite;
    }
    public void setOverwrite(boolean overwrite) {
        _overwrite = overwrite;
    }
    public String getFrom() {
        return _from;
    }
    public void setFrom(String from) {
        _from = from;
    }
    public String getTo() {
        return _to;
    }
    public void setTo(String to) {
        _to = to;
    }
    public boolean isTransitive() {
        return _transitive;
    }
    public void setTransitive(boolean transitive) {
        _transitive = transitive;
    }
    public String getType() {
        return _type;
    }
    public void setType(String type) {
        _type = type;
    }

    public String getMatcher() {
        return _matcher;
    }

    public void setMatcher(String matcher) {
        _matcher = matcher;
    }
}
