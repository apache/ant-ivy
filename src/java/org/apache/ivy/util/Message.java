/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivy.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.ivy.core.IvyContext;

/**
 * Logging utility class.
 * <p>
 * To initialize Message you can call {@link #init(MessageImpl)} with
 * the {@link MessageImpl} of your choice.
 * <p> 
 * This only takes effect in the current thread.
 */
public final class Message {
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

    private static List problems = new ArrayList();

    private static List warns = new ArrayList();

    private static List errors = new ArrayList();

    private static boolean showProgress = true;

    private static boolean showedInfo = false;

    public static void init(MessageImpl impl) {
        IvyContext.getContext().setMessageImpl(impl);
        showInfo();
    }

    /**
     * same as init, but without displaying info
     * 
     * @param impl
     */
    public static void setImpl(MessageImpl impl) {
        IvyContext.getContext().setMessageImpl(impl);
    }

    public static MessageImpl getImpl() {
        return IvyContext.getContext().getMessageImpl();
    }

    public static boolean isInitialised() {
        return IvyContext.getContext().getMessageImpl() != null;
    }

    private static void showInfo() {
        if (!showedInfo) {
            Properties props = new Properties();
            URL moduleURL = Message.class.getResource("/module.properties");
            if (moduleURL != null) {
                try {
                    InputStream module = moduleURL.openStream();
                    props.load(module);
                    debug("version information loaded from " + moduleURL);
                    info(":: Ivy " + props.getProperty("version") + " - "
                           + props.getProperty("date") + " :: http://incubator.apache.org/ivy/ ::");
                    module.close();
                } catch (IOException e) {
                    info(":: Ivy non official version :: http://incubator.apache.org/ivy/ ::");
                }
            } else {
                info(":: Ivy non official version :: http://incubator.apache.org/ivy/ ::");
            }
            showedInfo = true;
        }
    }

    public static void debug(String msg) {
        MessageImpl messageImpl = IvyContext.getContext().getMessageImpl();
        if (messageImpl != null) {
            messageImpl.log(msg, MSG_DEBUG);
        } else {
            System.err.println(msg);
        }
    }

    public static void verbose(String msg) {
        MessageImpl messageImpl = IvyContext.getContext().getMessageImpl();
        if (messageImpl != null) {
            messageImpl.log(msg, MSG_VERBOSE);
        } else {
            System.err.println(msg);
        }
    }

    public static void info(String msg) {
        MessageImpl messageImpl = IvyContext.getContext().getMessageImpl();
        if (messageImpl != null) {
            messageImpl.log(msg, MSG_INFO);
        } else {
            System.err.println(msg);
        }
    }

    public static void rawinfo(String msg) {
        MessageImpl messageImpl = IvyContext.getContext().getMessageImpl();
        if (messageImpl != null) {
            messageImpl.rawlog(msg, MSG_INFO);
        } else {
            System.err.println(msg);
        }
    }

    public static void deprecated(String msg) {
        MessageImpl messageImpl = IvyContext.getContext().getMessageImpl();
        if (messageImpl != null) {
            messageImpl.log("DEPRECATED: " + msg, MSG_WARN);
        } else {
            System.err.println(msg);
        }
    }

    public static void warn(String msg) {
        MessageImpl messageImpl = IvyContext.getContext().getMessageImpl();
        if (messageImpl != null) {
            messageImpl.log("WARN: " + msg, MSG_VERBOSE);
        } else {
            System.err.println(msg);
        }
        problems.add("WARN:  " + msg);
        warns.add(msg);
    }

    public static void error(String msg) {
        MessageImpl messageImpl = IvyContext.getContext().getMessageImpl();
        if (messageImpl != null) {
            // log in verbose mode because message is appended as a problem, and will be
            // logged at the end at error level
            messageImpl.log("ERROR: " + msg, MSG_VERBOSE);
        } else {
            System.err.println(msg);
        }
        problems.add("\tERROR: " + msg);
        errors.add(msg);
    }

    public static List getProblems() {
        return problems;
    }

    public static void sumupProblems() {
        if (problems.size() > 0) {
            info("\n:: problems summary ::");
            MessageImpl messageImpl = IvyContext.getContext().getMessageImpl();
            if (warns.size() > 0) {
                info(":::: WARNINGS");
                for (Iterator iter = warns.iterator(); iter.hasNext();) {
                    String msg = (String) iter.next();
                    if (messageImpl != null) {
                        messageImpl.log("\t" + msg + "\n", MSG_WARN);
                    } else {
                        System.err.println(msg);
                    }
                }
            }
            if (errors.size() > 0) {
                info(":::: ERRORS");
                for (Iterator iter = errors.iterator(); iter.hasNext();) {
                    String msg = (String) iter.next();
                    if (messageImpl != null) {
                        messageImpl.log("\t" + msg + "\n", MSG_ERR);
                    } else {
                        System.err.println(msg);
                    }
                }
            }
            info("\n:: USE VERBOSE OR DEBUG MESSAGE LEVEL FOR MORE DETAILS");
            problems.clear();
            warns.clear();
            errors.clear();
        }
    }

    public static void progress() {
        if (showProgress) {
            MessageImpl messageImpl = IvyContext.getContext().getMessageImpl();
            if (messageImpl != null) {
                messageImpl.progress();
            } else {
                System.out.println(".");
            }
        }
    }

    public static void endProgress() {
        endProgress("");
    }

    public static void endProgress(String msg) {
        if (showProgress) {
            MessageImpl messageImpl = IvyContext.getContext().getMessageImpl();
            if (messageImpl != null) {
                messageImpl.endProgress(msg);
            }
        }
    }

    public static boolean isShowProgress() {
        return showProgress;
    }

    public static void setShowProgress(boolean progress) {
        showProgress = progress;
    }

    public static void uninit() {
        IvyContext.getContext().setMessageImpl(null);
    }
    
    private Message() {
    }
}
