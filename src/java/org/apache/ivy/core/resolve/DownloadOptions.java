package org.apache.ivy.core.resolve;

import java.io.File;

import org.apache.ivy.core.cache.CacheManager;
import org.apache.ivy.core.event.EventManager;
import org.apache.ivy.core.settings.IvySettings;

public class DownloadOptions {
	private IvySettings _settings;
	private CacheManager _cacheManager;
	private EventManager _eventManager = null; // can be null
	private boolean _useOrigin = false;

	public DownloadOptions(IvySettings settings, File cache) {
		this(settings, new CacheManager(settings, cache));
	}
	
	public DownloadOptions(IvySettings settings, CacheManager cacheManager) {
		this(settings, cacheManager, null, false);
	}
	
	public DownloadOptions(IvySettings settings, CacheManager cacheManager, EventManager eventManager, boolean useOrigin) {
		_settings = settings;
		_cacheManager = cacheManager;
		_eventManager = eventManager;
		_useOrigin = useOrigin;
	}
	
	public IvySettings getSettings() {
		return _settings;
	}
	public boolean isUseOrigin() {
		return _useOrigin;
	}

	public CacheManager getCacheManager() {
		return _cacheManager;
	}

	public EventManager getEventManager() {
		return _eventManager;
	}
	
}
