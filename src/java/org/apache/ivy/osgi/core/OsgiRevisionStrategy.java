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

import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.latest.ArtifactInfo;
import org.apache.ivy.plugins.latest.ComparatorLatestStrategy;
import org.apache.ivy.plugins.version.VersionMatcher;

public class OsgiRevisionStrategy extends ComparatorLatestStrategy {

    private static final Pattern ALPHA_NUM_REGEX = Pattern.compile("([a-zA-Z])(\\d)");

    private static final Pattern NUM_ALPHA_REGEX = Pattern.compile("(\\d)([a-zA-Z])");

    private static final Pattern LABEL_REGEX = Pattern.compile("[_\\-\\+]");

    /**
     * Compares two ModuleRevisionId by their revision. Revisions are compared using an algorithm
     * inspired by PHP version_compare one.
     */
    final class MridComparator implements Comparator/* <ModuleRevisionId> */{

        public int compare(Object o1, Object o2) {
            return compare((ModuleRevisionId) o1, (ModuleRevisionId) o2);
        }

        public int compare(ModuleRevisionId o1, ModuleRevisionId o2) {
            String rev1 = o1.getRevision();
            String rev2 = o2.getRevision();

            String[] outerParts1 = rev1.split("[\\.]");
            String[] outerParts2 = rev2.split("[\\.]");

            for (int i = 0; i < outerParts1.length && i < outerParts2.length; i++) {
                String outerPart1 = outerParts1[i];
                String outerPart2 = outerParts2[i];

                if (outerPart1.equals(outerPart2)) {
                    continue;
                }

                outerPart1 = ALPHA_NUM_REGEX.matcher(outerPart1).replaceAll("$1_$2");
                outerPart1 = NUM_ALPHA_REGEX.matcher(outerPart1).replaceAll("$1_$2");
                outerPart2 = ALPHA_NUM_REGEX.matcher(outerPart2).replaceAll("$1_$2");
                outerPart2 = NUM_ALPHA_REGEX.matcher(outerPart2).replaceAll("$1_$2");

                String[] innerParts1 = LABEL_REGEX.split(outerPart1);
                String[] innerParts2 = LABEL_REGEX.split(outerPart2);

                for (int j = 0; j < innerParts1.length && j < innerParts2.length; j++) {
                    if (innerParts1[j].equals(innerParts2[j])) {
                        continue;
                    }
                    boolean is1Number = isNumber(innerParts1[j]);
                    boolean is2Number = isNumber(innerParts2[j]);
                    if (is1Number && !is2Number) {
                        return 1;
                    }
                    if (is2Number && !is1Number) {
                        return -1;
                    }
                    if (is1Number && is2Number) {
                        return Long.valueOf(innerParts1[j]).compareTo(Long.valueOf(innerParts2[j]));
                    }
                    // both are strings, we compare them taking into account special meaning
                    Map/* <String, Integer> */meanings = getSpecialMeanings();
                    Integer sm1 = (Integer) meanings.get(innerParts1[j].toLowerCase(Locale.US));
                    Integer sm2 = (Integer) meanings.get(innerParts2[j].toLowerCase(Locale.US));
                    if (sm1 != null) {
                        sm2 = sm2 == null ? new Integer(0) : sm2;
                        return sm1.compareTo(sm2);
                    }
                    if (sm2 != null) {
                        return new Integer(0).compareTo(sm2);
                    }
                    return innerParts1[j].compareTo(innerParts2[j]);
                }
                if (i < innerParts1.length) {
                    return isNumber(innerParts1[i]) ? 1 : -1;
                }
                if (i < innerParts2.length) {
                    return isNumber(innerParts2[i]) ? -1 : 1;
                }
            }

            if (outerParts1.length > outerParts2.length) {
                return 1;
            } else if (outerParts1.length < outerParts2.length) {
                return -1;
            }

            return 0;
        }

        private boolean isNumber(String str) {
            return str.matches("\\d+");
        }
    }

    /**
     * Compares two ArtifactInfo by their revision. Revisions are compared using an algorithm
     * inspired by PHP version_compare one, unless a dynamic revision is given, in which case the
     * version matcher is used to perform the comparison.
     */
    final class ArtifactInfoComparator implements Comparator/* <ArtifactInfo> */{

        public int compare(Object o1, Object o2) {
            return compare((ArtifactInfo) o1, (ArtifactInfo) o2);
        }

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

    public static class SpecialMeaning {
        private String name;

        private Integer value;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getValue() {
            return value;
        }

        public void setValue(Integer value) {
            this.value = value;
        }

        public void validate() {
            if (name == null) {
                throw new IllegalStateException("a special meaning should have a name");
            }
            if (value == null) {
                throw new IllegalStateException("a special meaning should have a value");
            }
        }
    }

    private static final Map/* <String, Integer> */DEFAULT_SPECIAL_MEANINGS;
    static {
        DEFAULT_SPECIAL_MEANINGS = new HashMap/* <String, Integer> */();
        DEFAULT_SPECIAL_MEANINGS.put("dev", new Integer(-1));
        DEFAULT_SPECIAL_MEANINGS.put("rc", new Integer(1));
        DEFAULT_SPECIAL_MEANINGS.put("final", new Integer(2));
    }

    private final Comparator/* <ModuleRevisionId> */mridComparator = new MridComparator();

    private final Comparator/* <ArtifactInfo> */artifactInfoComparator = new ArtifactInfoComparator();

    private Map/* <String, Integer> */specialMeanings = null;

    private boolean usedefaultspecialmeanings = true;

    public OsgiRevisionStrategy() {
        setComparator(artifactInfoComparator);
        setName("latest-revision");
    }

    public void addConfiguredSpecialMeaning(SpecialMeaning meaning) {
        meaning.validate();
        getSpecialMeanings().put(meaning.getName().toLowerCase(Locale.US), meaning.getValue());
    }

    public synchronized Map/* <String, Integer> */getSpecialMeanings() {
        if (specialMeanings == null) {
            specialMeanings = new HashMap/* <String, Integer> */();
            if (isUsedefaultspecialmeanings()) {
                specialMeanings.putAll(DEFAULT_SPECIAL_MEANINGS);
            }
        }
        return specialMeanings;
    }

    public boolean isUsedefaultspecialmeanings() {
        return usedefaultspecialmeanings;
    }

    public void setUsedefaultspecialmeanings(boolean usedefaultspecialmeanings) {
        this.usedefaultspecialmeanings = usedefaultspecialmeanings;
    }
}
