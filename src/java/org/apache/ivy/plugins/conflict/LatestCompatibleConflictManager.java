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
package org.apache.ivy.plugins.conflict;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Stack;

import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.core.resolve.IvyNodeBlacklist;
import org.apache.ivy.core.resolve.IvyNodeCallers.Caller;
import org.apache.ivy.core.resolve.IvyNodeEviction.EvictionData;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.RestartResolveProcess;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.latest.LatestStrategy;
import org.apache.ivy.plugins.version.VersionMatcher;
import org.apache.ivy.util.Message;

/**
 * This conflict manager can be used to allow only compatible dependencies to be used together (like
 * the strict conflict manager), but it has the advantage of using a best effort algorithm to find a
 * set of compatible dependencies, even if it requires stepping back to older revisions (as long as
 * they are in the set of compatibility).
 * <p>
 * Here is an example of what this conflict manager is able to do:<br/>
 * <b>Available Modules</b>:
 * 
 * <pre>
 * #A;2-&gt;{ #B;[1.0,1.5] #C;[2.0,2.5] }
 * #B;1.4-&gt;#D;1.5
 * #B;1.5-&gt;#D;2.0
 * #C;2.5-&gt;#D;[1.0,1.6]
 * </pre>
 * 
 * <b>Result</b>: #B;1.4, #C;2.5, #D;1.5<br/>
 * <b>Details</b>The conflict manager finds that the latest matching version of #B (1.5) depends on
 * a version of #D incompatible with what is expected by the latest matching version of #C. Hence
 * the conflict manager blacklists #B;1.5, and the version range [1.0,1.5] is resolved again to end
 * up with #B;1.4 which depends on #D;1.5, which is fine to work with #C;2.5.
 * </p>
 */
public class LatestCompatibleConflictManager extends LatestConflictManager {
    public LatestCompatibleConflictManager() {
    }

    public LatestCompatibleConflictManager(String name, LatestStrategy strategy) {
        super(name, strategy);
    }

    @Override
    public Collection<IvyNode> resolveConflicts(IvyNode parent, Collection<IvyNode> conflicts) {
        if (conflicts.size() < 2) {
            return conflicts;
        }
        VersionMatcher versionMatcher = getSettings().getVersionMatcher();

        Iterator<IvyNode> iter = conflicts.iterator();
        IvyNode node = iter.next();
        ModuleRevisionId mrid = node.getResolvedId();

        if (versionMatcher.isDynamic(mrid)) {
            while (iter.hasNext()) {
                IvyNode other = iter.next();
                if (versionMatcher.isDynamic(other.getResolvedId())) {
                    // two dynamic versions in conflict, not enough information yet
                    return null;
                } else if (!versionMatcher.accept(mrid, other.getResolvedId())) {
                    // incompatibility found
                    if (!handleIncompatibleConflict(parent, conflicts, node, other)) {
                        return null;
                    }
                }
            }
            // no incompatibility nor dynamic version found, let's return the latest static version
            if (conflicts.size() == 2) {
                // very common special case of only two modules in conflict,
                // let's return the second one (static)
                Iterator<IvyNode> it = conflicts.iterator();
                it.next();
                return Collections.singleton(it.next());
            }
            Collection<IvyNode> newConflicts = new LinkedHashSet<IvyNode>(conflicts);
            newConflicts.remove(node);
            return super.resolveConflicts(parent, newConflicts);
        } else {
            // the first node is a static revision, let's see if all other versions match
            while (iter.hasNext()) {
                IvyNode other = iter.next();
                if (!versionMatcher.accept(other.getResolvedId(), mrid)) {
                    // incompatibility found
                    if (!handleIncompatibleConflict(parent, conflicts, node, other)) {
                        return null;
                    }
                }
            }
            // no incompatibility found, let's return this static version
            return Collections.singleton(node);
        }
    }

