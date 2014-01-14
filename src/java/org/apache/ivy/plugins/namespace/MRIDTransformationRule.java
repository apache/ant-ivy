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
package org.apache.ivy.plugins.namespace;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.util.Message;

public class MRIDTransformationRule implements NamespaceTransformer {
    private static class MridRuleMatcher {
        private static final String[] TYPES = new String[] {"o", "m", "b", "r"};

        private Matcher[] matchers = new Matcher[TYPES.length];

        public boolean match(MRIDRule src, ModuleRevisionId mrid) {
            // CheckStyle:MagicNumber| OFF
            matchers[0] = Pattern.compile(getPattern(src.getOrg())).matcher(mrid.getOrganisation());
            if (!matchers[0].matches()) {
                return false;
            }
            matchers[1] = Pattern.compile(getPattern(src.getModule())).matcher(mrid.getName());
            if (!matchers[1].matches()) {
                return false;
            }
            if (mrid.getBranch() == null) {
                matchers[2] = null;
            } else {
                matchers[2] = Pattern.compile(getPattern(src.getBranch()))
                        .matcher(mrid.getBranch());
                if (!matchers[2].matches()) {
                    return false;
                }
            }
            matchers[3] = Pattern.compile(getPattern(src.getRev())).matcher(mrid.getRevision());
            if (!matchers[3].matches()) {
                return false;
            }

            return true;
            // CheckStyle:MagicNumber| ON
        }

        public ModuleRevisionId apply(MRIDRule dest, ModuleRevisionId mrid) {
            String org = applyRules(dest.getOrg(), "o");
            String mod = applyRules(dest.getModule(), "m");
            String branch = applyRules(dest.getBranch(), "b");
            String rev = applyRules(dest.getRev(), "r");

            return ModuleRevisionId.newInstance(org, mod, branch, rev,
                mrid.getQualifiedExtraAttributes());
        }

        private String applyRules(String str, String type) {
            for (int i = 0; i < TYPES.length; i++) {
                str = applyTypeRule(str, TYPES[i], type, matchers[i]);
            }
            return str;
        }

        private String applyTypeRule(String rule, String type, String ruleType, Matcher m) {
            if (m == null) {
                return rule;
            }
            String res = rule == null ? "$" + ruleType + "0" : rule;
            for (int i = 0; i < TYPES.length; i++) {
                if (TYPES[i].equals(type)) {
                    res = res.replaceAll("([^\\\\])\\$" + type, "$1\\$");
                    res = res.replaceAll("^\\$" + type, "\\$");
                } else {
                    res = res.replaceAll("([^\\\\])\\$" + TYPES[i], "$1\\\\\\$" + TYPES[i]);
                    res = res.replaceAll("^\\$" + TYPES[i], "\\\\\\$" + TYPES[i]);
                }
            }

            StringBuffer sb = new StringBuffer();
            m.reset();
            m.find();
            m.appendReplacement(sb, res);

            String str = sb.toString();
            // null rule not replaced, let it be null
            if (rule == null && ("$" + ruleType + "0").equals(str)) {
                return null;
            }

            return str;
        }

        private String getPattern(String p) {
            return p == null ? ".*" : p;
        }
    }

    private List src = new ArrayList();

    private MRIDRule dest;

    public void addSrc(MRIDRule src) {
        this.src.add(src);
    }

    public void addDest(MRIDRule dest) {
        if (this.dest != null) {
            throw new IllegalArgumentException("only one dest is allowed per mapping");
        }
        this.dest = dest;
    }

    public ModuleRevisionId transform(ModuleRevisionId mrid) {
        MridRuleMatcher matcher = new MridRuleMatcher();
        for (Iterator iter = src.iterator(); iter.hasNext();) {
            MRIDRule rule = (MRIDRule) iter.next();
            if (matcher.match(rule, mrid)) {
                ModuleRevisionId destMrid = matcher.apply(dest, mrid);
                Message.debug("found matching namespace rule: " + rule + ". Applied " + dest
                        + " on " + mrid + ". Transformed to " + destMrid);
                return destMrid;
            }
        }
        return mrid;
    }

    public boolean isIdentity() {
        return false;
    }

}
