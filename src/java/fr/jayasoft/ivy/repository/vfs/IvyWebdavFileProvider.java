package fr.jayasoft.ivy.repository.vfs;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.vfs.FileName;
import org.apache.commons.vfs.FileSystem;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemOptions;
import org.apache.commons.vfs.provider.GenericFileName;
import org.apache.commons.vfs.provider.webdav.WebdavFileProvider;

/**
 * Modified version of the WebdavFileProvider from VFS which adds support for httpclient 3.x.
 * See http://issues.apache.org/jira/browse/VFS-74 for more info.
 * 
 * A provider for WebDAV.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @author Maarten Coene
 * @version $Revision: 417178 $ $Date: 2006-06-26 05:31:41 -0700 (Mon, 26 Jun 2006) $
 */
public class IvyWebdavFileProvider extends WebdavFileProvider {

	/***********************************************************************************************
	 * Creates a filesystem.
	 */
	protected FileSystem doCreateFileSystem(final FileName name,
			final FileSystemOptions fileSystemOptions) throws FileSystemException {
		// Create the file system
		final GenericFileName rootName = (GenericFileName) name;

		HttpClient httpClient = IvyWebdavClientFactory.createConnection(rootName.getHostName(),
				rootName.getPort(), rootName.getUserName(), rootName.getPassword(),
				fileSystemOptions);

		return new IvyWebdavFileSystem(rootName, httpClient, fileSystemOptions);
	}

}
