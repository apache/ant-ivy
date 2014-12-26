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

import java.util.List;
import java.util.ListIterator;
import java.util.regex.Pattern;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.core.search.SearchEngine;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.latest.ArtifactInfo;
import org.apache.ivy.plugins.latest.LatestStrategy;
import org.apache.ivy.plugins.matcher.ExactOrRegexpPatternMatcher;
import org.apache.ivy.plugins.matcher.ExactPatternMatcher;
import org.apache.ivy.plugins.matcher.Matcher;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.plugins.version.VersionMatcher;
import org.apache.tools.ant.BuildException;

/**
 * Look for the latest module in the repository matching the given criteria, and sets a set of
 * properties according to what was found.
 */
public class IvyBuildNumber extends IvyTask {

    public static class ResolvedModuleRevisionArtifactInfo implements ArtifactInfo {
        private ModuleRevisionId rmr;

        public ResolvedModuleRevisionArtifactInfo(ModuleRevisionId rmr) {
            this.rmr = rmr;
        }

        public String getRevision() {
            return rmr.getRevision();
        }

        public long getLastModified() {
            return -1;
        }

    }

    private String organisation;

    private String module;

    private String branch;

    private String revision;

    private String revSep = ".";

    private String prefix = "ivy.";

    private String defaultValue = "0";

    private String defaultBuildNumber = "0";

    private String resolver = null;

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getOrganisation() {
        return organisation;
    }

