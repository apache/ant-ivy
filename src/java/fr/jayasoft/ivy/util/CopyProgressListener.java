/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.util;

/**
 * Listen to copy progression
 */
public interface CopyProgressListener {
    void start(CopyProgressEvent evt);
    void progress(CopyProgressEvent evt);
    void end(CopyProgressEvent evt);
}
