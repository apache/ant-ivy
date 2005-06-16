/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.repository;

public interface Resource {
    public String getName();
    public long getLastModified();
    public long getContentLength();
    public boolean exists();
}
