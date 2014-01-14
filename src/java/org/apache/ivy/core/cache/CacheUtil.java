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
package org.apache.ivy.core.cache;

/**
 * Utility class providing some cache related facilities.
 * 
 */
public final class CacheUtil {

    /**
     * Checks that the given pattern is acceptable as a cache pattern
     * 
     * @param cachePattern
     *            the pattern to check
     * @throws IllegalArgumentException
     *             if the pattern isn't acceptable as cache pattern
     */
    public static void checkCachePattern(String cachePattern) {
        if (cachePattern == null) {
            throw new IllegalArgumentException("null cache pattern not allowed.");
        }
        if (cachePattern.startsWith("..")) {
            throw new IllegalArgumentException("invalid cache pattern: '" + cachePattern
                    + "': cache patterns must not lead outside cache directory");
        }
        if (cachePattern.startsWith("/")) {
            throw new IllegalArgumentException("invalid cache pattern: '" + cachePattern
                    + "': cache patterns must not be absolute");
        }
    }

    private CacheUtil() {
    }
}
