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
package org.apache.ivy.util.filter;

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
