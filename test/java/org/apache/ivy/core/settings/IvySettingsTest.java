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
package org.apache.ivy.core.settings;

import java.io.IOException;
import java.text.ParseException;

import org.apache.ivy.Ivy;
import org.apache.ivy.plugins.resolver.DependencyResolver;

import junit.framework.TestCase;

public class IvySettingsTest extends TestCase {

    public void testChangeDefaultResolver() throws ParseException, IOException {
        Ivy ivy = new Ivy();
        ivy.configureDefault();

        IvySettings settings = ivy.getSettings();
        DependencyResolver defaultResolver = settings.getDefaultResolver();

        assertNotNull(defaultResolver);
        assertEquals("default", defaultResolver.getName());
        assertSame("default resolver cached", defaultResolver, settings.getDefaultResolver());

        settings.setDefaultResolver("public");
        DependencyResolver newDefault = settings.getDefaultResolver();
        assertNotNull(newDefault);
        assertNotSame("default resolver has changed", defaultResolver, newDefault);
        assertEquals("resolver changed successfully", "public", newDefault.getName());
    }

    public void testVariables() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configureDefault();
        IvySettings settings = ivy.getSettings();

        // test set
        assertNull(settings.getVariable("foo"));
        settings.setVariable("foo", "bar", false, null, null);
        assertEquals("bar", settings.getVariable("foo"));

        // test no override
        settings.setVariable("foo", "wrong", false, null, null);
        assertEquals("bar", settings.getVariable("foo"));

        // test override
        settings.setVariable("foo", "right", true, null, null);
        assertEquals("right", settings.getVariable("foo"));

        // test ifset no exist
        assertNull(settings.getVariable("bar"));
        settings.setVariable("bar", "foo", true, "noexist", null);
        assertNull(settings.getVariable("bar"));

        // test ifset exist
        settings.setVariable("bar", "foo", true, "foo", null);
        assertEquals("foo", settings.getVariable("bar"));

        // test unlessset exist
        assertNull(settings.getVariable("thing"));
        settings.setVariable("thing", "foo", true, null, "foo");
        assertNull(settings.getVariable("thing"));

        // test unlessset noexist
        settings.setVariable("thing", "foo", true, null, "noexist");
        assertEquals("foo", settings.getVariable("thing"));

        // test ifset no exist and unlessset exist
        assertNull(settings.getVariable("ivy"));
        settings.setVariable("ivy", "rocks", true, "noexist", "foo");
        assertNull(settings.getVariable("ivy"));

        // test ifset no exist and unlessset no exist
        settings.setVariable("ivy", "rocks", true, "noexist", "noexist");
        assertNull(settings.getVariable("ivy"));

        // test ifset exist and unlessset exist
        settings.setVariable("ivy", "rocks", true, "foo", "foo");
        assertNull(settings.getVariable("ivy"));

        // test ifset exist and unlessset no exist
        settings.setVariable("ivy", "rocks", true, "foo", "noexist");
        assertEquals("rocks", settings.getVariable("ivy"));
    }
}
