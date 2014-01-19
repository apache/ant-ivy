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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.resolver.URLResolver;
import org.apache.ivy.plugins.resolver.util.ResolvedResource;
import org.apache.ivy.util.FileUtil;
import org.apache.ivy.util.Message;

/**
 * Resolver that performs a "build" operation to resolve artifacts.
 * 
 * <p>
 * The resolver is configured with a base URL, from which the "ivy.xml" and "packager.xml" files are
 * resolved. The latter file contains instructions describing how to build the actual artifacts.
 */
public class PackagerResolver extends URLResolver {

    private static final String PACKAGER_ARTIFACT_NAME = "packager";

    private static final String PACKAGER_ARTIFACT_TYPE = "packager";

    private static final String PACKAGER_ARTIFACT_EXT = "xml";

    private final HashMap/* <ModuleRevisionId, PackagerCacheEntry> */packagerCache = new HashMap();

    private File buildRoot;

    private File resourceCache;

    private String resourceURL;

    private Map/* <String,String> */properties = new LinkedHashMap();

    private boolean validate = true;

    private boolean preserve;

    private boolean restricted = true;

    private boolean verbose;

    private boolean quiet;

    public PackagerResolver() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                clearCache();
            }
        });
    }

    protected synchronized void clearCache() {
        if (this.preserve) {
            return;
        }
        for (Iterator i = packagerCache.values().iterator(); i.hasNext();) {
            PackagerCacheEntry entry = (PackagerCacheEntry) i.next();
            entry.cleanup();
        }
        packagerCache.clear();
        if (this.buildRoot != null) {
            FileUtil.forceDelete(this.buildRoot);
        }
    }

    /**
     * Set root directory under which builds take place.
     */
    public void setBuildRoot(File buildRoot) {
        this.buildRoot = buildRoot;
    }

    /**
     * Returns root directory under which builds take place.
     */
    public File getBuildRoot() {
        return buildRoot;
    }

    /**
     * Set resource cache directory.
     */
    public void setResourceCache(File resourceCache) {
        this.resourceCache = resourceCache;
    }

    /**
     * Get resource cache directory.
     */
    public File getResourceCache() {
        return resourceCache;
    }

    /**
     * Set base resource override URL pattern.
     */
    public void setResourceURL(String resourceURL) {
        this.resourceURL = resourceURL;
    }

    /**
     * Set pattern for locating "packager.xml" files.
     */
    public void setPackagerPattern(String pattern) {
        ArrayList list = new ArrayList();
        list.add(pattern);
        setArtifactPatterns(list);
    }

    /**
     * Set whether to preserve build directories. Default is false.
     */
    public void setPreserveBuildDirectories(boolean preserve) {
        this.preserve = preserve;
    }

    /**
     * Set whether to enable restricted mode. Default is true.
     */
    public void setRestricted(boolean restricted) {
        this.restricted = restricted;
    }

    /**
     * Set whether to run ant with the -verbose flag. Default is false.
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * Set whether to run ant with the -quiet flag. Default is false.
     */
    public void setQuiet(boolean quiet) {
        this.quiet = quiet;
    }

    /**
     * Set whether to validate downloaded packager.xml files. Default is true.
     */
    public void setValidate(boolean validate) {
        this.validate = validate;
    }

    public void setAllownomd(boolean b) {
        Message.error("allownomd not supported by resolver " + this);
    }

    public void setDescriptor(String rule) {
        if (DESCRIPTOR_OPTIONAL.equals(rule)) {
            Message.error("descriptor=\"" + DESCRIPTOR_OPTIONAL + "\" not supported by resolver "
                    + this);
            return;
        }
        super.setDescriptor(rule);
    }

    /**
     * Sets a property to be passed to the child Ant build responsible for packaging the dependency.
     * 
     * @param propertyKey
     *            the property to pass
     * @param propertyValue
     *            the value of the property to pass
     */
    public void setProperty(String propertyKey, String propertyValue) {
        properties.put(propertyKey, propertyValue);
    }

    // @Override
    public void validate() {
        super.validate();
        if (this.buildRoot == null) {
            throw new IllegalStateException("no buildRoot specified");
        }
        if (getArtifactPatterns().size() == 0) {
            throw new IllegalStateException("no packager pattern specified");
        }
    }

    // @Override
    public synchronized ResolvedResource findArtifactRef(Artifact artifact, Date date) {

        // For our special packager.xml file, defer to superclass
        if (PACKAGER_ARTIFACT_NAME.equals(artifact.getName())
                && PACKAGER_ARTIFACT_TYPE.equals(artifact.getType())
                && PACKAGER_ARTIFACT_EXT.equals(artifact.getExt())) {
            return super.findArtifactRef(artifact, date);
        }

        // Check the cache
        ModuleRevisionId mr = artifact.getModuleRevisionId();
        PackagerCacheEntry entry = (PackagerCacheEntry) packagerCache.get(mr);

        // Ignore invalid entries
        if (entry != null && !entry.isBuilt()) {
            packagerCache.remove(mr);
            entry.cleanup();
            entry = null;
        }

        // Build the artifacts (if not done already)
        if (entry == null) {
            ResolvedResource packager = findArtifactRef(new DefaultArtifact(mr, null,
                    PACKAGER_ARTIFACT_NAME, PACKAGER_ARTIFACT_TYPE, PACKAGER_ARTIFACT_EXT), date);
            if (packager == null) {
                return null;
            }
            entry = new PackagerCacheEntry(mr, this.buildRoot, this.resourceCache,
                    this.resourceURL, this.validate, this.preserve, this.restricted, this.verbose,
                    this.quiet);
            try {
                entry.build(packager.getResource(), properties);
            } catch (IOException e) {
                throw new RuntimeException("can't build artifact " + artifact, e);
            }
            packagerCache.put(mr, entry);
        }

        // Return reference to desired artifact
        return entry.getBuiltArtifact(artifact);
    }

    public String getTypeName() {
        return "packager";
    }
}
