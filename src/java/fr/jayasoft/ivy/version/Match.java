package fr.jayasoft.ivy.version;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.ModuleRevisionId;
import fr.jayasoft.ivy.matcher.Matcher;
import fr.jayasoft.ivy.matcher.PatternMatcher;
import fr.jayasoft.ivy.util.IvyPatternHelper;


/**
 * 
 * @author Maarten Coene
 */
public class Match {
	private String _revision;
	private String _pattern;
	private String _args;
	private String _matcher;
	
	public String getArgs() {
		return _args;
	}
	
	public void setArgs(String args) {
		this._args = args;
	}
	
	public String getMatcher() {
		return _matcher;
	}
	
	public void setMatcher(String matcher) {
		this._matcher = matcher;
	}
	
	public String getPattern() {
		return _pattern;
	}
	
	public void setPattern(String pattern) {
		this._pattern = pattern;
	}
	
	public String getRevision() {
		return _revision;
	}
	
	public void setRevision(String revision) {
		this._revision = revision;
	}
	
	public Matcher getPatternMatcher(ModuleRevisionId askedMrid) {
		String revision = askedMrid.getRevision();
		
		String[] args = split(getArgs());
		String[] argValues = getRevisionArgs(revision);
		
		if (args.length != argValues.length) {
			return new NoMatchMatcher();
		}
		
		Map variables = new HashMap();
		for (int i = 0; i < args.length; i++) {
			variables.put(args[i], argValues[i]);
		}
		
		String pattern = getPattern();
		pattern = IvyPatternHelper.substituteVariables(pattern, variables);
		
		PatternMatcher pMatcher = Ivy.getCurrent().getMatcher(_matcher);
		return pMatcher.getMatcher(pattern);
	}
	
	private String[] getRevisionArgs(String revision) {
		int bracketStartIndex = revision.indexOf('(');
		if (bracketStartIndex == -1) {
			return new String[0];
		}
		
		int bracketEndIndex = revision.indexOf(')');
		if (bracketEndIndex <= (bracketStartIndex + 1)) {
			return new String[0];
		}
		
		String args = revision.substring(bracketStartIndex + 1, bracketEndIndex);
		return split(args);
	}
	
	private static String[] split(String string) {
		if (string == null) {
			return new String[0];
		}
		
		StringTokenizer tokenizer = new StringTokenizer(string, ", ");
		List tokens = new ArrayList();
		while (tokenizer.hasMoreTokens()) {
			tokens.add(tokenizer.nextToken());
		}
		
		return (String[]) tokens.toArray(new String[tokens.size()]);
	}
	
	private static class NoMatchMatcher implements Matcher {
		public boolean isExact() {
			return false;
		}
		public boolean matches(String str) {
			return false;
		}
	}
}
