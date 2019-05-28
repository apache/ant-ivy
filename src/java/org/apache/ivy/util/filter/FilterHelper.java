/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
        List<String> acceptedTypes = new ArrayList<>(types.length);
        for (String type : types) {
            String current = type.trim();
            if ("*".equals(current)) {
                return NO_FILTER;
            }
            acceptedTypes.add(current);
        }
        return new ArtifactTypeFilter(acceptedTypes);
    }

    /**
     * @param <T> The type parameter
     * @param col
     *            The collection to filter.
     * @param filter
     *            The filter to use.
     * @return a new collection instance containing the only the the items from the given
     *         collection, which are accepted by the filter.
     *
     * <p>
     * Comment: We could have used
     * <a href="https://jakarta.apache.org/commons/collections/">Commons Collections</a> facility for
     * this, if we accepted additional dependencies on third party jars.
     * </p>
     */
    public static <T> Collection<T> filter(Collection<T> col, Filter<T> filter) {
        if (filter == null) {
            return col;
        }
        Collection<T> ret = new ArrayList<>(col);
        Iterator<T> iter = ret.iterator();
        while (iter.hasNext()) {
            if (!filter.accept(iter.next())) {
                iter.remove();
            }
        }
        return ret;
    }
}
