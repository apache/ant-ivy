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
package org.apache.ivy.ant;

import java.io.File;

import junit.framework.TestCase;

import org.apache.ivy.TestHelper;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.util.DefaultMessageImpl;
import org.apache.ivy.util.Message;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;

public class IvyPostResolveTaskTest extends TestCase {
    private File _cache;
    private IvyPostResolveTask _task;
    private Project _project;
    
    protected void setUp() throws Exception {
    	Message.init(new DefaultMessageImpl(10));

        createCache();
        _project = new Project();
        _project.setProperty("ivy.settings.file", "test/repositories/ivysettings.xml");

        _task = new IvyPostResolveTask() {
        	public void execute() throws BuildException {
        		prepareAndCheck();
        	}
        };
        _task.setProject(_project);
        _task.setCache(_cache);
    }

    private void createCache() {
        _cache = new File("build/cache");
        _cache.mkdirs();
    }
    
    protected void tearDown() throws Exception {
        cleanCache();
    }

    private void cleanCache() {
        Delete del = new Delete();
        del.setProject(new Project());
        del.setDir(_cache);
        del.execute();
    }

    public void testWithPreviousResolveInSameBuildAndLessConfs() throws Exception {
    	IvyResolve resolve = new IvyResolve();
    	resolve.setProject(_project);
    	resolve.setCache(_cache);
    	resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-multiconf.xml"));
    	resolve.setConf("default,compile");
    	resolve.execute();

    	ResolveReport reportBefore = (ResolveReport) _project.getReference("ivy.resolved.report");
    
    	_task.setConf("default");
    	_task.execute();

    	ResolveReport reportAfter = (ResolveReport) _project.getReference("ivy.resolved.report");
    	
    	assertSame("IvyPostResolveTask has performed a resolve where it shouldn't", reportBefore, reportAfter);
    }

    public void testWithPreviousResolveInSameBuildAndSameConfs() throws Exception {
    	IvyResolve resolve = new IvyResolve();
    	resolve.setProject(_project);
    	resolve.setCache(_cache);
    	resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-multiconf.xml"));
    	resolve.setConf("default");
    	resolve.execute();

    	ResolveReport reportBefore = (ResolveReport) _project.getReference("ivy.resolved.report");
    
    	_task.setConf("default");
    	_task.execute();

    	ResolveReport reportAfter = (ResolveReport) _project.getReference("ivy.resolved.report");
    	
    	assertSame("IvyPostResolveTask has performed a resolve where it shouldn't", reportBefore, reportAfter);
    }

    public void testWithPreviousResolveInSameBuildAndWildcard() throws Exception {
    	IvyResolve resolve = new IvyResolve();
    	resolve.setProject(_project);
    	resolve.setCache(_cache);
    	resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-multiconf.xml"));
    	resolve.setConf("*");
    	resolve.execute();

    	ResolveReport reportBefore = (ResolveReport) _project.getReference("ivy.resolved.report");
    
    	_task.setConf("default");
    	_task.execute();

    	ResolveReport reportAfter = (ResolveReport) _project.getReference("ivy.resolved.report");
    	
    	assertSame("IvyPostResolveTask has performed a resolve where it shouldn't", reportBefore, reportAfter);
    }

    public void testWithPreviousResolveInSameBuildAndBothWildcard() throws Exception {
    	IvyResolve resolve = new IvyResolve();
    	resolve.setProject(_project);
    	resolve.setCache(_cache);
    	resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-multiconf.xml"));
    	resolve.setConf("*");
    	resolve.execute();

    	ResolveReport reportBefore = (ResolveReport) _project.getReference("ivy.resolved.report");
    
    	_task.setConf("*");
    	_task.execute();

    	ResolveReport reportAfter = (ResolveReport) _project.getReference("ivy.resolved.report");
    	
    	assertSame("IvyPostResolveTask has performed a resolve where it shouldn't", reportBefore, reportAfter);
    }

