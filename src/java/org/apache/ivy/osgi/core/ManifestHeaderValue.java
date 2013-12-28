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
package org.apache.ivy.osgi.core;

import java.io.PrintStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Parse a header of a manifest. The manifest header is composed with the following rules:
 * 
 * <pre>
 * header ::= header-element (',' header-element)*
 * header-element ::= values (';' (attribute | directive) )*
 * values ::= value (';' value)*
 * value ::= &lt;any string value that does not have ';' or ','&gt;
 * attribute ::= key '=' value
 * directive ::= key '=' value
 * key ::= token
 * value ::= token | quoted-string | double-quoted-string
 * </pre>
 */
public class ManifestHeaderValue {

    private List<ManifestHeaderElement> elements = new ArrayList<ManifestHeaderElement>();

    ManifestHeaderValue() {
        // just for unit testing
    }

    public ManifestHeaderValue(String header) throws ParseException {
        if (header != null) {
            new ManifestHeaderParser(header).parse();
        }
    }

    public List<ManifestHeaderElement> getElements() {
        return elements;
    }

    public String getSingleValue() {
        if (elements.isEmpty()) {
            return null;
        }
        List<String> values = getElements().iterator().next().getValues();
        if (values.isEmpty()) {
            return null;
        }
        return values.iterator().next();
    }

