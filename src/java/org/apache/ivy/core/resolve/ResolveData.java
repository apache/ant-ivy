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
package org.apache.ivy.core.resolve;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.ivy.core.cache.CacheManager;
import org.apache.ivy.core.event.EventManager;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ConfigurationResolveReport;
import org.apache.ivy.core.settings.IvySettings;


public class ResolveData {
    private Map _nodes; // shared map of all nodes: Map (ModuleRevisionId -> IvyNode)
    private Date _date;
    private boolean _validate;
    private boolean _transitive;
    private ConfigurationResolveReport _report;
	private CacheManager _cacheManager;
	private ResolveEngine _engine;

    public ResolveData(ResolveData data, boolean validate) {
        this(data._engine, data._cacheManager, data._date, data._report, validate, data._transitive, data._nodes);
    }

    public ResolveData(ResolveEngine engine, File cache, Date date, ConfigurationResolveReport report, boolean validate) {
        this(engine, cache, date, report, validate, new HashMap());
    }

    public ResolveData(ResolveEngine engine, File cache, Date date, ConfigurationResolveReport report, boolean validate, Map nodes) {
    	this(engine, cache, date, report, validate, true, nodes);
    }
    public ResolveData(ResolveEngine engine, File cache, Date date, ConfigurationResolveReport report, boolean validate, boolean transitive, Map nodes) {
    	this(engine, new CacheManager(engine.getSettings(), cache), date, report, validate, transitive, nodes);
    }
    public ResolveData(ResolveEngine engine, CacheManager cacheManager, Date date, ConfigurationResolveReport report, boolean validate, boolean transitive, Map nodes) {
    	_engine = engine;
        _date = date;
        _report = report;
        _validate = validate;
        _transitive = transitive;
        _nodes = nodes;
        _cacheManager = cacheManager;
    }

    

    public Date getDate() {
        return _date;
    }
    

    public Map getNodes() {
        return _nodes;
    }
    

    public ConfigurationResolveReport getReport() {
        return _report;
    }
    

    public boolean isValidate() {
        return _validate;
    }

    public IvyNode getNode(ModuleRevisionId mrid) {
        return (IvyNode)_nodes.get(mrid);
    }

    public void register(IvyNode node) {
        _nodes.put(node.getId(), node);
    }

    public void register(ModuleRevisionId id, IvyNode node) {
        _nodes.put(id, node);
    }

    public void setReport(ConfigurationResolveReport report) {
        _report = report;
    }

	public boolean isTransitive() {
		return _transitive;
	}

	public IvySettings getSettings() {
		return _engine.getSettings();
	}

	public CacheManager getCacheManager() {
		return _cacheManager;
	}

	public EventManager getEventManager() {
		return _engine.getEventManager();
	}

	public ResolveEngine getEngine() {
		return _engine;
	}
    

    
}
