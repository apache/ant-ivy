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
package org.apache.ivy.osgi.core;

import java.text.ParseException;

import junit.framework.TestCase;

public class ManifestHeaderTest extends TestCase {

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

    private void genericTestEquals(String v1, String v2) throws Exception {
        assertEquals(new ManifestHeaderValue(v1), new ManifestHeaderValue(v2));
    }

    public void testSpaceAndQuote() throws Exception {
        genericTestEquals("value1;att=value2;att2=other", "value1;att='value2';att2=other");
        genericTestEquals("value1;att=value2;att2=other", "value1;att=  'value2'  ;att2=other");
        genericTestEquals("value1;att=value2;att2=other", "value1;att=   value2 \t ;att2=other");
        genericTestEquals("value1;att:=value2;att2=other", "value1;att:=  'value2'  ;att2=other");
        genericTestEquals("value1;att=value2;att2=other", "value1;att=\"value2\";att2=other");
    }

    public void testReflexivity() throws Exception {
        genericTestEquals("value1;value2", "value2;value1");
        genericTestEquals("value1,value2", "value2,value1");
        genericTestEquals("value1;resolution:=mandatory;color:=red",
            "value1;color:=red;resolution:=mandatory");
        genericTestEquals("value1;version=1.2.3;color=red", "value1;color=red;version=1.2.3");
        genericTestEquals("value1;version=1.2.3;color:=red", "value1;color:=red;version=1.2.3");
    }

    public void testSyntaxError() throws Exception {
        genericTestSyntaxError("value1=");
        genericTestSyntaxError("value1;version=1;value2");
        genericTestSyntaxError("value1;version=");
        genericTestSyntaxError("value1;version:");
        genericTestSyntaxError("value1;version:=");
        genericTestSyntaxError("value1;=1");
        genericTestSyntaxError("value1;att=''value");
    }

    private void genericTestSyntaxError(String value) {
        try {
            new ManifestHeaderValue(value);
            fail("Syntax error not detected");
        } catch (ParseException e) {
            // error detected: OK
            ManifestHeaderValue.writeParseException(System.out, value, e);
        }
    }

    public void testSpaceInValue() throws Exception {
        ManifestHeaderValue value = new ManifestHeaderValue("glassfish javax.servlet.3.1.0.b33");
        assertEquals("glassfish javax.servlet.3.1.0.b33", value.getSingleValue());
    }
}
