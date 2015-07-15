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
package org.apache.ivy.core.cache;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Map;
import java.util.Properties;

import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.RelativeUrlResolver;
import org.apache.ivy.core.module.descriptor.ExtendsDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.module.status.StatusManager;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.IvySettingsAware;
import org.apache.ivy.plugins.conflict.ConflictManager;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.ivy.plugins.namespace.Namespace;
import org.apache.ivy.plugins.parser.ModuleDescriptorParser;
import org.apache.ivy.plugins.parser.ParserSettings;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorParser;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.util.FileUtil;

public class DefaultResolutionCacheManager implements ResolutionCacheManager, IvySettingsAware {

    private static final String DEFAULT_CACHE_RESOLVED_IVY_PATTERN = "resolved-[organisation]-[module]-[revision].xml";

    private static final String DEFAULT_CACHE_RESOLVED_IVY_PROPERTIES_PATTERN = "resolved-[organisation]-[module]-[revision].properties";

    private String resolvedIvyPattern = DEFAULT_CACHE_RESOLVED_IVY_PATTERN;

    private String resolvedIvyPropertiesPattern = DEFAULT_CACHE_RESOLVED_IVY_PROPERTIES_PATTERN;

    private File basedir;

    private String name = "resolution-cache";

    private IvySettings settings;

    public DefaultResolutionCacheManager() {
    }

    public DefaultResolutionCacheManager(File basedir) {
        setBasedir(basedir);
    }

    public void setSettings(IvySettings settings) {
        this.settings = settings;
    }

    public File getResolutionCacheRoot() {
        return basedir;
    }

    public File getBasedir() {
        return basedir;
    }

    public void setBasedir(File basedir) {
        this.basedir = basedir;
    }

    public String getResolvedIvyPattern() {
        return resolvedIvyPattern;
    }

    public void setResolvedIvyPattern(String cacheResolvedIvyPattern) {
        this.resolvedIvyPattern = cacheResolvedIvyPattern;
    }

    public String getResolvedIvyPropertiesPattern() {
        return resolvedIvyPropertiesPattern;
    }