    /**
     * Handles an incompatible conflict
     * <p>
     * An incompatible conflicts is handled with this pseudo algorithm:
     * 
     * <pre>
     * take latest among two nodes in conflict
     *   for all callers
     *      if dependency is a version constraint (dynamic)
     *         blacklist the mapped version
     *      else
     *         recurse for all callers
     *   if a version constraint has been found
     *     restart resolve
     *   else
     *     throw strict conflict exception
     * </pre>
     * 
     * </p>
     * 
     * @param parent
     *            the parent node of nodes in conflict
     * @param conflicts
     *            all the nodes in conflict
     * @param node
     *            one of the two incompatible nodes
     * @param other
     *            the other incompatible node
     * @return true if the incompatible conflict has been handled, false otherwise (in which case
     *         resolveConflicts should return null)
     */
    private boolean handleIncompatibleConflict(IvyNode parent, Collection<IvyNode> conflicts,
            IvyNode node, IvyNode other) {
        // we never actually return anything else than false or throw an exception,
        // but returning a boolean make the calling code cleaner
        try {
            IvyNodeArtifactInfo latest = (IvyNodeArtifactInfo) getStrategy().findLatest(
                toArtifactInfo(Arrays.asList(new IvyNode[] {node, other})), null);
            if (latest != null) {
                IvyNode latestNode = latest.getNode();
                IvyNode oldestNode = latestNode == node ? other : node;
                blackListIncompatibleCallerAndRestartResolveIfPossible(getSettings(), parent,
                    oldestNode, latestNode);
                // if we arrive here, we haven't managed to blacklist all paths to the latest
                // node, we try with the oldest
                blackListIncompatibleCallerAndRestartResolveIfPossible(getSettings(), parent,
                    latestNode, oldestNode);
                // still not possible, we aren't able to find a solution to the incompatibility
                handleUnsolvableConflict(parent, conflicts, node, other);

                return true; // never actually reached
            } else {
                return false;
            }
        } catch (NoConflictResolvedYetException ex) {
            // we have not enough informations in the nodes to resolve conflict
            // according to the resolveConflicts contract, resolveConflicts must return null
            return false;
        }
    }

    private void blackListIncompatibleCallerAndRestartResolveIfPossible(IvySettings settings,
            IvyNode parent, IvyNode selected, IvyNode evicted) {
        Stack<IvyNode> callerStack = new Stack<IvyNode>();
        callerStack.push(evicted);
        Collection<IvyNodeBlacklist> toBlacklist = blackListIncompatibleCaller(
            settings.getVersionMatcher(), parent, selected, evicted, callerStack);
        if (toBlacklist != null) {
            final StringBuffer blacklisted = new StringBuffer();
            for (Iterator<IvyNodeBlacklist> iterator = toBlacklist.iterator(); iterator.hasNext();) {
                IvyNodeBlacklist blacklist = iterator.next();
                blacklist.getBlacklistedNode().blacklist(blacklist);
                blacklisted.append(blacklist.getBlacklistedNode());
                if (iterator.hasNext()) {
                    blacklisted.append(" ");
                }
            }

            String rootModuleConf = parent.getData().getReport().getConfiguration();
            evicted.markEvicted(new EvictionData(rootModuleConf, parent, this, Collections
                    .singleton(selected), "with blacklisting of " + blacklisted));

            if (settings.debugConflictResolution()) {
                Message.debug("evicting " + evicted + " by "
                        + evicted.getEvictedData(rootModuleConf));
            }
            throw new RestartResolveProcess("trying to handle incompatibilities between "
                    + selected + " and " + evicted);
        }
    }

