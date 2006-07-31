package fr.jayasoft.ivy.event;

import fr.jayasoft.ivy.filter.Filter;

public interface Trigger extends IvyListener {
	Filter getEventFilter();
}
