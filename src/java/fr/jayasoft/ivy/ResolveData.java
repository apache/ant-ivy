/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import fr.jayasoft.ivy.report.ConfigurationResolveReport;

public class ResolveData {
    private Map _nodes; // shared map of all nodes: Map (ModuleRevisionId -> IvyNode)
    private Ivy _ivy;
    private File _cache;
    private Date _date;
    private boolean _validate;
    private boolean _transitive;
    private ConfigurationResolveReport _report;

    public ResolveData(ResolveData data, boolean validate) {
        this(data._ivy, data._cache, data._date, data._report, validate, data._nodes);
    }

    public ResolveData(Ivy ivy, File cache, Date date, ConfigurationResolveReport report, boolean validate) {
        this(ivy, cache, date, report, validate, new HashMap());
    }

    public ResolveData(Ivy ivy, File cache, Date date, ConfigurationResolveReport report, boolean validate, Map nodes) {
    	this(ivy, cache, date, report, validate, true, nodes);
    }
    public ResolveData(Ivy ivy, File cache, Date date, ConfigurationResolveReport report, boolean validate, boolean transitive, Map nodes) {
        _ivy = ivy;
        _cache = cache;
        _date = date;
        _report = report;
        _validate = validate;
        _transitive = transitive;
        _nodes = nodes;
    }

    public File getCache() {
        return _cache;
    }
    

    public Date getDate() {
        return _date;
    }
    

    public Ivy getIvy() {
        return _ivy;
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
    

    
}
