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
package org.apache.ivy.core.retrieve;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.LogOptions;
import org.apache.ivy.core.cache.ResolutionCacheManager;
import org.apache.ivy.core.event.EventManager;
import org.apache.ivy.core.event.retrieve.EndRetrieveArtifactEvent;
import org.apache.ivy.core.event.retrieve.EndRetrieveEvent;
import org.apache.ivy.core.event.retrieve.StartRetrieveArtifactEvent;
import org.apache.ivy.core.event.retrieve.StartRetrieveEvent;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.plugins.report.XmlReportParser;
import org.apache.ivy.util.FileUtil;
import org.apache.ivy.util.Message;

public class RetrieveEngine {
    private static final int KILO = 1024;

    private RetrieveEngineSettings settings;

    private EventManager eventManager;

    public RetrieveEngine(RetrieveEngineSettings settings, EventManager eventManager) {
        this.settings = settings;
        this.eventManager = eventManager;
    }

    /**
     * example of destFilePattern : - lib/[organisation]/[module]/[artifact]-[revision].[type] -
     * lib/[artifact].[type] : flatten with no revision moduleId is used with confs and
     * localCacheDirectory to determine an ivy report file, used as input for the copy If such a
     * file does not exist for any conf (resolve has not been called before ?) then an
     * IllegalStateException is thrown and nothing is copied.
     * 
     * @deprecated Use
     *             {@link #retrieve(org.apache.ivy.core.module.id.ModuleRevisionId, RetrieveOptions)}
     *             instead
     */
    @Deprecated
    public int retrieve(ModuleRevisionId mrid, String destFilePattern, RetrieveOptions options)
            throws IOException {
        RetrieveOptions retieveOptions = new RetrieveOptions(options);
        retieveOptions.setDestArtifactPattern(destFilePattern);

        RetrieveReport result = retrieve(mrid, retieveOptions);
        return result.getNbrArtifactsCopied();
    }

