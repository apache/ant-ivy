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
package org.apache.ivy.plugins.parser.m2;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.DefaultDependencyArtifactDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultExcludeRule;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.descriptor.Configuration.Visibility;
import org.apache.ivy.core.module.id.ArtifactId;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.matcher.ExactPatternMatcher;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.ivy.plugins.parser.ModuleDescriptorParser;
import org.apache.ivy.plugins.parser.m2.PomReader.PomDependencyData;
import org.apache.ivy.plugins.parser.m2.PomReader.PomDependencyMgt;
import org.apache.ivy.plugins.repository.Resource;


/**
 * Build a module descriptor.  This class handle the complexity of the structure of an ivy 
 * ModuleDescriptor and isolate the PomModuleDescriptorParser from it. 
 */
public class PomModuleDescriptorBuilder {

    
    public static final Configuration[] MAVEN2_CONFIGURATIONS = new Configuration[] {
        new Configuration("default", Visibility.PUBLIC,
                "runtime dependencies and master artifact can be used with this conf",
                new String[] {"runtime", "master"}, true, null),
        new Configuration("master", Visibility.PUBLIC,
                "contains only the artifact published by this module itself, "
                + "with no transitive dependencies",
                new String[0], true, null),
        new Configuration("compile", Visibility.PUBLIC,
                "this is the default scope, used if none is specified. "
                + "Compile dependencies are available in all classpaths.",
                new String[0], true, null),
        new Configuration("provided", Visibility.PUBLIC,
                "this is much like compile, but indicates you expect the JDK or a container "
                + "to provide it. "
                + "It is only available on the compilation classpath, and is not transitive.",
                new String[0], true, null),
        new Configuration("runtime", Visibility.PUBLIC,
                "this scope indicates that the dependency is not required for compilation, "
                + "but is for execution. It is in the runtime and test classpaths, "
                + "but not the compile classpath.",
                new String[] {"compile"}, true, null),
        new Configuration("test", Visibility.PRIVATE,
                "this scope indicates that the dependency is not required for normal use of "
                + "the application, and is only available for the test compilation and "
                + "execution phases.",
                new String[] {"runtime"}, true, null),
        new Configuration("system", Visibility.PUBLIC,
                "this scope is similar to provided except that you have to provide the JAR "
                + "which contains it explicitly. The artifact is always available and is not "
                + "looked up in a repository.",
                new String[0], true, null),
        new Configuration("optional", Visibility.PUBLIC, 
                "contains all optional dependencies", new String[0], true, null)
                };

    static final Map MAVEN2_CONF_MAPPING = new HashMap();

    private static final String DEPENDENCY_MANAGEMENT = "m:dependency.management";        
    private static final String PROPERTIES = "m:properties";
    private static final String EXTRA_INFO_DELIMITER = "__";

    
    static interface ConfMapper {
        public void addMappingConfs(DefaultDependencyDescriptor dd, boolean isOptional);
    }
    
