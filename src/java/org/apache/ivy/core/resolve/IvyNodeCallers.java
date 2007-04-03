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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;

public class IvyNodeCallers {
    public static class Caller {
        private ModuleDescriptor _md;
        private ModuleRevisionId _mrid;
        private Map _confs = new HashMap(); // Map (String callerConf -> String[] dependencyConfs)
        private DependencyDescriptor _dd;
        private boolean _callerCanExclude;
        
        public Caller(ModuleDescriptor md, ModuleRevisionId mrid, DependencyDescriptor dd, boolean callerCanExclude) {
            _md = md;
            _mrid = mrid;
            _dd = dd;
            _callerCanExclude = callerCanExclude;
        }
        public void addConfiguration(String callerConf, String[] dependencyConfs) {
            String[] prevDepConfs = (String[])_confs.get(callerConf);
            if (prevDepConfs != null) {
                Set newDepConfs = new HashSet(Arrays.asList(prevDepConfs));
                newDepConfs.addAll(Arrays.asList(dependencyConfs));
                _confs.put(callerConf, (String[])newDepConfs.toArray(new String[newDepConfs.size()]));
            } else {
                _confs.put(callerConf, dependencyConfs);
            }
        }
        public String[] getCallerConfigurations() {
            return (String[])_confs.keySet().toArray(new String[_confs.keySet().size()]);
        }
        public ModuleRevisionId getModuleRevisionId() {
            return _mrid;
        }
        public boolean equals(Object obj) {
            if (! (obj instanceof Caller)) {
                return false;
            }
            Caller other = (Caller)obj;
            return other._confs.equals(_confs) 
                && _mrid.equals(other._mrid);
        }
        public int hashCode() {
            int hash = 31;
            hash = hash * 13 + _confs.hashCode();
            hash = hash * 13 + _mrid.hashCode();
            return hash;
        }
        public String toString() {
            return _mrid.toString();
        }
        public ModuleRevisionId getAskedDependencyId() {
            return _dd.getDependencyRevisionId();
        }
        public ModuleDescriptor getModuleDescriptor() {
            return _md;
        }
        public boolean canExclude() {
            return _callerCanExclude || _md.canExclude() || _dd.canExclude();
        }
        public DependencyDescriptor getDependencyDescriptor() {
            return _dd;
        }
    }

    // Map (String rootModuleConf -> Map (ModuleRevisionId -> Caller)): key in second map is used to easily get a caller by its mrid
    private Map _callersByRootConf = new HashMap(); 
    
    // this map contains all the module ids calling this one (including transitively) as keys
    // the mapped nodes (values) correspond to a direct caller from which the transitive caller comes
    
    private Map _allCallers = new HashMap(); // Map (ModuleId -> IvyNode)
    
    private IvyNode _node;

    
    public IvyNodeCallers(IvyNode node) {
		_node = node;
	}

	/**
     * 
     * @param rootModuleConf
     * @param mrid
     * @param callerConf
     * @param dependencyConfs '*' must have been resolved
     * @param dd the dependency revision id asked by the caller
     */
    public void addCaller(String rootModuleConf, IvyNode callerNode, String callerConf, String[] dependencyConfs, DependencyDescriptor dd) {
        ModuleDescriptor md = callerNode.getDescriptor();
        ModuleRevisionId mrid = callerNode.getId(); 
        if (mrid.getModuleId().equals(_node.getId().getModuleId())) {
            throw new IllegalArgumentException("a module is not authorized to depend on itself: "+_node.getId());
        }
        Map callers = (Map)_callersByRootConf.get(rootModuleConf);
        if (callers == null) {
            callers = new HashMap();
            _callersByRootConf.put(rootModuleConf, callers);
        }
        Caller caller = (Caller)callers.get(mrid);
        if (caller == null) {
            caller = new Caller(md, mrid, dd, callerNode.canExclude(rootModuleConf));
            callers.put(mrid, caller);
        }
        caller.addConfiguration(callerConf, dependencyConfs);

        IvyNode parent = callerNode.getRealNode();
    	for (Iterator iter = parent.getAllCallersModuleIds().iterator(); iter.hasNext();) {
			ModuleId mid = (ModuleId) iter.next();
			_allCallers.put(mid, parent);
		}
        _allCallers.put(mrid.getModuleId(), callerNode);
    }

    public Caller[] getCallers(String rootModuleConf) {
        Map callers = (Map)_callersByRootConf.get(rootModuleConf);
        if (callers == null) {
            return new Caller[0];
        }
        return (Caller[])callers.values().toArray(new Caller[callers.values().size()]);
    }

    public Caller[] getAllCallers() {
        Set all = new HashSet();
        for (Iterator iter = _callersByRootConf.values().iterator(); iter.hasNext();) {
            Map callers = (Map)iter.next();
            all.addAll(callers.values());
        }
        return (Caller[])all.toArray(new Caller[all.size()]);
    }

	public Collection getAllCallersModuleIds() {
		return _allCallers.keySet();
	}

	public void updateFrom(IvyNodeCallers callers, String rootModuleConf) {
        Map nodecallers = (Map)callers._callersByRootConf.get(rootModuleConf);
        if (nodecallers != null) {
            Map thiscallers = (Map)_callersByRootConf.get(rootModuleConf);
            if (thiscallers == null) {
                thiscallers = new HashMap();
                _callersByRootConf.put(rootModuleConf, thiscallers);
            }
            for (Iterator iter = nodecallers.values().iterator(); iter.hasNext();) {
                Caller caller = (Caller)iter.next();
                if (!thiscallers.containsKey(caller.getModuleRevisionId())) {
                    thiscallers.put(caller.getModuleRevisionId(), caller);
                }
            }
        }
	}

	public IvyNode getDirectCallerFor(ModuleId from) {
		return (IvyNode) _allCallers.get(from);
	}

    /**
     * Returns true if ALL callers exclude the given artifact in the given root module conf
     * @param rootModuleConf
     * @param artifact
     * @return
     */
    boolean doesCallersExclude(String rootModuleConf, Artifact artifact) {
        return doesCallersExclude(rootModuleConf, artifact, new Stack());
    }
    boolean doesCallersExclude(String rootModuleConf, Artifact artifact, Stack callersStack) {
        if (callersStack.contains(_node.getId())) {
            return false;
        }
        callersStack.push(_node.getId());
        try {
            Caller[] callers = getCallers(rootModuleConf);
            if (callers.length == 0) {
                return false;
            }
            for (int i = 0; i < callers.length; i++) {
                if (!callers[i].canExclude()) {
                    return false;
                }
                ModuleDescriptor md = callers[i].getModuleDescriptor();
                if (!doesExclude(md, rootModuleConf, callers[i].getCallerConfigurations(), callers[i].getDependencyDescriptor(), artifact, callersStack)) {
                    return false;
                }
            }
            return true;
        } finally {
            callersStack.pop();
        }
    }


    private boolean doesExclude(ModuleDescriptor md, String rootModuleConf, String[] moduleConfs, DependencyDescriptor dd, Artifact artifact, Stack callersStack) {
        // artifact is excluded if it match any of the exclude pattern for this dependency...
        if (dd != null) {
            if (dd.doesExclude(moduleConfs, artifact.getId().getArtifactId())) {
                return true;
            }
        }
        if (md.doesExclude(moduleConfs, artifact.getId().getArtifactId())) {
            return true;
        }
        // ... or if it is excluded by all its callers
        IvyNode c = _node.getData().getNode(md.getModuleRevisionId());
        if (c != null) {
            return c.doesCallersExclude(rootModuleConf, artifact, callersStack);
        } else {
            return false;
        }
    }

}
