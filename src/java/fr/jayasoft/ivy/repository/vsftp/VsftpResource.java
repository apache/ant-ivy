package fr.jayasoft.ivy.repository.vsftp;

import java.io.IOException;
import java.io.InputStream;

import fr.jayasoft.ivy.repository.LazyResource;
import fr.jayasoft.ivy.repository.Resource;
import fr.jayasoft.ivy.util.Message;

public class VsftpResource extends LazyResource {
	private VsftpRepository _repository;
	
	public VsftpResource(VsftpRepository repository, String file) {
		super(file);
		_repository = repository;
	}

    protected void init() {
		try {
			init(_repository.getInitResource(getName()));
		} catch (IOException e) {
			Message.verbose(e.toString());
		}
    }
    

	public InputStream openStream() throws IOException {
    	throw new UnsupportedOperationException("vsftp resource does not support openStream operation");
    }
    
	public Resource clone(String cloneName) {
		try {
			return _repository.getResource(cloneName);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
