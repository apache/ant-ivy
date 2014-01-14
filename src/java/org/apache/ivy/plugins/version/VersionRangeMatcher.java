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
package org.apache.ivy.plugins.version;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.latest.ArtifactInfo;
import org.apache.ivy.plugins.latest.LatestStrategy;

/**
 * Matches version ranges: [1.0,2.0] matches all versions greater or equal to 1.0 and lower or equal
 * to 2.0 [1.0,2.0[ matches all versions greater or equal to 1.0 and lower than 2.0 ]1.0,2.0]
 * matches all versions greater than 1.0 and lower or equal to 2.0 ]1.0,2.0[ matches all versions
 * greater than 1.0 and lower than 2.0 [1.0,) matches all versions greater or equal to 1.0 ]1.0,)
 * matches all versions greater than 1.0 (,2.0] matches all versions lower or equal to 2.0 (,2.0[
 * matches all versions lower than 2.0 This class uses a latest strategy to compare revisions. If
 * none is set, it uses the default one of the ivy instance set through setIvy(). If neither a
 * latest strategy nor a ivy instance is set, an IllegalStateException will be thrown when calling
 * accept(). Note that it can't work with latest time strategy, cause no time is known for the
 * limits of the range. Therefore only purely revision based LatestStrategy can be used.
 */
public class VersionRangeMatcher extends AbstractVersionMatcher {
    // todo: check these constants
    private static final String OPEN_INC = "[";

    private static final String OPEN_EXC = "]";

    private static final String OPEN_EXC_MAVEN = "(";

    private static final String CLOSE_INC = "]";

    private static final String CLOSE_EXC = "[";

    private static final String CLOSE_EXC_MAVEN = ")";

    private static final String LOWER_INFINITE = "(";

    private static final String UPPER_INFINITE = ")";

    private static final String SEPARATOR = ",";

    // following patterns are built upon constants above and should not be modified
    private static final String OPEN_INC_PATTERN = "\\" + OPEN_INC;

    private static final String OPEN_EXC_PATTERN = "\\" + OPEN_EXC + "\\" + OPEN_EXC_MAVEN;

    private static final String CLOSE_INC_PATTERN = "\\" + CLOSE_INC;

    private static final String CLOSE_EXC_PATTERN = "\\" + CLOSE_EXC + "\\" + CLOSE_EXC_MAVEN;

    private static final String LI_PATTERN = "\\" + LOWER_INFINITE;

    private static final String UI_PATTERN = "\\" + UPPER_INFINITE;

    private static final String SEP_PATTERN = "\\s*\\" + SEPARATOR + "\\s*";

    private static final String OPEN_PATTERN = "[" + OPEN_INC_PATTERN + OPEN_EXC_PATTERN + "]";

    private static final String CLOSE_PATTERN = "[" + CLOSE_INC_PATTERN + CLOSE_EXC_PATTERN + "]";

    private static final String ANY_NON_SPECIAL_PATTERN = "[^\\s" + SEPARATOR + OPEN_INC_PATTERN
            + OPEN_EXC_PATTERN + CLOSE_INC_PATTERN + CLOSE_EXC_PATTERN + LI_PATTERN + UI_PATTERN
            + "]";

    private static final String FINITE_PATTERN = OPEN_PATTERN + "\\s*(" + ANY_NON_SPECIAL_PATTERN
            + "+)" + SEP_PATTERN + "(" + ANY_NON_SPECIAL_PATTERN + "+)\\s*" + CLOSE_PATTERN;

    private static final String LOWER_INFINITE_PATTERN = LI_PATTERN + SEP_PATTERN + "("
            + ANY_NON_SPECIAL_PATTERN + "+)\\s*" + CLOSE_PATTERN;

    private static final String UPPER_INFINITE_PATTERN = OPEN_PATTERN + "\\s*("
            + ANY_NON_SPECIAL_PATTERN + "+)" + SEP_PATTERN + UI_PATTERN;

    private static final Pattern FINITE_RANGE = Pattern.compile(FINITE_PATTERN);

    private static final Pattern LOWER_INFINITE_RANGE = Pattern.compile(LOWER_INFINITE_PATTERN);

    private static final Pattern UPPER_INFINITE_RANGE = Pattern.compile(UPPER_INFINITE_PATTERN);

    private static final Pattern ALL_RANGE = Pattern.compile(FINITE_PATTERN + "|"
            + LOWER_INFINITE_PATTERN + "|" + UPPER_INFINITE_PATTERN);

    private final class MRIDArtifactInfo implements ArtifactInfo {
        private ModuleRevisionId mrid;

        public MRIDArtifactInfo(ModuleRevisionId id) {
            mrid = id;
        }

        public long getLastModified() {
            return 0;
        }

        public String getRevision() {
            return mrid.getRevision();
        }
    }

