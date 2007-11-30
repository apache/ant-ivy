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
package org.apache.ivy.plugins.lock;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.HashMap;
import java.util.Map;

import org.apache.ivy.util.Message;

public abstract class FileBasedLockStrategy extends AbstractLockStrategy {
    private static final int SLEEP_TIME = 100;

    private static final long DEFAULT_TIMEOUT = 2 * 60 * 1000;

    private static final String CREATE_FILE = "create-file";
    private static final String LOCK_FILE = "lock-file";
    
    private String fileLockMethod = CREATE_FILE;
    private long timeout = DEFAULT_TIMEOUT;
    
    private Map locks = new HashMap();
    
    protected boolean acquireLock(File file) throws InterruptedException {
        Message.debug("acquiring lock on " + file);
        long start = System.currentTimeMillis();
        if (CREATE_FILE.equals(fileLockMethod)) {
            do {
                try {
                    if (file.getParentFile().exists() || file.getParentFile().mkdirs()) {
                        if (file.createNewFile()) {
                            Message.debug("lock acquired on " + file);
                            return true;
                        } else {
                            Message.debug("file creation failed " + file);
                        }
                    }
                } catch (IOException e) {
                    // ignored
                    Message.verbose("file creation failed due to an exception: " 
                        + e.getMessage() + " (" + file + ")");
                }
                Thread.sleep(SLEEP_TIME);
            } while (System.currentTimeMillis() - start < timeout);
            return false;
        } else if (LOCK_FILE.equals(fileLockMethod)) {
            do {
                try {
                    if (file.getParentFile().exists() || file.getParentFile().mkdirs()) {
                        RandomAccessFile raf =
                            new RandomAccessFile(file, "rw");            
                        FileChannel channel = raf.getChannel();
                        try {
                            FileLock l = channel.tryLock();
                            if (l != null) {
                                locks.put(file, l);
                                Message.debug("lock acquired on " + file);
                                return true;
                            }
                        } finally {
                            raf.close();
                        }
                    }
                } catch (IOException e) {
                    // ignored
                    Message.verbose("file lock failed due to an exception: " 
                        + e.getMessage() + " (" + file + ")");
                }
                Thread.sleep(SLEEP_TIME);
            } while (System.currentTimeMillis() - start < timeout);
            return false;
        } else {
            throw new IllegalStateException("unknown lock method " + fileLockMethod);
        }
    }
    
    protected void releaseLock(File file) {
        if (CREATE_FILE.equals(fileLockMethod)) {
            file.delete();
            Message.debug("lock released on " + file);
        } else if (LOCK_FILE.equals(fileLockMethod)) {
            FileLock l = (FileLock) locks.get(file);
            if (l == null) {
                throw new IllegalArgumentException("file not previously locked: " + file);
            }
            try {
                l.release();
            } catch (IOException e) {
                Message.error("problem while releasing lock on " + file + ": " + e.getMessage());
            }
        } else {
            throw new IllegalStateException("unknown lock method " + fileLockMethod);
        }
    }

}
