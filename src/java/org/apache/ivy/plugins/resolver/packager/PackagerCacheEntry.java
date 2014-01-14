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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.plugins.resolver.util.ResolvedResource;
import org.apache.ivy.util.FileUtil;
import org.apache.ivy.util.Message;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildLogger;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;

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

    private final boolean restricted;

    private final boolean verbose;

    private final boolean quiet;

    private boolean built;

    // CheckStyle:ParameterNumber OFF
    public PackagerCacheEntry(ModuleRevisionId mr, File rootDir, File resourceCache,
            String resourceURL, boolean validate, boolean preserve, boolean restricted,
            boolean verbose, boolean quiet) {
        this.mr = mr;
        this.dir = getSubdir(rootDir, this.mr);
        this.resourceCache = resourceCache;
        this.resourceURL = resourceURL;
        this.validate = validate;
        this.preserve = preserve;
        this.restricted = restricted;
        this.verbose = verbose;
        this.quiet = quiet;
    }

    // CheckStyle:ParameterNumber ON

    /**
     * Attempt to build this entry.
     * 
     * @param packagerResource
     *            packager metadata resource
     * @param properties
     *            a map of properties to pass to the child Ant build responsible for dependency
     *            packaging
     * 
     * @throws IllegalStateException
     *             if this entry has already been built
     */
    public synchronized void build(Resource packagerResource, Map properties) throws IOException {
        // Sanity check
        if (this.built) {
            throw new IllegalStateException("build in directory `" + this.dir
                    + "' already completed");
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
        InputStream packagerXML = packagerResource.openStream();
        saveFile("packager.xml", packagerXML);

        // Write packager XSLT
        saveFile("packager.xsl");

        // Write packager XSD
        saveFile("packager-1.0.xsd");

        // Write master Ant build file
        saveFile("build.xml");

        // Execute the Ant build file
        Project project = new Project();
        project.init();
        project.setUserProperty("ant.file", new File(dir, "build.xml").getAbsolutePath());
        ProjectHelper.configureProject(project, new File(dir, "build.xml"));
        project.setBaseDir(dir);

        // Configure logging verbosity
        BuildLogger logger = new DefaultLogger();
        logger.setMessageOutputLevel(this.verbose ? Project.MSG_VERBOSE
                : this.quiet ? Project.MSG_WARN : Project.MSG_INFO);
        logger.setOutputPrintStream(System.out);
        logger.setErrorPrintStream(System.err);
        project.addBuildListener(logger);

        // Set properties
        project.setUserProperty("ivy.packager.organisation", ""
                + this.mr.getModuleId().getOrganisation());
        project.setUserProperty("ivy.packager.module", "" + this.mr.getModuleId().getName());
        project.setUserProperty("ivy.packager.revision", "" + this.mr.getRevision());
        project.setUserProperty("ivy.packager.branch", "" + this.mr.getBranch());
        if (this.resourceCache != null) {
            project.setUserProperty("ivy.packager.resourceCache",
                "" + this.resourceCache.getCanonicalPath());
        }
        if (this.resourceURL != null) {
            project.setUserProperty("ivy.packager.resourceURL", "" + getResourceURL());
        }
        if (this.validate) {
            project.setUserProperty("ivy.packager.validate", "true");
        }
        project.setUserProperty("ivy.packager.restricted", "" + this.restricted);
        project.setUserProperty("ivy.packager.quiet", String.valueOf(quiet));
        if (properties != null) {
            for (Iterator it = properties.entrySet().iterator(); it.hasNext();) {
                Entry entry = (Entry) it.next();
                project.setUserProperty((String) entry.getKey(), (String) entry.getValue());
            }
        }

        // Execute task
        Message.verbose("performing packager resolver build in " + this.dir);
        try {
            project.executeTarget("build");
            this.built = true;
        } catch (BuildException e) {
            Message.verbose("packager resolver build failed: " + e);
            throw e;
        }
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
     * @throws IllegalStateException
     *             if this entry's built has not (yet) completed successfully
     */
    public ResolvedResource getBuiltArtifact(Artifact artifact) {
        if (!this.built) {
            throw new IllegalStateException("build in directory `" + this.dir
                    + "' has not yet successfully completed");
        }
        return new ResolvedResource(new BuiltFileResource(this.dir, artifact),
                this.mr.getRevision());
    }

    public synchronized boolean cleanup() {
        this.built = false;
        return FileUtil.forceDelete(this.dir);
    }

    protected void saveFile(String name, InputStream input) throws IOException {
        FileUtil.copy(input, new File(this.dir, name), null);
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
        String baseURL = IvyPatternHelper.substitute(resourceURL, mr.getOrganisation(),
            mr.getName(), mr.getRevision(), null, null, null, null,
            mr.getQualifiedExtraAttributes(), null);
        int slash = baseURL.lastIndexOf('/');
        if (slash != -1) {
            baseURL = baseURL.substring(0, slash + 1);
        }
        return baseURL;
    }

    private static File getSubdir(File rootDir, ModuleRevisionId mr) {
        return new File(rootDir, mr.getOrganisation() + File.separatorChar + mr.getName()
                + File.separatorChar + mr.getRevision());
    }
}
