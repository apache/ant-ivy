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
package fr.jayasoft.ivy.matcher;

import java.util.regex.Pattern;

public final class RegexpPatternMatcher implements PatternMatcher {
    public static class RegexpMatcher implements Matcher {
        private Pattern _p;

        public RegexpMatcher(String exp) {
            _p = Pattern.compile(exp);
        }

        public boolean matches(String str) {
            return _p.matcher(str).matches();
        }

        public boolean isExact() {
            return false;
        }
    }
    private static final RegexpPatternMatcher INSTANCE = new RegexpPatternMatcher();
    public static PatternMatcher getInstance() {
        return INSTANCE;
    }
    
    private RegexpPatternMatcher() {        
    }
    
    public boolean match(String str, String exp) {
        if (exp == null) {
            return str == null;
        }
        return Pattern.matches(exp, str);
    }

    public String getName() {
        return REGEXP;
    }

    public Matcher getMatcher(String exp) {
        if (ANY_EXPRESSION.equals(exp)) {
            return AnyMatcher.getInstance();
        }
        return new RegexpMatcher(exp);
    }
}
