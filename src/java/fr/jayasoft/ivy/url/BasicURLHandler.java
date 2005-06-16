/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.url;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;

import org.apache.commons.httpclient.HttpStatus;

import fr.jayasoft.ivy.util.CopyProgressListener;
import fr.jayasoft.ivy.util.FileUtil;
import fr.jayasoft.ivy.util.Message;

/**
 * @author Xavier Hanin
 *
 */
public class BasicURLHandler implements URLHandler {
    public boolean isReachable(URL url) {
        return isReachable(url, 0);
    }
    public boolean isReachable(URL url, int timeout) {
        try {
            URLConnection con = url.openConnection();
            if (con instanceof HttpURLConnection) {
                int status = ((HttpURLConnection)con).getResponseCode();
                if (status == HttpStatus.SC_OK) {
                    return true;
                }
                if (status == HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED) {
                    Message.warn("Your proxy requires authentication.");
                }else if (String.valueOf(status).startsWith("4")) {
                    Message.verbose("CLIENT ERROR: "+((HttpURLConnection)con).getResponseMessage()+" url="+url);
                }else if (String.valueOf(status).startsWith("5")) {
                    Message.error("SERVER ERROR: "+((HttpURLConnection)con).getResponseMessage()+" url="+url);
                }
                Message.debug("HTTP response status: "+status+" url="+url);
            } else {
                int contentLength = con.getContentLength();
                return contentLength > 0;
            }
        } catch (UnknownHostException e) {
            Message.warn("Host " + e.getMessage() +" not found. url="+url);
            Message.info("You probably access the destination server through a proxy server that is not well configured.");
        } catch (IOException e) {
            Message.error("Server access Error: "+e.getMessage()+" url="+url);
        }
        return false;
    }
    
    public InputStream openStream(URL url) throws IOException {
        URLConnection conn = null;
        InputStream inStream = null;
        try {
            conn = url.openConnection();
            inStream = conn.getInputStream();
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            
            byte[] buffer = new byte[4096];
            int len;
            while ((len = inStream.read(buffer)) > 0) {
                outStream.write(buffer, 0, len);
            }
            return new ByteArrayInputStream(outStream.toByteArray());
        }
        finally {
            if (inStream != null) {
                inStream.close();
            }
            
            if (conn != null) {
                if (conn instanceof HttpURLConnection) {
                    //System.out.println("Closing HttpURLConnection");
                    ((HttpURLConnection) conn).disconnect();
                }
            }
        }
    }
    public void download(URL src, File dest, CopyProgressListener l) throws IOException {
        URLConnection srcConn = null;
        try {
            srcConn = src.openConnection();
            FileUtil.copy(srcConn.getInputStream(), dest, l);
        }
        finally {
            if (srcConn != null) {
                if (srcConn instanceof HttpURLConnection) {
                    //System.out.println("Closing HttpURLConnection");
                    ((HttpURLConnection) srcConn).disconnect();
                }
            }
        }
    }
}
