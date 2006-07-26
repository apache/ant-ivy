package fr.jayasoft.ivy.repository.vfs;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.vfs.FileSystemOptions;
import org.apache.commons.vfs.provider.GenericFileName;
import org.apache.commons.vfs.provider.webdav.WebDavFileSystem;


/**
 * This class extends from WebDavFileSystem because it doesn't provide an accessible constructor.
 * 
 * @author Maarten Coene
 */
class IvyWebdavFileSystem extends WebDavFileSystem {

	protected IvyWebdavFileSystem(GenericFileName arg0, HttpClient arg1, FileSystemOptions arg2) {
		super(arg0, arg1, arg2);
	}

}
