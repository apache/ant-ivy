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
package org.apache.ivy.util.url;

import java.util.HashMap;
import java.util.Map;

import org.apache.ivy.util.Credentials;
import org.apache.ivy.util.Message;

/**
 * 
 */
public final class CredentialsStore {
    /**
     * A Map of Credentials objects keyed by the 'key' of the Credentials.
     */
    private static final Map KEYRING = new HashMap();

    public static final CredentialsStore INSTANCE = new CredentialsStore();

    private CredentialsStore() {
    }

    public void addCredentials(String realm, String host, String userName, String passwd) {
        if (userName == null) {
            return;
        }
        Credentials c = new Credentials(realm, host, userName, passwd);
        Message.debug("credentials added: " + c);
        KEYRING.put(c.getKey(), c);
        // add also with host only, to be able to find credential with host only
        // (useful for httpclient especially)
        KEYRING.put(c.getHost(), c);
    }

    public Credentials getCredentials(String realm, String host) {
        return (Credentials) KEYRING.get(Credentials.buildKey(realm, host));
    }

}
