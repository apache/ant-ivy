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
package org.apache.ivy.osgi.core;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;

@RunWith(Enclosed.class)
public class ManifestHeaderTest {

    public static class SingleTests {

        @Test
        public void testNormal() throws Exception {
            ManifestHeaderElement simpleValue = new ManifestHeaderElement();
            simpleValue.addValue("value");
            ManifestHeaderValue header = new ManifestHeaderValue();
            header.addElement(simpleValue);
            assertEquals(header, new ManifestHeaderValue("value"));

            ManifestHeaderElement simplePackage = new ManifestHeaderElement();
            simplePackage.addValue("simple.package");
            header.addElement(simplePackage);
            assertEquals(header, new ManifestHeaderValue("value,simple.package"));

            ManifestHeaderElement doubleValue = new ManifestHeaderElement();
            doubleValue.addValue("value1");
            doubleValue.addValue("value2");
            ManifestHeaderValue headerDouble = new ManifestHeaderValue();
            headerDouble.addElement(doubleValue);
            assertEquals(headerDouble, new ManifestHeaderValue("value1;value2"));

            ManifestHeaderElement versionValue = new ManifestHeaderElement();
            versionValue.addValue("value1");
            versionValue.addValue("value2");
            versionValue.addAttribute("version", "1.2.3");
            ManifestHeaderValue headerVersion = new ManifestHeaderValue();
            headerVersion.addElement(versionValue);
            assertEquals(headerVersion, new ManifestHeaderValue("value1;value2;version=1.2.3"));

            ManifestHeaderElement optionValue1 = new ManifestHeaderElement();
            optionValue1.addValue("value1");
            optionValue1.addDirective("resolution", "optional");
            ManifestHeaderElement optionValue2 = new ManifestHeaderElement();
            optionValue2.addValue("value2");
            optionValue2.addValue("value3");
            optionValue2.addDirective("resolution", "mandatory");
            optionValue2.addAttribute("version", "1.2.3");
            ManifestHeaderValue headerOption = new ManifestHeaderValue();
            headerOption.addElement(optionValue1);
            headerOption.addElement(optionValue2);
            assertEquals(headerOption, new ManifestHeaderValue(
                    "value1;resolution:=optional,value2;value3;resolution:=mandatory;version=1.2.3"));

            ManifestHeaderElement quoteValue = new ManifestHeaderElement();
            quoteValue.addValue("value1");
            quoteValue.addAttribute("att", "value2;value3");
            ManifestHeaderValue headerQuote = new ManifestHeaderValue();
            headerQuote.addElement(quoteValue);
            assertEquals(headerQuote, new ManifestHeaderValue("value1;att='value2;value3'"));
        }

        @Test
        public void testSpaceInValue() throws Exception {
            ManifestHeaderValue value = new ManifestHeaderValue("glassfish javax.servlet.3.1.0.b33");
            assertEquals("glassfish javax.servlet.3.1.0.b33", value.getSingleValue());
        }

    }

    @RunWith(Parameterized.class)
    public static class IllegalOptionTests {

        @Rule
        public ExpectedException expExc = ExpectedException.none();

        @Parameterized.Parameters(name = "Illegal token at {1} in {0}")
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][] {{"value1=", 6},
                    {"value1;version=1;value2", 22}, {"value1;version=", 14},
                    {"value1;version:", 14}, {"value1;version:=", 15},
                    {"value1;=1", 7}, {"value1;att=''value", 13}});
        }

        @Parameterized.Parameter
        public String value;

        @Parameterized.Parameter(1)
        public int offset;

        /**
         * Expected failure when the parameter is illegal
         */
        @Test
        public void testSyntaxError() throws ParseException {
            expExc.expect(ParseException.class);
            expExc.expect(hasProperty("errorOffset", is(offset)));
            new ManifestHeaderValue(value);
        }

    }

    @RunWith(Parameterized.class)
    public static class OptionNormalisationTests {

        @Parameterized.Parameters(name = "Value {1} is normalised to {0}")
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][] {
                    {"value1;att=value2;att2=other", "value1;att='value2';att2=other"},
                    {"value1;att=value2;att2=other", "value1;att=  'value2'  ;att2=other"},
                    {"value1;att=value2;att2=other", "value1;att=   value2 \t ;att2=other"},
                    {"value1;att:=value2;att2=other", "value1;att:=  'value2'  ;att2=other"},
                    {"value1;att=value2;att2=other", "value1;att=\"value2\";att2=other"},
                    {"value1;value2", "value2;value1"}, {"value1,value2", "value2,value1"},
                    {"value1;resolution:=mandatory;color:=red",
                    "value1;color:=red;resolution:=mandatory"},
                    {"value1;version=1.2.3;color=red", "value1;color=red;version=1.2.3"},
                    {"value1;version=1.2.3;color:=red", "value1;color:=red;version=1.2.3"}});
        }

        @Parameterized.Parameter
        public String value;

        @Parameterized.Parameter(1)
        public String normalised;

        @Test
        public void testNormalisation() throws Exception {
            assertEquals(new ManifestHeaderValue(normalised), new ManifestHeaderValue(value));
        }

    }

}
