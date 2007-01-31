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
package org.apache.ivy.core.publish;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.cache.CacheManager;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.MDArtifact;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorParser;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorUpdater;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.util.Message;
import org.xml.sax.SAXException;

public class PublishEngine {
	private IvySettings _settings;
	
    public PublishEngine(IvySettings settings) {
		_settings = settings;
	}
	/**
     * 
     * @param pubrevision 
     * @param resolverName the name of a resolver to use for publication
     * @param srcArtifactPattern a pattern to find artifacts to publish with the given resolver
     * @param srcIvyPattern a pattern to find ivy file to publish, null if ivy file should not be published
     * @return a collection of missing artifacts (those that are not published)
     * @throws ParseException
     */
    public Collection publish(ModuleRevisionId mrid, String pubrevision, File cache, String srcArtifactPattern, String resolverName, String srcIvyPattern, boolean validate) throws IOException {
        return publish(mrid, pubrevision, cache, srcArtifactPattern, resolverName, srcIvyPattern, validate, false);
    }
    /**
     * 
     * @param pubrevision 
     * @param resolverName the name of a resolver to use for publication
     * @param srcArtifactPattern a pattern to find artifacts to publish with the given resolver
     * @param srcIvyPattern a pattern to find ivy file to publish, null if ivy file should not be published
     * @return a collection of missing artifacts (those that are not published)
     * @throws ParseException
     */
    public Collection publish(ModuleRevisionId mrid, String pubrevision, File cache, String srcArtifactPattern, String resolverName, String srcIvyPattern, boolean validate, boolean overwrite) throws IOException {
    	return publish(mrid, pubrevision, cache, srcArtifactPattern, resolverName, srcIvyPattern, null, null, null, validate, overwrite, false, null);
    }
    public Collection publish(ModuleRevisionId mrid, String pubrevision, File cache, String srcArtifactPattern, String resolverName, String srcIvyPattern, String status, Date pubdate, Artifact[] extraArtifacts, boolean validate, boolean overwrite, boolean update, String conf) throws IOException {
    	return publish(mrid, pubrevision, cache, Collections.singleton(srcArtifactPattern), resolverName, srcIvyPattern, status, pubdate, extraArtifacts, validate, overwrite, update, conf);
    }
    /**
     * Publishes a module to the repository.
     * 
     * The publish can update the ivy file to publish if update is set to true. In this case it will use
     * the given pubrevision, pubdate and status. If pudate is null it will default to the current date.
     * If status is null it will default to the current ivy file status (which itself defaults to integration if none is found).
     * If update is false, then if the revision is not the same in the ivy file than the one expected (given as parameter),
     * this method will fail with an  IllegalArgumentException.
     * pubdate and status are not used if update is false.
     * extra artifacts can be used to publish more artifacts than actually declared in the ivy file.
     * This can be useful to publish additional metadata or reports.
     * The extra artifacts array can be null (= no extra artifacts), and if non null only the name, type, ext url 
     * and extra attributes of the artifacts are really used. Other methods can return null safely. 
     * 
     * @param mrid
     * @param pubrevision
     * @param cache
     * @param srcArtifactPattern
     * @param resolverName
     * @param srcIvyPattern
     * @param status
     * @param pubdate
     * @param validate
     * @param overwrite
     * @param update
     * @return
     * @throws IOException
     */
    public Collection publish(ModuleRevisionId mrid, String pubrevision, File cache, Collection srcArtifactPattern, String resolverName, String srcIvyPattern, String status, Date pubdate, Artifact[] extraArtifacts, boolean validate, boolean overwrite, boolean update, String conf) throws IOException {
        Message.info(":: publishing :: "+mrid.getModuleId());
        Message.verbose("\tvalidate = "+validate);
        long start = System.currentTimeMillis();
        srcIvyPattern = _settings.substitute(srcIvyPattern);
        CacheManager cacheManager = getCacheManager(cache);
        // 1) find the resolved module descriptor
        ModuleRevisionId pubmrid = ModuleRevisionId.newInstance(mrid, pubrevision);
        File ivyFile;
        if (srcIvyPattern != null) {
        	ivyFile = new File(IvyPatternHelper.substitute(srcIvyPattern, DefaultArtifact.newIvyArtifact(pubmrid, new Date())));
        	if (!ivyFile.exists()) {
        		throw new IllegalArgumentException("ivy file to publish not found for "+mrid+": call deliver before ("+ivyFile+")");
        	}
        } else {
        	ivyFile = cacheManager.getResolvedIvyFileInCache(mrid);
        	if (!ivyFile.exists()) {
        		throw new IllegalStateException("ivy file not found in cache for "+mrid+": please resolve dependencies before publishing ("+ivyFile+")");
        	}
        }
        
        ModuleDescriptor md = null;
        URL ivyFileURL = null;
        try {
        	ivyFileURL = ivyFile.toURL();
        	md = XmlModuleDescriptorParser.getInstance().parseDescriptor(_settings, ivyFileURL, false);
        	if (srcIvyPattern != null) {
            	if (!pubrevision.equals(md.getModuleRevisionId().getRevision())) {
            		if (update) {
            			File tmp = File.createTempFile("ivy", ".xml");
            			tmp.deleteOnExit();
            			try {
							XmlModuleDescriptorUpdater.update(_settings, ivyFileURL, tmp, new HashMap(), status==null?md.getStatus():status, pubrevision, pubdate==null?new Date():pubdate, null, true);
							ivyFile = tmp;
							// we parse the new file to get updated module descriptor
							md = XmlModuleDescriptorParser.getInstance().parseDescriptor(_settings, ivyFile.toURL(), false);
							srcIvyPattern = ivyFile.getAbsolutePath();
						} catch (SAXException e) {
				        	throw new IllegalStateException("bad ivy file for "+mrid+": "+ivyFile+": "+e);
						}
            		} else {
            			throw new IllegalArgumentException("cannot publish "+ivyFile+" as "+pubrevision+": bad revision found in ivy file. Use deliver before.");
            		}
            	}
        	} else {
				md.setResolvedModuleRevisionId(pubmrid);
        	}
        } catch (MalformedURLException e) {
        	throw new RuntimeException("malformed url obtained for file "+ivyFile);
        } catch (ParseException e) {
        	throw new IllegalStateException("bad ivy file for "+mrid+": "+ivyFile+": "+e);
        }
        
        DependencyResolver resolver = _settings.getResolver(resolverName);
        if (resolver == null) {
            throw new IllegalArgumentException("unknown resolver "+resolverName);
        }
        
        // collect all declared artifacts of this module
        Collection missing = publish(md, resolver, srcArtifactPattern, srcIvyPattern, extraArtifacts, overwrite, conf);
        Message.verbose("\tpublish done ("+(System.currentTimeMillis()-start)+"ms)");
        return missing;
    }

