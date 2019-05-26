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
package org.apache.ivy.osgi.repo;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.DownloadStatus;
import org.apache.ivy.core.report.MetadataArtifactDownloadReport;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.osgi.core.BundleInfo;
import org.apache.ivy.osgi.core.BundleInfoAdapter;
import org.apache.ivy.osgi.util.Version;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.plugins.repository.url.URLRepository;
import org.apache.ivy.plugins.repository.url.URLResource;
import org.apache.ivy.plugins.resolver.BasicResolver;
import org.apache.ivy.plugins.resolver.util.MDResolvedResource;
import org.apache.ivy.plugins.resolver.util.ResolvedResource;
import org.apache.ivy.plugins.resolver.util.ResourceMDParser;
import org.apache.ivy.util.Message;

import static org.apache.ivy.util.StringUtils.isNullOrEmpty;

public abstract class AbstractOSGiResolver extends BasicResolver {

    private static final String CAPABILITY_EXTRA_ATTR = "osgi_bundle";

    protected static final RepoDescriptor FAILING_REPO_DESCRIPTOR = new EditableRepoDescriptor(
            null, null);

    private RepoDescriptor repoDescriptor = null;

    private URLRepository repository = new URLRepository();

    public static class RequirementStrategy {
        // take the first matching
        public static RequirementStrategy first = new RequirementStrategy();

        // if there are any ambiguity, fail to resolve
        public static RequirementStrategy noambiguity = new RequirementStrategy();

        public static RequirementStrategy valueOf(String strategy) {
            if (strategy.equals("first")) {
                return first;
            }
            if (strategy.equals("noambiguity")) {
                return noambiguity;
            }
            throw new IllegalStateException();
        }
    }

    private RequirementStrategy requirementStrategy = RequirementStrategy.noambiguity;

    public void setRequirementStrategy(RequirementStrategy importPackageStrategy) {
        this.requirementStrategy = importPackageStrategy;
    }

    public void setRequirementStrategy(String strategy) {
        setRequirementStrategy(RequirementStrategy.valueOf(strategy));
    }

    protected void setRepoDescriptor(RepoDescriptor repoDescriptor) {
        this.repoDescriptor = repoDescriptor;
    }

    public URLRepository getRepository() {
        return repository;
    }

    protected void ensureInit() {
        if (repoDescriptor == null) {
            try {
                init();
            } catch (Exception e) {
                repoDescriptor = FAILING_REPO_DESCRIPTOR;
                throw new RuntimeException("Error while loading the OSGi repo descriptor"
                        + e.getMessage() + " (" + e.getClass().getName() + ")", e);
            }
        } else if (repoDescriptor == FAILING_REPO_DESCRIPTOR) {
            throw new RuntimeException("The repository " + getName() + " already failed to load");
        }
    }

    protected abstract void init();

    public RepoDescriptor getRepoDescriptor() {
        ensureInit();
        return repoDescriptor;
    }

    @Override
    public boolean isAllownomd() {
        // this a repo based resolver, we always have md
        return false;
    }

    public ResolvedResource findIvyFileRef(DependencyDescriptor dd, ResolveData data) {
        ModuleRevisionId mrid = dd.getDependencyRevisionId();

        String osgiType = mrid.getOrganisation();
        if (osgiType == null) {
            throw new RuntimeException("Unsupported OSGi module Id: " + mrid.getModuleId());
        }
        String id = mrid.getName();
        Collection<ModuleDescriptor> mds = ModuleDescriptorWrapper.unwrap(getRepoDescriptor()
                .findModules(osgiType, id));
        if (mds == null || mds.isEmpty()) {
            Message.verbose("\t " + id + " not found.");
            return null;
        }

        ResolvedResource[] ret;
        if (BundleInfo.BUNDLE_TYPE.equals(osgiType)) {
            ret = findBundle(dd, data, mds);
        } else {
            ret = findCapability(dd, data, mds);
        }

        ResolvedResource found = findResource(ret, getDefaultRMDParser(dd.getDependencyId()), mrid,
            data.getDate());
        if (found == null) {
            Message.debug("\t" + getName() + ": no resource found for " + mrid);
        }
        return found;
    }