    public RetrieveReport retrieve(ModuleRevisionId mrid, RetrieveOptions options)
            throws IOException {
        RetrieveReport report = new RetrieveReport();

        ModuleId moduleId = mrid.getModuleId();
        if (LogOptions.LOG_DEFAULT.equals(options.getLog())) {
            Message.info(":: retrieving :: " + moduleId + (options.isSync() ? " [sync]" : ""));
        } else {
            Message.verbose(":: retrieving :: " + moduleId + (options.isSync() ? " [sync]" : ""));
        }
        Message.verbose("\tcheckUpToDate=" + settings.isCheckUpToDate());
        long start = System.currentTimeMillis();

        String destFilePattern = IvyPatternHelper.substituteVariables(
            options.getDestArtifactPattern(), settings.getVariables());
        String destIvyPattern = IvyPatternHelper.substituteVariables(options.getDestIvyPattern(),
            settings.getVariables());

        String[] confs = getConfs(mrid, options);
        if (LogOptions.LOG_DEFAULT.equals(options.getLog())) {
            Message.info("\tconfs: " + Arrays.asList(confs));
        } else {
            Message.verbose("\tconfs: " + Arrays.asList(confs));
        }
        if (this.eventManager != null) {
            this.eventManager.fireIvyEvent(new StartRetrieveEvent(mrid, confs, options));
        }

        try {
            Map/* <File, File> */destToSrcMap = null;
            // Map<ArtifactDownloadReport, Set<String>>
            Map artifactsToCopy = determineArtifactsToCopy(mrid, destFilePattern, options);
            File fileRetrieveRoot = settings.resolveFile(IvyPatternHelper
                    .getTokenRoot(destFilePattern));
            report.setRetrieveRoot(fileRetrieveRoot);
            File ivyRetrieveRoot = destIvyPattern == null ? null : settings
                    .resolveFile(IvyPatternHelper.getTokenRoot(destIvyPattern));
            Collection targetArtifactsStructure = new HashSet(); // Set(File) set of all paths
            // which should be present at
            // then end of retrieve (useful
            // for sync)
            Collection targetIvysStructure = new HashSet(); // same for ivy files

            if (options.isMakeSymlinksInMass()) {
                // The HashMap is of "destToSrc" because src could go two places, but dest can only
                // come from one
                destToSrcMap = new HashMap();
            }

            // do retrieve
            long totalCopiedSize = 0;
            for (Iterator iter = artifactsToCopy.keySet().iterator(); iter.hasNext();) {
                ArtifactDownloadReport artifact = (ArtifactDownloadReport) iter.next();
                File archive = artifact.getLocalFile();
                if (artifact.getUnpackedLocalFile() != null) {
                    archive = artifact.getUnpackedLocalFile();
                }
                if (archive == null) {
                    Message.verbose("\tno local file available for " + artifact + ": skipping");
                    continue;
                }
                Set dest = (Set) artifactsToCopy.get(artifact);
                Message.verbose("\tretrieving " + archive);
                for (Iterator it2 = dest.iterator(); it2.hasNext();) {
                    IvyContext.getContext().checkInterrupted();
                    File destFile = settings.resolveFile((String) it2.next());
                    if (!settings.isCheckUpToDate() || !upToDate(archive, destFile, options)) {
                        Message.verbose("\t\tto " + destFile);
                        if (this.eventManager != null) {
                            // There is no unitary event for the mass sym linking.
                            // skip the event declaration.
                            if (!options.isMakeSymlinksInMass()) {
                                this.eventManager.fireIvyEvent(new StartRetrieveArtifactEvent(
                                        artifact, destFile));
                            }
                        }
                        if (options.isMakeSymlinksInMass()) {
                            if (FileUtil.prepareCopy(archive, destFile, true)) {
                                destToSrcMap.put(destFile, archive);
                            }
                        } else if (options.isMakeSymlinks()) {
                            FileUtil.symlink(archive, destFile, null, true);
                        } else {
                            FileUtil.copy(archive, destFile, null, true);
                        }
                        if (this.eventManager != null) {
                            // There is no unitary event for the mass sym linking.
                            // skip the event declaration.
                            if (!options.isMakeSymlinksInMass()) {
                                this.eventManager.fireIvyEvent(new EndRetrieveArtifactEvent(
                                        artifact, destFile));
                            }
                        }
                        totalCopiedSize += FileUtil.getFileLength(destFile);
                        report.addCopiedFile(destFile, artifact);
                    } else {
                        Message.verbose("\t\tto " + destFile + " [NOT REQUIRED]");
                        report.addUpToDateFile(destFile, artifact);
                    }

                    if ("ivy".equals(artifact.getType())) {
                        targetIvysStructure
                                .addAll(FileUtil.getPathFiles(ivyRetrieveRoot, destFile));
                    } else {
                        Iterator destFiles = FileUtil.listAll(destFile, Collections.EMPTY_LIST)
                                .iterator();
                        while (destFiles.hasNext()) {
                            targetArtifactsStructure.addAll(FileUtil.getPathFiles(fileRetrieveRoot,
                                (File) destFiles.next()));
                        }
                    }
                }
            }

            if (options.isMakeSymlinksInMass()) {
                Message.verbose("\tMass symlinking " + destToSrcMap.size() + " files");
                FileUtil.symlinkInMass(destToSrcMap, true);
            }

            if (options.isSync()) {
                Message.verbose("\tsyncing...");

                String[] ignorableFilenames = settings.getIgnorableFilenames();
                Collection ignoreList = Arrays.asList(ignorableFilenames);

                Collection existingArtifacts = FileUtil.listAll(fileRetrieveRoot, ignoreList);
                Collection existingIvys = ivyRetrieveRoot == null ? null : FileUtil.listAll(
                    ivyRetrieveRoot, ignoreList);

                if (fileRetrieveRoot.equals(ivyRetrieveRoot)) {
                    Collection target = targetArtifactsStructure;
                    target.addAll(targetIvysStructure);
                    Collection existing = existingArtifacts;
                    existing.addAll(existingIvys);
                    sync(target, existing);
                } else {
                    sync(targetArtifactsStructure, existingArtifacts);
                    if (existingIvys != null) {
                        sync(targetIvysStructure, existingIvys);
                    }
                }
            }
            long elapsedTime = System.currentTimeMillis() - start;
            String msg = "\t"
                    + report.getNbrArtifactsCopied()
                    + " artifacts copied"
                    + (settings.isCheckUpToDate() ? (", " + report.getNbrArtifactsUpToDate() + " already retrieved")
                            : "") + " (" + (totalCopiedSize / KILO) + "kB/" + elapsedTime + "ms)";
            if (LogOptions.LOG_DEFAULT.equals(options.getLog())) {
                Message.info(msg);
            } else {
                Message.verbose(msg);
            }
            Message.verbose("\tretrieve done (" + (elapsedTime) + "ms)");
            if (this.eventManager != null) {
                this.eventManager.fireIvyEvent(new EndRetrieveEvent(mrid, confs, elapsedTime,
                        report.getNbrArtifactsCopied(), report.getNbrArtifactsUpToDate(),
                        totalCopiedSize, options));
            }

            return report;
        } catch (Exception ex) {
            throw new RuntimeException("problem during retrieve of " + moduleId + ": " + ex, ex);
        }
    }

