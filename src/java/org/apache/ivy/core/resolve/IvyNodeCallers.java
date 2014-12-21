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
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;

public class IvyNodeCallers {
    public static class Caller {
        private ModuleDescriptor md;

        private ModuleRevisionId mrid;

        // callerConf -> dependencyConfs
        private Map<String, String[]> confs = new HashMap<String, String[]>();

        private DependencyDescriptor dd;

        private boolean callerCanExclude;

        private boolean real = true;

        public Caller(ModuleDescriptor md, ModuleRevisionId mrid, DependencyDescriptor dd,
                boolean callerCanExclude) {
            this.md = md;
            this.mrid = mrid;
            this.dd = dd;
            this.callerCanExclude = callerCanExclude;
        }

        public void addConfiguration(String callerConf, String[] dependencyConfs) {
            updateConfs(callerConf, dependencyConfs);
            Configuration conf = md.getConfiguration(callerConf);
            if (conf != null) {
                String[] confExtends = conf.getExtends();
                if (confExtends != null) {
                    for (int i = 0; i < confExtends.length; i++) {
                        addConfiguration(confExtends[i], dependencyConfs);
                    }
                }
            }
        }

        private void updateConfs(String callerConf, String[] dependencyConfs) {
            String[] prevDepConfs = confs.get(callerConf);
            if (prevDepConfs != null) {
                Set<String> newDepConfs = new HashSet<String>(Arrays.asList(prevDepConfs));
                newDepConfs.addAll(Arrays.asList(dependencyConfs));
                confs.put(callerConf, newDepConfs.toArray(new String[newDepConfs.size()]));
            } else {
                confs.put(callerConf, dependencyConfs);
            }
        }

        public String[] getCallerConfigurations() {
            return confs.keySet().toArray(new String[confs.keySet().size()]);
        }