    private final Comparator comparator = new Comparator() {
        public int compare(Object o1, Object o2) {
            if (o1.equals(o2)) {
                return 0;
            }
            ArtifactInfo art1 = new MRIDArtifactInfo((ModuleRevisionId) o1);
            ArtifactInfo art2 = new MRIDArtifactInfo((ModuleRevisionId) o2);
            ArtifactInfo art = getLatestStrategy()
                    .findLatest(new ArtifactInfo[] {art1, art2}, null);
            return art == art1 ? -1 : 1;
        }
    };

    private LatestStrategy latestStrategy;

    private String latestStrategyName = "default";

    public VersionRangeMatcher() {
        super("version-range");
    }

    public VersionRangeMatcher(String name) {
        super(name);
    }

    public VersionRangeMatcher(String name, LatestStrategy strategy) {
        super(name);
        this.latestStrategy = strategy;
    }

    public boolean isDynamic(ModuleRevisionId askedMrid) {
        String revision = askedMrid.getRevision();
        return ALL_RANGE.matcher(revision).matches();
    }

    public boolean accept(ModuleRevisionId askedMrid, ModuleRevisionId foundMrid) {
        String revision = askedMrid.getRevision();
        Matcher m;
        m = FINITE_RANGE.matcher(revision);
        if (m.matches()) {
            String lower = m.group(1);
            String upper = m.group(2);
            return isUpper(askedMrid, lower, foundMrid, revision.startsWith(OPEN_INC))
                    && isLower(askedMrid, upper, foundMrid, revision.endsWith(CLOSE_INC));
        }
        m = LOWER_INFINITE_RANGE.matcher(revision);
        if (m.matches()) {
            String upper = m.group(1);
            return isLower(askedMrid, upper, foundMrid, revision.endsWith(CLOSE_INC));
        }
        m = UPPER_INFINITE_RANGE.matcher(revision);
        if (m.matches()) {
            String lower = m.group(1);
            return isUpper(askedMrid, lower, foundMrid, revision.startsWith(OPEN_INC));
        }
        return false;
    }

    private boolean isLower(ModuleRevisionId askedMrid, String revision,
            ModuleRevisionId foundMrid, boolean inclusive) {
        ModuleRevisionId mRevId = ModuleRevisionId.newInstance(askedMrid, revision);
        int result = comparator.compare(mRevId, foundMrid);
        return result <= (inclusive ? 0 : -1);
    }

    private boolean isUpper(ModuleRevisionId askedMrid, String revision,
            ModuleRevisionId foundMrid, boolean inclusive) {
        ModuleRevisionId mRevId = ModuleRevisionId.newInstance(askedMrid, revision);
        int result = comparator.compare(mRevId, foundMrid);
        return result >= (inclusive ? 0 : 1);
    }

    public int compare(ModuleRevisionId askedMrid, ModuleRevisionId foundMrid,
            Comparator staticComparator) {
        String revision = askedMrid.getRevision();
        Matcher m;
        m = UPPER_INFINITE_RANGE.matcher(revision);
        if (m.matches()) {
            // no upper limit, the dynamic revision can always be considered greater
            return 1;
        }
        String upper;
        m = FINITE_RANGE.matcher(revision);
        if (m.matches()) {
            upper = m.group(2);
        } else {
            m = LOWER_INFINITE_RANGE.matcher(revision);
            if (m.matches()) {
                upper = m.group(1);
            } else {
                throw new IllegalArgumentException(
                        "impossible to compare: askedMrid is not a dynamic revision: " + askedMrid);
            }
        }
        int c = staticComparator.compare(ModuleRevisionId.newInstance(askedMrid, upper), foundMrid);
        // if the comparison consider them equal, we must return -1, because we can't consider the
        // dynamic revision to be greater. Otherwise we can safeely return the result of the static
        // comparison
        return c == 0 ? -1 : c;
    }

    public LatestStrategy getLatestStrategy() {
        if (latestStrategy == null) {
            if (getSettings() == null) {
                throw new IllegalStateException(
                        "no ivy instance nor latest strategy configured in version range matcher "
                                + this);
            }
            if (latestStrategyName == null) {
                throw new IllegalStateException(
                        "null latest strategy defined in version range matcher " + this);
            }
            latestStrategy = getSettings().getLatestStrategy(latestStrategyName);
            if (latestStrategy == null) {
                throw new IllegalStateException("unknown latest strategy '" + latestStrategyName
                        + "' configured in version range matcher " + this);
            }
        }
        return latestStrategy;
    }

    public void setLatestStrategy(LatestStrategy latestStrategy) {
        this.latestStrategy = latestStrategy;
    }

    public void setLatest(String latestStrategyName) {
        this.latestStrategyName = latestStrategyName;
    }

}
