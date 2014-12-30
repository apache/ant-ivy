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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.util.Message;

/**
 * A ConflictManager that can be used to resolve conflicts based on regular expressions of the
 * revision of the module. The conflict manager is added like this:
 * 
 * <pre>
 *    &lt;!-- Match all revisions, but ignore the last dot(.) and the character after it.
 *        Used to match api changes in out milestones. --&gt;
 *    &lt;conflict-managers&gt;
 *        &lt;regexp-cm name=&quot;regexp&quot; 
 *                   regexp=&quot;(.*)\..$&quot; ignoreNonMatching=&quot;true&quot;/&gt;
 *    &lt;/conflict-managers&gt;
 * </pre>
 * 
 * The regular expression must contain a capturing group. The group will be used to resolve the
 * conflicts by an String.equals() test. If ignoreNonMatching is false non matching modules will
 * result in an exception. If it is true they will be compaired by their full revision.
 */
public class RegexpConflictManager extends AbstractConflictManager {
    private Pattern pattern = Pattern.compile("(.*)");

    private boolean mIgnoreNonMatching;

    public RegexpConflictManager() {
    }

    public void setRegexp(String regexp) {
        pattern = Pattern.compile(regexp);
        Matcher matcher = pattern.matcher("abcdef");
        if (matcher.groupCount() != 1) {
            String message = "Pattern does not contain ONE (capturing group): '" + pattern + "'";
            Message.error(message);
            throw new IllegalArgumentException(message);
        }
    }

    public void setIgnoreNonMatching(boolean ignoreNonMatching) {
        mIgnoreNonMatching = ignoreNonMatching;
    }

    public Collection<IvyNode> resolveConflicts(IvyNode parent, Collection<IvyNode> conflicts) {
        IvyNode lastNode = null;
        for (IvyNode node : conflicts) {

            if (lastNode != null && !matchEquals(node, lastNode)) {
                String msg = lastNode + ":" + getMatch(lastNode) + " (needed by "
                        + Arrays.asList(lastNode.getAllRealCallers()) + ") conflicts with " + node
                        + ":" + getMatch(node) + " (needed by "
                        + Arrays.asList(node.getAllRealCallers()) + ")";
                throw new StrictConflictException(msg);
            }
            if (lastNode == null || nodeIsGreater(node, lastNode)) {
                lastNode = node;
            }
        }

        return Collections.singleton(lastNode);
    }

    private boolean nodeIsGreater(IvyNode node, IvyNode lastNode) {
        return getMatch(node).compareTo(getMatch(lastNode)) > 0;
    }

    private boolean matchEquals(IvyNode lastNode, IvyNode node) {
        return getMatch(lastNode).equals(getMatch(node));
    }

    private String getMatch(IvyNode node) {
        String revision = node.getId().getRevision();
        Matcher matcher = pattern.matcher(revision);
        if (matcher.matches()) {
            String match = matcher.group(1);
            if (match != null) {
                return match;
            }
            warnOrThrow("First group of pattern: '" + pattern + "' does not match: " + revision
                    + " " + node);
        } else {
            warnOrThrow("Pattern: '" + pattern + "' does not match: " + revision + " " + node);
        }
        return revision;
    }

    private void warnOrThrow(String message) {
        if (mIgnoreNonMatching) {
            Message.warn(message);
        } else {
            throw new StrictConflictException(message);
        }
    }
}
