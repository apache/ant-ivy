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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;

/**
 * Convenient class used only for uncapitalization. Usually use commons lang but here we do not want
 * to have such a dependency for only one feature
 */
public final class StringUtils {

    private StringUtils() {
        // Utility class
    }

    public static String uncapitalize(String string) {
        if (isNullOrEmpty(string)) {
            return string;
        }
        if (string.length() == 1) {
            return string.toLowerCase(Locale.US);
        }
        return string.substring(0, 1).toLowerCase(Locale.US) + string.substring(1);
    }

    /**
     * Returns the error message associated with the given Throwable. The error message returned
     * will try to be as precise as possible, handling cases where e.getMessage() is not meaningful,
     * like {@link NullPointerException} for instance.
     *
     * @param t the throwable to get the error message from
     * @return the error message of the given exception
     */
    public static String getErrorMessage(Throwable t) {
        if (t == null) {
            return "";
        }
        if (t instanceof InvocationTargetException) {
            InvocationTargetException ex = (InvocationTargetException) t;
            t = ex.getTargetException();
        }
        String errMsg = (t instanceof RuntimeException) ? t.getMessage() : t.toString();
        if (isNullOrEmpty(errMsg) || "null".equals(errMsg)) {
            errMsg = t.getClass().getName() + " at " + t.getStackTrace()[0].toString();
        }
        return errMsg;
    }

    /**
     * Returns the exception stack trace as a String.
     *
     * @param e
     *            the exception to get the stack trace from.
     * @return the exception stack trace
     */
    public static String getStackTrace(Throwable e) {
        if (e == null) {
            return "";
        }
        StringWriter sw = new StringWriter();
        PrintWriter printWriter = new PrintWriter(sw, true);
        e.printStackTrace(printWriter);
        return sw.getBuffer().toString();
    }

    @Deprecated
    public static String join(Object[] objs, String sep) {
        for (int i = 0; i < objs.length; i++) {
            if (!(objs[i] instanceof String)) {
                objs[i] = objs[i].toString();
            }
        }
        return joinArray((String[]) objs, sep);
    }

    /**
     * Joins the given string array in one string, each separated by the given separator.
     *
     * Example:
     *
     * <pre>
     * joinArray(new String[] {"one", "two", "three"}, ", ") -&gt; "one, two, three"
     * </pre>
     *
     * @param objs
     *            The array of strings.
     * @param sep
     *            The separator to use.
     * @return The concatenated string.
     */
    public static String joinArray(String[] objs, String sep) {
        StringBuilder buf = new StringBuilder();
        for (String obj : objs) {
            buf.append(obj).append(sep);
        }
        if (objs.length > 0) {
            buf.setLength(buf.length() - sep.length()); // delete sep
        }
        return buf.toString();
    }

    /**
     * Splits the given string into a string array using comma as a separator.
     * Every array member gets {@link String#trim() trimmed}.
     *
     * @param list the string
     * @return String[]
     */
    public static String[] splitToArray(String list) {
        if (list == null) {
            return null;
        }
        return list.trim().split("\\s*,\\s*");
    }

    /**
     * Checks that a string is not null or consists of whitespace by {@link String#trim() trimming}
     * and checking the {@link String#isEmpty() length} of the result.
     *
     * @param s the string to check
     * @return boolean
     */
    public static boolean isNullOrEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    /**
     * Asserts that the passed <code>value</code> is not null and not an empty {@link String}.
     * The implementation of this method {@link String#trim() trims} the (non-null)
     * <code>value</code> to check whether the value is an empty string. If the <code>value</code>
     * is either null or empty, then this method throws an {@link IllegalArgumentException}
     * with the passed <code>errorMessage</code> as the message in the exception.
     *
     * @param value        The value to check for
     * @param errorMessage The error message
     */
    public static void assertNotNullNorEmpty(final String value, final String errorMessage) {
        if (isNullOrEmpty(value)) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    @Deprecated
    public static void assertNotNullNotEmpty(final String value, final String errorMessage) {
        assertNotNullNorEmpty(value, errorMessage);
    }

    // basic string codec (same algo as CVS passfile, inspired by ant CVSPass class
    /**
     * Array containing char conversion data
     */
    private static final char[] SHIFTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16,
            17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 114, 120, 53, 79, 96, 109,
            72, 108, 70, 64, 76, 67, 116, 74, 68, 87, 111, 52, 75, 119, 49, 34, 82, 81, 95, 65,
            112, 86, 118, 110, 122, 105, 41, 57, 83, 43, 46, 102, 40, 89, 38, 103, 45, 50, 42, 123,
            91, 35, 125, 55, 54, 66, 124, 126, 59, 47, 92, 71, 115, 78, 88, 107, 106, 56, 36, 121,
            117, 104, 101, 100, 69, 73, 99, 63, 94, 93, 39, 37, 61, 48, 58, 113, 32, 90, 44, 98,
            60, 51, 33, 97, 62, 77, 84, 80, 85, 223, 225, 216, 187, 166, 229, 189, 222, 188, 141,
            249, 148, 200, 184, 136, 248, 190, 199, 170, 181, 204, 138, 232, 218, 183, 255, 234,
            220, 247, 213, 203, 226, 193, 174, 172, 228, 252, 217, 201, 131, 230, 197, 211, 145,
            238, 161, 179, 160, 212, 207, 221, 254, 173, 202, 146, 224, 151, 140, 196, 205, 130,
            135, 133, 143, 246, 192, 159, 244, 239, 185, 168, 215, 144, 139, 165, 180, 157, 147,
            186, 214, 176, 227, 231, 219, 169, 175, 156, 206, 198, 129, 164, 150, 210, 154, 177,
            134, 127, 182, 128, 158, 208, 162, 132, 167, 209, 149, 241, 153, 251, 237, 236, 171,
            195, 243, 233, 253, 240, 194, 250, 191, 155, 142, 137, 245, 235, 163, 242, 178, 152};

    /**
     * Encrypt the given string in a way which anybody having access to this method algorithm can
     * easily decrypt. This is useful only to avoid clear string storage in a file for example, but
     * shouldn't be considered as a real mean of security. This only works with simple characters
     * (char &lt; 256).
     *
     * @param str
     *            the string to encrypt
     * @return the encrypted version of the string
     */
    public static final String encrypt(String str) {
        if (str == null) {
            return null;
        }
        StringBuilder buf = new StringBuilder();
        for (char c : str.toCharArray()) {
            if (c >= SHIFTS.length) {
                throw new IllegalArgumentException(
                        "encrypt method can only be used with simple characters. '" + c
                                + "' not allowed");
            }
            buf.append(SHIFTS[c]);
        }
        return buf.toString();
    }

    /**
     * Decrypts a string encrypted with encrypt.
     *
     * @param str
     *            the encrypted string to decrypt
     * @return The decrypted string.
     */
    public static final String decrypt(String str) {
        if (str == null) {
            return null;
        }
        StringBuilder buf = new StringBuilder();
        for (char c : str.toCharArray()) {
            buf.append(decrypt(c));
        }
        return buf.toString();
    }

    private static char decrypt(char c) {
        for (char i = 0; i < SHIFTS.length; i++) {
            if (SHIFTS[i] == c) {
                return i;
            }
        }
        throw new IllegalArgumentException("Impossible to decrypt '" + c
                + "'. Unhandled character.");
    }

    public static String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
}
