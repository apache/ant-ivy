/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivy.core.module.id;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.plugins.matcher.ExactPatternMatcher;
import org.apache.ivy.plugins.matcher.MapMatcher;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.ivy.util.filter.Filter;
import org.apache.ivy.util.filter.NoFilter;
import org.junit.Before;
import org.junit.Test;

public class ModuleRulesTest {
    private ModuleRules<String> rules;

    private String[] rule;

    @Before
    public void setUp() {
        rules = new ModuleRules<>();
        rule = new String[10];
        for (int i = 0; i < rule.length; i++) {
            rule[i] = "RULE_" + i;
        }
    }

    @Test
    public void testGetRule() {
        // fixture
        rules.defineRule(mapMatcher().organization("apache").build(), rule[0]);
        rules.defineRule(mapMatcher().organization("other").build(), rule[1]);

        // test
        assertRule(rule[0], "apache#module1;1.5");
        assertRule(rule[0], "apache#module2;3.0");
        assertRule(rule[1], "other#module2;1.5");
        assertRule(null, "unknown#module1;1.5");
    }

    @Test
    public void testGetRuleWithFilter() {
        // fixture
        rules.defineRule(mapMatcher().organization("apache").build(), rule[0]);
        rules.defineRule(mapMatcher().module("module1").build(), rule[1]);
        rules.defineRule(mapMatcher().module("module2").build(), rule[2]);

        // test
        assertRule(rule[0], "apache#module1;1.5", acceptAll());
        assertRule(rule[1], "apache#module1;1.5", acceptSecond());
        assertModuleIdRule(rule[1], "apache#module1", acceptSecond());
        assertRule(null, "apache#module1;1.5", acceptNone());
        assertRule(rule[2], "apache#module2;1.5", acceptSecond());
        assertRule(null, "unknown#module4;1.5", acceptAll());
    }

    // test helpers

    private Filter<String> acceptNone() {
        return new Filter<String>() {
            public boolean accept(String o) {
                return false;
            }

            public String toString() {
                return "AcceptNone";
            }
        };
    }

    private Filter<String> acceptSecond() {
        return new Filter<String>() {
            private int cpt;

            public boolean accept(String o) {
                return ++cpt == 2;
            }

            public String toString() {
                return "AcceptSecond";
            }
        };
    }

    private Filter<String> acceptAll() {
        return NoFilter.instance();
    }

    private void assertRule(String rule, String mrid) {
        String ruleFound = rules.getRule(ModuleRevisionId.parse(mrid));
        assertEquals("unexcepted rule for " + mrid, rule, ruleFound);
    }

    private void assertRule(String rule, String mrid, Filter<String> filter) {
        String ruleFound = rules.getRule(ModuleRevisionId.parse(mrid), filter);
        assertEquals("unexcepted rule for " + mrid + " filtered by " + filter, rule, ruleFound);
    }

    private void assertModuleIdRule(String rule, String mid, Filter<String> filter) {
        String ruleFound = rules.getRule(ModuleId.parse(mid), filter);
        assertEquals("unexcepted rule for " + mid + " filtered by " + filter, rule, ruleFound);
    }

    private MridMatcherBuilder mapMatcher() {
        return new MridMatcherBuilder();
    }

    public class MridMatcherBuilder {
        private Map<String, String> attributes = new HashMap<>();

        private PatternMatcher matcher = ExactPatternMatcher.INSTANCE;

        public MridMatcherBuilder organization(String org) {
            attributes.put(IvyPatternHelper.ORGANISATION_KEY, org);
            return this;
        }

        public MridMatcherBuilder module(String mod) {
            attributes.put(IvyPatternHelper.MODULE_KEY, mod);
            return this;
        }

        public MapMatcher build() {
            return new MapMatcher(attributes, matcher);
        }
    }

}
