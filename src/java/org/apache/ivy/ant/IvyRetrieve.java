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

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import org.apache.ivy.core.LogOptions;
import org.apache.ivy.core.retrieve.RetrieveOptions;
import org.apache.ivy.core.retrieve.RetrieveReport;
import org.apache.ivy.util.filter.Filter;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Mapper;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.PatternSet;
import org.apache.tools.ant.util.FileNameMapper;

/**
 * This task allow to retrieve dependencies from the cache to a local directory like a lib dir.
 */
public class IvyRetrieve extends IvyPostResolveTask {

    private static final Collection<String> OVERWRITEMODE_VALUES = Arrays.asList(
        RetrieveOptions.OVERWRITEMODE_ALWAYS, RetrieveOptions.OVERWRITEMODE_NEVER,
        RetrieveOptions.OVERWRITEMODE_NEWER, RetrieveOptions.OVERWRITEMODE_DIFFERENT);

    private String pattern;

    private String ivypattern = null;

    private boolean sync = false;

    private boolean symlink = false;

    private boolean symlinkmass = false;

    private String overwriteMode = RetrieveOptions.OVERWRITEMODE_NEWER;

    private String pathId = null;

    private String setId = null;

    private Mapper mapper = null;

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getPathId() {
        return pathId;
    }

    public void setPathId(String pathId) {
        this.pathId = pathId;
    }

    public String getSetId() {
        return setId;
    }

    public void setSetId(String setId) {
        this.setId = setId;
    }

    @Override
    public void doExecute() throws BuildException {
        prepareAndCheck();

        if (!getAllowedLogOptions().contains(getLog())) {
            throw new BuildException("invalid option for 'log': " + getLog()
                    + ". Available options are " + getAllowedLogOptions());
        }

        pattern = getProperty(pattern, getSettings(), "ivy.retrieve.pattern");
        try {
            Filter artifactFilter = getArtifactFilter();
            RetrieveReport report = getIvyInstance().retrieve(
                getResolvedMrid(),
                ((RetrieveOptions) new RetrieveOptions().setLog(getLog()))
                        .setConfs(splitConfs(getConf())).setDestArtifactPattern(pattern)
                        .setDestIvyPattern(ivypattern).setArtifactFilter(artifactFilter)
                        .setSync(sync).setOverwriteMode(getOverwriteMode())
                        .setUseOrigin(isUseOrigin()).setMakeSymlinks(symlink)
                        .setMakeSymlinksInMass(symlinkmass).setResolveId(getResolveId())
                        .setMapper(mapper == null ? null : new MapperAdapter(mapper)));

            int targetsCopied = report.getNbrArtifactsCopied();
            boolean haveTargetsBeenCopied = targetsCopied > 0;
            getProject().setProperty("ivy.nb.targets.copied", String.valueOf(targetsCopied));
            getProject().setProperty("ivy.targets.copied", String.valueOf(haveTargetsBeenCopied));

            if (getPathId() != null) {
                Path path = new Path(getProject());
                getProject().addReference(getPathId(), path);

                for (File file : report.getRetrievedFiles()) {
                    path.createPathElement().setLocation(file);
                }
            }

            if (getSetId() != null) {
                FileSet fileset = new FileSet();
                fileset.setProject(getProject());
                getProject().addReference(getSetId(), fileset);

                fileset.setDir(report.getRetrieveRoot());

                for (File file : report.getRetrievedFiles()) {
                    PatternSet.NameEntry ne = fileset.createInclude();
                    ne.setName(getPath(report.getRetrieveRoot(), file));
                }
            }
        } catch (Exception ex) {
            throw new BuildException("impossible to ivy retrieve: " + ex, ex);
        }
    }

    protected Collection<String> getAllowedLogOptions() {
        return Arrays.asList(new String[] {LogOptions.LOG_DEFAULT, LogOptions.LOG_DOWNLOAD_ONLY,
                LogOptions.LOG_QUIET});
    }

    public String getIvypattern() {
        return ivypattern;
    }

    public void setIvypattern(String ivypattern) {
        this.ivypattern = ivypattern;
    }

    public boolean isSync() {
        return sync;
    }

    public void setSync(boolean sync) {
        this.sync = sync;
    }

    /**
     * Option to create symlinks instead of copying.
     */
    public void setSymlink(boolean symlink) {
        this.symlink = symlink;
    }

    /**
     * Option to create symlinks in one mass action, instead of separately.
     */
    public void setSymlinkmass(boolean symlinkmass) {
        this.symlinkmass = symlinkmass;
    }

    public void setOverwriteMode(String overwriteMode) {
        if (!OVERWRITEMODE_VALUES.contains(overwriteMode)) {
            throw new IllegalArgumentException("invalid overwriteMode value '" + overwriteMode
                    + "'. " + "Valid values are " + OVERWRITEMODE_VALUES);
        }
        this.overwriteMode = overwriteMode;
    }

    public String getOverwriteMode() {
        return overwriteMode;
    }

    /**
     * Add a mapper to convert the file names.
     * 
     * @param mapper
     *            a <code>Mapper</code> value.
     */
    public void addMapper(Mapper mapper) {
        if (this.mapper != null) {
            throw new IllegalArgumentException("Cannot define more than one mapper");
        }
        this.mapper = mapper;
    }

    /**
     * Add a nested filenamemapper.
     * 
     * @param fileNameMapper
     *            the mapper to add.
     */
    public void add(FileNameMapper fileNameMapper) {
        Mapper m = new Mapper(getProject());
        m.add(fileNameMapper);
        addMapper(m);
    }

    /**
     * Returns the path of the file relative to the given base directory.
     * 
     * @param base
     *            the parent directory to which the file must be evaluated.
     * @param file
     *            the file for which the path should be returned
     * @return the path of the file relative to the given base directory.
     */
    private String getPath(File base, File file) {
        String absoluteBasePath = base.getAbsolutePath();

        int beginIndex = absoluteBasePath.length();

        // checks if the basePath ends with the file separator (which can for instance
        // happen if the basePath is the root on unix)
        if (!absoluteBasePath.endsWith(File.separator)) {
            beginIndex++; // skip the seperator char as well
        }

        return file.getAbsolutePath().substring(beginIndex);
    }

}
