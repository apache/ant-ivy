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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;
import java.util.jar.Pack200.Unpacker;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

import org.apache.ivy.util.url.URLHandlerRegistry;

/**
 * Utility class used to deal with file related operations, like copy, full reading, symlink, ...
 */
public final class FileUtil {

    private FileUtil() {
        // Utility class
    }

    // according to tests by users, 64kB seems to be a good value for the buffer used during copy
    // further improvements could be obtained using NIO API
    private static final int BUFFER_SIZE = 64 * 1024;

    private static final byte[] EMPTY_BUFFER = new byte[0];

    private static final Pattern ALLOWED_PATH_PATTERN = Pattern.compile("[\\w-./\\\\:~ %\\(\\)]+");

    public static void symlinkInMass(Map<File, File> destToSrcMap, boolean overwrite)
            throws IOException {

        // This pattern could be more forgiving if somebody wanted it to be...
        // ...but this should satisfy 99+% of all needs, without letting unsafe operations be done.
        // If you paths is not supported, you then skip this mass option.
        // NOTE: A space inside the path is allowed (I can't control other programmers who like them
        // in their working directory names)...
        // but trailing spaces on file names will be checked otherwise and refused.
        try {
            StringBuffer sb = new StringBuffer();

            Iterator<Entry<File, File>> keyItr = destToSrcMap.entrySet().iterator();
            while (keyItr.hasNext()) {
                Entry<File, File> entry = keyItr.next();
                File destFile = entry.getKey();
                File srcFile = entry.getValue();
                if (!ALLOWED_PATH_PATTERN.matcher(srcFile.getAbsolutePath()).matches()) {
                    throw new IOException("Unsafe file to 'mass' symlink: '"
                            + srcFile.getAbsolutePath() + "'");
                }
                if (!ALLOWED_PATH_PATTERN.matcher(destFile.getAbsolutePath()).matches()) {
                    throw new IOException("Unsafe file to 'mass' symlink to: '"
                            + destFile.getAbsolutePath() + "'");
                }

                // Add to our buffer of commands
                sb.append("ln -s -f \"" + srcFile.getAbsolutePath() + "\"  \""
                        + destFile.getAbsolutePath() + "\";");
                if (keyItr.hasNext()) {
                    sb.append("\n");
                }
            }

            String commands = sb.toString();
            // Run the buffer of commands we have built.
            Runtime runtime = Runtime.getRuntime();
            Message.verbose("executing \"sh\" of:\n\t" + commands.replaceAll("\n", "\n\t"));
            Process process = runtime.exec("sh");
            OutputStream os = process.getOutputStream();
            os.write(commands.getBytes("UTF-8"));
            os.flush();
            os.close();

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

                throw new IOException("error running ln commands with 'sh':\n" + error);
            }
        } catch (InterruptedException x) {
            Thread.currentThread().interrupt();
        }
    }

    public static void symlink(File src, File dest, CopyProgressListener l, boolean overwrite)
            throws IOException {
        if (!prepareCopy(src, dest, overwrite)) {
            return;
        }
        try {
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

            // check if the creation of the symbolic link was successful
            if (!dest.exists()) {
                throw new IOException("error symlinking: " + dest + " doesn't exists");
            }

            // check if the result is a true symbolic link
            if (dest.getAbsolutePath().equals(dest.getCanonicalPath())) {
                dest.delete(); // just make sure we do delete the invalid symlink!
                throw new IOException("error symlinking: " + dest + " isn't a symlink");
            }
        } catch (IOException e) {
            Message.verbose("symlink failed; falling back to copy", e);
            copy(src, dest, l, overwrite);
        } catch (InterruptedException x) {
            Thread.currentThread().interrupt();
        }
    }

    public static boolean copy(File src, File dest, CopyProgressListener l) throws IOException {
        return copy(src, dest, l, false);
    }

    public static boolean prepareCopy(File src, File dest, boolean overwrite) throws IOException {
        if (src.isDirectory()) {
            if (dest.exists()) {
                if (!dest.isDirectory()) {
                    throw new IOException("impossible to copy: destination is not a directory: "
                            + dest);
                }
            } else {
                dest.mkdirs();
            }
            return true;
        }
        // else it is a file copy
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
        if (dest.getParentFile() != null) {
            dest.getParentFile().mkdirs();
        }
        return true;
    }

    public static boolean copy(File src, File dest, CopyProgressListener l, boolean overwrite)
            throws IOException {
        if (!prepareCopy(src, dest, overwrite)) {
            return false;
        }
        if (src.isDirectory()) {
            return deepCopy(src, dest, l, overwrite);
        }
        // else it is a file copy
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

    public static boolean deepCopy(File src, File dest, CopyProgressListener l, boolean overwrite)
            throws IOException {
        // the list of files which already exist in the destination folder
        List<File> existingChild = Collections.emptyList();
        if (dest.exists()) {
            if (!dest.isDirectory()) {
                // not expected type, remove
                dest.delete();
                // and create a folder
                dest.mkdirs();
                dest.setLastModified(src.lastModified());
            } else {
                // existing folder, gather existing children
                File[] children = dest.listFiles();
                if (children != null) {
                    existingChild = Arrays.asList(children);
                }
            }
        } else {
            dest.mkdirs();
            dest.setLastModified(src.lastModified());
        }
        // copy files one by one
        File[] toCopy = src.listFiles();
        if (toCopy != null) {
            for (int i = 0; i < toCopy.length; i++) {
                // compute the destination file
                File childDest = new File(dest, toCopy[i].getName());
                // if file existing, 'mark' it as taken care of
                existingChild.remove(childDest);
                if (toCopy[i].isDirectory()) {
                    deepCopy(toCopy[i], childDest, l, overwrite);
                } else {
                    copy(toCopy[i], childDest, l, overwrite);
                }
            }
        }
        // some file exist in the destination but not in the source: delete them
        for (int i = 0; i < existingChild.size(); i++) {
            forceDelete(existingChild.get(i));
        }
        return true;
    }

    public static void copy(URL src, File dest, CopyProgressListener l) throws IOException {
        URLHandlerRegistry.getDefault().download(src, dest, l);
    }

    public static void copy(File src, URL dest, CopyProgressListener l) throws IOException {
        URLHandlerRegistry.getDefault().upload(src, dest, l);
    }

    public static void copy(InputStream src, File dest, CopyProgressListener l) throws IOException {
        if (dest.getParentFile() != null) {
            dest.getParentFile().mkdirs();
        }
        copy(src, new FileOutputStream(dest), l);
    }

    public static void copy(InputStream src, OutputStream dest, CopyProgressListener l)
            throws IOException {
        copy(src, dest, l, true);
    }

    public static void copy(InputStream src, OutputStream dest, CopyProgressListener l,
            boolean autoClose) throws IOException {
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

            try {
                dest.flush();
            } catch (IOException ex) {
                // ignore
            }

            // close the streams
            if (autoClose) {
                src.close();
                dest.close();
            }
        } finally {
            if (autoClose) {
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

    /**
     * Recursively delete file
     * 
     * @param file
     *            the file to delete
     * @return true if the deletion completed successfully (ie if the file does not exist on the
     *         filesystem after this call), false if a deletion was not performed successfully.
     */
    public static boolean forceDelete(File file) {
        if (!file.exists()) {
            return true;
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    if (!forceDelete(files[i])) {
                        return false;
                    }
                }
            }
        }
        return file.delete();
    }

    /**
     * Returns a list of Files composed of all directories being parent of file and child of root +
     * file and root themselves. Example: getPathFiles(new File("test"), new
     * File("test/dir1/dir2/file.txt")) => {new File("test/dir1"), new File("test/dir1/dir2"), new
     * File("test/dir1/dir2/file.txt") } Note that if root is not an ancester of file, or if root is
     * null, all directories from the file system root will be returned.
     */
    public static List<File> getPathFiles(File root, File file) {
        List<File> ret = new ArrayList<File>();
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
     * @param dir
     *            The directory from which all files, including files in subdirectory) are
     *            extracted.
     * @param ignore
     *            a Collection of filenames which must be excluded from listing
     * @return A collectoin containing all the files of the given directory and it's subdirectories.
     */
    public static Collection<File> listAll(File dir, Collection<String> ignore) {
        return listAll(dir, new ArrayList<File>(), ignore);
    }

    private static Collection<File> listAll(File file, Collection<File> list,
            Collection<String> ignore) {
        if (ignore.contains(file.getName())) {
            return list;
        }

        if (file.exists()) {
            list.add(file);
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                listAll(files[i], list, ignore);
            }
        }
        return list;
    }

    public static File resolveFile(File file, String filename) {
        File result = new File(filename);
        if (!result.isAbsolute()) {
            result = new File(file, filename);
        }

        return normalize(result.getPath());
    }

    // ////////////////////////////////////////////
    // The following code comes from Ant FileUtils
    // ////////////////////////////////////////////

    /**
     * &quot;Normalize&quot; the given absolute path.
     * 
     * <p>
     * This includes:
     * <ul>
     * <li>Uppercase the drive letter if there is one.</li>
     * <li>Remove redundant slashes after the drive spec.</li>
     * <li>Resolve all ./, .\, ../ and ..\ sequences.</li>
     * <li>DOS style paths that start with a drive letter will have \ as the separator.</li>
     * </ul>
     * Unlike {@link File#getCanonicalPath()} this method specifically does not resolve symbolic
     * links.
     * 
     * @param path
     *            the path to be normalized.
     * @return the normalized version of the path.
     * 
     * @throws java.lang.NullPointerException
     *             if path is null.
     */
    public static File normalize(final String path) {
        Stack<String> s = new Stack<String>();
        String[] dissect = dissect(path);
        s.push(dissect[0]);

        StringTokenizer tok = new StringTokenizer(dissect[1], File.separator);
        while (tok.hasMoreTokens()) {
            String thisToken = tok.nextToken();
            if (".".equals(thisToken)) {
                continue;
            }
            if ("..".equals(thisToken)) {
                if (s.size() < 2) {
                    // Cannot resolve it, so skip it.
                    return new File(path);
                }
                s.pop();
            } else { // plain component
                s.push(thisToken);
            }
        }
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.size(); i++) {
            if (i > 1) {
                // not before the filesystem root and not after it, since root
                // already contains one
                sb.append(File.separatorChar);
            }
            sb.append(s.elementAt(i));
        }
        return new File(sb.toString());
    }

    /**
     * Dissect the specified absolute path.
     * 
     * @param path
     *            the path to dissect.
     * @return String[] {root, remaining path}.
     * @throws java.lang.NullPointerException
     *             if path is null.
     * @since Ant 1.7
     */
    private static String[] dissect(String path) {
        char sep = File.separatorChar;
        path = path.replace('/', sep).replace('\\', sep);

        // // make sure we are dealing with an absolute path
        // if (!isAbsolutePath(path)) {
        // throw new BuildException(path + " is not an absolute path");
        // }
        String root = null;
        int colon = path.indexOf(':');
        if (colon > 0) { // && (ON_DOS || ON_NETWARE)) {

            int next = colon + 1;
            root = path.substring(0, next);
            char[] ca = path.toCharArray();
            root += sep;
            // remove the initial separator; the root has it.
            next = (ca[next] == sep) ? next + 1 : next;

            StringBuffer sbPath = new StringBuffer();
            // Eliminate consecutive slashes after the drive spec:
            for (int i = next; i < ca.length; i++) {
                if (ca[i] != sep || ca[i - 1] != sep) {
                    sbPath.append(ca[i]);
                }
            }
            path = sbPath.toString();
        } else if (path.length() > 1 && path.charAt(1) == sep) {
            // UNC drive
            int nextsep = path.indexOf(sep, 2);
            nextsep = path.indexOf(sep, nextsep + 1);
            root = (nextsep > 2) ? path.substring(0, nextsep + 1) : path;
            path = path.substring(root.length());
        } else {
            root = File.separator;
            path = path.substring(1);
        }
        return new String[] {root, path};
    }

    /**
     * Get the length of the file, or the sum of the children lengths if it is a directory
     * 
     * @param file
     * @return
     */
    public static long getFileLength(File file) {
        long l = 0;
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    l += getFileLength(files[i]);
                }
            }
        } else {
            l = file.length();
        }
        return l;
    }

    public static InputStream unwrapPack200(InputStream packed) throws IOException {
        BufferedInputStream buffered = new BufferedInputStream(packed);
        buffered.mark(4);
        byte[] magic = new byte[4];
        buffered.read(magic, 0, 4);
        buffered.reset();

        InputStream in = buffered;

        if (magic[0] == (byte) 0x1F && magic[1] == (byte) 0x8B && magic[2] == (byte) 0x08) {
            // this is a gziped pack200
            in = new GZIPInputStream(in);
        }

        Unpacker unpacker = Pack200.newUnpacker();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JarOutputStream jar = new JarOutputStream(baos);
        unpacker.unpack(new UncloseInputStream(in), jar);
        jar.close();
        return new ByteArrayInputStream(baos.toByteArray());
    }

    /**
     * Wrap an input stream and do not close the stream on call to close(). Used to avoid closing a
     * {@link ZipInputStream} used with {@link Unpacker#unpack(File, JarOutputStream)}
     */
    private static final class UncloseInputStream extends InputStream {

        private InputStream wrapped;

        public UncloseInputStream(InputStream wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public void close() throws IOException {
            // do not close
        }

        @Override
        public int read() throws IOException {
            return wrapped.read();
        }

        @Override
        public int hashCode() {
            return wrapped.hashCode();
        }

        @Override
        public int read(byte[] b) throws IOException {
            return wrapped.read(b);
        }

        @Override
        public boolean equals(Object obj) {
            return wrapped.equals(obj);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return wrapped.read(b, off, len);
        }

        @Override
        public long skip(long n) throws IOException {
            return wrapped.skip(n);
        }

        @Override
        public String toString() {
            return wrapped.toString();
        }

        @Override
        public int available() throws IOException {
            return wrapped.available();
        }

        @Override
        public void mark(int readlimit) {
            wrapped.mark(readlimit);
        }

        @Override
        public void reset() throws IOException {
            wrapped.reset();
        }

        @Override
        public boolean markSupported() {
            return wrapped.markSupported();
        }

    }
}
