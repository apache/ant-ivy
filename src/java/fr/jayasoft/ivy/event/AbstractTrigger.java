package fr.jayasoft.ivy.event;

import fr.jayasoft.ivy.IvyContext;
import fr.jayasoft.ivy.filter.Filter;
import fr.jayasoft.ivy.matcher.PatternMatcher;

/**
 * Base class for easy trigger implementation.
 * 
 * This base class takes of the event filtering part, the only method to implement in subclasses
 * is {@link IvyListener#progress(IvyEvent)} which should do whatever the trigger needs to do when
 * the event occurs. This method will only be called when an event matching the trigger filter occurs.
 * 
 * 
 * 
 * @since 1.4
 * @author Xavier Hanin
 *
 */
public abstract class AbstractTrigger implements Trigger {
	private Filter _filter;
	
	private String _event;
	private String _filterExpression;
	private String _matcher = PatternMatcher.EXACT;
	
	public Filter getEventFilter() {
		if (_filter == null) {
			_filter = createFilter();
		}
		return _filter;
	}

	private Filter createFilter() {
		return new IvyEventFilter(getEvent(), getFilter(), getPatternMatcher());
	}

	private PatternMatcher getPatternMatcher() {
		return IvyContext.getContext().getIvy().getMatcher(_matcher);
	}

	public String getEvent() {
		return _event;
	}

	public void setEvent(String event) {
		_event = event;
	}

	public String getFilter() {
		return _filterExpression;
	}

	public void setFilter(String filterExpression) {
		_filterExpression = filterExpression;
	}

	public String getMatcher() {
		return _matcher;
	}

	public void setMatcher(String matcher) {
		_matcher = matcher;
	}


}
