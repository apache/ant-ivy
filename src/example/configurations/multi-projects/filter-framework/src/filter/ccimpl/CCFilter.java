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
package filter.ccimpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import filter.IFilter;

public class CCFilter implements IFilter {
    
    public String[] filter(String[] values, final String prefix) {
        if (values == null) {
            return null;
        }
        if (prefix == null) {
            return values;
        }

        List result = new ArrayList(Arrays.asList(values));
        CollectionUtils.filter(result, new Predicate() {
            public boolean evaluate(Object o) {
                return o != null && o.toString().startsWith(prefix);
            }
        });
        return (String[]) result.toArray(new String[result.size()]);
    }
}
