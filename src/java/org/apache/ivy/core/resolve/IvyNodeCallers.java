/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivy.core.resolve;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;

public class IvyNodeCallers {
    public static class Caller {
        private ModuleDescriptor md;

        private ModuleRevisionId mrid;

        // callerConf -> dependencyConfs
        private Map<String, String[]> confs = new HashMap<>();

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
                    for (String confExtend : confExtends) {
                        addConfiguration(confExtend, dependencyConfs);
                    }
                }
            }
        }

        private void updateConfs(String callerConf, String[] dependencyConfs) {
            String[] prevDepConfs = confs.get(callerConf);
            if (prevDepConfs != null) {
                Set<String> newDepConfs = new HashSet<>(Arrays.asList(prevDepConfs));
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

        @Deprecated
        public ModuleRevisionId getAskedDependencyId(ResolveData resolveData) {
            return getAskedDependencyId();
        }

        public ModuleRevisionId getAskedDependencyId() {
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
    private Map<String, Map<ModuleRevisionId, Caller>> callersByRootConf = new HashMap<>();

    // this map contains all the module ids calling this one (including transitively) as keys.
    // the mapped nodes (values) correspond to a direct caller from which the transitive caller
    // comes
    private Map<ModuleId, IvyNode> allCallers = new HashMap<>();

    private IvyNode node;

    public IvyNodeCallers(IvyNode node) {
        this.node = node;
    }

    /**
     * @param rootModuleConf ditto
     * @param callerNode IvyNode
     * @param callerConf ditto
     * @param requestedConf ditto
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
            callers = new HashMap<>();
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

    private Set<Caller> getCallersByMrid(String rootModuleConf, ModuleRevisionId mrid) {
        Map<ModuleRevisionId, Caller> callers = callersByRootConf.get(rootModuleConf);
        if (callers == null) {
            return Collections.emptySet();
        }

        Set<Caller> mridCallers = new HashSet<>();
        for (Caller caller : callers.values()) {
            if (caller.getAskedDependencyId().equals(mrid)) {
                mridCallers.add(caller);
            }
        }
        return mridCallers;
    }

    public Caller[] getAllCallers() {
        Set<Caller> all = new HashSet<>();
        for (Map<ModuleRevisionId, Caller> callers : callersByRootConf.values()) {
            all.addAll(callers.values());
        }
        return all.toArray(new Caller[all.size()]);
    }

    public Caller[] getAllRealCallers() {
        Set<Caller> all = new HashSet<>();
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
                thiscallers = new HashMap<>();
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
     * @param rootModuleConf ditto
     * @param artifact Artifact
     * @return boolean
     */
    boolean doesCallersExclude(String rootModuleConf, Artifact artifact) {
        return doesCallersExclude(rootModuleConf, artifact, new ArrayDeque<IvyNode>());
    }

    boolean doesCallersExclude(String rootModuleConf, Artifact artifact,
            Deque<IvyNode> callersStack) {
        /* The caller stack is, from bottom to top, the path from the
           artifact we're considering excluding up towards the
           root. */
        callersStack.push(node);
        try {
            Set<Caller> callers = getCallersByMrid(rootModuleConf, node.getId());
            if (callers.isEmpty()) {
                return false;
            }
            boolean allInconclusive = true;
            String[] moduleConfs = new String[] {rootModuleConf};
            callers: for (Caller caller : callers) {
                /* Each ancestor of this artifact (called "descendant", here, since it's
                   a descendant relative to this.node) might itself have been excluded by
                   an older ancestor (this.node); if it is, then it is as if artifact
                   itself were excluded in this path. */
                for (IvyNode descendant : callersStack) {
                    if (node.directlyExcludes(node.getDescriptor(), moduleConfs,
                            caller.getDependencyDescriptor(),
                            DefaultArtifact.newIvyArtifact(descendant.getId(), null))) {
                        allInconclusive = false;
                        continue callers;
                    }
                }
                if (!caller.canExclude()) {
                    return false;
                }
                Boolean doesExclude = node.doesExclude(caller.getModuleDescriptor(), rootModuleConf,
                        caller.getCallerConfigurations(), caller.getDependencyDescriptor(),
                        artifact, callersStack);
                if (doesExclude != null) {
                    if (!doesExclude) {
                        return false;
                    }
                    allInconclusive = false;
                }
            }
            return !allInconclusive;
        } finally {
            callersStack.pop();
        }
    }

}
