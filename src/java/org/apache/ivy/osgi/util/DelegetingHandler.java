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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class DelegetingHandler/* <P extends DelegetingHandler<?>> */ extends DefaultHandler implements DTDHandler,
        ContentHandler, ErrorHandler {

    private DelegetingHandler/* <?> */delegate = null;

    private/* P */DelegetingHandler parent;

    private final Map/* <String, DelegetingHandler<?>> */mapping = new HashMap/*
                                                                               * <String,
                                                                               * DelegetingHandler
                                                                               * <?>>
                                                                               */();

    private final String tagName;

    private boolean started = false;

    private boolean skip = false;

    private StringBuffer charBuffer = new StringBuffer();

    private boolean bufferingChar = false;

    private Locator locator;

    public DelegetingHandler(String name, /* P */DelegetingHandler parent) {
        this.tagName = name;
        this.parent = parent;
        if (parent != null) {
            parent.mapping.put(name, this);
        }
        charBuffer.setLength(0);
    }

    public String getName() {
        return tagName;
    }

    public/* P */DelegetingHandler getParent() {
        return parent;
    }

    public void setBufferingChar(boolean bufferingChar) {
        this.bufferingChar = bufferingChar;
    }

    public boolean isBufferingChar() {
        return bufferingChar;
    }

    public String getBufferedChars() {
        return charBuffer.toString();
    }

    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
        Iterator itHandler = mapping.values().iterator();
        while (itHandler.hasNext()) {
            DelegetingHandler/* <?> */subHandler = (DelegetingHandler) itHandler.next();
            subHandler.setDocumentLocator(locator);
        }
    }

    public Locator getLocator() {
        return locator;
    }

    public void skip() {
        skip = true;
        Iterator itHandler = mapping.values().iterator();
        while (itHandler.hasNext()) {
            DelegetingHandler/* <?> */subHandler = (DelegetingHandler) itHandler.next();
            subHandler.stopDelegating();
        }
    }

    protected void stopDelegating() {
        parent.delegate = null;
        skip = false;
        started = false;
        charBuffer.setLength(0);
        Iterator itHandler = mapping.values().iterator();
        while (itHandler.hasNext()) {
            DelegetingHandler/* <?> */subHandler = (DelegetingHandler) itHandler.next();
            subHandler.stopDelegating();
        }
    }

    public final void startDocument() throws SAXException {
        if (skip) {
            return;
        }
        if (delegate != null) {
            delegate.startDocument();
        } else {
            doStartDocument();
        }
    }

    /**
     * @throws SAXException
     */
    protected void doStartDocument() throws SAXException {
        // by default do nothing
    }

    public final void endDocument() throws SAXException {
        if (skip) {
            return;
        }
        if (delegate != null) {
            delegate.endDocument();
        } else {
            doEndDocument();
        }
    }

    /**
     * @throws SAXException
     */
    protected void doEndDocument() throws SAXException {
        // by default do nothing
    }

    public final void startElement(String uri, String localName, String n, Attributes atts)
            throws SAXException {
        if (delegate != null) {
            // we are already delegating, let's continue
            delegate.startElement(uri, localName, n, atts);
        } else {
            if (!started) { // first time called ?
                // just for the root, check the expected element name
                // not need to check the delegated as the mapping is already taking care of it
                if (parent == null && !n.equals(tagName)) {
                    // we are at the root and the saxed element doesn't match
                    throw new SAXException("The root element of the parsed document '" + n
                            + "' didn't matched the expected one: '" + tagName + "'");
                }
                handleAttributes(atts);
                started = true;
            } else {
                if (skip) {
                    // we con't care anymore about that part of the xml tree
                    return;
                }
                // time now to delegate for a new element
                delegate = (DelegetingHandler) mapping.get(n);
                if (delegate != null) {
                    delegate.startElement(uri, localName, n, atts);
                }
            }
        }
    }

    /**
     * Called when the expected node is achieved
     * 
     * @param atts
     *            the xml attributes attached to the expected node
     * @exception SAXException
     *                in case the parsing should be completely stopped
     */
    protected void handleAttributes(Attributes atts) throws SAXException {
        // nothing to do by default
    }

    /**
     * @throws SAXException
     */
    protected void doStartElement(String uri, String localName, String name, Attributes atts)
            throws SAXException {
        // by default do nothing
    }

    public final void endElement(String uri, String localName, String n) throws SAXException {
        if (delegate != null) {
            // we are already delegating, let's continue
            delegate.endElement(uri, localName, n);
        } else {
            if (!skip) {
                doEndElement(uri, localName, n);
            }
            if (parent != null && tagName.equals(n)) {
                // the current element is closed, let's tell the parent to stop delegating
                stopDelegating();
            }
        }
    }

    /**
     * @throws SAXException
     */
    protected void doEndElement(String uri, String localName, String name) throws SAXException {
        // by default do nothing
    }

    public final void characters(char[] ch, int start, int length) throws SAXException {
        if (skip) {
            return;
        }
        if (delegate != null) {
            delegate.characters(ch, start, length);
        } else {
            doCharacters(ch, start, length);
        }
    }

    /**
     * @throws SAXException
     */
    protected void doCharacters(char[] ch, int start, int length) throws SAXException {
        if (bufferingChar) {
            charBuffer.append(ch, start, length);
        }
    }

    public final void startPrefixMapping(String prefix, String uri) throws SAXException {
        if (skip) {
            return;
        }
        if (delegate != null) {
            delegate.startPrefixMapping(prefix, uri);
        } else {
            doStartPrefixMapping(prefix, uri);
        }
    }

    /**
     * @throws SAXException
     */
    protected void doStartPrefixMapping(String prefix, String uri) throws SAXException {
        // by default do nothing
    }

    public final void endPrefixMapping(String prefix) throws SAXException {
        if (skip) {
            return;
        }
        if (delegate != null) {
            delegate.endPrefixMapping(prefix);
        } else {
            doEndPrefixMapping(prefix);
        }
    }

    /**
     * @throws SAXException
     */
    protected void doEndPrefixMapping(String prefix) throws SAXException {
        // by default do nothing
    }

    public final void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        if (skip) {
            return;
        }
        if (delegate != null) {
            delegate.ignorableWhitespace(ch, start, length);
        } else {
            doIgnorableWhitespace(ch, start, length);
        }
    }

    /**
     * @throws SAXException
     */
    protected void doIgnorableWhitespace(char[] ch, int start, int length) throws SAXException {
        // by default do nothing
    }

    public final void notationDecl(String name, String publicId, String systemId)
            throws SAXException {
        if (skip) {
            return;
        }
        if (delegate != null) {
            delegate.notationDecl(name, publicId, systemId);
        } else {
            doNotationDecl(name, publicId, systemId);
        }
    }

    /**
     * @throws SAXException
     */
    protected void doNotationDecl(String name, String publicId, String systemId)
            throws SAXException {
        // by default do nothing
    }

    public final void processingInstruction(String target, String data) throws SAXException {
        if (skip) {
            return;
        }
        if (delegate != null) {
            delegate.processingInstruction(target, data);
        } else {
            doProcessingInstruction(target, data);
        }
    }

    /**
     * @throws SAXException
     */
    protected void doProcessingInstruction(String target, String data) throws SAXException {
        // by default do nothing
    }

    public final void skippedEntity(String name) throws SAXException {
        if (skip) {
            return;
        }
        if (delegate != null) {
            delegate.skippedEntity(name);
        } else {
            doSkippedEntity(name);
        }
    }

    /**
     * @throws SAXException
     */
    protected void doSkippedEntity(String name) throws SAXException {
        // by default do nothing
    }

    /**
     * @throws SAXException
     */
    public final void unparsedEntityDecl(String name, String publicId, String systemId,
            String notationName) throws SAXException {
        if (skip) {
            return;
        }
        if (delegate != null) {
            delegate.unparsedEntityDecl(name, publicId, systemId, notationName);
        } else {
            doUnparsedEntityDecl(name, publicId, systemId, notationName);
        }
    }

    /**
     * @throws SAXException
     */
    protected void doUnparsedEntityDecl(String name, String publicId, String systemId,
            String notationName) throws SAXException {
        // by default do nothing
    }

    // ERROR HANDLING

    public final void warning(SAXParseException exception) throws SAXException {
        if (skip) {
            return;
        }
        if (delegate != null) {
            delegate.warning(exception);
        } else {
            doWarning(exception);
        }
    }

    /**
     * @throws SAXException
     */
    protected void doWarning(SAXParseException exception) throws SAXException {
        // by default do nothing
    }

    public final void error(SAXParseException exception) throws SAXException {
        if (skip) {
            return;
        }
        if (delegate != null) {
            delegate.error(exception);
        } else {
            doError(exception);
        }
    }

    /**
     * @throws SAXException
     */
    protected void doError(SAXParseException exception) throws SAXException {
        // by default do nothing
    }

    public final void fatalError(SAXParseException exception) throws SAXException {
        if (skip) {
            return;
        }
        if (delegate != null) {
            delegate.fatalError(exception);
        } else {
            doFatalError(exception);
        }
    }

    /**
     * @throws SAXException
     */
    protected void doFatalError(SAXParseException exception) throws SAXException {
        // by default do nothing
    }

}
