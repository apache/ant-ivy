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
package org.apache.ivy.ant;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.core.sort.SortOptions;
import org.apache.ivy.plugins.matcher.MapMatcher;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.ivy.plugins.parser.ModuleDescriptorParserRegistry;
import org.apache.ivy.util.Message;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;

/**
 * Creates an ant filelist of files (usually build.xml) ordered according to the dependencies
 * declared in ivy files.
 */
public class IvyBuildList extends IvyTask {
    public static final class OnMissingDescriptor {
        public static final String HEAD = "head";

        public static final String TAIL = "tail";

        public static final String SKIP = "skip";

        public static final String FAIL = "fail";

        public static final String WARN = "warn";

        private OnMissingDescriptor() {
        }
    }

    public static final class BuildListModule {

        private String organisation;

        private String module;

        private String revision;

        private String branch;

        private File file;

        public String getOrganisation() {
            return organisation;
        }

        public void setOrganisation(String organisation) {
            this.organisation = organisation;
        }

        public String getModule() {
            return module;
        }

        public void setModule(String module) {
            this.module = module;
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

        public File getFile() {
            return file;
        }

        public void setFile(File file) {
            this.file = file;
        }

    }

    public static final String DESCRIPTOR_REQUIRED = "required";

    private List<FileSet> buildFileSets = new ArrayList<>();

    private String reference;

    private boolean haltOnError = true;

    private String onMissingDescriptor = OnMissingDescriptor.HEAD;

    private boolean reverse = false;

    private String ivyFilePath;

    private String root = "*";

    private List<BuildListModule> roots = new ArrayList<>();

    private boolean excludeRoot = false;

    private String leaf = "*";

    private List<BuildListModule> leafs = new ArrayList<>();

    private String delimiter = ",";

    private boolean excludeLeaf = false;

    private boolean onlydirectdep = false;

    private String restartFrom = "*";

