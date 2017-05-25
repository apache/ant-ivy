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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ivy.plugins.repository.vfs.VfsRepository;

/**
 *
 */
public class VfsResolver extends RepositoryResolver {
    private static final Pattern URL_PATTERN = Pattern.compile("[a-z]*://(.+):(.+)@.*");

    private static final int PASSWORD_GROUP = 2;

    public VfsResolver() {
        setRepository(new VfsRepository());
    }

    @Override
    public String getTypeName() {
        return "vfs";
    }

    @Override
    public String hidePassword(String name) {
        return prepareForDisplay(name);
    }

    public static String prepareForDisplay(String name) {
        StringBuffer s = new StringBuffer(name);
        Matcher m = URL_PATTERN.matcher(s);
        if (m.matches()) {
            final String password = m.group(PASSWORD_GROUP);
            final int passwordposi = s.indexOf(password);
            StringBuffer stars = new StringBuffer(password);
            for (int posi = 0; posi < password.length(); posi++) {
                stars.setCharAt(posi, '*');
            }
            String replacement = stars.toString();
            s = s.replace(passwordposi, passwordposi + password.length(), replacement);
        }
        return s.toString();

    }
}
