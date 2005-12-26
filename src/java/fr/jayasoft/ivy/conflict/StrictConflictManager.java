/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 *
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.conflict;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import fr.jayasoft.ivy.IvyNode;
import fr.jayasoft.ivy.conflict.AbstractConflictManager;
import fr.jayasoft.ivy.util.Message;


public class StrictConflictManager extends AbstractConflictManager
{

    public StrictConflictManager() {
    }


    public Collection resolveConflicts(IvyNode parent, Collection conflicts)
    {
        IvyNode lastNode = null;
        for (Iterator iter = conflicts.iterator(); iter.hasNext();) {
            IvyNode node = (IvyNode)iter.next();

            if (lastNode != null && !lastNode.equals(node)) {
                String msg = lastNode + " (needed by " + lastNode.getParent() + ") conflicts with " + node + " (needed by " + node.getParent() + ")";
                Message.error(msg);
                Message.sumupProblems();
                throw new StrictConflictException(msg);
            }
            lastNode = node;
        }

        return Collections.singleton(lastNode);
    }

}
