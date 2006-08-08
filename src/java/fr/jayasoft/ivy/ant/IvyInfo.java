/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.ant;

import java.io.File;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import fr.jayasoft.ivy.Configuration;
import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.ModuleDescriptor;
import fr.jayasoft.ivy.Configuration.Visibility;
import fr.jayasoft.ivy.parser.ModuleDescriptorParserRegistry;

/**
 * Parses information about an ivy file and make them available in ant.
 * 
 * @author Xavier Hanin
 *
 */
public class IvyInfo extends IvyTask {
    private File _file = null;
    
    public File getFile() {
        return _file;
    }
    public void setFile(File file) {
        _file = file;
    }

    public void execute() throws BuildException {
        Ivy ivy = getIvyInstance();
        if (_file == null) {
            _file = new File(getProject().getBaseDir(), getProperty(ivy, "ivy.dep.file"));
        }
        
        try {
			ModuleDescriptor md = ModuleDescriptorParserRegistry.getInstance().parseDescriptor(ivy, _file.toURL(), doValidate(ivy));
            getProject().setProperty("ivy.organisation", md.getModuleRevisionId().getOrganisation());
            getProject().setProperty("ivy.module", md.getModuleRevisionId().getName());
            if (md.getModuleRevisionId().getRevision() != null) {
            	getProject().setProperty("ivy.revision", md.getModuleRevisionId().getRevision());
            } else {
            	getProject().setProperty("ivy.revision", "working@"+Ivy.getLocalHostName());
            }
            getProject().setProperty("ivy.configurations", mergeConfs(md.getConfigurationsNames()));
            
            // store the public configurations in a separate property
            Configuration[] configs = md.getConfigurations();
            List publicConfigsList = new ArrayList();
            for (int i = 0; i < configs.length; i++) {
            	if (Visibility.PUBLIC.equals(configs[i].getVisibility())) {
            		publicConfigsList.add(configs[i].getName());
            	}
            }
            String[] publicConfigs = (String[]) publicConfigsList.toArray(new String[publicConfigsList.size()]);
            getProject().setProperty("ivy.public.configurations", mergeConfs(publicConfigs));
        } catch (MalformedURLException e) {
            throw new BuildException("unable to convert given ivy file to url: "+_file+": "+e, e);
        } catch (ParseException e) {
            log(e.getMessage(), Project.MSG_ERR);
            throw new BuildException("syntax errors in ivy file: "+e, e);
        } catch (Exception e) {
            throw new BuildException("impossible to resolve dependencies: "+e, e);
        }
    }
}
