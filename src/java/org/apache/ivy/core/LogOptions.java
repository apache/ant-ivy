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
package org.apache.ivy.core;

public class LogOptions {
    /**
     * Defaults log settings. Output all usual messages during the resolve process.
     */
    public static final String LOG_DEFAULT = "default";

    /**
     * This log setting disable all usual messages but download ones.
     */
    public static final String LOG_DOWNLOAD_ONLY = "download-only";

    /**
     * This log setting disable all usual messages during the resolve process.
     */
    public static final String LOG_QUIET = "quiet";

    /**
     * The log settings to use. One of {@link #LOG_DEFAULT}, {@link #LOG_DOWNLOAD_ONLY},
     * {@link #LOG_QUIET}
     */
    private String log = LOG_DEFAULT;

    public LogOptions() {
    }

    public LogOptions(LogOptions options) {
        log = options.log;
    }

    public String getLog() {
        return log;
    }

    public LogOptions setLog(String log) {
        this.log = log;
        return this;
    }
}
