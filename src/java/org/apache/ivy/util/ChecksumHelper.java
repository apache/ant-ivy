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
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class ChecksumHelper {

    private static final int BUFFER_SIZE = 2048;

    private static Map algorithms = new HashMap();
    static {
        algorithms.put("md5", "MD5");
        algorithms.put("sha1", "SHA-1");
    }

    /**
     * Checks the checksum of the given file against the given checksumFile, and throws an
     * IOException if the checksum is not compliant
     * 
     * @param dest
     *            the file to test
     * @param checksumFile
     *            the file containing the expected checksum
     * @param algorithm
     *            the checksum algorithm to use
     * @throws IOException
     *             if an IO problem occur whle reading files or if the checksum is not compliant
     */
    public static void check(File dest, File checksumFile, String algorithm) throws IOException {
        String csFileContent = FileUtil
                .readEntirely(new BufferedReader(new FileReader(checksumFile))).trim()
                .toLowerCase(Locale.US);
        String expected;
        if (csFileContent.indexOf(' ') > -1
                && (csFileContent.startsWith("md") || csFileContent.startsWith("sha"))) {
            int lastSpaceIndex = csFileContent.lastIndexOf(' ');
            expected = csFileContent.substring(lastSpaceIndex + 1);
        } else {
            int spaceIndex = csFileContent.indexOf(' ');
            if (spaceIndex != -1) {
                expected = csFileContent.substring(0, spaceIndex);

                // IVY-1155: support some strange formats like this one:
                // https://repo1.maven.org/maven2/org/apache/pdfbox/fontbox/0.8.0-incubator/fontbox-0.8.0-incubator.jar.md5
                if (expected.endsWith(":")) {
                    StringBuffer result = new StringBuffer();
                    char[] chars = csFileContent.substring(spaceIndex + 1).toCharArray();
                    for (int i = 0; i < chars.length; i++) {
                        if (!Character.isWhitespace(chars[i])) {
                            result.append(chars[i]);
                        }
                    }
                    expected = result.toString();
                }
            } else {
                expected = csFileContent;
            }
        }

        String computed = computeAsString(dest, algorithm).trim().toLowerCase(Locale.US);
        if (!expected.equals(computed)) {
            throw new IOException("invalid " + algorithm + ": expected=" + expected + " computed="
                    + computed);
        }
    }

    public static String computeAsString(File f, String algorithm) throws IOException {
        return byteArrayToHexString(compute(f, algorithm));
    }

    private static byte[] compute(File f, String algorithm) throws IOException {
        InputStream is = new FileInputStream(f);

        try {
            MessageDigest md = getMessageDigest(algorithm);
            md.reset();

            byte[] buf = new byte[BUFFER_SIZE];
            int len = 0;
            while ((len = is.read(buf)) != -1) {
                md.update(buf, 0, len);
            }
            return md.digest();
        } finally {
            is.close();
        }
    }

    public static boolean isKnownAlgorithm(String algorithm) {
        return algorithms.containsKey(algorithm);
    }

    private static MessageDigest getMessageDigest(String algorithm) {
        String mdAlgorithm = (String) algorithms.get(algorithm);
        if (mdAlgorithm == null) {
            throw new IllegalArgumentException("unknown algorithm " + algorithm);
        }
        try {
            return MessageDigest.getInstance(mdAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("unknown algorithm " + algorithm);
        }
    }

    // byte to hex string converter
    private static final char[] CHARS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a',
            'b', 'c', 'd', 'e', 'f'};

    /**
     * Convert a byte[] array to readable string format. This makes the "hex" readable!
     * 
     * @return result String buffer in String format
     * @param in
     *            byte[] buffer to convert to string format
     */
    public static String byteArrayToHexString(byte[] in) {
        byte ch = 0x00;

        if (in == null || in.length <= 0) {
            return null;
        }

        StringBuffer out = new StringBuffer(in.length * 2);

        // CheckStyle:MagicNumber OFF
        for (int i = 0; i < in.length; i++) {
            ch = (byte) (in[i] & 0xF0); // Strip off high nibble
            ch = (byte) (ch >>> 4); // shift the bits down
            ch = (byte) (ch & 0x0F); // must do this is high order bit is on!

            out.append(CHARS[ch]); // convert the nibble to a String Character
            ch = (byte) (in[i] & 0x0F); // Strip off low nibble
            out.append(CHARS[ch]); // convert the nibble to a String Character
        }
        // CheckStyle:MagicNumber ON

        return out.toString();
    }

    private ChecksumHelper() {
    }
}
