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

import fr.jayasoft.ivy.util.CopyProgressListener;

/**
 * This interface is responsible for handling some URL manipulation
 * (stream opening, downloading, check reachability, ...). 
 * 
 * @author Xavier Hanin
 *
 */
public interface URLHandler {
    public static class URLInfo {
        private long _contentLength;
        private long _lastModified;
        private boolean _available;
        
        protected URLInfo(boolean available, long contentLength, long lastModified) {
            _available = available;
            _contentLength = contentLength;
            _lastModified = lastModified;
        }
        public boolean isReachable() {
            return _available;
        }
        public long getContentLength() {
            return _contentLength;
        }
        public long getLastModified() {
            return _lastModified;
        }
    }
    public static final URLInfo UNAVAILABLE = new URLInfo(false, 0,0);
    
    /**
     * Please prefer getURLInfo when several infos are needed.
     * @param url the url to check
     * @return true if the target is reachable
     */
    public boolean isReachable(URL url);
    /**
     * Please prefer getURLInfo when several infos are needed.
     * @param url the url to check
     * @return true if the target is reachable
     */
    public boolean isReachable(URL url, int timeout);
    /**
     * Returns the length of the target if the given url is reachable, and without
     * error code in case of http urls.
     * Please prefer getURLInfo when several infos are needed.
     * @param url the url to check
     * @return the length of the target if available, 0 if not reachable
     */
    public long getContentLength(URL url);
    /**
     * Returns the length of the target if the given url is reachable, and without
     * error code in case of http urls.
     * @param url the url to check
     * @param timeout the maximum time before considering an url is not reachable
     *        a timeout of zero indicates no timeout
     * @return the length of the target if available, 0 if not reachable
     */
    public long getContentLength(URL url, int timeout);
    
    /**
     * Please prefer getURLInfo when several infos are needed.
     * @param url the url to check
     * @return last modified timestamp of the given url
     */
    public long getLastModified(URL url);
    /**
     * Please prefer getURLInfo when several infos are needed.
     * @param url the url to check
     * @return last modified timestamp of the given url
     */
    public long getLastModified(URL url, int timeout);
    
    /**
     * never returns null, return UNAVAILABLE when url is not reachable
     * @param url
     * @return
     */
    public URLInfo getURLInfo(URL url);
    /**
     * never returns null, return UNAVAILABLE when url is not reachable
     * @param url
     * @return
     */
    public URLInfo getURLInfo(URL url, int timeout);
    
    public InputStream openStream(URL url) throws IOException;
    public void download(URL src, File dest, CopyProgressListener l) throws IOException;
}
