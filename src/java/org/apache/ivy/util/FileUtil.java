/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivy.util;

import org.apache.ivy.core.settings.TimeoutConstraint;
import org.apache.ivy.util.url.TimeoutConstrainedURLHandler;
import org.apache.ivy.util.url.URLHandler;
import org.apache.ivy.util.url.URLHandlerRegistry;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.jar.JarOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

import static java.util.jar.Pack200.newUnpacker;

/**
 * Utility class used to deal with file related operations, like copy, full reading, symlink, ...
 */
public final class FileUtil {

    private FileUtil() {
        // Utility class
    }

    // according to tests by users, 64kB seems to be a good value for the buffer used during copy;
    // further improvements could be obtained using NIO API
    private static final int BUFFER_SIZE = 64 * 1024;

    private static final byte[] EMPTY_BUFFER = new byte[0];

    /**
     * Creates a symbolic link at {@code link} whose target will be the {@code target}. Depending
     * on the underlying filesystem, this method may not always be able to create a symbolic link,
     * in which case this method returns {@code false}.
     *
     * @param target    The {@link File} which will be the target of the symlink being created
     * @param link      The path to the symlink that needs to be created
     * @param overwrite {@code true} if any existing file at {@code link} has to be overwritten.
     *                  False otherwise
     * @return Returns true if the symlink was successfully created. Returns false if the symlink
     * could not be created
     * @throws IOException if {@link Files#createSymbolicLink} fails
     */
    public static boolean symlink(final File target, final File link, final boolean overwrite)
            throws IOException {
        // prepare for symlink
        if (target.isFile()) {
            // it's a file that is being symlinked, so do the necessary preparation
            // for the linking, similar to what we do with preparation for copying
            if (!prepareCopy(target, link, overwrite)) {
                return false;
            }
        } else {
            // it's a directory being symlinked

            // see if the directory represented by the "link" exists and is already a symbolic
            // link. If it is and if we are asked to overwrite then we *only* break the link
            // in preparation of symlink creation, later in this method
            if (Files.isSymbolicLink(link.toPath()) && overwrite) {
                Message.verbose("Un-linking existing symbolic link " + link + " during symlink creation, since overwrite=true");
                Files.delete(link.toPath());
            }
            // make sure the "link" that is being created has the necessary parent directories
            // in place before triggering symlink creation
            if (link.getParentFile() != null) {
                link.getParentFile().mkdirs();
            }
        }
        Files.createSymbolicLink(link.toPath(), target.getAbsoluteFile().toPath());
        return true;
    }

    /**
     * This is the same as calling {@link #copy(File, File, CopyProgressListener, boolean)} with
     * {@code overwrite} param as {@code true}
     *
     * @param src  The source to copy
     * @param dest The destination
     * @param l    A {@link CopyProgressListener}. Can be null
     * @return Returns true if the file was copied. Else returns false
     * @throws IOException If any exception occurs during the copy operation
     */
    public static boolean copy(File src, File dest, CopyProgressListener l) throws IOException {
        return copy(src, dest, l, false);
    }

