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
 * Event reporting a stream copy progression
 */
public class CopyProgressEvent {
    private long totalReadBytes;

    private byte[] buffer;

    private int readBytes;

    public CopyProgressEvent() {
    }

    public CopyProgressEvent(byte[] buffer, int read, long total) {
        update(buffer, read, total);
    }

    public CopyProgressEvent(byte[] buffer, long total) {
        update(buffer, 0, total);
    }

    protected CopyProgressEvent update(byte[] buffer, int read, long total) {
        this.buffer = buffer;
        this.readBytes = read;
        this.totalReadBytes = total;
        return this;
    }

    public long getTotalReadBytes() {
        return totalReadBytes;
    }

    public byte[] getBuffer() {
        return buffer;
    }

    public int getReadBytes() {
        return readBytes;
    }

}
