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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.ivy.util.url.URLHandlerRegistry;

/**
 * Utility class used to deal with file related operations, like copy, full reading, symlink, ...
 */
public final class FileUtil {
    
    private FileUtil() {
        //Utility class
    }
    
    // according to tests by users, 64kB seems to be a good value for the buffer used during copy
    // further improvements could be obtained using NIO API
    private static final int BUFFER_SIZE = 64 * 1024;

    private static final byte[] EMPTY_BUFFER = new byte[0];

    public static void symlink(File src, File dest, CopyProgressListener l, boolean overwrite)
            throws IOException {
        try {
            if (dest.exists()) {
                if (!dest.isFile()) {
                    throw new IOException("impossible to copy: destination is not a file: " + dest);
                }
                if (!overwrite) {
                    Message.verbose(dest + " already exists, nothing done");
                    return;
                }
            }
            if (dest.getParentFile() != null) {
                dest.getParentFile().mkdirs();
            }

            Runtime runtime = Runtime.getRuntime();
            Message.verbose("executing 'ln -s -f " + src.getAbsolutePath() + " " + dest.getPath()
                    + "'");
            Process process = runtime.exec(new String[] {"ln", "-s", "-f", src.getAbsolutePath(),
                    dest.getPath()});

            if (process.waitFor() != 0) {
                InputStream errorStream = process.getErrorStream();
                InputStreamReader isr = new InputStreamReader(errorStream);
                BufferedReader br = new BufferedReader(isr);

                StringBuffer error = new StringBuffer();
                String line;
                while ((line = br.readLine()) != null) {
                    error.append(line);
                    error.append('\n');
                }

                throw new IOException("error symlinking " + src + " to " + dest + ":\n" + error);
            }
            if (!dest.exists()) {
                throw new IOException("error symlinking " + dest + " doesn't exists"); 
            }
        } catch (IOException x) {
            Message.verbose("symlink failed; falling back to copy");
            StringWriter buffer = new StringWriter();
            x.printStackTrace(new PrintWriter(buffer));
            Message.debug(buffer.toString());
            copy(src, dest, l, overwrite);
        } catch (InterruptedException x) {
            Thread.currentThread().interrupt();
        }
    }

    public static boolean copy(File src, File dest, CopyProgressListener l) throws IOException {
        return copy(src, dest, l, false);
    }

    public static boolean copy(File src, File dest, CopyProgressListener l, boolean overwrite)
            throws IOException {
        if (dest.exists()) {
            if (!dest.isFile()) {
                throw new IOException("impossible to copy: destination is not a file: " + dest);
            }
            if (overwrite) {
                if (!dest.canWrite()) {
                    dest.delete();
                } // if dest is writable, the copy will overwrite it without requiring a delete
            } else {
                Message.verbose(dest + " already exists, nothing done");
                return false;
            }
        }
        copy(new FileInputStream(src), dest, l);
        long srcLen = src.length();
        long destLen = dest.length();
        if (srcLen != destLen) {
            dest.delete();
            throw new IOException("size of source file " + src.toString() + "(" + srcLen
                    + ") differs from size of dest file " + dest.toString() + "(" + destLen
                    + ") - please retry");
        }
        dest.setLastModified(src.lastModified());
        return true;
    }

    public static void copy(URL src, File dest, CopyProgressListener l) throws IOException {
        URLHandlerRegistry.getDefault().download(src, dest, l);
    }

    public static void copy(InputStream src, File dest, CopyProgressListener l) throws IOException {
        if (dest.getParentFile() != null) {
            dest.getParentFile().mkdirs();
        }
        copy(src, new FileOutputStream(dest), l);
    }

    public static void copy(InputStream src, OutputStream dest, CopyProgressListener l)
            throws IOException {
        CopyProgressEvent evt = null;
        if (l != null) {
            evt = new CopyProgressEvent();
        }
        try {
            byte[] buffer = new byte[BUFFER_SIZE];
            int c;
            long total = 0;

            if (l != null) {
                l.start(evt);
            }
            while ((c = src.read(buffer)) != -1) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new IOException("transfer interrupted");
                }
                dest.write(buffer, 0, c);
                total += c;
                if (l != null) {
                    l.progress(evt.update(buffer, c, total));
                }
            }

