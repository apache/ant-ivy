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
package org.apache.ivy.plugins.resolver;

import java.net.URL;

import org.apache.ivy.util.url.URLHandler;
import org.apache.ivy.util.url.URLHandlerRegistry;

/**
 * TODO write javadoc
 */
public class IBiblioHelper {
    private static boolean _checked = false;

    private static String _mirror = null;

    private static URLHandler handler = URLHandlerRegistry.getHttp();

    public static String getIBiblioMirror() throws Exception {
        if (!_checked) {
            String[] mirrors = new String[] {"http://mirrors.ibiblio.org/maven"};
            String[] mirrorsRoot = new String[] {"http://mirrors.ibiblio.org/maven"};

            long best = -1;
            for (int i = 0; i < mirrors.length; i++) {
                long start = System.currentTimeMillis();
                if (handler.isReachable(new URL(mirrors[i]), 500)) {
                    long took = System.currentTimeMillis() - start;
                    System.out.println("reached " + mirrors[i] + " in " + took + "ms");
                    if (best == -1 || took < best) {
                        best = took;
                        _mirror = mirrorsRoot[i];
                        if (took < 400) {
                            // this mirror is good enough
                            break;
                        }
                    }
                }
            }
            if (_mirror == null) {
                System.out.println("No ibiblio mirror available: no ibiblio test done");
            }
        }
        return _mirror;
    }

    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();
        String biblioMirror = getIBiblioMirror();
        System.out.println("best mirror is " + biblioMirror + " - found in "
                + (System.currentTimeMillis() - start) + "ms");
    }

}
