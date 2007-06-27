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
package org.apache.ivy.util;

/**
 * Utility class used to perform some checks.
 */
public final class Checks {
    private Checks() {
    }
    
    /**
     * Checks that an object is not null, and throw an exception if the object is null.
     * 
     * @param o
     *            the object to check
     * @param objectName
     *            the name of the object to check. This name will be used in the exception message.
     * @throws IllegalArgumentException
     *             if the object is null
     */
    public static void checkNotNull(Object o, String objectName) {
        if (o == null) {
            throw new IllegalArgumentException(objectName + " must not be null");
        }
    }

}