    public ResolvedResource[] findBundle(DependencyDescriptor dd, ResolveData data,
            Collection<ModuleDescriptor> mds) {
        ResolvedResource[] ret = new ResolvedResource[mds.size()];
        int i = 0;
        for (ModuleDescriptor md : mds) {
            MetadataArtifactDownloadReport report = new MetadataArtifactDownloadReport(null);
            report.setDownloadStatus(DownloadStatus.NO);
            report.setSearched(true);
            ResolvedModuleRevision rmr = new ResolvedModuleRevision(this, this, md, report);
            MDResolvedResource mdrr = new MDResolvedResource(null, md.getRevision(), rmr);
            ret[i++] = mdrr;
        }
        return ret;
    }

    public ResolvedResource[] findCapability(DependencyDescriptor dd, ResolveData data,
            Collection<ModuleDescriptor> mds) {
        List<ResolvedResource> ret = new ArrayList<>(mds.size());
        for (ModuleDescriptor md : mds) {
            IvyNode node = data.getNode(md.getModuleRevisionId());
            if (node != null && node.getDescriptor() != null) {
                // already resolved import, no need to go further
                return new ResolvedResource[] {buildResolvedCapabilityMd(dd, node.getDescriptor())};
            }
            ret.add(buildResolvedCapabilityMd(dd, md));
        }
        return ret.toArray(new ResolvedResource[mds.size()]);
    }

    private MDResolvedResource buildResolvedCapabilityMd(DependencyDescriptor dd,
            ModuleDescriptor md) {
        String org = dd.getDependencyRevisionId().getOrganisation();
        String name = dd.getDependencyRevisionId().getName();
        String rev = md.getExtraInfoContentByTagName(BundleInfoAdapter.EXTRA_INFO_EXPORT_PREFIX
                + name);
        ModuleRevisionId capabilityRev = ModuleRevisionId.newInstance(org, name, rev,
            Collections.singletonMap(CAPABILITY_EXTRA_ATTR, md.getModuleRevisionId().toString()));

        DefaultModuleDescriptor capabilityMd = new DefaultModuleDescriptor(capabilityRev,
                getSettings().getStatusManager().getDefaultStatus(), new Date());

        String useConf = BundleInfoAdapter.CONF_USE_PREFIX + dd.getDependencyRevisionId().getName();

        capabilityMd.addConfiguration(BundleInfoAdapter.CONF_DEFAULT);
        capabilityMd.addConfiguration(BundleInfoAdapter.CONF_OPTIONAL);
        capabilityMd.addConfiguration(BundleInfoAdapter.CONF_TRANSITIVE_OPTIONAL);
        capabilityMd.addConfiguration(new Configuration(useConf));

        DefaultDependencyDescriptor capabilityDD = new DefaultDependencyDescriptor(
                md.getModuleRevisionId(), false);
        capabilityDD.addDependencyConfiguration(BundleInfoAdapter.CONF_NAME_DEFAULT,
            BundleInfoAdapter.CONF_NAME_DEFAULT);
        capabilityDD.addDependencyConfiguration(BundleInfoAdapter.CONF_NAME_OPTIONAL,
            BundleInfoAdapter.CONF_NAME_OPTIONAL);
        capabilityDD.addDependencyConfiguration(BundleInfoAdapter.CONF_NAME_TRANSITIVE_OPTIONAL,
            BundleInfoAdapter.CONF_NAME_TRANSITIVE_OPTIONAL);
        capabilityDD.addDependencyConfiguration(useConf, useConf);
        capabilityMd.addDependency(capabilityDD);

        MetadataArtifactDownloadReport report = new MetadataArtifactDownloadReport(null);
        report.setDownloadStatus(DownloadStatus.NO);
        report.setSearched(true);
        ResolvedModuleRevision rmr = new ResolvedModuleRevision(this, this, capabilityMd, report);
        return new MDResolvedResource(null, capabilityMd.getRevision(), rmr);
    }

