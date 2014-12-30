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
package org.apache.ivy.core.module.id;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.ivy.plugins.matcher.MapMatcher;
import org.apache.ivy.util.Checks;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.filter.Filter;
import org.apache.ivy.util.filter.NoFilter;

/**
 * A list of module specific rules.
 * <p>
 * This class defines a list of module specific rules. For each module only one rule apply,
 * sometimes none.
 * </p>
 * <p>
 * To know which rule to apply, they are configured using matchers. So you can define a rule
 * applying to all module from one particular organization, or to all modules with a revisions
 * matching a pattern, and so on.
 * </p>
 * <p>
 * Rules condition are evaluated in order, so the first matching rule is returned.
 * </p>
 * <p>
 * Rules themselves can be represented by any object, depending on the purpose of the rule (define
 * which resolver to use, which TTL in cache, ...)
 * </p>
 */
public class ModuleRules<T> {

    private Map<MapMatcher, T> rules = new LinkedHashMap<MapMatcher, T>();

    private MatcherLookup matcher_lookup = new MatcherLookup();

    /**
     * Constructs an empty ModuleRules.
     */
    public ModuleRules() {
    }

    private ModuleRules(Map<MapMatcher, T> rules) {
        this.rules = new LinkedHashMap<MapMatcher, T>(rules);
        for (MapMatcher matcher : rules.keySet()) {
            matcher_lookup.add(matcher);
        }
    }

    /**
     * Defines a new rule for the given condition.
     * 
     * @param condition
     *            the condition for which the rule should be applied. Must not be <code>null</code>.
     * @param rule
     *            the rule to apply. Must not be <code>null</code>.
     */
    public void defineRule(MapMatcher condition, T rule) {
        Checks.checkNotNull(condition, "condition");
        Checks.checkNotNull(rule, "rule");

        rules.put(condition, rule);
        matcher_lookup.add(condition);
    }

    /**
     * Returns the rule object matching the given {@link ModuleId}, or <code>null</code> if no rule
     * applies.
     * 
     * @param mid
     *            the {@link ModuleId} to search the rule for. Must not be <code>null</code>.
     * @return the rule object matching the given {@link ModuleId}, or <code>null</code> if no rule
     *         applies.
     * @see #getRule(ModuleId, Filter)
     */
    public T getRule(ModuleId mid) {
        return getRule(mid, NoFilter.<T> instance());
    }

    /**
     * Returns the rules objects matching the given {@link ModuleId}, or an empty array if no rule
     * applies.
     * 
     * @param mid
     *            the {@link ModuleId} to search the rule for. Must not be <code>null</code>.
     * @return an array of rule objects matching the given {@link ModuleId}.
     */
    public T[] getRules(ModuleId mid) {
        return getRules(mid.getAttributes(), NoFilter.<T> instance());
    }

    /**
     * Returns the rule object matching the given {@link ModuleRevisionId}, or <code>null</code> if
     * no rule applies.
     * 
     * @param mrid
     *            the {@link ModuleRevisionId} to search the rule for. Must not be <code>null</code>
     *            .
     * @return the rule object matching the given {@link ModuleRevisionId}, or <code>null</code> if
     *         no rule applies.
     * @see #getRule(ModuleRevisionId, Filter)
     */
    public T getRule(ModuleRevisionId mrid) {
        return getRule(mrid, NoFilter.<T> instance());
    }

    /**
     * Returns the rule object matching the given {@link ModuleId} and accepted by the given
     * {@link Filter}, or <code>null</code> if no rule applies.
     * 
     * @param mid
     *            the {@link ModuleRevisionId} to search the rule for. Must not be <code>null</code>
     *            .
     * @param filter
     *            the filter to use to filter the rule to return. The {@link Filter#accept(Object)}
     *            method will be called only with rule objects matching the given {@link ModuleId},
     *            and the first rule object accepted by the filter will be returned. Must not be
     *            <code>null</code>.
     * @return the rule object matching the given {@link ModuleId}, or <code>null</code> if no rule
     *         applies.
     * @see #getRule(ModuleRevisionId, Filter)
     */
    public T getRule(ModuleId mid, Filter<T> filter) {
        Checks.checkNotNull(mid, "mid");
        return getRule(mid.getAttributes(), filter);
    }

