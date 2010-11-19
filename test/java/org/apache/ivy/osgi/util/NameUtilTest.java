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
package org.apache.ivy.osgi.util;

import static org.junit.Assert.*;

import org.apache.ivy.osgi.util.NameUtil;
import org.apache.ivy.osgi.util.NameUtil.OrgAndName;
import org.junit.*;


public class NameUtilTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testAsOrgAndName() {
        OrgAndName oan;
        
        oan = NameUtil.instance().asOrgAndName("foo");
        assertEquals("foo", oan.org);
        assertEquals("foo", oan.name);
        
        oan = NameUtil.instance().asOrgAndName("java.foo");
        assertEquals("java", oan.org);
        assertEquals("foo", oan.name);
        
        oan = NameUtil.instance().asOrgAndName("java.foo.bar");
        assertEquals("java", oan.org);
        assertEquals("foo.bar", oan.name);
        
        oan = NameUtil.instance().asOrgAndName("javax.foo");
        assertEquals("javax", oan.org);
        assertEquals("foo", oan.name);
        
        oan = NameUtil.instance().asOrgAndName("javax.foo.bar");
        assertEquals("javax", oan.org);
        assertEquals("foo.bar", oan.name);
        
        oan = NameUtil.instance().asOrgAndName("org.eclipse.foo");
        assertEquals("org.eclipse", oan.org);
        assertEquals("foo", oan.name);
        
        oan = NameUtil.instance().asOrgAndName("org.eclipse.foo.bar");
        assertEquals("org.eclipse", oan.org);
        assertEquals("foo.bar", oan.name);
        
        oan = NameUtil.instance().asOrgAndName("com.eclipse.foo.bar");
        assertEquals("com.eclipse", oan.org);
        assertEquals("foo.bar", oan.name);
        
        oan = NameUtil.instance().asOrgAndName("net.eclipse.foo.bar");
        assertEquals("net.eclipse", oan.org);
        assertEquals("foo.bar", oan.name);
    }

}
