/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.status;

public class Status {
    private String _name;
    private boolean _integration;
    
    public Status() {
    }
    
    public Status(String name, boolean integration) {
        _name = name;
        _integration = integration;
    }
    public boolean isIntegration() {
        return _integration;
    }
    public void setIntegration(boolean integration) {
        _integration = integration;
    }
    public String getName() {
        return _name;
    }
    public void setName(String name) {
        _name = name;
    }
}
