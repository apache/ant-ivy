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
import java.nio.channels.FileLock;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.ivy.util.Message;

public abstract class FileBasedLockStrategy extends AbstractLockStrategy {
    private static final int SLEEP_TIME = 100;

    private static final long DEFAULT_TIMEOUT = 2 * 60 * 1000;

    /**
     * The locker to use to make file lock attempts.
     */
    private FileLocker locker;

    private long timeout = DEFAULT_TIMEOUT;

    /**
     * Lock counter list must be static: locks are implicitly shared to the entire process, so the
     * list too much be.
     */
    private static ConcurrentMap/* <File, Map<Thread, Integer>> */currentLockHolders = new ConcurrentHashMap();

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
        Thread currentThread = Thread.currentThread();
        if (isDebugLocking()) {
            debugLocking("acquiring lock on " + file);
        }
        long start = System.currentTimeMillis();
        do {
            synchronized (currentLockHolders) {
                if (isDebugLocking()) {
                    debugLocking("entered synchronized area (locking)");
                }
                int lockCount = hasLock(file, currentThread);
                if (isDebugLocking()) {
                    debugLocking("current status for " + file + " is " + lockCount
                            + " held locks: " + getCurrentLockHolderNames(file));
                }
                if (lockCount < 0) {
                    /* Another thread in this process holds the lock; we need to wait */
                    if (isDebugLocking()) {
                        debugLocking("waiting for another thread to release the lock: "
                                + getCurrentLockHolderNames(file));
                    }
                } else if (lockCount > 0) {
                    int holdLocks = incrementLock(file, currentThread);
                    if (isDebugLocking()) {
                        debugLocking("reentrant lock acquired on " + file + " in "
                                + (System.currentTimeMillis() - start) + "ms" + " - hold locks = "
                                + holdLocks);
                    }
                    return true;
                } else {
                    /* No prior lock on this file is held at all */
                    if (locker.tryLock(file)) {
                        if (isDebugLocking()) {
                            debugLocking("lock acquired on " + file + " in "
                                    + (System.currentTimeMillis() - start) + "ms");
                        }
                        incrementLock(file, currentThread);
                        return true;
                    }
                }
            }
            if (isDebugLocking()) {
                debugLocking("failed to acquire lock; sleeping for retry...");
            }
            Thread.sleep(SLEEP_TIME);
        } while (System.currentTimeMillis() - start < timeout);
        return false;
    }

    protected void releaseLock(File file) {
        Thread currentThread = Thread.currentThread();
        if (isDebugLocking()) {
            debugLocking("releasing lock on " + file);
        }
        synchronized (currentLockHolders) {
            if (isDebugLocking()) {
                debugLocking("entered synchronized area (unlocking)");
            }
            int holdLocks = decrementLock(file, currentThread);
            if (holdLocks == 0) {
                locker.unlock(file);
                if (isDebugLocking()) {
                    debugLocking("lock released on " + file);
                }
            } else {
                if (isDebugLocking()) {
                    debugLocking("reentrant lock released on " + file + " - hold locks = "
                            + holdLocks);
                }
            }
        }
    }

    private static void debugLocking(String msg) {
        Message.info(Thread.currentThread() + " " + System.currentTimeMillis() + " " + msg);
    }

    /**
     * Determine the state of the lockfile.
     * 
     * Must be called from within a synchronized block.
     * 
     * Three possibilities exist: - The lock is held by the current thread (>0) - The lock is held
     * by one or more different threads (-1) - The lock is not held at all (0).
     * 
     * @param file
     *            file to lock
     * @param forThread
     *            thread for which lock status is being queried
     */
    private int hasLock(File file, Thread forThread) {
        Map locksPerThread = (Map) currentLockHolders.get(file);
        if (locksPerThread == null) {
            return 0;
        }
        if (locksPerThread.isEmpty()) {
            return 0;
        }
        Integer counterObj = (Integer) locksPerThread.get(forThread);
        int counter = counterObj == null ? 0 : counterObj.intValue();
        if (counter > 0) {
            return counter;
        } else {
            return -1;
        }
    }

    /**
     * Record that this thread holds the lock.
     * 
     * Asserts that the lock has been previously grabbed by this thread. Must be called from a
     * synchronized block in which the lock was grabbed.
     * 
     * @param file
     *            file which has been locked
     * @param forThread
     *            thread for which locking occurred
     * @return number of times this thread has grabbed the lock
     */
    private int incrementLock(File file, Thread forThread) {
        Map locksPerThread = (Map) currentLockHolders.get(file);
        if (locksPerThread == null) {
            locksPerThread = new ConcurrentHashMap();
            currentLockHolders.put(file, locksPerThread);
        }
        Integer c = (Integer) locksPerThread.get(forThread);
        int holdLocks = c == null ? 1 : c.intValue() + 1;
        locksPerThread.put(forThread, new Integer(holdLocks));
        return holdLocks;
    }

    /**
     * Decrease depth of this thread's lock.
     * 
     * Must be called within a synchronized block.
     * 
     * If this returns 0, the caller is responsible for releasing the lock within that same block.
     * 
     * @param file
     *            file for which lock depth is being decreased
     * @param forThread
     *            thread for which lock depth is being decreased
     * @return remaining depth of this lock
     */
    private int decrementLock(File file, Thread forThread) {
        ConcurrentHashMap locksPerThread = (ConcurrentHashMap) currentLockHolders.get(file);
        if (locksPerThread == null) {
            throw new RuntimeException("Calling decrementLock on a thread which holds no locks");
        }
        Integer c = (Integer) locksPerThread.get(forThread);
        int oldHeldLocks = c == null ? 0 : c.intValue();
        if (oldHeldLocks <= 0) {
            throw new RuntimeException("Calling decrementLock on a thread which holds no locks");
        }
        int newHeldLocks = oldHeldLocks - 1;
        if (newHeldLocks > 0) {
            locksPerThread.put(forThread, new Integer(newHeldLocks));
        } else {
            locksPerThread.remove(forThread);
        }
        return newHeldLocks;
    }

    /**
     * Return a string naming the threads which currently hold this lock.
     */
    protected String getCurrentLockHolderNames(File file) {
        StringBuilder sb = new StringBuilder();
        ConcurrentHashMap m = (ConcurrentHashMap) currentLockHolders.get(file);
        if (m == null) {
            return "(NULL)";
        }
        Enumeration threads = m.keys();
        while (threads.hasMoreElements()) {
            Thread t = (Thread) threads.nextElement();
            sb.append(t.toString());
            if (threads.hasMoreElements()) {
                sb.append(", ");
            }
        }
        return sb.toString();
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
                        DeleteOnExitHook.add(file);
                        return true;
                    } else {
                        if (debugLocking) {
                            debugLocking("file creation failed " + file);
                        }
                    }
                }
            } catch (IOException e) {
                // ignored
                Message.verbose("file creation failed due to an exception: " + e.getMessage()
                        + " (" + file + ")");
            }
            return false;
        }

        public void unlock(File file) {
            file.delete();
            DeleteOnExitHook.remove(file);
        }
    }

    /**
     * Locks a file using the {@link FileLock} mechanism.
     */
    public static class NIOFileLocker implements FileLocker {

        private Map locks = new ConcurrentHashMap();

        private boolean debugLocking;

        public NIOFileLocker(boolean debugLocking) {
            this.debugLocking = debugLocking;
        }

        private static class LockData {
            private RandomAccessFile raf;

            private FileLock l;

            LockData(RandomAccessFile raf, FileLock l) {
                this.raf = raf;
                this.l = l;
            }
        }

        public boolean tryLock(File file) {
            try {
                if (file.getParentFile().exists() || file.getParentFile().mkdirs()) {
                    // this must not be closed until unlock
                    RandomAccessFile raf = new RandomAccessFile(file, "rw");
                    FileLock l = raf.getChannel().tryLock();
                    if (l != null) {
                        synchronized (this) {
                            locks.put(file, new LockData(raf, l));
                        }
                        return true;
                    } else {
                        if (debugLocking) {
                            debugLocking("failed to acquire lock on " + file);
                        }
                    }
                }
            } catch (IOException e) {
                // ignored
                Message.verbose("file lock failed due to an exception: " + e.getMessage() + " ("
                        + file + ")");
            }
            return false;
        }

        public void unlock(File file) {
            synchronized (this) {
                LockData data = (LockData) locks.get(file);
                if (data == null) {
                    throw new IllegalArgumentException("file not previously locked: " + file);
                }

                try {
                    locks.remove(file);
                    data.l.release();
                    data.raf.close();
                } catch (IOException e) {
                    Message.error("problem while releasing lock on " + file + ": " + e.getMessage());
                }
            }
        }

    }
}
