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
package org.apache.ivy.osgi.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

/**
 * Provides a bundle name conversion utility.
 * 
 * @author alex@radeski.net
 */
public class NameUtil {

    private static final NameUtil instance = new NameUtil();

    public static NameUtil instance() {
        return instance;
    }

    private final Set<String> tlds = new HashSet<String>();

    private NameUtil() {
        final InputStream inputStream = getClass().getClassLoader().getResourceAsStream("orgs.list");
        if (inputStream == null) {
            throw new IllegalStateException("Unable to find required file in classpath: orgs.list");
        }
        final BufferedReader bis = new BufferedReader(new InputStreamReader(inputStream));
        try {
            String line = null;
            while ((line = bis.readLine()) != null) {
                line = line.trim();
                if (line.equals("") || line.startsWith("#")) {
                    continue;
                }
                tlds.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public OrgAndName asOrgAndName(String qname) {
        final String[] tokens = qname.split("\\.");
        if ((tokens == null) || (tokens.length == 0)) {
            throw new IllegalStateException("Qualified name is empty or invalid: " + qname);
        }

        String org = null;
        String name = null;

        if (tlds.contains(tokens[0])) {
            org = append(tokens, 0, 1);
            name = append(tokens, 2, tokens.length);
        } else if (tokens.length == 1) {
            org = tokens[0];
            name = tokens[0];
        } else if (tokens.length >= 2) {
            org = tokens[0];
            name = append(tokens, 1, tokens.length);
        }

        if (org == null || name == null) {
            throw new IllegalStateException("Null org/name: org=" + org + ", name=" + name + ", qname=" + qname);
        }

        return new OrgAndName(org, name);
    }

    private String append(String[] strs, int start, int end) {
        final StringBuffer sbuf = new StringBuffer();
        boolean dot = false;
        for (int i = start; i <= end && i < strs.length; i++) {
            if (dot) {
                sbuf.append('.');
            }
            sbuf.append(strs[i]);
            dot = true;
        }
        return sbuf.toString();
    }

    public static class OrgAndName {
        public final String org;
        public final String name;

        private OrgAndName(String org, String name) {
            this.org = org;
            this.name = name;
        }

    }
}
