/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.ant;

import org.apache.tools.ant.Project;

import fr.jayasoft.ivy.util.MessageImpl;

/**
 * Implementation of the simple message facility for ant.
 * 
 * @author Xavier Hanin
 */
public class AntMessageImpl implements MessageImpl {
    private Project _project;

    private static long _lastProgressFlush = 0;
    private static StringBuffer _buf = new StringBuffer();

    /**
     * @param project
     */
    public AntMessageImpl(Project project) {
        _project = project;
    }

    public void log(String msg, int level) {
        _project.log(msg, level);
    }

    public void progress() {
        _buf.append(".");
        if (_lastProgressFlush == 0) {
            _lastProgressFlush = System.currentTimeMillis();
        }
        if (_project != null) {
            // log with ant causes a new line -> we do it only once in a while
            if (System.currentTimeMillis() - _lastProgressFlush > 1500) {
                _project.log(_buf.toString());
                _buf.setLength(0);
                _lastProgressFlush = System.currentTimeMillis();
            }
        }
    }
    
    public void endProgress(String msg) {
        _project.log(_buf + msg);
        _buf.setLength(0);
        _lastProgressFlush = 0;
    }
}