    public void addFileset(FileSet buildFiles) {
        buildFileSets.add(buildFiles);
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    public BuildListModule createRoot() {
        BuildListModule root = new BuildListModule();
        roots.add(root);
        return root;
    }

    public boolean isExcludeRoot() {
        return excludeRoot;
    }

    public void setExcludeRoot(boolean root) {
        excludeRoot = root;
    }

    public String getLeaf() {
        return leaf;
    }

    public void setLeaf(String leaf) {
        this.leaf = leaf;
    }

    public BuildListModule createLeaf() {
        BuildListModule leaf = new BuildListModule();
        leafs.add(leaf);
        return leaf;
    }

    public boolean isExcludeLeaf() {
        return excludeLeaf;
    }

    public void setExcludeLeaf(boolean excludeLeaf) {
        this.excludeLeaf = excludeLeaf;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public boolean getOnlydirectdep() {
        return onlydirectdep;
    }

    public void setOnlydirectdep(boolean onlydirectdep) {
        this.onlydirectdep = onlydirectdep;
    }

    @Override
    public void doExecute() throws BuildException {
        if (reference == null) {
            throw new BuildException("reference should be provided in ivy build list");
        }
        if (buildFileSets.isEmpty()) {
            throw new BuildException(
                    "at least one nested fileset should be provided in ivy build list");
        }

        Ivy ivy = getIvyInstance();
        IvySettings settings = ivy.getSettings();

        ivyFilePath = getProperty(ivyFilePath, settings, "ivy.buildlist.ivyfilepath");

        Path path = new Path(getProject());

        Map<ModuleDescriptor, File> buildFiles = new HashMap<>();
        List<File> independent = new ArrayList<>();
        List<File> noDescriptor = new ArrayList<>();
        Collection<ModuleDescriptor> mds = new ArrayList<>();

        Set<MapMatcher> rootModules = convert(roots, root, settings);
        Set<MapMatcher> leafModules = convert(leafs, leaf, settings);
        Set<MapMatcher> restartFromModules = convert(Collections.<BuildListModule>emptyList(), restartFrom, settings);

        for (FileSet fs : buildFileSets) {
            DirectoryScanner ds = fs.getDirectoryScanner(getProject());
            for (String build : ds.getIncludedFiles()) {
                File buildFile = new File(ds.getBasedir(), build);
                File ivyFile = getIvyFileFor(buildFile);
                if (!ivyFile.exists()) {
                    onMissingDescriptor(buildFile, ivyFile, noDescriptor);
                } else {
                    try {
                        ModuleDescriptor md = ModuleDescriptorParserRegistry.getInstance()
                                .parseDescriptor(settings, ivyFile.toURI().toURL(),
                                    doValidate(settings));
                        buildFiles.put(md, buildFile);
                        mds.add(md);
                        Message.debug("Add " + md.getModuleRevisionId().getModuleId());
                    } catch (Exception ex) {
                        if (haltOnError) {
                            throw new BuildException("impossible to parse ivy file for "
                                    + buildFile + ": ivyfile=" + ivyFile + " exception=" + ex, ex);
                        } else {
                            Message.warn("impossible to parse ivy file for " + buildFile
                                    + ": ivyfile=" + ivyFile + " exception=" + ex.getMessage());
                            Message.info("\t=> adding it at the beginning of the path");
                            independent.add(buildFile);
                        }
                    }
                }
            }
        }

        List<ModuleDescriptor> leafModuleDescriptors =
                findModuleDescriptors(mds, leafModules, "leaf");
        List<ModuleDescriptor> rootModuleDescriptors =
                findModuleDescriptors(mds, rootModules, "root");
        List<ModuleDescriptor> restartFromModuleDescriptors =
                findModuleDescriptors(mds, restartFromModules, "restartFrom");

        if (!rootModuleDescriptors.isEmpty()) {
            Message.info("Filtering modules based on roots [" + extractModuleNames(rootModules) + "]");
            mds = filterModulesFromRoot(mds, rootModuleDescriptors);
        }
        if (!leafModuleDescriptors.isEmpty()) {
            Message.info("Filtering modules based on leafs [" + extractModuleNames(leafModules) + "]");
            mds = filterModulesFromLeaf(mds, leafModuleDescriptors);
        }

        List<ModuleDescriptor> sortedModules = ivy.sortModuleDescriptors(mds, SortOptions.DEFAULT);

        if (!OnMissingDescriptor.TAIL.equals(onMissingDescriptor)) {
            for (File buildFile : noDescriptor) {
                addBuildFile(path, buildFile);
            }
        }
        for (File buildFile : independent) {
            addBuildFile(path, buildFile);
        }
        if (isReverse()) {
            Collections.reverse(sortedModules);
        }
        // Remove modules that are before the restartFrom point
        // Independent modules (without valid ivy file) can not be addressed
        // so they are not removed from build path.
        if (!restartFromModuleDescriptors.isEmpty()) {
            boolean foundRestartFrom = false;
            List<ModuleDescriptor> keptModules = new ArrayList<>();
            // Only accept one (first) module
            ModuleDescriptor restartFromModuleDescriptor = restartFromModuleDescriptors.get(0);
            for (ModuleDescriptor md : sortedModules) {
                if (md.equals(restartFromModuleDescriptor)) {
                    foundRestartFrom = true;
                }
                if (foundRestartFrom) {
                    keptModules.add(md);
                }
            }
            sortedModules = keptModules;
        }
        StringBuilder order = new StringBuilder();
        for (ModuleDescriptor md : sortedModules) {
            if (order.length() > 0) {
                order.append(", ");
            }
            order.append(md.getModuleRevisionId().getModuleId());
            addBuildFile(path, buildFiles.get(md));
        }
        if (OnMissingDescriptor.TAIL.equals(onMissingDescriptor)) {
            for (File buildFile : noDescriptor) {
                addBuildFile(path, buildFile);
            }
        }

        getProject().addReference(getReference(), path);
        getProject().setProperty("ivy.sorted.modules", order.toString());
    }

    private Set<MapMatcher> convert(List<BuildListModule> modulesList, String modulesString, IvySettings settings) {
        Set<MapMatcher> result = new LinkedHashSet<>();

        for (BuildListModule module : modulesList) {
            File ivyFile = module.getFile();
            if (ivyFile == null) {
                String org = module.getOrganisation();
                String name = module.getModule();
                String rev = module.getRevision();
                String branch = module.getBranch();

                Map<String, String> attributes = new HashMap<>();
                attributes.put(IvyPatternHelper.ORGANISATION_KEY, org == null ? PatternMatcher.ANY_EXPRESSION : org);
                attributes.put(IvyPatternHelper.MODULE_KEY, name == null ? PatternMatcher.ANY_EXPRESSION : name);
                attributes.put(IvyPatternHelper.MODULE_KEY, rev == null ? PatternMatcher.ANY_EXPRESSION : rev);
                attributes.put(IvyPatternHelper.MODULE_KEY, branch == null ? PatternMatcher.ANY_EXPRESSION : branch);

                result.add(new MapMatcher(attributes, settings.getMatcher(PatternMatcher.EXACT)));
            } else {
                try {
                    ModuleDescriptor md = ModuleDescriptorParserRegistry.getInstance()
                            .parseDescriptor(settings, ivyFile.toURI().toURL(),
                                    doValidate(settings));

                    Map<String, String> attributes = new HashMap<>();
                    attributes.putAll(md.getModuleRevisionId().getAttributes());
                    attributes.put("resource", md.getResource().getName());

                    result.add(new MapMatcher(attributes, settings.getMatcher(PatternMatcher.EXACT)));
                } catch (Exception e) {
                    throw new BuildException(e);
                }
            }
        }

        if (!"*".equals(modulesString)) {
            StringTokenizer st = new StringTokenizer(modulesString, getDelimiter());
            while (st.hasMoreTokens()) {
                Map<String, String> attributes = new HashMap<>();
                attributes.put(IvyPatternHelper.MODULE_KEY, st.nextToken());

                result.add(new MapMatcher(attributes, settings.getMatcher(PatternMatcher.EXACT)));
            }
        }

        return result;
    }

    private void onMissingDescriptor(File buildFile, File ivyFile, List<File> noDescriptor) {
        switch (onMissingDescriptor) {
            case OnMissingDescriptor.FAIL:
                throw new BuildException("a module has no module descriptor and"
                        + " onMissingDescriptor=fail. Build file: " + buildFile
                        + ". Expected descriptor: " + ivyFile);
            case OnMissingDescriptor.SKIP:
                Message.debug("skipping " + buildFile + ": descriptor " + ivyFile
                        + " doesn't exist");
                break;
            case OnMissingDescriptor.WARN:
                Message.warn("a module has no module descriptor. " + "Build file: " + buildFile
                        + ". Expected descriptor: " + ivyFile);
                // fall through
            default:
                Message.verbose(String.format("no descriptor for %s: descriptor=%s: adding it at the %s of the path",
                        buildFile, ivyFile, (OnMissingDescriptor.TAIL.equals(onMissingDescriptor) ? "tail" : "head")));
                Message.verbose("\t(change onMissingDescriptor if you want to take another action");
                noDescriptor.add(buildFile);
                break;
        }
    }

    private List<ModuleDescriptor> findModuleDescriptors(
            Collection<ModuleDescriptor> mds, Set<MapMatcher> matchers, String kind) {
        List<ModuleDescriptor> result = new ArrayList<>();
        Set<MapMatcher> missingMatchers = new HashSet<>(matchers);

        for (ModuleDescriptor md : mds) {
            Map<String, String> attributes = new HashMap<>();
            attributes.putAll(md.getAttributes());
            attributes.put("resource", md.getResource().getName());

            for (MapMatcher matcher : matchers) {
                if (matcher.matches(attributes)) {
                    missingMatchers.remove(matcher);
                    result.add(md);
                }
            }
        }

        if (!missingMatchers.isEmpty()) {
            throw new BuildException("unable to find " + kind + " module(s) "
                    + extractModuleNames(missingMatchers) + " in build fileset");
        }

        return result;
    }

    private String extractModuleNames(Set<MapMatcher> matchers) {
        StringBuilder result = new StringBuilder();

        String sep = "";
        for (MapMatcher matcher : matchers) {
            result.append(sep);

            Map<String, String> attributes = matcher.getAttributes();
            String organisation = attributes.get(IvyPatternHelper.ORGANISATION_KEY);
            if (organisation != null && !PatternMatcher.ANY_EXPRESSION.equals(organisation)) {
                result.append(organisation);
                result.append('#');
            }
            result.append(attributes.get(IvyPatternHelper.MODULE_KEY));
            sep = ", ";
        }

        return result.toString();
    }

    /**
     * Returns a collection of ModuleDescriptors that are contained in the input collection of
     * ModuleDescriptors and upon which the root module depends
     *
     * @param mds
     *            input collection of ModuleDescriptors
     * @param rootmds
     *            root module
     * @return filtered list of modules
     */
    private Collection<ModuleDescriptor> filterModulesFromRoot(Collection<ModuleDescriptor> mds,
            List<ModuleDescriptor> rootmds) {
        Map<ModuleId, ModuleDescriptor> moduleIdMap = new HashMap<>();
        for (ModuleDescriptor md : mds) {
            moduleIdMap.put(md.getModuleRevisionId().getModuleId(), md);
        }

        // recursively process the nodes
        Set<ModuleDescriptor> toKeep = new LinkedHashSet<>();

        for (ModuleDescriptor rootmd : rootmds) {
            processFilterNodeFromRoot(rootmd, toKeep, moduleIdMap);
            // With the excluderoot attribute set to true, take the rootmd out of the toKeep set.
            if (excludeRoot) {
                // Only for logging purposes
                Message.verbose("Excluded module "
                        + rootmd.getModuleRevisionId().getModuleId().getName());
            } else {
                toKeep.add(rootmd);
            }
        }

        // just for logging
        for (ModuleDescriptor md : toKeep) {
            Message.verbose("Kept module " + md.getModuleRevisionId().getModuleId().getName());
        }

        return toKeep;
    }

    /**
     * Adds the current node to the toKeep collection and then processes the each of the direct
     * dependencies of this node that appear in the moduleIdMap (indicating that the dependency is
     * part of this BuildList)
     *
     * @param node
     *            the node to be processed
     * @param toKeep
     *            the set of ModuleDescriptors that should be kept
     * @param moduleIdMap
     *            reference mapping of moduleId to ModuleDescriptor that are part of the BuildList
     */
    private void processFilterNodeFromRoot(ModuleDescriptor node, Set<ModuleDescriptor> toKeep,
            Map<ModuleId, ModuleDescriptor> moduleIdMap) {
        // toKeep.add(node);
        for (DependencyDescriptor dep : node.getDependencies()) {
            ModuleId id = dep.getDependencyId();
            ModuleDescriptor md = moduleIdMap.get(id);
            // we test if this module id has a module descriptor, and if it isn't already in the
            // toKeep Set, in which there's probably a circular dependency
            if (md != null && !toKeep.contains(md)) {
                toKeep.add(md);
                if (!getOnlydirectdep()) {
                    processFilterNodeFromRoot(md, toKeep, moduleIdMap);
                }
            }
        }
    }

    /**
     * Returns a collection of ModuleDescriptors that are contained in the input collection of
     * ModuleDescriptors which depends on the leaf module
     *
     * @param mds
     *            input collection of ModuleDescriptors
     * @param leafmds
     *            leaf module
     * @return filtered list of modules
     */
    private Collection<ModuleDescriptor> filterModulesFromLeaf(Collection<ModuleDescriptor> mds,
            List<ModuleDescriptor> leafmds) {
        Map<ModuleId, ModuleDescriptor> moduleIdMap = new HashMap<>();
        for (ModuleDescriptor md : mds) {
            moduleIdMap.put(md.getModuleRevisionId().getModuleId(), md);
        }

        // recursively process the nodes
        Set<ModuleDescriptor> toKeep = new LinkedHashSet<>();
        for (ModuleDescriptor leafmd : leafmds) {
            // With the excludeleaf attribute set to true, take the rootmd out of the toKeep set.
            if (excludeLeaf) {
                Message.verbose("Excluded module "
                        + leafmd.getModuleRevisionId().getModuleId().getName());
            } else {
                toKeep.add(leafmd);
            }
            processFilterNodeFromLeaf(leafmd, toKeep, moduleIdMap);
        }

        // just for logging
        for (ModuleDescriptor md : toKeep) {
            Message.verbose("Kept module " + md.getModuleRevisionId().getModuleId().getName());
        }

        return toKeep;
    }

    /**
     * Search in the moduleIdMap modules depending on node, add them to the toKeep set and process
     * them recursively.
     *
     * @param node
     *            the node to be processed
     * @param toKeep
     *            the set of ModuleDescriptors that should be kept
     * @param moduleIdMap
     *            reference mapping of moduleId to ModuleDescriptor that are part of the BuildList
     */
    private void processFilterNodeFromLeaf(ModuleDescriptor node, Set<ModuleDescriptor> toKeep,
            Map<ModuleId, ModuleDescriptor> moduleIdMap) {
        for (ModuleDescriptor md : moduleIdMap.values()) {
            for (DependencyDescriptor dep : md.getDependencies()) {
                if (node.getModuleRevisionId().getModuleId().equals(dep.getDependencyId())
                        && !toKeep.contains(md)) {
                    toKeep.add(md);
                    if (!getOnlydirectdep()) {
                        processFilterNodeFromLeaf(md, toKeep, moduleIdMap);
                    }
                }
            }
        }
    }

    private void addBuildFile(Path path, File buildFile) {
        FileSet fs = new FileSet();
        fs.setFile(buildFile);
        path.addFileset(fs);
    }

    private File getIvyFileFor(File buildFile) {
        return new File(buildFile.getParentFile(), ivyFilePath);
    }

    public boolean isHaltonerror() {
        return haltOnError;
    }

    public void setHaltonerror(boolean haltOnError) {
        this.haltOnError = haltOnError;
    }

    public String getIvyfilepath() {
        return ivyFilePath;
    }

    public void setIvyfilepath(String ivyFilePath) {
        this.ivyFilePath = ivyFilePath;
    }

    public String getOnMissingDescriptor() {
        return onMissingDescriptor;
    }

    public void setOnMissingDescriptor(String onMissingDescriptor) {
        this.onMissingDescriptor = onMissingDescriptor;
    }

    /**
     * @return boolean
     * @deprecated use {@link #getOnMissingDescriptor()} instead.
     */
    @Deprecated
    public boolean isSkipbuildwithoutivy() {
        return OnMissingDescriptor.SKIP.equals(onMissingDescriptor);
    }

    /**
     * @param skipBuildFilesWithoutIvy boolean
     * @deprecated use {@link #setOnMissingDescriptor(String)} instead.
     */
    @Deprecated
    public void setSkipbuildwithoutivy(boolean skipBuildFilesWithoutIvy) {
        Message.deprecated("skipbuildwithoutivy is deprecated, use onMissingDescriptor instead.");
        this.onMissingDescriptor = skipBuildFilesWithoutIvy ? OnMissingDescriptor.SKIP
                : OnMissingDescriptor.FAIL;
    }

    public boolean isReverse() {
        return reverse;
    }

    public void setReverse(boolean reverse) {
        this.reverse = reverse;
    }

    public String getRestartFrom() {
        return restartFrom;
    }

    public void setRestartFrom(String restartFrom) {
        this.restartFrom = restartFrom;
    }

}
