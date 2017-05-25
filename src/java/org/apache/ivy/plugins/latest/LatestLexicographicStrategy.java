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

import java.util.Comparator;

public class LatestLexicographicStrategy extends ComparatorLatestStrategy {
    /**
     * Compares two revisions. Revisions are compared lexicographically unless a 'latest' revision
     * is found. If the latest revision found is an absolute latest (latest. like), then it is
     * assumed to be the greater. If a partial latest is found, then it is assumed to be greater
     * than any matching fixed revision.
     */
    private static final Comparator<ArtifactInfo> COMPARATOR = new Comparator<ArtifactInfo>() {
        public int compare(ArtifactInfo o1, ArtifactInfo o2) {
            String rev1 = o1.getRevision();
            String rev2 = o2.getRevision();
            if (rev1.startsWith("latest")) {
                return 1;
            }
            if (rev1.endsWith("+") && rev2.startsWith(rev1.substring(0, rev1.length() - 1))) {
                return 1;
            }
            if (rev2.startsWith("latest")) {
                return -1;
            }
            if (rev2.endsWith("+") && rev1.startsWith(rev2.substring(0, rev2.length() - 1))) {
                return -1;
            }
            return rev1.compareTo(rev2);
        }

    };

    public LatestLexicographicStrategy() {
        super(COMPARATOR);
        setName("latest-lexico");
    }

}
