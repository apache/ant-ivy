package fr.jayasoft.ivy.version;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import fr.jayasoft.ivy.ModuleRevisionId;
import fr.jayasoft.ivy.matcher.Matcher;
import fr.jayasoft.ivy.version.AbstractVersionMatcher;

/**
 * 
 * @author Maarten Coene
 */
public class PatternVersionMatcher extends AbstractVersionMatcher {

	private List _matches = new ArrayList(); 
	private Map _RevisionMatches = new HashMap();  // revision -> list of Match instances
	private boolean _init = false;

	public void addMatch(Match match) {
		_matches.add(match);
	}
	
	private void init() {
		if (!_init) {
			for (Iterator it = _matches.iterator(); it.hasNext(); ) {
				Match match = (Match) it.next();
				List matches = (List) _RevisionMatches.get(match.getRevision());
				if (matches == null) {
					matches = new ArrayList();
					_RevisionMatches.put(match.getRevision(), matches);
				}
				matches.add(match);
			}
			_init = true;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean accept(ModuleRevisionId askedMrid, ModuleRevisionId foundMrid) {
		init();
		boolean accept = false;

		String revision = askedMrid.getRevision();
		int bracketIndex = revision.indexOf('(');
		if (bracketIndex > 0) {
			revision = revision.substring(0, bracketIndex);
		}
		
		List matches = (List) _RevisionMatches.get(revision);
		
		if (matches != null) {
			Iterator it = matches.iterator();
			while (!accept && it.hasNext()) {
				Match match = (Match) it.next();
				Matcher matcher = match.getPatternMatcher(askedMrid);
				accept = matcher.matches(foundMrid.getRevision());
			}
		}
		
		return accept;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isDynamic(ModuleRevisionId askedMrid) {
		init();
		String revision = askedMrid.getRevision();
		int bracketIndex = revision.indexOf('(');
		if (bracketIndex > 0) {
			revision = revision.substring(0, bracketIndex);
		}
		return _RevisionMatches.containsKey(revision);
	}

}
