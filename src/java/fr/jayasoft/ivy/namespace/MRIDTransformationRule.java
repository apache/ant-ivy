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
        String[] types = new String[] {"o", "m", "r"};
        Matcher[] _matchers = new Matcher[3];
        
        public boolean match(MRIDRule src, ModuleRevisionId mrid) {
            _matchers[0] = Pattern.compile(getPattern(src.getOrg())).matcher(mrid.getOrganisation());
            if (!_matchers[0].matches()) {
                return false;
            }
            _matchers[1] = Pattern.compile(getPattern(src.getModule())).matcher(mrid.getName());
            if (!_matchers[1].matches()) {
                return false;
            }
            _matchers[2] = Pattern.compile(getPattern(src.getRev())).matcher(mrid.getRevision());
            if (!_matchers[2].matches()) {
                return false;
            }
            
            return true;
        }
        public ModuleRevisionId apply(MRIDRule dest, ModuleRevisionId mrid) {
            String org = applyRules(dest.getOrg());
            String mod = applyRules(dest.getModule());
            String rev = applyRules(dest.getRev());
            
            return ModuleRevisionId.newInstance(org, mod, rev);
        }
        private String applyRules(String str) {
            for (int i = 0; i < types.length; i++) {
                str = applyTypeRule(str, types[i], _matchers[i]);
            }
            return str;
        }
        
        private String applyTypeRule(String rule, String type, Matcher m) {
            String res = rule == null ? "$"+type+"0" : rule;
            for (int i = 0; i < types.length; i++) {
                if (types[i].equals(type)) {
                    res = res.replaceAll("[^\\\\]\\$"+type, "\\$");
                    res = res.replaceAll("^\\$"+type, "\\$");
                } else {
                    res = res.replaceAll("[^\\\\]\\$"+types[i], "\\\\\\$"+types[i]);
                    res = res.replaceAll("^\\$"+types[i], "\\\\\\$"+types[i]);
                }
            }
            
            StringBuffer sb = new StringBuffer();
            m.appendReplacement(sb, res);
            return sb.toString();            
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

}
