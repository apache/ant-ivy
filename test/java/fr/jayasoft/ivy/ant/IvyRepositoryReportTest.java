package fr.jayasoft.ivy.ant;

import java.io.File;

import junit.framework.TestCase;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;

public class IvyRepositoryReportTest extends TestCase {
    private File _cache;
    private IvyRepositoryReport _report;
    
    protected void setUp() throws Exception {
        createCache();
        Project project = new Project();
        project.setProperty("ivy.conf.file", "test/repositories/ivyconf-1.xml");

        _report = new IvyRepositoryReport();
        _report.setProject(project);
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
    
    public void test() {}

// no xslt transformation is possible in the junit test on our continuous integration server for the moment...
//    public void testGraph() throws Exception {
//    	_report.setOrganisation("org1");
//		_report.setXml(false);
//		_report.setGraph(true);
//		_report.setTodir(_cache);
//		_report.setOutputname("test-graph");
//    	_report.execute();
//    	File graphml = new File(_cache, "test-graph.graphml");
//    	assertTrue(graphml.exists());
//    	String g = FileUtil.readEntirely(new BufferedReader(new FileReader(graphml)));
//    	assertFalse(g.indexOf("caller") != -1);
//    	assertTrue(g.indexOf("mod1.1") != -1);
//    }
//
//    public void testDot() throws Exception {
//    	_report.setOrganisation("org1");
//		_report.setXml(false);
//		_report.setDot(true);
//		_report.setTodir(_cache);
//		_report.setOutputname("test-graph");
//    	_report.execute();
//    	File dot = new File(_cache, "test-graph.dot");
//    	assertTrue(dot.exists());
//    	String g = FileUtil.readEntirely(new BufferedReader(new FileReader(dot)));
//    	assertFalse(g.indexOf("caller") != -1);
//    	assertTrue(g.indexOf("mod1.1") != -1);
//    }
}
