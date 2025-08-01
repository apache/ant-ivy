/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivy.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ivy.core.cache.ArtifactOrigin;
import org.apache.ivy.core.cache.RepositoryCacheManager;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.settings.IvyVariableContainer;
import org.apache.ivy.core.settings.IvyVariableContainerImpl;
import org.apache.ivy.util.Message;

import static org.apache.ivy.util.StringUtils.isNullOrEmpty;

/**
 */
public final class IvyPatternHelper {

    private IvyPatternHelper() {
        // Helper class
    }

    public static final String CONF_KEY = "conf";

    public static final String TYPE_KEY = "type";

    public static final String EXT_KEY = "ext";

    public static final String ARTIFACT_KEY = "artifact";

    public static final String BRANCH_KEY = "branch";

    public static final String REVISION_KEY = "revision";

    public static final String MODULE_KEY = "module";

    public static final String ORGANISATION_KEY = "organisation";

    public static final String ORGANISATION_KEY2 = "organization";

    public static final String ORGANISATION_PATH_KEY = "orgPath";

    public static final String ORIGINAL_ARTIFACTNAME_KEY = "originalname";

    private static final Pattern PARAM_PATTERN = Pattern.compile("\\@\\{(.*?)\\}");

    private static final Pattern VAR_PATTERN = Pattern.compile("\\$\\{(.*?)\\}");

    public static String substitute(String pattern, ModuleRevisionId moduleRevision) {
        return substitute(pattern, moduleRevision.getOrganisation(), moduleRevision.getName(),
            moduleRevision.getBranch(), moduleRevision.getRevision(), "ivy", "ivy", "xml", null,
            null, moduleRevision.getQualifiedExtraAttributes(), null);
    }

    public static String substitute(String pattern, ModuleRevisionId moduleRevision,
            String artifact, String type, String ext) {
        return substitute(pattern, moduleRevision, new DefaultArtifact(moduleRevision, null,
                artifact, type, ext));
    }

    public static String substitute(String pattern, Artifact artifact) {
        return substitute(pattern, artifact, (String) null);
    }

    public static String substitute(String pattern, Artifact artifact, ArtifactOrigin origin) {
        return substitute(pattern, artifact.getModuleRevisionId(), artifact, null, origin);
    }

    public static String substitute(String pattern, Artifact artifact, String conf) {
        return substitute(pattern, artifact.getModuleRevisionId(), artifact, conf, null);
    }

    public static String substitute(String pattern, ModuleRevisionId mrid, Artifact artifact) {
        return substitute(pattern, mrid, artifact, null, null);
    }

    public static String substitute(String pattern, ModuleRevisionId mrid, Artifact artifact,
            String conf, ArtifactOrigin origin) {
        return substitute(pattern, mrid.getOrganisation(), mrid.getName(), mrid.getBranch(),
            mrid.getRevision(), artifact.getName(), artifact.getType(), artifact.getExt(), conf,
            origin, mrid.getQualifiedExtraAttributes(), artifact.getQualifiedExtraAttributes());
    }

    public static String substitute(String pattern, String org, String module, String revision,
            String artifact, String type, String ext) {
        return substitute(pattern, org, module, null, revision, artifact, type, ext,
                null, null, null, null);
    }

    // CheckStyle:ParameterNumber OFF
    public static String substitute(String pattern, String org, String module, String revision,
            String artifact, String type, String ext, String conf) {
        return substitute(pattern, org, module, null, revision, artifact, type, ext, conf,
                null, null, null);
    }

    public static String substitute(String pattern, String org, String module, String revision,
            String artifact, String type, String ext, String conf, Map<String, String> extraModuleAttributes,
            Map<String, String> extraArtifactAttributes) {
        return substitute(pattern, org, module, null, revision, artifact, type, ext, conf,
                null, extraModuleAttributes, extraArtifactAttributes);
    }

