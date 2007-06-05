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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

/**
 * An implementation of Properties which stores the values encrypted. The use is transparent from
 * the user point of view (use as any Properties instance), except that get, put and putAll do not
 * handle encryption/decryption. This means that get returns the encrypted value, while put and
 * putAll puts given values without encrypting them. It this thus recommended to void using them,
 * use setProperty and getProperty instead.
 */
public class EncrytedProperties extends Properties {

    public EncrytedProperties() {
        super();
    }

    public synchronized Object setProperty(String key, String value) {
        return StringUtils.decrypt((String) super.setProperty(key, StringUtils.encrypt(value)));
    }

    public String getProperty(String key) {
        return StringUtils.decrypt(super.getProperty(key));
    }

    public String getProperty(String key, String defaultValue) {
        return StringUtils.decrypt(super.getProperty(key, StringUtils.encrypt(defaultValue)));
    }

    public boolean containsValue(Object value) {
        return super.containsValue(StringUtils.encrypt((String) value));
    }

    public synchronized boolean contains(Object value) {
        return super.contains(StringUtils.encrypt((String) value));
    }

    public Collection values() {
        List ret = new ArrayList(super.values());
        for (int i = 0; i < ret.size(); i++) {
            ret.set(i, StringUtils.decrypt((String) ret.get(i)));
        }
        return ret;
    }
}
