package fr.jayasoft.ivy.repository;

public class BasicResource implements Resource {
	private boolean _local;
	private String _name;
	private long _lastModified;
	private long _contentLength;
	private boolean _exists;
	
	
	public BasicResource(String name, boolean exists, long contentLength, long lastModified, boolean local) {
		_name = name;
		_exists = exists;
		_contentLength = contentLength;
		_lastModified = lastModified;
		_local = local;
	}

	public Resource clone(String cloneName) {
		throw new UnsupportedOperationException("bassic resource do not support the clone method");
	}

	public boolean exists() {
		return _exists;
	}

	public long getContentLength() {
		return _contentLength;
	}

	public long getLastModified() {
		return _lastModified;
	}

	public String getName() {
		return _name;
	}

	public boolean isLocal() {
		return _local;
	}

}
