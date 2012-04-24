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
package org.apache.ivy.osgi.repo;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

public abstract class AbstractOSGiResolver extends BasicResolver {

    private static final String CAPABILITY_EXTRA_ATTR = "osgi_bundle";

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
            init();
        }
    }

    abstract protected void init();

    private RepoDescriptor getRepoDescriptor() {
        ensureInit();
        return repoDescriptor;
    }

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
        Set/* <ModuleDescriptor> */mds = getRepoDescriptor().findModule(osgiType, id);
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

    public ResolvedResource[] findBundle(DependencyDescriptor dd, ResolveData data, Set/*
                                                                                        * <
                                                                                        * ModuleDescriptor
                                                                                        * >
                                                                                        */mds) {
        ResolvedResource[] ret = new ResolvedResource[mds.size()];
        int i = 0;
        Iterator itMd = mds.iterator();
        while (itMd.hasNext()) {
            ModuleDescriptor md = (ModuleDescriptor) itMd.next();
            MetadataArtifactDownloadReport report = new MetadataArtifactDownloadReport(null);
            report.setDownloadStatus(DownloadStatus.NO);
            report.setSearched(true);
            ResolvedModuleRevision rmr = new ResolvedModuleRevision(this, this, md, report);
            MDResolvedResource mdrr = new MDResolvedResource(null, md.getRevision(), rmr);
            ret[i++] = mdrr;
        }
        return ret;
    }

    public ResolvedResource[] findCapability(DependencyDescriptor dd, ResolveData data, Set/*
                                                                                            * <
                                                                                            * ModuleDescriptor
                                                                                            * >
                                                                                            */mds) {
        ResolvedResource[] ret = new ResolvedResource[mds.size()];
        int i = 0;
        Iterator itMd = mds.iterator();
        while (itMd.hasNext()) {
            ModuleDescriptor md = (ModuleDescriptor) itMd.next();
            IvyNode node = data.getNode(md.getModuleRevisionId());
            if (node != null) {
                // already resolved import, no need to go further
                return new ResolvedResource[] {buildResolvedCapabilityMd(dd, node.getDescriptor())};
            }
            ret[i++] = buildResolvedCapabilityMd(dd, md);
        }
        return ret;
    }

    private MDResolvedResource buildResolvedCapabilityMd(DependencyDescriptor dd,
            ModuleDescriptor md) {
        String org = dd.getDependencyRevisionId().getOrganisation();
        String name = dd.getDependencyRevisionId().getName();
        String rev = (String) md.getExtraInfo().get(
            BundleInfoAdapter.EXTRA_INFO_EXPORT_PREFIX + name);
        ModuleRevisionId capabilityRev = ModuleRevisionId.newInstance(org, name, rev,
            Collections.singletonMap(CAPABILITY_EXTRA_ATTR, md.getModuleRevisionId().toString()));

        DefaultModuleDescriptor capabilityMd = new DefaultModuleDescriptor(capabilityRev,
                "release", new Date());

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

    public ResolvedResource findResource(ResolvedResource[] rress, ResourceMDParser rmdparser,
            ModuleRevisionId mrid, Date date) {
        ResolvedResource found = super.findResource(rress, rmdparser, mrid, date);

        String osgiType = mrid.getOrganisation();
        // for non bundle requirement : log the selected bundle
        if (!BundleInfo.BUNDLE_TYPE.equals(osgiType)) {
            // several candidates with different symbolic name : make an warning about the ambiguity
            if (rress.length != 1) {
                // several candidates with different symbolic name ?
                Map/* <String, List<MDResolvedResource>> */matching = new HashMap();
                for (int i = 0; i < rress.length; i++) {
                    String name = ((MDResolvedResource) rress[i]).getResolvedModuleRevision()
                            .getDescriptor().getExtraAttribute(CAPABILITY_EXTRA_ATTR);
                    List/* <MDResolvedResource> */list = (List) matching.get(name);
                    if (list == null) {
                        list = new ArrayList/* <MDResolvedResource> */();
                        matching.put(name, list);
                    }
                    list.add(rress[i]);
                }
                if (matching.keySet().size() != 1) {
                    if (requirementStrategy == RequirementStrategy.first) {
                        Message.warn("Ambiguity for the '" + osgiType + "' requirement "
                                + mrid.getName() + ";version=" + mrid.getRevision());
                        Iterator itMatching = matching.entrySet().iterator();
                        while (itMatching.hasNext()) {
                            Entry/* <String, List<MDResolvedResource>> */entry = (Entry) itMatching
                                    .next();
                            Message.warn("\t" + entry.getKey());
                            Iterator itB = ((List) entry.getValue()).iterator();
                            while (itB.hasNext()) {
                                MDResolvedResource c = (MDResolvedResource) itB.next();
                                Message.warn("\t\t" + c.getRevision()
                                        + (found == c ? " (selected)" : ""));
                            }
                        }
                    } else if (requirementStrategy == RequirementStrategy.noambiguity) {
                        Message.error("Ambiguity for the '" + osgiType + "' requirement "
                                + mrid.getName() + ";version=" + mrid.getRevision());
                        Iterator itMatching = matching.entrySet().iterator();
                        while (itMatching.hasNext()) {
                            Entry/* <String, List<MDResolvedResource>> */entry = (Entry) itMatching
                                    .next();
                            Message.error("\t" + entry.getKey());
                            Iterator itB = ((List) entry.getValue()).iterator();
                            while (itB.hasNext()) {
                                MDResolvedResource c = (MDResolvedResource) itB.next();
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

    public ResolvedResource findArtifactRef(Artifact artifact, Date date) {
        URL url = artifact.getUrl();
        if (url == null) {
            // not an artifact resolved by this resolver
            return null;
        }
        Message.verbose("\tusing url for " + artifact + ": " + url);
        logArtifactAttempt(artifact, url.toExternalForm());
        Resource resource = new URLResource(url);
        return new ResolvedResource(resource, artifact.getModuleRevisionId().getRevision());
    }

    protected void checkModuleDescriptorRevision(ModuleDescriptor systemMd,
            ModuleRevisionId systemMrid) {
        String osgiType = systemMrid.getOrganisation();
        // only check revision if we're searching for a bundle (package and bundle have different
        // version
        if (osgiType == null || osgiType.equals(BundleInfo.BUNDLE_TYPE)) {
            super.checkModuleDescriptorRevision(systemMd, systemMrid);
        }
    }

    protected Collection/* <String> */filterNames(Collection/* <String> */names) {
        getSettings().filterIgnore(names);
        return names;
    }

    protected Collection findNames(Map tokenValues, String token) {
        if (IvyPatternHelper.ORGANISATION_KEY.equals(token)) {
            return getRepoDescriptor().getModuleByCapbilities().keySet();
        }

        String osgiType = (String) tokenValues.get(IvyPatternHelper.ORGANISATION_KEY);
        if (osgiType == null || osgiType.length() == 0) {
            return Collections.EMPTY_LIST;
        }

        String rev = (String) tokenValues.get(IvyPatternHelper.REVISION_KEY);

        if (IvyPatternHelper.MODULE_KEY.equals(token)) {
            Map/* <String, Map<String, Set<ModuleDescriptor>>> */moduleByCapbilities = getRepoDescriptor()
                    .getModuleByCapbilities();
            if (osgiType != null) {
                Map/* <String, Set<ModuleDescriptor>> */moduleByCapabilityValue = (Map) moduleByCapbilities
                        .get(osgiType);
                if (moduleByCapabilityValue == null) {
                    return Collections.EMPTY_LIST;
                }
                Set/* <String> */capabilityValues = new HashSet();
                return moduleByCapabilityValue.keySet();
            } else {
                Set/* <String> */capabilityValues = new HashSet();
                Iterator/* <Map<String, Set<ModuleDescriptor>>> */it = ((Collection) moduleByCapbilities
                        .values()).iterator();
                while (it.hasNext()) {
                    Map/* <String, Set<ModuleDescriptor>> */moduleByCapbilityValue = (Map) it
                            .next();
                    filterCapabilityValues(capabilityValues, moduleByCapbilityValue, tokenValues,
                        rev);
                }
                return capabilityValues;
            }
        }

        if (IvyPatternHelper.REVISION_KEY.equals(token)) {
            String name = (String) tokenValues.get(IvyPatternHelper.MODULE_KEY);
            List/* <String> */versions = new ArrayList/* <String> */();
            Set/* <ModuleDescriptor> */mds = getRepoDescriptor().findModule(osgiType, name);
            if (mds != null) {
                Iterator itMd = mds.iterator();
                while (itMd.hasNext()) {
                    ModuleDescriptor md = (ModuleDescriptor) itMd.next();
                    versions.add(md.getRevision());
                }
            }
            return versions;
        }

        if (IvyPatternHelper.CONF_KEY.equals(token)) {
            String name = (String) tokenValues.get(IvyPatternHelper.MODULE_KEY);
            if (name == null) {
                return Collections.EMPTY_LIST;
            }
            if (osgiType.equals(BundleInfo.PACKAGE_TYPE)) {
                return Collections.singletonList(BundleInfoAdapter.CONF_USE_PREFIX + name);
            }
            Set/* <BundleCapabilityAndLocation> */bundleCapabilities = getRepoDescriptor()
                    .findModule(osgiType, name);
            if (bundleCapabilities == null) {
                return Collections.EMPTY_LIST;
            }
            String version = (String) tokenValues.get(IvyPatternHelper.REVISION_KEY);
            if (version == null) {
                return Collections.EMPTY_LIST;
            }
            Version v;
            try {
                v = new Version(version);
            } catch (ParseException e) {
                return Collections.EMPTY_LIST;
            }
            BundleCapabilityAndLocation found = null;
            Iterator itBundle = bundleCapabilities.iterator();
            while (itBundle.hasNext()) {
                BundleCapabilityAndLocation bundleCapability = (BundleCapabilityAndLocation) itBundle
                        .next();
                if (bundleCapability.getVersion().equals(v)) {
                    found = bundleCapability;
                }
            }
            if (found == null) {
                return Collections.EMPTY_LIST;
            }
            List/* <String> */confs = BundleInfoAdapter.getConfigurations(found.getBundleInfo());
            return confs;
        }
        return Collections.EMPTY_LIST;
    }

    /**
     * Populate capabilityValues with capability values for which at least one module match the
     * expected revision
     */
    private void filterCapabilityValues(Set/* <String> */capabilityValues,
            Map/* <String, Set<ModuleDescriptor>> */moduleByCapbilityValue,
            Map/* <String, String */tokenValues, String rev) {
        if (rev == null) {
            // no revision, all match then
            capabilityValues.addAll(moduleByCapbilityValue.keySet());
        } else {
            Iterator/* <Entry<String, Set<ModuleDescriptor>>> */it = moduleByCapbilityValue
                    .entrySet().iterator();
            while (it.hasNext()) {
                Entry/* <String, Set<ModuleDescriptor>> */entry = (Entry) it.next();
                Iterator/* <ModuleDescriptor> */itModules = ((Set) entry.getValue()).iterator();
                boolean moduleMatchRev = false;
                while (!moduleMatchRev && itModules.hasNext()) {
                    ModuleDescriptor md = (ModuleDescriptor) itModules.next();
                    moduleMatchRev = rev.equals(md.getRevision());
                }
                if (moduleMatchRev) {
                    // at least one module matched, the capability value is ok to add
                    capabilityValues.add(entry.getKey());
                }
            }
        }
    }

    public Map[] listTokenValues(String[] tokens, Map criteria) {
        Set/* <String> */tokenSet = new HashSet/* <String> */(Arrays.asList(tokens));
        Set/* <Map<String, String>> */listTokenValues = listTokenValues(tokenSet, criteria);
        return (Map[]) listTokenValues.toArray(new Map[listTokenValues.size()]);
    }

    private Set/* <Map<String, String>> */listTokenValues(Set/* <String> */tokens, Map/*
                                                                                       * <String,
                                                                                       * String>
                                                                                       */criteria) {
        if (tokens.isEmpty()) {
            return Collections./* <Map<String, String>> */singleton(criteria);
        }

        Set/* <String> */tokenSet = new HashSet/* <String> */(tokens);

        Map/* <String, String> */values = new HashMap/* <String, String> */();

        tokenSet.remove(IvyPatternHelper.ORGANISATION_KEY);
        String osgiType = (String) criteria.get(IvyPatternHelper.ORGANISATION_KEY);
        if (osgiType == null || osgiType.length() == 0) {
            return Collections.EMPTY_SET;
        }
        values.put(IvyPatternHelper.ORGANISATION_KEY, osgiType);

        if (osgiType == null) {
            Set/* <Map<String, String>> */tokenValues = new HashSet/* <Map<String, String>> */();
            Map/* <String, String> */newCriteria = new HashMap/* <String, String> */(criteria);
            newCriteria.put(IvyPatternHelper.ORGANISATION_KEY, BundleInfo.BUNDLE_TYPE);
            tokenValues.addAll(listTokenValues(tokenSet, newCriteria));
            newCriteria = new HashMap/* <String, String> */(criteria);
            newCriteria.put(IvyPatternHelper.ORGANISATION_KEY, BundleInfo.PACKAGE_TYPE);
            tokenValues.addAll(listTokenValues(tokenSet, newCriteria));
            newCriteria = new HashMap/* <String, String> */(criteria);
            newCriteria.put(IvyPatternHelper.ORGANISATION_KEY, BundleInfo.SERVICE_TYPE);
            tokenValues.addAll(listTokenValues(tokenSet, newCriteria));
            return tokenValues;
        }

        Set/* <String> */capabilities = getRepoDescriptor().getCapabilityValues(osgiType);
        if (capabilities == null || capabilities.isEmpty()) {
            return Collections.EMPTY_SET;
        }

        tokenSet.remove(IvyPatternHelper.MODULE_KEY);
        String module = (String) criteria.get(IvyPatternHelper.MODULE_KEY);
        if (module == null) {
            Set/* <Map<String, String>> */tokenValues = new HashSet/* <Map<String, String>> */();
            Iterator itNames = capabilities.iterator();
            while (itNames.hasNext()) {
                String name = (String) itNames.next();
                Map/* <String, String> */newCriteria = new HashMap/* <String, String> */(criteria);
                newCriteria.put(IvyPatternHelper.MODULE_KEY, name);
                tokenValues.addAll(listTokenValues(tokenSet, newCriteria));
            }
            return tokenValues;
        }
        values.put(IvyPatternHelper.MODULE_KEY, module);

        tokenSet.remove(IvyPatternHelper.REVISION_KEY);
        String rev = (String) criteria.get(IvyPatternHelper.REVISION_KEY);
        if (rev == null) {
            Set/* <BundleCapabilityAndLocation> */bundleCapabilities = getRepoDescriptor()
                    .findModule(osgiType, module);
            if (bundleCapabilities == null) {
                return Collections.EMPTY_SET;
            }
            Set/* <Map<String, String>> */tokenValues = new HashSet/* <Map<String, String>> */();
            Iterator itBundle = bundleCapabilities.iterator();
            while (itBundle.hasNext()) {
                BundleCapabilityAndLocation capability = (BundleCapabilityAndLocation) itBundle
                        .next();
                Map/* <String, String> */newCriteria = new HashMap/* <String, String> */(criteria);
                newCriteria.put(IvyPatternHelper.REVISION_KEY, capability.getVersion().toString());
                tokenValues.addAll(listTokenValues(tokenSet, newCriteria));
            }
            return tokenValues;
        }
        values.put(IvyPatternHelper.REVISION_KEY, rev);

        tokenSet.remove(IvyPatternHelper.CONF_KEY);
        String conf = (String) criteria.get(IvyPatternHelper.CONF_KEY);
        if (conf == null) {
            if (osgiType.equals(BundleInfo.PACKAGE_TYPE)) {
                values.put(IvyPatternHelper.CONF_KEY, BundleInfoAdapter.CONF_USE_PREFIX + module);
                return Collections./* <Map<String, String>> */singleton(values);
            }
            Set/* <BundleCapabilityAndLocation> */bundleCapabilities = getRepoDescriptor()
                    .findModule(osgiType, module);
            if (bundleCapabilities == null) {
                return Collections.EMPTY_SET;
            }
            Version v;
            try {
                v = new Version(rev);
            } catch (ParseException e) {
                return Collections.EMPTY_SET;
            }
            BundleCapabilityAndLocation found = null;
            Iterator itBundle = bundleCapabilities.iterator();
            while (itBundle.hasNext()) {
                BundleCapabilityAndLocation bundleCapability = (BundleCapabilityAndLocation) itBundle
                        .next();
                if (bundleCapability.getVersion().equals(v)) {
                    found = bundleCapability;
                }
            }
            if (found == null) {
                return Collections.EMPTY_SET;
            }
            Set/* <Map<String, String>> */tokenValues = new HashSet/* <Map<String, String>> */();
            List/* <String> */configurations = BundleInfoAdapter.getConfigurations(found
                    .getBundleInfo());
            for (int i = 0; i < configurations.size(); i++) {
                Map/* <String, String> */newCriteria = new HashMap/* <String, String> */(criteria);
                newCriteria.put(IvyPatternHelper.CONF_KEY, configurations.get(i));
                tokenValues.add(newCriteria);
            }
            return tokenValues;
        }
        values.put(IvyPatternHelper.CONF_KEY, conf);

        return Collections./* <Map<String, String>> */singleton(values);
    }

    protected long get(Resource resource, File dest) throws IOException {
        Message.verbose("\t" + getName() + ": downloading " + resource.getName());
        Message.debug("\t\tto " + dest);
        if (dest.getParentFile() != null) {
            dest.getParentFile().mkdirs();
        }
        getRepository().get(resource.getName(), dest);
        return dest.length();
    }

    protected Resource getResource(String source) throws IOException {
        return getRepository().getResource(source);
    }

    public void publish(Artifact artifact, File src, boolean overwrite) throws IOException {
        throw new UnsupportedOperationException();
    }

}
