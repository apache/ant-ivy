package fr.jayasoft.ivy.repository.ssh;


import java.io.IOException;
import java.io.InputStream;

import fr.jayasoft.ivy.repository.Resource;
import fr.jayasoft.ivy.util.Message;

/**
 * Resource for SSH Ivy Repository
 */
public class SshResource implements Resource {

    private boolean resolved = false;
	private String uri = null;
	private boolean bExists = false;
	private long len = 0;
	private long lastModified = 0;
    private SshRepository repository = null;
	
	public SshResource() {
        resolved = true;
	}
	
    public SshResource(SshRepository repository, String uri) {
        this.uri = uri;
        this.repository = repository;
        resolved = false;
    }
    
	public SshResource(SshRepository repository, String uri, boolean bExists, long len, long lastModified) {
		this.uri = uri;
		this.bExists = bExists;
		this.len = len;
		this.lastModified = lastModified;
        this.repository = repository;
        resolved = true;
	}
	
	/* (non-Javadoc)
	 * @see fr.jayasoft.ivy.repository.Resource#exists()
	 */
	public boolean exists() {
        if(!resolved)
            resolve();
		return bExists;
	}

	/* (non-Javadoc)
	 * @see fr.jayasoft.ivy.repository.Resource#getContentLength()
	 */
	public long getContentLength() {
        if(!resolved)
            resolve();
		return len;
	}

	/* (non-Javadoc)
	 * @see fr.jayasoft.ivy.repository.Resource#getLastModified()
	 */
	public long getLastModified() {
        if(!resolved)
            resolve();
		return lastModified;
	}

	private void resolve() {
        Message.debug("SShResource: resolving "+uri);
        SshResource res = repository.resolveResource(uri);
        len = res.getContentLength();
        lastModified = res.getLastModified();
        bExists = res.exists();
        resolved = true;
        Message.debug("SShResource: resolved "+this);
    }

    /* (non-Javadoc)
	 * @see fr.jayasoft.ivy.repository.Resource#getName()
	 */
	public String getName() {
		return uri;
	}

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("SshResource:");
        buffer.append(uri);
        buffer.append(" (");
        buffer.append(len);
        buffer.append(")]");
        return buffer.toString();
    }
    
    public boolean isLocal() {
        return false;
    }

    public Resource clone(String cloneName) {
        return new SshResource(repository, cloneName);
    }

    public InputStream openStream() throws IOException {
        return repository.openStream(this);
    }
}
