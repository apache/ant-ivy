package fr.jayasoft.ivy.repository.vfs;

import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpURL;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemOptions;
import org.apache.commons.vfs.provider.webdav.WebdavConnectionManager;
import org.apache.commons.vfs.provider.webdav.WebdavFileSystemConfigBuilder;
import org.apache.webdav.lib.WebdavResource;

import fr.jayasoft.ivy.url.HttpClientHandler;

/***************************************************************************************************
 * Modified version of the WebdavClientFactory from VFS which adds support for httpclient 3.x.
 * See http://issues.apache.org/jira/browse/VFS-74 for more info.
 * 
 * Create a HttpClient instance
 * 
 * @author <a href="mailto:imario@apache.org">Mario Ivankovits</a>
 * @author Maarten Coene
 * @version $Revision: 330479 $ $Date: 2005-11-03 07:19:24 +0100 (Do, 03 Nov 2005) $
 */
class IvyWebdavClientFactory {

	private IvyWebdavClientFactory() {
	}

	/***********************************************************************************************
	 * Creates a new connection to the server.
	 */
	public static HttpClient createConnection(String hostname, int port, String username,
			String password, FileSystemOptions fileSystemOptions) throws FileSystemException {
		// Create an Http client
		HttpClient client;
		try {
			final HttpURL url = new HttpURL(username, password, hostname, port, "/");

			// WebdavResource resource = null;
			WebdavResource resource = new WebdavResource() {
			};

			if (fileSystemOptions != null) {
				String proxyHost = WebdavFileSystemConfigBuilder.getInstance().getProxyHost(
						fileSystemOptions);
				int proxyPort = WebdavFileSystemConfigBuilder.getInstance().getProxyPort(
						fileSystemOptions);

				if (proxyHost != null && proxyPort > 0) {
					// resource = new WebdavResource(url, proxyHost, proxyPort);
					resource.setProxy(proxyHost, proxyPort);
				}
			}

			/*
			 * if (resource == null) { resource = new WebdavResource(url); }
			 * resource.setProperties(WebdavResource.NOACTION, 1);
			 */
			resource.setHttpURL(url, WebdavResource.NOACTION, 1);

			client = resource.retrieveSessionInstance();
			HttpClientHandler handler = new HttpClientHandler();
			int httpClientVersion = handler.getHttpClientMajorVersion();
			if (httpClientVersion == 2) {
				// VFS only supports httpclient v2 for now...
				client.setHttpConnectionManager(new WebdavConnectionManager());
			} else {
				client.setHttpConnectionManager(new IvyWebdavConnectionManager());
			}
		} catch (final IOException e) {
			throw new FileSystemException("vfs.provider.webdav/connect.error", hostname, e);
		}

		return client;
	}
}
