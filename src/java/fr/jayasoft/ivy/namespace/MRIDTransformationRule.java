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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.jayasoft.ivy.ModuleRevisionId;
import fr.jayasoft.ivy.util.Message;

public class MRIDTransformationRule implements NamespaceTransformer {
    private static class MridRuleMatcher {
        private static final String[] TYPES = new String[] {"o", "m", "b", "r"};
        private Matcher[] _matchers = new Matcher[4];
        
        public boolean match(MRIDRule src, ModuleRevisionId mrid) {
            _matchers[0] = Pattern.compile(getPattern(src.getOrg())).matcher(mrid.getOrganisation());
            if (!_matchers[0].matches()) {
                return false;
            }
            _matchers[1] = Pattern.compile(getPattern(src.getModule())).matcher(mrid.getName());
            if (!_matchers[1].matches()) {
                return false;
            }
            if (mrid.getBranch() == null) {
            	_matchers[2] = null;
            } else {
            	_matchers[2] =  Pattern.compile(getPattern(src.getBranch())).matcher(mrid.getBranch());
            	if (!_matchers[2].matches()) {
            		return false;
            	}
            }
            _matchers[3] = Pattern.compile(getPattern(src.getRev())).matcher(mrid.getRevision());
            if (!_matchers[3].matches()) {
                return false;
            }
            
            return true;
        }
        public ModuleRevisionId apply(MRIDRule dest, ModuleRevisionId mrid) {
            String org = applyRules(dest.getOrg(), "o");
            String mod = applyRules(dest.getModule(), "m");
            String branch = applyRules(dest.getBranch(), "b");
            String rev = applyRules(dest.getRev(), "r");
            
            return ModuleRevisionId.newInstance(org, mod, branch, rev, mrid.getExtraAttributes());
        }
        private String applyRules(String str, String type) {
            for (int i = 0; i < TYPES.length; i++) {
                str = applyTypeRule(str, TYPES[i], type, _matchers[i]);
            }
            return str;
        }
        
        private String applyTypeRule(String rule, String type, String ruleType, Matcher m) {
        	if (m == null) {
        		return rule;
        	}
            String res = rule == null ? "$"+ruleType+"0" : rule;
            for (int i = 0; i < TYPES.length; i++) {
                if (TYPES[i].equals(type)) {
                    res = res.replaceAll("([^\\\\])\\$"+type, "$1\\$");
                    res = res.replaceAll("^\\$"+type, "\\$");
                } else {
                    res = res.replaceAll("([^\\\\])\\$"+TYPES[i], "$1\\\\\\$"+TYPES[i]);
                    res = res.replaceAll("^\\$"+TYPES[i], "\\\\\\$"+TYPES[i]);
                }
            }
            
            StringBuffer sb = new StringBuffer();
            m.reset();
            m.find();
            m.appendReplacement(sb, res);

            String str = sb.toString();
			// null rule not replaced, let it be null 
            if (rule == null && ("$"+ruleType+"0").equals(str)) {
            	return null;
            }
            
            return str;
        }
        
        private String getPattern(String p) {
            return p == null ? ".*" : p;
        }
    }
    private List _src = new ArrayList();
    private MRIDRule _dest;

    public void addSrc(MRIDRule src) {
        _src.add(src);
    }

    public void addDest(MRIDRule dest) {
        if (_dest != null) {
            throw new IllegalArgumentException("only one dest is allowed per mapping");
        }
        _dest = dest;
    }

    public ModuleRevisionId transform(ModuleRevisionId mrid) {
        MridRuleMatcher matcher = new MridRuleMatcher();
        for (Iterator iter = _src.iterator(); iter.hasNext();) {
            MRIDRule rule = (MRIDRule)iter.next();
            if (matcher.match(rule, mrid)) {
                ModuleRevisionId destMrid = matcher.apply(_dest, mrid);
                Message.debug("found matching namespace rule: "+rule+". Applied "+_dest+" on "+mrid+". Transformed to "+destMrid);
                return destMrid;
            }
        }
        return mrid;
    }

    public boolean isIdentity() {
        return false;
    }

}