    public void setOrganisation(String organisation) {
        this.organisation = organisation;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getDefault() {
        return defaultValue;
    }

    public void setDefault(String default1) {
        defaultValue = default1;
    }

    public String getResolver() {
        return resolver;
    }

    public void setResolver(String resolver) {
        this.resolver = resolver;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void doExecute() throws BuildException {
        if (organisation == null) {
            throw new BuildException("no organisation provided for ivy buildnumber task");
        }
        if (module == null) {
            throw new BuildException("no module name provided for ivy buildnumber task");
        }
        if (prefix == null) {
            throw new BuildException("null prefix not allowed");
        }

        Ivy ivy = getIvyInstance();
        IvySettings settings = ivy.getSettings();
        if (branch == null) {
            branch = settings.getDefaultBranch(new ModuleId(organisation, module));
        }
        if (revision == null || revision.length() == 0) {
            revision = "latest.integration";
        } else if (!revision.endsWith("+")) {
            revision = revision + "+";
        }
        if (!prefix.endsWith(".") && prefix.length() > 0) {
            prefix = prefix + ".";
        }

        SearchEngine searcher = new SearchEngine(settings);

        PatternMatcher patternMatcher = new PatternMatcher() {
            private PatternMatcher exact = new ExactPatternMatcher();

            private PatternMatcher regexp = new ExactOrRegexpPatternMatcher();

            public Matcher getMatcher(String expression) {
                if (expression.equals(organisation) || expression.equals(module)
                        || expression.equals(branch)) {
                    return exact.getMatcher(expression);
                } else {
                    return regexp.getMatcher(expression);
                }
            }

            public String getName() {
                return "buildnumber-matcher";
            }
        };

        String revisionPattern = ".*";
        if (revision.endsWith("+")) {
            revisionPattern = Pattern.quote(revision.substring(0, revision.length() - 1)) + ".*";
        }

        ModuleRevisionId mrid = ModuleRevisionId.newInstance(organisation, module, branch,
            revisionPattern);
        ModuleRevisionId[] revisions;
        if (resolver == null) {
            revisions = searcher.listModules(mrid, patternMatcher);
        } else {
            DependencyResolver depResolver = settings.getResolver(resolver);
            if (depResolver == null) {
                throw new BuildException("Unknown resolver: " + resolver);
            }
            revisions = searcher.listModules(depResolver, mrid, patternMatcher);
        }

        ArtifactInfo[] infos = new ArtifactInfo[revisions.length];
        for (int i = 0; i < revisions.length; i++) {
            infos[i] = new ResolvedModuleRevisionArtifactInfo(revisions[i]);
        }

        VersionMatcher matcher = settings.getVersionMatcher();
        LatestStrategy latestStrategy = settings.getLatestStrategy("latest-revision");
        List sorted = latestStrategy.sort(infos);

        ModuleRevisionId askedMrid = ModuleRevisionId.newInstance(organisation, module, branch,
            revision);

        String foundRevision = null;
        for (ListIterator iter = sorted.listIterator(sorted.size()); iter.hasPrevious();) {
            ResolvedModuleRevisionArtifactInfo info = (ResolvedModuleRevisionArtifactInfo) iter
                    .previous();

            if (!matcher.accept(askedMrid, info.rmr)) {
                continue;
            }

            if (matcher.needModuleDescriptor(askedMrid, info.rmr)) {
                ResolvedModuleRevision rmr = ivy.findModule(info.rmr);
                if (matcher.accept(askedMrid, rmr.getDescriptor())) {
                    foundRevision = info.rmr.getRevision();
                }
            } else {
                foundRevision = info.rmr.getRevision();
            }

            if (foundRevision != null) {
                break;
            }
        }

        NewRevision newRevision = computeNewRevision(foundRevision);
        setProperty("revision", newRevision.getRevision());
        setProperty("new.revision", newRevision.getNewRevision());
        setProperty("build.number", newRevision.getBuildNumber());
        setProperty("new.build.number", newRevision.getNewBuildNumber());
    }

    private void setProperty(String propertyName, String value) {
        if (value != null) {
            getProject().setProperty(prefix + propertyName, value);
        }
    }

    private NewRevision computeNewRevision(String revision) {
        String revPrefix = "latest.integration".equals(this.revision) ? "" : this.revision
                .substring(0, this.revision.length() - 1);
        if (revision != null && !revision.startsWith(revPrefix)) {
            throw new BuildException("invalid exception found in repository: '" + revision
                    + "' for '" + revPrefix + "'");
        }
        if (revision == null) {
            if (revPrefix.length() > 0) {
                return new NewRevision(revision, revPrefix
                        + (revPrefix.endsWith(revSep) ? defaultBuildNumber : revSep
                                + defaultBuildNumber), null, defaultBuildNumber);
            } else {
                Range r = findLastNumber(defaultValue);
                if (r == null) { // no number found
                    return new NewRevision(revision, defaultValue, null, null);
                } else {
                    long n = Long.parseLong(defaultValue.substring(r.getStartIndex(),
                        r.getEndIndex()));
                    return new NewRevision(revision, defaultValue, null, String.valueOf(n));
                }
            }
        }
        Range r;
        if (revPrefix.length() == 0) {
            r = findLastNumber(revision);
            if (r == null) {
                return new NewRevision(revision, revision
                        + (revision.endsWith(revSep) ? "1" : revSep + "1"), null, "1");
            }
        } else {
            r = findFirstNumber(revision, revPrefix.length());
            if (r == null) {
                return new NewRevision(revision, revPrefix
                        + (revPrefix.endsWith(revSep) ? "1" : revSep + "1"), null, "1");
            }
        }
        long n = Long.parseLong(revision.substring(r.getStartIndex(), r.getEndIndex())) + 1;
        return new NewRevision(revision, revision.substring(0, r.getStartIndex()) + n,
                String.valueOf(n - 1), String.valueOf(n));
    }

    private Range findFirstNumber(String str, int startIndex) {
        // let's find the first digit in the string
        int startNumberIndex = startIndex;
        while (startNumberIndex < str.length() && !Character.isDigit(str.charAt(startNumberIndex))) {
            startNumberIndex++;
        }
        if (startNumberIndex == str.length()) {
            return null;
        }
        // let's find the end of the number
        int endNumberIndex = startNumberIndex + 1;
        while (endNumberIndex < str.length() && Character.isDigit(str.charAt(endNumberIndex))) {
            endNumberIndex++;
        }
        return new Range(startNumberIndex, endNumberIndex);
    }

    private Range findLastNumber(String str) {
        int endNumberIndex = str.length() - 1;
        while (endNumberIndex >= 0 && !Character.isDigit(str.charAt(endNumberIndex))) {
            endNumberIndex--;
        }
        int startNumberIndex = endNumberIndex == -1 ? -1 : endNumberIndex - 1;
        while (startNumberIndex >= 0 && Character.isDigit(str.charAt(startNumberIndex))) {
            startNumberIndex--;
        }
        endNumberIndex++;
        startNumberIndex++;
        if (startNumberIndex == endNumberIndex) { // no number found
            return null;
        } else {
            return new Range(startNumberIndex, endNumberIndex);
        }
    }

    private static class Range {
        private int startIndex;

        private int endIndex;

        public Range(int startIndex, int endIndex) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }

        public int getStartIndex() {
            return startIndex;
        }

        public int getEndIndex() {
            return endIndex;
        }
    }

    private static class NewRevision {
        private String revision;

        private String newRevision;

        private String buildNumber;

        private String newBuildNumber;

        public NewRevision(String revision, String newRevision, String buildNumber,
                String newBuildNumber) {
            this.revision = revision;
            this.newRevision = newRevision;
            this.buildNumber = buildNumber;
            this.newBuildNumber = newBuildNumber;
        }

        public String getRevision() {
            return revision;
        }

        public String getNewRevision() {
            return newRevision;
        }

        public String getBuildNumber() {
            return buildNumber;
        }

        public String getNewBuildNumber() {
            return newBuildNumber;
        }
    }

    public String getRevSep() {
        return revSep;
    }

    public void setRevSep(String revSep) {
        this.revSep = revSep;
    }

    public String getDefaultBuildNumber() {
        return defaultBuildNumber;
    }

    public void setDefaultBuildNumber(String defaultBuildNumber) {
        this.defaultBuildNumber = defaultBuildNumber;
    }
}
