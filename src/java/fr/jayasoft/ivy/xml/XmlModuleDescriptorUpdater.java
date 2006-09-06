/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.xml;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.ModuleRevisionId;
import fr.jayasoft.ivy.namespace.Namespace;
import fr.jayasoft.ivy.util.Message;
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
    public static void update(URL srcURL, File destFile, final Map resolvedRevisions, final String status, 
            final String revision, final Date pubdate) 
                                throws IOException, SAXException {
        update(null, srcURL, destFile, resolvedRevisions, status, revision, pubdate, null, false);
    }

    public static void update(final Ivy ivy, URL srcURL, File destFile, final Map resolvedRevisions, final String status, 
            final String revision, final Date pubdate, final Namespace ns, final boolean replaceInclude) 
                                throws IOException, SAXException {
    	update(ivy, srcURL.openStream(), destFile, resolvedRevisions, status, revision, pubdate, ns, replaceInclude);
    }
    
    public static void update(final Ivy ivy, InputStream in, File destFile, final Map resolvedRevisions, final String status, 
            final String revision, final Date pubdate, final Namespace ns, final boolean replaceInclude) 
                                throws IOException, SAXException {
        if (destFile.getParentFile() != null) {
            destFile.getParentFile().mkdirs();
        }
        OutputStream fos = new FileOutputStream(destFile);
        try {
           update(ivy, in, fos, resolvedRevisions, status, revision, pubdate, ns, replaceInclude);
        } finally {
           try {
               in.close();
           } catch (IOException e) {}
           try {
               fos.close();
           } catch (IOException e) {}
        }
    }
    
    public static void update(final Ivy ivy, InputStream inStream, OutputStream outStream, final Map resolvedRevisions, final String status, 
            final String revision, final Date pubdate, final Namespace ns, final boolean replaceInclude) 
                                throws IOException, SAXException {
        final PrintWriter out = new PrintWriter(outStream);
        final BufferedInputStream in = new BufferedInputStream(inStream);
        
        in.mark(10000); // assume the header is never larger than 10000 bytes.
        copyHeader(in, out);
        in.reset(); // reposition the stream at the beginning
            
        try {
            XMLHelper.parse(in, null, new DefaultHandler() {
                // never print *ln* cause \n is found in copied characters stream
                // nor do we need do handle indentation, original one is maintained except for attributes
                
                private String _organisation = null;
                private String _defaultConfMapping = null; // defaultConfMapping of imported configurations, if any
                private Boolean _confMappingOverride = null; // confMappingOverride of imported configurations, if any
                private String _justOpen = null; // used to know if the last open tag was empty, to adjust termination with /> instead of ></qName>
                private Stack _context = new Stack();
                public void startElement(String uri, String localName,
                        String qName, Attributes attributes)
                        throws SAXException {
                    if (_justOpen != null) {
                        out.print(">");
                    }
                    _context.push(qName);
                    if ("info".equals(qName)) {
                        _organisation = substitute(ivy, attributes.getValue("organisation"));
                        out.print("<info organisation=\""+_organisation
                                				+"\" module=\""+substitute(ivy, attributes.getValue("module"))+"\"");
                        if (revision != null) {
                            out.print(" revision=\""+revision+"\"");
                        } else if (attributes.getValue("revision") != null) {
                            out.print(" revision=\""+substitute(ivy, attributes.getValue("revision"))+"\"");
                        }
                        if (status != null) {
                            out.print(" status=\""+status+"\"");
                        } else {
                            out.print(" status=\""+substitute(ivy, attributes.getValue("status"))+"\"");
                        }
                        if (pubdate != null) {
                            out.print(" publication=\""+Ivy.DATE_FORMAT.format(pubdate)+"\"");
                        } else if (attributes.getValue("publication") != null) {
                            out.print(" publication=\""+substitute(ivy, attributes.getValue("publication"))+"\"");
                        }
                        Collection stdAtts = Arrays.asList(new String[] {"organisation", "module", "revision", "status", "publication", "namespace"});
                        if (attributes.getValue("namespace") != null) {
                            out.print(" namespace=\""+substitute(ivy, attributes.getValue("namespace"))+"\"");
                        }
                        for (int i=0; i<attributes.getLength(); i++) {
                        	if (!stdAtts.contains(attributes.getQName(i))) {
                        		out.print(" "+attributes.getQName(i)+"=\""+substitute(ivy, attributes.getValue(i))+"\"");
                        	}
                        }
                    } else if (replaceInclude && "include".equals(qName) && _context.contains("configurations")) {
                        try {
                            URL url;
                            String fileName = substitute(ivy, attributes.getValue("file"));
                            if (fileName == null) {
                                String urlStr = substitute(ivy, attributes.getValue("url"));
                                url = new URL(urlStr);
                            } else {
                                url = new File(fileName).toURL();
                            }     
                            XMLHelper.parse(url, null, new DefaultHandler() {
                                boolean _first = true;
                                public void startElement(String uri, String localName,
                                        String qName, Attributes attributes)
                                        throws SAXException {
                                    if ("configurations".equals(qName)) {
                                        String defaultconf = substitute(ivy, attributes.getValue("defaultconfmapping"));
                                        if (defaultconf != null) {
                                            _defaultConfMapping = defaultconf;
                                        }
                                        String mappingOverride = substitute(ivy, attributes.getValue("confmappingoverride"));
                                        if (mappingOverride != null) {
                                           _confMappingOverride = Boolean.valueOf(mappingOverride);
                                        }
                                    } else if ("conf".equals(qName)) {
                                        // copy
                                        if (!_first) {
                                            out.print("/>\n\t\t");
                                        } else {
                                            _first = false;
                                        }
                                        out.print("<"+qName);
                                        for (int i=0; i<attributes.getLength(); i++) {
                                            out.print(" "+attributes.getQName(i)+"=\""+substitute(ivy, attributes.getValue(i))+"\"");
                                        }
                                    }
                                }
                            });
                        } catch (Exception e) {
                            Message.warn("exception occured while importing configurations: "+e.getMessage());
                            throw new SAXException(e);
                        }
                    } else if ("dependency".equals(qName)) {
                        out.print("<dependency");
                        String org = substitute(ivy, attributes.getValue("org"));
                        org = org == null ? _organisation : org;
                        String module = substitute(ivy, attributes.getValue("name"));
                        String branch = substitute(ivy, attributes.getValue("branch"));
                        String revision = substitute(ivy, attributes.getValue("rev"));
                        ModuleRevisionId localMid = ModuleRevisionId.newInstance(org, module, branch, revision);
                        ModuleRevisionId systemMid = ns == null ? 
                                localMid : 
                                ns.getToSystemTransformer().transform(localMid);
                        
                        for (int i=0; i<attributes.getLength(); i++) {
                            String attName = attributes.getQName(i);
                            if ("rev".equals(attName)) {
                                String rev = (String)resolvedRevisions.get(systemMid);
                                if (rev != null) {
                                    out.print(" rev=\""+rev+"\"");
                                } else {
                                    out.print(" rev=\""+systemMid.getRevision()+"\"");
                                }
                            } else if ("org".equals(attName)) {
                                out.print(" org=\""+systemMid.getOrganisation()+"\"");
                            } else if ("name".equals(attName)) {
                                out.print(" name=\""+systemMid.getName()+"\"");
                            } else if ("branch".equals(attName)) {
                                out.print(" branch=\""+systemMid.getBranch()+"\"");
                            } else {
                                out.print(" "+attName+"=\""+substitute(ivy, attributes.getValue(attName))+"\"");
                            }
                        }
                    } else if ("dependencies".equals(qName)) {
                        // copy
                        out.print("<"+qName);
                        for (int i=0; i<attributes.getLength(); i++) {
                            out.print(" "+attributes.getQName(i)+"=\""+substitute(ivy, attributes.getValue(i))+"\"");
                        }
                        // add default conf mapping if needed
                        if (_defaultConfMapping != null && attributes.getValue("defaultconfmapping") == null) {
                            out.print(" defaultconfmapping=\""+_defaultConfMapping+"\"");
                        }
                        // add confmappingoverride if needed
                        if (_confMappingOverride != null && attributes.getValue("confmappingoverride") == null) {
                           out.print(" confmappingoverride=\""+_confMappingOverride.toString()+"\"");
                        }
                    } else {
                        // copy
                        out.print("<"+qName);
                        for (int i=0; i<attributes.getLength(); i++) {
                            out.print(" "+attributes.getQName(i)+"=\""+substitute(ivy, attributes.getValue(i))+"\"");
                        }
                    }
                    _justOpen = qName;
//                    indent.append("\t");
                }

                private String substitute(Ivy ivy, String value) {
                    return ivy == null ? value : ivy.substitute(value);
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
                    _context.pop();
                }

                public void endDocument() throws SAXException {
                    out.print(LINE_SEPARATOR);
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
            }, new LexicalHandler() {
				public void endCDATA() throws SAXException {
				}

				public void endDTD() throws SAXException {
				}

				public void startCDATA() throws SAXException {
				}

				public void comment(char[] ch, int start, int length) throws SAXException {
					StringBuffer comment = new StringBuffer();
					comment.append(ch, start, length);
					out.print("<!--");
					out.print(comment.toString());
					out.print("-->");
				}

				public void endEntity(String name) throws SAXException {
				}

				public void startEntity(String name) throws SAXException {
				}

				public void startDTD(String name, String publicId, String systemId) throws SAXException {
				}
            });
        } catch (ParserConfigurationException e) {
            IllegalStateException ise = new IllegalStateException("impossible to update Ivy files: parser problem");
            ise.initCause(e);
            throw ise;
        }
    }
    
    /**
     * Copy xml header from src url ivy file to given printwriter
     * In fact, copies everything before <ivy-module to out, except
     * if <ivy-module is not found, in which case nothing is copied.
     * 
     * @param in
     * @param out
     * @throws IOException
     */
    private static void copyHeader(InputStream in, PrintWriter out) throws IOException {
        StringBuffer buf = new StringBuffer();
        BufferedReader r = new BufferedReader(new InputStreamReader(in));
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
        //r.close();
    }
}
