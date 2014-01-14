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
package org.apache.ivy;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.repository.file.FileResource;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.plugins.resolver.util.ResolvedResource;

/**
 * Fixture easing the development of tests requiring to set up a simple repository with some
 * modules, using micro ivy format to describe the repository. <br/>
 * Example of use:
 * 
 * <pre>
 * public class MyTest extends TestCase {
 *     private TestFixture fixture;
 * 
 *     protected void setUp() throws Exception {
 *         fixture = new TestFixture();
 *         // additional setup here
 *     }
 * 
 *     protected void tearDown() throws Exception {
 *         fixture.clean();
 *     }
 * 
 *     public void testXXX() throws Exception {
 *         fixture.addMD(&quot;#A;1-&gt; { #B;[1.5,1.6] #C;2.5 }&quot;).addMD(&quot;#B;1.5-&gt;#D;2.0&quot;)
 *                 .addMD(&quot;#B;1.6-&gt;#D;2.0&quot;).addMD(&quot;#C;2.5-&gt;#D;[1.0,1.6]&quot;).addMD(&quot;#D;1.5&quot;)
 *                 .addMD(&quot;#D;1.6&quot;).addMD(&quot;#D;2.0&quot;).init();
 *         ResolveReport r = fixture.resolve(&quot;#A;1&quot;);
 *         // assertions go here
 *     }
 * }
 * </pre>
 */
public class TestFixture {

    private Collection mds = new ArrayList();

    private Ivy ivy;

    public TestFixture() {
        try {
            this.ivy = new Ivy();
            IvySettings settings = new IvySettings();
            settings.defaultInit();
            ivy.setSettings(settings);
            TestHelper.loadTestSettings(ivy.getSettings());
            ivy.bind();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public TestFixture addMD(String microIvy) {
        mds.add(TestHelper.parseMicroIvyDescriptor(microIvy));
        return this;
    }

    public TestFixture init() throws IOException {
        TestHelper.fillRepository(getTestRepository(), mds);
        return this;
    }

    private DependencyResolver getTestRepository() {
        return ivy.getSettings().getResolver("test");
    }

    public IvySettings getSettings() {
        return ivy.getSettings();
    }

    public Ivy getIvy() {
        return ivy;
    }

    public void clean() {
        TestHelper.cleanTest();
    }

    public File getIvyFile(String mrid) {
        ResolvedResource r = getTestRepository().findIvyFileRef(
            new DefaultDependencyDescriptor(ModuleRevisionId.parse(mrid), false),
            TestHelper.newResolveData(getSettings()));
        if (r == null) {
            throw new IllegalStateException("module not found: " + mrid);
        }
        return ((FileResource) r.getResource()).getFile();
    }

    public ResolveReport resolve(String mrid) throws MalformedURLException, ParseException,
            IOException {
        return ivy.resolve(getIvyFile(mrid), TestHelper.newResolveOptions(getSettings()));
    }

}