    public static String substitute(String pattern, String org, String module, String branch,
            String revision, String artifact, String type, String ext, String conf,
            ArtifactOrigin origin, Map<String, String> extraModuleAttributes, Map<String, String> extraArtifactAttributes) {
        Map<String, Object> tokens = new HashMap<>();
        if (extraModuleAttributes != null) {
            for (Map.Entry<String, String> entry : extraModuleAttributes.entrySet()) {
                String token = entry.getKey();
                if (token.indexOf(':') > 0) {
                    token = token.substring(token.indexOf(':') + 1);
                }
                tokens.put(token, new Validated(token, entry.getValue()));
            }
        }
        if (extraArtifactAttributes != null) {
            for (Map.Entry<String, String> entry : extraArtifactAttributes.entrySet()) {
                String token = entry.getKey();
                if (token.indexOf(':') > 0) {
                    token = token.substring(token.indexOf(':') + 1);
                }
                tokens.put(token, new Validated(token, entry.getValue()));
            }
        }
        tokens.put(ORGANISATION_KEY, org == null ? "" : new Validated(ORGANISATION_KEY, org));
        tokens.put(ORGANISATION_KEY2, org == null ? "" : new Validated(ORGANISATION_KEY2, org));
        tokens.put(ORGANISATION_PATH_KEY, org == null ? "" : org.replace('.', '/'));
        tokens.put(MODULE_KEY, module == null ? "" : new Validated(MODULE_KEY, module));
        tokens.put(BRANCH_KEY, branch == null ? "" : new Validated(BRANCH_KEY, branch));
        tokens.put(REVISION_KEY, revision == null ? "" : new Validated(REVISION_KEY, revision));
        tokens.put(ARTIFACT_KEY, new Validated(ARTIFACT_KEY, artifact == null ? module : artifact));
        tokens.put(TYPE_KEY, type == null ? "jar" : new Validated(TYPE_KEY, type));
        tokens.put(EXT_KEY, ext == null ? "jar" : new Validated(EXT_KEY, ext));
        tokens.put(CONF_KEY, conf == null ? "default" : new Validated(CONF_KEY, conf));
        if (origin == null) {
            tokens.put(ORIGINAL_ARTIFACTNAME_KEY, new OriginalArtifactNameValue(org, module,
                    branch, revision, artifact, type, ext, extraModuleAttributes,
                    extraArtifactAttributes));
        } else {
            tokens.put(ORIGINAL_ARTIFACTNAME_KEY, new OriginalArtifactNameValue(origin));
        }

        return substituteTokens(pattern, tokens, false, true);
    }

    // CheckStyle:ParameterNumber ON

    public static String substituteVariables(String pattern, Map<String, String> variables) {
        return substituteVariables(pattern, new IvyVariableContainerImpl(variables), new Stack<String>());
    }

    public static String substituteVariables(String pattern, IvyVariableContainer variables) {
        return substituteVariables(pattern, variables, new Stack<String>());
    }

    private static String substituteVariables(String pattern, IvyVariableContainer variables,
            Stack<String> substituting) {
        // if you supply null, null is what you get
        if (pattern == null) {
            return null;
        }

        Matcher m = VAR_PATTERN.matcher(pattern);

        boolean useVariables = false;
        StringBuffer sb = null;
        while (m.find()) {
            if (!useVariables) {
                useVariables = true;
                sb = new StringBuffer();
            }
            String var = m.group(1);
            String val = variables.getVariable(var);
            if (val != null) {
                int index = substituting.indexOf(var);
                if (index != -1) {
                    List<String> cycle = new ArrayList<>(substituting.subList(index, substituting.size()));
                    cycle.add(var);
                    throw new IllegalArgumentException("cyclic variable definition: cycle = "
                            + cycle);
                }
                substituting.push(var);
                val = substituteVariables(val, variables, substituting);
                substituting.pop();
            } else {
                val = m.group();
            }
            m.appendReplacement(sb, val.replaceAll("\\\\", "\\\\\\\\").replaceAll("\\$", "\\\\\\$"));
        }
        if (useVariables) {
            m.appendTail(sb);
            return sb.toString();
        } else {
            return pattern;
        }
    }

    // This is a cludge to reconcile different values passed to the method
    public static String substituteTokens(String pattern, Map<String, String> tokens) {
        Map<String, Object> tokensCopy = new HashMap<>();
        tokensCopy.putAll(tokens);
        return substituteTokens(pattern, tokensCopy, true, true);
    }

