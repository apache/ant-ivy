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
package org.apache.ivy.report;

import java.io.ByteArrayOutputStream;
import java.io.File;

import junit.framework.TestCase;

import org.apache.ivy.Ivy;
import org.apache.ivy.report.ResolveReport;
import org.apache.ivy.report.XmlReportOutputter;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;


public class XmlReportOutputterTest extends TestCase {
	private final Ivy _ivy;
    private File _cache;

    public XmlReportOutputterTest() throws Exception {
        _ivy = new Ivy();
        _ivy.configure(new File("test/repositories/ivyconf.xml"));
    }

    protected void setUp() throws Exception {
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
        Delete del = new Delete();
        del.setProject(new Project());
        del.setDir(_cache);
        del.execute();
    }
    
	public void testWriteOrigin() throws Exception {
        ResolveReport report = _ivy.resolve(new File("test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml").toURL(),
                null, new String[] {"default"}, _cache, null, true);
        assertNotNull(report);
		
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		XmlReportOutputter outputter = new XmlReportOutputter();
		outputter.output(report.getConfigurationReport("default"), buffer);
		buffer.flush();
		String xml = buffer.toString();
		
        String expectedLocation = "location=\"" + new File("test/repositories/1/org1/mod1.2/jars/mod1.2-2.0.jar").getAbsolutePath() + "\"";
        String expectedIsLocal = "is-local=\"true\"";

        assertTrue("XML doesn't contain artifact location attribute", xml.indexOf(expectedLocation) != -1);
        assertTrue("XML doesn't contain artifact is-local attribute", xml.indexOf(expectedIsLocal) != -1);
	}
}
