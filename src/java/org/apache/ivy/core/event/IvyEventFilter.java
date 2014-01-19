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

import org.apache.ivy.plugins.matcher.ExactPatternMatcher;
import org.apache.ivy.plugins.matcher.Matcher;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.ivy.util.filter.AndFilter;
import org.apache.ivy.util.filter.Filter;
import org.apache.ivy.util.filter.NoFilter;
import org.apache.ivy.util.filter.NotFilter;
import org.apache.ivy.util.filter.OrFilter;

/**
 * A filter implementation filtering {@link IvyEvent} based upon an event name and a filter
 * expression. The name will be matched against the event name using the {@link PatternMatcher} used
 * to construct this object. The filter expression is a string describing how the event should be
 * filtered according to its attributes values. The matching between the filter values and the event
 * attribute values is done using the {@link PatternMatcher} used to construct this object. Here are
 * some examples:
 * <table>
 * <tr>
 * <td>expression</td>
 * <td>effect</td>
 * </tr>
 * <tr>
 * <td>type=zip</td>
 * <td>accepts event with a type attribute matching zip</td>
 * </tr>
 * <tr>
 * <td>type=zip,jar</td>
 * <td>accepts event with a type attribute matching zip or jar</td>
 * </tr>
 * <tr>
 * <td>type=src AND ext=zip</td>
 * <td>accepts event with a type attribute matching src AND an ext attribute matching zip</td>
 * </tr>
 * <tr>
 * <td>type=src OR ext=zip</td>
 * <td>accepts event with a type attribute matching src OR an ext attribute matching zip</td>
 * </tr>
 * <tr>
 * <td>NOT type=src</td>
 * <td>accepts event with a type attribute NOT matching src</td>
 * </tr>
 * </table>
 * Combination of these can be used, but no parentheses are supported right now, so only the default
 * priority can be used. The priority order is this one: AND OR NOT = This means that artifact=foo
 * AND ext=zip OR type=src will match event with artifact matching foo AND (ext matching zip OR type
 * matching src)
 * 
 * @since 1.4
 */
public class IvyEventFilter implements Filter {
    private static final String NOT = "NOT ";

    private static final String OR = " OR ";

    private static final String AND = " AND ";

    private PatternMatcher matcher;

    private Filter nameFilter;

    private Filter attFilter;

    public IvyEventFilter(String event, String filterExpression, PatternMatcher matcher) {
        this.matcher = matcher == null ? ExactPatternMatcher.INSTANCE : matcher;
        if (event == null) {
            nameFilter = NoFilter.INSTANCE;
        } else {
            final Matcher eventNameMatcher = this.matcher.getMatcher(event);
            nameFilter = new Filter() {
                public boolean accept(Object o) {
                    IvyEvent e = (IvyEvent) o;
                    return eventNameMatcher.matches(e.getName());
                }
            };
        }
        attFilter = filterExpression == null || filterExpression.trim().length() == 0 ? NoFilter.INSTANCE
                : parseExpression(filterExpression);
    }

    private Filter parseExpression(String filterExpression) {
        // expressions handled for the moment: (informal grammar)
        // EXP := SIMPLE_EXP | AND_EXP | OR_EXP | NOT_EXP
        // AND_EXP := EXP && EXP
        // OR_EXP := EXP || EXP
        // NOT_EXP := ! EXP
        // SIMPLE_EXP := attname = comma, separated, list, of, accepted, values
        // example: organisation = foo && module = bar, baz
        filterExpression = filterExpression.trim();
        int index = filterExpression.indexOf(AND);
        if (index == -1) {
            index = filterExpression.indexOf(OR);
            if (index == -1) {
                if (filterExpression.startsWith(NOT)) {
                    return new NotFilter(parseExpression(filterExpression.substring(NOT.length())));
                } else {
                    index = filterExpression.indexOf("=");
                    if (index == -1) {
                        throw new IllegalArgumentException("bad filter expression: "
                                + filterExpression + ": no equal sign found");
                    }
                    final String attname = filterExpression.substring(0, index).trim();
                    String[] values = filterExpression.substring(index + 1).trim().split(",");
                    final Matcher[] matchers = new Matcher[values.length];
                    for (int i = 0; i < values.length; i++) {
                        matchers[i] = matcher.getMatcher(values[i].trim());
                    }
                    return new Filter() {
                        public boolean accept(Object o) {
                            IvyEvent e = (IvyEvent) o;
                            String val = (String) e.getAttributes().get(attname);
                            if (val == null) {
                                return false;
                            }
                            for (int i = 0; i < matchers.length; i++) {
                                if (matchers[i].matches(val)) {
                                    return true;
                                }
                            }
                            return false;
                        }
                    };
                }
            } else {
                return new OrFilter(parseExpression(filterExpression.substring(0, index)),
                        parseExpression(filterExpression.substring(index + OR.length())));
            }
        } else {
            return new AndFilter(parseExpression(filterExpression.substring(0, index)),
                    parseExpression(filterExpression.substring(index + AND.length())));
        }
    }

    public boolean accept(Object o) {
        if (!(o instanceof IvyEvent)) {
            return false;
        }
        return nameFilter.accept(o) && attFilter.accept(o);
    }

}