    @Override
    public ResolvedResource findResource(ResolvedResource[] rress, ResourceMDParser rmdparser,
            ModuleRevisionId mrid, Date date) {
        ResolvedResource found = super.findResource(rress, rmdparser, mrid, date);
        if (found == null) {
            return null;
        }

        String osgiType = mrid.getOrganisation();
        // for non bundle requirement : log the selected bundle
        if (!BundleInfo.BUNDLE_TYPE.equals(osgiType)) {
            // several candidates with different symbolic name : make an warning about the ambiguity
            if (rress.length != 1) {
                // several candidates with different symbolic name ?
                Map<String, List<MDResolvedResource>> matching = new HashMap<>();
                for (ResolvedResource rres : rress) {
                    String name = ((MDResolvedResource) rres).getResolvedModuleRevision()
                            .getDescriptor().getExtraAttribute(CAPABILITY_EXTRA_ATTR);
                    List<MDResolvedResource> list = matching.get(name);
                    if (list == null) {
                        list = new ArrayList<>();
                        matching.put(name, list);
                    }
                    list.add((MDResolvedResource) rres);
                }
                if (matching.keySet().size() != 1) {
                    if (requirementStrategy == RequirementStrategy.first) {
                        Message.warn("Ambiguity for the '" + osgiType + "' requirement "
                                + mrid.getName() + ";version=" + mrid.getRevision());
                        for (Map.Entry<String, List<MDResolvedResource>> entry : matching.entrySet()) {
                            Message.warn("\t" + entry.getKey());
                            for (MDResolvedResource c : entry.getValue()) {
                                Message.warn("\t\t" + c.getRevision()
                                        + (found == c ? " (selected)" : ""));
                            }
                        }
                    } else if (requirementStrategy == RequirementStrategy.noambiguity) {
                        Message.error("Ambiguity for the '" + osgiType + "' requirement "
                                + mrid.getName() + ";version=" + mrid.getRevision());
                        for (Map.Entry<String, List<MDResolvedResource>> entry : matching.entrySet()) {
                            Message.error("\t" + entry.getKey());
                            for (MDResolvedResource c : entry.getValue()) {
                                Message.error("\t\t" + c.getRevision()
                                        + (found == c ? " (best match)" : ""));
                            }
                        }
                        return null;
                    }
                }
            }
            Message.info("'" + osgiType + "' requirement " + mrid.getName() + ";version="
                    + mrid.getRevision() + " satisfied by "
                    + ((MDResolvedResource) found).getResolvedModuleRevision().getId().getName()
                    + ";" + found.getRevision());
        }

        return found;
    }

    @Override
    public ResolvedResource findArtifactRef(Artifact artifact, Date date) {
        URL url = artifact.getUrl();
        if (url == null) {
            // not an artifact resolved by this resolver
            return null;
        }
        Message.verbose("\tusing url for " + artifact + ": " + url);
        logArtifactAttempt(artifact, url.toExternalForm());
        final Resource resource = new URLResource(url, this.getTimeoutConstraint());
        return new ResolvedResource(resource, artifact.getModuleRevisionId().getRevision());
    }

    @Override
    protected void checkModuleDescriptorRevision(ModuleDescriptor systemMd,
            ModuleRevisionId systemMrid) {
        String osgiType = systemMrid.getOrganisation();
        // only check revision if we're searching for a bundle (package and bundle have different
        // version
        if (osgiType == null || osgiType.equals(BundleInfo.BUNDLE_TYPE)) {
            super.checkModuleDescriptorRevision(systemMd, systemMrid);
        }
    }

    @Override
    protected Collection<String> filterNames(Collection<String> names) {
        getSettings().filterIgnore(names);
        return names;
    }

