package fr.jayasoft.ivy.repository.vsftp;

import java.io.IOException;

import fr.jayasoft.ivy.repository.BasicResource;
import fr.jayasoft.ivy.repository.Resource;

public class VsftpResource extends BasicResource {
	private VsftpRepository _repository;
	
	public VsftpResource(VsftpRepository repository, String name, boolean exists, long contentLength, long lastModified) {
		super(name, exists, contentLength, lastModified, false);
		_repository = repository;
	}

	public Resource clone(String cloneName) {
		try {
			return _repository.getResource(cloneName);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
