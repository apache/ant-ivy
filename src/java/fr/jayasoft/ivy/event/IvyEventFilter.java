package fr.jayasoft.ivy.event;

import fr.jayasoft.ivy.filter.AndFilter;
import fr.jayasoft.ivy.filter.Filter;
import fr.jayasoft.ivy.filter.NoFilter;
import fr.jayasoft.ivy.filter.NotFilter;
import fr.jayasoft.ivy.filter.OrFilter;
import fr.jayasoft.ivy.matcher.ExactPatternMatcher;
import fr.jayasoft.ivy.matcher.Matcher;
import fr.jayasoft.ivy.matcher.PatternMatcher;

public class IvyEventFilter implements Filter {
	private PatternMatcher _matcher;
	private Filter _nameFilter;
	private Filter _attFilter;

	public IvyEventFilter(String event, String filterExpression, PatternMatcher matcher) {
		_matcher = matcher == null ? ExactPatternMatcher.getInstance() : matcher;
		if (event == null) {
			_nameFilter = NoFilter.INSTANCE;
		} else {
			final Matcher eventNameMatcher = _matcher.getMatcher(event);
			_nameFilter = new Filter() {
				public boolean accept(Object o) {
					IvyEvent e = (IvyEvent) o;
					return eventNameMatcher.matches(e.getName());
				}
			};
		}
		_attFilter = filterExpression == null  || filterExpression.trim().length() == 0 ?
				NoFilter.INSTANCE
				: parseExpression(filterExpression);
	}

	private Filter parseExpression(String filterExpression) {
		// expressions handled for the moment: (informal grammar)
		// EXP := SIMPLE_EXP | AND_EXP | OR_EXP | NOT_EXP
		// AND_EXP := EXP && EXP
		// OR_EXP := EXP || EXP
		// NOT_EXP := ! EXP
		// SIMPLE_EXP := attname = comma, separated, list, of, accepted, values
		// example: organisation = foo && module = bar, baz
		filterExpression = filterExpression.trim();
		int index = filterExpression.indexOf("&&");
		if (index == -1) {
			index = filterExpression.indexOf("||");
			if (index == -1) {
				if (filterExpression.startsWith("!")) {
					return new NotFilter(parseExpression(filterExpression.substring(1)));
				} else {
					index = filterExpression.indexOf("=");
					if (index == -1) {
						throw new IllegalArgumentException("bad filter expression: "+filterExpression+": no equal sign found");
					}
					final String attname = filterExpression.substring(0, index).trim();
					String[] values = filterExpression.substring(index+1).trim().split(",");
					final Matcher[] matchers = new Matcher[values.length];
					for (int i = 0; i < values.length; i++) {
						matchers[i] = _matcher.getMatcher(values[i].trim());
					}
					return new Filter() {
						public boolean accept(Object o) {
							IvyEvent e = (IvyEvent) o;
							String val = (String) e.getAttributes().get(attname);
							if (val == null) {
								return false;
							}
							for (int i = 0; i < matchers.length; i++) {
								if (matchers[i].matches(val)) {
									return true;
								}
							}
							return false;
						}
					};
				}
			} else {
				return new OrFilter(parseExpression(filterExpression.substring(0, index)), parseExpression(filterExpression.substring(index+2)));
			}
		} else {
			return new AndFilter(parseExpression(filterExpression.substring(0, index)), parseExpression(filterExpression.substring(index+2)));
		}
	}

	public boolean accept(Object o) {
		if (! (o instanceof IvyEvent)) {
			return false;
		}
		return _nameFilter.accept(o) && _attFilter.accept(o);
	}

}
