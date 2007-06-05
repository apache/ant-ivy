package org.apache.ivy.ant;

import java.io.File;
import java.net.MalformedURLException;

import junit.framework.TestCase;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Reference;

public class IvyTaskTest extends TestCase {

    public void testDefaultSettings() throws MalformedURLException {
        Project p = new Project();
        p.setBasedir("test/repositories");
        // p.setProperty("ivy.settings.file" , "ivysettings.xml");
        p.setProperty("myproperty", "myvalue");
        IvyTask task = new IvyTask() {
            public void doExecute() throws BuildException {
            }
        };
        task.setProject(p);

        Ivy ivy = task.getIvyInstance();
        assertNotNull(ivy);
        IvySettings settings = ivy.getSettings();
        assertNotNull(settings);

        assertEquals(new File("build/cache"), settings.getDefaultCache());
        // The next test doesn't always works on windows (mix C: and c: drive)
        assertEquals(new File("test/repositories/ivysettings.xml").getAbsolutePath().toUpperCase(),
            new File((String) settings.getVariables().get("ivy.settings.file")).getAbsolutePath()
                    .toUpperCase());
        assertEquals(new File("test/repositories/ivysettings.xml").toURL().toExternalForm()
                .toUpperCase(), ((String) settings.getVariables().get("ivy.settings.url"))
                .toUpperCase());
        assertEquals(new File("test/repositories").getAbsolutePath().toUpperCase(),
            ((String) settings.getVariables().get("ivy.settings.dir")).toUpperCase());
        assertEquals("myvalue", settings.getVariables().get("myproperty"));
    }

    public void testReferencedSettings() throws MalformedURLException {
        Project p = new Project();
        // p.setBasedir("test/repositories");
        // p.setProperty("ivy.settings.file" , "ivysettings.xml");
        p.setProperty("myproperty", "myvalue");

        IvyAntSettings antSettings = new IvyAntSettings();
        antSettings.setProject(p);
        // antSettings.setId("mySettings");
        antSettings.setFile(new File("test/repositories/ivysettings.xml"));
        p.addReference("mySettings", antSettings);

        IvyTask task = new IvyTask() {
            public void doExecute() throws BuildException {
            }
        };
        task.setProject(p);
        task.setSettingsRef(new Reference(p, "mySettings"));
        Ivy ivy = task.getIvyInstance();
        assertNotNull(ivy);
        IvySettings settings = ivy.getSettings();
        assertNotNull(settings);

        assertEquals(new File("build/cache"), settings.getDefaultCache());
        assertEquals(new File("test/repositories/ivysettings.xml").getAbsolutePath(), settings
                .getVariables().get("ivy.settings.file"));
        assertEquals(new File("test/repositories/ivysettings.xml").toURL().toExternalForm(),
            settings.getVariables().get("ivy.settings.url"));
        assertEquals(new File("test/repositories").getAbsolutePath(), settings.getVariables().get(
            "ivy.settings.dir"));
        assertEquals("myvalue", settings.getVariables().get("myproperty"));

    }

}
