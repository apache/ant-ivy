/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Random;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;

import fr.jayasoft.ivy.report.ResolveReport;
import fr.jayasoft.ivy.resolver.FileSystemResolver;
import fr.jayasoft.ivy.util.FileUtil;
import fr.jayasoft.ivy.xml.XmlModuleDescriptorWriter;

/**
 * Not a Junit test, performance depends on the machine on which the test is run...
 */
public class TestPerformance {
    private final static String PATTERN = "build/test/perf/[module]/[artifact]-[revision].[ext]";

    private final Ivy _ivy;
    private File _cache;

    public TestPerformance() throws Exception {
        _ivy = new Ivy();
        FileSystemResolver resolver = new FileSystemResolver();
        resolver.setName("def");
        resolver.setIvy(_ivy);
        
        resolver.addIvyPattern(PATTERN);
        resolver.addArtifactPattern(PATTERN);
        
        _ivy.addResolver(resolver);
        _ivy.setDefaultResolver("def");
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
    }

    private void cleanCache() {
        Delete del = new Delete();
        del.setProject(new Project());
        del.setDir(_cache);
        del.execute();
    }

    private void cleanRepo() {
        Delete del = new Delete();
        del.setProject(new Project());
        del.setDir(new File("build/test/perf"));
        del.execute();
    }


    private void generateModules(int nbModules, int minDependencies, int maxDependencies, int minVersions, int maxVersions) throws IOException {
        int nb = 0;
        int curDep = 1;
        int varDeps = maxDependencies - minDependencies;
        int varVersions = maxVersions - minVersions;
        Random r = new Random(System.currentTimeMillis());
        
        while (nb < nbModules) {
            int deps = minDependencies + r.nextInt(varDeps+1);
            int versions = minVersions + r.nextInt(varVersions+1);
            
            int prevCurDep = curDep;
            for (int ver = 0; ver < versions; ver++) {
                DefaultModuleDescriptor md = new DefaultModuleDescriptor(ModuleRevisionId.newInstance("jayasoft", "mod"+nb, "1."+ver),
                        "integration", new Date());
                
                curDep = prevCurDep;
                for (int i = 0; i<deps && curDep < nbModules; i++) {
                    int d;
                    if (i%2 == 1) {
                        d = nb + i;
                        if (d >= prevCurDep) {
                            d = curDep;
                            curDep++;
                        }
                    } else {
                        d = curDep;
                        curDep++;
                    }
                    DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(md, 
                            ModuleRevisionId.newInstance("jayasoft", "mod"+d, "latest.integration"),
                            false, false, true);
                    dd.addDependencyConfiguration("default", "default");
                    md.addDependency(dd);
                }
                XmlModuleDescriptorWriter.write(md, new File("build/test/perf/mod"+nb+"/ivy-1."+ver+".xml"));
                FileUtil.copy(new File("test/repositories/1/org1/mod1.1/jars/mod1.1-1.0.jar"), new File("build/test/perf/mod"+nb+"/mod"+nb+"-1."+ver+".jar"), null);
            }
            nb++;
        }
    }
    
    public void testPerfs() throws Exception {
        generateModules(70, 2, 5, 2, 15);
        
        long start = System.currentTimeMillis();
        ResolveReport report = _ivy.resolve(new File("build/test/perf/mod0/ivy-1.0.xml").toURL(), "1.0", new String[] {"*"}, _cache, null, true);        
        long end = System.currentTimeMillis();
        System.out.println("resolve "+report.getConfigurationReport("default").getNodesNumber()+" modules took "+(end - start)+" ms");

        cleanRepo();
    }
    
    public static void main(String[] args) throws Exception {
        TestPerformance t = new TestPerformance();
        t.setUp();
        t.testPerfs();
        t.tearDown();
    }
}
