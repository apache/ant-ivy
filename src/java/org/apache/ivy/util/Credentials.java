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
 * 
 */
public class Credentials {
    private String realm;

    private String host;

    private String userName;

    private String passwd;

    public Credentials(String realm, String host, String userName, String passwd) {
        this.realm = realm;
        this.host = host;
        this.userName = userName;
        this.passwd = passwd;
    }

    public String getHost() {
        return host;
    }

    public String getPasswd() {
        return passwd;
    }

    public String getRealm() {
        return realm;
    }

    public String getUserName() {
        return userName;
    }

    public static String buildKey(String realm, String host) {
        if (realm == null || "".equals(realm.trim())) {
            return host;
        } else {
            return realm + "@" + host;
        }
    }

    /**
     * Return a string that can be used for debug purpose. It contains only stars for each password
     * character.
     */
    public String toString() {
        return getKey() + " " + getUserName() + "/" + getPasswdAsStars();
    }

    private String getPasswdAsStars() {
        if (passwd == null) {
            return null;
        }
        StringBuffer sb = new StringBuffer();
        for (int i = passwd.length(); i > 0; i--) {
            sb.append('*');
        }
        return sb.toString();
    }

    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (o instanceof Credentials) {
            Credentials c = (Credentials) o;
            return getKey().equals(c.getKey());
        }

        return false;
    }

    public int hashCode() {
        return getKey().hashCode();
    }

    public String getKey() {
        return buildKey(realm, host);
    }
}
