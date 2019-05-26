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
package org.apache.ivy.plugins.version;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.matcher.Matcher;
import org.apache.ivy.plugins.matcher.PatternMatcher;

/**
 *
 */
public class Match {
    private String revision;

    private String pattern;

    private String args;

    private String matcher;

    public String getArgs() {
        return args;
    }

    public void setArgs(String args) {
        this.args = args;
    }

    public String getMatcher() {
        return matcher;
    }

    public void setMatcher(String matcher) {
        this.matcher = matcher;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public Matcher getPatternMatcher(ModuleRevisionId askedMrid) {
        String revision = askedMrid.getRevision();

        List<String> args = split(getArgs());
        List<String> argValues = getRevisionArgs(revision);

        if (args.size() != argValues.size()) {
            return new NoMatchMatcher();
        }

        Map<String, String> variables = new HashMap<>();
        for (String arg : args) {
            variables.put(arg, argValues.get(args.indexOf(arg)));
        }

        String pattern = getPattern();
        pattern = IvyPatternHelper.substituteVariables(pattern, variables);

        PatternMatcher pMatcher = IvyContext.getContext().getSettings().getMatcher(matcher);
        return pMatcher.getMatcher(pattern);
    }

    private List<String> getRevisionArgs(String revision) {
        int bracketStartIndex = revision.indexOf('(');
        if (bracketStartIndex == -1) {
            return Collections.emptyList();
        }

        int bracketEndIndex = revision.indexOf(')');
        if (bracketEndIndex <= (bracketStartIndex + 1)) {
            return Collections.emptyList();
        }

        return split(revision.substring(bracketStartIndex + 1, bracketEndIndex));
    }

    private static List<String> split(String string) {
        if (string == null) {
            return Collections.emptyList();
        }

        StringTokenizer tokenizer = new StringTokenizer(string, ", ");
        List<String> tokens = new LinkedList<>();
        while (tokenizer.hasMoreTokens()) {
            tokens.add(tokenizer.nextToken());
        }

        return tokens;
    }

    private static class NoMatchMatcher implements Matcher {
        public boolean isExact() {
            return false;
        }

        public boolean matches(String str) {
            return false;
        }
    }
}
