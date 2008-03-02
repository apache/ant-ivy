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


public final class FilterProvider {
    
    public static IFilter getFilter() {
        try {
            Class clazz = Class.forName("filter.ccimpl.CCFilter");
            return (IFilter) clazz.newInstance();
        } catch (Exception e) {
            try {
                Class clazz = Class.forName("filter.hmimpl.HMFilter");
                return (IFilter) clazz.newInstance();
            } catch (Exception e1) {
                System.err.println("No filter implementation found in classpath !");
            }
            return null;
        }
    }
    
    private FilterProvider() {
    }
}