    @Override
    protected Collection<String> findNames(Map<String, String> tokenValues, String token) {
        if (IvyPatternHelper.ORGANISATION_KEY.equals(token)) {
            return getRepoDescriptor().getCapabilities();
        }

        String osgiType = tokenValues.get(IvyPatternHelper.ORGANISATION_KEY);
        if (isNullOrEmpty(osgiType)) {
            return Collections.emptyList();
        }

        if (IvyPatternHelper.MODULE_KEY.equals(token)) {
            return getRepoDescriptor().getCapabilityValues(osgiType);
        }

        if (IvyPatternHelper.REVISION_KEY.equals(token)) {
            String name = tokenValues.get(IvyPatternHelper.MODULE_KEY);
            List<String> versions = new ArrayList<>();
            Set<ModuleDescriptorWrapper> mds = getRepoDescriptor().findModules(osgiType, name);
            if (mds != null) {
                for (ModuleDescriptorWrapper md : mds) {
                    versions.add(md.getBundleInfo().getVersion().toString());
                }
            }
            return versions;
        }

        if (IvyPatternHelper.CONF_KEY.equals(token)) {
            String name = tokenValues.get(IvyPatternHelper.MODULE_KEY);
            if (name == null) {
                return Collections.emptyList();
            }
            if (osgiType.equals(BundleInfo.PACKAGE_TYPE)) {
                return Collections.singletonList(BundleInfoAdapter.CONF_USE_PREFIX + name);
            }
            Collection<ModuleDescriptor> mds = ModuleDescriptorWrapper.unwrap(getRepoDescriptor()
                    .findModules(osgiType, name));
            if (mds == null) {
                return Collections.emptyList();
            }
            String version = tokenValues.get(IvyPatternHelper.REVISION_KEY);
            if (version == null) {
                return Collections.emptyList();
            }
            ModuleDescriptor found = null;
            for (ModuleDescriptor md : mds) {
                if (md.getRevision().equals(version)) {
                    found = md;
                }
            }
            if (found == null) {
                return Collections.emptyList();
            }
            return Arrays.asList(found.getConfigurationsNames());
        }
        return Collections.emptyList();
    }

