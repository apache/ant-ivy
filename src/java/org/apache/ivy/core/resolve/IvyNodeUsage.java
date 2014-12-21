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

import org.apache.ivy.core.module.descriptor.DependencyArtifactDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.IncludeRule;
import org.apache.ivy.core.module.descriptor.WorkspaceModuleDescriptor;

/**
 * Class collecting usage data for an IvyNode.
 * <p>
 * Usage data contains the configurations required by callers for each root module configuration,
 * the configurations required by caller node and caller configuration, dependency artifacts
 * descriptors declared by callers, include rules declared by callers, and blacklisted data by root
 * module conf.
 * </p>
 */
public class IvyNodeUsage {

    private static final class NodeConf {
        private IvyNode node;

        private String conf;

        public NodeConf(IvyNode node, String conf) {
            if (node == null) {
                throw new NullPointerException("node must not null");
            }
            if (conf == null) {
                throw new NullPointerException("conf must not null");
            }
            this.node = node;
            this.conf = conf;
        }

        public final String getConf() {
            return conf;
        }

        public final IvyNode getNode() {
            return node;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof NodeConf)) {
                return false;
            }
            return getNode().equals(((NodeConf) obj).getNode())
                    && getConf().equals(((NodeConf) obj).getConf());
        }

        @Override
        public int hashCode() {
            // CheckStyle:MagicNumber| OFF
            int hash = 33;
            hash += getNode().hashCode() * 17;
            hash += getConf().hashCode() * 17;
            // CheckStyle:MagicNumber| OFF
            return hash;
        }

        @Override
        public String toString() {
            return "NodeConf(" + conf + ")";
        }
    }

    private static final class Depender {
        private DependencyDescriptor dd;

        private String dependerConf;

        public Depender(DependencyDescriptor dd, String dependerConf) {
            this.dd = dd;
            this.dependerConf = dependerConf;
        }

        @Override
        public String toString() {
            return dd + " [" + dependerConf + "]";
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Depender)) {
                return false;
            }
            Depender other = (Depender) obj;
            return other.dd == dd && other.dependerConf.equals(dependerConf);
        }

        @Override
        public int hashCode() {
            int hash = 33;
            hash += dd.hashCode() * 13;
            hash += dependerConf.hashCode() * 13;
            return hash;
        }
    }

    private IvyNode node;

    // used to know which configurations of the dependency are required
    // for each root module configuration
    // rootConfName -> confNames
    private Map<String, Set<String>> rootModuleConfs = new HashMap<String, Set<String>>();

    private Map<NodeConf, Set<String>> requiredConfs = new HashMap<NodeConf, Set<String>>();

    private Map<String, Set<Depender>> dependers = new HashMap<String, Set<Depender>>();

    // rootModuleConf -> black list
    private Map<String, IvyNodeBlacklist> blacklisted = new HashMap<String, IvyNodeBlacklist>();

    public IvyNodeUsage(IvyNode node) {
        this.node = node;
    }

    protected Collection<String> getRequiredConfigurations(IvyNode in, String inConf) {
        return requiredConfs.get(new NodeConf(in, inConf));
    }

    protected void setRequiredConfs(IvyNode parent, String parentConf, Collection<String> confs) {
        requiredConfs.put(new NodeConf(parent, parentConf), new HashSet<String>(confs));
    }

    /**
     * Returns the configurations of the dependency required in a given root module configuration.
     * 
     * @param rootModuleConf
     * @return
     */
    protected Set<String> getConfigurations(String rootModuleConf) {
        return rootModuleConfs.get(rootModuleConf);
    }

    protected Set<String> addAndGetConfigurations(String rootModuleConf) {
        Set<String> depConfs = rootModuleConfs.get(rootModuleConf);
        if (depConfs == null) {
            depConfs = new HashSet<String>();
            rootModuleConfs.put(rootModuleConf, depConfs);
        }
        return depConfs;
    }

    protected Set<String> getRootModuleConfigurations() {
        return rootModuleConfs.keySet();
    }

    public void updateDataFrom(Collection<IvyNodeUsage> usages, String rootModuleConf) {
        for (IvyNodeUsage usage : usages) {
            updateDataFrom(usage, rootModuleConf);
        }
    }

    private void updateDataFrom(IvyNodeUsage usage, String rootModuleConf) {
        // update requiredConfs
        updateMapOfSet(usage.requiredConfs, requiredConfs);

        // update rootModuleConfs
        updateMapOfSetForKey(usage.rootModuleConfs, rootModuleConfs, rootModuleConf);

        // update dependencyArtifacts
        updateMapOfSetForKey(usage.dependers, dependers, rootModuleConf);
    }

    private <K, V> void updateMapOfSet(Map<K, Set<V>> from, Map<K, Set<V>> to) {
        for (K key : from.keySet()) {
            updateMapOfSetForKey(from, to, key);
        }
    }

    private <K, V> void updateMapOfSetForKey(Map<K, Set<V>> from, Map<K, Set<V>> to, K key) {
        Set<V> set = from.get(key);
        if (set != null) {
            Set<V> toupdate = to.get(key);
            if (toupdate != null) {
                toupdate.addAll(set);
            } else {
                to.put(key, new HashSet<V>(set));
            }
        }
    }

    private <K, V> void addObjectsForConf(K rootModuleConf, V objectToAdd, Map<K, Set<V>> map) {
        Set<V> set = map.get(rootModuleConf);
        if (set == null) {
            set = new HashSet<V>();
            map.put(rootModuleConf, set);
        }
        set.add(objectToAdd);
    }

    public void addUsage(String rootModuleConf, DependencyDescriptor dd, String parentConf) {
        addObjectsForConf(rootModuleConf, new Depender(dd, parentConf), dependers);
    }

    protected Set<DependencyArtifactDescriptor> getDependencyArtifactsSet(String rootModuleConf) {
        if (node.getDescriptor() instanceof WorkspaceModuleDescriptor) {
            // for a module in the "workspace" artifacts will be actually declared by the resolver
            return null;
        }
        Collection<Depender> dependersInConf = dependers.get(rootModuleConf);
        if (dependersInConf == null) {
            return null;
        }
        Set<DependencyArtifactDescriptor> dependencyArtifacts = new HashSet<DependencyArtifactDescriptor>();
        for (Depender depender : dependersInConf) {
            DependencyArtifactDescriptor[] dads = depender.dd
                    .getDependencyArtifacts(depender.dependerConf);
            dependencyArtifacts.addAll(Arrays.asList(dads));
        }
        return dependencyArtifacts;
    }

    protected Set<IncludeRule> getDependencyIncludesSet(String rootModuleConf) {
        Collection<Depender> dependersInConf = dependers.get(rootModuleConf);
        if (dependersInConf == null) {
            return null;
        }
        Set<IncludeRule> dependencyIncludes = new HashSet<IncludeRule>();
        for (Depender depender : dependersInConf) {
            IncludeRule[] rules = depender.dd.getIncludeRules(depender.dependerConf);
            if (rules == null || rules.length == 0) {
                // no include rule in at least one depender -> we must include everything,
                // and so return no include rule at all
                return null;
            }
            dependencyIncludes.addAll(Arrays.asList(rules));
        }
        return dependencyIncludes;
    }

    protected void removeRootModuleConf(String rootModuleConf) {
        rootModuleConfs.remove(rootModuleConf);
    }

    protected void blacklist(IvyNodeBlacklist bdata) {
        blacklisted.put(bdata.getRootModuleConf(), bdata);
    }

    /**
     * Indicates if this node has been blacklisted in the given root module conf.
     * <p>
     * A blacklisted node should be considered as if it doesn't even exist on the repository.
     * </p>
     * 
     * @param rootModuleConf
     *            the root module conf for which we'd like to know if the node is blacklisted
     * 
     * @return true if this node is blacklisted int he given root module conf, false otherwise
     * @see #blacklist(String)
     */
    protected boolean isBlacklisted(String rootModuleConf) {
        return blacklisted.containsKey(rootModuleConf);
    }

    /**
     * Returns the blacklist data of this node in the given root module conf, or <code>null</code>
     * if this node is not blacklisted in this root module conf.
     * 
     * @param rootModuleConf
     *            the root module configuration to consider
     * @return the blacklist data if any
     */
    protected IvyNodeBlacklist getBlacklistData(String rootModuleConf) {
        return blacklisted.get(rootModuleConf);
    }

    protected IvyNode getNode() {
        return node;
    }

    /**
     * Indicates if at least one depender has a transitive dependency descriptor for the given root
     * module conf.
     * 
     * @param rootModuleConf
     *            the root module conf to consider
     * @return <code>true</code> if at least one depender has a transitive dependency descriptor for
     *         the given root module conf, <code>false</code> otherwise.
     */
    public boolean hasTransitiveDepender(String rootModuleConf) {
        Set<Depender> dependersSet = dependers.get(rootModuleConf);
        if (dependersSet == null) {
            return false;
        }
        for (Depender depender : dependersSet) {
            if (depender.dd.isTransitive()) {
                return true;
            }
        }
        return false;
    }

}
