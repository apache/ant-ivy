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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.util.Message;



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
                String msg = lastNode + " (needed by " + Arrays.asList(lastNode.getAllCallers()) + ") conflicts with " + node + " (needed by " + Arrays.asList(node.getAllCallers()) + ")";
                Message.error(msg);
                Message.sumupProblems();
                throw new StrictConflictException(msg);
            }
            lastNode = node;
        }

        return Collections.singleton(lastNode);
    }

}
