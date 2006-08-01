/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.url;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthPolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;

import fr.jayasoft.ivy.util.CopyProgressListener;
import fr.jayasoft.ivy.util.Credentials;
import fr.jayasoft.ivy.util.FileUtil;
import fr.jayasoft.ivy.util.Message;

/**
 * @author Xavier Hanin
 *
 */
public class HttpClientHandler extends AbstractURLHandler {
    private static final SimpleDateFormat LAST_MODIFIED_FORMAT = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", Locale.US);
    
    // proxy configuration: obtain from system properties
    private int _proxyPort;

    private String _proxyRealm = null;
    private String _proxyHost = null;
    private String _proxyUserName = null;
    private String _proxyPasswd = null;
    
    private HttpClientHelper _httpClientHelper;
    
    public HttpClientHandler() {
        configureProxy();
    }

    private void configureProxy() {
        _proxyRealm = null;
        //no equivalent for realm in jdk proxy support ?
        _proxyHost = System.getProperty("http.proxyHost");
        //TODO constant is better ...
        if(useProxy()) {
            _proxyPort = Integer.parseInt(System.getProperty("http.proxyPort", "80"));
            _proxyUserName = System.getProperty("http.proxyUser");
            _proxyPasswd = System.getProperty("http.proxyPassword");
            //It seems there is no equivalent in HttpClient for
            // 'http.nonProxyHosts' property
            Message.verbose("proxy configured: host="+_proxyHost+" port="+_proxyPort+" user="+_proxyUserName);
        } else {
            Message.verbose("no proxy configured");
        }
    }
    
    
    
    public InputStream openStream(URL url) throws IOException {
        GetMethod get = doGet(url);
        return new GETInputStream(get);
    }
    
    public void download(URL src, File dest, CopyProgressListener l) throws IOException {
        GetMethod get = doGet(src);
        FileUtil.copy(get.getResponseBodyAsStream(), dest, l);
        get.releaseConnection();
    }
    
    public URLInfo getURLInfo(URL url) {
        return getURLInfo(url, 0);
    }

    public URLInfo getURLInfo(URL url, int timeout) {
        HeadMethod head = null;
        try {
            head = doHead(url, timeout);
            int status = head.getStatusCode();
            head.releaseConnection();
            if (status == HttpStatus.SC_OK) {
                return new URLInfo(true, getResponseContentLength(head), getLastModified(head));
            }
            if (status == HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED) {
                Message.error("Your proxy requires authentication.");
            }else if (String.valueOf(status).startsWith("4")) {
                Message.verbose("CLIENT ERROR: "+head.getStatusText()+" url="+url);
            }else if (String.valueOf(status).startsWith("5")) {
                Message.warn("SERVER ERROR: "+head.getStatusText()+" url="+url);
            }
            Message.debug("HTTP response status: "+status +"="+head.getStatusText()+" url="+url);
        } catch (HttpException e) {
            Message.error("HttpClientHandler: "+e.getMessage()+":" + e.getReasonCode()+"="+e.getReason()+" url="+url);
        } catch (UnknownHostException e) {
            Message.warn("Host " + e.getMessage() +" not found. url="+url);
            Message.info("You probably access the destination server through a proxy server that is not well configured.");
        }catch (IOException e) {
            Message.error("HttpClientHandler: "+e.getMessage()+" url="+url);
        } finally{
            if(head != null) {
                head.releaseConnection();
            }
        }
        return UNAVAILABLE;
    }
    
    private long getLastModified(HeadMethod head) {
        Header header = head.getResponseHeader("last-modified");
        if (header != null) {
            String lastModified = header.getValue();
            try {
                return LAST_MODIFIED_FORMAT.parse(lastModified).getTime();
            } catch (ParseException e) {
            }
            return System.currentTimeMillis();
        } else {
            return System.currentTimeMillis();
        }
    }

    private long getResponseContentLength(HeadMethod head) {
        return getHttpClientHelper().getResponseContentLength(head);
    }

    private HttpClientHelper getHttpClientHelper() {
        if (_httpClientHelper == null) {
            // use commons httpclient 3.0 if available
            try {
                HttpMethodBase.class.getMethod("getResponseContentLength", new Class[0]);
                _httpClientHelper = new HttpClientHelper3x();
                Message.verbose("using commons httpclient 3.x helper");
            } catch (SecurityException e) {
                Message.verbose("unable to get access to getResponseContentLength of commons-httpclient HeadMethod. Please use commons-httpclient 3.0 or use ivy with sufficient security permissions.");
                Message.verbose("exception: "+e.getMessage());
                _httpClientHelper = new HttpClientHelper2x();
                Message.verbose("using commons httpclient 2.x helper");
            } catch (NoSuchMethodException e) {
                _httpClientHelper = new HttpClientHelper2x();
                Message.verbose("using commons httpclient 2.x helper");
            }
        }
        return _httpClientHelper;
    }
    
