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

import java.io.File;

import org.apache.ivy.Ivy;
import org.apache.ivy.TestHelper;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;

import junit.framework.TestCase;

public class XmlReportParserTest extends TestCase {

    private Ivy ivy;

    protected void setUp() throws Exception {
        ivy = new Ivy();
        ivy.configure(new File("test/repositories/ivysettings.xml"));
        TestHelper.createCache();
    }

    protected void tearDown() throws Exception {
        TestHelper.cleanCache();
    }

    public void testGetResolvedModule() throws Exception {
        ResolveReport report = ivy.resolve(
            new File("test/java/org/apache/ivy/plugins/report/ivy-with-info.xml"),
            getResolveOptions(new String[] {"default"}).setValidate(false).setResolveId(
                "testGetResolvedModule"));
        assertNotNull(report);

        ModuleRevisionId modRevId = report.getModuleDescriptor().getModuleRevisionId();

        XmlReportParser parser = new XmlReportParser();
        parser.parse(ivy.getResolutionCacheManager().getConfigurationResolveReportInCache(
            "testGetResolvedModule", "default"));
        ModuleRevisionId parsedModRevId = parser.getResolvedModule();

        assertEquals("Resolved module doesn't equals parsed module", modRevId, parsedModRevId);
    }

    private ResolveOptions getResolveOptions(String[] confs) {
        return new ResolveOptions().setConfs(confs);
    }
}