        public ModuleRevisionId getModuleRevisionId() {
            return mrid;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Caller)) {
                return false;
            }
            Caller other = (Caller) obj;
            return other.confs.equals(confs) && mrid.equals(other.mrid);
        }

        @Override
        public int hashCode() {
            // CheckStyle:MagicNumber| OFF
            int hash = 31;
            hash = hash * 13 + confs.hashCode();
            hash = hash * 13 + mrid.hashCode();
            // CheckStyle:MagicNumber| ON
            return hash;
        }

        @Override
        public String toString() {
            return mrid.toString();
        }

        public ModuleRevisionId getAskedDependencyId(ResolveData resolveData) {
            return dd.getDependencyRevisionId();
        }

        public ModuleDescriptor getModuleDescriptor() {
            return md;
        }

        public boolean canExclude() {
            return callerCanExclude || md.canExclude() || dd.canExclude();
        }

        public DependencyDescriptor getDependencyDescriptor() {
            return dd;
        }

        public void setRealCaller(boolean b) {
            this.real = b;
        }

        public boolean isRealCaller() {
            return real;
        }
    }

    // key in second map is used to easily get a caller by its mrid
    private Map<String, Map<ModuleRevisionId, Caller>> callersByRootConf = new HashMap<String, Map<ModuleRevisionId, Caller>>();

    // this map contains all the module ids calling this one (including transitively) as keys.
    // the mapped nodes (values) correspond to a direct caller from which the transitive caller
    // comes
    private Map<ModuleId, IvyNode> allCallers = new HashMap<ModuleId, IvyNode>();

    private IvyNode node;

    public IvyNodeCallers(IvyNode node) {
        this.node = node;
    }

    /**
     * @param rootModuleConf
     * @param mrid
     * @param callerConf
     * @param dependencyConfs
     *            '*' must have been resolved
     * @param dd
     *            the dependency revision id asked by the caller
     */
    public void addCaller(String rootModuleConf, IvyNode callerNode, String callerConf,
            String requestedConf, String[] dependencyConfs, DependencyDescriptor dd) {
        ModuleDescriptor md = callerNode.getDescriptor();
        ModuleRevisionId mrid = callerNode.getResolvedId();
        if (mrid.getModuleId().equals(node.getId().getModuleId())) {
            throw new IllegalArgumentException("a module is not authorized to depend on itself: "
                    + node.getId());
        }
        Map<ModuleRevisionId, Caller> callers = callersByRootConf.get(rootModuleConf);
        if (callers == null) {
            callers = new HashMap<ModuleRevisionId, Caller>();
            callersByRootConf.put(rootModuleConf, callers);
        }
        Caller caller = callers.get(mrid);
        if (caller == null) {
            caller = new Caller(md, mrid, dd, callerNode.canExclude(rootModuleConf));
            callers.put(mrid, caller);
        }
        caller.addConfiguration(requestedConf, dependencyConfs);

        IvyNode parent = callerNode.getRealNode();
        for (ModuleId mid : parent.getAllCallersModuleIds()) {
            allCallers.put(mid, parent);
        }
        allCallers.put(mrid.getModuleId(), callerNode);
    }

    void removeCaller(String rootModuleConf, ModuleRevisionId callerMrid) {
        allCallers.remove(callerMrid.getModuleId());
        Map<ModuleRevisionId, Caller> callers = callersByRootConf.get(rootModuleConf);
        if (callers != null) {
            callers.remove(callerMrid);
        }
    }

    public Caller[] getCallers(String rootModuleConf) {
        Map<ModuleRevisionId, Caller> callers = callersByRootConf.get(rootModuleConf);
        if (callers == null) {
            return new Caller[0];
        }
        return callers.values().toArray(new Caller[callers.values().size()]);
    }

    public Caller[] getAllCallers() {
        Set<Caller> all = new HashSet<Caller>();
        for (Map<ModuleRevisionId, Caller> callers : callersByRootConf.values()) {
            all.addAll(callers.values());
        }
        return all.toArray(new Caller[all.size()]);
    }

    public Caller[] getAllRealCallers() {
        Set<Caller> all = new HashSet<Caller>();
        for (Map<ModuleRevisionId, Caller> callers : callersByRootConf.values()) {
            for (Caller c : callers.values()) {
                if (c.isRealCaller()) {
                    all.add(c);
                }
            }
        }
        return all.toArray(new Caller[all.size()]);
    }

    public Collection<ModuleId> getAllCallersModuleIds() {
        return allCallers.keySet();
    }

    void updateFrom(IvyNodeCallers callers, String rootModuleConf, boolean real) {
        Map<ModuleRevisionId, Caller> nodecallers = callers.callersByRootConf.get(rootModuleConf);
        if (nodecallers != null) {
            Map<ModuleRevisionId, Caller> thiscallers = callersByRootConf.get(rootModuleConf);
            if (thiscallers == null) {
                thiscallers = new HashMap<ModuleRevisionId, Caller>();
                callersByRootConf.put(rootModuleConf, thiscallers);
            }
            for (Caller caller : nodecallers.values()) {
                if (!thiscallers.containsKey(caller.getModuleRevisionId())) {
                    if (!real) {
                        caller.setRealCaller(false);
                    }
                    thiscallers.put(caller.getModuleRevisionId(), caller);
                }
            }
        }
    }

    public IvyNode getDirectCallerFor(ModuleId from) {
        return allCallers.get(from);
    }

    /**
     * Returns true if ALL callers exclude the given artifact in the given root module conf
     * 
     * @param rootModuleConf
     * @param artifact
     * @return
     */
    boolean doesCallersExclude(String rootModuleConf, Artifact artifact) {
        return doesCallersExclude(rootModuleConf, artifact, new Stack<ModuleRevisionId>());
    }

    boolean doesCallersExclude(String rootModuleConf, Artifact artifact,
            Stack<ModuleRevisionId> callersStack) {
        callersStack.push(node.getId());
        try {
            Caller[] callers = getCallers(rootModuleConf);
            if (callers.length == 0) {
                return false;
            }
            boolean allUnconclusive = true;
            for (int i = 0; i < callers.length; i++) {
                if (!callers[i].canExclude()) {
                    return false;
                }
                ModuleDescriptor md = callers[i].getModuleDescriptor();
                Boolean doesExclude = node.doesExclude(md, rootModuleConf,
                    callers[i].getCallerConfigurations(), callers[i].getDependencyDescriptor(),
                    artifact, callersStack);
                if (doesExclude != null) {
                    if (!doesExclude.booleanValue()) {
                        return false;
                    }
                    allUnconclusive = false;
                }
            }
            return allUnconclusive ? false : true;
        } finally {
            callersStack.pop();
        }
    }

}
