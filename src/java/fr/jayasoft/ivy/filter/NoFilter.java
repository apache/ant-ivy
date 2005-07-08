/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.filter;

class NoFilter implements Filter {
    public boolean accept(Object o) {
        return true;
    }

}