    private static String substituteTokens(String pattern, Map<String, Object> tokens, boolean external, boolean checkPathTraversal) {
        Map<String, Object> tokensCopy = external ? tokens : new HashMap<>(tokens);
        if (tokensCopy.containsKey(ORGANISATION_KEY) && !tokensCopy.containsKey(ORGANISATION_KEY2)) {
            tokensCopy.put(ORGANISATION_KEY2, tokensCopy.get(ORGANISATION_KEY));
        }
        if (tokensCopy.containsKey(ORGANISATION_KEY)
                && !tokensCopy.containsKey(ORGANISATION_PATH_KEY)) {
            String org = (String) tokensCopy.get(ORGANISATION_KEY);
            tokensCopy.put(ORGANISATION_PATH_KEY, org == null ? "" : org.replace('.', '/'));
        }

        StringBuilder buffer = new StringBuilder();

        StringBuffer optionalPart = null;
        StringBuffer tokenBuffer = null;
        boolean insideOptionalPart = false;
        boolean insideToken = false;
        boolean tokenSeen = false;
        boolean tokenHadValue = false;

        for (char ch : pattern.toCharArray()) {
            int i = pattern.indexOf(ch);
            switch (ch) {
                case '(':
                    if (insideOptionalPart) {
                        throw new IllegalArgumentException(
                                "invalid start of optional part at position " + i + " in pattern "
                                        + pattern);
                    }

                    optionalPart = new StringBuffer();
                    insideOptionalPart = true;
                    tokenSeen = false;
                    tokenHadValue = false;
                    break;
                case ')':
                    if (!insideOptionalPart || insideToken) {
                        throw new IllegalArgumentException(
                                "invalid end of optional part at position " + i + " in pattern "
                                        + pattern);
                    }

                    if (tokenHadValue) {
                        buffer.append(optionalPart.toString());
                    } else if (!tokenSeen) {
                        buffer.append('(').append(optionalPart.toString()).append(')');
                    }
                    insideOptionalPart = false;
                    break;
                case '[':
                    if (insideToken) {
                        throw new IllegalArgumentException("invalid start of token at position "
                                + i + " in pattern " + pattern);
                    }

                    tokenBuffer = new StringBuffer();
                    insideToken = true;
                    break;
                case ']':
                    if (!insideToken) {
                        throw new IllegalArgumentException("invalid end of token at position " + i
                                + " in pattern " + pattern);
                    }

                    String token = tokenBuffer.toString();
                    Object tokenValue = tokensCopy.get(token);
                    String value = (tokenValue == null) ? null : tokenValue.toString();
                    if (insideOptionalPart) {
                        tokenHadValue = !isNullOrEmpty(value);
                        optionalPart.append(value);
                    } else {
                        if (value == null) { // the token wasn't set, it's kept as is
                            value = "[" + token + "]";
                        }
                        buffer.append(value);
                    }
                    insideToken = false;
                    tokenSeen = true;
                    break;
                default:
                    if (insideToken) {
                        tokenBuffer.append(ch);
                    } else if (insideOptionalPart) {
                        optionalPart.append(ch);
                    } else {
                        buffer.append(ch);
                    }
                    break;
            }
        }

        if (insideToken) {
            throw new IllegalArgumentException("last token hasn't been closed in pattern "
                    + pattern);
        }

        if (insideOptionalPart) {
            throw new IllegalArgumentException("optional part hasn't been closed in pattern "
                    + pattern);
        }

        String afterTokenSubstitution = buffer.toString();
        if (checkPathTraversal) {
            checkAgainstPathTraversal(pattern, afterTokenSubstitution);
        }

        return afterTokenSubstitution;
    }

    public static String substituteVariable(String pattern, String variable, String value) {
        StringBuffer buf = new StringBuffer(pattern);
        substituteVariable(buf, variable, value);
        return buf.toString();
    }

    public static void substituteVariable(StringBuffer buf, String variable, String value) {
        String from = "${" + variable + "}";
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
        return "[" + token + "]";
    }

    public static String substituteParams(String pattern, Map<String, String> params) {
        return substituteParams(pattern, new IvyVariableContainerImpl(params), new Stack<String>());
    }