    /**
     * Returns the rule object matching the given {@link ModuleRevisionId} and accepted by the given
     * {@link Filter}, or <code>null</code> if no rule applies.
     * 
     * @param mrid
     *            the {@link ModuleRevisionId} to search the rule for. Must not be <code>null</code>
     *            .
     * @param filter
     *            the filter to use to filter the rule to return. The {@link Filter#accept(Object)}
     *            method will be called only with rule objects matching the given
     *            {@link ModuleRevisionId}, and the first rule object accepted by the filter will be
     *            returned. Must not be <code>null</code>.
     * @return the rule object matching the given {@link ModuleRevisionId}, or <code>null</code> if
     *         no rule applies.
     * @see #getRule(ModuleRevisionId)
     */
    public T getRule(ModuleRevisionId mrid, Filter<T> filter) {
        Checks.checkNotNull(mrid, "mrid");
        Checks.checkNotNull(filter, "filter");
        Map<String, String> moduleAttributes = mrid.getAttributes();
        return getRule(moduleAttributes, filter);
    }

    private T getRule(Map<String, String> moduleAttributes, Filter<T> filter) {
        List<MapMatcher> matchers = matcher_lookup.get(moduleAttributes);
        for (MapMatcher midm : matchers) {
            T rule = rules.get(midm);
            if (filter.accept(rule)) {
                return rule;
            }
        }
        return null;
    }

    /**
     * Returns the rules object matching the given {@link ModuleRevisionId} and accepted by the
     * given {@link Filter}, or an empty array if no rule applies.
     * 
     * @param mrid
     *            the {@link ModuleRevisionId} to search the rule for. Must not be <code>null</code>
     *            .
     * @param filter
     *            the filter to use to filter the rule to return. The {@link Filter#accept(Object)}
     *            method will be called only with rule objects matching the given
     *            {@link ModuleRevisionId}. Must not be <code>null</code>.
     * @return an array of rule objects matching the given {@link ModuleRevisionId}.
     */
    public T[] getRules(ModuleRevisionId mrid, Filter<T> filter) {
        Checks.checkNotNull(mrid, "mrid");
        Checks.checkNotNull(filter, "filter");
        Map<String, String> moduleAttributes = mrid.getAttributes();
        return getRules(moduleAttributes, filter);
    }

    private T[] getRules(Map<String, String> moduleAttributes, Filter<T> filter) {
        List<MapMatcher> matchers = matcher_lookup.get(moduleAttributes);
        List<T> matchingRules = new ArrayList<T>();
        for (MapMatcher midm : matchers) {
            T rule = rules.get(midm);
            if (filter.accept(rule)) {
                matchingRules.add(rule);
            }
        }
        return matchingRules.toArray((T[]) new Object[0]);
    }

    /**
     * Dump the list of rules to {@link Message#debug(String)}
     * 
     * @param prefix
     *            the prefix to use for each line dumped
     */
    public void dump(String prefix) {
        if (rules.isEmpty()) {
            Message.debug(prefix + "NONE");
        } else {
            for (Entry<MapMatcher, T> entry : rules.entrySet()) {
                MapMatcher midm = entry.getKey();
                T rule = entry.getValue();
                Message.debug(prefix + midm + " -> " + rule);
            }
        }
    }

    /**
     * Returns an unmodifiable view of all the rules defined on this ModuleRules.
     * <p>
     * The rules are returned in a Map where they keys are the MapMatchers matching the rules
     * object, and the values are the rules object themselves.
     * </p>
     * 
     * @return an unmodifiable view of all the rules defined on this ModuleRules.
     */
    public Map<MapMatcher, T> getAllRules() {
        return Collections.unmodifiableMap(rules);
    }

    @Override
    public Object clone() {
        return new ModuleRules<T>(rules);
    }
}
