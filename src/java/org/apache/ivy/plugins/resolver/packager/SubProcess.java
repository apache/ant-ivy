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
package org.apache.ivy.plugins.resolver.packager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Simple utility class for executing subprocesses. Handles I/O streams
 * by closing standard input and relaying standard output and error.
 */
public class SubProcess {

    private static final int BUFSIZE = 1024;

    private final String[] cmd;
    private final String[] env;
    private final File dir;

    /**
     * Constructor.
     *
     * @param cmd command parameters
     * @param env command environment
     * @param dir command working directory
     * @see Runtime.exec(String[], String[], File)
     */
    public SubProcess(String[] cmd, String[] env, File dir) {
        this.cmd = cmd;
        this.env = env;
        this.dir = dir;
    }

    /**
     * Execute the process and wait for it to complete.
     *
     * @return exit value from process
     */
    public int run() throws IOException, InterruptedException {
        Process proc = Runtime.getRuntime().exec(this.cmd, this.env, this.dir);
        proc.getOutputStream().close();
        Thread relay1 = startRelay(proc.getInputStream(), System.out);
        Thread relay2 = startRelay(proc.getErrorStream(), System.err);
        int result;
        relay1.join();
        relay2.join();
        return proc.waitFor();
    }

    /**
     * Create and start a separate thread that copies input to output and closes
     * the input when done.
     *
     * @param in input stream to read from
     * @param out output stream to copy to
     * @return thread doing the work
     */
    public static Thread startRelay(final InputStream in, final OutputStream out) {
        Thread thread = new Thread() {
            public void run() {
                try {
                    relayStream(in, out);
                    in.close();
                } catch (IOException e) {
                    return;
                }
            }
        };
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    /**
     * Copy from input to output. Does not close either stream when finished.
     *
     * @param in input stream to read from
     * @param out output stream to copy to
     */
    public static void relayStream(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[BUFSIZE];
        int r;
        while ((r = in.read(buf)) != -1) {
            out.write(buf, 0, r);
        }
        out.flush();
    }
}