    static {
        MAVEN2_CONF_MAPPING.put("compile", new ConfMapper() {
            public void addMappingConfs(DefaultDependencyDescriptor dd, boolean isOptional) {
                if (isOptional) {
                    dd.addDependencyConfiguration("optional", "compile(*)");
                    //dd.addDependencyConfiguration("optional", "provided(*)");
                    dd.addDependencyConfiguration("optional", "master(*)");
                    
                } else {
                    dd.addDependencyConfiguration("compile", "compile(*)");
                    //dd.addDependencyConfiguration("compile", "provided(*)");
                    dd.addDependencyConfiguration("compile", "master(*)");
                    dd.addDependencyConfiguration("runtime", "runtime(*)");
                }
            }
        });
        MAVEN2_CONF_MAPPING.put("provided", new ConfMapper() {
            public void addMappingConfs(DefaultDependencyDescriptor dd, boolean isOptional) {
                if (isOptional) {
                    dd.addDependencyConfiguration("optional", "compile(*)");
                    dd.addDependencyConfiguration("optional", "provided(*)");
                    dd.addDependencyConfiguration("optional", "runtime(*)");
                    dd.addDependencyConfiguration("optional", "master(*)");                    
                } else {
                    dd.addDependencyConfiguration("provided", "compile(*)");
                    dd.addDependencyConfiguration("provided", "provided(*)");
                    dd.addDependencyConfiguration("provided", "runtime(*)");
                    dd.addDependencyConfiguration("provided", "master(*)");
                }
            }
        });
        MAVEN2_CONF_MAPPING.put("runtime", new ConfMapper() {
            public void addMappingConfs(DefaultDependencyDescriptor dd, boolean isOptional) {
                if (isOptional) {
                    dd.addDependencyConfiguration("optional", "compile(*)");
                    dd.addDependencyConfiguration("optional", "provided(*)");
                    dd.addDependencyConfiguration("optional", "master(*)");
                    
                } else {
                    dd.addDependencyConfiguration("runtime", "compile(*)");
                    dd.addDependencyConfiguration("runtime", "runtime(*)");
                    dd.addDependencyConfiguration("runtime", "master(*)");
                }
            }
        });
        MAVEN2_CONF_MAPPING.put("test", new ConfMapper() {
            public void addMappingConfs(DefaultDependencyDescriptor dd, boolean isOptional) {
                //optional doesn't make sense in the test scope
                dd.addDependencyConfiguration("test", "runtime(*)");
                dd.addDependencyConfiguration("test", "master(*)");
            }
        });
        MAVEN2_CONF_MAPPING.put("system", new ConfMapper() {
            public void addMappingConfs(DefaultDependencyDescriptor dd, boolean isOptional) {
                //optional doesn't make sense in the system scope
                dd.addDependencyConfiguration("system", "master(*)");
            }
        });
    }

    
    
    private final DefaultModuleDescriptor ivyModuleDescriptor;


    private ModuleRevisionId mrid;

    
    public PomModuleDescriptorBuilder(ModuleDescriptorParser parser, Resource res) {
        ivyModuleDescriptor = new DefaultModuleDescriptor(parser, res);
        ivyModuleDescriptor.setResolvedPublicationDate(new Date(res.getLastModified()));
        for (int i = 0; i < MAVEN2_CONFIGURATIONS.length; i++) {
            ivyModuleDescriptor.addConfiguration(MAVEN2_CONFIGURATIONS[i]);
        }
        ivyModuleDescriptor.setMappingOverride(true);
        ivyModuleDescriptor.addExtraAttributeNamespace("m", Ivy.getIvyHomeURL() + "maven");
    }


    public ModuleDescriptor getModuleDescriptor() {
        return ivyModuleDescriptor;
    }


    public void setModuleRevId(String groupId, String artifactId, String version) {
        mrid = ModuleRevisionId.newInstance(groupId, artifactId, version);
        ivyModuleDescriptor.setModuleRevisionId(mrid);
     }


    public void addArtifact(String artifactId, String packaging) {
        ivyModuleDescriptor.addArtifact("master", 
                new DefaultArtifact(mrid, new Date(), artifactId, packaging, packaging));
    }


