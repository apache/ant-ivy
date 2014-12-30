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

import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ContextualSAXHandler extends DefaultHandler {

    private Stack<String> contextStack = new Stack<String>();

    private StringBuffer buffer = new StringBuffer();

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        buffer.append(ch, start, length);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException {
        contextStack.push(qName);
        buffer.setLength(0);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        contextStack.pop();
        buffer.setLength(0);
    }

    protected String getContext() {
        StringBuffer buf = new StringBuffer();
        for (String ctx : contextStack) {
            buf.append(ctx).append("/");
        }
        if (buf.length() > 0) {
            buf.setLength(buf.length() - 1);
        }
        return buf.toString();
    }

    protected String getText() {
        return buffer.toString();
    }

}
