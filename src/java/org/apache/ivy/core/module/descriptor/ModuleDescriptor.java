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
package org.apache.ivy.core.module.descriptor;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.ivy.core.module.id.ArtifactId;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.module.id.ModuleRules;
import org.apache.ivy.plugins.conflict.ConflictManager;
import org.apache.ivy.plugins.latest.ArtifactInfo;
import org.apache.ivy.plugins.parser.ModuleDescriptorParser;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.plugins.version.VersionMatcher;
import org.apache.ivy.util.extendable.ExtendableItem;

/**
 * Descriptor of a module. This is the Java representation of an ivy.xml
 */
public interface ModuleDescriptor extends ExtendableItem, ArtifactInfo,
        DependencyDescriptorMediator {

    public static final String DEFAULT_CONFIGURATION = "default";

    public static final String CALLER_ALL_CONFIGURATION = "all";

    /**
     * Returns true if this descriptor is a default one, i.e. one generated for a module not
     * actually having one.
     * 
     * @return
     */
    boolean isDefault();

    ModuleRevisionId getModuleRevisionId();

    /**
     * The module revision id returned here is the resolved one, i.e. it is never a latest one. If
     * the revision has not been resolved, a null revision should be returned by getRevision() of
     * the returned ModuleRevisionId. This revision must be the same as the module descriptor
     * resolved revision id unless no module descriptor is defined
     * 
     * @return
     */
    ModuleRevisionId getResolvedModuleRevisionId();

    /**
     * This method update the resolved module revision id
     * 
     * @param revId
     */
    void setResolvedModuleRevisionId(ModuleRevisionId revId);

    /**
     * Get the list of parent descriptors imported via an &lt;extends&gt; element. Only directly
     * imported descriptors are included; the parent's parents are not included.
     */
    ExtendsDescriptor[] getInheritedDescriptors();

    /**
     * This method update the resolved publication date
     * 
     * @param publicationDate
     */
    void setResolvedPublicationDate(Date publicationDate);

    String getStatus();

    /**
     * May be <code>null</code> if unknown in the descriptor itself.
     * 
     * @return The publication date or <code>null</code> when not knwon.
     */
    Date getPublicationDate();

    /**
     * The publication date of the module revision should be the date at which it has been
     * published, i.e. in general the date of any of its published artifacts, since all published
     * artifact of a module should follow the same publishing cycle.
     */
    Date getResolvedPublicationDate();

    /**
     * Returns all the configurations declared by this module as an array. This array is never empty
     * (a 'default' conf is assumed when none is declared in the ivy file)
     * 
     * @return all the configurations declared by this module as an array.
     */
    Configuration[] getConfigurations();

    String[] getConfigurationsNames();

    String[] getPublicConfigurationsNames();

    Artifact[] getArtifacts(String conf);

    /**
     * Returns all artifacts of this module, excluding the artifact corresponding to the module
     * descriptor.
     * 
     * @return all published artifacts of this module
     * @see #getMetadataArtifact()
     */
    Artifact[] getAllArtifacts();

    /**
     * @retun The dependencies of the module. If there is no dependencies return an empty array (non
     *        null)
     */
    DependencyDescriptor[] getDependencies();

    /**
     * Returns true if the module described by this descriptor dependes directly upon the given
     * module descriptor
     * 
     * @param md
     * @return
     */
    boolean dependsOn(VersionMatcher matcher, ModuleDescriptor md);

    /**
     * @param confName
     * @return
     */
    Configuration getConfiguration(String confName);

    /**
     * Returns the conflict manager to use for the given ModuleId, or <code>null</code> if no
     * specific conflict manager is associated with the given module id in this module descriptor.
     * 
     * @param id
     * @return
     */
    ConflictManager getConflictManager(ModuleId id);

    /**
     * Returns the licenses of the module described by this descriptor
     * 
     * @return
     */
    License[] getLicenses();

    String getHomePage();

    String getDescription();

    long getLastModified();

    /**
     * Writes this module descriptor as an ivy file. If this descriptor was obtained through the
     * parsing of an ivy file, it should keep the layout of the file the most possible similar to
     * the original one.
     * 
     * @param ivyFile
     *            the destination ivy file
     */
    void toIvyFile(File ivyFile) throws ParseException, IOException;

    /**
     * The ModuleDescriptorParser used to parse this module descriptor, null is no parser was used.
     * 
     * @return
     */
    ModuleDescriptorParser getParser();

    /**
     * The resource being the source of this module descriptor, null if no resource corresponds to
     * this module descriptor
     * 
     * @return
     */
    Resource getResource();

    /**
     * Returns the Artifact representing this module descriptor itself.
     * <p>
     * Even though the module descriptor is never described as a published artifact of a module in
     * the module descriptor itself, it is often useful to consider it as any other artifact of the
     * module. This method allows to access to the Artifact object representing this module
     * descriptor for this purpose.
     * </p>
     * 
     * @return the Artifact representing this module descriptor itself.
     */
    Artifact getMetadataArtifact();

    /**
     * Returns true if this descriptor contains any exclusion rule
     * 
     * @return true if this descriptor contains any exclusion rule
     */
    boolean canExclude();

    /**
     * Returns true if an exclude rule of this module attached to any of the given configurations
     * matches the given artifact id, and thus exclude it
     * 
     * @param moduleConfs
     * @param artifactId
     * @return
     */
    boolean doesExclude(String[] moduleConfs, ArtifactId artifactId);

    /**
     * Returns an array of all the exclude rules this module descriptor currently holds. Module
     * Descriptor exclude rules are used to exclude (usually transitive) dependencies for the whole
     * module.
     * 
     * @return an array of {@link ExcludeRule} this module descriptor holds
     */
    public ExcludeRule[] getAllExcludeRules();

    /**
     * Returns all the dependency descriptor mediators used by this {@link ModuleDescriptor}, as an
     * instance of {@link ModuleRules}.
     * <p>
     * All rules in the {@link ModuleRules} object returned are {@link DependencyDescriptorMediator}
     * .
     * </p>
     * 
     * @return all the dependency descriptor mediators used by this {@link ModuleDescriptor}.
     */
    public ModuleRules/* <DependencyDescriptorMediator> */getAllDependencyDescriptorMediators();

    /**
     * Returns the list of xml namespaces used by extra attributes, as Map from prefix to namespace
     * URIs.
     * <p>
     * The returned list is never <code>null</code>, it is empty when no extra attribute is used or
     * if extra attributes are used without xml namespaces
     * </p>
     * 
     * @return the list of xml namespaces used by extra attributes, as Map from prefix to namespace
     *         URIs.
     */
    Map<String, String> getExtraAttributesNamespaces();

    /**
     * Returns the custom info provided in the info tag. All the tags except the description are
     * given. The key is the name of the tag, the value is its content. <br />
     * 
     * @deprecated this method is not returning the full content of the extra info: to get the full
     *             structure of the extra infos, use getExtraInfos()
     */
    @Deprecated
    Map<String, String> getExtraInfo();

    /**
     * Returns a list of extras infos (tag name, attributes and content). All the tags except the
     * description are given.
     * 
     * @since 2.4.0
     * @return
     */
    List<ExtraInfoHolder> getExtraInfos();

    /**
     * Returns content from first extrainfo matching with given tag name
     * 
     * @since 2.4.0
     * @return
     */
    String getExtraInfoContentByTagName(String tagName);

    /**
     * Returns first extrainfo matching with given tag name
     * 
     * @since 2.4.0
     * @return
     */
    ExtraInfoHolder getExtraInfoByTagName(String tagName);
}
