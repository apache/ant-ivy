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

import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.MDArtifact;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.DownloadStatus;
import org.apache.ivy.core.report.MetadataArtifactDownloadReport;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.osgi.core.BundleInfo;
import org.apache.ivy.osgi.core.BundleInfoAdapter;
import org.apache.ivy.osgi.core.ExecutionEnvironmentProfileProvider;
import org.apache.ivy.osgi.obr.xml.OBRXMLParser;
import org.apache.ivy.osgi.util.Version;
import org.apache.ivy.plugins.conflict.ConflictManager;
import org.apache.ivy.plugins.latest.ArtifactInfo;
import org.apache.ivy.plugins.repository.Repository;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.plugins.repository.file.FileRepository;
import org.apache.ivy.plugins.resolver.BasicResolver;
import org.apache.ivy.plugins.resolver.util.ResolvedResource;
import org.apache.ivy.plugins.version.VersionMatcher;
import org.apache.ivy.util.Message;
import org.xml.sax.SAXException;

public abstract class BundleRepoResolver extends BasicResolver {

    private Repository repository = null;

    private BundleRepo repoDescriptor = null;

    private ExecutionEnvironmentProfileProvider profileProvider;

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

    public void setImportPackageStrategy(RequirementStrategy importPackageStrategy) {
        this.requirementStrategy = importPackageStrategy;
    }

    public void setImportPackageStrategy(String strategy) {
        setImportPackageStrategy(RequirementStrategy.valueOf(strategy));
    }

    public void add(ExecutionEnvironmentProfileProvider pp) {
        this.profileProvider = pp;
    }

    protected void setRepository(Repository repository) {
        this.repository = repository;
    }

    protected void setRepoDescriptor(BundleRepo repoDescriptor) {
        this.repoDescriptor = repoDescriptor;
    }

    protected void ensureInit() {
        if (repoDescriptor == null || repository == null) {
            init();
        }
    }

    abstract protected void init();

    public Repository getRepository() {
        ensureInit();
        return repository;
    }

    private BundleRepo getRepoDescriptor() {
        ensureInit();
        return repoDescriptor;
    }

    public ResolvedModuleRevision getDependency(DependencyDescriptor dd, ResolveData data)
            throws ParseException {
        DefaultModuleDescriptor md = getDependencyMD(dd, data);
        if (md == null) {
            // not found, so let's return the mrid resolved by a previous resolver
            return data.getCurrentResolvedModuleRevision();
        }
        Artifact mdar = new MDArtifact(md, "MANIFEST", "manifest", "MF");
        MetadataArtifactDownloadReport mdardr = new MetadataArtifactDownloadReport(mdar);
        mdardr.setDownloadStatus(DownloadStatus.SUCCESSFUL);
        return new ResolvedModuleRevision(this, this, md, mdardr);
    }

    private DefaultModuleDescriptor getDependencyMD(DependencyDescriptor dd, ResolveData data) {
        ModuleRevisionId mrid = dd.getDependencyRevisionId();

        String osgiAtt = mrid.getExtraAttribute(BundleInfoAdapter.EXTRA_ATTRIBUTE_NAME);
        Map/* <String, Set<BundleCapabilityAndLocation>> */bundleCapabilities = (Map) getRepoDescriptor()
                .getBundleByCapabilities().get(osgiAtt);
        if (bundleCapabilities == null) {
            Message.verbose("\t Not an OSGi dependency: " + mrid);
            return null;
        }

        String id = mrid.getName();
        Set/* <BundleCapabilityAndLocation> */bundleReferences = (Set) bundleCapabilities.get(id);
        if (bundleReferences == null || bundleReferences.isEmpty()) {
            Message.verbose("\t " + id + " not found.");
            return null;
        }

        List/* <BundleCandidate> */ret = new ArrayList/* <BundleCandidate> */();
        Iterator itBundle = bundleReferences.iterator();
        while (itBundle.hasNext()) {
            BundleCapabilityAndLocation bundleCapability = (BundleCapabilityAndLocation) itBundle
                    .next();
            BundleInfo bundleInfo = bundleCapability.getBundleInfo();
            if (!bundleCapability.getType().equals(BundleInfo.BUNDLE_TYPE)) {
                ModuleRevisionId foundMrid = ModuleRevisionId.newInstance("",
                    bundleInfo.getSymbolicName(), bundleInfo.getVersion().toString(),
                    BundleInfoAdapter.OSGI_BUNDLE);
                if (data.getVisitData(foundMrid) != null) {
                    // already resolved import, no need to go further
                    DefaultModuleDescriptor md = BundleInfoAdapter.toModuleDescriptor(bundleInfo,
                        profileProvider);
                    md.setPublicationDate(new Date(0));
                    return md;
                }
            }
            BundleCandidate candidate = new BundleCandidate();
            candidate.bundleInfo = bundleInfo;
            candidate.version = bundleCapability.getVersion().toString();
            ret.add(candidate);
        }

        DefaultModuleDescriptor found = selectResource(ret, mrid, data.getDate());
        if (found == null) {
            Message.debug("\t" + getName() + ": no resource found for " + mrid);
        }
        return found;
    }

