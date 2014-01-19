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
package org.apache.ivy.core.report;

/**
 *
 */
public final class DownloadStatus {
    private String name;

    private DownloadStatus(String name) {
        this.name = name;
    }

    /**
     * means that download was not required
     */
    public static final DownloadStatus NO = new DownloadStatus("no");

    public static final DownloadStatus SUCCESSFUL = new DownloadStatus("successful");

    public static final DownloadStatus FAILED = new DownloadStatus("failed");

    /**
     * Returns the {@link DownloadStatus} corresponding to the given String representation.
     * 
     * @return the {@link DownloadStatus} corresponding to the given String representation.
     * @throws IllegalArgumentException
     *             if the given String does not correspond to any {@link DownloadStatus}.
     */
    public static final DownloadStatus fromString(String status) {
        if (NO.name.equals(status)) {
            return NO;
        }
        if (SUCCESSFUL.name.equals(status)) {
            return SUCCESSFUL;
        }
        if (FAILED.name.equals(status)) {
            return FAILED;
        }
        throw new IllegalArgumentException("unknown download status '" + status + "'");
    }

    public String toString() {
        return name;
    }
}
