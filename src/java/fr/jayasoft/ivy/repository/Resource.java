/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.repository;

import java.io.IOException;
import java.io.InputStream;

/**
 * Represents a resource in an Ivy {@link Repository}.
 * 
 * The resource interface allows one to obtain the following information about a resource:
 * <ul>
 *   <li>resource name/identifier in repository syntax</li>
 *   <li>date the resource was last modified.</li>
 *   <li>size of the resource in bytes.</li>
 *   <li>if the resource is available.</li>
 *</ul>
 *</p>
 *<h4>Implementation Notes</h4>
 *   In implementing the interface you need to ensure the following behaviors:
 *   <ul>
 *     <li>All of the methods specified in the interface fail by returning an empty value
 *         (<code>false</code>, <code>0</code>, <code>""</code>). In other words, the specified interface 
 *         methods should not throw RuntimeExceptions.
 *     </li>
 *     <li>Failure conditions should be logged using the
 *     {@link fr.jayasoft.ivy.util.Message#verbose} method.
 *     </li>
 *     <li>Failure of one of the interface's specified methods results in all other interface specified
 *     methods returning an empty value  (<code>false</code>, <code>0</code>, <code>""</code>).</li>
 *   </ul>
 *   </p>
 */

public interface Resource {
	/**
	 * Get the name of the resource.
	 * 
	 * @return the repositorie's assigned resource name/identifier.
	 */
    public String getName();
    
    /**
     * Get the date the resource was last modified
     * 
     * @return A <code>long</code> value representing the time the file was
     *          last modified, measured in milliseconds since the epoch
     *          (00:00:00 GMT, January 1, 1970), or <code>0L</code> if the
     *          file does not exist or if an I/O error occurs.
     */
    public long getLastModified();
    
    /**
     * Get the resource size
     * 
     * @return a <code>long</code> value representing the size of the resource in bytes.
     */
    public long getContentLength();
    
    /**
     * Determine if the resource is available.
     * </p>
     * Note that this method only checks for availability, not for actual existence.
     * 
     * @return <code>boolean</code> value indicating if the resource is available.
     */
    public boolean exists();
    
    /**
     * Is this resource local to this host, i.e. is it on the file system?
     *
     * @return <code>boolean</code> value indicating if the resource is local.
     */
    public boolean isLocal();

    /**
     * Clones this resource with a new resource with a different name
     * @param cloneName the name of the clone
     * @return the cloned resource
     */
	public Resource clone(String cloneName);
	
	/**
	 * Opens a stream on this resource
	 * @return the opened input stream
	 */
	public InputStream openStream() throws IOException;
}
