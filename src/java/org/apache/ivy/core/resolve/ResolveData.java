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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.ivy.core.cache.CacheManager;
import org.apache.ivy.core.event.EventManager;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ConfigurationResolveReport;
import org.apache.ivy.core.settings.IvySettings;


public class ResolveData {
    private Map _visitData; // shared map of all visit data: Map (ModuleRevisionId -> VisitData)
    private Date _date;
    private boolean _validate;
    private boolean _transitive;
    private ConfigurationResolveReport _report;
	private CacheManager _cacheManager;
	private ResolveEngine _engine;

    public ResolveData(ResolveData data, boolean validate) {
        this(data._engine, data._cacheManager, data._date, data._report, validate, data._transitive, data._visitData);
    }

    public ResolveData(ResolveEngine engine, File cache, Date date, ConfigurationResolveReport report, boolean validate) {
        this(engine, cache, date, report, validate, true, new LinkedHashMap());
    }

    public ResolveData(ResolveEngine engine, File cache, Date date, ConfigurationResolveReport report, boolean validate, Map nodes) {
    	this(engine, cache, date, report, validate, true, nodes);
    }
    public ResolveData(ResolveEngine engine, File cache, Date date, ConfigurationResolveReport report, boolean validate, boolean transitive, Map nodes) {
    	this(engine, new CacheManager(engine.getSettings(), cache), date, report, validate, transitive, nodes);
    }

    public ResolveData(ResolveEngine engine, File cache, Date date, ConfigurationResolveReport report, boolean validate, boolean transitive) {
		this(engine, cache, date, report, validate, transitive, new LinkedHashMap());
	}
    public ResolveData(ResolveEngine engine, CacheManager cacheManager, Date date, ConfigurationResolveReport report, boolean validate, boolean transitive, Map visitData) {
    	_engine = engine;
        _date = date;
        _report = report;
        _validate = validate;
        _transitive = transitive;
        _visitData = visitData;
        _cacheManager = cacheManager;
    }

    

	public Date getDate() {
        return _date;
    }
    

    /**
     * Returns the Map of visit data.
     * Map (ModuleRevisionId -> VisitData)
     * @return
     */
    public Map getVisitDataMap() {
        return _visitData;
    }
    

    public ConfigurationResolveReport getReport() {
        return _report;
    }
    

    public boolean isValidate() {
        return _validate;
    }

    public IvyNode getNode(ModuleRevisionId mrid) {
        VisitData visitData = getVisitData(mrid);
		return visitData == null ? null : visitData.getNode();
    }
    
    public Collection getNodes() {
    	Collection nodes = new ArrayList();
    	for (Iterator iter = _visitData.values().iterator(); iter.hasNext();) {
			VisitData vdata = (VisitData) iter.next();
			nodes.add(vdata.getNode());
		}
    	return nodes;
    }
    
    public Collection getNodeIds() {
    	return _visitData.keySet();
    }
    
    public VisitData getVisitData(ModuleRevisionId mrid) {
    	return (VisitData) _visitData.get(mrid);
    }

    public void register(VisitNode node) {
    	register(node.getId(), node);
    }

    public void register(ModuleRevisionId mrid, VisitNode node) {
		VisitData visitData = getVisitData(mrid);
    	if (visitData == null) {
    		visitData = new VisitData(node.getNode());
    		visitData.addVisitNode(node);
    		_visitData.put(mrid, visitData);
    	} else {
    		visitData.setNode(node.getNode());
    		visitData.addVisitNode(node);
    	}
    }

    /**
     * Updates the visit data currently associated with the given mrid
     * with the given node and the visit nodes of the old visitData
     * for the given rootModuleConf
     * @param mrid the module revision id for which the update should be done
     * @param node the IvyNode to associate with the visit data to update
     * @param rootModuleConf the root module configuration in which the update is made
     */
    void replaceNode(ModuleRevisionId mrid, IvyNode node, String rootModuleConf) {
		VisitData visitData = getVisitData(mrid);
    	if (visitData == null) {
    		throw new IllegalArgumentException("impossible to replace node for id "+mrid+". No registered node found.");
    	}
    	VisitData keptVisitData = getVisitData(node.getId());
    	if (keptVisitData == null) {
    		throw new IllegalArgumentException("impossible to replace node with "+node+". No registered node found for "+node.getId()+".");
    	}
    	// replace visit data in Map (discards old one)
    	_visitData.put(mrid, keptVisitData);
    	// update visit data with discarde visit nodes
    	keptVisitData.addVisitNodes(rootModuleConf, visitData.getVisitNodes(rootModuleConf));
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
