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
package org.apache.ivy.ant;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;

public class IvyDependencyConf {

    private List/* <IvyDependencyConfMapped> */mappeds = new ArrayList();

    public static class IvyDependencyConfMapped {
        private String name;

        public void setName(String name) {
            this.name = name;
        }

    }

    private String mapped;

    public void setMapped(String mapped) {
        this.mapped = mapped;
    }

    public IvyDependencyConfMapped createMapped() {
        IvyDependencyConfMapped m = new IvyDependencyConfMapped();
        mappeds.add(m);
        return m;
    }

    void addConf(DefaultDependencyDescriptor dd, String masterConf) {
        if (mapped != null) {
            String[] mappeds = mapped.split(",");
            for (int i = 0; i < mappeds.length; i++) {
                dd.addDependencyConfiguration(masterConf, mappeds[i].trim());
            }
        }
        Iterator itMappeds = mappeds.iterator();
        while (itMappeds.hasNext()) {
            IvyDependencyConfMapped m = (IvyDependencyConfMapped) itMappeds.next();
            dd.addDependencyConfiguration(masterConf, m.name);
        }
    }
}
