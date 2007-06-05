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
package org.apache.ivy.plugins.matcher;

/**
 * An interface that defines a string matcher.
 */
public interface Matcher {

    /**
     * Check whether a given string is matched by this matcher.
     * 
     * @param input
     *            the string to be matched. Cannot be null.
     * @return true if the input string is matched, false otherwise.
     */
    public boolean matches(/* @NotNull */String input);

    /**
     * Return if the matcher will match *only* if the expression equals the input. <i> WARN: This is
     * used only as a performance trick, to avoid scanning for things when you already know exactly
     * what you want. In the install task where it used it avoid scanning the repository to list all
     * modules to find that only one matches, and that it has the name requested. </i>
     * 
     * @return true if the matcher only matches when the expression is equals to the input, false
     *         otherwise.
     */
    public boolean isExact();
}