    public static boolean prepareCopy(final File src, final File dest, final boolean overwrite) throws IOException {
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
            // If overwrite is specified as "true" and the dest file happens to be a symlink,
            // we delete the "link" (a.k.a unlink it). This is for cases like
            // https://issues.apache.org/jira/browse/IVY-1498 where not unlinking the existing
            // symlink can lead to potentially overwriting the wrong "target" file
            // TODO: This behaviour is intentionally hardcoded here for now, since I don't
            // see a reason (yet) to expose it as a param of this method. If any use case arises
            // we can have this behaviour decided by the callers of this method, by passing
            // a value for this param
            final boolean unlinkSymlinkIfOverwrite = true;
            if (!dest.isFile()) {
                throw new IOException("impossible to copy: destination is not a file: " + dest);
            }
            if (overwrite) {
                if (Files.isSymbolicLink(dest.toPath()) && unlinkSymlinkIfOverwrite) {
                    // unlink (a.k.a delete the symlink path)
                    dest.delete();
                } else if (!dest.canWrite()) {
                    // if the file *isn't* "writable" (see javadoc of File.canWrite() on what
                    // that means) we delete it.
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
        // check if it's the same file (the src and the dest). if they are the same, skip the copy
        try {
            if (Files.isSameFile(src.toPath(), dest.toPath())) {
                Message.verbose("Skipping copy of file " + src + " to " + dest + " since they are the same file");
                // we consider the file as copied if overwrite is true
                return overwrite;
            }
        } catch (NoSuchFileException nsfe) {
            // ignore and move on and attempt the copy
        } catch (IOException ioe) {
            // log and move on and attempt the copy
            Message.verbose("Could not determine if " + src + " and dest " + dest + " are the same file", ioe);
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
                    existingChild = new ArrayList<>(Arrays.asList(children));
                }
            }
        } else {
            dest.mkdirs();
            dest.setLastModified(src.lastModified());
        }
        // copy files one by one
        File[] toCopy = src.listFiles();
        if (toCopy != null) {
            for (File cf : toCopy) {
                // compute the destination file
                File childDest = new File(dest, cf.getName());
                // if file existing, 'mark' it as taken care of
                if (!existingChild.isEmpty()) {
                    existingChild.remove(childDest);
                }
                if (cf.isDirectory()) {
                    deepCopy(cf, childDest, l, overwrite);
                } else {
                    copy(cf, childDest, l, overwrite);
                }
            }
        }
        // some file exist in the destination but not in the source: delete them
        for (File child : existingChild) {
            forceDelete(child);
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    public static void copy(final URL src, final File dest, final CopyProgressListener listener,
                            final TimeoutConstraint timeoutConstraint) throws IOException {
        final URLHandler handler = URLHandlerRegistry.getDefault();
        if (handler instanceof TimeoutConstrainedURLHandler) {
            ((TimeoutConstrainedURLHandler) handler).download(src, dest, listener, timeoutConstraint);
            return;
        }
        handler.download(src, dest, listener);
    }

    @SuppressWarnings("deprecation")
    public static void copy(final File src, final URL dest, final CopyProgressListener listener,
                            final TimeoutConstraint timeoutConstraint) throws IOException {
        final URLHandler handler = URLHandlerRegistry.getDefault();
        if (handler instanceof TimeoutConstrainedURLHandler) {
            ((TimeoutConstrainedURLHandler) handler).upload(src, dest, listener, timeoutConstraint);
            return;
        }
        handler.upload(src, dest, listener);
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
            StringBuilder buf = new StringBuilder();

            String line = in.readLine();
            while (line != null) {
                buf.append(line).append("\n");
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
            StringBuilder sb = new StringBuilder();
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
                for (File df : files) {
                    if (!forceDelete(df)) {
                        return false;
                    }
                }
            }
        }
        return file.delete();
    }

    /**
     * Returns a list of Files composed of all directories being parent of file and child of root +
     * file and root themselves. Example: <code>getPathFiles(new File("test"), new
     * File("test/dir1/dir2/file.txt")) =&gt; {new File("test/dir1"), new File("test/dir1/dir2"),
     * new File("test/dir1/dir2/file.txt") }</code> Note that if root is not an ancestor of file, or
     * if root is null, all directories from the file system root will be returned.
     *
     * @param root File
     * @param file File
     * @return List&lt;File&gt;
     */
    public static List<File> getPathFiles(File root, File file) {
        List<File> ret = new ArrayList<>();
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
     * @param dir
     *            The directory from which all files, including files in subdirectory) are
     *            extracted.
     * @param ignore
     *            a Collection of filenames which must be excluded from listing
     * @return a collection containing all the files of the given directory and it's subdirectories,
     *         recursively.
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
            for (File lf : files) {
                listAll(lf, list, ignore);
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
     * @param path the path to be normalized.
     * @return the normalized version of the path.
     * @throws NullPointerException if path is null.
     */
    public static File normalize(final String path) {
        final Stack<String> s = new Stack<>();
        final DissectedPath dissectedPath = dissect(path);
        s.push(dissectedPath.root);

        final StringTokenizer tok = new StringTokenizer(dissectedPath.remainingPath, File.separator);
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
        StringBuilder sb = new StringBuilder();
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
     * @return {@link DissectedPath}
     * @throws java.lang.NullPointerException
     *             if path is null.
     * @since Ant 1.7
     */
    private static DissectedPath dissect(final String path) {
        final char sep = File.separatorChar;
        final String pathToDissect = path.replace('/', sep).replace('\\', sep).trim();

        // check if the path starts with a filesystem root
        final File[] filesystemRoots = File.listRoots();
        if (filesystemRoots != null) {
            for (final File filesystemRoot : filesystemRoots) {
                if (pathToDissect.startsWith(filesystemRoot.getPath())) {
                    // filesystem root is the root and the rest of the path is the "remaining path"
                    final String root = filesystemRoot.getPath();
                    final String rest = pathToDissect.substring(root.length());
                    final StringBuilder sbPath = new StringBuilder();
                    // Eliminate consecutive slashes after the drive spec:
                    for (int i = 0; i < rest.length(); i++) {
                        final char currentChar = rest.charAt(i);
                        if (i == 0) {
                            sbPath.append(currentChar);
                            continue;
                        }
                        final char previousChar = rest.charAt(i - 1);
                        if (currentChar != sep || previousChar != sep) {
                            sbPath.append(currentChar);
                        }
                    }
                    return new DissectedPath(root, sbPath.toString());
                }
            }
        }
        // UNC drive
        if (pathToDissect.length() > 1 && pathToDissect.charAt(1) == sep) {
            int nextsep = pathToDissect.indexOf(sep, 2);
            nextsep = pathToDissect.indexOf(sep, nextsep + 1);
            final String root = (nextsep > 2) ? pathToDissect.substring(0, nextsep + 1) : pathToDissect;
            final String rest = pathToDissect.substring(root.length());
            return new DissectedPath(root, rest);
        }

        return new DissectedPath(File.separator, pathToDissect);
    }

    /**
     * Get the length of the file, or the sum of the children lengths if it is a directory
     *
     * @param file File
     * @return long
     */
    public static long getFileLength(File file) {
        long l = 0;
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File gf : files) {
                    l += getFileLength(gf);
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

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JarOutputStream jar = new JarOutputStream(baos);
        newUnpacker().unpack(new UncloseInputStream(in), jar);
        jar.close();
        return new ByteArrayInputStream(baos.toByteArray());
    }

    /**
     * Wrap an input stream and do not close the stream on call to close(). Used to avoid closing a
     * {@link ZipInputStream} used with {@link Pack200.Unpacker#unpack(File, JarOutputStream)}
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

    private static final class DissectedPath {
        private final String root;

        private final String remainingPath;

        private DissectedPath(final String root, final String remainingPath) {
            this.root = root;
            this.remainingPath = remainingPath;
        }

        @Override
        public String toString() {
            return "Dissected Path [root=" + root + ", remainingPath="
                    + remainingPath + "]";
        }
    }
}
