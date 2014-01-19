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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.ivy.util.url.URLHandlerRegistry;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

public abstract class XMLHelper {

    static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

    static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";

    static final String XERCES_LOAD_EXTERNAL_DTD = "http://apache.org/xml/features/nonvalidating/load-external-dtd";

    static final String XML_NAMESPACE_PREFIXES = "http://xml.org/sax/features/namespace-prefixes";

    static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

    private static boolean canUseSchemaValidation = true;

    private static Boolean canDisableExternalDtds = null;

    private static SAXParser newSAXParser(URL schema, InputStream schemaStream,
            boolean loadExternalDtds) throws ParserConfigurationException, SAXException {
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        parserFactory.setNamespaceAware(true);
        parserFactory.setValidating(canUseSchemaValidation && (schema != null));
        if (!loadExternalDtds && canDisableExternalDtds(parserFactory)) {
            parserFactory.setFeature(XERCES_LOAD_EXTERNAL_DTD, false);
        }
        SAXParser parser = parserFactory.newSAXParser();

        if (canUseSchemaValidation && (schema != null)) {
            try {
                parser.setProperty(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
                parser.setProperty(JAXP_SCHEMA_SOURCE, schemaStream);
            } catch (SAXNotRecognizedException ex) {
                Message.warn("problem while setting JAXP validating property on SAXParser... "
                        + "XML validation will not be done", ex);
                canUseSchemaValidation = false;
                parserFactory.setValidating(false);
                parser = parserFactory.newSAXParser();
            }
        }

        parser.getXMLReader().setFeature(XML_NAMESPACE_PREFIXES, true);
        return parser;
    }

    private static boolean canDisableExternalDtds(SAXParserFactory parserFactory) {
        if (canDisableExternalDtds == null) {
            try {
                parserFactory.getFeature(XERCES_LOAD_EXTERNAL_DTD);
                canDisableExternalDtds = Boolean.TRUE;
            } catch (Exception ex) {
                canDisableExternalDtds = Boolean.FALSE;
            }
        }
        return canDisableExternalDtds.booleanValue();
    }

    /**
     * Convert an URL to a valid systemId according to RFC 2396.
     */
    public static String toSystemId(URL url) {
        try {
            return new URI(url.toExternalForm()).toASCIIString();
        } catch (URISyntaxException e) {
            return url.toExternalForm();
        }
    }

    // IMPORTANT: validation errors are only notified to the given handler, and
    // do not cause exception
    // implement warning error and fatalError methods in handler to be informed
    // of validation errors
    public static void parse(URL xmlURL, URL schema, DefaultHandler handler) throws SAXException,
            IOException, ParserConfigurationException {
        parse(xmlURL, schema, handler, null);
    }

    public static void parse(URL xmlURL, URL schema, DefaultHandler handler, LexicalHandler lHandler)
            throws SAXException, IOException, ParserConfigurationException {
        InputStream xmlStream = URLHandlerRegistry.getDefault().openStream(xmlURL);
        try {
            InputSource inSrc = new InputSource(xmlStream);
            inSrc.setSystemId(toSystemId(xmlURL));
            parse(inSrc, schema, handler, lHandler);
        } finally {
            try {
                xmlStream.close();
            } catch (IOException e) {
                // ignored
            }
        }
    }

    public static void parse(InputStream xmlStream, URL schema, DefaultHandler handler,
            LexicalHandler lHandler) throws SAXException, IOException, ParserConfigurationException {
        parse(new InputSource(xmlStream), schema, handler, lHandler);
    }

    public static void parse(InputSource xmlStream, URL schema, DefaultHandler handler,
            LexicalHandler lHandler) throws SAXException, IOException, ParserConfigurationException {
        parse(xmlStream, schema, handler, lHandler, true);
    }

    public static void parse(InputSource xmlStream, URL schema, DefaultHandler handler,
            LexicalHandler lHandler, boolean loadExternalDtds) throws SAXException, IOException,
            ParserConfigurationException {
        InputStream schemaStream = null;
        try {
            if (schema != null) {
                schemaStream = URLHandlerRegistry.getDefault().openStream(schema);
            }
            SAXParser parser = XMLHelper.newSAXParser(schema, schemaStream, loadExternalDtds);

            if (lHandler != null) {
                try {
                    parser.setProperty("http://xml.org/sax/properties/lexical-handler", lHandler);
                } catch (SAXException ex) {
                    Message.warn("problem while setting the lexical handler property on SAXParser",
                        ex);
                    // continue without the lexical handler
                }
            }

            parser.parse(xmlStream, handler);
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
        return canUseSchemaValidation;
    }

    /**
     * Escapes invalid XML characters in the given character data using XML entities. For the
     * moment, only the following characters are being escaped: (<), (&), (') and (").
     * 
     * Remark: we don't escape the (>) character to keep the readability of the configuration
     * mapping! The XML spec only requires that the (&) and (<) characters are being escaped inside
     * character data.
     * 
     * @param text
     *            the character data to escape
     * @return the escaped character data
     */
    public static String escape(String text) {
        if (text == null) {
            return null;
        }

        StringBuffer result = new StringBuffer(text.length());

        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            switch (chars[i]) {
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
                    result.append(chars[i]);
            }
        }

        return result.toString();
    }

    public static Document parseToDom(InputSource source, EntityResolver entityResolver)
            throws IOException, SAXException {
        DocumentBuilder docBuilder = getDocBuilder(entityResolver);
        return docBuilder.parse(source);
    }

    public static DocumentBuilder getDocBuilder(EntityResolver entityResolver) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            DocumentBuilder docBuilder = factory.newDocumentBuilder();
            if (entityResolver != null) {
                docBuilder.setEntityResolver(entityResolver);
            }
            return docBuilder;
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    private XMLHelper() {
    }

}
