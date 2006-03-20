/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.conflict;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import fr.jayasoft.ivy.ArtifactInfo;
import fr.jayasoft.ivy.DependencyDescriptor;
import fr.jayasoft.ivy.IvyNode;
import fr.jayasoft.ivy.LatestStrategy;
import fr.jayasoft.ivy.util.Message;

public class LatestConflictManager extends AbstractConflictManager {
    private static class IvyNodeArtifactInfo implements ArtifactInfo {
        private final IvyNode _node;

        private IvyNodeArtifactInfo(IvyNode dep) {
            _node = dep;
        }

        public long getLastModified() {
            return _node.getPublication();
        }

        public String getRevision() {
            return _node.getId().getRevision();
        }
        
        public IvyNode getNode() {
            return _node;
        }
    }



    private LatestStrategy _strategy;
    private String _strategyName;

    public LatestConflictManager() {
    }

    public LatestConflictManager(LatestStrategy strategy) {
        _strategy = strategy;
    }

    public LatestConflictManager(String name, LatestStrategy strategy) {
        setName(name);
        _strategy = strategy;
    }

    public Collection resolveConflicts(IvyNode parent, Collection conflicts) {
        if (conflicts.size() < 2) {
            return conflicts;
        }
        for (Iterator iter = conflicts.iterator(); iter.hasNext();) {
            IvyNode node = (IvyNode)iter.next();
            DependencyDescriptor dd = node.getDependencyDescriptor(parent);
            if (dd != null && dd.isForce() && parent.getResolvedId().equals(dd.getParentRevisionId())) {
                return Collections.singleton(node);
            }
        }
        ArtifactInfo latest = getStrategy().findLatest(toArtifactInfo(conflicts), null);
        if (latest != null) {
            return Collections.singleton(((IvyNodeArtifactInfo)latest).getNode());
        } else {
            return conflicts;
        }
    }

    private ArtifactInfo[] toArtifactInfo(Collection conflicts) {
        List artifacts = new ArrayList(conflicts.size());
        for (Iterator iter = conflicts.iterator(); iter.hasNext();) {
            IvyNode node = (IvyNode)iter.next();
            artifacts.add(new IvyNodeArtifactInfo(node));
        }
        return (ArtifactInfo[])artifacts.toArray(new ArtifactInfo[artifacts.size()]);
    }

    public LatestStrategy getStrategy() {
        if (_strategy == null) {
            if (_strategyName != null) {
                _strategy = getIvy().getLatestStrategy(_strategyName);
                if (_strategy == null) {
                    Message.error("unknown latest strategy: "+_strategyName);
                    _strategy = getIvy().getDefaultLatestStrategy();
                }
            } else {
                _strategy = getIvy().getDefaultLatestStrategy();
            }
        }
        return _strategy;
    }
    

    /**
     * To conform to configurator API
     * @param latestStrategy
     */
    public void setLatest(String strategyName) {
        _strategyName = strategyName;
    }
    
    public void setStrategy(LatestStrategy strategy) {
        _strategy = strategy;
    }
    

    public String toString() {
        return String.valueOf(_strategy);
    }
}