    public int getHttpClientMajorVersion() {
    	HttpClientHelper helper = getHttpClientHelper();
    	return helper.getHttpClientMajorVersion();
    }

    private GetMethod doGet(URL url) throws IOException, HttpException {
        HttpClient client = getClient(url);

        GetMethod get = new GetMethod(url.toExternalForm());
        get.setDoAuthentication(useAuthentication(url) || useProxyAuthentication());
        client.executeMethod(get);
        return get;
    }

    private HeadMethod doHead(URL url, int timeout) throws IOException, HttpException {
        HttpClient client = getClient(url);
        client.setTimeout(timeout);

        HeadMethod head = new HeadMethod(url.toExternalForm());
        head.setDoAuthentication(useAuthentication(url) || useProxyAuthentication());
        client.executeMethod(head);
        return head;
    }

    private HttpClient getClient(URL url) {
        HttpClient client = new HttpClient();
        
        List authPrefs = new ArrayList(2);
        authPrefs.add(AuthPolicy.DIGEST);
        authPrefs.add(AuthPolicy.BASIC);
        // Exclude the NTLM authentication scheme because it is not supported by this class
        client.getParams().setParameter(AuthPolicy.AUTH_SCHEME_PRIORITY, authPrefs);

        if (useProxy()) {
            client.getHostConfiguration().setProxy(_proxyHost, _proxyPort);
            if (useProxyAuthentication()) {
                client.getState().setProxyCredentials(_proxyRealm, _proxyHost,
                    new UsernamePasswordCredentials(_proxyUserName, _proxyPasswd));
            }
        }
        Credentials c = getCredentials(url);
        if (c != null) {
        	Message.debug("found credentials for "+url+": "+c);
	        client.getState().setCredentials(
	            c.getRealm(),
	            c.getHost(),
	            new UsernamePasswordCredentials(c.getUserName(), c.getPasswd())
	        );
        }
        return client;
    }

    private boolean useProxy() {
        return _proxyHost != null && _proxyHost.trim().length() > 0;
    }
    private boolean useAuthentication(URL url) {
        return getCredentials(url) != null;
    }
    
    private Credentials getCredentials(URL url) {
		return CredentialsStore.INSTANCE.getCredentials(null, url.getHost());
	}

	private boolean useProxyAuthentication() {
        return (_proxyUserName != null && _proxyUserName.trim().length() > 0);
    }
    
    private static final class GETInputStream extends InputStream {
        private InputStream _is;
        private GetMethod _get;

        private GETInputStream(GetMethod get) throws IOException {
            _get = get;
            _is = get.getResponseBodyAsStream();
        }

        public int available() throws IOException {
            return _is.available();
        }

        public void close() throws IOException {
            _is.close();
            _get.releaseConnection();
        }

        public boolean equals(Object obj) {
            return _is.equals(obj);
        }

        public int hashCode() {
            return _is.hashCode();
        }

        public void mark(int readlimit) {
            _is.mark(readlimit);
        }

        public boolean markSupported() {
            return _is.markSupported();
        }

        public int read() throws IOException {
            return _is.read();
        }

        public int read(byte[] b, int off, int len) throws IOException {
            return _is.read(b, off, len);
        }

        public int read(byte[] b) throws IOException {
            return _is.read(b);
        }

        public void reset() throws IOException {
            _is.reset();
        }

        public long skip(long n) throws IOException {
            return _is.skip(n);
        }

        public String toString() {
            return _is.toString();
        }
    }
    private static final class HttpClientHelper3x implements HttpClientHelper {
        private HttpClientHelper3x() {
        }

        public long getResponseContentLength(HeadMethod head) {
            return head.getResponseContentLength();
        }

		/**
		 * {@inheritDoc}
		 */
		public int getHttpClientMajorVersion() {
			return 3;
		}
    }
    private static final class HttpClientHelper2x implements HttpClientHelper {
        private HttpClientHelper2x() {
        }

        public long getResponseContentLength(HeadMethod head) {
            Header header = head.getResponseHeader("Content-Length");
            if (header != null) {
                try {
                    return Integer.parseInt(header.getValue());
                } catch (NumberFormatException e) {
                    Message.verbose("Invalid content-length value: " + e.getMessage());
                }
            }
            return 0;
        }

		/**
		 * {@inheritDoc}
		 */
		public int getHttpClientMajorVersion() {
			return 2;
		}
    }
    public interface HttpClientHelper {
        long getResponseContentLength(HeadMethod head);
        int getHttpClientMajorVersion();
    }
}
