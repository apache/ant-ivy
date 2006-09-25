/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.util;

public class DefaultMessageImpl implements MessageImpl {
    private int _level = Message.MSG_INFO;

    /**
     * @param level
     */
    public DefaultMessageImpl(int level) {
        _level = level;
    }

    public void log(String msg, int level) {
        if (level <= _level) {
            System.out.println(msg);
        }        
    }
    
    public void rawlog(String msg, int level) {
    	log(msg, level);
    }

    public void progress() {
        System.out.print(".");
    }

    public void endProgress(String msg) {
        System.out.println(msg);
    }

    public int getLevel() {
        return _level;
    }
}
