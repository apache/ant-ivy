/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.ant;

import java.io.File;

import junit.framework.TestCase;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;

public class IvyReportTest extends TestCase {
    private File _cache;
    private IvyReport _report;
    private Project _project;
    
    protected void setUp() throws Exception {
        createCache();
        _project = new Project();
        _project.setProperty("ivy.conf.file", "test/repositories/ivyconf.xml");

        _report = new IvyReport();
        _report.setTaskName("report");
        _report.setProject(_project);
        _report.setCache(_cache);
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

    public void testRegularCircular() throws Exception {
        _project.setProperty("ivy.dep.file", "test/repositories/2/mod11.1/ivy-1.0.xml");
        IvyResolve res = new IvyResolve();
        res.setProject(_project);
        res.execute();
        
        _report.setTodir(new File(_cache, "report"));
        _report.setXml(true);
        
        // do not test any xsl transformation here, because of problems of build in our continuous integration server
        _report.setXsl(false); 
        _report.setGraph(false);
        
        _report.execute();
        
        assertTrue(new File(_cache, "report/org11-mod11.1-compile.xml").exists());
    }
}
