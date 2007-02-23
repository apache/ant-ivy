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
package org.apache.ivy.plugins.version;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.matcher.Matcher;


/**
 * 
 * @author Maarten Coene
 */
public class PatternVersionMatcher extends AbstractVersionMatcher {

	private List _matches = new ArrayList(); 
	private Map _revisionMatches = new HashMap();  // revision -> list of Match instances
	private boolean _init = false;

	public void addMatch(Match match) {
		_matches.add(match);
	}
	
	private void init() {
		if (!_init) {
			for (Iterator it = _matches.iterator(); it.hasNext(); ) {
				Match match = (Match) it.next();
				List matches = (List) _revisionMatches.get(match.getRevision());
				if (matches == null) {
					matches = new ArrayList();
					_revisionMatches.put(match.getRevision(), matches);
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
		
		List matches = (List) _revisionMatches.get(revision);
		
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
		return _revisionMatches.containsKey(revision);
	}

}
