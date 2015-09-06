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
package org.apache.ivy.plugins.report;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.util.CacheCleaner;
import org.apache.ivy.util.XMLHelper;
import org.xml.sax.helpers.DefaultHandler;

import junit.framework.TestCase;

public class XmlReportWriterTest extends TestCase {
    private Ivy _ivy;

    private File _cache;

    protected void setUp() throws Exception {
        _ivy = new Ivy();
        _ivy.configure(new File("test/repositories/ivysettings.xml"));
        createCache();
    }

    private void createCache() {
        _cache = new File("build/cache");
        _cache.mkdirs();
    }

    protected void tearDown() throws Exception {
        cleanCache();
    }

    private void cleanCache() {
        CacheCleaner.deleteDir(_cache);
    }

    public void testWriteOrigin() throws Exception {
        ResolveReport report = _ivy.resolve(new File(
                "test/repositories/1/special-encoding-root-ivy.xml"),
            getResolveOptions(new String[] {"default"}));
        assertNotNull(report);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        XmlReportWriter writer = new XmlReportWriter();
        writer.output(report.getConfigurationReport("default"), buffer);
        buffer.flush();
        String xml = buffer.toString(XmlReportWriter.REPORT_ENCODING);

        String expectedLocation = "location=\""
                + new File("test/repositories/1/org1/mod1.2/jars/mod1.2-2.0.jar").getAbsolutePath()
                + "\"";
        String expectedIsLocal = "is-local=\"true\"";
        String expectedOrg = "organisation=\"sp\u00E9cial\"";

        assertTrue("XML doesn't contain artifact location attribute",
            xml.indexOf(expectedLocation) != -1);
        assertTrue("XML doesn't contain artifact is-local attribute",
            xml.indexOf(expectedIsLocal) != -1);
        assertTrue("XML doesn't contain the organisation", xml.indexOf(expectedOrg) != -1);

        // check that the XML is valid
        XMLHelper.parse(new ByteArrayInputStream(buffer.toByteArray()), null, new DefaultHandler(),
            null);
    }

    public void testEscapeXml() throws Exception {
        _ivy.configure(new File("test/repositories/IVY-635/ivysettings.xml"));
        ResolveReport report = _ivy.resolve(new File(
                "test/java/org/apache/ivy/plugins/report/ivy-635.xml"),
            getResolveOptions(new String[] {"default"}));
        assertNotNull(report);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        XmlReportWriter writer = new XmlReportWriter();
        writer.output(report.getConfigurationReport("default"), buffer);
        buffer.flush();
        String xml = buffer.toString();

        String expectedArtName = "art1&amp;_.txt";

        assertTrue("XML doesn't contain escaped artifact name", xml.indexOf(expectedArtName) != -1);
    }

    public void testWriteModuleInfo() throws Exception {
        ResolveReport report = _ivy.resolve(new File(
                "test/java/org/apache/ivy/plugins/report/ivy-with-info.xml"),
            getResolveOptions(new String[] {"default"}).setValidate(false));
        assertNotNull(report);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        XmlReportWriter writer = new XmlReportWriter();
        writer.output(report.getConfigurationReport("default"), buffer);
        buffer.flush();
        String xml = buffer.toString();

        String orgAttribute = "organisation=\"org1\"";
        String modAttribute = "module=\"mod1\"";
        String revAttribute = "revision=\"1.0\"";
        String extra1Attribute = "extra-blabla=\"abc\"";
        String extra2Attribute = "extra-blabla2=\"123\"";

        assertTrue("XML doesn't contain organisation attribute", xml.indexOf(orgAttribute) != -1);
        assertTrue("XML doesn't contain module attribute", xml.indexOf(modAttribute) != -1);
        assertTrue("XML doesn't contain revision attribute", xml.indexOf(revAttribute) != -1);
        assertTrue("XML doesn't contain extra attribute 1", xml.indexOf(extra1Attribute) != -1);
        assertTrue("XML doesn't contain extra attribute 2", xml.indexOf(extra2Attribute) != -1);
    }

    private ResolveOptions getResolveOptions(String[] confs) {
        return new ResolveOptions().setConfs(confs);
    }
}
