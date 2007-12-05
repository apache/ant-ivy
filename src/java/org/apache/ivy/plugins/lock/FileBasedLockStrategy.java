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

    /**
     * The locker to use to make file lock attempts.
     * <p>
     * Two implementations of FileLocker are provided below, according to our tests the
     * CreateFileLocker is both performing better and much more reliable than NIOFileLocker.
     * </p>
     */
    private FileLocker locker;
    
    private long timeout = DEFAULT_TIMEOUT;
    
    private Map/*<File, Integer>*/ currentLockCounters = new HashMap();
    
    protected FileBasedLockStrategy() {
        this(new CreateFileLocker(false), false);
    }

    protected FileBasedLockStrategy(boolean debugLocking) {
        this(new CreateFileLocker(debugLocking), debugLocking);
    }

    protected FileBasedLockStrategy(FileLocker locker, boolean debugLocking) {
        super(debugLocking);
        this.locker = locker;
    }

        
    protected boolean acquireLock(File file) throws InterruptedException {
        if (isDebugLocking()) {
            debugLocking("acquiring lock on " + file);
        }
        long start = System.currentTimeMillis();
        do {
            synchronized (this) {
                if (hasLock(file)) {
                    int holdLocks = incrementLock(file);
                    if (isDebugLocking()) {
                        debugLocking("reentrant lock acquired on " + file 
                            + " in " + (System.currentTimeMillis() - start) + "ms"
                            + " - hold locks = " + holdLocks);
                    }
                    return true;
                }
                if (locker.tryLock(file)) {
                    if (isDebugLocking()) {
                        debugLocking("lock acquired on " + file 
                            + " in " + (System.currentTimeMillis() - start) + "ms");
                    }
                    incrementLock(file);
                    return true;
                }
            }
            Thread.sleep(SLEEP_TIME);
        } while (System.currentTimeMillis() - start < timeout);
        return false;
    }

    protected void releaseLock(File file) {
        synchronized (this) {
            int holdLocks = decrementLock(file);
            if (holdLocks == 0) {
                locker.unlock(file);
                if (isDebugLocking()) {
                    debugLocking("lock released on " + file);
                }
            } else {
                if (isDebugLocking()) {
                    debugLocking("reentrant lock released on " + file 
                        + " - hold locks = " + holdLocks);
                }                
            }
        }
    }

    
    private static void debugLocking(String msg) {
        Message.info(Thread.currentThread() + " " + System.currentTimeMillis() + " " + msg);
    }

    private boolean hasLock(File file) {
        Integer c = (Integer) currentLockCounters.get(file);
        return c != null && c.intValue() > 0;
    }
    
    private int incrementLock(File file) {
        Integer c = (Integer) currentLockCounters.get(file);
        int holdLocks = c == null ? 1 : c.intValue() + 1;
        currentLockCounters.put(file, new Integer(holdLocks));
        return holdLocks;
    }

    private int decrementLock(File file) {
        Integer c = (Integer) currentLockCounters.get(file);
        int dc = c == null ? 0 : c.intValue() - 1;
        currentLockCounters.put(file, new Integer(dc));
        return dc;
    }

    public static interface FileLocker {
        boolean tryLock(File f);
        void unlock(File f);
    }
    
    /**
     * "locks" a file by creating it if it doesn't exist, relying on the
     * {@link File#createNewFile()} atomicity.
     */
    public static class CreateFileLocker implements FileLocker {
        private boolean debugLocking;

        public CreateFileLocker(boolean debugLocking) {
            this.debugLocking = debugLocking;
        }

        public boolean tryLock(File file) {
            try {
                if (file.getParentFile().exists() || file.getParentFile().mkdirs()) {
                    if (file.createNewFile()) {
                        return true;
                    } else {
                        if (debugLocking) {
                            debugLocking("file creation failed " + file);
                        }
                    }
                }
            } catch (IOException e) {
                // ignored
                Message.verbose("file creation failed due to an exception: " 
                    + e.getMessage() + " (" + file + ")");
            }
            return false;
        }

        public void unlock(File file) {
            file.delete();
        }
    }
    /**
     * Locks a file using the {@link FileLock} mechanism. 
     */
    public static class NIOFileLocker implements FileLocker {
        
        private Map locks = new HashMap();
        private boolean debugLocking;
        
        public NIOFileLocker(boolean debugLocking) {
            this.debugLocking = debugLocking;
        }

        public boolean tryLock(File file) {
            try {
                if (file.getParentFile().exists() || file.getParentFile().mkdirs()) {
                    RandomAccessFile raf =
                        new RandomAccessFile(file, "rw");            
                    FileChannel channel = raf.getChannel();
                    try {
                        FileLock l = channel.tryLock();
                        if (l != null) {
                            synchronized (this) {
                                locks.put(file, l);
                            }
                            return true;
                        } else {
                            if (debugLocking) {
                                debugLocking("failed to acquire lock on " + file);
                            }
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
            return false;
        }

        public void unlock(File file) {
            synchronized (this) {
                FileLock l = (FileLock) locks.get(file);
                if (l == null) {
                    throw new IllegalArgumentException("file not previously locked: " + file);
                }
                try {
                    l.release();
                } catch (IOException e) {
                    Message.error(
                        "problem while releasing lock on " + file + ": " + e.getMessage());
                }
            }
        }
        
    }
}
