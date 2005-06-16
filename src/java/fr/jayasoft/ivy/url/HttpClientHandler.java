/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.url;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.UnknownHostException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;

import fr.jayasoft.ivy.util.CopyProgressListener;
import fr.jayasoft.ivy.util.FileUtil;
import fr.jayasoft.ivy.util.Message;

/**
 * @author Xavier Hanin
 *
 */
public class HttpClientHandler implements URLHandler {
    private String _realm = null;
    private String _host = null;
    private String _userName = null;
    private String _passwd = null;
    
    // proxy configuration: obtain from system properties
    private int _proxyPort;

    private String _proxyRealm = null;
    private String _proxyHost = null;
    private String _proxyUserName = null;
    private String _proxyPasswd = null;
    
    public HttpClientHandler() {
        configureProxy();
    }
    
    /**
     * @param realm may be null
     * @param host may be null
     * @param userName may be null
     * @param passwd may be null
     */
    public HttpClientHandler(String realm, String host, String userName, String passwd) {
        _realm = realm;
        _host = host;
        _userName = userName;
        _passwd = passwd;
        if (useAuthentication()) {
            Message.verbose("using authentication in realm "+_realm+" and host "+_host+" for user "+_userName);
        } else {
            Message.verbose("no http authentication will be used");
        }
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
        byte[] response = get.getResponseBody();
        get.releaseConnection();
        
        return new ByteArrayInputStream(response);
    }
    
    public void download(URL src, File dest, CopyProgressListener l) throws IOException {
        GetMethod get = doGet(src);
        FileUtil.copy(get.getResponseBodyAsStream(), dest, l);
        get.releaseConnection();
    }
    
    public boolean isReachable(URL url) {
        return isReachable(url, 0);
    }
    
    public boolean isReachable(URL url, int timeout) {
        try {
            HeadMethod head = doHead(url, timeout);
            int status = head.getStatusCode();
            head.releaseConnection();
            if (status == HttpStatus.SC_OK) {
                return true;
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
        }
        return false;
    }    

    private GetMethod doGet(URL url) throws IOException, HttpException {
        HttpClient client = getClient();

        GetMethod get = new GetMethod(url.toExternalForm());
        get.setDoAuthentication(useAuthentication() || useProxyAuthentication());
        client.executeMethod(get);
        return get;
    }

    private HeadMethod doHead(URL url, int timeout) throws IOException, HttpException {
        HttpClient client = getClient();
        client.setTimeout(timeout);

        HeadMethod head = new HeadMethod(url.toExternalForm());
        head.setDoAuthentication(useAuthentication() || useProxyAuthentication());
        client.executeMethod(head);
        return head;
    }

    private HttpClient getClient() {
        HttpClient client = new HttpClient();
        if (useProxy()) {
            client.getHostConfiguration().setProxy(_proxyHost, _proxyPort);
            if (useProxyAuthentication()) {
                client.getState().setProxyCredentials(_proxyRealm, _proxyHost,
                    new UsernamePasswordCredentials(_proxyUserName, _proxyPasswd));
            }
        }
        if (useAuthentication()) {
	        client.getState().setCredentials(
	            _realm,
	            _host,
	            new UsernamePasswordCredentials(_userName, _passwd)
	        );
        }
        return client;
    }

    private boolean useProxy() {
        return _proxyHost != null && _proxyHost.trim().length() > 0;
    }
    private boolean useAuthentication() {
        return (_userName != null && _userName.trim().length() > 0);
    }
    private boolean useProxyAuthentication() {
        return (_proxyUserName != null && _proxyUserName.trim().length() > 0);
    }
}
