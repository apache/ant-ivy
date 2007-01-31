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
package org.apache.ivy.core.deliver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.cache.CacheManager;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorParser;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorUpdater;
import org.apache.ivy.util.Message;
import org.xml.sax.SAXException;

public class DeliverEngine {
	private IvySettings _settings;
	
    public DeliverEngine(IvySettings settings) {
		_settings = settings;
	}

	public void deliver(ModuleRevisionId mrid,
            String revision,
            File cache, 
            String destIvyPattern, 
            String status,
            Date pubdate,
            PublishingDependencyRevisionResolver pdrResolver, 
            boolean validate
            ) throws IOException, ParseException {
        deliver(mrid, revision, cache, destIvyPattern, status, pubdate, pdrResolver, validate, true);
    }
    
    /**
     * delivers a resolved ivy file based upon last resolve call status and
     * the given PublishingDependencyRevisionResolver.
     * If resolve report file cannot be found in cache, then it throws 
     * an IllegalStateException (maybe resolve has not been called before ?)
     * Moreover, the given PublishingDependencyRevisionResolver is used for each 
     * dependency to get its published information. This can particularly useful
     * when the publish is made for a delivery, and when we wish to deliver each
     * dependency which is still in integration. The PublishingDependencyRevisionResolver
     * can then do the delivering work for the dependency and return the new (delivered)
     * dependency info (with the delivered revision). Note that 
     * PublishingDependencyRevisionResolver is only called for each <b>direct</b> dependency.
     * 
     * @param status the new status, null to keep the old one
     * @throws ParseException
     */
    public void deliver(ModuleRevisionId mrid,
            String revision,
            File cache, 
            String destIvyPattern, 
            String status,
            Date pubdate,
            PublishingDependencyRevisionResolver pdrResolver, 
            boolean validate,
            boolean resolveDynamicRevisions) throws IOException, ParseException {
        Message.info(":: delivering :: "+mrid+" :: "+revision+" :: "+status+" :: "+pubdate);
        Message.verbose("\tvalidate = "+validate);
        long start = System.currentTimeMillis();
        destIvyPattern = _settings.substitute(destIvyPattern);
        CacheManager cacheManager = getCacheManager(cache);
        
        // 1) find the resolved module descriptor in cache
        File ivyFile = cacheManager.getResolvedIvyFileInCache(mrid);
        if (!ivyFile.exists()) {
            throw new IllegalStateException("ivy file not found in cache for "+mrid+": please resolve dependencies before publishing ("+ivyFile+")");
        }
        ModuleDescriptor md = null;
        URL ivyFileURL = null;
        try {
            ivyFileURL = ivyFile.toURL();
            md = XmlModuleDescriptorParser.getInstance().parseDescriptor(_settings, ivyFileURL, validate);
            md.setResolvedModuleRevisionId(ModuleRevisionId.newInstance(mrid, revision));
            md.setResolvedPublicationDate(pubdate);
        } catch (MalformedURLException e) {
            throw new RuntimeException("malformed url obtained for file "+ivyFile , e);
        } catch (ParseException e) {
            throw new RuntimeException("bad ivy file in cache for "+mrid+": please clean and resolve again" , e);
        }
        
        // 2) parse resolvedRevisions From properties file
        Map resolvedRevisions = new HashMap(); // Map (ModuleId -> String revision)
        Map dependenciesStatus = new HashMap(); // Map (ModuleId -> String status)
        File ivyProperties = cacheManager.getResolvedIvyPropertiesInCache(mrid);
        if (!ivyProperties.exists()) {
            throw new IllegalStateException("ivy properties not found in cache for "+mrid+": please resolve dependencies before publishing ("+ivyFile+")");
        }
        Properties props = new Properties();
        props.load(new FileInputStream(ivyProperties));
        
        for (Iterator iter = props.keySet().iterator(); iter.hasNext();) {
            String depMridStr = (String)iter.next();
            String[] parts = props.getProperty(depMridStr).split(" ");
            ModuleRevisionId decodedMrid = ModuleRevisionId.decode(depMridStr);
            if (resolveDynamicRevisions) {
                resolvedRevisions.put(decodedMrid, parts[0]);
            }
            dependenciesStatus.put(decodedMrid, parts[1]);
        }
        
        // 3) use pdrResolver to resolve dependencies info
        Map resolvedDependencies = new HashMap(); // Map (ModuleRevisionId -> String revision)
        DependencyDescriptor[] dependencies = md.getDependencies();
        for (int i = 0; i < dependencies.length; i++) {
            String rev = (String)resolvedRevisions.get(dependencies[i].getDependencyRevisionId());
            if (rev == null) {
                rev = dependencies[i].getDependencyRevisionId().getRevision();
            }
            String depStatus = (String)dependenciesStatus.get(dependencies[i].getDependencyRevisionId());
            resolvedDependencies.put(dependencies[i].getDependencyRevisionId(), 
                    pdrResolver.resolve(md, status, 
                            ModuleRevisionId.newInstance(dependencies[i].getDependencyRevisionId(), rev), 
                            depStatus));
        }
        
        // 4) copy the source resolved ivy to the destination specified, 
        //    updating status, revision and dependency revisions obtained by
        //    PublishingDependencyRevisionResolver
        String publishedIvy = IvyPatternHelper.substitute(destIvyPattern, md.getResolvedModuleRevisionId());
        Message.info("\tdelivering ivy file to "+publishedIvy);
        try {
            XmlModuleDescriptorUpdater.update(_settings, ivyFileURL, 
                    new File(publishedIvy),
                    resolvedDependencies, status, revision, pubdate, null, true);
        } catch (SAXException ex) {
            throw new RuntimeException("bad ivy file in cache for "+mrid+": please clean and resolve again" , ex);
        }
        
        Message.verbose("\tdeliver done ("+(System.currentTimeMillis()-start)+"ms)");
    }

	private CacheManager getCacheManager(File cache) {
		//TODO : reuse instance
		CacheManager cacheManager = new CacheManager(_settings, cache);
		return cacheManager;
	}
}