    public Collection publish(ModuleDescriptor md, DependencyResolver resolver, Collection srcArtifactPattern, String srcIvyPattern, Artifact[] extraArtifacts, boolean overwrite, String conf) throws IOException {
        Collection missing = new ArrayList();
        Set artifactsSet = new HashSet();
		String[] confs;
		if (null == conf || "".equals(conf)) {
			confs = md.getConfigurationsNames();
		} else {
			StringTokenizer st = new StringTokenizer(conf, ",");
			confs = new String[st.countTokens()];
			int counter = 0;
			while (st.hasMoreTokens()) {
				confs[counter] = st.nextToken().trim();
				counter++;
			}
		}

		for (int i = 0; i < confs.length; i++) {
            Artifact[] artifacts = md.getArtifacts(confs[i]);
            for (int j = 0; j < artifacts.length; j++) {
                artifactsSet.add(artifacts[j]);
            }
        }
        if (extraArtifacts != null) {
        	for (int i = 0; i < extraArtifacts.length; i++) {
				artifactsSet.add(new MDArtifact(md, extraArtifacts[i].getName(), extraArtifacts[i].getType(), extraArtifacts[i].getExt(), extraArtifacts[i].getUrl(), extraArtifacts[i].getExtraAttributes()));
			}
        }
        // for each declared published artifact in this descriptor, do:
        for (Iterator iter = artifactsSet.iterator(); iter.hasNext();) {
            Artifact artifact = (Artifact) iter.next();
            //   1) copy the artifact using src patterns and resolver
            boolean published = false;
            for (Iterator iterator = srcArtifactPattern.iterator(); iterator.hasNext() && !published;) {
				String pattern = (String) iterator.next();
				published = publish(artifact, _settings.substitute(pattern), resolver, overwrite);
			}
            if (!published) {
            	Message.info("missing artifact "+artifact+":");
                for (Iterator iterator = srcArtifactPattern.iterator(); iterator.hasNext();) {
    				String pattern = (String) iterator.next();
                	Message.info("\t"+new File(IvyPatternHelper.substitute(pattern, artifact))+" file does not exist");
                }
                missing.add(artifact);
            }
        }
        if (srcIvyPattern != null) {
            Artifact artifact = MDArtifact.newIvyArtifact(md);
            if (!publish(artifact, srcIvyPattern, resolver, overwrite)) {
                Message.info("missing ivy file for "+md.getModuleRevisionId()+": "+new File(IvyPatternHelper.substitute(srcIvyPattern, artifact))+" file does not exist");
                missing.add(artifact);
            }
        }
        return missing;
    }

    private boolean publish(Artifact artifact, String srcArtifactPattern, DependencyResolver resolver, boolean overwrite) throws IOException {
    	IvyContext.getContext().checkInterrupted();
        File src = new File(IvyPatternHelper.substitute(srcArtifactPattern, artifact));
        if (src.exists()) {
            resolver.publish(artifact, src, overwrite);
            return true;
        } else {
            return false;
        }
    }

	private CacheManager getCacheManager(File cache) {
		//TODO : reuse instance
		CacheManager cacheManager = new CacheManager(_settings, cache);
		return cacheManager;
	}


}
