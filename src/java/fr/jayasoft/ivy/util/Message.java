/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author x.hanin
 *
 */
public class Message {
    // messages level copied from ant project, to avoid dependency on ant
    /** Message priority of "error". */
    public static final int MSG_ERR = 0;
    /** Message priority of "warning". */
    public static final int MSG_WARN = 1;
    /** Message priority of "information". */
    public static final int MSG_INFO = 2;
    /** Message priority of "verbose". */
    public static final int MSG_VERBOSE = 3;
    /** Message priority of "debug". */
    public static final int MSG_DEBUG = 4;

    private static MessageImpl _impl = null;

    private static StringBuffer _problems = new StringBuffer();
    
    private static boolean _showProgress = true;
    
    private static boolean _showedInfo = false;
    
    public static void init(MessageImpl impl) {
        _impl = impl;
        showInfo();
    }

    /** 
     * same as init, but without displaying info
     * @param impl
     */
    public static void setImpl(MessageImpl impl) {
        _impl = impl;
    }
    
    public static MessageImpl getImpl() {
    	return _impl;
    }
    
    public static boolean isInitialised() {
        return _impl != null;
    }

    private static void showInfo() {
        if (!_showedInfo ) {
            Properties props = new Properties();
            InputStream module = Message.class.getResourceAsStream("/module.properties");
            if (module != null) {
                try {
                    props.load(module);
                    info(":: Ivy "+props.getProperty("version")+" - "+props.getProperty("date")+" :: http://ivy.jayasoft.org/ ::");
                } catch (IOException e) {
                    info(":: Ivy non official version :: http://ivy.jayasoft.org/ ::");
                }
            } else {
                info(":: Ivy non official version :: http://ivy.jayasoft.org/ ::");
            }
            _showedInfo = true;
        }
    }

    public static void debug(String msg) {
        if (_impl != null) {
            _impl.log(msg, MSG_DEBUG);
        } else {
            System.err.println(msg);
        }
    }
    public static void verbose(String msg) {
        if (_impl != null) {
            _impl.log(msg, MSG_VERBOSE);
        } else {
            System.err.println(msg);
        }
    }
    public static void info(String msg) {
        if (_impl != null) {
            _impl.log(msg, MSG_INFO);
        } else {
            System.err.println(msg);
        }
    }
    public static void warn(String msg) {
        if (_impl != null) {
            _impl.log("WARN: "+msg, MSG_VERBOSE);
        } else {
            System.err.println(msg);
        }
        _problems.append("\tWARN:  "+msg).append("\n");
    }
    public static void error(String msg) {
        if (_impl != null) {
            // log in verbose mode because message is appended as a problem, and will be 
            // logged at the end at error level
            _impl.log("ERROR: "+msg, MSG_VERBOSE);
        } else {
            System.err.println(msg);
        }
        _problems.append("\tERROR: "+msg).append("\n");
    }
    public static void sumupProblems() {
        if (_problems.length() > 0) {
            info("\n:: problems summary ::");
            info(_problems.toString());
            info("\t--- USE VERBOSE OR DEBUG MESSAGE LEVEL FOR MORE DETAILS ---");
            _problems = new StringBuffer();
        }
    }

    public static void progress() {
        if (_showProgress) {
            if (_impl != null) {
                _impl.progress();
            } else {
                System.out.println(".");
            }
        }
    }

    public static void endProgress() {
        endProgress("");
    }

    public static void endProgress(String msg) {
        if (_showProgress) {
            if (_impl != null) {
                _impl.endProgress(msg);
            }
        }
    }

    public static boolean isShowProgress() {
        return _showProgress;
    }
    public static void setShowProgress(boolean progress) {
        _showProgress = progress;
    }

    public static void uninit() {
        _impl = null;
    }
}