    private boolean handleIncompatibleCaller(Stack<IvyNode> callerStack, IvyNode node,
            IvyNode callerNode, IvyNode conflictParent, IvyNode selectedNode, IvyNode evictedNode,
            Collection<IvyNodeBlacklist> blacklisted, VersionMatcher versionMatcher) {
        if (callerStack.subList(0, callerStack.size() - 1).contains(node)) {
            // circular dependency found and handled: the current top of the stack (node)
            // was already contained in the rest of the stack, the circle is closed, nothing
            // else to do
            return true;
        } else {
            callerStack.push(callerNode);
            Collection<IvyNodeBlacklist> sub = blackListIncompatibleCaller(versionMatcher,
                conflictParent, selectedNode, evictedNode, callerStack);
            callerStack.pop();
            if (sub == null) {
                // propagate the fact that a path with unblacklistable caller has been found
                return false;
            } else {
                blacklisted.addAll(sub);
                return true;
            }
        }
    }

    /**
     * Tries to blacklist exactly one version for all callers paths.
     * 
     * @param versionMatcher
     *            the version matcher to use to interpret versions
     * @param conflictParent
     *            the node in which the conflict is occurring
     * @param selectedNode
     *            the node in favor of which the conflict is resolved
     * @param evictedNode
     *            the node which will be evicted if we are able to blacklist all paths
     * @param node
     *            the node for which callers should be considered
     * @return the collection of blacklisting to do, null if a blacklist is not possible in at least
     *         one caller path
     */
    private Collection<IvyNodeBlacklist> blackListIncompatibleCaller(VersionMatcher versionMatcher,
            IvyNode conflictParent, IvyNode selectedNode, IvyNode evictedNode,
            Stack<IvyNode> callerStack) {
        Collection<IvyNodeBlacklist> blacklisted = new ArrayList<IvyNodeBlacklist>();
        IvyNode node = callerStack.peek();
        String rootModuleConf = conflictParent.getData().getReport().getConfiguration();
        Caller[] callers = node.getCallers(rootModuleConf);
        for (int i = 0; i < callers.length; i++) {
            IvyNode callerNode = node.findNode(callers[i].getModuleRevisionId());
            if (callerNode.isBlacklisted(rootModuleConf)) {
                continue;
            }
            if (versionMatcher.isDynamic(callers[i].getAskedDependencyId(node.getData()))) {
                blacklisted.add(new IvyNodeBlacklist(conflictParent, selectedNode, evictedNode,
                        node, rootModuleConf));
                if (node.isEvicted(rootModuleConf)
                        && !handleIncompatibleCaller(callerStack, node, callerNode, conflictParent,
                            selectedNode, evictedNode, blacklisted, versionMatcher)) {
                    return null;
                }
            } else if (!handleIncompatibleCaller(callerStack, node, callerNode, conflictParent,
                selectedNode, evictedNode, blacklisted, versionMatcher)) {
                return null;
            }
        }
        if (blacklisted.isEmpty() && !callerStack.subList(0, callerStack.size() - 1).contains(node)) {
            return null;
        }
        return blacklisted;
    }

    protected void handleUnsolvableConflict(IvyNode parent, Collection<IvyNode> conflicts,
            IvyNode node1, IvyNode node2) {
        throw new StrictConflictException(node1, node2);
    }

    @Override
    public void handleAllBlacklistedRevisions(DependencyDescriptor dd,
            Collection<ModuleRevisionId> foundBlacklisted) {
        ResolveData resolveData = IvyContext.getContext().getResolveData();
        Collection<IvyNode> blacklisted = new HashSet<IvyNode>();
        for (ModuleRevisionId mrid : foundBlacklisted) {
            blacklisted.add(resolveData.getNode(mrid));
        }

        for (IvyNode node : blacklisted) {
            IvyNodeBlacklist bdata = node.getBlacklistData(resolveData.getReport()
                    .getConfiguration());
            handleUnsolvableConflict(bdata.getConflictParent(),
                Arrays.asList(bdata.getEvictedNode(), bdata.getSelectedNode()),
                bdata.getEvictedNode(), bdata.getSelectedNode());
        }
    }

    @Override
    public String toString() {
        return getName();
    }
}
