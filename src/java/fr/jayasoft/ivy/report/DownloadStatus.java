/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.report;

/**
 * @author x.hanin
 *
 */
public class DownloadStatus {
    private String _name;
    private DownloadStatus(String name) {
        _name = name;
    }
    
    /**
     * means that download was not required
     */
    public static final DownloadStatus NO = new DownloadStatus("no");
    public static final DownloadStatus SUCCESSFUL = new DownloadStatus("successful");
    public static final DownloadStatus FAILED = new DownloadStatus("failed");
    public String toString() {
        return _name;
    }
}
