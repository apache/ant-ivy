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
package org.apache.ivy.core.retrieve;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.cache.CacheManager;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;

public class RetrieveTest extends TestCase {
    private final Ivy _ivy;
    private File _cache;

    public RetrieveTest() throws Exception {
        _ivy = new Ivy();
        _ivy.configure(new File("test/repositories/ivysettings.xml"));
    }

    protected void setUp() throws Exception {
        createCache();
    }

    private void createCache() {
        _cache = new File("build/cache");
        _cache.mkdirs();
    }
    
    protected void tearDown() throws Exception {
        cleanCache();
        Delete del = new Delete();
        del.setProject(new Project());
        del.setDir(new File("build/test/retrieve"));
        del.execute();
    }

    private void cleanCache() {
        Delete del = new Delete();
        del.setProject(new Project());
        del.setDir(_cache);
        del.execute();
    }

    public void testRetrieveSimple() throws Exception {
        // mod1.1 depends on mod1.2
        ResolveReport report = _ivy.resolve(new File("test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml").toURL(),
        		getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        
        String pattern = "build/test/retrieve/[module]/[conf]/[artifact]-[revision].[ext]";
        _ivy.retrieve(md.getModuleRevisionId(), pattern, getRetrieveOptions());
        assertTrue(new File(IvyPatternHelper.substitute(pattern, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar", "default")).exists());

        pattern = "build/test/retrieve/[module]/[conf]/[type]s/[artifact]-[revision].[ext]";
        _ivy.retrieve(md.getModuleRevisionId(), pattern, getRetrieveOptions());
        assertTrue(new File(IvyPatternHelper.substitute(pattern, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar", "default")).exists());
    }

    public void testRetrieveOverwrite() throws Exception {
        // mod1.1 depends on mod1.2
        ResolveReport report = _ivy.resolve(new File("test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml").toURL(),
        		getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        
        String pattern = "build/test/retrieve/[module]/[conf]/[artifact]-[revision].[ext]";
        
        // we create a fake old file to see if it is overwritten
        File file = new File(IvyPatternHelper.substitute(pattern, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar", "default"));
        file.getParentFile().mkdirs();
        file.createNewFile();
        file.setLastModified(10000);
        _ivy.retrieve(md.getModuleRevisionId(), pattern, getRetrieveOptions());
        assertEquals(new File("test/repositories/1/org1/mod1.2/jars/mod1.2-2.0.jar").lastModified(), file.lastModified());
    }

    public void testRetrieveWithSymlinks() throws Exception {
        // mod1.1 depends on mod1.2
        ResolveReport report = _ivy.resolve(new File("test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml").toURL(),
        		getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        
        String pattern = "build/test/retrieve/[module]/[conf]/[artifact]-[revision].[ext]";
        _ivy.retrieve(md.getModuleRevisionId(), pattern, getRetrieveOptions().setMakeSymlinks(true));
        assertLink(IvyPatternHelper.substitute(pattern, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar", "default"));

        pattern = "build/test/retrieve/[module]/[conf]/[type]s/[artifact]-[revision].[ext]";
        _ivy.retrieve(md.getModuleRevisionId(), pattern, getRetrieveOptions().setMakeSymlinks(true));
        assertLink(IvyPatternHelper.substitute(pattern, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar", "default"));
    }

    private void assertLink(String filename) throws IOException {
    	// if the OS is known to support symlink, check that the file is a symlink,
    	// otherwise just check the file exist.
    	
    	File file = new File(filename);
    	assertTrue("The file " + filename + " doesn't exist", file.exists());
    	
        String os = System.getProperty("os.name");
        if (os.equals("Linux") ||
            os.equals("Solaris") ||
            os.equals("FreeBSD")) {
        	// these OS should support symnlink, so check that the file is actually a symlink.
        	// this is done be checking that the canonical path is different from the absolute
        	// path.
        	File absFile = file.getAbsoluteFile();
        	File canFile = file.getCanonicalFile();
        	assertFalse("The file " + filename + " isn't a symlink", absFile.equals(canFile));
        }
    }

    public void testRetrieveWithVariable() throws Exception {
        // mod1.1 depends on mod1.2
        _ivy.setVariable("retrieve.dir", "retrieve");
        ResolveReport report = _ivy.resolve(new File("test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml").toURL(),
        		getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        
        String pattern = "build/test/${retrieve.dir}/[module]/[conf]/[artifact]-[revision].[ext]";
        _ivy.retrieve(md.getModuleRevisionId(), pattern, getRetrieveOptions());
        pattern = IvyPatternHelper.substituteVariable(pattern, "retrieve.dir", "retrieve");
        assertTrue(new File(IvyPatternHelper.substitute(pattern, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar", "default")).exists());

        pattern = "build/test/${retrieve.dir}/[module]/[conf]/[type]s/[artifact]-[revision].[ext]";
        _ivy.retrieve(md.getModuleRevisionId(), pattern, getRetrieveOptions());
        pattern = IvyPatternHelper.substituteVariable(pattern, "retrieve.dir", "retrieve");
        assertTrue(new File(IvyPatternHelper.substitute(pattern, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar", "default")).exists());
    }

	private RetrieveOptions getRetrieveOptions() {
		return new RetrieveOptions().setCache(CacheManager.getInstance(_ivy.getSettings(), _cache));
	}

    
    private ResolveOptions getResolveOptions(String[] confs) {
		return new ResolveOptions().setConfs(confs).setCache(CacheManager.getInstance(_ivy.getSettings(), _cache));
	}

}
