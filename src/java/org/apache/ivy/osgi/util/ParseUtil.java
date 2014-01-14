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
package org.apache.ivy.osgi.util;

import java.util.ArrayList;
import java.util.List;

public class ParseUtil {

    /**
     * Parses delimited string and returns an array containing the tokens. This parser obeys quotes,
     * so the delimiter character will be ignored if it is inside of a quote. This method assumes
     * that the quote character is not included in the set of delimiter characters.
     * 
     * @param value
     *            the delimited string to parse.
     * @param delim
     *            the characters delimiting the tokens.
     * @return an array of string tokens or null if there were no tokens.
     */
    // method largely inspired by Apache Felix 1.0.4 ManifestParser method
    public static String[] parseDelimitedString(String value, String delim) {
        if (value == null) {
            value = "";
        }

        final List<String> list = new ArrayList<String>();

        final int CHAR = 1;
        final int DELIMITER = 2;
        final int STARTQUOTE = 4;
        final int ENDQUOTE = 8;

        final StringBuffer sb = new StringBuffer();

        int expecting = (CHAR | DELIMITER | STARTQUOTE);

        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);

            final boolean isDelimiter = (delim.indexOf(c) >= 0);
            final boolean isQuote = (c == '"');

            if (isDelimiter && ((expecting & DELIMITER) > 0)) {
                list.add(sb.toString().trim());
                sb.delete(0, sb.length());
                expecting = (CHAR | DELIMITER | STARTQUOTE);
            } else if (isQuote && ((expecting & STARTQUOTE) > 0)) {
                sb.append(c);
                expecting = CHAR | ENDQUOTE;
            } else if (isQuote && ((expecting & ENDQUOTE) > 0)) {
                sb.append(c);
                expecting = (CHAR | STARTQUOTE | DELIMITER);
            } else if ((expecting & CHAR) > 0) {
                sb.append(c);
            } else {
                throw new IllegalArgumentException("Invalid delimited string: " + value);
            }
        }

        if (sb.length() > 0) {
            list.add(sb.toString().trim());
        }

        return list.toArray(new String[list.size()]);
    }
}
