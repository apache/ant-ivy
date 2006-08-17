package fr.jayasoft.ivy.repository;


public abstract class LazyResource implements Resource {
	private boolean _init = false;
	
	private boolean _local;
	private String _name;
	private long _lastModified;
	private long _contentLength;
	private boolean _exists;
	
	
	public LazyResource(String name) {
		_name = name;
	}
	
	protected abstract void init();

	private void checkInit() {
		if (!_init) {
			init();
			_init = true;
		}
	}
	
	public boolean exists() {
		checkInit();
		return _exists;
	}

	public long getContentLength() {
		checkInit();
		return _contentLength;
	}

	public long getLastModified() {
		checkInit();
		return _lastModified;
	}

	public String getName() {
		return _name;
	}

	public boolean isLocal() {
		checkInit();
		return _local;
	}

	public String toString() {
		return getName();
	}

	protected void setContentLength(long contentLength) {
		_contentLength = contentLength;
	}

	protected void setExists(boolean exists) {
		_exists = exists;
	}

	protected void setLastModified(long lastModified) {
		_lastModified = lastModified;
	}

	protected void setLocal(boolean local) {
		_local = local;
	}

    protected void init(Resource r) {
		setContentLength(r.getContentLength());
		setLocal(r.isLocal());
		setLastModified(r.getLastModified());
		setExists(r.exists());
	}


}
