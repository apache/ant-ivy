/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.repository;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface Repository {
    Resource getResource(String source)  throws IOException;
    void get(String source, File destination) throws IOException;
    void put(File source, String destination, boolean overwrite) throws IOException;
    /**
     * Returns the list of all resources names that can be found in the given
     * parent.
     * @param parent
     * @return a List of String, the names of the resources that can be found in the given
     * parent
     */
    List list(String parent) throws IOException;
    
    void addTransferListener(TransferListener listener);
    void removeTransferListener(TransferListener listener);
    boolean hasTransferListener(TransferListener listener);
    String getFileSeparator();
    String standardize(String source);
    String getName();
}
