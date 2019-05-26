/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ivy.plugins.parser.m2;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link MavenVersionRangeParser}
 */
public class MavenVersionRangeParserTest {

    /**
     * Tests the {@link MavenVersionRangeParser#rangeAccepts(String, String)} works correctly when the range
     * and the value being compared are the same exact values
     */
    @Test
    public void testExactValue() {
        assertTrue("Exact value was expected to match", MavenVersionRangeParser.rangeAccepts("3", "3"));
        assertTrue("Exact value was expected to match", MavenVersionRangeParser.rangeAccepts("1.0", "1.0"));
        assertTrue("Exact value was expected to match", MavenVersionRangeParser.rangeAccepts("2.1.4", "2.1.4"));
    }

    /**
     * Tests the {@link MavenVersionRangeParser#rangeAccepts(String, String)} works correctly when a range of the form
     * {@code (,1.0]} is used to compare against some value.
     */
    @Test
    public void testLessThanEqualBoundRange() {
        final String range = "(,5.0]";
        assertTrue("<= range was expected to match", MavenVersionRangeParser.rangeAccepts(range, "1"));
        assertTrue("<= range was expected to match", MavenVersionRangeParser.rangeAccepts(range, "3.41.2"));
        assertTrue("<= range was expected to match", MavenVersionRangeParser.rangeAccepts(range, "5.0"));
        assertTrue("<= range was expected to match", MavenVersionRangeParser.rangeAccepts(range, "5"));
        assertTrue("<= range was expected to match", MavenVersionRangeParser.rangeAccepts(range, "0"));


        assertFalse("<= range wasn't expected to match", MavenVersionRangeParser.rangeAccepts(range, "5.0.1"));
        assertFalse("<= range wasn't expected to match", MavenVersionRangeParser.rangeAccepts(range, "5.1"));
        assertFalse("<= range wasn't expected to match", MavenVersionRangeParser.rangeAccepts(range, "6"));
    }

    /**
     * Tests the {@link MavenVersionRangeParser#rangeAccepts(String, String)} works correctly when a range of the form
     * {@code (,1.0)} is used to compare against some value.
     */
    @Test
    public void testLessThanBoundRange() {
        final String range = "(,23.0.1)";
        assertTrue("'<' range was expected to match", MavenVersionRangeParser.rangeAccepts(range, "1"));
        assertTrue("'<' range was expected to match", MavenVersionRangeParser.rangeAccepts(range, "3.41.2"));
        assertTrue("'<' range was expected to match", MavenVersionRangeParser.rangeAccepts(range, "5.0"));
        assertTrue("'<' range was expected to match", MavenVersionRangeParser.rangeAccepts(range, "0"));
        assertTrue("'<' range was expected to match", MavenVersionRangeParser.rangeAccepts(range, "23.0"));


        assertFalse("'<' range wasn't expected to match", MavenVersionRangeParser.rangeAccepts(range, "23.0.1"));
        assertFalse("'<' range wasn't expected to match", MavenVersionRangeParser.rangeAccepts(range, "24"));
        assertFalse("'<' range wasn't expected to match", MavenVersionRangeParser.rangeAccepts(range, "26.2"));
    }

    /**
     * Tests the {@link MavenVersionRangeParser#rangeAccepts(String, String)} works correctly when a range of the form
     * {@code [1.0]} is used to compare against some value.
     */
    @Test
    public void testEqualsBoundRange() {
        final String range = "[1.0]";
        assertTrue("range was expected to match", MavenVersionRangeParser.rangeAccepts(range, "1.0"));
        assertFalse("range wasn't expected to match", MavenVersionRangeParser.rangeAccepts(range, "1.0.1"));
    }

