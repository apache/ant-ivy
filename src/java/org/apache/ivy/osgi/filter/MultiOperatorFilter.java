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
package org.apache.ivy.osgi.filter;

import java.util.ArrayList;
import java.util.List;

public abstract class MultiOperatorFilter extends OSGiFilter {

    private List<OSGiFilter> subFilters = new ArrayList<OSGiFilter>();

    public MultiOperatorFilter() {
        // default constructor
    }

    public MultiOperatorFilter(OSGiFilter[] filters) {
        for (int i = 0; i < filters.length; i++) {
            OSGiFilter filter = filters[i];
            add(filter);
        }
    }

    abstract protected char operator();

    @Override
    public void append(StringBuffer builder) {
        builder.append('(');
        builder.append(operator());
        for (OSGiFilter filter : subFilters) {
            filter.append(builder);
        }
        builder.append(')');
    }

    public void add(OSGiFilter subFilter2) {
        subFilters.add(subFilter2);
    }

    public List<OSGiFilter> getSubFilters() {
        return subFilters;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        for (OSGiFilter subFilter : subFilters) {
            result = prime * result + ((subFilter == null) ? 0 : subFilter.hashCode());
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof MultiOperatorFilter)) {
            return false;
        }
        MultiOperatorFilter other = (MultiOperatorFilter) obj;
        if (subFilters == null) {
            if (other.subFilters != null) {
                return false;
            }
        } else if (other.subFilters == null) {
            return false;
        } else if (subFilters.size() != other.subFilters.size()) {
            return false;
        } else if (!subFilters.containsAll(other.subFilters)) {
            return false;
        }
        return true;
    }
}
