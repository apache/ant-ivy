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
package filter;

import junit.framework.TestCase;

public abstract class AbstractTestFilter extends TestCase {
    
    public void testFilterNull() {
        Exception err = null;
        try {
            getIFilter().filter(null, null);
        } catch (NullPointerException npe) {
            err = npe;
        }
        assertNull(err);
    }

    /**
     * @return
     */
    public abstract IFilter getIFilter();
    
    public void testFilterNullValues() {
        Exception err = null;
        try {
            getIFilter().filter(null, "test");
        } catch (NullPointerException npe) {
            err = npe;
        }
        assertNull(err);
    }
    
    public void testFilterNullPrefix() {
        Exception err = null;
        try {
            getIFilter().filter(new String[]{"test"}, null);
        } catch (NullPointerException npe) {
            err = npe;
        }
        assertNull(err);
    } 
    
    public void testFilter() {
        String[] result = getIFilter().filter(
            new String[]{"test", "nogood", "mustbe filtered"}, "t");
        assertNotNull(result);
        assertEquals(result.length, 1);
    }    
    
    public void testFilterWithNullValues() {
        String[] result = getIFilter().filter(new String[]{"test", null, "mustbe filtered"}, "t");
        assertNotNull(result);
        assertEquals(result.length, 1);
    }
}
