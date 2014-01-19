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
package org.apache.ivy.plugins.namespace;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.ivy.core.module.id.ModuleRevisionId;

public class Namespace {
    public static final Namespace SYSTEM_NAMESPACE;
    static {
        SYSTEM_NAMESPACE = new Namespace();
    }

    private List/* <NamespaceRule> */rules = new ArrayList();

    private String name;

    private boolean chainRules = false;

    private NamespaceTransformer fromSystemTransformer = new NamespaceTransformer() {
        public ModuleRevisionId transform(ModuleRevisionId mrid) {
            if (mrid == null) {
                return null;
            }
            for (Iterator iter = rules.iterator(); iter.hasNext();) {
                NamespaceRule rule = (NamespaceRule) iter.next();
                ModuleRevisionId nmrid = rule.getFromSystem().transform(mrid);
                if (chainRules) {
                    mrid = nmrid;
                } else if (!nmrid.equals(mrid)) {
                    return nmrid;
                }
            }
            return mrid;
        }

        public boolean isIdentity() {
            return rules.isEmpty();
        }
    };

    private NamespaceTransformer toSystemTransformer = new NamespaceTransformer() {
        public ModuleRevisionId transform(ModuleRevisionId mrid) {
            if (mrid == null) {
                return null;
            }
            for (Iterator iter = rules.iterator(); iter.hasNext();) {
                NamespaceRule rule = (NamespaceRule) iter.next();
                ModuleRevisionId nmrid = rule.getToSystem().transform(mrid);
                if (chainRules) {
                    mrid = nmrid;
                } else if (!nmrid.equals(mrid)) {
                    return nmrid;
                }
            }
            return mrid;
        }

        public boolean isIdentity() {
            return rules.isEmpty();
        }
    };

    public void addRule(NamespaceRule rule) {
        rules.add(rule);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public NamespaceTransformer getFromSystemTransformer() {
        return fromSystemTransformer;
    }

    public NamespaceTransformer getToSystemTransformer() {
        return toSystemTransformer;
    }

    public boolean isChainrules() {
        return chainRules;
    }

    public void setChainrules(boolean chainRules) {
        this.chainRules = chainRules;
    }
}