    private static String substituteParams(String pattern, IvyVariableContainer params,
            Stack<String> substituting) {
        // TODO : refactor this with substituteVariables
        // if you supply null, null is what you get
        if (pattern == null) {
            return null;
        }

        Matcher m = PARAM_PATTERN.matcher(pattern);

        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String var = m.group(1);
            String val = params.getVariable(var);
            if (val != null) {
                int index = substituting.indexOf(var);
                if (index != -1) {
                    List<String> cycle = new ArrayList<>(substituting.subList(index, substituting.size()));
                    cycle.add(var);
                    throw new IllegalArgumentException("cyclic param definition: cycle = " + cycle);
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

    /**
     * This class returns the original name of the artifact 'on demand'. This is done to avoid
     * having to read the cached datafile containing the original location of the artifact if we
     * don't need it.
     */
    private static class OriginalArtifactNameValue {
        // module properties
        private String org;

        private String moduleName;

        private String branch;

        private String revision;

        private Map<String, String> extraModuleAttributes;

        // artifact properties
        private String artifactName;

        private String artifactType;

        private String artifactExt;

        private Map<String, String> extraArtifactAttributes;

        // cached origin;
        private ArtifactOrigin origin;

        public OriginalArtifactNameValue(String org, String moduleName, String branch,
                String revision, String artifactName, String artifactType, String artifactExt,
                Map<String, String> extraModuleAttributes, Map<String, String> extraArtifactAttributes) {
            this.org = org;
            this.moduleName = moduleName;
            this.branch = branch;
            this.revision = revision;
            this.artifactName = artifactName;
            this.artifactType = artifactType;
            this.artifactExt = artifactExt;
            this.extraModuleAttributes = extraModuleAttributes;
            this.extraArtifactAttributes = extraArtifactAttributes;
        }

        /**
         * @param origin ArtifactOrigin
         */
        public OriginalArtifactNameValue(ArtifactOrigin origin) {
            this.origin = origin;
        }

        // Called by substituteTokens only if the original artifact name is needed
        public String toString() {
            if (origin == null) {
                ModuleRevisionId revId = ModuleRevisionId.newInstance(org, moduleName, branch,
                    revision, extraModuleAttributes);
                Artifact artifact = new DefaultArtifact(revId, null, artifactName, artifactType,
                        artifactExt, extraArtifactAttributes);

                // TODO cache: see how we could know which actual cache manager to use, since this
                // will fail when using a resolver in a chain with a specific cache manager
                RepositoryCacheManager cacheManager = IvyContext.getContext().getSettings()
                        .getResolver(revId).getRepositoryCacheManager();

                origin = cacheManager.getSavedArtifactOrigin(artifact);

                if (ArtifactOrigin.isUnknown(origin)) {
                    Message.debug("no artifact origin found for " + artifact + " in "
                            + cacheManager);
                    return null;
                }
            }

            if (ArtifactOrigin.isUnknown(origin)) {
                return null;
            }

            // we assume that the original filename is the last part of the original file location
            String location = origin.getLocation();
            int lastPathIndex = location.lastIndexOf('/');
            if (lastPathIndex == -1) {
                lastPathIndex = location.lastIndexOf('\\');
            }
            int lastColonIndex = location.lastIndexOf('.');

            return location.substring(lastPathIndex + 1, lastColonIndex);
        }
    }

    public static String getTokenRoot(String pattern) {
        String token = getFirstToken(pattern);
        if (token == null) {
            // no token found, return the whole pattern
            return pattern;
        }

        int index = pattern.indexOf('[' + token + ']');
        if (index == -1) {
            // should not happen, but just in case
            return pattern;
        }

        // to tackle optional token parts, we follow this strategy:
        // 1. substitute the token with a dummy value (e.g. "xxx")
        // 2. substitute the token with an empty value
        // 3. compare the two results and find the first character that is different
        // -> this character is the first character that is not part of the root
        String sub1 = substituteTokens(pattern, Collections.singletonMap(token, "xxx"), false, false);
        String sub2 = substituteTokens(pattern, new HashMap<>(), true, false);

        // due to the optional part, the second substitution could result in a shorter string
        index = Math.min(index, sub2.length());

        // now we compare the two strings character by character until we find a difference
        for (int i = 0; i < index; i++) {
            if (sub1.charAt(i) != sub2.charAt(i)) {
                // we found the first character that is different, so we can return the root
                index = i;
                break;
            }
        }

        // now let's find the last path separator before that index
        // this tackles cases like "lib/config-[conf]/[module]" where we want to return "lib/" as root
        for (int i = index - 1; i >= 0; i--) {
            char c = sub1.charAt(i);
            if (c == '/' || c == '\\') {
                index = i + 1; // we want to include the separator in the result
                break;
            }
        }

        return sub1.substring(0, index);
    }

    public static String getFirstToken(String pattern) {
        if (pattern == null) {
            return null;
        }
        int startIndex = pattern.indexOf('[');
        if (startIndex == -1) {
            return null;
        }
        int endIndex = pattern.indexOf(']', startIndex);
        if (endIndex == -1) {
            return null;
        }
        return pattern.substring(startIndex + 1, endIndex);
    }

    /**
     * This class returns a captured value after validating it doesn't
     * contain any path traversal sequence.
     *
     * <p>{@code toString}</p> will be invoked when the value is
     * actually used as a token inside of a pattern passed to {@link
     * #substituteTokens}.</p>
     */
    private static class Validated {
        private final String tokenName, tokenValue;

        private Validated(String tokenName, String tokenValue) {
            this.tokenName = tokenName;
            this.tokenValue = tokenValue;
        }

        @Override
        public String toString() {
            if (tokenValue != null && !tokenValue.isEmpty()) {
                StringTokenizer tok = new StringTokenizer(tokenValue.replace("\\", "/"), "/");
                while (tok.hasMoreTokens()) {
                    if ("..".equals(tok.nextToken())) {
                        throw new IllegalArgumentException("\'" + tokenName + "\' value " + tokenValue + " contains an illegal path sequence");
                    }
                }
            }
            return tokenValue;
        }
    }

    private static void checkAgainstPathTraversal(String pattern, String afterTokenSubstitution) {
        String root = getTokenRoot(pattern);
        int rootLen = root.length(); // it is OK to have a token root containing .. sequences
        if (root.endsWith("/") || root.endsWith("\\")) {
            --rootLen;
        }
        String patternedPartWithNormalizedSlashes =
            afterTokenSubstitution.substring(rootLen).replace("\\", "/");
        if (patternedPartWithNormalizedSlashes.endsWith("/..")
            || patternedPartWithNormalizedSlashes.indexOf("/../") >= 0) {
            throw new IllegalArgumentException("path after token expansion contains an illegal sequence");
        }
    }

}
