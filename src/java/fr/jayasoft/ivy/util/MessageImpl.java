/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.util;

public interface MessageImpl {
    public void log(String msg, int level);
    public void rawlog(String msg, int level);
    public void progress();
    public void endProgress(String msg);
}
