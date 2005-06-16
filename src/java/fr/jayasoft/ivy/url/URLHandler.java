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
    /**
     * Returns true if the given url is reachable, and without
     * error code in case of http urls.
     * @param url the url to check
     * @return true if the given url is reachable
     */
    public boolean isReachable(URL url);
    /**
     * Returns true if the given url is reachable, and without
     * error code in case of http urls.
     * @param url the url to check
     * @param timeout the maximum time before considering an url is not reachable
     *        a timeout of zero indicates no timeout
     * @return true if the given url is reachable
     */
    public boolean isReachable(URL url, int timeout);
    public InputStream openStream(URL url) throws IOException;
    public void download(URL src, File dest, CopyProgressListener l) throws IOException;
}