            if (l != null) {
                evt.update(EMPTY_BUFFER, 0, total);
            }

            // close the streams
            src.close();
            dest.close();
        } finally {
            try {
                src.close();
            } catch (IOException ex) {
                // ignore
            }
            try {
                dest.close();
            } catch (IOException ex) {
                // ignore
            }
        }

        if (l != null) {
            l.end(evt);
        }
    }

    /**
     * Reads the whole BufferedReader line by line, using \n as line separator for each line.
     * <p>
     * Note that this method will add a final \n to the last line even though there is no new line
     * character at the end of last line in the original reader.
     * </p>
     * <p>
     * The BufferedReader is closed when this method returns.
     * </p>
     * 
     * @param in
     *            the {@link BufferedReader} to read from
     * @return a String with the whole content read from the {@link BufferedReader}
     * @throws IOException
     *             if an IO problems occur during reading
     */
    public static String readEntirely(BufferedReader in) throws IOException {
        try {
            StringBuffer buf = new StringBuffer();

            String line = in.readLine();
            while (line != null) {
                buf.append(line + "\n");
                line = in.readLine();
            }
            return buf.toString();
        } finally {
            in.close();
        }
    }

    /**
     * Reads the entire content of the file and returns it as a String.
     * 
     * @param f
     *            the file to read from
     * @return a String with the file content
     * @throws IOException
     *             if an IO problems occurs during reading
     */
    public static String readEntirely(File f) throws IOException {
        return readEntirely(new FileInputStream(f));
    }
    
    /**
     * Reads the entire content of the {@link InputStream} and returns it as a String.
     * <p>
     * The input stream is closed when this method returns.
     * </p>
     * 
     * @param is
     *            the {@link InputStream} to read from
     * @return a String with the input stream content
     * @throws IOException
     *             if an IO problems occurs during reading
     */
    public static String readEntirely(InputStream is) throws IOException {
        try {
            StringBuffer sb = new StringBuffer();
            byte[] buffer = new byte[BUFFER_SIZE];
            int c;

            while ((c = is.read(buffer)) != -1) {
                sb.append(new String(buffer, 0, c));
            }
            return sb.toString();
        } finally {
            is.close();
        }
    }

    public static String concat(String dir, String file) {
        return dir + "/" + file;
    }

    public static void forceDelete(File f) {
        if (f.isDirectory()) {
            File[] sub = f.listFiles();
            for (int i = 0; i < sub.length; i++) {
                forceDelete(sub[i]);
            }
        }
        f.delete();
    }

    /**
     * Returns a list of Files composed of all directories being parent of file and child of root +
     * file and root themselves. Example: getPathFiles(new File("test"), new
     * File("test/dir1/dir2/file.txt")) => {new File("test/dir1"), new File("test/dir1/dir2"), new
     * File("test/dir1/dir2/file.txt") } Note that if root is not an ancester of file, or if root is
     * null, all directories from the file system root will be returned.
     */
    public static List getPathFiles(File root, File file) {
        List ret = new ArrayList();
        while (file != null && !file.getAbsolutePath().equals(root.getAbsolutePath())) {
            ret.add(file);
            file = file.getParentFile();
        }
        if (root != null) {
            ret.add(root);
        }
        Collections.reverse(ret);
        return ret;
    }

    /**
     * Returns a collection of all Files being contained in the given directory, recursively,
     * including directories.
     * 
     * @param  dir  The directory from which all files, including files in subdirectory)
     *              are extracted.
     * @return  A collectoin containing all the files of the given directory and it's
     *              subdirectories.
     */
    public static Collection listAll(File dir) {
        return listAll(dir, new ArrayList());
    }

    private static Collection listAll(File file, Collection list) {
        if (file.exists()) {
            list.add(file);
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                listAll(files[i], list);
            }
        }
        return list;
    }

}
