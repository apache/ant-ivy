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
package org.apache.ivy.plugins.conflict;

import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.cache.CacheManager;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.core.resolve.ResolveOptions;

public class LatestConflictManagerTest extends TestCase {

	private Ivy ivy;

	protected void setUp() throws Exception {
		ivy = new Ivy();
		ivy.configure(LatestConflictManagerTest.class
				.getResource("ivyconf-latest.xml"));
	}

	// Test case for issue IVY-388
	public void testIvy388() throws Exception {
		ResolveReport report = ivy.resolve(LatestConflictManagerTest.class
				.getResource("ivy-388.xml"), 
				getResolveOptions());

		List deps = report.getDependencies();
		Iterator dependencies = deps.iterator();
		String[] confs = report.getConfigurations();
		while (dependencies.hasNext()) {
			IvyNode node = (IvyNode) dependencies.next();
			for (int i = 0; i < confs.length; i++) {
				String conf = confs[i];
				if (!node.isEvicted(conf)) {
					boolean flag1 = report.getConfigurationReport(conf)
							.getDependency(node.getResolvedId()) != null;
					boolean flag2 = report.getConfigurationReport(conf)
							.getModuleRevisionIds().contains(node.getResolvedId());
					assertEquals("Inconsistent data for node " + node + " in conf " + conf , flag1, flag2);
				}
			}
		}
	}
    
    private ResolveOptions getResolveOptions() {
		return new ResolveOptions().setCache(CacheManager.getInstance(ivy.getSettings())).setValidate(false);
	}
}
