/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy;

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;

import fr.jayasoft.ivy.filter.FilterHelper;
import fr.jayasoft.ivy.report.ResolveReport;
import fr.jayasoft.ivy.util.IvyPatternHelper;

import junit.framework.TestCase;

public class RetrieveTest extends TestCase {
    private final Ivy _ivy;
    private File _cache;

    public RetrieveTest() throws Exception {
        _ivy = new Ivy();
        _ivy.configure(new File("test/repositories/ivyconf.xml"));
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
                null, new String[] {"*"}, _cache, null, true);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        
        String pattern = "build/test/retrieve/[module]/[conf]/[artifact]-[revision].[ext]";
        _ivy.retrieve(md.getModuleRevisionId().getModuleId(), md.getConfigurationsNames(), _cache, pattern);
        assertTrue(new File(IvyPatternHelper.substitute(pattern, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar", "default")).exists());

        pattern = "build/test/retrieve/[module]/[conf]/[type]s/[artifact]-[revision].[ext]";
        _ivy.retrieve(md.getModuleRevisionId().getModuleId(), md.getConfigurationsNames(), _cache, pattern);
        assertTrue(new File(IvyPatternHelper.substitute(pattern, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar", "default")).exists());
    }

    public void testRetrieveWithSymlinks() throws Exception {
        // mod1.1 depends on mod1.2
        ResolveReport report = _ivy.resolve(new File("test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        
        String pattern = "build/test/retrieve/[module]/[conf]/[artifact]-[revision].[ext]";
        _ivy.retrieve(md.getModuleRevisionId().getModuleId(), md.getConfigurationsNames(), _cache, pattern, null, FilterHelper.NO_FILTER, false, false, true);
        assertLink(IvyPatternHelper.substitute(pattern, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar", "default"));

        pattern = "build/test/retrieve/[module]/[conf]/[type]s/[artifact]-[revision].[ext]";
        _ivy.retrieve(md.getModuleRevisionId().getModuleId(), md.getConfigurationsNames(), _cache, pattern, null, FilterHelper.NO_FILTER, false, false, true);
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
                null, new String[] {"*"}, _cache, null, true);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        
        String pattern = "build/test/${retrieve.dir}/[module]/[conf]/[artifact]-[revision].[ext]";
        _ivy.retrieve(md.getModuleRevisionId().getModuleId(), md.getConfigurationsNames(), _cache, pattern);
        pattern = IvyPatternHelper.substituteVariable(pattern, "retrieve.dir", "retrieve");
        assertTrue(new File(IvyPatternHelper.substitute(pattern, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar", "default")).exists());

        pattern = "build/test/${retrieve.dir}/[module]/[conf]/[type]s/[artifact]-[revision].[ext]";
        _ivy.retrieve(md.getModuleRevisionId().getModuleId(), md.getConfigurationsNames(), _cache, pattern);
        pattern = IvyPatternHelper.substituteVariable(pattern, "retrieve.dir", "retrieve");
        assertTrue(new File(IvyPatternHelper.substitute(pattern, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar", "default")).exists());
    }


}