    /**
     * Populate capabilityValues with capability values for which at least one module match the
     * expected revision
     */
    @SuppressWarnings("unused")
    private void filterCapabilityValues(Set<String> capabilityValues,
            Map<String, Set<ModuleDescriptor>> moduleByCapabilityValue,
            Map<String, String> tokenValues, String rev) {
        if (rev == null) {
            // no revision, all match then
            capabilityValues.addAll(moduleByCapabilityValue.keySet());
        } else {
            for (Map.Entry<String, Set<ModuleDescriptor>> entry : moduleByCapabilityValue.entrySet()) {
                boolean moduleMatchRev = false;
                for (ModuleDescriptor md : entry.getValue()) {
                    moduleMatchRev = rev.equals(md.getRevision());
                    if (moduleMatchRev) {
                        break;
                    }
                }
                if (moduleMatchRev) {
                    // at least one module matched, the capability value is ok to add
                    capabilityValues.add(entry.getKey());
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, String>[] listTokenValues(String[] tokens, Map<String, Object> criteria) {
        Set<String> tokenSet = new HashSet<>(Arrays.asList(tokens));
        Set<Map<String, String>> listTokenValues = listTokenValues(tokenSet, criteria);
        return listTokenValues.toArray(new Map[listTokenValues.size()]);
    }

    private Set<Map<String, String>> listTokenValues(Set<String> tokens,
            Map<String, Object> criteria) {
        Map<String, String> stringCriteria = new HashMap<>();
        for (Map.Entry<String, Object> entry : criteria.entrySet()) {
            Object value = entry.getValue();
            if (!(value instanceof String)) {
                // no support for matcher for now
                return Collections.emptySet();
            }
            stringCriteria.put(entry.getKey(), (String) value);
        }

        if (tokens.isEmpty()) {
            // no more tokens to resolve
            return Collections.singleton(stringCriteria);
        }

        Set<String> remainingTokens = new HashSet<>(tokens);

        remainingTokens.remove(IvyPatternHelper.ORGANISATION_KEY);
        String osgiType = stringCriteria.get(IvyPatternHelper.ORGANISATION_KEY);
        if (osgiType == null) {
            Map<String, Object> newCriteria = new HashMap<>(criteria);
            newCriteria.put(IvyPatternHelper.ORGANISATION_KEY, BundleInfo.BUNDLE_TYPE);
            Set<Map<String, String>> tokenValues = new HashSet<>(listTokenValues(remainingTokens,
                    newCriteria));
            newCriteria = new HashMap<>(criteria);
            newCriteria.put(IvyPatternHelper.ORGANISATION_KEY, BundleInfo.PACKAGE_TYPE);
            tokenValues.addAll(listTokenValues(remainingTokens, newCriteria));
            newCriteria = new HashMap<>(criteria);
            newCriteria.put(IvyPatternHelper.ORGANISATION_KEY, BundleInfo.SERVICE_TYPE);
            tokenValues.addAll(listTokenValues(remainingTokens, newCriteria));
            return tokenValues;
        }

        Map<String, String> values = new HashMap<>();
        values.put(IvyPatternHelper.ORGANISATION_KEY, osgiType);

        Set<String> capabilities = getRepoDescriptor().getCapabilityValues(osgiType);
        if (capabilities == null || capabilities.isEmpty()) {
            return Collections.emptySet();
        }

        remainingTokens.remove(IvyPatternHelper.MODULE_KEY);
        String module = stringCriteria.get(IvyPatternHelper.MODULE_KEY);
        if (module == null) {
            Set<Map<String, String>> tokenValues = new HashSet<>();
            for (String name : capabilities) {
                Map<String, Object> newCriteria = new HashMap<>(criteria);
                newCriteria.put(IvyPatternHelper.MODULE_KEY, name);
                tokenValues.addAll(listTokenValues(remainingTokens, newCriteria));
            }
            return tokenValues;
        }
        values.put(IvyPatternHelper.MODULE_KEY, module);

        remainingTokens.remove(IvyPatternHelper.REVISION_KEY);
        String rev = stringCriteria.get(IvyPatternHelper.REVISION_KEY);
        if (rev == null) {
            Set<ModuleDescriptorWrapper> mdws = getRepoDescriptor().findModules(osgiType, module);
            if (mdws == null || mdws.isEmpty()) {
                return Collections.emptySet();
            }
            Set<Map<String, String>> tokenValues = new HashSet<>();
            for (ModuleDescriptorWrapper mdw : mdws) {
                Map<String, Object> newCriteria = new HashMap<>(criteria);
                newCriteria.put(IvyPatternHelper.REVISION_KEY, mdw.getBundleInfo().getVersion()
                        .toString());
                tokenValues.addAll(listTokenValues(remainingTokens, newCriteria));
            }
            return tokenValues;
        }
        values.put(IvyPatternHelper.REVISION_KEY, rev);

        remainingTokens.remove(IvyPatternHelper.CONF_KEY);
        String conf = stringCriteria.get(IvyPatternHelper.CONF_KEY);
        if (conf == null) {
            if (osgiType.equals(BundleInfo.PACKAGE_TYPE)) {
                values.put(IvyPatternHelper.CONF_KEY, BundleInfoAdapter.CONF_USE_PREFIX + module);
                return Collections.singleton(values);
            }
            Set<ModuleDescriptorWrapper> bundles = getRepoDescriptor()
                    .findModules(osgiType, module);
            if (bundles == null) {
                return Collections.emptySet();
            }
            Version v = new Version(rev);
            ModuleDescriptorWrapper found = null;
            for (ModuleDescriptorWrapper bundle : bundles) {
                if (bundle.getBundleInfo().getVersion().equals(v)) {
                    found = bundle;
                }
            }
            if (found == null) {
                return Collections.emptySet();
            }
            Set<Map<String, String>> tokenValues = new HashSet<>();
            List<String> configurations = BundleInfoAdapter
                    .getConfigurations(found.getBundleInfo());
            for (String configuration : configurations) {
                Map<String, String> newCriteria = new HashMap<>(stringCriteria);
                newCriteria.put(IvyPatternHelper.CONF_KEY, configuration);
                tokenValues.add(newCriteria);
            }
            return tokenValues;
        }
        values.put(IvyPatternHelper.CONF_KEY, conf);

        return Collections.singleton(values);
    }

    @Override
    protected long get(Resource resource, File dest) throws IOException {
        Message.verbose("\t" + getName() + ": downloading " + resource.getName());
        Message.debug("\t\tto " + dest);
        if (dest.getParentFile() != null) {
            dest.getParentFile().mkdirs();
        }
        getRepository().get(resource.getName(), dest);
        return dest.length();
    }

    @Override
    protected Resource getResource(String source) throws IOException {
        return getRepository().getResource(source);
    }

    public void publish(Artifact artifact, File src, boolean overwrite) throws IOException {
        throw new UnsupportedOperationException();
    }

}
