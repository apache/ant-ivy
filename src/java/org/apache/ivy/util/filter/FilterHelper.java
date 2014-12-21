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

import org.apache.ivy.core.module.descriptor.Artifact;

public final class FilterHelper {
    private FilterHelper() {
    }

    public static final Filter<Artifact> NO_FILTER = NoFilter.instance();

    public static Filter<Artifact> getArtifactTypeFilter(String types) {
        if (types == null || types.trim().equals("*")) {
            return NO_FILTER;
        }
        String[] t = types.split(",");
        return getArtifactTypeFilter(t);
    }

    public static Filter<Artifact> getArtifactTypeFilter(String[] types) {
        if (types == null || types.length == 0) {
            return NO_FILTER;
        }
        List<String> acceptedTypes = new ArrayList<String>(types.length);
        for (int i = 0; i < types.length; i++) {
            String current = types[i].trim();
            if ("*".equals(current)) {
                return NO_FILTER;
            }
            acceptedTypes.add(current);
        }
        return new ArtifactTypeFilter(acceptedTypes);
    }

    /**
     * Returns a new collection containing only the items from the given collectoin, which are
     * accepted by the filter.
     * 
     * @param col
     *            The collection to filter.
     * @param filter
     *            The filter to use.
     * @return A new collection instance containing the only the instance accepted by the filter.
     * 
     * <br />
     *         Comment: We could have used <a
     *         href="http://jakarta.apache.org/commons/collections/">Commons-Collections</a>
     *         facility for this. If we accepted to add dependencies on third party jars.
     */
    public static <T> Collection<T> filter(Collection<T> col, Filter<T> filter) {
        if (filter == null) {
            return col;
        }
        Collection<T> ret = new ArrayList<T>(col);
        for (Iterator<T> iter = ret.iterator(); iter.hasNext();) {
            T element = iter.next();
            if (!filter.accept(element)) {
                iter.remove();
            }
        }
        return ret;
    }
}
