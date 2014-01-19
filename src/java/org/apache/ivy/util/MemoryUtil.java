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
 * Memory related utilities.
 */
public final class MemoryUtil {
    private static final int SAMPLING_SIZE = 100;

    private static final int SLEEP_TIME = 100;

    private MemoryUtil() {
    }

    /**
     * Returns the approximate size of a default instance of the given class.
     * 
     * @param clazz
     *            the class to evaluate.
     * @return the estimated size of instance, in bytes.
     */
    public static long sizeOf(Class clazz) {
        long size = 0;
        Object[] objects = new Object[SAMPLING_SIZE];
        try {
            clazz.newInstance();
            long startingMemoryUse = getUsedMemory();
            for (int i = 0; i < objects.length; i++) {
                objects[i] = clazz.newInstance();
            }
            long endingMemoryUse = getUsedMemory();
            float approxSize = (endingMemoryUse - startingMemoryUse) / (float) objects.length;
            size = Math.round(approxSize);
        } catch (Exception e) {
            Message.warn("Couldn't instantiate " + clazz, e);
        }
        return size;
    }

    /**
     * Returns the currently used memory, after calling garbage collector and waiting enough to get
     * maximal chance it is actually called. But since {@link Runtime#gc()} is only advisory,
     * results returned by this method should be treated as rough approximation only.
     * 
     * @return the currently used memory, in bytes.
     */
    public static long getUsedMemory() {
        gc();
        long totalMemory = Runtime.getRuntime().totalMemory();
        gc();
        long freeMemory = Runtime.getRuntime().freeMemory();
        long usedMemory = totalMemory - freeMemory;
        return usedMemory;
    }

    private static void gc() {
        try {
            System.gc();
            Thread.sleep(SLEEP_TIME);
            System.runFinalization();
            Thread.sleep(SLEEP_TIME);
            System.gc();
            Thread.sleep(SLEEP_TIME);
            System.runFinalization();
            Thread.sleep(SLEEP_TIME);
        } catch (Exception e) {
            Message.debug(e);
        }
    }

    public static void main(String[] args) throws ClassNotFoundException {
        System.out.println(sizeOf(Class.forName(args[0])));
    }
}