    public List<String> getValues() {
        if (elements.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> list = new ArrayList<String>();
        for (ManifestHeaderElement element : getElements()) {
            list.addAll(element.getValues());
        }
        return list;
    }

    void addElement(ManifestHeaderElement element) {
        this.elements.add(element);
    }

    class ManifestHeaderParser {

        /**
         * header to parse
         */
        private final String header;

        /**
         * the length of the source
         */
        private int length;

        /**
         * buffer
         */
        private StringBuffer buffer = new StringBuffer();

        /**
         * position in the source
         */
        private int pos = 0;

        /**
         * last read character
         */
        private char c;

        /**
         * the header element being build
         */
        private ManifestHeaderElement elem = new ManifestHeaderElement();

        /**
         * Once at true (at the first attribute parsed), only parameters are allowed
         */
        private boolean valuesParsed;

        /**
         * the last parsed parameter name
         */
        private String paramName;

        /**
         * true if the last parsed parameter is a directive (assigned via :=)
         */
        private boolean isDirective;

        /**
         * Default constructor
         * 
         * @param header
         *            the header to parse
         */
        ManifestHeaderParser(String header) {
            this.header = header;
            this.length = header.length();
        }

        /**
         * Do the parsing
         * 
         * @throws ParseException
         */
        void parse() throws ParseException {
            do {
                elem = new ManifestHeaderElement();
                int posElement = pos;
                parseElement();
                if (elem.getValues().isEmpty()) {
                    error("No defined value", posElement);
                    // try to recover: ignore that element
                    continue;
                }
                addElement(elem);
            } while (pos < length);
        }

        private char readNext() {
            if (pos == length) {
                c = '\0';
            } else {
                c = header.charAt(pos++);
            }
            return c;
        }

        private void error(String message) throws ParseException {
            error(message, pos - 1);
        }

        private void error(String message, int p) throws ParseException {
            throw new ParseException(message, p);
        }

        private void parseElement() throws ParseException {
            valuesParsed = false;
            do {
                parseValueOrParameter();
            } while (c == ';' && pos < length);
        }

        private void parseValueOrParameter() throws ParseException {
            // true if the value/parameter parsing has started, white spaces skipped
            boolean start = false;
            do {
                switch (readNext()) {
                    case '\0':
                        break;
                    case ';':
                    case ',':
                        endValue();
                        return;
                    case ':':
                    case '=':
                        endParameterName();
                        parseSeparator();
                        parseParameterValue();
                        return;
                    case ' ':
                    case '\t':
                    case '\n':
                    case '\r':
                        if (start) {
                            buffer.append(c);
                        }
                        break;
                    default:
                        start = true;
                        buffer.append(c);
                }
            } while (pos < length);
            endValue();
        }

        private void endValue() throws ParseException {
            if (valuesParsed) {
                error("Early end of a parameter");
                // try to recover: ignore it
                buffer.setLength(0);
                return;
            }
            if (buffer.length() == 0) {
                error("Empty value");
                // try to recover: just ignore the error
            }
            elem.addValue(buffer.toString());
            buffer.setLength(0);
        }

        private void endParameterName() throws ParseException {
            if (buffer.length() == 0) {
                error("Empty parameter name");
                // try to recover: won't store the value
                paramName = null;
            }
            paramName = buffer.toString();
            buffer.setLength(0);
        }

        private void parseSeparator() throws ParseException {
            if (c == '=') {
                isDirective = false;
                return;
            }
            if (readNext() != '=') {
                error("Expecting '='");
                // try to recover: will ignore this parameter
                pos--;
                paramName = null;
            }
            isDirective = true;
        }

        private void parseParameterValue() throws ParseException {
            // true if the value parsing has started, white spaces skipped
            boolean start = false;
            // true if the value parsing is ended, then only white spaces are allowed
            boolean end = false;
            boolean doubleQuoted = false;
            do {
                switch (readNext()) {
                    case '\0':
                        break;

                    case ',':
                    case ';':
                        endParameterValue();
                        return;
                    case '=':
                    case ':':
                        error("Illegal character '" + c + "' in parameter value of " + paramName);
                        // try to recover: ignore that parameter
                        paramName = null;
                        break;
                    case '\"':
                        doubleQuoted = true;
                    case '\'':
                        if (end && paramName != null) {
                            error("Expecting the end of a parameter value");
                            // try to recover: ignore that parameter
                            paramName = null;
                        }
                        if (start) {
                            // quote in the middle of the value, just add it as a quote
                            buffer.append(c);
                        } else {
                            start = true;
                            appendQuoted(doubleQuoted);
                            end = true;
                        }
                        break;
                    case '\\':
                        if (end && paramName != null) {
                            error("Expecting the end of a parameter value");
                            // try to recover: ignore that parameter
                            paramName = null;
                        }
                        start = true;
                        appendEscaped();
                        break;
                    case ' ':
                    case '\t':
                    case '\n':
                    case '\r':
                        if (start) {
                            end = true;
                        }
                        break;
                    default:
                        if (end && paramName != null) {
                            error("Expecting the end of a parameter value");
                            // try to recover: ignore that parameter
                            paramName = null;
                        }
                        start = true;
                        buffer.append(c);
                }
            } while (pos < length);
            endParameterValue();
        }

        private void endParameterValue() throws ParseException {
            if (paramName == null) {
                // recovering from an incorrect parameter: skip the value
                return;
            }
            if (buffer.length() == 0) {
                error("Empty parameter value");
                // try to recover: do not store the parameter
                return;
            }
            String value = buffer.toString();
            if (isDirective) {
                elem.addDirective(paramName, value);
            } else {
                elem.addAttribute(paramName, value);
            }
            valuesParsed = true;
            buffer.setLength(0);
        }

        private void appendQuoted(boolean doubleQuoted) {
            do {
                switch (readNext()) {
                    case '\0':
                        break;
                    case '\"':
                        if (doubleQuoted) {
                            return;
                        }
                        buffer.append(c);
                        break;
                    case '\'':
                        if (!doubleQuoted) {
                            return;
                        }
                        buffer.append(c);
                        break;
                    case '\\':
                        break;
                    default:
                        buffer.append(c);
                }
            } while (pos < length);
        }

        private void appendEscaped() {
            if (pos < length) {
                buffer.append(readNext());
            } else {
                buffer.append(c);
            }
        }
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ManifestHeaderValue)) {
            return false;
        }
        ManifestHeaderValue other = (ManifestHeaderValue) obj;
        if (other.elements.size() != elements.size()) {
            return false;
        }
        for (ManifestHeaderElement element : elements) {
            if (!other.elements.contains(element)) {
                return false;
            }
        }
        return true;
    }

    public String toString() {
        String string = "";
        Iterator<ManifestHeaderElement> it = elements.iterator();
        while (it.hasNext()) {
            string = string.concat(it.next().toString());
            if (it.hasNext()) {
                string = string.concat(",");
            }
        }
        return string;
    }

    public static void writeParseException(PrintStream out, String source, ParseException e) {
        out.println(e.getMessage());
        out.print("   " + source + "\n   ");
        for (int i = 0; i < e.getErrorOffset(); i++) {
            out.print(' ');
        }
        out.println('^');
    }
}