    private String[] getConfs(ModuleRevisionId mrid, RetrieveOptions options) throws IOException {
        String[] confs = options.getConfs();
        if (confs == null || (confs.length == 1 && "*".equals(confs[0]))) {
            try {
                ModuleDescriptor md = getCache().getResolvedModuleDescriptor(mrid);
                Message.verbose("no explicit confs given for retrieve, using ivy file: "
                        + md.getResource().getName());
                confs = md.getConfigurationsNames();
                options.setConfs(confs);
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                IOException ioex = new IOException(e.getMessage());
                ioex.initCause(e);
                throw ioex;
            }
        }
        return confs;
    }

    private ResolutionCacheManager getCache() {
        return settings.getResolutionCacheManager();
    }

    private void sync(Collection target, Collection existing) {
        Collection toRemove = new HashSet();
        for (Iterator iter = existing.iterator(); iter.hasNext();) {
            File file = (File) iter.next();
            toRemove.add(file.getAbsoluteFile());
        }
        for (Iterator iter = target.iterator(); iter.hasNext();) {
            File file = (File) iter.next();
            toRemove.remove(file.getAbsoluteFile());
        }
        for (Iterator iter = toRemove.iterator(); iter.hasNext();) {
            File file = (File) iter.next();
            if (file.exists()) {
                Message.verbose("\t\tdeleting " + file);
                FileUtil.forceDelete(file);
            }
        }
    }

    /**
     * @return Map<ArtifactDownloadReport, Set<String>>
     */
    public Map determineArtifactsToCopy(ModuleRevisionId mrid, String destFilePattern,
            RetrieveOptions options) throws ParseException, IOException {
        ModuleId moduleId = mrid.getModuleId();

        if (options.getResolveId() == null) {
            options.setResolveId(ResolveOptions.getDefaultResolveId(moduleId));
        }

        ResolutionCacheManager cacheManager = getCache();
        String[] confs = getConfs(mrid, options);
        String destIvyPattern = IvyPatternHelper.substituteVariables(options.getDestIvyPattern(),
            settings.getVariables());

        // find what we must retrieve where

        // ArtifactDownloadReport source -> Set (String copyDestAbsolutePath)
        final Map artifactsToCopy = new HashMap();
        // String copyDestAbsolutePath -> Set (ArtifactRevisionId source)
        final Map conflictsMap = new HashMap();
        // String copyDestAbsolutePath -> Set (ArtifactDownloadReport source)
        final Map conflictsReportsMap = new HashMap();
        // String copyDestAbsolutePath -> Set (String conf)
        final Map conflictsConfMap = new HashMap();

        XmlReportParser parser = new XmlReportParser();
        for (int i = 0; i < confs.length; i++) {
            final String conf = confs[i];

            File report = cacheManager.getConfigurationResolveReportInCache(options.getResolveId(),
                conf);
            parser.parse(report);

            Collection artifacts = new ArrayList(Arrays.asList(parser.getArtifactReports()));
            if (destIvyPattern != null) {
                ModuleRevisionId[] mrids = parser.getRealDependencyRevisionIds();
                for (int j = 0; j < mrids.length; j++) {
                    artifacts.add(parser.getMetadataArtifactReport(mrids[j]));
                }
            }
            for (Iterator iter = artifacts.iterator(); iter.hasNext();) {
                ArtifactDownloadReport adr = (ArtifactDownloadReport) iter.next();

                Artifact artifact = adr.getArtifact();
                String ext = artifact.getExt();
                if (adr.getUnpackedLocalFile() != null) {
                    ext = "";
                }

                String destPattern = "ivy".equals(adr.getType()) ? destIvyPattern : destFilePattern;

                if (!"ivy".equals(adr.getType())
                        && !options.getArtifactFilter().accept(adr.getArtifact())) {
                    continue; // skip this artifact, the filter didn't accept it!
                }

                ModuleRevisionId aMrid = artifact.getModuleRevisionId();
                String destFileName = IvyPatternHelper.substitute(destPattern,
                    aMrid.getOrganisation(), aMrid.getName(), aMrid.getBranch(),
                    aMrid.getRevision(), artifact.getName(), artifact.getType(), ext, conf,
                    adr.getArtifactOrigin(), aMrid.getQualifiedExtraAttributes(),
                    artifact.getQualifiedExtraAttributes());
                Set dest = (Set) artifactsToCopy.get(adr);
                if (dest == null) {
                    dest = new HashSet();
                    artifactsToCopy.put(adr, dest);
                }
                String copyDest = settings.resolveFile(destFileName).getAbsolutePath();

                String[] destinations = new String[] {copyDest};
                if (options.getMapper() != null) {
                    destinations = options.getMapper().mapFileName(copyDest);
                }

                for (int j = 0; j < destinations.length; j++) {
                    dest.add(destinations[j]);

                    Set conflicts = (Set) conflictsMap.get(destinations[j]);
                    Set conflictsReports = (Set) conflictsReportsMap.get(destinations[j]);
                    Set conflictsConf = (Set) conflictsConfMap.get(destinations[j]);
                    if (conflicts == null) {
                        conflicts = new HashSet();
                        conflictsMap.put(destinations[j], conflicts);
                    }
                    if (conflictsReports == null) {
                        conflictsReports = new HashSet();
                        conflictsReportsMap.put(destinations[j], conflictsReports);
                    }
                    if (conflictsConf == null) {
                        conflictsConf = new HashSet();
                        conflictsConfMap.put(destinations[j], conflictsConf);
                    }
                    if (conflicts.add(artifact.getId())) {
                        conflictsReports.add(adr);
                        conflictsConf.add(conf);
                    }
                }
            }
        }

        // resolve conflicts if any
        for (Iterator iter = conflictsMap.keySet().iterator(); iter.hasNext();) {
            String copyDest = (String) iter.next();
            Set artifacts = (Set) conflictsMap.get(copyDest);
            Set conflictsConfs = (Set) conflictsConfMap.get(copyDest);
            if (artifacts.size() > 1) {
                List artifactsList = new ArrayList((Collection) conflictsReportsMap.get(copyDest));
                // conflicts battle is resolved by a sort using a conflict resolving policy
                // comparator which consider as greater a winning artifact
                Collections.sort(artifactsList, getConflictResolvingPolicy());

                // after the sort, the winning artifact is the greatest one, i.e. the last one
                // we fail if different artifacts of the same module are mapped to the same file
                ArtifactDownloadReport winner = (ArtifactDownloadReport) artifactsList
                        .get(artifactsList.size() - 1);
                ModuleRevisionId winnerMD = winner.getArtifact().getModuleRevisionId();
                for (int i = artifactsList.size() - 2; i >= 0; i--) {
                    ArtifactDownloadReport current = (ArtifactDownloadReport) artifactsList.get(i);
                    if (winnerMD.equals(current.getArtifact().getModuleRevisionId())) {
                        throw new RuntimeException("Multiple artifacts of the module " + winnerMD
                                + " are retrieved to the same file! Update the retrieve pattern "
                                + " to fix this error.");
                    }
                }

                Message.info("\tconflict on " + copyDest + " in " + conflictsConfs + ": "
                        + winnerMD.getRevision() + " won");

                // we now iterate over the list beginning with the artifact preceding the winner,
                // and going backward to the least artifact
                for (int i = artifactsList.size() - 2; i >= 0; i--) {
                    ArtifactDownloadReport looser = (ArtifactDownloadReport) artifactsList.get(i);
                    Message.verbose("\t\tremoving conflict looser artifact: "
                            + looser.getArtifact());
                    // for each loser, we remove the pair (loser - copyDest) in the artifactsToCopy
                    // map
                    Set dest = (Set) artifactsToCopy.get(looser);
                    dest.remove(copyDest);
                    if (dest.isEmpty()) {
                        artifactsToCopy.remove(looser);
                    }
                }
            }
        }
        return artifactsToCopy;
    }

