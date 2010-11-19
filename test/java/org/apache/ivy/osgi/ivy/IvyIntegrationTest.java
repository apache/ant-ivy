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
package org.apache.ivy.osgi.ivy;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.plugins.parser.AbstractModuleDescriptorParserTester;

public class IvyIntegrationTest extends AbstractModuleDescriptorParserTester {

    private URL getTestResource(String resource) throws MalformedURLException {
        return new File("test/test-ivy/" + resource).toURI().toURL();
    }

    public void testAcmeResolveAlpha() throws Exception {
        final Ivy ivy = Ivy.newInstance();
        ivy.configure(getTestResource("acme-ivysettings.xml"));

        final ModuleRevisionId mrid = new ModuleRevisionId(new ModuleId("com.acme", "alpha"),
                "1.0+");
        // final ModuleRevisionId mrid = new ModuleRevisionId(new ModuleId("com.acme", "delta"),
        // "4+");
        // final ModuleRevisionId mrid = new ModuleRevisionId(new ModuleId("com.acme", "echo"),
        // "5+");
        final ResolveOptions options = new ResolveOptions();
        options.setConfs(new String[] {"default"});

        final ResolveReport report = ivy.resolve(mrid, options, false);
        assertEquals(5, report.getDependencies().size());

        final String[] names = new String[] {"com.acme#alpha;1.0.0.20080101",
                "com.acme#bravo;2.0.0.20080202", "com.acme#charlie;3.0.0.20080303",
                "com.acme#delta;4.0.0", "com.acme#echo;5.0.0"};
        final Set/* <String> */nodeNames = new HashSet/* <String> */(Arrays.asList(names));
        Iterator itNode = ((Collection/* <IvyNode> */) report.getDependencies()).iterator();
        while (itNode.hasNext()) {
            IvyNode node = (IvyNode) itNode.next();
            assertTrue(" Contains: " + node, nodeNames.contains(node.toString()));
        }
    }
}
