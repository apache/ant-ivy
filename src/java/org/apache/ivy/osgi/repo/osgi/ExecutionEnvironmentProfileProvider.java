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
package org.apache.ivy.osgi.repo.osgi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.ivy.osgi.repo.ExecutionEnvironmentProfile;
import org.apache.ivy.util.Message;


/**
 * Load profiles provided by the <tt>org.eclipse.osgi</tt> bundle.
 */
public class ExecutionEnvironmentProfileProvider {

    private static final String PROFILE_NAME = "osgi.java.profile.name";

    private static final String SYSTEM_PACKAGES = "org.osgi.framework.system.packages";

    private static final String PROFILE_LIST_FILE = "profile.list";

    private static final String PACKAGE_PREFIX = "org/apache/ivy/osgi/repo/osgi/";

    private static final String PROFILE_LIST = "java.profiles";

    private Map<String, ExecutionEnvironmentProfile> profileList;

    // private static final String BOOT_DELEGATION = "org.osgi.framework.bootdelegation";

    // private static final String ENVIRONMENT = "org.osgi.framework.executionenvironment";

    public ExecutionEnvironmentProfileProvider() throws IOException {
        profileList = loadDefaultProfileList();
    }

    public ExecutionEnvironmentProfile getProfile(String profile) {
        return profileList.get(profile);
    }

    public static Map<String, ExecutionEnvironmentProfile> loadDefaultProfileList() throws IOException {
        ClassLoader loader = ExecutionEnvironmentProfileProvider.class.getClassLoader();
        InputStream profileListFile = loader.getResourceAsStream(PACKAGE_PREFIX + PROFILE_LIST_FILE);
        if (profileListFile == null) {
            throw new FileNotFoundException(PACKAGE_PREFIX + PROFILE_LIST_FILE + " not found in the classpath");
        }
        Properties props = new Properties();
        props.load(profileListFile);
        String[] profileList = props.getProperty(PROFILE_LIST).split(",");
        Message.debug("Loading profiles " + profileList);
        Map<String, ExecutionEnvironmentProfile> map = new HashMap<String, ExecutionEnvironmentProfile>();
        for (String profileFile : profileList) {
            String p = profileFile.trim();
            if (p.length() != 0) {
                ExecutionEnvironmentProfile profile = load(loader.getResourceAsStream(PACKAGE_PREFIX + p));
                if (profile != null) {
                    Message.verbose("Execution environment profile " + profile.getName() + " loaded");
                    map.put(profile.getName(), profile);
                } else {
                    Message.warn("Unable to load the environement profile " + PACKAGE_PREFIX + p);
                }
            }
        }
        return map;
    }

    public static ExecutionEnvironmentProfile load(File f) throws IOException {
        return load(new FileInputStream(f));
    }

    public static ExecutionEnvironmentProfile load(InputStream in) throws IOException {
        Properties props = new Properties();
        props.load(in);
        return load(props);
    }

    public static ExecutionEnvironmentProfile load(Properties properties) {
        ExecutionEnvironmentProfile profile = new ExecutionEnvironmentProfile(properties.getProperty(PROFILE_NAME));
        String packagesList = properties.getProperty(SYSTEM_PACKAGES);
        if (packagesList == null) {
            Message.warn("The profile " + profile.getName() + " doesn't have any system package definition");
            return null;
        }
        String[] packages = packagesList.split(",");
        for (String pkg : packages) {
            profile.addPkgName(pkg.trim());
        }
        return profile;
    }
}
