package fr.jayasoft.ivy.event;

import fr.jayasoft.ivy.filter.Filter;

public class FilteredIvyListener implements IvyListener {
	private IvyListener _listener;
	private Filter _filter;

	public FilteredIvyListener(IvyListener listener, Filter filter) {
		_listener = listener;
		_filter = filter;
	}

	public IvyListener getIvyListener() {
		return _listener;
	}

	public Filter getFilter() {
		return _filter;
	}

	public void progress(IvyEvent event) {
		if (_filter.accept(event)) {
			_listener.progress(event);
		}
	}

}
