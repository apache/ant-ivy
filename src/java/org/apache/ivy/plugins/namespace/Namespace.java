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

    private List _rules = new ArrayList();

    private String _name;

    private boolean _chainRules = false;

    private NamespaceTransformer _fromSystemTransformer = new NamespaceTransformer() {
        public ModuleRevisionId transform(ModuleRevisionId mrid) {
            for (Iterator iter = _rules.iterator(); iter.hasNext();) {
                NamespaceRule rule = (NamespaceRule) iter.next();
                ModuleRevisionId nmrid = rule.getFromSystem().transform(mrid);
                if (_chainRules) {
                    mrid = nmrid;
                } else if (!nmrid.equals(mrid)) {
                    return nmrid;
                }
            }
            return mrid;
        }

        public boolean isIdentity() {
            return _rules.isEmpty();
        }
    };

    private NamespaceTransformer _toSystemTransformer = new NamespaceTransformer() {
        public ModuleRevisionId transform(ModuleRevisionId mrid) {
            for (Iterator iter = _rules.iterator(); iter.hasNext();) {
                NamespaceRule rule = (NamespaceRule) iter.next();
                ModuleRevisionId nmrid = rule.getToSystem().transform(mrid);
                if (_chainRules) {
                    mrid = nmrid;
                } else if (!nmrid.equals(mrid)) {
                    return nmrid;
                }
            }
            return mrid;
        }

        public boolean isIdentity() {
            return _rules.isEmpty();
        }
    };

    public void addRule(NamespaceRule rule) {
        _rules.add(rule);
    }

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        _name = name;
    }

    public NamespaceTransformer getFromSystemTransformer() {
        return _fromSystemTransformer;
    }

    public NamespaceTransformer getToSystemTransformer() {
        return _toSystemTransformer;
    }

    public boolean isChainrules() {
        return _chainRules;
    }

    public void setChainrules(boolean chainRules) {
        _chainRules = chainRules;
    }
}
