/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.filter;

public class NoFilter implements Filter {
	public static final Filter INSTANCE = new NoFilter();
	
	private NoFilter() {
	}
	
    public boolean accept(Object o) {
        return true;
    }

}