    /**
     * Tests the {@link MavenVersionRangeParser#rangeAccepts(String, String)} works correctly when a range of the form
     * {@code [1.0,)} is used to compare against some value.
     */
    @Test
    public void testGreaterThanEqualBoundRange() {
        final String range = "[7.0,)";
        assertTrue(">= range was expected to match", MavenVersionRangeParser.rangeAccepts(range, "7"));
        assertTrue(">= range was expected to match", MavenVersionRangeParser.rangeAccepts(range, "7.41.2"));
        assertTrue(">= range was expected to match", MavenVersionRangeParser.rangeAccepts(range, "7.0"));
        assertTrue(">= range was expected to match", MavenVersionRangeParser.rangeAccepts(range, "10"));
        assertTrue(">= range was expected to match", MavenVersionRangeParser.rangeAccepts(range, "8.2"));


        assertFalse(">= range wasn't expected to match", MavenVersionRangeParser.rangeAccepts(range, "5.0.1"));
        assertFalse(">= range wasn't expected to match", MavenVersionRangeParser.rangeAccepts(range, "5.1"));
        assertFalse(">= range wasn't expected to match", MavenVersionRangeParser.rangeAccepts(range, "6"));
        assertFalse(">= range wasn't expected to match", MavenVersionRangeParser.rangeAccepts(range, "0"));
    }

    /**
     * Tests the {@link MavenVersionRangeParser#rangeAccepts(String, String)} works correctly when a range of the form
     * {@code (1.0,)} is used to compare against some value.
     */
    @Test
    public void testGreaterThanBoundRange() {
        final String range = "(11.0,)";
        assertTrue("'>' range was expected to match", MavenVersionRangeParser.rangeAccepts(range, "12"));
        assertTrue("'>' range was expected to match", MavenVersionRangeParser.rangeAccepts(range, "42.121.1"));
        assertTrue("'>' range was expected to match", MavenVersionRangeParser.rangeAccepts(range, "12.0"));
        assertTrue("'>' range was expected to match", MavenVersionRangeParser.rangeAccepts(range, "50"));


        assertFalse("'>' range wasn't expected to match", MavenVersionRangeParser.rangeAccepts(range, "10.9"));
        assertFalse("'>' range wasn't expected to match", MavenVersionRangeParser.rangeAccepts(range, "11"));
        assertFalse("'>' range wasn't expected to match", MavenVersionRangeParser.rangeAccepts(range, "11.0"));
    }

    /**
     * Tests the {@link MavenVersionRangeParser#rangeAccepts(String, String)} works correctly when a range of the form
     * {@code (,1.0],[1.2,)} is used to compare against some value.
     */
    @Test
    public void testMultiRange() {
        final String range = "(,1.0],[1.2,)"; // x <= 1.0 or x >= 1.2
        assertTrue("Range with multiple sets was expected to match", MavenVersionRangeParser.rangeAccepts(range, "1.0"));
        assertTrue("Range with multiple sets was expected to match", MavenVersionRangeParser.rangeAccepts(range, "1"));
        assertTrue("Range with multiple sets was expected to match", MavenVersionRangeParser.rangeAccepts(range, "0.9"));
        assertTrue("Range with multiple sets was expected to match", MavenVersionRangeParser.rangeAccepts(range, "0.9.5"));
        assertTrue("Range with multiple sets was expected to match", MavenVersionRangeParser.rangeAccepts(range, "22"));
        assertTrue("Range with multiple sets was expected to match", MavenVersionRangeParser.rangeAccepts(range, "1.2"));
        assertTrue("Range with multiple sets was expected to match", MavenVersionRangeParser.rangeAccepts(range, "1.2.0"));

        assertFalse("Range with multiple sets wasn't expected to match", MavenVersionRangeParser.rangeAccepts(range, "1.1"));
        assertFalse("Range with multiple sets wasn't expected to match", MavenVersionRangeParser.rangeAccepts(range, "1.0.1"));
    }

    /**
     * Tests the {@link MavenVersionRangeParser#rangeAccepts(String, String)} works correctly when a range of the form
     * {@code (,1.1),(1.1,)} is used to compare against some value.
     */
    @Test
    public void testMultiRangeSpecificValue() {
        final String range = "(,1.1),(1.1,)"; // x != 1.1
        assertTrue("Range with multiple sets was expected to match", MavenVersionRangeParser.rangeAccepts(range, "1.0"));
        assertTrue("Range with multiple sets was expected to match", MavenVersionRangeParser.rangeAccepts(range, "1.0.1"));
        assertTrue("Range with multiple sets was expected to match", MavenVersionRangeParser.rangeAccepts(range, "1.2"));

        assertFalse("Range with multiple sets wasn't expected to match", MavenVersionRangeParser.rangeAccepts(range, "1.1"));
    }
}
