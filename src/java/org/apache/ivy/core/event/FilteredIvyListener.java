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
package org.apache.ivy.core.event;

import org.apache.ivy.util.filter.Filter;

public class FilteredIvyListener implements IvyListener {
    private IvyListener listener;

    private Filter filter;

    public FilteredIvyListener(IvyListener listener, Filter filter) {
        this.listener = listener;
        this.filter = filter;
    }

    public IvyListener getIvyListener() {
        return listener;
    }

    public Filter getFilter() {
        return filter;
    }

    public void progress(IvyEvent event) {
        if (filter.accept(event)) {
            listener.progress(event);
        }
    }

}
