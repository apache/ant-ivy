/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.xml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.ModuleId;
import fr.jayasoft.ivy.util.XMLHelper;

/**
 * Used to update ivy files. Uses ivy file as source and not ModuleDescriptor to preserve
 * as much as possible the original syntax
 * 
 * @author Hanin
 *
 */
public class XmlModuleDescriptorUpdater {
    public static String LINE_SEPARATOR = System.getProperty("line.separator");
    /**
     * used to copy a module descriptor xml file (also known as ivy file)
     * and update the revisions of its dependencies, its status and revision
     * 
     * @param srcURL the url of the source module descriptor file
     * @param destFile The file to which the updated module descriptor should be output
     * @param resolvedRevisions Map from ModuleId of dependencies to new revision (as String)
     * @param status the new status, null to keep the old one
     * @param revision the new revision, null to keep the old one
     */
    public static void update(URL srcURL, File destFile, final Map resolvedRevisions, final String status, final String revision, final Date pubdate) 
    							throws IOException, SAXException {
        update(srcURL, destFile, resolvedRevisions, status, revision, pubdate, null);
    }

    /**
     * used to copy a module descriptor xml file (also known as ivy file)
     * and update the revisions of its dependencies, its status and revision
     * 
     * @param srcURL the url of the source module descriptor file
     * @param destFile The file to which the updated module descriptor should be output
     * @param resolvedRevisions Map from ModuleId of dependencies to new revision (as String)
     * @param status the new status, null to keep the old one
     * @param revision the new revision, null to keep the old one
     */
    public static void update(URL srcURL, File destFile, final Map resolvedRevisions, final String status, 
            final String revision, final Date pubdate, final String resolverName) 
    							throws IOException, SAXException {
        if (destFile.getParentFile() != null) {
            destFile.getParentFile().mkdirs();
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(destFile);
            final PrintWriter out = new PrintWriter(fos);
            copyHeader(srcURL, out);
            XMLHelper.parse(srcURL, null, new DefaultHandler() {
                // never print *ln* cause \n is found in copied characters stream
                // nor do we need do handle indentation, original one is maintained except for attributes
                
                private String _organisation = null;
                private String _justOpen = null; // used to know if the last open tag was empty, to adjust termination with /> instead of ></qName> 
                public void startElement(String uri, String localName,
                        String qName, Attributes attributes)
                        throws SAXException {
                    if (_justOpen != null) {
                        out.print(">");
                    }
                    if ("info".equals(qName)) {
                        _organisation = attributes.getValue("organisation");
                        out.print("<info organisation=\""+_organisation
                                				+"\" module=\""+attributes.getValue("module")+"\"");
                        if (revision != null) {
                            out.print(" revision=\""+revision+"\"");
                        } else if (attributes.getValue("revision") != null) {
                            out.print(" revision=\""+attributes.getValue("revision")+"\"");
                        }
                        if (status != null) {
                            out.print(" status=\""+status+"\"");
                        } else {
                            out.print(" status=\""+attributes.getValue("status")+"\"");
                        }
                        if (pubdate != null) {
                            out.print(" publication=\""+Ivy.DATE_FORMAT.format(pubdate)+"\"");
                        } else if (attributes.getValue("publication") != null) {
                            out.print(" publication=\""+attributes.getValue("publication")+"\"");
                        }
                        if (resolverName != null) {
                            out.print(" resolver=\""+resolverName+"\"");
                        } else if (attributes.getValue("resolver") != null) {
                            out.print(" resolver=\""+attributes.getValue("resolver")+"\"");
                        }
                    } else if ("dependency".equals(qName)) {
                        out.print("<dependency");
                        String org = attributes.getValue("org");
                        org = org == null ? _organisation : org;
                        ModuleId mid = new ModuleId(org, attributes.getValue("name"));
                        for (int i=0; i<attributes.getLength(); i++) {
                            String attName = attributes.getQName(i);
                            if ("rev".equals(attName)) {
                                String rev = (String)resolvedRevisions.get(mid);
                                if (rev != null) {
                                    out.print(" rev=\""+rev+"\"");
                                } else {
                                    out.print(" rev=\""+attributes.getValue("rev")+"\"");
                                }
                            } else {
                                out.print(" "+attName+"=\""+attributes.getValue(attName)+"\"");
                            }
                        }
                    } else {
                        // copy
                        out.print("<"+qName);
                        for (int i=0; i<attributes.getLength(); i++) {
                            out.print(" "+attributes.getQName(i)+"=\""+attributes.getValue(i)+"\"");
                        }
                    }
                    _justOpen = qName;
//                    indent.append("\t");
                }

                public void characters(char[] ch, int start, int length)
                        throws SAXException {
                    if (_justOpen != null) {
                        out.print(">"); 
                        _justOpen = null;
                    }
                    for (int i = start; i < start + length; i++) {
                        out.print(ch[i]);
                    }
                }

                public void endElement(String uri, String localName,
                        String qName) throws SAXException {
                    if (qName.equals(_justOpen)) {
                        out.print("/>");
                    } else {
                        out.print("</"+qName+">");
                    }
                    _justOpen = null;
                }

                public void endDocument() throws SAXException {
                    out.flush();
                    out.close();
                }
                
                public void warning(SAXParseException e) throws SAXException {
                    throw e;
                }
                public void error(SAXParseException e) throws SAXException {
                    throw e;
                }
                public void fatalError(SAXParseException e) throws SAXException {
                    throw e;
                }
            });
        } catch (IOException ex) {
            throw ex;
        } catch (ParserConfigurationException e) {
            IllegalStateException ise = new IllegalStateException("impossible to update "+srcURL+": parser problem");
            ise.initCause(e);
            throw ise;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
        }
    }
    
    /**
     * Copy xml header from src url ivy file to given printwriter
     * In fact, copies everything before <ivy-module to out, except
     * if <ivy-module is not found, in which case nothing is copied.
     * 
     * @param srcURL
     * @param out
     * @throws IOException
     */
    private static void copyHeader(URL srcURL, PrintWriter out) throws IOException {
        StringBuffer buf = new StringBuffer();
        BufferedReader r = new BufferedReader(new InputStreamReader(srcURL.openStream()));
        for (String line = r.readLine(); line != null; line = r.readLine()) {
            int index = line.indexOf("<ivy-module");
            if (index == -1) {
                buf.append(line).append(LINE_SEPARATOR);
            } else {
                buf.append(line.substring(0, index));
                out.print(buf.toString());
                break;
            }
        }
        r.close();
    }

    public static void main(String[] args) throws Exception {
        URL test = new File("test/xml/module1/module1.ivy.xml").toURL();
        Map resolvedRevisions = new HashMap();
        resolvedRevisions.put(new ModuleId("jayasoft", "module3"), "3.3");
        resolvedRevisions.put(new ModuleId("jayasoft", "module4"), "4.4");
        update(test, new File("build/cache/ivy.xml"), resolvedRevisions, "release", "1.3", new Date());
        System.out.println("job done");
    }
}