    public void setResolvedIvyPropertiesPattern(String cacheResolvedIvyPropertiesPattern) {
        this.resolvedIvyPropertiesPattern = cacheResolvedIvyPropertiesPattern;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public File getResolvedIvyFileInCache(ModuleRevisionId mrid) {
        String file = IvyPatternHelper.substitute(getResolvedIvyPattern(), mrid.getOrganisation(),
            mrid.getName(), mrid.getRevision(), "ivy", "ivy", "xml");
        return new File(getResolutionCacheRoot(), file);
    }

    public File getResolvedIvyPropertiesInCache(ModuleRevisionId mrid) {
        String file = IvyPatternHelper.substitute(getResolvedIvyPropertiesPattern(),
            mrid.getOrganisation(), mrid.getName(), mrid.getRevision(), "ivy", "ivy", "xml");
        return new File(getResolutionCacheRoot(), file);
    }

    public File getConfigurationResolveReportInCache(String resolveId, String conf) {
        return new File(getResolutionCacheRoot(), resolveId + "-" + conf + ".xml");
    }

    public File[] getConfigurationResolveReportsInCache(final String resolveId) {
        final String prefix = resolveId + "-";
        final String suffix = ".xml";
        return getResolutionCacheRoot().listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return (name.startsWith(prefix) && name.endsWith(suffix));
            }
        });
    }

    public ModuleDescriptor getResolvedModuleDescriptor(ModuleRevisionId mrid)
            throws ParseException, IOException {
        File ivyFile = getResolvedIvyFileInCache(mrid);
        if (!ivyFile.exists()) {
            throw new IllegalStateException("Ivy file not found in cache for " + mrid + "!");
        }

        Properties paths = new Properties();

        File parentsFile = getResolvedIvyPropertiesInCache(ModuleRevisionId.newInstance(mrid,
            mrid.getRevision() + "-parents"));
        if (parentsFile.exists()) {
            FileInputStream in = new FileInputStream(parentsFile);
            paths.load(in);
            in.close();
        }

        ParserSettings pSettings = new CacheParserSettings(settings, paths);

        URL ivyFileURL = ivyFile.toURI().toURL();
        return getModuleDescriptorParser(ivyFile).parseDescriptor(pSettings, ivyFileURL, false);
    }

    /**
     * Choose write module descriptor parser for a given moduleDescriptor
     * 
     * @param moduleDescriptorFile
     *            a given module descriptor
     * @return
     */
    protected ModuleDescriptorParser getModuleDescriptorParser(File moduleDescriptorFile) {
        return XmlModuleDescriptorParser.getInstance();
    }

    public void saveResolvedModuleDescriptor(ModuleDescriptor md) throws ParseException,
            IOException {
        ModuleRevisionId mrevId = md.getResolvedModuleRevisionId();
        File ivyFileInCache = getResolvedIvyFileInCache(mrevId);
        md.toIvyFile(ivyFileInCache);

        Properties paths = new Properties();
        saveLocalParents(mrevId, md, ivyFileInCache, paths);

        if (!paths.isEmpty()) {
            File parentsFile = getResolvedIvyPropertiesInCache(ModuleRevisionId.newInstance(mrevId,
                mrevId.getRevision() + "-parents"));
            FileOutputStream out = new FileOutputStream(parentsFile);
            paths.store(out, null);
            out.close();
        }
    }

    private void saveLocalParents(ModuleRevisionId baseMrevId, ModuleDescriptor md, File mdFile,
            Properties paths) throws ParseException, IOException {
        ExtendsDescriptor[] parents = md.getInheritedDescriptors();
        for (int i = 0; i < parents.length; i++) {
            if (!parents[i].isLocal()) {
                // we store only local parents in the cache!
                continue;
            }

            ModuleDescriptor parent = parents[i].getParentMd();
            ModuleRevisionId pRevId = ModuleRevisionId.newInstance(baseMrevId,
                baseMrevId.getRevision() + "-parent." + paths.size());
            File parentFile = getResolvedIvyFileInCache(pRevId);
            parent.toIvyFile(parentFile);

            paths.setProperty(mdFile.getName() + "|" + parents[i].getLocation(),
                parentFile.getAbsolutePath());
            saveLocalParents(baseMrevId, parent, parentFile, paths);
        }
    }

    public String toString() {
        return name;
    }

    public void clean() {
        FileUtil.forceDelete(getBasedir());
    }

    private static class CacheParserSettings implements ParserSettings {

        private ParserSettings delegate;

        private Map parentPaths;

        public CacheParserSettings(ParserSettings delegate, Map parentPaths) {
            this.delegate = delegate;
            this.parentPaths = parentPaths;
        }

        public String substitute(String value) {
            return delegate.substitute(value);
        }

        public Map substitute(Map strings) {
            return delegate.substitute(strings);
        }

        public ResolutionCacheManager getResolutionCacheManager() {
            return delegate.getResolutionCacheManager();
        }

        public ConflictManager getConflictManager(String name) {
            return delegate.getConflictManager(name);
        }

        public PatternMatcher getMatcher(String matcherName) {
            return delegate.getMatcher(matcherName);
        }

        public Namespace getNamespace(String namespace) {
            return delegate.getNamespace(namespace);
        }

        public StatusManager getStatusManager() {
            return delegate.getStatusManager();
        }

        public RelativeUrlResolver getRelativeUrlResolver() {
            return new MapURLResolver(parentPaths, delegate.getRelativeUrlResolver());
        }

        public DependencyResolver getResolver(ModuleRevisionId mRevId) {
            return delegate.getResolver(mRevId);
        }

        public File resolveFile(String filename) {
            return delegate.resolveFile(filename);
        }

        public String getDefaultBranch(ModuleId moduleId) {
            return delegate.getDefaultBranch(moduleId);
        }

        public Namespace getContextNamespace() {
            return delegate.getContextNamespace();
        }

        public String getVariable(String value) {
            return delegate.getVariable(value);
        }
    }

    private static class MapURLResolver extends RelativeUrlResolver {

        private Map paths;

        private RelativeUrlResolver delegate;

        private MapURLResolver(Map paths, RelativeUrlResolver delegate) {
            this.paths = paths;
            this.delegate = delegate;
        }

        public URL getURL(URL context, String url) throws MalformedURLException {
            String path = context.getPath();
            if (path.indexOf('/') >= 0) {
                String file = path.substring(path.lastIndexOf('/') + 1);

                if (paths.containsKey(file + "|" + url)) {
                    File result = new File(paths.get(file + "|" + url).toString());
                    return result.toURI().toURL();
                }
            }

            return delegate.getURL(context, url);
        }
    }
}
