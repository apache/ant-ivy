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
package org.apache.ivy.plugins.repository;

import org.apache.ivy.util.CopyProgressEvent;
import org.apache.ivy.util.CopyProgressListener;

public class RepositoryCopyProgressListener implements CopyProgressListener {
    private final AbstractRepository repository;

    public RepositoryCopyProgressListener(AbstractRepository repository) {
        this.repository = repository;
    }

    private Long totalLength = null;

    public void start(CopyProgressEvent evt) {
        if (totalLength != null) {
            repository.fireTransferStarted(totalLength.longValue());
        } else {
            repository.fireTransferStarted();
        }
    }

    public void progress(CopyProgressEvent evt) {
        repository.fireTransferProgress(evt.getReadBytes());
    }

    public void end(CopyProgressEvent evt) {
        repository.fireTransferProgress(evt.getReadBytes());
        repository.fireTransferCompleted();
    }

    public Long getTotalLength() {
        return totalLength;
    }

    public void setTotalLength(Long totalLength) {
        this.totalLength = totalLength;
    }
}