    private boolean upToDate(File source, File target, RetrieveOptions options) {
        if (!target.exists()) {
            return false;
        }

        String overwriteMode = options.getOverwriteMode();
        if (RetrieveOptions.OVERWRITEMODE_ALWAYS.equals(overwriteMode)) {
            return false;
        }

        if (RetrieveOptions.OVERWRITEMODE_NEVER.equals(overwriteMode)) {
            return true;
        }

        if (RetrieveOptions.OVERWRITEMODE_NEWER.equals(overwriteMode)) {
            return source.lastModified() <= target.lastModified();
        }

        if (RetrieveOptions.OVERWRITEMODE_DIFFERENT.equals(overwriteMode)) {
            return source.lastModified() == target.lastModified();
        }

        // unknown, so just to be sure
        return false;
    }

    /**
     * The returned comparator should consider greater the artifact which gains the conflict battle.
     * This is used only during retrieve... prefer resolve conflict manager to resolve conflicts.
     * 
     * @return
     */
    private Comparator getConflictResolvingPolicy() {
        return new Comparator() {
            // younger conflict resolving policy
            public int compare(Object o1, Object o2) {
                Artifact a1 = ((ArtifactDownloadReport) o1).getArtifact();
                Artifact a2 = ((ArtifactDownloadReport) o2).getArtifact();
                if (a1.getPublicationDate().after(a2.getPublicationDate())) {
                    // a1 is after a2 <=> a1 is younger than a2 <=> a1 wins the conflict battle
                    return +1;
                } else if (a1.getPublicationDate().before(a2.getPublicationDate())) {
                    // a1 is before a2 <=> a2 is younger than a1 <=> a2 wins the conflict battle
                    return -1;
                } else {
                    return 0;
                }
            }
        };
    }

}
