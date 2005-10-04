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
        if (dest.exists() && !dest.canWrite()) {
            if (overwrite && dest.isFile()) {
                dest.delete();
            } else {
                throw new IOException("impossible to copy: destination is not writable: "+dest);
            }
        }
        copy(new FileInputStream(src), dest, l);
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

}
