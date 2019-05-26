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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.PatternSet.NameEntry;
import org.apache.tools.ant.types.Resource;

/**
 * Creates an ant fileset consisting in all artifacts found during a resolve. Note that this task
 * is not compatible with the useOrigin mode.
 */
public class IvyCacheFileset extends IvyCacheTask {
    private String setid;

    public String getSetid() {
        return setid;
    }

    public void setSetid(String id) {
        setid = id;
    }

    public void setUseOrigin(boolean useOrigin) {
        if (useOrigin) {
            throw new UnsupportedOperationException(
                    "the cachefileset task does not support the useOrigin mode, since filesets "
                            + "require to have only one root directory. Please use the the "
                            + "cachepath task instead");
        }
    }

    public void doExecute() throws BuildException {
        prepareAndCheck();
        if (setid == null) {
            throw new BuildException("setid is required in ivy cachefileset");
        }
        try {
            final List<ArtifactDownloadReport> artifactDownloadReports = getArtifactReports();
            if (artifactDownloadReports.isEmpty()) {
                // generate an empty fileset
                final FileSet emptyFileSet = new EmptyFileSet();
                emptyFileSet.setProject(getProject());
                getProject().addReference(setid, emptyFileSet);
                return;
            }
            // find a common base dir of the resolved artifacts
            final File baseDir = this.requireCommonBaseDir(artifactDownloadReports);
            final FileSet fileset = new FileSet();
            fileset.setDir(baseDir);
            fileset.setProject(getProject());
            // enroll each of the artifact files into the fileset
            for (final ArtifactDownloadReport artifactDownloadReport : artifactDownloadReports) {
                if (artifactDownloadReport.getLocalFile() == null) {
                    continue;
                }
                final NameEntry ne = fileset.createInclude();
                ne.setName(getPath(baseDir, artifactDownloadReport.getLocalFile()));
            }
            getProject().addReference(setid, fileset);
        } catch (Exception ex) {
            throw new BuildException("impossible to build ivy cache fileset: " + ex, ex);
        }
    }

    /**
     * Returns a common base directory, determined from the
     * {@link ArtifactDownloadReport#getLocalFile() local files} of the passed
     * <code>artifactDownloadReports</code>. If no common base directory can be determined, this
     * method throws a {@link BuildException}
     *
     * @param artifactDownloadReports The artifact download reports for which the common base
     *                                directory of the artifacts has to be determined
     * @return File
     */
    File requireCommonBaseDir(final List<ArtifactDownloadReport> artifactDownloadReports) {
        File base = null;
        for (final ArtifactDownloadReport artifactDownloadReport : artifactDownloadReports) {
            if (artifactDownloadReport.getLocalFile() == null) {
                continue;
            }
            if (base == null) {
                // use the parent dir of the artifact as the base
                base = artifactDownloadReport.getLocalFile().getParentFile().getAbsoluteFile();
            } else {
                // try and find a common base directory between the current base
                // directory and the artifact's file
                base = getBaseDir(base, artifactDownloadReport.getLocalFile());
                if (base == null) {
                    // fail fast - we couldn't determine a common base directory, throw an error
                    throw new BuildException("Cannot find a common base directory, from resolved "
                            + "artifacts, for generating a cache fileset");
                }
            }
        }
        if (base == null) {
            // finally, we couldn't determine a common base directory, throw an error
            throw new BuildException("Cannot find a common base directory, from resolved "
                    + "artifacts, for generating a cache fileset");
        }
        return base;
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
            beginIndex++; // skip the separator char as well
        }

        return file.getAbsolutePath().substring(beginIndex);
    }

    /**
     * Returns the common base directory between the passed <code>file1</code> and
     * <code>file2</code>.
     * <p>
     * The returned base directory will be a parent of both the <code>file1</code> and
     * <code>file2</code> or it will be <code>null</code>.
     * </p>
     *
     * @param file1
     *         One of the files, for which the common base directory is being sought, may be null.
     * @param file2
     *         The other file for which the common base directory should be returned, may be null.
     * @return the common base directory between a <code>file1</code> and <code>file2</code>.
     *         Returns null if no common base directory could be determined or if either
     *         <code>file1</code> or <code>file2</code> is null
     */
    File getBaseDir(final File file1, final File file2) {
        if (file1 == null || file2 == null) {
            return null;
        }
        final Iterator<File> file1Parents = getParents(file1).iterator();
        final Iterator<File> file2Parents = getParents(file2.getAbsoluteFile()).iterator();
        File result = null;
        while (file1Parents.hasNext() && file2Parents.hasNext()) {
            File next = file1Parents.next();
            if (next.equals(file2Parents.next())) {
                result = next;
            } else {
                break;
            }
        }
        return result;
    }

    /**
     * @return a list of files, starting with the root and ending with the file itself
     */
    private LinkedList<File> getParents(File file) {
        LinkedList<File> r = new LinkedList<>();
        while (file != null) {
            r.addFirst(file);
            file = file.getParentFile();
        }
        return r;
    }

    private static class EmptyFileSet extends FileSet {

        private DirectoryScanner ds = new EmptyDirectoryScanner();

        public Iterator<Resource> iterator() {
            return new EmptyIterator<>();
        }

        public Object clone() {
            return new EmptyFileSet();
        }

        public int size() {
            return 0;
        }

        public DirectoryScanner getDirectoryScanner(Project project) {
            return ds;
        }
    }

    private static class EmptyIterator<T> implements Iterator<T> {

        public boolean hasNext() {
            return false;
        }

        public T next() {
            throw new NoSuchElementException("EmptyFileSet Iterator");
        }

        public void remove() {
            throw new IllegalStateException("EmptyFileSet Iterator");
        }

    }

    private static class EmptyDirectoryScanner extends DirectoryScanner {

        public String[] getIncludedFiles() {
            return new String[0];
        }

    }
}
