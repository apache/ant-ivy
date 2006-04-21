/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.conflict;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.jayasoft.ivy.IvyNode;
import fr.jayasoft.ivy.util.Message;

/**
 * A ConflictManager that can be used to resolve conflicts based on regular
 * expressions of the revision of the module. The conflict manager is added like
 * this:
 * 
 * <pre>
 *    &lt;!-- Match all revisions, but ignore the last dot(.) and the character after it.
 *        Used to match api changes in out milestones. --&gt;
 *    &lt;conflict-managers&gt;
 *        &lt;regexp-cm name=&quot;regexp&quot; regexp=&quot;(.*)\..$&quot; ignoreNonMatching=&quot;true&quot;/&gt;
 *    &lt;/conflict-managers&gt;
 * </pre>
 * 
 * The regular expression must contain a capturing group. The group will be used
 * to resolve the conflicts by an String.equals() test. If ignoreNonMatching is
 * false non matching modules will result in an exception. If it is true they
 * will be compaired by their full revision.
 * 
 * @author Anders janmyr
 * 
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
			String message = "Pattern does not contain ONE (capturing group): '"
					+ pattern + "'";
			Message.error(message);
			throw new IllegalArgumentException(message);
		}
	}

	public void setIgnoreNonMatching(boolean ignoreNonMatching) {
		mIgnoreNonMatching = ignoreNonMatching;
	}

	public Collection resolveConflicts(IvyNode parent, Collection conflicts) {
		IvyNode lastNode = null;
		for (Iterator iter = conflicts.iterator(); iter.hasNext();) {
			IvyNode node = (IvyNode) iter.next();

			if (lastNode != null && !matchEquals(node, lastNode)) {
				String msg = lastNode + ":" + getMatch(lastNode)
						+ " (needed by " + lastNode.getParent()
						+ ") conflicts with " + node + ":" + getMatch(node)
						+ " (needed by " + node.getParent() + ")";
				Message.error(msg);
				Message.sumupProblems();
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
			warnOrThrow("First group of pattern: '" + pattern
					+ "' does not match: " + revision + " " + node);
		} else {
			warnOrThrow("Pattern: '" + pattern + "' does not match: "
					+ revision + " " + node);
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