    private class BundleCandidate implements ArtifactInfo {

        BundleInfo bundleInfo;

        String version;

        public long getLastModified() {
            return 0;
        }

        public String getRevision() {
            return version;
        }

    }

    public DefaultModuleDescriptor selectResource(List/* <BundleCandidate> */rress,
            ModuleRevisionId mrid, Date date) {
        VersionMatcher versionMatcher = getSettings().getVersionMatcher();

        List/* <BundleCandidate> */founds = new ArrayList/* <BundleCandidate> */();
        List/* <BundleCandidate> */sorted = getLatestStrategy().sort(
            (ArtifactInfo[]) rress.toArray(new BundleCandidate[rress.size()]));
        List/* <String> */rejected = new ArrayList/* <String> */();
        List/* <ModuleRevisionId> */foundBlacklisted = new ArrayList/* <ModuleRevisionId> */();
        IvyContext context = IvyContext.getContext();

        Iterator itBundle = sorted.iterator();
        while (itBundle.hasNext()) {
            BundleCandidate rres = (BundleCandidate) itBundle.next();
            if (filterNames(new ArrayList/* <String> */(Collections.singleton(rres.getRevision())))
                    .isEmpty()) {
                Message.debug("\t" + getName() + ": filtered by name: " + rres);
                continue;
            }
            if ((date != null && rres.getLastModified() > date.getTime())) {
                Message.verbose("\t" + getName() + ": too young: " + rres);
                rejected.add(rres.getRevision() + " (" + rres.getLastModified() + ")");
                continue;
            }
            ModuleRevisionId foundMrid = ModuleRevisionId.newInstance(mrid, rres.getRevision());

            ResolveData data = context.getResolveData();
            if (data != null && data.getReport() != null
                    && data.isBlacklisted(data.getReport().getConfiguration(), foundMrid)) {
                Message.debug("\t" + getName() + ": blacklisted: " + rres);
                rejected.add(rres.getRevision() + " (blacklisted)");
                foundBlacklisted.add(foundMrid);
                continue;
            }

            if (!versionMatcher.accept(mrid, foundMrid)) {
                Message.debug("\t" + getName() + ": rejected by version matcher: " + rres);
                rejected.add(rres.getRevision());
                continue;
            }
            if (versionMatcher.needModuleDescriptor(mrid, foundMrid)) {
                throw new IllegalStateException();
            } else {
                founds.add(rres);
            }
        }
        if (founds.isEmpty() && !rejected.isEmpty()) {
            logAttempt(rejected.toString());
        }
        if (founds.isEmpty() && !foundBlacklisted.isEmpty()) {
            // all acceptable versions have been blacklisted, this means that an unsolvable conflict
            // has been found
            DependencyDescriptor dd = context.getDependencyDescriptor();
            IvyNode parentNode = context.getResolveData().getNode(dd.getParentRevisionId());
            ConflictManager cm = parentNode.getConflictManager(mrid.getModuleId());
            cm.handleAllBlacklistedRevisions(dd, foundBlacklisted);
        }
        if (founds.isEmpty()) {
            return null;
        }

        BundleCandidate found = (BundleCandidate) founds.get(0);

        String osgiAtt = mrid.getExtraAttribute(BundleInfoAdapter.EXTRA_ATTRIBUTE_NAME);
        // for non bundle requirement : log the selected bundle
        if (!BundleInfo.BUNDLE_TYPE.equals(osgiAtt)) {
            // several candidates with different symbolic name : make an warning about the ambiguity
            if (founds.size() != 1) {
                // several candidates with different symbolic name ?
                Map/* <String, List<BundleCandidate>> */matching = new HashMap/*
                                                                               * <String,
                                                                               * List<BundleCandidate
                                                                               * >>
                                                                               */();
                Iterator itBundle2 = founds.iterator();
                while (itBundle2.hasNext()) {
                    BundleCandidate c = (BundleCandidate) itBundle2.next();
                    String name = c.bundleInfo.getSymbolicName();
                    List/* <BundleCandidate> */list = (List) matching.get(name);
                    if (list == null) {
                        list = new ArrayList/* <BundleCandidate> */();
                        matching.put(name, list);
                    }
                    list.add(c);
                }
                if (matching.keySet().size() != 1) {
                    if (requirementStrategy == RequirementStrategy.first) {
                        Message.warn("Ambiguity for the '" + osgiAtt + "' requirement "
                                + mrid.getName() + ";version=" + mrid.getRevision());
                        Iterator itMatching = matching.entrySet().iterator();
                        while (itMatching.hasNext()) {
                            Entry/* <String, List<BundleCandidate>> */entry = (Entry) itMatching
                                    .next();
                            Message.warn("\t" + entry.getKey());
                            Iterator itB = ((List) entry.getValue()).iterator();
                            while (itB.hasNext()) {
                                BundleCandidate c = (BundleCandidate) itB.next();
                                Message.warn("\t\t" + c.getRevision()
                                        + (found == c ? " (selected)" : ""));
                            }
                        }
                    } else if (requirementStrategy == RequirementStrategy.noambiguity) {
                        Message.error("Ambiguity for the '" + osgiAtt + "' requirement "
                                + mrid.getName() + ";version=" + mrid.getRevision());
                        Iterator itMatching = matching.entrySet().iterator();
                        while (itMatching.hasNext()) {
                            Entry/* <String, List<BundleCandidate>> */entry = (Entry) itMatching
                                    .next();
                            Message.error("\t" + entry.getKey());
                            Iterator itB = ((List) entry.getValue()).iterator();
                            while (itB.hasNext()) {
                                BundleCandidate c = (BundleCandidate) itB.next();
                                Message.error("\t\t" + c.getRevision()
                                        + (found == c ? " (best match)" : ""));
                            }
                        }
                        return null;
                    }
                }
            }
            Message.info("'" + osgiAtt + "' requirement " + mrid.getName() + ";version="
                    + mrid.getRevision() + " satisfied by " + found.bundleInfo.getSymbolicName()
                    + ";" + found.getRevision());
        }

        DefaultModuleDescriptor md = BundleInfoAdapter.toModuleDescriptor(found.bundleInfo,
            profileProvider);
        md.setPublicationDate(new Date(found.getLastModified()));
        return md;
    }

