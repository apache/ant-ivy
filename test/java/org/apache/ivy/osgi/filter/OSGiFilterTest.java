/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivy.osgi.filter;

import static org.junit.Assert.assertEquals;

import java.text.ParseException;

import org.apache.ivy.osgi.filter.CompareFilter.Operator;
import org.junit.Test;

public class OSGiFilterTest {

    @Test(expected = ParseException.class)
    public void testParserFailureNoParens() throws ParseException {
        OSGiFilterParser.parse("c>2");
    }

    @Test(expected = ParseException.class)
    public void testParserFailureEmpty() throws ParseException {
        OSGiFilterParser.parse("");
    }

    @Test(expected = ParseException.class)
    public void testParserFailureSingleParens() throws ParseException {
        OSGiFilterParser.parse(")");
    }

    @Test
    public void testParser() throws Exception {
        OSGiFilter cgt2 = new CompareFilter("c", Operator.GREATER_THAN, "2");
        checkParse(cgt2, "(c>2)");
        OSGiFilter twoeqd = new CompareFilter("2", Operator.EQUALS, "d");
        checkParse(twoeqd, "(2=d)");
        OSGiFilter foodotbarge0dot0 = new CompareFilter("foo.bar", Operator.GREATER_OR_EQUAL, "0.0");
        checkParse(foodotbarge0dot0, "(foo.bar>=0.0)");
        OSGiFilter and = new AndFilter(new OSGiFilter[] {foodotbarge0dot0});
        checkParse(and, "(&(foo.bar>=0.0))");
        OSGiFilter and2 = new AndFilter(new OSGiFilter[] {cgt2, twoeqd, foodotbarge0dot0});
        checkParse(and2, "(&(c>2)(2=d)(foo.bar>=0.0))");
        OSGiFilter spaceAfterAnd = new AndFilter(new OSGiFilter[] {twoeqd});
        checkParse(spaceAfterAnd, "(& (2=d))");

        OSGiFilter version350 = new CompareFilter("version", Operator.GREATER_OR_EQUAL, "3.5.0");
        OSGiFilter version400 = new CompareFilter("version", Operator.GREATER_OR_EQUAL, "4.0.0");
        OSGiFilter notVersion400 = new NotFilter(version400);
        OSGiFilter bundle = new CompareFilter("bundle", Operator.EQUALS, "org.eclipse.core.runtime");
        OSGiFilter andEverythingWithSpace = new AndFilter(new OSGiFilter[] {version350,
                notVersion400, bundle});
        checkParse(andEverythingWithSpace,
            "(&     (version>=3.5.0)     (!(version>=4.0.0))     (bundle=org.eclipse.core.runtime)    )");
    }

    private void checkParse(OSGiFilter expected, String toParse) throws ParseException {
        OSGiFilter parsed = OSGiFilterParser.parse(toParse);
        assertEquals(expected, parsed);
    }
}
