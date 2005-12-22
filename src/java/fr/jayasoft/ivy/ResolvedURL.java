/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import fr.jayasoft.ivy.util.Message;

public class ResolvedURL implements ArtifactInfo {
    URL url;
    String revision;
    private Long _lastModified;
    
    public ResolvedURL(URL url, String revision) {
        this.url = url;
        this.revision = revision;
    }
    
    public String toString() {
        return url + "(" + revision + ")";
    }

    public String getRevision() {
        return revision;
    }

    public long getLastModified() {
        if (_lastModified == null) {
            URLConnection con = null;
            try {
                con = url.openConnection();
                _lastModified = new Long(con.getLastModified());
            } catch (IOException e) {
                Message.warn("impossible to open connection to "+url+": "+e.getMessage());
                _lastModified = new Long(0);
            } finally {
                if (con instanceof HttpURLConnection) {
                    ((HttpURLConnection)con).disconnect();
                }
            }
        }
        return _lastModified.longValue();
    }

    public URL getURL() {
        return url;
    }
}
