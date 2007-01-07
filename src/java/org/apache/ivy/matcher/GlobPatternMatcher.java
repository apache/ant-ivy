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

import org.apache.ivy.util.Message;
import org.apache.oro.text.GlobCompiler;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Matcher;



public final class GlobPatternMatcher implements PatternMatcher {
    public static class GlobMatcher implements Matcher {
        private Pattern _p;

        public GlobMatcher(String exp) {
            try {
                _p = new GlobCompiler().compile(exp);
            } catch (MalformedPatternException e) {
                Message.error("impossible to compile glob pattern: "+exp);
            }
        }

        public boolean matches(String str) {
            return _p != null && new Perl5Matcher().matches(str, _p);
        }

        public boolean isExact() {
            return false;
        }
    }
    private static final GlobPatternMatcher INSTANCE = new GlobPatternMatcher();
    public static PatternMatcher getInstance() {
        return INSTANCE;
    }
    
    private GlobPatternMatcher() {        
    }

    public String getName() {
        return GLOB;
    }

    public Matcher getMatcher(String exp) {
        if (ANY_EXPRESSION.equals(exp)) {
            return AnyMatcher.getInstance();
        }
        return new GlobMatcher(exp);
    }
}
