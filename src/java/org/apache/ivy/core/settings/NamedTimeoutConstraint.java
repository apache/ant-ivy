/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.ivy.core.settings;

import org.apache.ivy.util.StringUtils;

/**
 * An implementation of {@link TimeoutConstraint} which can be identified by a name
 */
public class NamedTimeoutConstraint implements TimeoutConstraint {

    private String name;

    private int connectionTimeout = -1;

    private int readTimeout = -1;

    public NamedTimeoutConstraint() {
    }

    public NamedTimeoutConstraint(final String name) {
        StringUtils.assertNotNullNorEmpty(name, "Name of a timeout constraint cannot be null or empty string");
        this.name = name;
    }

    public void setName(final String name) {
        StringUtils.assertNotNullNorEmpty(name, "Name of a timeout constraint cannot be null or empty string");
        this.name = name;
    }

    /**
     * @return Returns the name of the timeout constraint
     */
    public String getName() {
        return this.name;
    }

    @Override
    public int getConnectionTimeout() {
        return this.connectionTimeout;
    }

    @Override
    public int getReadTimeout() {
        return this.readTimeout;
    }

    /**
     * Sets the connection timeout of this timeout constraint
     * @param connectionTimeout The connection timeout in milliseconds.
     */
    public void setConnectionTimeout(final int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    /**
     * Sets the read timeout of this timeout constraint
     * @param readTimeout The read timeout in milliseconds.
     */
    public void setReadTimeout(final int readTimeout) {
        this.readTimeout = readTimeout;
    }
}
