/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.jayasoft.ivy.Artifact;
import fr.jayasoft.ivy.DefaultArtifact;
import fr.jayasoft.ivy.ModuleRevisionId;

/**
 * @author x.hanin
 * @author Maarten Coene (for the optional part management)
 */
public class IvyPatternHelper {
    public static final String CONF_KEY = "conf";
    public static final String TYPE_KEY = "type";
    public static final String EXT_KEY = "ext";
    public static final String ARTIFACT_KEY = "artifact";
    public static final String REVISION_KEY = "revision";
    public static final String MODULE_KEY = "module";
    public static final String ORGANISATION_KEY = "organisation";
    public static final String ORGANISATION_KEY2 = "organization";
    
    private static final Pattern PARAM_PATTERN = Pattern.compile("\\@\\{(.*?)\\}");
    private static final Pattern VAR_PATTERN = Pattern.compile("\\$\\{(.*?)\\}");
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\[(.*?)\\]");
    
    public static String substitute(String pattern, ModuleRevisionId moduleRevision) {
        return substitute(pattern, 
                moduleRevision.getOrganisation(),
                moduleRevision.getName(),
                moduleRevision.getRevision(),
                "ivy",
                "ivy",
                "xml",
                null, 
                moduleRevision.getAttributes());
    }
    public static String substitute(String pattern, ModuleRevisionId moduleRevision, String artifact, String type, String ext) {
        return substitute(pattern, 
                moduleRevision,
                new DefaultArtifact(moduleRevision, null, artifact, type, ext),
                null);
    }
    public static String substitute(String pattern, Artifact artifact) {
        return substitute(pattern, artifact, null);
    }
    public static String substitute(String pattern, Artifact artifact, String conf) {
        return substitute(pattern, artifact.getModuleRevisionId(), artifact, null);
    }
    public static String substitute(String pattern, ModuleRevisionId mrid, Artifact artifact) {
        return substitute(pattern, mrid, artifact, null);
    }
    public static String substitute(String pattern, ModuleRevisionId mrid, Artifact artifact, String conf) {
        Map attributes = new HashMap();
        attributes.putAll(mrid.getAttributes());
        attributes.putAll(artifact.getAttributes());
        return substitute(pattern, 
                mrid.getOrganisation(),
                mrid.getName(),
                mrid.getRevision(),
                artifact.getName(),
                artifact.getType(),
                artifact.getExt(),
                conf,
                attributes);
    }

    
    public static String substitute(String pattern, String org, String module, String revision, String artifact, String type, String ext) {
        return substitute(pattern, org, module, revision, artifact, type, ext, null);
    }
    
    public static String substitute(String pattern, String org, String module, String revision, String artifact, String type, String ext, String conf) {
        return substitute(pattern, org, module, revision, artifact, type, ext, conf, null);
    }
    
    public static String substitute(String pattern, String org, String module, String revision, String artifact, String type, String ext, String conf, Map extraAttributes) {
        Map tokens = new HashMap(extraAttributes == null ? Collections.EMPTY_MAP : extraAttributes);        
        tokens.put(ORGANISATION_KEY, org==null?"":org);
        tokens.put(ORGANISATION_KEY2, org==null?"":org);
        tokens.put(MODULE_KEY, module==null?"":module);
        tokens.put(REVISION_KEY, revision==null?"":revision);
        tokens.put(ARTIFACT_KEY, artifact==null?module:artifact);
        tokens.put(TYPE_KEY, type==null?"jar":type);
        tokens.put(EXT_KEY, ext==null?"jar":ext);
        tokens.put(CONF_KEY, conf==null?"default":conf);
        return substituteTokens(pattern, tokens);
    }
    
    public static String substitute(String pattern, Map variables, Map tokens) {
        return substituteTokens(substituteVariables(pattern, variables), tokens);
    }
    
    public static String substituteVariables(String pattern, Map variables) {
        return substituteVariables(pattern, variables, new Stack());
    }
    
    private static String substituteVariables(String pattern, Map variables, Stack substituting) {
        // if you supply null, null is what you get
        if (pattern == null) {
            return null;
        }
        
        Matcher m = VAR_PATTERN.matcher(pattern);
        
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String var = m.group(1);
            String val = (String)variables.get(var);
            if (val != null) {
                int index;
                if ((index = substituting.indexOf(var)) != -1) {
                    List cycle = new ArrayList(substituting.subList(index, substituting.size()));
                    cycle.add(var);
                    throw new IllegalArgumentException("cyclic variable definition: cycle = "+cycle);
                }
                substituting.push(var);
                val = substituteVariables(val, variables, substituting);
                substituting.pop();
            } else {
                val = m.group();
            }
            m.appendReplacement(sb, val.replaceAll("\\\\", "\\\\\\\\").replaceAll("\\$", "\\\\\\$"));
        }
        m.appendTail(sb);

