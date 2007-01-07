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
package org.apache.ivy.matcher;


public final class ExactOrRegexpPatternMatcher implements PatternMatcher {
    public static class ExactOrRegexpMatcher implements Matcher {
        private Matcher _exact;
        private Matcher _regexp;

        public ExactOrRegexpMatcher(String exp) {
            _exact = ExactPatternMatcher.getInstance().getMatcher(exp);
            _regexp = RegexpPatternMatcher.getInstance().getMatcher(exp);
        }

        public boolean matches(String str) {
            return _exact.matches(str) || _regexp.matches(str);
        }

        public boolean isExact() {
            return false;
        }
    }
    private static final ExactOrRegexpPatternMatcher INSTANCE = new ExactOrRegexpPatternMatcher();
    public static PatternMatcher getInstance() {
        return INSTANCE;
    }
    
    private ExactOrRegexpPatternMatcher() {        
    }
    
    public String getName() {
        return EXACT_OR_REGEXP;
    }

    public Matcher getMatcher(String exp) {
        if (ANY_EXPRESSION.equals(exp)) {
            return AnyMatcher.getInstance();
        }
        return new ExactOrRegexpMatcher(exp);
    }
}
