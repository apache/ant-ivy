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
 * @author x.hanin
 *
 */
public class DownloadStatus {
    private String _name;
    private DownloadStatus(String name) {
        _name = name;
    }
    
    /**
     * means that download was not required
     */
    public static final DownloadStatus NO = new DownloadStatus("no");
    public static final DownloadStatus SUCCESSFUL = new DownloadStatus("successful");
    public static final DownloadStatus FAILED = new DownloadStatus("failed");
    public String toString() {
        return _name;
    }
}