        return sb.toString();
    }
    
    public static String substituteTokens(String pattern, Map tokens) {
        StringBuffer buffer = new StringBuffer();
        
        char[] chars = pattern.toCharArray();
        
        StringBuffer optionalPart = null;
        StringBuffer tokenBuffer = null;
        boolean insideOptionalPart = false;
        boolean insideToken = false;
        boolean tokenHadValue = false;
        
        for (int i = 0; i < chars.length; i++) {
            switch (chars[i]) {
            case '(':
                if (insideOptionalPart) {
                    throw new IllegalArgumentException("invalid start of optional part at position " + i + " in pattern " + pattern);
                }

                optionalPart = new StringBuffer();
                insideOptionalPart = true;
                tokenHadValue = false;
                break;

            case ')':
                if (!insideOptionalPart || insideToken) {
                    throw new IllegalArgumentException("invalid end of optional part at position " + i + " in pattern " + pattern);
                }

                if (tokenHadValue) {
                    buffer.append(optionalPart.toString());
                }

                insideOptionalPart = false;
                break;
                
            case '[':
                if (insideToken) {
                    throw new IllegalArgumentException("invalid start of token at position " + i + " in pattern " + pattern);
                }
                
                tokenBuffer = new StringBuffer();               
                insideToken = true;
                break;
                
            case ']':
                if (!insideToken) {
                    throw new IllegalArgumentException("invalid end of token at position " + i + " in pattern " + pattern);
                }
                
                String token = tokenBuffer.toString();
                String value = (String) tokens.get(token);
                
                if (insideOptionalPart) {
                    tokenHadValue = (value != null) && (value.length() > 0);
                    optionalPart.append(value);
                } else {
                    if (value == null) { // the token wasn't set, it's kept as is
                        value = "["+token+"]";
                    }
                    buffer.append(value);
                }
                
                insideToken = false;
                break;
                
            default:
                if (insideToken) {
                    tokenBuffer.append(chars[i]);
                } else if (insideOptionalPart) {
                    optionalPart.append(chars[i]);
                } else {
                    buffer.append(chars[i]);
                }
            
                break;
            }
        }
        
        if (insideToken) {
            throw new IllegalArgumentException("last token hasn't been closed in pattern " + pattern);
        }
        
        if (insideOptionalPart) {
            throw new IllegalArgumentException("optional part hasn't been closed in pattern " + pattern);
        }
        
        return buffer.toString();
    }
    
    public static String substituteVariable(String pattern, String variable, String value) {
        StringBuffer buf = new StringBuffer(pattern);
        substituteVariable(buf, variable, value);
        return buf.toString();
    }
    
    public static void substituteVariable(StringBuffer buf, String variable, String value) {
        String from = "${"+variable+"}";
        int fromLength = from.length();
        for (int index = buf.indexOf(from); index != -1; index = buf.indexOf(from, index)) {
            buf.replace(index, index + fromLength, value);
        }
    }
    
    public static String substituteToken(String pattern, String token, String value) {
        StringBuffer buf = new StringBuffer(pattern);
        substituteToken(buf, token, value);
        return buf.toString();
    }
    
    public static void substituteToken(StringBuffer buf, String token, String value) {
        String from = getTokenString(token);
        int fromLength = from.length();
        for (int index = buf.indexOf(from); index != -1; index = buf.indexOf(from, index)) {
            buf.replace(index, index + fromLength, value);
        }
    }
    public static String getTokenString(String token) {
        return "["+token+"]";
    }
    
    public static String substituteParams(String pattern, Map params) {
        return substituteParams(pattern, params, new Stack());
    }
    
    private static String substituteParams(String pattern, Map params, Stack substituting) {
        //TODO : refactor this with substituteVariables
        // if you supply null, null is what you get
        if (pattern == null) {
            return null;
        }
        
        Matcher m = PARAM_PATTERN.matcher(pattern);
        
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String var = m.group(1);
            String val = (String)params.get(var);
            if (val != null) {
                int index;
                if ((index = substituting.indexOf(var)) != -1) {
                    List cycle = new ArrayList(substituting.subList(index, substituting.size()));
                    cycle.add(var);
                    throw new IllegalArgumentException("cyclic param definition: cycle = "+cycle);
                }
                substituting.push(var);
                val = substituteVariables(val, params, substituting);
                substituting.pop();
            } else {
                val = m.group();
            }
            m.appendReplacement(sb, val.replaceAll("\\\\", "\\\\\\\\").replaceAll("\\@", "\\\\\\@"));
        }
        m.appendTail(sb);

        return sb.toString();
    }
    
    public static void main(String[] args) {
        String pattern = "[organisation]/[module]/build/archives/[type]s/[artifact]-[revision].[ext]";
        System.out.println("pattern= "+pattern);
        System.out.println("resolved= "+substitute(pattern, "jayasoft", "Test", "1.0", "test", "jar", "jar"));
        
        Map variables = new HashMap();
        variables.put("test", "mytest");
        variables.put("test2", "${test}2");
        pattern = "${test} ${test2} ${nothing}";
        System.out.println("pattern= "+pattern);
        System.out.println("resolved= "+substituteVariables(pattern, variables));
    }
}
