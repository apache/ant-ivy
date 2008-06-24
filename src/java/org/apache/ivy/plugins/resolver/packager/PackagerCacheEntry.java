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
package org.apache.ivy.plugins.resolver.packager;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.resolver.util.ResolvedResource;

/**
 * Represents one entry in the cache of a {@link PackagerResolver}.
 */
public class PackagerCacheEntry {

    private final ModuleRevisionId mr;
    private final File dir;
    private final File resourceCache;
    private final String resourceURL;
    private final boolean validate;
    private final boolean preserve;
    private final boolean verbose;
    private final boolean quiet;

    private boolean built;

    // CheckStyle:ParameterNumber OFF
    public PackagerCacheEntry(ModuleRevisionId mr, File rootDir,
      File resourceCache, String resourceURL, boolean validate,
      boolean preserve, boolean verbose, boolean quiet) {
        this.mr = mr;
        this.dir = getSubdir(rootDir, this.mr);
        this.resourceCache = resourceCache;
        this.resourceURL = resourceURL;
        this.validate = validate;
        this.preserve = preserve;
        this.verbose = verbose;
        this.quiet = quiet;
    }
    // CheckStyle:ParameterNumber ON

    /**
     * Attempt to build this entry.
     *
     * @param packagerXML packager XML input stream
     * @throws IllegalStateException if this entry has already been built
     */
    public synchronized void build(InputStream packagerXML) throws IOException {

        // Sanity check
        if (this.built) {
            throw new IllegalStateException("build in directory `"
              + this.dir + "' already completed");
        }

        // Remove work directory if it exists (e.g. left over from last time)
        if (this.dir.exists()) {
            if (!cleanup()) {
                throw new IOException("can't remove directory `" + this.dir + "'");
            }
        }

        // Create work directory
        if (!this.dir.mkdirs()) {
            throw new IOException("can't create directory `" + this.dir + "'");
        }

        // Write out packager XML
        saveFile("packager.xml", packagerXML);

        // Write packager XSLT
        saveFile("packager.xsl");

        // Write packager XSD
        saveFile("packager-1.0.xsd");

        // Write master ant build file
        saveFile("build.xml");

        // Create new process argument list
        ArrayList paramList = new ArrayList();
        paramList.add("ant");
        if (this.verbose) {
            paramList.add("-verbose");
        }
        if (this.quiet) {
            paramList.add("-quiet");
        }
        paramList.add("-Divy.packager.organisation=" + this.mr.getModuleId().getOrganisation());
        paramList.add("-Divy.packager.module=" + this.mr.getModuleId().getName());
        paramList.add("-Divy.packager.revision=" + this.mr.getRevision());
        paramList.add("-Divy.packager.branch=" + this.mr.getBranch());
        if (this.resourceCache != null) {
            paramList.add("-Divy.packager.resourceCache=" + this.resourceCache.getCanonicalPath());
        }
        if (this.resourceURL != null) {
            paramList.add("-Divy.packager.resourceURL=" + getResourceURL());
        }
        if (this.validate) {
            paramList.add("-Divy.packager.validate=true");
        }
        String[] params = (String[]) paramList.toArray(new String[paramList.size()]);

        // Run ant
        SubProcess proc = new SubProcess(params, null, this.dir);
        int result;
        try {
            result = proc.run();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (result != 0) {
            throw new IOException("build in directory `" + this.dir + "' failed");
        }
        this.built = true;
    }

    /**
     * Has this entry been successfully built?
     */
    public synchronized boolean isBuilt() {
        return this.built;
    }

    /**
     * Get a built artifact.
     *
     * @throws IllegalStateException if this entry's built has not
     *  (yet) completed successfully
     */
    public ResolvedResource getBuiltArtifact(Artifact artifact) {
        if (!this.built) {
            throw new IllegalStateException("build in directory `" + this.dir
              + "' has not yet successfully completed");
        }
        return new ResolvedResource(
          new BuiltFileResource(this.dir, artifact), this.mr.getRevision());
    }

    public synchronized boolean cleanup() {
        this.built = false;
        return PackagerResolver.deleteRecursive(this.dir);
    }

    protected void saveFile(String name, InputStream input) throws IOException {
        OutputStream out = new BufferedOutputStream(
          new FileOutputStream(new File(this.dir, name)));
        SubProcess.relayStream(input, out);
        input.close();
        out.close();
    }

    protected void saveFile(String name) throws IOException {
        InputStream input = getClass().getResourceAsStream(name);
        if (input == null) {
            throw new IOException("can't find resource `" + name + "'");
        }
        saveFile(name, input);
    }

    // @Override
    protected void finalize() throws Throwable {
        try {
            if (!this.preserve) {
                cleanup();
            }
        } finally {
            super.finalize();
        }
    }

    private String getResourceURL() {
        String baseURL = IvyPatternHelper.substitute(this.resourceURL, this.mr.getOrganisation(),
          this.mr.getName(), this.mr.getRevision(), null, null, null, null,
          this.mr.getAttributes());
        int slash = baseURL.lastIndexOf('/');
        if (slash != -1) {
            baseURL = baseURL.substring(0, slash + 1);
        }
        return baseURL;
    }

    private static File getSubdir(File rootDir, ModuleRevisionId mr) {
        return new File(rootDir,
          mr.getOrganisation() + File.separatorChar
          + mr.getName() + File.separatorChar
          + mr.getRevision());
    }
}

