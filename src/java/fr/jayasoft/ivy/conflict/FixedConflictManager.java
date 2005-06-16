/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.conflict;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import fr.jayasoft.ivy.IvyNode;

public class FixedConflictManager extends AbstractConflictManager {
    private Collection _revs;
    public FixedConflictManager(String[] revs) {
        _revs = Arrays.asList(revs);
        setName("fixed"+_revs);
    }
    public Collection resolveConflicts(IvyNode parent, Collection conflicts) {
        Collection resolved = new ArrayList(conflicts.size());
        for (Iterator iter = conflicts.iterator(); iter.hasNext();) {
            IvyNode node = (IvyNode)iter.next();
            String revision = node.getResolvedId().getRevision();
            if (_revs.contains(revision)) {
                resolved.add(node);
            }
        }
        return resolved;
    }

    public Collection getRevs() {
        return _revs;
    }
    
}
