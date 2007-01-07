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
package org.apache.ivy.event;

import java.util.Date;

import org.apache.ivy.DefaultModuleDescriptor;
import org.apache.ivy.Ivy;
import org.apache.ivy.ModuleDescriptor;
import org.apache.ivy.ModuleRevisionId;
import org.apache.ivy.event.IvyEventFilter;
import org.apache.ivy.event.resolve.EndResolveEvent;
import org.apache.ivy.event.resolve.StartResolveEvent;
import org.apache.ivy.report.ResolveReport;

import junit.framework.TestCase;

public class IvyEventFilterTest extends TestCase {
	Ivy ivy = new Ivy();
	ModuleDescriptor md = new DefaultModuleDescriptor(ModuleRevisionId.newInstance("foo", "bar", "1.0"), "integration", new Date());
	ModuleDescriptor md2 = new DefaultModuleDescriptor(ModuleRevisionId.newInstance("foo2", "bar", "1.0"), "integration", new Date());
	ModuleDescriptor md3 = new DefaultModuleDescriptor(ModuleRevisionId.newInstance("foo3", "baz", "1.0"), "integration", new Date());
	ModuleDescriptor md4 = new DefaultModuleDescriptor(ModuleRevisionId.newInstance("foo", "baz", "1.0"), "integration", new Date());

	public void testSimple() {
		IvyEventFilter f = new IvyEventFilter("pre-resolve", null, null);
		
		assertTrue(f.accept(new StartResolveEvent(ivy, md, new String[] {"default"})));
		assertFalse(f.accept(new EndResolveEvent(ivy, md, new String[] {"default"}, new ResolveReport(md))));
	}
	
	public void testSimpleExpression() {
		IvyEventFilter f = new IvyEventFilter("pre-resolve", "organisation = foo", null);
		
		assertTrue(f.accept(new StartResolveEvent(ivy, md, new String[] {"default"})));
		assertFalse(f.accept(new StartResolveEvent(ivy, md2, new String[] {"default"})));
		assertTrue(f.accept(new StartResolveEvent(ivy, md4, new String[] {"default"})));

		f = new IvyEventFilter("pre-resolve", "module = bar", null);
		
		assertTrue(f.accept(new StartResolveEvent(ivy, md, new String[] {"default"})));
		assertTrue(f.accept(new StartResolveEvent(ivy, md2, new String[] {"default"})));
		assertFalse(f.accept(new StartResolveEvent(ivy, md3, new String[] {"default"})));
		assertFalse(f.accept(new StartResolveEvent(ivy, md4, new String[] {"default"})));

		f = new IvyEventFilter("pre-resolve", "organisation = foo, foo2", null);
		
		assertTrue(f.accept(new StartResolveEvent(ivy, md, new String[] {"default"})));
		assertTrue(f.accept(new StartResolveEvent(ivy, md2, new String[] {"default"})));
		assertFalse(f.accept(new StartResolveEvent(ivy, md3, new String[] {"default"})));
		assertTrue(f.accept(new StartResolveEvent(ivy, md4, new String[] {"default"})));
	}

	public void testAndExpression() {
		IvyEventFilter f = new IvyEventFilter("pre-resolve", "organisation = foo AND module = bar", null);
		
		assertTrue(f.accept(new StartResolveEvent(ivy, md, new String[] {"default"})));
		assertFalse(f.accept(new StartResolveEvent(ivy, md2, new String[] {"default"})));
		assertFalse(f.accept(new StartResolveEvent(ivy, md4, new String[] {"default"})));

		f = new IvyEventFilter("pre-resolve", "organisation = foo,foo2 AND module = bar", null);
		
		assertTrue(f.accept(new StartResolveEvent(ivy, md, new String[] {"default"})));
		assertTrue(f.accept(new StartResolveEvent(ivy, md2, new String[] {"default"})));
		assertFalse(f.accept(new StartResolveEvent(ivy, md3, new String[] {"default"})));
		assertFalse(f.accept(new StartResolveEvent(ivy, md4, new String[] {"default"})));
	}

	public void testOrExpression() {
		IvyEventFilter f = new IvyEventFilter("pre-resolve", "organisation = foo3 OR module = bar", null);
		
		assertTrue(f.accept(new StartResolveEvent(ivy, md, new String[] {"default"})));
		assertTrue(f.accept(new StartResolveEvent(ivy, md2, new String[] {"default"})));
		assertTrue(f.accept(new StartResolveEvent(ivy, md3, new String[] {"default"})));
		assertFalse(f.accept(new StartResolveEvent(ivy, md4, new String[] {"default"})));
	}

	public void testNotExpression() {
		IvyEventFilter f = new IvyEventFilter("pre-resolve", "NOT organisation = foo", null);
		
		assertFalse(f.accept(new StartResolveEvent(ivy, md, new String[] {"default"})));
		assertTrue(f.accept(new StartResolveEvent(ivy, md2, new String[] {"default"})));
		assertTrue(f.accept(new StartResolveEvent(ivy, md3, new String[] {"default"})));
		assertFalse(f.accept(new StartResolveEvent(ivy, md4, new String[] {"default"})));
	}
}
