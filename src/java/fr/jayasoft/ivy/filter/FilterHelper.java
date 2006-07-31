/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class FilterHelper {
    public static Filter NO_FILTER = NoFilter.INSTANCE;
    
    public static Filter getArtifactTypeFilter(String types) {
        if (types == null || types.trim().equals("*")) {
            return NO_FILTER;
        }
        String[] t = types.split(",");
        List acceptedTypes = new ArrayList(types.length());
        for (int i = 0; i < t.length; i++) {
            acceptedTypes.add(t[i].trim());
        }
        return new ArtifactTypeFilter(acceptedTypes);
    }

    /**
     * we could have used commons-collections facility for this...
     * if we accepted to add dependencies on third party jars
     * @param col
     * @param filter
     * @return
     */
    public static Collection filter(Collection col, Filter filter) {
        if (filter == null) {
            return col;
        }
        Collection ret = new ArrayList(col);
        for (Iterator iter = ret.iterator(); iter.hasNext();) {
            Object element = (Object)iter.next();
            if (!filter.accept(element)) {
                iter.remove();
            }
        }
        return ret;
    }
}
