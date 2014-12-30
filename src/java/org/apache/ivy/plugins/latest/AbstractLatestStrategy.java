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
package org.apache.ivy.plugins.latest;

import java.util.Date;
import java.util.List;
import java.util.ListIterator;

public abstract class AbstractLatestStrategy implements LatestStrategy {
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public ArtifactInfo findLatest(ArtifactInfo[] infos, Date date) {
        List<ArtifactInfo> l = sort(infos);

        // the latest revision comes last, use a ListIterator to iterate the
        // sorted list in the reverse direction.
        for (ListIterator<ArtifactInfo> iter = l.listIterator(l.size()); iter.hasPrevious();) {
            ArtifactInfo info = iter.previous();
            if (date == null || info.getLastModified() < date.getTime()) {
                return info;
            }
        }
        return null;
    }
}
