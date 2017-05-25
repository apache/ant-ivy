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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ComparatorLatestStrategy extends AbstractLatestStrategy {

    private Comparator<ArtifactInfo> comparator;

    public ComparatorLatestStrategy() {
    }

    public ComparatorLatestStrategy(Comparator<ArtifactInfo> comparator) {
        this.comparator = comparator;
    }

    public List<ArtifactInfo> sort(ArtifactInfo[] infos) {
        List<ArtifactInfo> ret = new ArrayList<ArtifactInfo>(Arrays.asList(infos));
        Collections.sort(ret, comparator);
        return ret;
    }

    public Comparator<ArtifactInfo> getComparator() {
        return comparator;
    }

    public void setComparator(Comparator<ArtifactInfo> comparator) {
        this.comparator = comparator;
    }

}
