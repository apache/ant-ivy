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

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;

import org.apache.ivy.util.url.URLHandlerRegistry;
import org.w3c.dom.Document;
import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

public abstract class XMLHelper {

    static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

    static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";

    static final String XERCES_LOAD_EXTERNAL_DTD = "http://apache.org/xml/features/nonvalidating/load-external-dtd";

    static final String XML_NAMESPACE_PREFIXES = "http://xml.org/sax/features/namespace-prefixes";

    static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

    private static final String XML_ACCESS_EXTERNAL_SCHEMA = "http://javax.xml.XMLConstants/property/accessExternalSchema";
    private static final String XML_ACCESS_EXTERNAL_DTD = "http://javax.xml.XMLConstants/property/accessExternalDTD";
    public static final String ALLOW_DOCTYPE_PROCESSING = "ivy.xml.allow-doctype-processing";
    public static final String EXTERNAL_RESOURCES = "ivy.xml.external-resources";

    private static SAXParser newSAXParser(final URL schema, final InputStream schemaStream,
        final boolean allowXmlDoctypeProcessing, final ExternalResources externalResources)
        throws ParserConfigurationException, SAXException {
        final SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        parserFactory.setNamespaceAware(true);
        parserFactory.setValidating(schema != null);
        configureSafeFeatures(parserFactory, allowXmlDoctypeProcessing, externalResources);

        SAXParser parser = parserFactory.newSAXParser();
        if (schema != null) {
            try {
                parser.setProperty(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
                parser.setProperty(JAXP_SCHEMA_SOURCE, schemaStream);
            } catch (SAXNotRecognizedException ex) {
                Message.warn("problem while setting JAXP validating property on SAXParser... "
                        + "XML validation will not be done", ex);
                parserFactory.setValidating(false);
                parser = parserFactory.newSAXParser();
            }
        }
        final XMLReader reader = parser.getXMLReader();
        reader.setFeature(XML_NAMESPACE_PREFIXES, true);
        reader.setProperty(XML_ACCESS_EXTERNAL_SCHEMA, externalResources.getAllowedProtocols());
        reader.setProperty(XML_ACCESS_EXTERNAL_DTD, externalResources.getAllowedProtocols());
        return parser;
    }

    /**
     * Convert an URL to a valid systemId according to RFC 2396.
     *
     * @param url URL
     * @return String
     */
    public static String toSystemId(URL url) {
        try {
            return new URI(url.toExternalForm()).toASCIIString();
        } catch (URISyntaxException e) {
            return url.toExternalForm();
        }
    }

    // IMPORTANT: validation errors are only notified to the given handler, and
    // do not cause exception implement warning error and fatalError methods in
    // handler to be informed of validation errors

    public static void parse(URL xmlURL, URL schema, DefaultHandler handler) throws SAXException,
            IOException, ParserConfigurationException {
        parse(xmlURL, schema, handler, null);
    }

    public static void parse(URL xmlURL, URL schema, DefaultHandler handler, LexicalHandler lHandler)
            throws SAXException, IOException, ParserConfigurationException {
        parse(xmlURL, schema, handler, lHandler, ExternalResources.fromSystemProperty());
    }

    @SuppressWarnings("deprecation")
    public static void parse(URL xmlURL, URL schema, DefaultHandler handler, LexicalHandler lHandler,
            final ExternalResources externalResources)
            throws SAXException, IOException, ParserConfigurationException {
        try (InputStream xmlStream = URLHandlerRegistry.getDefault().openStream(xmlURL)) {
            InputSource inSrc = new InputSource(xmlStream);
            inSrc.setSystemId(toSystemId(xmlURL));
            parse(inSrc, schema, handler, lHandler, externalResources);
        }
    }

    public static void parse(InputStream xmlStream, URL schema, DefaultHandler handler,
            LexicalHandler lHandler) throws SAXException, IOException, ParserConfigurationException {
        parse(xmlStream, schema, handler, lHandler, ExternalResources.fromSystemProperty());
    }

    public static void parse(InputStream xmlStream, URL schema, DefaultHandler handler,
            LexicalHandler lHandler, final ExternalResources externalResources)
            throws SAXException, IOException, ParserConfigurationException {
        parse(new InputSource(xmlStream), schema, handler, lHandler);
    }

    public static void parse(InputSource xmlStream, URL schema, DefaultHandler handler,
            LexicalHandler lHandler) throws SAXException, IOException, ParserConfigurationException {
        parse(xmlStream, schema, handler, lHandler, ExternalResources.fromSystemProperty());
    }

    public static void parse(final InputSource xmlStream, final URL schema,
                             final DefaultHandler handler, final LexicalHandler lHandler,
                             final boolean loadExternalDtds) throws SAXException, IOException,
            ParserConfigurationException {
        parse(xmlStream, schema, handler, lHandler,
            loadExternalDtds ? ExternalResources.LOCAL_ONLY : ExternalResources.PROHIBIT);
    }

    @SuppressWarnings("deprecation")
    public static void parse(final InputSource xmlStream, final URL schema,
                             final DefaultHandler handler, final LexicalHandler lHandler,
                             final ExternalResources externalResources) throws SAXException, IOException,
            ParserConfigurationException {
        InputStream schemaStream = null;
        try {
            if (schema != null) {
                schemaStream = URLHandlerRegistry.getDefault().openStream(schema);
            }
            SAXParser parser = XMLHelper.newSAXParser(schema, schemaStream,
                    isXmlDoctypeProcessingAllowed(), externalResources);

            if (lHandler != null) {
                try {
                    parser.setProperty("http://xml.org/sax/properties/lexical-handler", lHandler);
                } catch (SAXException ex) {
                    Message.warn("problem while setting the lexical handler property on SAXParser",
                        ex);
                    // continue without the lexical handler
                }
            }

            DefaultHandler h = externalResources == ExternalResources.IGNORE
                ? new NoopEntityResolverDefaultHandler(handler)
                : handler;
            parser.parse(xmlStream, h);
        } finally {
            if (schemaStream != null) {
                try {
                    schemaStream.close();
                } catch (IOException ex) {
                    // ignored
                }
            }
        }
    }

    public static boolean canUseSchemaValidation() {
        return true;
    }

    /**
     * Escapes invalid XML characters in the given character data using XML entities. For the
     * moment, only the following characters are being escaped: (&lt;), (&amp;), (') and (&quot;).
     *
     * Remark: we don't escape the (&gt;) character to keep the readability of the configuration
     * mapping! The XML spec only requires that the (&amp;) and (&lt;) characters are being escaped
     * inside character data.
     *
     * @param text
     *            the character data to escape
     * @return the escaped character data
     */
    public static String escape(String text) {
        if (text == null) {
            return null;
        }

        StringBuilder result = new StringBuilder(text.length());

        for (char ch : text.toCharArray()) {
            switch (ch) {
                case '&':
                    result.append("&amp;");
                    break;
                case '<':
                    result.append("&lt;");
                    break;
                case '\'':
                    result.append("&apos;");
                    break;
                case '\"':
                    result.append("&quot;");
                    break;
                default:
                    result.append(ch);
            }
        }

        return result.toString();
    }

    public static Document parseToDom(InputSource source, EntityResolver entityResolver)
            throws IOException, SAXException {
        return parseToDom(source, entityResolver, isXmlDoctypeProcessingAllowed(),
            ExternalResources.fromSystemProperty());
    }

    public static Document parseToDom(InputSource source, EntityResolver entityResolver,
            boolean allowXmlDoctypeProcessing, ExternalResources externalResources)
            throws IOException, SAXException {
        DocumentBuilder docBuilder = getDocBuilder(entityResolver, allowXmlDoctypeProcessing,
            externalResources);
        return docBuilder.parse(source);
    }

    public static DocumentBuilder getDocBuilder(EntityResolver entityResolver) {
        return getDocBuilder(entityResolver, isXmlDoctypeProcessingAllowed(),
            ExternalResources.fromSystemProperty());
    }

    public static DocumentBuilder getDocBuilder(EntityResolver entityResolver,
            boolean allowXmlDoctypeProcessing, ExternalResources externalResources) {
        try {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            configureSafeFeatures(factory, allowXmlDoctypeProcessing, externalResources);
            DocumentBuilder docBuilder = factory.newDocumentBuilder();
            if (externalResources == ExternalResources.IGNORE) {
                entityResolver = new NoopEntityResolver(entityResolver);
            }
            if (entityResolver != null) {
                docBuilder.setEntityResolver(entityResolver);
            }
            return docBuilder;
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public static Transformer getTransformer(Source source) throws TransformerConfigurationException {
        TransformerFactory factory = getTransformerFactory();
        return factory.newTransformer(source);
    }

    public static TransformerHandler getTransformerHandler() throws TransformerConfigurationException {
        SAXTransformerFactory factory = getTransformerFactory();
        return factory.newTransformerHandler();
    }

    public enum ExternalResources {
        PROHIBIT(""),
        // technically the URIs for IGNORE will never get resolved at all.
        // "all" pacifies some version of Java that check the property before delegating to the EntityResolver (which is
        // going to return an empty content anyway)
        IGNORE("all"),
        LOCAL_ONLY("file, jar:file"),
        ALL("all");

        private final String allowedProtocols;

        private ExternalResources(String allowedProtocols) {
            this.allowedProtocols = allowedProtocols;
        }

        private String getAllowedProtocols() {
            return allowedProtocols;
        }

        public static ExternalResources fromSystemProperty() {
            final String val = System.getProperty(EXTERNAL_RESOURCES);
            if (val != null) {
                if (val.equalsIgnoreCase("ignore")) {
                    return IGNORE;
                }
                if (val.equalsIgnoreCase("all")) {
                    return ALL;
                }
                if (val.equalsIgnoreCase("local-only") || val.equalsIgnoreCase("local_only")) {
                    return LOCAL_ONLY;
                }
            }
            return PROHIBIT;
        }
    }

    public static boolean isXmlDoctypeProcessingAllowed() {
        return "true".equals(System.getProperty(ALLOW_DOCTYPE_PROCESSING));
    }

    private XMLHelper() {
    }

    private static SAXTransformerFactory getTransformerFactory() {
        TransformerFactory factory = SAXTransformerFactory.newInstance();
        configureSafeFeatures(factory);
        return (SAXTransformerFactory) factory;
    }

    private static void configureSafeFeatures(final DocumentBuilderFactory factory,
            final boolean allowXmlDoctypeProcessing, final ExternalResources externalResources) {
        final String DISALLOW_DOCTYPE_DECL = "http://apache.org/xml/features/disallow-doctype-decl";
        trySetFeature(factory, DISALLOW_DOCTYPE_DECL, !allowXmlDoctypeProcessing);

        // available since Java 6, as XMLConstants.FEATURE_SECURE_PROCESSING. We can't use Java 6
        // at compile time, in current version, so inline the constant here
        final String FEATURE_SECURE_PROCESSING = "http://javax.xml.XMLConstants/feature/secure-processing";
        trySetFeature(factory, FEATURE_SECURE_PROCESSING, true);

        final String ALLOW_EXTERNAL_GENERAL_ENTITIES = "http://xml.org/sax/features/external-general-entities";
        trySetFeature(factory, ALLOW_EXTERNAL_GENERAL_ENTITIES, false);

        final String ALLOW_EXTERNAL_PARAM_ENTITIES = "http://xml.org/sax/features/external-parameter-entities";
        trySetFeature(factory, ALLOW_EXTERNAL_PARAM_ENTITIES, false);

        final String LOAD_EXTERNAL_DTD = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
        trySetFeature(factory, LOAD_EXTERNAL_DTD, externalResources != ExternalResources.PROHIBIT);

        try {
            factory.setXIncludeAware(false);
        } catch (Exception e) {
            // ignore
        }
        try {
            factory.setExpandEntityReferences(false);
        } catch (Exception e) {
            // ignore
        }
    }

    private static void configureSafeFeatures(final SAXParserFactory factory,
            final boolean allowXmlDoctypeProcessing, final ExternalResources externalResources) {
        final String DISALLOW_DOCTYPE_DECL = "http://apache.org/xml/features/disallow-doctype-decl";
        trySetFeature(factory, DISALLOW_DOCTYPE_DECL, !allowXmlDoctypeProcessing);

        // available since Java 6, as XMLConstants.FEATURE_SECURE_PROCESSING. We can't use Java 6
        // at compile time, in current version, so inline the constant here
        final String FEATURE_SECURE_PROCESSING = "http://javax.xml.XMLConstants/feature/secure-processing";
        trySetFeature(factory, FEATURE_SECURE_PROCESSING, true);

        final boolean allowEntities = externalResources == ExternalResources.LOCAL_ONLY
            || externalResources == ExternalResources.ALL;
        final String ALLOW_EXTERNAL_GENERAL_ENTITIES = "http://xml.org/sax/features/external-general-entities";
        trySetFeature(factory, ALLOW_EXTERNAL_GENERAL_ENTITIES, allowEntities);

        final String ALLOW_EXTERNAL_PARAM_ENTITIES = "http://xml.org/sax/features/external-parameter-entities";
        trySetFeature(factory, ALLOW_EXTERNAL_PARAM_ENTITIES, allowEntities);
        final String LOAD_EXTERNAL_DTD = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
        trySetFeature(factory, LOAD_EXTERNAL_DTD, externalResources != ExternalResources.PROHIBIT);
        try {
            factory.setXIncludeAware(false);
        } catch (Exception e) {
            // ignore
        }
    }

    private static void configureSafeFeatures(final TransformerFactory factory) {
        // available since Java 7, as XMLConstants.ACCESS_EXTERNAL_DTD, ACCESS_EXTERNAL_SCHEMA and
        // ACCESS_EXTERNAL_STYLESHEET respectively.
        // We can't use Java 7 at compile time, in current version, so inline the constants here
        trySetAttribute(factory, XML_ACCESS_EXTERNAL_DTD, "");
        trySetAttribute(factory, XML_ACCESS_EXTERNAL_SCHEMA, "");
        trySetAttribute(factory, "http://javax.xml.XMLConstants/property/accessExternalStylesheet", "");
    }

    private static boolean isFeatureSupported(final SAXParserFactory factory, final String feature) {
        try {
            factory.getFeature(feature);
            return true;
        } catch (ParserConfigurationException e) {
            return false;
        } catch (SAXNotRecognizedException e) {
            return false;
        } catch (SAXNotSupportedException e) {
            return false;
        }
    }

    private static boolean isFeatureSupported(final DocumentBuilderFactory factory, final String feature) {
        try {
            factory.getFeature(feature);
            return true;
        } catch (ParserConfigurationException e) {
            return false;
        }
    }

    private static boolean isAttributeSupported(final TransformerFactory factory, final String attribute) {
        try {
            factory.getAttribute(attribute);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static boolean trySetFeature(final DocumentBuilderFactory factory,
                                               final String feature, final boolean val) {
        if (!isFeatureSupported(factory, feature)) {
            return false;
        }
        try {
            factory.setFeature(feature, val);
            return true;
        } catch (ParserConfigurationException e) {
            // log and continue
            Message.warn("Failed to set feature " + feature + " on DocumentBuilderFactory", e);
            return false;
        }
    }

    private static boolean trySetFeature(final SAXParserFactory factory,
                                         final String feature, final boolean val) {
        if (!isFeatureSupported(factory, feature)) {
            return false;
        }
        try {
            factory.setFeature(feature, val);
            return true;
        } catch (ParserConfigurationException e) {
            // log and continue
            Message.warn("Failed to set feature " + feature + " on SAXParserFactory", e);
            return false;
        } catch (SAXNotRecognizedException e) {
            // log and continue
            Message.warn("Failed to set feature " + feature + " on SAXParserFactory", e);
            return false;
        } catch (SAXNotSupportedException e) {
            // log and continue
            Message.warn("Failed to set feature " + feature + " on SAXParserFactory", e);
            return false;
        }
    }

    private static boolean trySetAttribute(final TransformerFactory factory,
                                         final String attribute, final String val) {
        if (!isAttributeSupported(factory, attribute)) {
            return false;
        }
        try {
            factory.setAttribute(attribute, val);
            return true;
        } catch (IllegalArgumentException e) {
            // log and continue
            Message.warn("Failed to set attribute " + attribute + " on TransformerFactory", e);
            return false;
        }
    }

    private static final InputSource EMPTY_INPUT_SOURCE = new InputSource(new StringReader(""));

    private static class NoopEntityResolver implements EntityResolver {
        private EntityResolver wrapped;

        private NoopEntityResolver(EntityResolver wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
            if (wrapped != null) {
                InputSource s = wrapped.resolveEntity(publicId, systemId);
                if (s != null) {
                    return s;
                }
            }
            return EMPTY_INPUT_SOURCE;
        }
    }

    private static class NoopEntityResolverDefaultHandler extends DefaultHandler {

        private DefaultHandler wrapped;

        private NoopEntityResolverDefaultHandler(DefaultHandler wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
            if (wrapped != null) {
                InputSource s = wrapped.resolveEntity(publicId, systemId);
                if (s != null) {
                    return s;
                }
            }
            return EMPTY_INPUT_SOURCE;
        }

        @Override
        public void notationDecl(String name, String publicId, String systemId) throws SAXException {
            wrapped.notationDecl(name, publicId, systemId);
        }

        @Override
        public void unparsedEntityDecl(String name, String publicId, String systemId, String notationName)
            throws SAXException {
            wrapped.unparsedEntityDecl(name, publicId, systemId, notationName);
        }

        @Override
        public void setDocumentLocator(Locator locator) {
            wrapped.setDocumentLocator(locator);
        }

        @Override
        public void startDocument() throws SAXException {
            wrapped.startDocument();
        }

        @Override
        public void endDocument() throws SAXException {
            wrapped.endDocument();
        }

        @Override
        public void startPrefixMapping(String prefix, String uri) throws SAXException {
            wrapped.startPrefixMapping(prefix, uri);
        }

        @Override
        public void endPrefixMapping(String prefix) throws SAXException {
            wrapped.endPrefixMapping(prefix);
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException {
            wrapped.startElement(uri, localName, qName, attributes);
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            wrapped.endElement(uri, localName, qName);
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            wrapped.characters(ch, start, length);
        }

        @Override
        public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
            wrapped.ignorableWhitespace(ch, start, length);
        }

        @Override
        public void processingInstruction(String target, String data) throws SAXException {
            wrapped.processingInstruction(target, data);
        }

        @Override
        public void skippedEntity(String name) throws SAXException {
            wrapped.skippedEntity(name);
        }

        @Override
        public void warning(SAXParseException e) throws SAXException {
            wrapped.warning(e);
        }

        @Override
        public void error(SAXParseException e) throws SAXException {
            wrapped.error(e);
        }

        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            wrapped.fatalError(e);
        }
    }
}
