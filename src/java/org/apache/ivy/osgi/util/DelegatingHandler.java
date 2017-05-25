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
import java.util.Locale;
import java.util.Map;

import org.apache.ivy.util.Message;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class DelegatingHandler extends DefaultHandler implements DTDHandler, ContentHandler,
        ErrorHandler {

    private DelegatingHandler delegate = null;

    DelegatingHandler parent;

    private final Map<String, DelegatingHandler> saxHandlerMapping = new HashMap<String, DelegatingHandler>();

    private final Map<String, ChildElementHandler<?>> childHandlerMapping = new HashMap<String, DelegatingHandler.ChildElementHandler<?>>();

    private final String tagName;

    private boolean started = false;

    private boolean skip = false;

    private boolean skipOnError = false;

    private StringBuffer charBuffer = new StringBuffer();

    private boolean bufferingChar = false;

    private Locator locator;

    public DelegatingHandler(String name) {
        this.tagName = name;
        charBuffer.setLength(0);
    }

    protected <DH extends DelegatingHandler> void addChild(DH saxHandler,
            ChildElementHandler<DH> elementHandler) {
        saxHandlerMapping.put(saxHandler.getName(), saxHandler);
        childHandlerMapping.put(saxHandler.getName(), elementHandler);
        saxHandler.parent = this;
    }

    public String getName() {
        return tagName;
    }

    public DelegatingHandler getParent() {
        return parent;
    }

    public void setBufferingChar(boolean bufferingChar) {
        this.bufferingChar = bufferingChar;
    }

    public void setSkipOnError(boolean skipOnError) {
        this.skipOnError = skipOnError;
    }

    public boolean isBufferingChar() {
        return bufferingChar;
    }

    public String getBufferedChars() {
        return charBuffer.toString();
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
        for (DelegatingHandler subHandler : saxHandlerMapping.values()) {
            subHandler.setDocumentLocator(locator);
        }
    }

    public Locator getLocator() {
        return locator;
    }

    /**
     * Return an sort of identifier of the current element being parsed. It will only be used for
     * logging purpose.
     * 
     * @return an empty string by default
     */
    protected String getCurrentElementIdentifier() {
        return "";
    }

    public void skip() {
        skip = true;
        for (DelegatingHandler subHandler : saxHandlerMapping.values()) {
            subHandler.stopDelegating();
        }
    }

    protected void stopDelegating() {
        parent.delegate = null;
        skip = false;
        started = false;
        for (DelegatingHandler/* <?> */subHandler : saxHandlerMapping.values()) {
            subHandler.stopDelegating();
        }
    }

    private interface SkipOnErrorCallback {
        public void call() throws SAXException;
    }

    private void skipOnError(SkipOnErrorCallback callback) throws SAXException {
        try {
            callback.call();
        } catch (SAXException e) {
            if (skipOnError) {
                skip();
                log(Message.MSG_ERR, e.getMessage(), e);
            } else {
                throw e;
            }
        }
    }

    @Override
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

    @Override
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

    @Override
    public final void startElement(final String uri, final String localName, final String n,
            final Attributes atts) throws SAXException {
        // reset the char buffer
        charBuffer.setLength(0);
        if (delegate != null) {
            // we are already delegating, let's continue
            skipOnError(new SkipOnErrorCallback() {
                public void call() throws SAXException {
                    delegate.startElement(uri, localName, n, atts);
                }
            });
        } else {
            if (!started) { // first time called ?
                // just for the root, check the expected element name
                // not need to check the delegated as the mapping is already taking care of it
                if (parent == null && !localName.equals(tagName)) {
                    // we are at the root and the saxed element doesn't match
                    throw new SAXException("The root element of the parsed document '" + localName
                            + "' didn't matched the expected one: '" + tagName + "'");
                }
                skipOnError(new SkipOnErrorCallback() {
                    public void call() throws SAXException {
                        handleAttributes(atts);
                    }
                });
                started = true;
            } else {
                if (skip) {
                    // we con't care anymore about that part of the xml tree
                    return;
                }
                // time now to delegate for a new element
                delegate = saxHandlerMapping.get(localName);
                if (delegate != null) {
                    skipOnError(new SkipOnErrorCallback() {
                        public void call() throws SAXException {
                            delegate.startElement(uri, localName, n, atts);
                        }
                    });
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

    @Override
    public final void endElement(final String uri, final String localName, final String n)
            throws SAXException {
        if (delegate != null) {
            final DelegatingHandler savedDelegate = delegate;
            // we are already delegating, let's continue
            skipOnError(new SkipOnErrorCallback() {
                public void call() throws SAXException {
                    delegate.endElement(uri, localName, n);
                }
            });
            if (delegate == null) {
                // we just stopped delegating, it means that the child has ended
                final ChildElementHandler<?> childHandler = childHandlerMapping.get(localName);
                if (childHandler != null) {
                    skipOnError(new SkipOnErrorCallback() {
                        public void call() throws SAXException {
                            childHandler._childHanlded(savedDelegate);
                        }
                    });
                }
            }
        } else {
            if (!skip) {
                doEndElement(uri, localName, n);
            }
            if (parent != null && tagName.equals(localName)) {
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

    public static abstract class ChildElementHandler<DH extends DelegatingHandler> {

        public abstract void childHanlded(DH child) throws SAXParseException;

        // because we know what we're doing
        @SuppressWarnings("unchecked")
        private void _childHanlded(DelegatingHandler delegate) throws SAXParseException {
            childHanlded((DH) delegate);
        }

    }

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    // //////////////////////
    // Functions related to error handling
    // //////////////////////

    protected void log(int logLevel, String message, Throwable t) {
        Message.debug(t);
        log(logLevel, message);
    }

    protected void log(int logLevel, String message) {
        Message.log(logLevel, getLocation(getLocator()) + message);
    }

    protected static String getLocation(Locator locator) {
        if (locator == null) {
            return "";
        }
        return "[line " + locator.getLineNumber() + " col. " + locator.getColumnNumber() + "] ";
    }

    private void skipOnError(DelegatingHandler currentHandler,
            Class<? extends DelegatingHandler> handlerClassToSkip, String message) {
        DelegatingHandler handlerToSkip = currentHandler;
        while (!(handlerClassToSkip.isAssignableFrom(handlerToSkip.getClass()))) {
            handlerToSkip = handlerToSkip.getParent();
        }
        log(Message.MSG_ERR, message + ". The '" + handlerToSkip.getName() + "' element "
                + getCurrentElementIdentifier() + " is then ignored.");
        handlerToSkip.skip();
    }

    // //////////////////////
    // Helpers to parse the attributes
    // //////////////////////

    protected String getRequiredAttribute(Attributes atts, String name) throws SAXParseException {
        String value = atts.getValue(name);
        if (value == null) {
            throw new SAXParseException("Required attribute '" + name + "' not found", getLocator());
        }
        return value;
    }

    protected String getOptionalAttribute(Attributes atts, String name, String defaultValue) {
        String value = atts.getValue(name);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    protected int getRequiredIntAttribute(Attributes atts, String name, Integer logLevel)
            throws SAXParseException {
        return parseInt(name, getRequiredAttribute(atts, name));
    }

    protected Integer getOptionalIntAttribute(Attributes atts, String name, Integer defaultValue)
            throws SAXParseException {
        String value = atts.getValue(name);
        if (value == null) {
            return defaultValue;
        }
        return new Integer(parseInt(name, value));
    }

    private int parseInt(String name, String value) throws SAXParseException {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new SAXParseException("Attribute '" + name
                    + "' is expected to be an integer but was '" + value + "' (" + e.getMessage()
                    + ")", getLocator());
        }
    }

    protected long getRequiredLongAttribute(Attributes atts, String name) throws SAXParseException {
        return parseLong(name, getRequiredAttribute(atts, name));
    }

    protected Long getOptionalLongAttribute(Attributes atts, String name, Long defaultValue)
            throws SAXParseException {
        String value = atts.getValue(name);
        if (value == null) {
            return defaultValue;
        }
        return new Long(parseLong(name, value));
    }

    private long parseLong(String name, String value) throws SAXParseException {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new SAXParseException("Attribute '" + name
                    + "' is expected to be an long but was '" + value + "' (" + e.getMessage()
                    + ")", getLocator());
        }
    }

    protected boolean getRequiredBooleanAttribute(Attributes atts, String name)
            throws SAXParseException {
        return parseBoolean(name, getRequiredAttribute(atts, name));
    }

    protected Boolean getOptionalBooleanAttribute(Attributes atts, String name, Boolean defaultValue)
            throws SAXParseException {
        String value = atts.getValue(name);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.valueOf(parseBoolean(name, value));
    }

    static final String TRUE = Boolean.TRUE.toString().toLowerCase(Locale.US);

    static final String FALSE = Boolean.FALSE.toString().toLowerCase(Locale.US);

    private boolean parseBoolean(String name, String value) throws SAXParseException {
        String lowerValue = value.toLowerCase(Locale.US);
        if (lowerValue.equals(TRUE)) {
            return true;
        }
        if (lowerValue.equals(FALSE)) {
            return false;
        }
        throw new SAXParseException("Attribute '" + name
                + "' is expected to be a boolean but was '" + value + "'", getLocator());
    }
}
