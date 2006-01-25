/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.namespace;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import fr.jayasoft.ivy.ModuleRevisionId;

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
                NamespaceRule rule = (NamespaceRule)iter.next();
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
                NamespaceRule rule = (NamespaceRule)iter.next();
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