    public ResolvedResource findArtifactRef(Artifact artifact, Date date) {
        ModuleRevisionId mrid = artifact.getModuleRevisionId();
        try {
            return new ResolvedResource(getRepository().getResource(artifact.getUrl().getFile()),
                    artifact.getModuleRevisionId().getRevision());
        } catch (IOException e) {
            throw new RuntimeException(getName() + ": unable to get resource for " + mrid
                    + ": res=" + artifact.getName() + ": " + e.getMessage(), e);
        }
    }

    protected Collection/* <String> */filterNames(Collection/* <String> */names) {
        getSettings().filterIgnore(names);
        return names;
    }

    protected Collection findNames(Map tokenValues, String token) {
        if (BundleInfoAdapter.EXTRA_ATTRIBUTE_NAME.equals(token)) {
            return Arrays.asList(new String[] {BundleInfo.BUNDLE_TYPE, BundleInfo.PACKAGE_TYPE,
                    BundleInfo.SERVICE_TYPE});
        }

        String osgiAtt = (String) tokenValues.get(BundleInfoAdapter.EXTRA_ATTRIBUTE_NAME);

        Map/* <String, Set<BundleCapabilityAndLocation>> */bundleCapabilityMap = (Map) getRepoDescriptor()
                .getBundleByCapabilities().get(osgiAtt);
        if (bundleCapabilityMap == null || bundleCapabilityMap.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        if (IvyPatternHelper.ORGANISATION_KEY.equals(token)) {
            return Collections.singletonList("");
        }

        if (IvyPatternHelper.MODULE_KEY.equals(token)) {
            return bundleCapabilityMap.keySet();
        }

        if (IvyPatternHelper.REVISION_KEY.equals(token)) {
            String name = (String) tokenValues.get(IvyPatternHelper.MODULE_KEY);
            List/* <String> */versions = new ArrayList/* <String> */();
            Set/* <BundleCapabilityAndLocation> */bundleCapabilities = (Set) bundleCapabilityMap
                    .get(name);
            if (bundleCapabilities != null) {
                Iterator itBundle = bundleCapabilities.iterator();
                while (itBundle.hasNext()) {
                    BundleCapabilityAndLocation bundleCapability = (BundleCapabilityAndLocation) itBundle
                            .next();
                    versions.add(bundleCapability.getVersion().toString());
                }
            }
            return versions;
        }

        if (IvyPatternHelper.CONF_KEY.equals(token)) {
            String name = (String) tokenValues.get(IvyPatternHelper.MODULE_KEY);
            if (name == null) {
                return Collections.EMPTY_LIST;
            }
            if (osgiAtt.equals(BundleInfo.PACKAGE_TYPE)) {
                return Collections.singletonList(BundleInfoAdapter.CONF_USE_PREFIX + name);
            }
            Set/* <BundleCapabilityAndLocation> */bundleCapabilities = (Set) bundleCapabilityMap
                    .get(name);
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
            } catch (NumberFormatException e) {
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
            DefaultModuleDescriptor md = BundleInfoAdapter.toModuleDescriptor(
                found.getBundleInfo(), profileProvider);
            List/* <String> */confs = new ArrayList/* <String> */();
            Configuration[] configurations = md.getConfigurations();
            for (int i = 0; i < configurations.length; i++) {
                Configuration conf = configurations[i];
                confs.add(conf.getName());
            }
            return confs;
        }
        return Collections.EMPTY_LIST;
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

        tokenSet.remove(BundleInfoAdapter.EXTRA_ATTRIBUTE_NAME);
        String osgiAtt = (String) criteria.get(BundleInfoAdapter.EXTRA_ATTRIBUTE_NAME);
        if (osgiAtt == null) {
            Set/* <Map<String, String>> */tokenValues = new HashSet/* <Map<String, String>> */();
            Map/* <String, String> */newCriteria = new HashMap/* <String, String> */(criteria);
            newCriteria.put(BundleInfoAdapter.EXTRA_ATTRIBUTE_NAME, BundleInfo.BUNDLE_TYPE);
            tokenValues.addAll(listTokenValues(tokenSet, newCriteria));
            newCriteria = new HashMap/* <String, String> */(criteria);
            newCriteria.put(BundleInfoAdapter.EXTRA_ATTRIBUTE_NAME, BundleInfo.PACKAGE_TYPE);
            tokenValues.addAll(listTokenValues(tokenSet, newCriteria));
            newCriteria = new HashMap/* <String, String> */(criteria);
            newCriteria.put(BundleInfoAdapter.EXTRA_ATTRIBUTE_NAME, BundleInfo.SERVICE_TYPE);
            tokenValues.addAll(listTokenValues(tokenSet, newCriteria));
            return tokenValues;
        }
        values.put(BundleInfoAdapter.EXTRA_ATTRIBUTE_NAME, osgiAtt);

        Map/* <String, Set<BundleCapabilityAndLocation>> */bundleCapabilityMap = (Map) getRepoDescriptor()
                .getBundleByCapabilities().get(osgiAtt);
        if (bundleCapabilityMap == null || bundleCapabilityMap.isEmpty()) {
            return Collections.EMPTY_SET;
        }

        tokenSet.remove(IvyPatternHelper.ORGANISATION_KEY);
        String org = (String) criteria.get(IvyPatternHelper.ORGANISATION_KEY);
        if (org != null && org.length() != 0) {
            return Collections.EMPTY_SET;
        }
        values.put(IvyPatternHelper.ORGANISATION_KEY, "");

        tokenSet.remove(IvyPatternHelper.MODULE_KEY);
        String module = (String) criteria.get(IvyPatternHelper.MODULE_KEY);
        if (module == null) {
            Set/* <String> */names = bundleCapabilityMap.keySet();
            Set/* <Map<String, String>> */tokenValues = new HashSet/* <Map<String, String>> */();
            Iterator itNames = names.iterator();
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
            Set/* <BundleCapabilityAndLocation> */bundleCapabilities = (Set) bundleCapabilityMap
                    .get(module);
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
            if (osgiAtt.equals(BundleInfo.PACKAGE_TYPE)) {
                values.put(IvyPatternHelper.CONF_KEY, BundleInfoAdapter.CONF_USE_PREFIX + module);
                return Collections./* <Map<String, String>> */singleton(values);
            }
            Set/* <BundleCapabilityAndLocation> */bundleCapabilities = (Set) bundleCapabilityMap
                    .get(module);
            if (bundleCapabilities == null) {
                return Collections.EMPTY_SET;
            }
            Version v;
            try {
                v = new Version(rev);
            } catch (NumberFormatException e) {
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
            DefaultModuleDescriptor md = BundleInfoAdapter.toModuleDescriptor(
                found.getBundleInfo(), profileProvider);
            Configuration[] configurations = md.getConfigurations();
            for (int i = 0; i < configurations.length; i++) {
                Configuration c = configurations[i];
                Map/* <String, String> */newCriteria = new HashMap/* <String, String> */(criteria);
                newCriteria.put(IvyPatternHelper.CONF_KEY, c.getName());
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

    // useless methods that must not be called

    public ResolvedResource findIvyFileRef(DependencyDescriptor dd, ResolveData data) {
        throw new UnsupportedOperationException();
    }

}
