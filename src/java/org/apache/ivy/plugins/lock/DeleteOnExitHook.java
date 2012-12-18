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
package org.apache.ivy.plugins.lock;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashSet;

class DeleteOnExitHook {

    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                runHook();
            }
        });
    }

    private static LinkedHashSet files = new LinkedHashSet();

    private DeleteOnExitHook() {
    }

    static synchronized void add(File file) {
        files.add(file);
    }

    static synchronized void remove(File file) {
        files.remove(file);
    }

    static synchronized void runHook() {
        Iterator itr = files.iterator();
        while (itr.hasNext()) {
            ((File) itr.next()).delete();
            itr.remove();
        }
    }
}