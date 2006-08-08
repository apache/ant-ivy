/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.ant;

import java.io.File;
import java.net.MalformedURLException;
import java.text.ParseException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.ModuleDescriptor;
import fr.jayasoft.ivy.external.m2.PomModuleDescriptorParser;
import fr.jayasoft.ivy.repository.url.URLResource;

/**
 * Convert a pom to an ivy file
 * 
 * @author Xavier Hanin
 *
 */
public class IvyConvertPom extends IvyTask {
    private File _pomFile = null;
    private File _ivyFile = null;
    
    public File getPomFile() {
        return _pomFile;
    }
    public void setPomFile(File file) {
        _pomFile = file;
    }
    public File getIvyFile() {
        return _ivyFile;
    }
    public void setIvyFile(File ivyFile) {
        _ivyFile = ivyFile;
    }
    
    
    public void execute() throws BuildException {
        try {
            if (_pomFile == null) {
                throw new BuildException("source pom file is required for convertpom task");
            }
            if (_ivyFile == null) {
                throw new BuildException("destination ivy file is required for convertpom task");
            }
            ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(new Ivy(), _pomFile.toURL(), false);
            PomModuleDescriptorParser.getInstance().toIvyFile(_pomFile.toURL().openStream(), new URLResource(_pomFile.toURL()), getIvyFile(), md);
        } catch (MalformedURLException e) {
            throw new BuildException("unable to convert given pom file to url: "+_pomFile+": "+e, e);
        } catch (ParseException e) {
            log(e.getMessage(), Project.MSG_ERR);
            throw new BuildException("syntax errors in pom file "+_pomFile+": "+e, e);
        } catch (Exception e) {
            throw new BuildException("impossible convert given pom file to ivy file: "+e+" from="+_pomFile+" to="+_ivyFile, e);
        }
    }
}
