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
package org.apache.ivy.osgi.core;

import java.text.ParseException;
import java.util.Comparator;

import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.osgi.util.Version;
import org.apache.ivy.plugins.latest.ArtifactInfo;
import org.apache.ivy.plugins.latest.ComparatorLatestStrategy;
import org.apache.ivy.plugins.version.VersionMatcher;

public class OsgiLatestStrategy extends ComparatorLatestStrategy {

    final class MridComparator implements Comparator<ModuleRevisionId> {

        public int compare(ModuleRevisionId o1, ModuleRevisionId o2) {
            Version v1;
            Version v2;
            try {
                v1 = new Version(o1.getRevision());
                v2 = new Version(o2.getRevision());
            } catch (ParseException e) {
                throw new RuntimeException("Uncomparable versions:" + o1.getRevision() + " and "
                        + o2.getRevision() + " (" + e.getMessage() + ")");
            }
            try {
                return v1.compareTo(v2);
            } catch (RuntimeException e) {
                if (e.getCause() instanceof ParseException) {
                    throw new RuntimeException("Uncomparable versions:" + o1.getRevision()
                            + " and " + o2.getRevision() + " (" + e.getMessage() + ")");
                }
                throw e;
            }
        }

    }

    final class ArtifactInfoComparator implements Comparator<ArtifactInfo> {

        public int compare(ArtifactInfo o1, ArtifactInfo o2) {
            String rev1 = o1.getRevision();
            String rev2 = o2.getRevision();

            /*
             * The revisions can still be not resolved, so we use the current version matcher to
             * know if one revision is dynamic, and in this case if it should be considered greater
             * or lower than the other one. Note that if the version matcher compare method returns
             * 0, it's because it's not possible to know which revision is greater. In this case we
             * consider the dynamic one to be greater, because most of the time it will then be
             * actually resolved and a real comparison will occur.
             */
            VersionMatcher vmatcher = IvyContext.getContext().getSettings().getVersionMatcher();
            ModuleRevisionId mrid1 = ModuleRevisionId.newInstance("", "", rev1);
            ModuleRevisionId mrid2 = ModuleRevisionId.newInstance("", "", rev2);

            if (vmatcher.isDynamic(mrid1)) {
                int c = vmatcher.compare(mrid1, mrid2, mridComparator);
                return c >= 0 ? 1 : -1;
            } else if (vmatcher.isDynamic(mrid2)) {
                int c = vmatcher.compare(mrid2, mrid1, mridComparator);
                return c >= 0 ? -1 : 1;
            }

            return mridComparator.compare(mrid1, mrid2);
        }
    }

    private final Comparator<ModuleRevisionId> mridComparator = new MridComparator();

    private final Comparator<ArtifactInfo> artifactInfoComparator = new ArtifactInfoComparator();

    public OsgiLatestStrategy() {
        setComparator(artifactInfoComparator);
        setName("latest-osgi");
    }

}