    public void testWithPreviousResolveInSameBuildAndMoreConfs() throws Exception {
    	IvyResolve resolve = new IvyResolve();
    	resolve.setProject(_project);
    	resolve.setCache(_cache);
    	resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-multiconf.xml"));
    	resolve.setConf("compile");
    	resolve.execute();

    	ResolveReport reportBefore = (ResolveReport) _project.getReference("ivy.resolved.report");
    	assertTrue(getArchiveFileInCache("org1", "mod1.1", "2.0", "mod1.1", "jar", "jar").exists());
    	assertFalse(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    
    	_task.setConf("*");
    	_task.execute();

    	ResolveReport reportAfter = (ResolveReport) _project.getReference("ivy.resolved.report");
    	
    	assertNotSame("IvyPostResolveTask hasn't performed a resolve where it should have", reportBefore, reportAfter);
    	assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    }

    public void testWithResolveIdAndPreviousResolveInSameBuildAndLessConfs() throws Exception {
    	IvyResolve resolve = new IvyResolve();
    	resolve.setProject(_project);
    	resolve.setCache(_cache);
    	resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-multiconf.xml"));
    	resolve.setConf("default,compile");
    	resolve.setResolveId("testResolveId");
    	resolve.execute();

    	ResolveReport report1 = (ResolveReport) _project.getReference("ivy.resolved.report.testResolveId");

    	// perform another resolve
    	resolve = new IvyResolve();
    	resolve.setProject(_project);
    	resolve.setCache(_cache);
    	resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-simple.xml"));
    	resolve.setConf("*");
    	resolve.execute();
    
    	ResolveReport reportBefore = (ResolveReport) _project.getReference("ivy.resolved.report");
    	
    	_task.setConf("default");
    	_task.setResolveId("testResolveId");
    	_task.execute();

    	ResolveReport reportAfter = (ResolveReport) _project.getReference("ivy.resolved.report");
    	ResolveReport report2 = (ResolveReport) _project.getReference("ivy.resolved.report.testResolveId");
    	
    	assertSame("IvyPostResolveTask has performed a resolve where it shouldn't", reportBefore, reportAfter);
    	assertSame("IvyPostResolveTask has performed a resolve where it shouldn't", report1, report2);
    }

    public void testWithResolveIdAndPreviousResolveInSameBuildAndSameConfs() throws Exception {
    	IvyResolve resolve = new IvyResolve();
    	resolve.setProject(_project);
    	resolve.setCache(_cache);
    	resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-multiconf.xml"));
    	resolve.setConf("default");
    	resolve.setResolveId("testResolveId");
    	resolve.execute();

    	ResolveReport report1 = (ResolveReport) _project.getReference("ivy.resolved.report.testResolveId");

    	// perform another resolve
    	resolve = new IvyResolve();
    	resolve.setProject(_project);
    	resolve.setCache(_cache);
    	resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-simple.xml"));
    	resolve.setConf("*");
    	resolve.execute();

    	ResolveReport reportBefore = (ResolveReport) _project.getReference("ivy.resolved.report");
    
    	_task.setConf("default");
    	_task.setResolveId("testResolveId");
    	_task.execute();

    	ResolveReport reportAfter = (ResolveReport) _project.getReference("ivy.resolved.report");
    	ResolveReport report2 = (ResolveReport) _project.getReference("ivy.resolved.report.testResolveId");
    	
    	assertSame("IvyPostResolveTask has performed a resolve where it shouldn't", reportBefore, reportAfter);
    	assertSame("IvyPostResolveTask has performed a resolve where it shouldn't", report1, report2);
    }

    public void testWithResolveIdAndPreviousResolveInSameBuildAndWildcard() throws Exception {
    	IvyResolve resolve = new IvyResolve();
    	resolve.setProject(_project);
    	resolve.setCache(_cache);
    	resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-multiconf.xml"));
    	resolve.setConf("*");
    	resolve.setResolveId("testResolveId");
    	resolve.execute();

    	ResolveReport report1 = (ResolveReport) _project.getReference("ivy.resolved.report.testResolveId");

    	// perform another resolve
    	resolve = new IvyResolve();
    	resolve.setProject(_project);
    	resolve.setCache(_cache);
    	resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-simple.xml"));
    	resolve.setConf("*");
    	resolve.execute();

    	ResolveReport reportBefore = (ResolveReport) _project.getReference("ivy.resolved.report");
    
    	_task.setConf("default");
    	_task.setResolveId("testResolveId");
    	_task.execute();

    	ResolveReport reportAfter = (ResolveReport) _project.getReference("ivy.resolved.report");
    	ResolveReport report2 = (ResolveReport) _project.getReference("ivy.resolved.report.testResolveId");
    	
    	assertSame("IvyPostResolveTask has performed a resolve where it shouldn't", reportBefore, reportAfter);
    	assertSame("IvyPostResolveTask has performed a resolve where it shouldn't", report1, report2);
    }

    public void testWithResolveIdAndPreviousResolveInSameBuildAndBothWildcard() throws Exception {
    	IvyResolve resolve = new IvyResolve();
    	resolve.setProject(_project);
    	resolve.setCache(_cache);
    	resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-multiconf.xml"));
    	resolve.setConf("*");
    	resolve.setResolveId("testResolveId");
    	resolve.execute();

    	ResolveReport report1 = (ResolveReport) _project.getReference("ivy.resolved.report.testResolveId");

    	// perform another resolve
    	resolve = new IvyResolve();
    	resolve.setProject(_project);
    	resolve.setCache(_cache);
    	resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-simple.xml"));
    	resolve.setConf("*");
    	resolve.execute();

    	ResolveReport reportBefore = (ResolveReport) _project.getReference("ivy.resolved.report");
    
    	_task.setConf("*");
    	_task.setResolveId("testResolveId");
    	_task.execute();

    	ResolveReport reportAfter = (ResolveReport) _project.getReference("ivy.resolved.report");
    	ResolveReport report2 = (ResolveReport) _project.getReference("ivy.resolved.report.testResolveId");
    	
    	assertSame("IvyPostResolveTask has performed a resolve where it shouldn't", reportBefore, reportAfter);
    	assertSame("IvyPostResolveTask has performed a resolve where it shouldn't", report1, report2);
    }

    public void testWithResolveIdAndPreviousResolveInSameBuildAndMoreConfs() throws Exception {
    	IvyResolve resolve = new IvyResolve();
    	resolve.setProject(_project);
    	resolve.setCache(_cache);
    	resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-multiconf.xml"));
    	resolve.setConf("compile");
    	resolve.setResolveId("testResolveId");
    	resolve.execute();

    	ResolveReport report1 = (ResolveReport) _project.getReference("ivy.resolved.report.testResolveId");
    	assertTrue(getArchiveFileInCache("org1", "mod1.1", "2.0", "mod1.1", "jar", "jar").exists());
    	assertFalse(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

    	// perform another resolve
    	resolve = new IvyResolve();
    	resolve.setProject(_project);
    	resolve.setCache(_cache);
    	resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-simple.xml"));
    	resolve.setConf("*");
    	resolve.execute();

    	ResolveReport reportBefore = (ResolveReport) _project.getReference("ivy.resolved.report");
    
    	_task.setConf("*");
    	_task.setResolveId("testResolveId");
    	_task.execute();

    	ResolveReport reportAfter = (ResolveReport) _project.getReference("ivy.resolved.report");
    	ResolveReport report2 = (ResolveReport) _project.getReference("ivy.resolved.report.testResolveId");
    	
    	assertNotSame("IvyPostResolveTask hasn't performed a resolve where it should have", reportBefore, reportAfter);
    	assertNotSame("IvyPostResolveTask hasn't performed a resolve where it should have", report1, report2);
    	assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    }

    private File getArchiveFileInCache(String organisation, String module, String revision, String artifact, String type, String ext) {
		return TestHelper.getArchiveFileInCache(_task.getIvyInstance(), _cache, 
				organisation, module, revision, artifact, type, ext);
	}
}
