/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import fr.jayasoft.ivy.url.URLHandlerRegistry;

/**
 * @author x.hanin
 *
 */
public class FileUtil {
    // tried some other values with empty files... seems to be the best one (512 * 1024 is very bad)
    // 8 * 1024 is also the size used by ant in its FileUtils... maybe they've done more study about it ;-)
    private static final int BUFFER_SIZE = 8 * 1024; 
    public static void copy(File src, File dest, CopyProgressListener l) throws IOException {
        copy(src, dest, l, false);
    }
    public static void copy(File src, File dest, CopyProgressListener l, boolean overwrite) throws IOException {
        if (dest.exists()) {
        	if (!dest.isFile()) {
        		throw new IOException("impossible to copy: destination is not a file: "+dest);
        	}
        	if (overwrite) {
        		if (!dest.canWrite()) {
        			dest.delete();
        		} // if dest is writable, the copy will overwrite it without requiring a delete
        	} else {
        		Message.verbose(dest+" already exists, nothing done");
        	}
        }
        copy(new FileInputStream(src), dest, l);
        long srcLen = src.length();
        long destLen = dest.length();
        if (srcLen != destLen) {
        	dest.delete();
        	throw new IOException("size of source file " + src.toString() + "("
        			+ srcLen + ") differs from size of dest file " + dest.toString()
        			+ "(" + destLen + ") - please retry");
        }
        dest.setLastModified(src.lastModified());
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

    public static void copy(InputStream src, OutputStream dest, CopyProgressListener l) throws IOException {
        try {
            CopyProgressEvent evt = null;
            if (l != null) {
                evt = new CopyProgressEvent();
            }
            byte buffer[]=new byte[BUFFER_SIZE];
            int c;
            long total = 0;
            
            if (l != null) {
                l.start(evt);
            }
            while( (c = src.read(buffer)) != -1 ) {
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
                l.end(evt.update(buffer, 0, total));
            }
        } finally {
            try {
                src.close();
            } catch (IOException ex) {
                dest.close();
                throw ex;
            }
            dest.close();
        }
    }

    public static String readEntirely(BufferedReader in) throws IOException {
        StringBuffer buf = new StringBuffer();

        String line = in.readLine();
        while (line != null) {
            buf.append(line + "\n");
            line = in.readLine();
        }
        in.close();
        return buf.toString();
    }
    
    public static String concat(String dir, String file) {
        return dir+"/"+file;
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
     * Returns a list of Files composed of all directories being
     * parent of file and child of root + file and root themselves.
     * 
     * Example:
     * getPathFiles(new File("test"), new File("test/dir1/dir2/file.txt"))
     * => {new File("test/dir1"), new File("test/dir1/dir2"), new File("test/dir1/dir2/file.txt") }
     * 
     * Note that if root is not an ancester of file, or if root is null, all directories from the
     * file system root will be returned. 
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
	 * Returns a collection of all Files being contained in the given directory,
	 * recursively, including directories.
	 * @param dir
	 * @return
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
