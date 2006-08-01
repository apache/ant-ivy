package fr.jayasoft.ivy.ant;

import java.io.File;

import junit.framework.TestCase;
import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.report.ResolveReport;
import fr.jayasoft.ivy.util.FileUtil;

public class AntBuildTriggerTest extends TestCase {
	public void test() throws Exception {
		assertFalse(new File("test/triggers/ant-build/local/A/A.jar").exists());		

		Ivy ivy = new Ivy();
		ivy.configure(new File("test/triggers/ant-build/ivyconf.xml"));
		
		ResolveReport r = ivy.resolve(new File("test/triggers/ant-build/B/ivy.xml"));
		assertFalse(r.hasError());
		
		// should have triggered an A publish
		assertTrue(new File("test/triggers/ant-build/local/A/A.jar").exists());		
	}
	
	protected void tearDown() throws Exception {
		FileUtil.forceDelete(new File("test/triggers/ant-build/local/A"));
		FileUtil.forceDelete(new File("test/triggers/ant-build/cache"));
	}
}