    public void addDependency(Resource res, PomDependencyData dep) throws ParseException {
        if (!MAVEN2_CONF_MAPPING.containsKey(dep.getScope())) {
            String msg = "Unknown scope " + dep.getScope() + " for dependency "
                    + ModuleId.newInstance(dep.getGroupId(), dep.getArtifaceId()) + " in "
                    + res.getName();
            throw new ParseException(msg, 0);
        }
        
        String version = dep.getVersion();
        version = (version == null || version.length() == 0) ? getDefaultVersion(dep) : version;
        ModuleRevisionId moduleRevId = ModuleRevisionId.newInstance(dep.getGroupId(), dep
                .getArtifaceId(), version);
        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(ivyModuleDescriptor,
                moduleRevId, true, false, true);
        ConfMapper mapping = (ConfMapper) MAVEN2_CONF_MAPPING.get(dep.getScope());
        mapping.addMappingConfs(dd, dep.isOptional());
        Map extraAtt = new HashMap();
        if (dep.getClassifier() != null) {
            // we deal with classifiers by setting an extra attribute and forcing the
            // dependency to assume such an artifact is published
            extraAtt.put("m:classifier", dep.getClassifier());
            DefaultDependencyArtifactDescriptor depArtifact = 
                    new DefaultDependencyArtifactDescriptor(dd.getDependencyId().getName(),
                        "jar", "jar", null, extraAtt);
            // here we have to assume a type and ext for the artifact, so this is a limitation
            // compared to how m2 behave with classifiers
            String optionalizedScope = dep.isOptional() ? "optional" : dep.getScope();
            dd.addDependencyArtifact(optionalizedScope, depArtifact);
        }
        
        for (Iterator itExcl = dep.getExcludedModules().iterator(); itExcl.hasNext();) {
            ModuleId excludedModule = (ModuleId) itExcl.next();
            String[] confs = dd.getModuleConfigurations();
            for (int k = 0; k < confs.length; k++) {
                dd.addExcludeRule(confs[k], new DefaultExcludeRule(new ArtifactId(
                    excludedModule, PatternMatcher.ANY_EXPRESSION,
                                PatternMatcher.ANY_EXPRESSION,
                                PatternMatcher.ANY_EXPRESSION),
                                ExactPatternMatcher.INSTANCE, null));
            }
        }
    
        ivyModuleDescriptor.addDependency(dd);
    }


    public void addDependency(DependencyDescriptor descriptor) {
        ivyModuleDescriptor.addDependency(descriptor);
    }


    public void addDependencyMgt(PomDependencyMgt dep) {
        String key = getDependencyMgtExtraInfoKey(dep.getGroupId(), dep.getArtifaceId());
        ivyModuleDescriptor.addExtraInfo(key, dep.getVersion());
    }

    private String getDefaultVersion(PomDependencyData dep) {
        String key = getDependencyMgtExtraInfoKey(dep.getGroupId(), dep.getArtifaceId());        
        return (String) ivyModuleDescriptor.getExtraInfo().get(key);
    }


    private static String getDependencyMgtExtraInfoKey(String groupId, String artifaceId) {
        return DEPENDENCY_MANAGEMENT + EXTRA_INFO_DELIMITER + groupId
                + EXTRA_INFO_DELIMITER + artifaceId;
    }
    
    private static String getPropertyExtraInfoKey(String propertyName) {
        return PROPERTIES + EXTRA_INFO_DELIMITER + propertyName;
    }

    

    public void addExtraInfos(Map extraAttributes) {
        for (Iterator it = extraAttributes.entrySet().iterator(); it.hasNext();) {
            Map.Entry entry = (Entry) it.next();
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            addExtraInfo(key, value);
        }
    }


    private void addExtraInfo(String key, String value) {
        if (!ivyModuleDescriptor.getExtraInfo().containsKey(key)) {
            ivyModuleDescriptor.addExtraInfo(key, value);
        }
    }

    
    
    public static Map extractPomProperties(Map extraInfo) {
        Map r = new HashMap();
        for (Iterator it = extraInfo.entrySet().iterator(); it.hasNext();) {
            Map.Entry extraInfoEntry = (Map.Entry) it.next();
            if (((String) extraInfoEntry.getKey()).startsWith(PROPERTIES)) {
                String prop = ((String) extraInfoEntry.getKey()).substring(PROPERTIES.length()
                        + EXTRA_INFO_DELIMITER.length());
                r.put(prop, extraInfoEntry.getValue());
            }
        }
        return r;
    }


    public void addProperty(String propertyName, String value) {
        addExtraInfo(getPropertyExtraInfoKey(propertyName), value);
    }

    
}
